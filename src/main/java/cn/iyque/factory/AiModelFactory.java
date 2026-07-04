package cn.iyque.factory;

import cn.iyque.properties.AiModelsProperties;
import cn.iyque.properties.AiVectorProperties;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Slf4j
public class AiModelFactory {
    private final AiModelsProperties chatProperties;
    private final AiVectorProperties vectorProperties;

    private final Map<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingModel> embeddingModelCache = new ConcurrentHashMap<>();

    public AiModelFactory(AiModelsProperties chatProperties, AiVectorProperties vectorProperties) {
        this.chatProperties = chatProperties;
        this.vectorProperties = vectorProperties;
    }

    /**
     * 构造一个强制走 HTTP/1.1 的 langchain4j HttpClient builder。
     * <p>
     * JDK 内置 {@link java.net.http.HttpClient} 默认尝试 HTTP/2 明文升级 (h2c)，
     * 部分 OpenAI 兼容服务端 (如 Ray Serve) 不支持 h2c，会直接返回
     * 400 "Invalid HTTP request received."。强制 HTTP/1.1 可规避。
     * 同时对本地 / 私有模型来说，通用大模型响应时间可能超过默认 HTTP 超时（分钟级），
     * 这里把连接和读取超时都调大。
     */
    private JdkHttpClientBuilder http1ClientBuilder() {
        return new JdkHttpClientBuilder()
                .httpClientBuilder(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1))
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(600));
    }

    @PostConstruct
    public void initializeModels() {
        initializeChatModels();
        initializeEmbeddingModels();
    }

    private void initializeChatModels() {
        for (String modelName : chatProperties.getEnabled()) {
            modelName = modelName.trim();
            AiModelsProperties.ModelConfig config = chatProperties.getConfigs().get(modelName);

            if (config == null || !config.isValid()) {
                log.warn("聊天模型 [{}] 配置缺失或 apiKey 为空，跳过初始化", modelName);
                continue;
            }

            ChatLanguageModel chatModel = OpenAiChatModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .modelName(config.getModelName())
                    .timeout(Duration.ofSeconds(600))
                    .httpClientBuilder(http1ClientBuilder())
                    .build();
            chatModelCache.put(modelName, chatModel);

            StreamingChatLanguageModel streamingModel = OpenAiStreamingChatModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .modelName(config.getModelName())
                    .timeout(Duration.ofSeconds(600))
                    .httpClientBuilder(http1ClientBuilder())
                    .build();
            streamingModelCache.put(modelName, streamingModel);

            log.info("已加载聊天模型: {} | 同步={}, 流式={}",
                    modelName,
                    chatModel.getClass().getSimpleName(),
                    streamingModel.getClass().getSimpleName());
        }

        if (chatModelCache.isEmpty()) {
            log.warn("未加载任何聊天模型！请检查 ai.models 配置");
        }
    }

    private void initializeEmbeddingModels() {
        if (vectorProperties.getEnabled() == null || vectorProperties.getEnabled().isEmpty()) {
            log.warn("未配置向量模型！请检查 ai.vector.enabled 配置");
            return;
        }

        for (String modelName : vectorProperties.getEnabled()) {
            modelName = modelName.trim();
            AiVectorProperties.ModelConfig config = vectorProperties.getConfigs().get(modelName);

            if (config == null || !config.isValid()) {
                log.warn("向量模型 [{}] 配置缺失或 apiKey 为空，跳过初始化", modelName);
                continue;
            }

            EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .modelName(config.getModelName())
                    .timeout(Duration.ofSeconds(600))
                    .httpClientBuilder(http1ClientBuilder())
                    .build();
            embeddingModelCache.put(modelName, embeddingModel);

            log.info("已加载向量模型: {}", modelName);
        }

        if (embeddingModelCache.isEmpty()) {
            log.warn("未加载任何向量模型！请检查 ai.vector 配置");
        }
    }

    public ChatLanguageModel getChatModel(String modelName) {
        ChatLanguageModel model = chatModelCache.get(modelName);
        if (model == null) {
            throw new IllegalArgumentException("同步模型未启用或配置缺失: " + modelName +
                    "，可用模型: " + chatModelCache.keySet());
        }
        return model;
    }

    public ChatLanguageModel getChatModel(String modelName, Double temperature, Double topP) {
        AiModelsProperties.ModelConfig config = chatProperties.getConfigs().get(modelName);
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("同步模型未启用或配置缺失: " + modelName);
        }
        
        return OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(600))
                .temperature(temperature != null ? temperature : 0.7)
                .topP(topP != null ? topP : 0.9)
                .httpClientBuilder(http1ClientBuilder())
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    public StreamingChatLanguageModel getStreamingModel(String modelName) {
        StreamingChatLanguageModel model = streamingModelCache.get(modelName);
        if (model == null) {
            throw new IllegalArgumentException("流式模型未启用或配置缺失: " + modelName +
                    "，可用模型: " + streamingModelCache.keySet());
        }
        return model;
    }

    public StreamingChatLanguageModel getStreamingModel(String modelName, Double temperature, Double topP) {
        AiModelsProperties.ModelConfig config = chatProperties.getConfigs().get(modelName);
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("流式模型未启用或配置缺失: " + modelName);
        }
        
        return OpenAiStreamingChatModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(600))
                .temperature(temperature != null ? temperature : 0.7)
                .topP(topP != null ? topP : 0.9)
                .httpClientBuilder(http1ClientBuilder())
                .build();
    }

    public List<String> getEnabledChatModels() {
        return new ArrayList<>(chatModelCache.keySet());
    }

    public List<String> getEnabledModels() {
        return getEnabledChatModels();
    }

    public List<String> getEnabledEmbeddingModels() {
        return new ArrayList<>(embeddingModelCache.keySet());
    }

    public EmbeddingModel getEmbeddingModel(String modelName) {
        EmbeddingModel model = embeddingModelCache.get(modelName);
        if (model == null) {
            throw new IllegalArgumentException("向量模型未启用或配置缺失: " + modelName +
                    "，可用模型: " + embeddingModelCache.keySet());
        }
        return model;
    }

    public EmbeddingModel getEmbeddingModel(String modelName, Integer dimension) {
        EmbeddingModel model = embeddingModelCache.get(modelName);
        if (model != null) {
            return model;
        }

        AiVectorProperties.ModelConfig config = vectorProperties.getConfigs().get(modelName);
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("向量模型未启用或配置缺失: " + modelName +
                    "，可用模型: " + embeddingModelCache.keySet());
        }

        model = OpenAiEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .baseUrl(config.getBaseUrl())
                .modelName(config.getModelName())
                .timeout(Duration.ofSeconds(600))
                .httpClientBuilder(http1ClientBuilder())
                .build();

        embeddingModelCache.put(modelName, model);
        log.info("已创建向量模型: {}, dimension: {}", modelName, dimension);
        return model;
    }
}
