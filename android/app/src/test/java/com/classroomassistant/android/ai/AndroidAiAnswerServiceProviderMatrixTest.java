package com.classroomassistant.android.ai;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class AndroidAiAnswerServiceProviderMatrixTest {

    @Test
    public void defaultBaseUrlFor_matchesProviderMatrix() throws Exception {
        AndroidAiAnswerService service = new AndroidAiAnswerService();

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("OPENAI", "https://api.openai.com");
        expected.put("OPENAI_COMPATIBLE", "");
        expected.put("ANTHROPIC", "https://api.anthropic.com/v1");
        expected.put("GEMINI", "https://generativelanguage.googleapis.com/v1beta/openai");
        expected.put("DEEPSEEK", "https://api.deepseek.com");
        expected.put("QIANFAN", "");
        expected.put("KIMI", "https://api.moonshot.cn/v1");
        expected.put("DASHSCOPE", "https://dashscope.aliyuncs.com/compatible-mode/v1");
        expected.put("HUNYUAN", "https://api.hunyuan.cloud.tencent.com/v1");
        expected.put("ZHIPU", "https://open.bigmodel.cn/api/paas/v4");
        expected.put("SILICONFLOW", "https://api.siliconflow.cn/v1");
        expected.put("MINIMAX", "https://api.minimax.chat/v1");
        expected.put("MISTRAL", "https://api.mistral.ai/v1");
        expected.put("GROQ", "https://api.groq.com/openai/v1");
        expected.put("COHERE", "https://api.cohere.ai/compatibility/v1");
        expected.put("OPENROUTER", "https://openrouter.ai/api/v1");
        expected.put("AZURE_OPENAI", "");
        expected.put("BAICHUAN", "https://api.baichuan-ai.com/v1");
        expected.put("YI", "https://api.lingyiwanwu.com/v1");
        expected.put("STEPFUN", "https://api.stepfun.com/v1");
        expected.put("XAI", "https://api.x.ai/v1");
        expected.put("FIREWORKS", "https://api.fireworks.ai/inference/v1");
        expected.put("TOGETHER_AI", "https://api.together.xyz/v1");
        expected.put("PERPLEXITY", "https://api.perplexity.ai");
        expected.put("NOVITA", "https://api.novita.ai/v3/openai");
        expected.put("REPLICATE", "https://api.replicate.com/v1");
        expected.put("CEREBRAS", "https://api.cerebras.ai/v1");
        expected.put("SAMBANOVA", "https://api.sambanova.ai/v1");
        expected.put("OLLAMA", "http://127.0.0.1:11434/v1");
        expected.put("LMSTUDIO", "http://127.0.0.1:1234/v1");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String actual = invokeString(service, "defaultBaseUrlFor", entry.getKey());
            assertEquals("provider=" + entry.getKey(), entry.getValue(), actual);
        }
    }

    @Test
    public void resolveModelName_returnsExpectedDefaults() throws Exception {
        AndroidAiAnswerService service = new AndroidAiAnswerService();

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("OPENAI", "gpt-4o-mini");
        expected.put("OPENAI_COMPATIBLE", "gpt-4o-mini");
        expected.put("ANTHROPIC", "claude-3-5-sonnet-20241022");
        expected.put("GEMINI", "gemini-2.0-flash");
        expected.put("DEEPSEEK", "deepseek-chat");
        expected.put("QIANFAN", "ernie-4.0-8k");
        expected.put("KIMI", "moonshot-v1-8k");
        expected.put("DASHSCOPE", "qwen-plus");
        expected.put("HUNYUAN", "hunyuan-lite");
        expected.put("ZHIPU", "glm-4-flash");
        expected.put("SILICONFLOW", "Qwen/Qwen2.5-7B-Instruct");
        expected.put("MINIMAX", "abab6.5s-chat");
        expected.put("MISTRAL", "mistral-small-latest");
        expected.put("GROQ", "llama-3.3-70b-versatile");
        expected.put("COHERE", "command-r-plus");
        expected.put("OPENROUTER", "openai/gpt-4o-mini");
        expected.put("AZURE_OPENAI", "gpt-4o-mini");
        expected.put("BAICHUAN", "Baichuan4");
        expected.put("YI", "yi-large");
        expected.put("STEPFUN", "step-2");
        expected.put("XAI", "grok-2-1212");
        expected.put("FIREWORKS", "accounts/fireworks/models/llama-v3p1-70b-instruct");
        expected.put("TOGETHER_AI", "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo");
        expected.put("PERPLEXITY", "sonar");
        expected.put("NOVITA", "meta-llama/llama-3.1-70b-instruct");
        expected.put("REPLICATE", "meta/meta-llama-3-70b-instruct");
        expected.put("CEREBRAS", "llama3.1-8b");
        expected.put("SAMBANOVA", "Meta-Llama-3.1-70B-Instruct");
        expected.put("OLLAMA", "qwen2.5:7b");
        expected.put("LMSTUDIO", "local-model");

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String actual = invokeString(service, "resolveModelName", entry.getKey(), "");
            assertEquals("provider=" + entry.getKey(), entry.getValue(), actual);
        }
    }

    @Test
    public void resolveModelListEndpoint_normalizesBaseAndFallbacks() throws Exception {
        AndroidAiAnswerService service = new AndroidAiAnswerService();

        assertEquals(
                "https://proxy.example.com/v1/models",
                invokeString(service, "resolveModelListEndpoint", "OPENAI_COMPATIBLE", "https://proxy.example.com/v1/")
        );
        assertEquals(
                "https://api.deepseek.com/v1/models",
                invokeString(service, "resolveModelListEndpoint", "DEEPSEEK", "")
        );
        assertEquals(
                "",
                invokeString(service, "resolveModelListEndpoint", "QIANFAN", "https://qianfan.baidubce.com")
        );
    }

    @Test
    public void resolveModelListAuthHeader_matchesProviderProtocol() throws Exception {
        AndroidAiAnswerService service = new AndroidAiAnswerService();

        assertEquals("x-api-key", invokeString(service, "resolveModelListAuthHeaderName", "ANTHROPIC"));
        assertEquals("api-key", invokeString(service, "resolveModelListAuthHeaderName", "AZURE_OPENAI"));
        assertEquals("Authorization", invokeString(service, "resolveModelListAuthHeaderName", "OPENAI"));

        assertEquals("Token abc", invokeString(service, "resolveModelListAuthHeaderValue", "REPLICATE", "abc"));
        assertEquals("abc", invokeString(service, "resolveModelListAuthHeaderValue", "ANTHROPIC", "abc"));
        assertEquals("Bearer abc", invokeString(service, "resolveModelListAuthHeaderValue", "OPENAI", "abc"));
    }

    private static String invokeString(AndroidAiAnswerService service, String methodName, String... args)
            throws Exception {
        Class<?>[] signature = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            signature[i] = String.class;
        }
        Method method = AndroidAiAnswerService.class.getDeclaredMethod(methodName, signature);
        method.setAccessible(true);
        Object value = method.invoke(service, (Object[]) args);
        return value == null ? "" : value.toString();
    }
}
