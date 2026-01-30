# AI 模块

## 模块描述

AI 模块提供大模型调用入口、Prompt 模板与熔断控制，当前提供占位实现以便工程可运行。

## 类/接口说明

| 类/接口 | 说明 |
| --- | --- |
| `LLMClient` | 模型调用接口 |
| `DefaultLLMClient` | 默认占位实现 |
| `LangChain4jClient` | LangChain4j 实现 |
| `LLMClientFactory` | 客户端工厂 |
| `LLMConfig` | 模型配置 |
| `PromptTemplate` | Prompt 模板 |
| `CircuitBreaker` | 熔断器 |
| `AnswerListener` | 流式回调接口 |

## 注意事项

1. 真实模型接入时替换 `DefaultLLMClient`。
2. Prompt 模板需保持稳定。

## 更新日志

| 版本 | 日期 | 更新内容 |
| --- | --- | --- |
| 1.0 | 2026-01-30 | 初始化 AI 模块骨架 |
| 1.1 | 2026-01-30 | 增加 LangChain4j 客户端实现 |
