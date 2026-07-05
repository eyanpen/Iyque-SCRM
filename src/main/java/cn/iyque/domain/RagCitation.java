package cn.iyque.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 单条 RAG 引用元数据，跟随 SSE 首帧下发给前端。
 * 前端拿到后可以：
 *   - 把 AI 回答里的 [资料 N] 变成可点击 span (idx 匹配)
 *   - 点击后调 GET /iYqueAi/rag/fragment/{fid} 拉全文
 *   - 通过 docId 调 GET /knowledge/attach/download/{docId} 下载原始文件
 *
 * ID 字段用 String 序列化 —— Snowflake Long 超过 JS Number.MAX_SAFE_INTEGER,
 * 直接传数值会精度丢失，所以显式声明为 String。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagCitation {
    /** 在 prompt 里的序号 (1-based)，与 AI 回答里 [资料 N] 的 N 对齐 */
    private int idx;

    /** iyque_knowledge_fragment.id */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fid;

    /** iyque_knowledge_attach.id (原始文件) */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long docId;

    /** iyque_knowledge_info.id (知识库) */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long kid;

    /** 原始文件名（如 "穷查理宝典.pdf"）—— 方便前端不用二次请求就显示 */
    private String docName;

    /** Qdrant Cosine 相似度 (0-1)。可为 null，若上下游未透传 */
    private Double score;
}
