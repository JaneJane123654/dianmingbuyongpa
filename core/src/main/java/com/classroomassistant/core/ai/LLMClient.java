package com.classroomassistant.core.ai;

/**
 * 大语言模型客户端接口
 * 
 * 定义了与大语言模型交互的标准方法，包括同步和异步（流式）生成回答。
 */
public interface LLMClient {

    /**
     * 配置大模型客户端
     * @param config 包含模型类型、API Key、超时时间等配置信息
     */
    void configure(LLMConfig config);

    /**
     * 同步生成回答
     * @param prompt 发送给模型的提示词
     * @return 模型生成的完整回答内容
     * @throws RuntimeException 如果请求失败或超时
     */
    String generateAnswer(String prompt);

    /**
     * 异步生成回答（流式输出）
     * @param prompt 发送给模型的提示词
     * @param listener 接收生成结果和状态的监听器
     */
    void generateAnswerAsync(String prompt, AnswerListener listener);
}
