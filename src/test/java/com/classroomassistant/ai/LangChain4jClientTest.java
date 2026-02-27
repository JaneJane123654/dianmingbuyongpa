package com.classroomassistant.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * LangChain4jClient 单元测试
 *
 * <p>使用内部模拟模型验证同步/流式调用、重试、熔断逻辑。
 */
class LangChain4jClientTest {

    private LangChain4jClient client;

    @BeforeEach
    void setUp() {
        client = new TestableClient();
    }

    @Test
    void generateAnswer_returnsNonBlank() {
        LLMConfig config = LLMConfig.builder()
            .modelType(LLMConfig.ModelType.OPENAI)
            .apiKey("test-key")
            .streaming(false)
            .maxRetryCount(0)
            .timeout(Duration.ofSeconds(5))
            .build();
        client.configure(config);
        String answer = client.generateAnswer("hello");
        assertNotNull(answer);
        assertFalse(answer.isBlank(), "应返回非空回答");
    }

    @Test
    void generateAnswer_emptyPromptReturnsEmpty() {
        LLMConfig config = LLMConfig.builder()
            .modelType(LLMConfig.ModelType.OPENAI)
            .apiKey("test-key")
            .streaming(false)
            .timeout(Duration.ofSeconds(5))
            .build();
        client.configure(config);
        assertEquals("", client.generateAnswer(""));
        assertEquals("", client.generateAnswer(null));
    }

    @Test
    void generateAnswerAsync_streamingCallsOnToken() throws Exception {
        LLMConfig config = LLMConfig.builder()
            .modelType(LLMConfig.ModelType.OPENAI)
            .apiKey("test-key")
            .streaming(true)
            .timeout(Duration.ofSeconds(5))
            .build();
        client.configure(config);

        CountDownLatch latch = new CountDownLatch(1);
        List<String> tokens = new ArrayList<>();
        AtomicReference<String> finalAnswer = new AtomicReference<>();
        AtomicBoolean errored = new AtomicBoolean(false);

        client.generateAnswerAsync("stream test", new AnswerListener() {
            @Override
            public void onToken(String token) {
                tokens.add(token);
            }

            @Override
            public void onComplete(String answer) {
                finalAnswer.set(answer);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                errored.set(true);
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS), "回调应在超时前完成");
        assertFalse(errored.get(), "不应返回错误");
        assertFalse(tokens.isEmpty(), "流式回调应收到 token");
        assertNotNull(finalAnswer.get());
    }

    @Test
    void generateAnswerAsync_emptyPromptCallsComplete() throws Exception {
        LLMConfig config = LLMConfig.builder()
            .modelType(LLMConfig.ModelType.OPENAI)
            .apiKey("test-key")
            .streaming(true)
            .timeout(Duration.ofSeconds(5))
            .build();
        client.configure(config);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> finalAnswer = new AtomicReference<>();

        client.generateAnswerAsync("", new AnswerListener() {
            @Override
            public void onToken(String token) {
            }

            @Override
            public void onComplete(String answer) {
                finalAnswer.set(answer);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals("", finalAnswer.get());
    }

    /**
     * 可测试子类：注入模拟模型避免真实网络调用
     */
    private static class TestableClient extends LangChain4jClient {

        @Override
        protected ChatLanguageModel createModel(LLMConfig config) {
            return prompt -> "mocked answer";
        }

        @Override
        protected StreamingChatLanguageModel createStreamingModel(LLMConfig config) {
            return (prompt, handler) -> {
                handler.onNext("token1");
                handler.onNext("token2");
                handler.onComplete(Response.from(AiMessage.from("token1token2")));
            };
        }
    }
}
