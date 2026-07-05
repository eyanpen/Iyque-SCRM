package cn.iyque.controller;

import cn.iyque.constant.HttpStatus;
import cn.iyque.dao.IYqueKnowledgeAttachDao;
import cn.iyque.dao.IYqueKnowledgeFragmentDao;
import cn.iyque.domain.ResponseResult;
import cn.iyque.domain.FunctionRoute;
import cn.iyque.domain.AiChatRequest;
import cn.iyque.domain.EmbeddingResponse;
import cn.iyque.entity.IYqueKnowledgeAttach;
import cn.iyque.entity.IYqueKnowledgeFragment;
import cn.iyque.exception.IYqueException;
import cn.iyque.factory.AiModelFactory;
import cn.iyque.service.IYqueAiService;
import cn.iyque.service.FunctionRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI控制器
 * 提供AI相关的RESTful接口，包括模型管理、聊天对话、向量计算等功能
 */
@RestController
@RequestMapping("/iYqueAi")
public class IYqueAiController {

    @Autowired
    private IYqueAiService iYqueAiService;
    
    @Autowired
    private AiModelFactory aiModelFactory;
    
    @Autowired
    private FunctionRouteService functionRouteService;

    @Autowired
    private IYqueKnowledgeFragmentDao yqueKnowledgeFragmentDao;

    @Autowired
    private IYqueKnowledgeAttachDao yqueKnowledgeAttachDao;

    /**
     * RAG 引用详情查询。前端点击 [资料 N] 时用 fid 来这里拉全量内容 + 附件元信息。
     *
     * 返回字段:
     *   fid, kid, docId, idx (在原文档里的分片序号), content, createTime
     *   docName (来自 iyque_knowledge_attach)
     *   docType
     *   downloadUrl  相对路径, 前端直接拼 baseURL 后可下载原始文件
     */
    @GetMapping("/rag/fragment/{fid}")
    public ResponseResult<Map<String, Object>> getRagFragment(@PathVariable Long fid) {
        Optional<IYqueKnowledgeFragment> opt = yqueKnowledgeFragmentDao.findById(fid);
        if (!opt.isPresent()) {
            // 语义上"记录不存在"不是异常, 用 code=200 + data=null 让前端安静地展示
            // "未找到" 而不是弹全局红色 Notification.error。同时保留 msg 供前端展示原因。
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("fid", String.valueOf(fid));
            notFound.put("notFound", true);
            ResponseResult<Map<String, Object>> resp = new ResponseResult<>(notFound);
            resp.setMsg("片段不存在或已被删除: " + fid);
            return resp;
        }
        IYqueKnowledgeFragment frag = opt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("fid", String.valueOf(frag.getId()));   // Long -> String 避免 JS 精度丢失
        data.put("kid", frag.getKid() != null ? String.valueOf(frag.getKid()) : null);
        data.put("docId", frag.getDocId() != null ? String.valueOf(frag.getDocId()) : null);
        data.put("idx", frag.getIdx());
        data.put("content", frag.getContent());
        data.put("createTime", frag.getCreateTime());

        if (frag.getDocId() != null) {
            Optional<IYqueKnowledgeAttach> attachOpt = yqueKnowledgeAttachDao.findById(frag.getDocId());
            if (attachOpt.isPresent()) {
                IYqueKnowledgeAttach attach = attachOpt.get();
                data.put("docName", attach.getDocName());
                data.put("docType", attach.getDocType());
                data.put("downloadUrl", "/knowledge/attach/download/" + attach.getId());
                // 探测原始文件是否真的在磁盘上, 前端据此决定"下载"按钮 disable 与否。
                // 与 downloadAttach 的路径策略保持一致: 优先 upload/<docId>_<docName>, 兜底 upload/<docName>
                boolean fileAvailable = false;
                try {
                    java.nio.file.Path base = java.nio.file.Paths.get(
                            System.getProperty("iyque.upload.dir", "upload")).toAbsolutePath().normalize();
                    java.nio.file.Path c1 = base.resolve(attach.getId() + "_" + attach.getDocName()).normalize();
                    java.nio.file.Path c2 = base.resolve(attach.getDocName()).normalize();
                    fileAvailable =
                            (c1.startsWith(base) && c1.toFile().isFile()) ||
                            (c2.startsWith(base) && c2.toFile().isFile());
                } catch (Exception ignore) { /* fileAvailable stays false */ }
                data.put("fileAvailable", fileAvailable);
            } else {
                data.put("fileAvailable", false);
            }
        } else {
            data.put("fileAvailable", false);
        }
        return new ResponseResult<>(data);
    }
    
