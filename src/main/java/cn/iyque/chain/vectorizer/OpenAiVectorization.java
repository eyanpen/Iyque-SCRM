package cn.iyque.chain.vectorizer;

import cn.iyque.properties.AiVectorProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 /v1/embeddings 端点向量化实现。
 *
 * <p>读取 {@code ai.vector.configs.embedding.{api-key, base-url, model-name}}，
 * 直接发起 HTTP POST 请求。适用于:</p>
 * <ul>
 *     <li>本地 Ollama: {@code base-url=http://localhost:11434/v1}, api-key 任意占位</li>
 *     <li>阿里百炼:   {@code base-url=https://.../compatible-mode/v1}</li>
 *     <li>SiliconFlow / GLM 等所有兼容端点</li>
 * </ul>
 */
@Component
@Slf4j
public class OpenAiVectorization implements Vectorization {

    @Autowired
    private AiVectorProperties aiVectorProperties;

    private static final ObjectMapper mapper = new ObjectMapper();
    private HttpClient http;

    @PostConstruct
    public void init() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private AiVectorProperties.ModelConfig embeddingConfig() {
        if (aiVectorProperties == null || aiVectorProperties.getConfigs() == null) return null;
        return aiVectorProperties.getConfigs().get("embedding");
    }

    @Override
    public List<List<Float>> batchVectorization(List<String> chunkList, String kid) {
        if (chunkList == null || chunkList.isEmpty()) return Collections.emptyList();

        AiVectorProperties.ModelConfig cfg = embeddingConfig();
        if (cfg == null || cfg.getBaseUrl() == null || cfg.getModelName() == null) {
            log.warn("ai.vector.configs.embedding 未配置，返回空向量 (chunks={})", chunkList.size());
            return Collections.emptyList();
        }

        // 分批发送：Ollama /v1/embeddings 对单请求内的 input 数组长度有上限
        // (实测 691 条会返回 HTTP 400)，这里按 EMBEDDING_BATCH_SIZE 切成小组顺序调用。
        // 一次失败即返回空列表，让上层 (storeEmbeddings) 优雅跳过 Qdrant upsert。
        final int total = chunkList.size();
        if (total <= EMBEDDING_BATCH_SIZE) {
            return callEmbeddingsBatch(cfg, chunkList);
        }

        List<List<Float>> all = new ArrayList<>(total);
        int batches = (total + EMBEDDING_BATCH_SIZE - 1) / EMBEDDING_BATCH_SIZE;
        log.info("Embedding 分批: 总 chunks={}, 每批={}, 共 {} 批", total, EMBEDDING_BATCH_SIZE, batches);
        for (int i = 0; i < total; i += EMBEDDING_BATCH_SIZE) {
            int to = Math.min(i + EMBEDDING_BATCH_SIZE, total);
            List<String> sub = chunkList.subList(i, to);
            List<List<Float>> part = callEmbeddingsBatch(cfg, sub);
            if (part.isEmpty() || part.size() != sub.size()) {
                log.error("Embedding 分批失败: 批次 {}/{} (offset {}~{}), 返回 {} 条，预期 {} 条 —— 中止",
                        (i / EMBEDDING_BATCH_SIZE) + 1, batches, i, to - 1, part.size(), sub.size());
                return Collections.emptyList();
            }
            all.addAll(part);
        }
        return all;
    }

    /** Ollama /v1/embeddings 单请求最多同时向量化的文本条数。 */
    private static final int EMBEDDING_BATCH_SIZE = 64;

    /** 单次 HTTP 调用：把一批文本发给 embedding 端点，返回对应的向量列表。 */
    private List<List<Float>> callEmbeddingsBatch(AiVectorProperties.ModelConfig cfg, List<String> chunkList) {
        try {
            String url = cfg.getBaseUrl().replaceAll("/+$", "") + "/embeddings";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", cfg.getModelName());
            body.put("input", chunkList);
            String payload = mapper.writeValueAsString(body);

            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(10))   // 与 AiModelFactory.readTimeout(600s) 保持一致
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
            if (cfg.getApiKey() != null && !cfg.getApiKey().isEmpty()) {
                rb.header("Authorization", "Bearer " + cfg.getApiKey());
            }

            log.info("AI请求 [Ollama embedding] modelKey=embedding, model={}@{}, texts={}, firstPreview=\"{}\"",
                    cfg.getModelName(), url, chunkList.size(), truncate(chunkList.get(0), 80));
            long __t0 = System.currentTimeMillis();
            HttpResponse<String> resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            long __elapsed = System.currentTimeMillis() - __t0;
            if (resp.statusCode() >= 400) {
                log.error("AI响应 [Ollama embedding] modelKey=embedding, model={}@{}, HTTP {} elapsed={}ms body={}",
                        cfg.getModelName(), url, resp.statusCode(), __elapsed, truncate(resp.body(), 500));
                return Collections.emptyList();
            }
            log.info("AI响应 [Ollama embedding] modelKey=embedding, model={}@{}, HTTP {} elapsed={}ms body-len={}",
                    cfg.getModelName(), url, resp.statusCode(), __elapsed,
                    resp.body() == null ? 0 : resp.body().length());

            JsonNode root = mapper.readTree(resp.body());
            List<List<Float>> out = new ArrayList<>();
            for (JsonNode d : root.path("data")) {
                List<Float> v = new ArrayList<>();
                for (JsonNode x : d.path("embedding")) v.add((float) x.asDouble());
                out.add(v);
            }
            if (out.isEmpty()) {
                log.warn("Embedding 返回体 data 为空: {}", truncate(resp.body(), 500));
            }
            return out;
        } catch (Exception e) {
            log.error("批量向量化调用失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Float> singleVectorization(String chunk, String kid) {
        List<List<Float>> r = batchVectorization(Collections.singletonList(chunk), kid);
        return r.isEmpty() ? Collections.emptyList() : r.get(0);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…(" + (s.length() - max) + " more)";
    }
}
