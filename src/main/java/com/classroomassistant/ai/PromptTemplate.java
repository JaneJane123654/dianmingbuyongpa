package com.classroomassistant.ai;

import java.util.Objects;

/**
 * AI 提示词模板 (Prompt Template)
 *
 * <p>负责统一拼装发送给 AI 模型的提示词（Prompt），确保回复风格的一致性。
 * 通过将模板逻辑集中在此类中，可以避免提示词拼接逻辑在业务代码中散落，便于后期维护和针对不同模型进行微调。
 *
 * <p>支持自定义模板和多种预设场景模板。模板中使用 {@code %s} 或 {@code {{content}}} 作为内容占位符。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class PromptTemplate {

    private static final String DEFAULT_TEMPLATE = "你是课堂助手，请根据以下课堂内容给出简洁回答：\n\n%s";

    /**
     * 预设的模板场景
     */
    public enum Scenario {
        /** 默认课堂助手场景 */
        CLASSROOM_ASSISTANT("你是课堂助手，请根据以下课堂内容给出简洁回答：\n\n%s"),
        /** 课堂笔记整理场景 */
        NOTE_ORGANIZER("请将以下课堂内容整理成结构化的笔记，使用清晰的层次结构：\n\n%s"),
        /** 问答提取场景 */
        QA_EXTRACTOR("请从以下课堂内容中提取关键问题和答案：\n\n%s"),
        /** 课堂总结场景 */
        SUMMARIZER("请简要总结以下课堂内容的要点（不超过200字）：\n\n%s"),
        /** 知识点解释场景 */
        EXPLAINER("请详细解释以下课堂内容中涉及的知识点：\n\n%s");

        private final String template;

        Scenario(String template) {
            this.template = template;
        }

        public String getTemplate() {
            return template;
        }
    }

    private String template;

    /**
     * 使用默认模板创建实例
     */
    public PromptTemplate() {
        this.template = DEFAULT_TEMPLATE;
    }

    /**
     * 使用自定义模板创建实例
     *
     * @param customTemplate 自定义模板字符串，需包含 %s 或 {{content}} 占位符
     */
    public PromptTemplate(String customTemplate) {
        setTemplate(customTemplate);
    }

    /**
     * 使用预设场景创建实例
     *
     * @param scenario 预设的模板场景
     */
    public PromptTemplate(Scenario scenario) {
        this.template = scenario != null ? scenario.getTemplate() : DEFAULT_TEMPLATE;
    }

    /**
     * 设置自定义模板
     *
     * @param customTemplate 自定义模板字符串，需包含 %s 或 {{content}} 占位符
     * @return 当前实例（支持链式调用）
     */
    public PromptTemplate setTemplate(String customTemplate) {
        if (customTemplate == null || customTemplate.isBlank()) {
            this.template = DEFAULT_TEMPLATE;
        } else {
            // 支持 {{content}} 格式的占位符
            this.template = customTemplate.replace("{{content}}", "%s");
        }
        return this;
    }

    /**
     * 设置预设场景模板
     *
     * @param scenario 预设的模板场景
     * @return 当前实例（支持链式调用）
     */
    public PromptTemplate setScenario(Scenario scenario) {
        this.template = scenario != null ? scenario.getTemplate() : DEFAULT_TEMPLATE;
        return this;
    }

    /**
     * 获取当前使用的模板
     *
     * @return 模板字符串
     */
    public String getTemplate() {
        return template;
    }

    /**
     * 构建最终的 Prompt 字符串
     *
     * @param lectureText 原始课堂文本内容（如 ASR 识别出的文字）
     * @return 拼装后的提示词文本。如果输入为 null，则返回包含空内容的模板。
     */
    public String build(String lectureText) {
        String content = Objects.requireNonNullElse(lectureText, "").trim();
        return String.format(template, content);
    }

    /**
     * 使用指定场景构建 Prompt
     *
     * @param lectureText 原始课堂文本内容
     * @param scenario    要使用的场景模板
     * @return 拼装后的提示词文本
     */
    public String build(String lectureText, Scenario scenario) {
        String content = Objects.requireNonNullElse(lectureText, "").trim();
        String targetTemplate = scenario != null ? scenario.getTemplate() : template;
        return String.format(targetTemplate, content);
    }
}
