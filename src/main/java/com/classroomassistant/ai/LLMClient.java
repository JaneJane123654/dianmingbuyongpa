package com.classroomassistant.ai;

/**
 * 大语言模型客户端接口
 */
public interface LLMClient {

    /**
     * 配置模型
     *
     * @param config 配置
     */
    void configure(LLMConfig config);

    /**
     * 同步生成回答
     *
     * @param prompt Prompt
     * @return 完整回答
     */
    String generateAnswer(String prompt);

    /**
     * 异步生成回答（流式）
     *
     * @param prompt Prompt
     * @param listener 监听器
     */
    void generateAnswerAsync(String prompt, AnswerListener listener);
}

