package cn.iyque.chain.vectorstore;

import cn.iyque.config.IYqueParamConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Qdrant 向量存储 (REST) —— 替代原 MilvusVectorStore。
 *
 * <p>使用 Qdrant Cloud / self-hosted 的 REST HTTP 接口 (默认端口 6333，云端 HTTPS)。
 * 通过 IYqueParamConfig.VectorStoreParam.endpoint + apiKey 完成连接。</p>
 *
 * <p>集合命名 = {@code collectionName + kid}，与原 Milvus 保持一致。<br/>
 * 每条向量的 payload 保存: kid / docId / fid，nearest 查询返回 fid 列表。</p>
 */
@Service
@Slf4j
public class QdrantVectorStore implements IYqueVectorStore {

    @Autowired
    private IYqueParamConfig paramConfig;

    private static final ObjectMapper mapper = new ObjectMapper();
    private HttpClient http;

    @PostConstruct
    public void init() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        log.info("QdrantVectorStore 初始化, endpoint={}", maskEndpoint());
    }

    private String maskEndpoint() {
        String ep = paramConfig.getVector() != null ? paramConfig.getVector().getEndpoint() : null;
        return StringUtils.isEmpty(ep) ? "<unset>" : ep;
    }

    private String base() {
        IYqueParamConfig.VectorStoreParam v = paramConfig.getVector();
        if (v == null) return null;
        String ep = v.getEndpoint();
        if (StringUtils.isEmpty(ep)) return null;
        return ep.replaceAll("/+$", "");
    }

    private String collectionOf(String kid) {
        return paramConfig.getVector().getCollectionName() + kid;
    }

    private HttpRequest.Builder req(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(base() + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json");
        String key = paramConfig.getVector().getApiKey();
        if (StringUtils.isNotEmpty(key)) {
            // Qdrant Cloud 同时接受 api-key header 和 Authorization Bearer
            b.header("api-key", key);
        }
        return b;
    }

    private JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            log.warn("Qdrant {} {} -> {}: {}", request.method(), request.uri(), resp.statusCode(), resp.body());
        }
        String body = resp.body();
        if (body == null || body.isEmpty()) return mapper.createObjectNode();
        return mapper.readTree(body);
    }

    private boolean collectionExists(String name) {
        if (base() == null) return false;
        try {
            HttpRequest r = req("/collections/" + name + "/exists").GET().build();
            JsonNode j = send(r);
            if (j.at("/result/exists").isBoolean()) {
                return j.at("/result/exists").asBoolean();
            }
            // 兼容老版本 Qdrant: /collections/{name} 200 表示存在
            HttpRequest r2 = req("/collections/" + name).GET().build();
            HttpResponse<String> resp = http.send(r2, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.warn("检查 Qdrant collection {} 是否存在失败: {}", name, e.getMessage());
            return false;
        }
    }

    private void ensureCollection(String name, int dim) {
        if (collectionExists(name)) return;
        try {
            Map<String, Object> vectors = new LinkedHashMap<>();
            vectors.put("size", dim);
            vectors.put("distance", "Cosine");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("vectors", vectors);

            HttpRequest r = req("/collections/" + name)
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            JsonNode j = send(r);
            log.info("Qdrant 创建 collection {} (dim={}) -> {}", name, dim, j.path("status").asText("?"));
        } catch (Exception e) {
            log.error("创建 Qdrant collection {} 失败", name, e);
            throw new RuntimeException("Qdrant collection 创建失败: " + name, e);
        }
    }

    @Override
    public void newSchema(String kid) {
        if (base() == null) {
            log.warn("Qdrant endpoint 未配置，跳过 newSchema(kid={})", kid);
            return;
        }
        Integer dim = paramConfig.getVector().getDimension();
        ensureCollection(collectionOf(kid), dim == null ? 1536 : dim);
    }

    @Override
    public void storeEmbeddings(List<String> chunkList, List<List<Float>> vectorList,
                                String kid, String docId, List<String> fidList) {
        if (base() == null) {
            log.warn("Qdrant endpoint 未配置，跳过 storeEmbeddings(kid={}, docId={})", kid, docId);
            return;
        }
        if (vectorList == null || vectorList.isEmpty()) {
            log.warn("storeEmbeddings 收到空向量列表 (kid={}, docId={})", kid, docId);
            return;
        }

        String collection = collectionOf(kid);
        ensureCollection(collection, vectorList.get(0).size());

        List<Map<String, Object>> points = new ArrayList<>();
        int n = Math.min(chunkList.size(), vectorList.size());
        for (int i = 0; i < n; i++) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("kid", kid);
            payload.put("docId", docId);
            payload.put("fid", i < fidList.size() ? fidList.get(i) : null);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("id", UUID.randomUUID().toString());
            point.put("vector", vectorList.get(i));
            point.put("payload", payload);
            points.add(point);
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("points", points);
            HttpRequest r = req("/collections/" + collection + "/points?wait=true")
                    .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            JsonNode j = send(r);
            log.info("Qdrant upsert 到 {}: {} 条 -> {}", collection, points.size(), j.path("status").asText("?"));
        } catch (Exception e) {
            log.error("Qdrant upsert 失败: {}", collection, e);
        }
    }

    @Override
    public void removeByKid(String kid) {
        if (base() == null) return;
        String collection = collectionOf(kid);
        try {
            HttpRequest r = req("/collections/" + collection).DELETE().build();
            JsonNode j = send(r);
            log.info("Qdrant drop collection {} -> {}", collection, j.path("status").asText("?"));
        } catch (Exception e) {
            log.warn("Qdrant drop collection {} 失败: {}", collection, e.getMessage());
        }
    }

    @Override
    public List<String> nearest(List<Float> queryVector, String kid) {
        if (base() == null) return Collections.emptyList();
        String collection = collectionOf(kid);
        if (!collectionExists(collection)) {
            log.info("Qdrant collection {} 不存在，nearest 返回空", collection);
            return Collections.emptyList();
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("vector", queryVector);
            body.put("limit", 3);
            body.put("with_payload", true);
            Float score = paramConfig.getVector().getScore();
            if (score != null) body.put("score_threshold", score);

            HttpRequest r = req("/collections/" + collection + "/points/search")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            JsonNode j = send(r);
            List<String> fids = new ArrayList<>();
            JsonNode arr = j.path("result");
            if (arr.isArray()) {
                for (JsonNode hit : arr) {
                    String fid = hit.path("payload").path("fid").asText(null);
                    if (StringUtils.isNotEmpty(fid) && !"null".equals(fid)) fids.add(fid);
                }
            }
            log.debug("Qdrant nearest({}) -> {} hits", collection, fids.size());
            return fids;
        } catch (Exception e) {
            log.error("Qdrant search 失败: {}", collection, e);
            return Collections.emptyList();
        }
    }
}
