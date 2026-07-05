package cn.iyque.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * retrieveRagContext 返回值：拼好的 system prompt 前缀 + 每条 [资料 N] 的元数据。
 * 空 prompt / 空 citations 表示 "没做 RAG"（未指定 kid 或检索失败）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagContextResult {
    /** 已拼好的 system prompt 前缀 (含 4 条规则 + [资料 N] 块)，空串代表无 RAG */
    private String prompt;

    /** [资料 N] 到 (fid, docId, kid, docName) 的映射，顺序对齐 idx=1..N */
    private List<RagCitation> citations;

    public static RagContextResult empty() {
        return RagContextResult.builder()
                .prompt("")
                .citations(Collections.emptyList())
                .build();
    }

    public boolean isEmpty() {
        return prompt == null || prompt.isEmpty();
    }
}
