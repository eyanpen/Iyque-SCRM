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
@TableName("iyque_ai_conversation")
public class IYqueAiConversation {
    @TableId
    private Long id;

    private String conversationId;

    private String title;

    private String mode;

    private Long userId;

    private String modelName;

    private String role;

    private Double temperature;

    private Double topP;

    private Integer maxHistoryRounds;

    /**
     * AI Chat 选中的知识库 id (iyque_knowledge_info.id)。
     * NULL 表示该会话不启用 RAG；非 NULL 时，chatWithMemoryStream 会先做检索再回答。
     * 与会话本身一起持久化，跨设备/浏览器一致。
     *
     * updateStrategy = IGNORED 是关键：MyBatis-Plus 默认 NOT_NULL 策略会跳过 null
     * 字段的 UPDATE，导致用户从"选中"改回"不使用"时后端无法清空。
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Long kid;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String lastMessage;

    @TableField(exist = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastMessageTime;

    private Integer deviceType;
}
