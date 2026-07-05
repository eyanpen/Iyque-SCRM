package cn.iyque.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("iyque_ai_conversation_message")
public class IYqueAiConversationMessage {

    @TableId
    private Long id;

    private String conversationId;

    private String type;

    private String content;

    private String timestamp;

    /**
     * RAG 引用元数据 (仅 AI 消息可能有值)。存储序列化后的 JSON 数组：
     *   [{ "idx":1, "fid":"...", "docId":"...", "kid":"...", "docName":"..." }, ...]
     * 用途：会话重新加载时把 [资料 N] 恢复为可点击 span。空 / null 表示该消息没做 RAG。
     * 数据库列用 LONGTEXT (足够放 6~20 条 citation)。
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String citations;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
