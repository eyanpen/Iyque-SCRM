package cn.iyque.domain;

import lombok.Data;

@Data
public class AiChatRequest {

    private String question;

    private String history;

    private String modelName;

    private String role;

    private Double temperature;

    private Double topP;

    private Integer maxHistoryRounds;

    /**
     * 可选：选中的知识库 ID（iyque_knowledge_info.id）。
     * 非 null 时后端会先做一次 RAG 检索，把命中的知识片段拼进 system prompt。
     * 为 null 表示纯对话，不走知识库。
     */
    private Long kid;

    public static final String ERROR_QUESTION_EMPTY = "问题不能为空";
    public static final String ERROR_CHAT_PROCESS = "AI处理问题异常: ";
    public static final String ERROR_NAVIGATION_PROCESS = "AI导航推荐异常: ";

    public boolean isQuestionEmpty() {
        return question == null || question.trim().isEmpty();
    }

    public String getChatErrorMessage(Throwable e) {
        return ERROR_CHAT_PROCESS + e.getMessage();
    }

    public String getNavigationErrorMessage(Throwable e) {
        return ERROR_NAVIGATION_PROCESS + e.getMessage();
    }
}
