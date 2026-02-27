package com.classroomassistant.ai;

/**
 * 大语言模型客户端接口 (Large Language Model Client)
 *
 * <p>定义了与大语言模型交互的标准方法，包括同步和异步（流式）生成回答。
 * 具体的模型实现（如 OpenAI、文心一言等）应实现此接口。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public interface LLMClient {

    /**
     * 配置大模型客户端
     *
     * @param config 包含模型类型、API Key、超时时间等配置信息的 {@link LLMConfig} 对象
     */
    void configure(LLMConfig config);

    /**
     * 同步生成回答
     *
     * @param prompt 发送给模型的提示词 (Prompt)
     * @return 模型生成的完整回答内容
     * @throws RuntimeException 如果请求失败或超时
     */
    String generateAnswer(String prompt);

    /**
     * 异步生成回答（流式输出）
     *
     * @param prompt   发送给模型的提示词 (Prompt)
     * @param listener 接收生成结果和状态的监听器 {@link AnswerListener}
     */
    void generateAnswerAsync(String prompt, AnswerListener listener);
}

