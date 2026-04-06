package com.classroomassistant.desktop.ai;

import com.classroomassistant.core.ai.PromptTemplate;
import com.classroomassistant.desktop.session.DesktopSettingsSnapshot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.qianfan.QianfanChatModel;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 桌面端 AI 回答服务。
 */
public class DesktopAiAnswerService {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .callTimeout(3, TimeUnit.MINUTES)
            .build();

    private final String defaultSystemPrompt = "你是课堂助手，请用简洁清晰的语言回答问题。";

    public String generateAnswer(DesktopSettingsSnapshot settings, String recognitionText, Consumer<String> onLog) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(onLog, "onLog");

        String question = recognitionText == null ? "" : recognitionText.trim();
        if (question.isBlank()) {
            onLog.accept("AI问答跳过：问题为空");
            return "";
        }
        if (settings.getAiToken().isBlank()) {
            onLog.accept("AI问答跳过：未配置 API Token");
            return "";
        }

        String prompt = PromptTemplate.buildPrompt(question);
        String provider = normalizeProvider(settings.getAiProvider());
        String modelName = resolveModelName(provider, settings.getAiModelName());

        try {
            ChatLanguageModel model = buildLangChainModel(provider,
                    modelName,
                    settings.getAiBaseUrl(),
                    settings.getAiToken(),
                    settings.getAiSecretKey());
            String content = model.generate(defaultSystemPrompt + "\n\n" + prompt);
            String result = content == null ? "" : content.trim();
            if (!result.isBlank()) {
                onLog.accept("AI问答完成: " + truncate(result, 120));
                return result;
            }
        } catch (Throwable error) {
            onLog.accept("AI问答(LangChain)异常: " + simplifyError(error));
        }

        return generateByHttp(provider,
                modelName,
                settings.getAiBaseUrl(),
                settings.getAiToken(),
                prompt,
                onLog);
    }

    private ChatLanguageModel buildLangChainModel(String provider,
            String modelName,
            String baseUrl,
            String apiToken,
            String apiSecretKey) {
        if ("QIANFAN".equals(provider) && !apiSecretKey.isBlank()) {
            return QianfanChatModel.builder()
                    .apiKey(apiToken)
                    .secretKey(apiSecretKey)
                    .modelName(modelName)
                    .build();
        }

        String resolvedBaseUrl = normalizeBaseUrl(baseUrl);
        if (resolvedBaseUrl.isBlank()) {
            resolvedBaseUrl = defaultBaseUrlFor(provider);
        }
        if (resolvedBaseUrl.isBlank()) {
            resolvedBaseUrl = "https://api.openai.com";
        }
        return OpenAiChatModel.builder()
                .baseUrl(resolvedBaseUrl)
                .apiKey(apiToken)
                .modelName(modelName)
                .build();
    }

    private String generateByHttp(String provider,
            String modelName,
            String baseUrl,
            String apiToken,
            String prompt,
            Consumer<String> onLog) {
        String endpoint = resolveChatEndpoint(provider, baseUrl);

        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.addProperty("temperature", 0.2);
        body.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", defaultSystemPrompt);
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);

        body.add("messages", messages);

        Request request = new Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer " + apiToken)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        long startedAt = System.currentTimeMillis();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            String payload = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                onLog.accept("AI问答失败: provider=" + provider + ", http=" + response.code() + ", body=" + truncate(payload, 280));
                return "";
            }
            String content = extractContent(payload);
            if (content.isBlank()) {
                onLog.accept("AI问答返回空内容");
                return "";
            }
            onLog.accept("AI问答完成(" + (System.currentTimeMillis() - startedAt) + "ms): " + truncate(content, 120));
            return content;
        } catch (Exception error) {
            onLog.accept("AI问答异常: " + simplifyError(error));
            return "";
        }
    }

    private String extractContent(String payload) {
        try {
            JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return "";
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null || !message.has("content") || message.get("content").isJsonNull()) {
                return "";
            }
            return message.get("content").getAsString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String resolveChatEndpoint(String provider, String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (!normalized.isBlank() && !"QIANFAN".equals(provider)) {
            return appendPath(normalized, "/v1/chat/completions");
        }

        switch (provider) {
            case "DEEPSEEK":
                return "https://api.deepseek.com/v1/chat/completions";
            case "KIMI":
                return "https://api.moonshot.cn/v1/chat/completions";
            case "QIANFAN":
                return "https://qianfan.baidubce.com/v2/chat/completions";
            default:
                return appendPath(defaultBaseUrlFor(provider), "/v1/chat/completions");
        }
    }

    private String defaultBaseUrlFor(String provider) {
        switch (provider) {
            case "OPENAI":
                return "https://api.openai.com";
            case "DEEPSEEK":
                return "https://api.deepseek.com";
            case "KIMI":
                return "https://api.moonshot.cn/v1";
            case "QIANFAN":
                return "";
            case "DASHSCOPE":
                return "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "GROQ":
                return "https://api.groq.com/openai/v1";
            case "OLLAMA":
                return "http://127.0.0.1:11434/v1";
            case "LMSTUDIO":
                return "http://127.0.0.1:1234/v1";
            default:
                return "https://api.openai.com";
        }
    }

    private String resolveModelName(String provider, String configured) {
        if (configured != null && !configured.trim().isBlank()) {
            return configured.trim();
        }
        switch (provider) {
            case "DEEPSEEK":
                return "deepseek-chat";
            case "KIMI":
                return "moonshot-v1-8k";
            case "QIANFAN":
                return "ernie-4.0-8k";
            case "GROQ":
                return "llama-3.3-70b-versatile";
            case "OLLAMA":
                return "qwen2.5:7b";
            case "LMSTUDIO":
                return "local-model";
            default:
                return "gpt-4o-mini";
        }
    }

    private String normalizeProvider(String rawProvider) {
        if (rawProvider == null || rawProvider.isBlank()) {
            return "OPENAI_COMPATIBLE";
        }
        return rawProvider.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String appendPath(String baseUrl, String suffix) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized.isBlank()) {
            return "https://api.openai.com" + suffix;
        }
        if (suffix.equals("/v1/chat/completions") && normalized.endsWith("/v1")) {
            return normalized + "/chat/completions";
        }
        if (normalized.endsWith(suffix)) {
            return normalized;
        }
        return normalized + suffix;
    }

    private String simplifyError(Throwable error) {
        String message = error.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return error.getClass().getSimpleName();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength)) + "...";
    }
}
