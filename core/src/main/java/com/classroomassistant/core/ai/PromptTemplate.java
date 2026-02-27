package com.classroomassistant.core.ai;

/**
 * 提示词模板
 */
public class PromptTemplate {
    
    public static final String DEFAULT_SYSTEM_PROMPT = 
        "你是一个课堂助手，帮助学生回答老师的提问。请用简洁清晰的语言回答问题。";
    
    public static final String ANSWER_TEMPLATE = 
        "问题：%s\n\n请给出简洁的回答：";
    
    /**
     * 构建完整的提示词
     * @param question 用户问题
     * @return 完整的提示词
     */
    public static String buildPrompt(String question) {
        return String.format(ANSWER_TEMPLATE, question);
    }
    
    /**
     * 构建带上下文的提示词
     * @param context 上下文信息
     * @param question 用户问题
     * @return 完整的提示词
     */
    public static String buildPromptWithContext(String context, String question) {
        return String.format("上下文：%s\n\n问题：%s\n\n请根据上下文回答问题：", context, question);
    }
}
