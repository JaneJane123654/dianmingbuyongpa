package com.classroomassistant.ai;

import java.util.Objects;

/**
 * Prompt 模板
 *
 * <p>统一拼装 AI 问答提示词，避免在业务层散落拼接逻辑。
 */
public class PromptTemplate {

    private static final String TEMPLATE = "你是课堂助手，请根据以下课堂内容给出简洁回答：\n\n%s";

    /**
     * 构建 Prompt
     *
     * @param lectureText 课堂文本
     * @return prompt
     */
    public String build(String lectureText) {
        String content = Objects.requireNonNullElse(lectureText, "").trim();
        return String.format(TEMPLATE, content);
    }
}
