package com.classroomassistant.desktop.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * OpenAI 兼容接口模型目录服务（桌面端）。
 *
 * <p>通过调用 `{baseUrl}/v1/models` 获取可用模型列表，用于设置界面的动态推荐。
 */
public class OpenAiModelCatalogService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(12);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    /**
     * 拉取模型列表。
     *
     * @param baseUrl 形如 `https://api.openai.com` 或 `https://xxx/v1`
     * @param apiKey API Key，可为空
     * @return 模型 ID 列表（去重后保持服务端返回顺序）
     */
    public List<String> fetchModelNames(String provider, String baseUrl, String apiKey) {
        String providerName = provider == null ? "OPENAI_COMPATIBLE" : provider.trim().toUpperCase();
        String endpoint = normalizeModelsEndpoint(baseUrl);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
        applyAuthHeader(builder, providerName, apiKey);
        HttpRequest request = builder.build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw buildHttpError(providerName, response.statusCode());
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
        LinkedHashSet<String> result = new LinkedHashSet<>();
        int length = jsonText.length();
        int fromIndex = 0;
        while (fromIndex < length) {
            int idKeyStart = jsonText.indexOf("\"id\"", fromIndex);
            if (idKeyStart < 0) {
                break;
            }
            int colon = jsonText.indexOf(':', idKeyStart + 4);
            if (colon < 0) {
                break;
            }
            int valueStart = colon + 1;
            while (valueStart < length && Character.isWhitespace(jsonText.charAt(valueStart))) {
                valueStart++;
            }
            if (valueStart >= length || jsonText.charAt(valueStart) != '"') {
                fromIndex = colon + 1;
                continue;
            }
            int cursor = valueStart + 1;
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            while (cursor < length) {
                char ch = jsonText.charAt(cursor);
                if (escaped) {
                    sb.append(ch);
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    break;
                } else {
                    sb.append(ch);
                }
                cursor++;
            }
            if (cursor < length && jsonText.charAt(cursor) == '"') {
                String id = sb.toString().trim();
                if (!id.isBlank()) {
                    result.add(id);
                }
                fromIndex = cursor + 1;
            } else {
                break;
            }
        }
        return new ArrayList<>(result);
    }

    private void applyAuthHeader(HttpRequest.Builder builder, String provider, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        String token = apiKey.trim();
        switch (provider) {
            case "ANTHROPIC":
                builder.header("x-api-key", token);
                builder.header("anthropic-version", "2023-06-01");
                return;
            case "AZURE_OPENAI":
                builder.header("api-key", token);
                return;
            case "REPLICATE":
                builder.header("Authorization", "Token " + token);
                return;
            default:
                builder.header("Authorization", "Bearer " + token);
        }
    }

    private IllegalStateException buildHttpError(String provider, int statusCode) {
        if (statusCode == 401) {
            return new IllegalStateException(
                    "HTTP 401：鉴权失败，请检查当前平台（" + provider + "）对应的 API Token / Key");
        }
        return new IllegalStateException("拉取模型失败，HTTP " + statusCode);
    }
}
