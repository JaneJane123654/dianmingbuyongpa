package com.classroomassistant.desktop.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;

class OpenAiModelCatalogServiceTest {

    @Test
    void normalizeModelsEndpointSupportsBaseWithoutV1() {
        String endpoint = OpenAiModelCatalogService.normalizeModelsEndpoint("https://api.openai.com");
        assertEquals("https://api.openai.com/v1/models", endpoint);
    }

    @Test
    void normalizeModelsEndpointSupportsBaseWithV1AndTrailingSlash() {
        String endpoint = OpenAiModelCatalogService.normalizeModelsEndpoint("https://api.deepseek.com/v1/");
        assertEquals("https://api.deepseek.com/v1/models", endpoint);
    }

    @Test
    void parseModelNamesExtractsDistinctIdsInOrder() {
        String json = "{\"object\":\"list\",\"data\":["
                + "{\"id\":\"gpt-4o-mini\"},"
                + "{\"id\":\"deepseek-chat\"},"
                + "{\"id\":\"gpt-4o-mini\"}]}";

        List<String> models = OpenAiModelCatalogService.parseModelNames(json);
        assertEquals(List.of("gpt-4o-mini", "deepseek-chat"), models);
    }

    @Test
    void parseModelNamesHandlesEscapedQuotes() {
        String json = "{\"data\":[{\"id\":\"model-\\\"x\"}]}";

        List<String> models = OpenAiModelCatalogService.parseModelNames(json);
        assertEquals(List.of("model-\"x"), models);
    }

    @Test
    void parseModelNamesHandlesInvalidPayload() {
        List<String> models = OpenAiModelCatalogService.parseModelNames("not-json");
        assertTrue(models.isEmpty());
    }

    @Test
    void applyAuthHeaderUsesProviderSpecificHeaders() throws Exception {
        OpenAiModelCatalogService service = new OpenAiModelCatalogService();
        var method = OpenAiModelCatalogService.class.getDeclaredMethod(
                "applyAuthHeader",
                HttpRequest.Builder.class,
                String.class,
                String.class);
        method.setAccessible(true);

        HttpRequest.Builder anthropicBuilder = HttpRequest.newBuilder();
        method.invoke(service, anthropicBuilder, "ANTHROPIC", "anthropic-key");
        HttpRequest anthropicRequest = anthropicBuilder.uri(java.net.URI.create("https://example.com")).build();
        assertEquals("anthropic-key", anthropicRequest.headers().firstValue("x-api-key").orElse(""));
        assertEquals("2023-06-01", anthropicRequest.headers().firstValue("anthropic-version").orElse(""));

        HttpRequest.Builder azureBuilder = HttpRequest.newBuilder();
        method.invoke(service, azureBuilder, "AZURE_OPENAI", "azure-key");
        HttpRequest azureRequest = azureBuilder.uri(java.net.URI.create("https://example.com")).build();
        assertEquals("azure-key", azureRequest.headers().firstValue("api-key").orElse(""));

        HttpRequest.Builder replicateBuilder = HttpRequest.newBuilder();
        method.invoke(service, replicateBuilder, "REPLICATE", "replicate-key");
        HttpRequest replicateRequest = replicateBuilder.uri(java.net.URI.create("https://example.com")).build();
        assertEquals("Token replicate-key", replicateRequest.headers().firstValue("Authorization").orElse(""));

        HttpRequest.Builder openAiBuilder = HttpRequest.newBuilder();
        method.invoke(service, openAiBuilder, "OPENAI", "openai-key");
        HttpRequest openAiRequest = openAiBuilder.uri(java.net.URI.create("https://example.com")).build();
        assertEquals("Bearer openai-key", openAiRequest.headers().firstValue("Authorization").orElse(""));
    }

    @Test
    void buildHttpErrorUsesHelpfulMessageFor401() throws Exception {
        OpenAiModelCatalogService service = new OpenAiModelCatalogService();
        var method = OpenAiModelCatalogService.class.getDeclaredMethod(
                "buildHttpError",
                String.class,
                int.class);
        method.setAccessible(true);

        IllegalStateException ex = (IllegalStateException) method.invoke(service, "ANTHROPIC", 401);
        assertTrue(ex.getMessage().contains("HTTP 401"));
        assertTrue(ex.getMessage().contains("ANTHROPIC"));
    }
}