    /**
     * 获取可用的聊天模型列表
     * @return 模型名称列表
     */
    @GetMapping("/models")
    public ResponseResult getAvailableModels() {
        try {
            List<String> models = aiModelFactory.getEnabledModels();
            return new ResponseResult(models);
        } catch (Exception e) {
            return new ResponseResult(HttpStatus.ERROR, "获取模型列表失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取可用的向量模型列表
     * @return 向量模型名称列表
     */
    @GetMapping("/embeddingModels")
    public ResponseResult getAvailableEmbeddingModels() {
        try {
            List<String> models = aiModelFactory.getEnabledEmbeddingModels();
            return new ResponseResult(models);
        } catch (Exception e) {
            return new ResponseResult(HttpStatus.ERROR, "获取向量模型列表失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取系统功能路由列表
     * 用于AI导航功能，返回系统中所有可用的功能模块信息
     * @return 功能路由列表，包含路径、标题、分类和描述
     */
    @GetMapping("/functionRoutes")
    public ResponseResult getFunctionRoutes() {
        try {
            List<FunctionRoute> routes = functionRouteService.getAllFunctionRoutes();
            return new ResponseResult(routes);
        } catch (Exception e) {
            return new ResponseResult(HttpStatus.ERROR, "获取功能路由列表失败: " + e.getMessage(), null);
        }
    }


    /**
     * 具有记忆功能的AI流式对话
     * 支持多轮对话上下文记忆，可自定义模型参数
     * @param request 包含问题、历史记录、模型配置等参数的请求体
     * @return SSE流式响应，实时返回AI回答内容
     */
    @PostMapping(value = "/chatWithMemoryStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatWithMemoryStream(@RequestBody AiChatRequest request) {
        try {
            if (request.isQuestionEmpty()) {
                return Flux.just(ServerSentEvent.<String>builder()
                        .data("{\"error\": \"" + AiChatRequest.ERROR_QUESTION_EMPTY + "\"}")
                        .build());
            }

            return iYqueAiService.aiChatWithMemoryStream(
                    request.getQuestion(),
                    request.getHistory(),
                    request.getModelName(),
                    request.getRole(),
                    request.getTemperature(),
                    request.getTopP(),
                    request.getMaxHistoryRounds(),
                    request.getKid())
                    .map(content -> ServerSentEvent.<String>builder()
                            .data(content)
                            .build())
                    .onErrorResume(e -> {
                        String errorMessage = e instanceof IYqueException ? e.getMessage() : request.getChatErrorMessage(e);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .data("{\"error\": \"" + errorMessage + "\"}")
                                .build());
                    });
        } catch (Exception e) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("{\"error\": \"" + request.getChatErrorMessage(e) + "\"}")
                    .build());
        }
    }

    /**
     * AI导航推荐流式对话
     * 根据用户问题智能推荐系统功能路径，无需记忆功能
     * @param request 包含问题、模型配置等参数的请求体
     * @return SSE流式响应，返回功能推荐内容
     */
    @PostMapping(value = "/navigationChatStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> navigationChatStream(@RequestBody AiChatRequest request) {
        try {
            if (request.isQuestionEmpty()) {
                return Flux.just(ServerSentEvent.<String>builder()
                        .data("{\"error\": \"" + AiChatRequest.ERROR_QUESTION_EMPTY + "\"}")
                        .build());
            }

            return iYqueAiService.aiNavigationChatStream(
                    request.getQuestion(),
                    request.getModelName(),
                    request.getRole(),
                    request.getTemperature(),
                    request.getTopP(),
                    request.getKid())
                    .map(content -> ServerSentEvent.<String>builder()
                            .data(content)
                            .build())
                    .onErrorResume(e -> {
                        String errorMessage = e instanceof IYqueException ? e.getMessage() : request.getNavigationErrorMessage(e);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .data("{\"error\": \"" + errorMessage + "\"}")
                                .build());
                    });
        } catch (Exception e) {
            return Flux.just(ServerSentEvent.<String>builder()
                    .data("{\"error\": \"" + request.getNavigationErrorMessage(e) + "\"}")
                    .build());
        }
    }

    /**
     * 文本向量化
     * 将文本转换为向量表示，用于向量检索等场景
     * @param texts 文本列表
     * @return 向量响应，包含每个文本对应的向量
     */
    @PostMapping("/embedding")
    public ResponseResult embedding(@RequestBody List<String> texts) {
        try {
            EmbeddingResponse response = iYqueAiService.embedding(texts);
            return new ResponseResult(response);
        } catch (Exception e) {
            return new ResponseResult(HttpStatus.ERROR, "文本向量化失败: " + e.getMessage(), null);
        }
    }

    /**
     * 文本向量化（指定模型）
     * 将文本转换为向量表示，支持指定向量模型
     * @param texts 文本列表
     * @param modelName 向量模型名称
     * @return 向量响应，包含每个文本对应的向量
     */
    @PostMapping("/embedding/{modelName}")
    public ResponseResult embeddingWithModel(@RequestBody List<String> texts, @org.springframework.web.bind.annotation.PathVariable String modelName) {
        try {
            EmbeddingResponse response = iYqueAiService.embedding(texts, modelName);
            return new ResponseResult(response);
        } catch (Exception e) {
            return new ResponseResult(HttpStatus.ERROR, "文本向量化失败: " + e.getMessage(), null);
        }
    }
}
