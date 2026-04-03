package com.classroomassistant.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAI 兼容接口模型目录服务。
 *
 * <p>通过调用 `{baseUrl}/v1/models` 获取可用模型列表，用于设置界面的动态推荐。
 */
public class OpenAiModelCatalogService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(12);
    private static final Pattern MODEL_ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();

    /**
     * 拉取模型列表。
     *
     * @param baseUrl 形如 `https://api.openai.com` 或 `https://xxx/v1`
     * @param apiKey  API Key，可为空
     * @return 模型 ID 列表（已去重并按字母序排序）
     */
    public List<String> fetchModelNames(LLMConfig.ModelType provider, String baseUrl, String apiKey) {
        LLMConfig.ModelType resolvedProvider = provider == null ? LLMConfig.ModelType.OPENAI_COMPATIBLE : provider;
        String endpoint = normalizeModelsEndpoint(baseUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
            .GET()
            .timeout(REQUEST_TIMEOUT)
            .header("Accept", "application/json");
        applyAuthHeader(builder, resolvedProvider, apiKey);
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw buildHttpError(resolvedProvider, response.statusCode());
            }
            return parseModelNames(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("拉取模型失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("拉取模型被中断", e);
        }
    }

    static String normalizeModelsEndpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Base URL 不能为空");
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/models";
        }
        return normalized + "/v1/models";
    }

    static List<String> parseModelNames(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Matcher matcher = MODEL_ID_PATTERN.matcher(jsonText);
        while (matcher.find()) {
            String modelId = matcher.group(1);
            if (modelId != null) {
                String normalized = modelId.trim();
                if (!normalized.isEmpty()) {
                    result.add(normalized);
                }
            }
        }
        return result.stream().distinct().sorted().collect(Collectors.toList());
    }

    private void applyAuthHeader(HttpRequest.Builder builder, LLMConfig.ModelType provider, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        String token = apiKey.trim();
        switch (provider) {
            case ANTHROPIC:
                builder.header("x-api-key", token);
                builder.header("anthropic-version", "2023-06-01");
                return;
            case AZURE_OPENAI:
                builder.header("api-key", token);
                return;
            case REPLICATE:
                builder.header("Authorization", "Token " + token);
                return;
            default:
                builder.header("Authorization", "Bearer " + token);
        }
    }

    private IllegalStateException buildHttpError(LLMConfig.ModelType provider, int statusCode) {
        if (statusCode == 401) {
            return new IllegalStateException(
                    "HTTP 401：鉴权失败，请检查当前平台（" + provider.name() + "）对应的 API Token / Key");
        }
        return new IllegalStateException("拉取模型失败，HTTP " + statusCode);
    }
}
