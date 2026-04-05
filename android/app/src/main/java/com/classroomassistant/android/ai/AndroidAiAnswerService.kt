package com.classroomassistant.android.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.qianfan.QianfanChatModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class AndroidAiAnswerService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .callTimeout(3, TimeUnit.MINUTES)
        .build()
) {

    private val systemPromptZh = "你是课堂助手，请用简洁清晰的语言回答问题。"
    private val systemPromptEn = "You are a classroom assistant. Answer clearly and concisely in English."

    fun generateAnswer(
        provider: String,
        modelName: String,
        baseUrl: String,
        apiToken: String,
        apiSecretKey: String,
        prompt: String,
        languageCode: String,
        customSystemPrompt: String,
        onLog: (String) -> Unit
    ): String {
        if (apiToken.isBlank()) {
            onLog("AI问答跳过：未配置 API Token")
            return ""
        }
        val normalizedProvider = provider.uppercase()
        if (normalizedProvider == "QIANFAN" && apiSecretKey.isBlank()) {
            return generateAnswerViaHttp(
                provider = provider,
                modelName = modelName,
                baseUrl = baseUrl,
                apiToken = apiToken,
                prompt = prompt,
                languageCode = languageCode,
                customSystemPrompt = customSystemPrompt,
                onLog = onLog
            )
        }
        return generateAnswerViaLangChain4j(
            provider = normalizedProvider,
            modelName = modelName,
            baseUrl = baseUrl,
            apiToken = apiToken,
            apiSecretKey = apiSecretKey,
            prompt = prompt,
            languageCode = languageCode,
            customSystemPrompt = customSystemPrompt,
            onLog = onLog
        )
    }

    private fun generateAnswerViaLangChain4j(
        provider: String,
        modelName: String,
        baseUrl: String,
        apiToken: String,
        apiSecretKey: String,
        prompt: String,
        languageCode: String,
        customSystemPrompt: String,
        onLog: (String) -> Unit
    ): String {
        val resolvedModelName = resolveModelName(provider, modelName)
        val promptText = "${resolveSystemPrompt(languageCode, customSystemPrompt)}\n\n$prompt"
        val model = buildLangChain4jModel(provider, resolvedModelName, baseUrl, apiToken, apiSecretKey)
        return try {
            val content = model.generate(promptText).orEmpty().trim()
            if (content.isBlank()) {
                onLog("AI问答返回空内容: provider=$provider")
                ""
            } else {
                onLog("AI问答完成: ${content.take(120)}")
                content
            }
        } catch (t: Throwable) {
            onLog("AI问答异常: ${t.message ?: t.javaClass.simpleName}")
            ""
        }
    }

    private fun buildLangChain4jModel(
        provider: String,
        modelName: String,
        baseUrl: String,
        apiToken: String,
        apiSecretKey: String
    ): ChatLanguageModel {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        val defaultBaseUrl = defaultBaseUrlFor(provider)
        val resolvedBaseUrl = if (normalizedBaseUrl.isBlank()) defaultBaseUrl else normalizedBaseUrl
        return when (provider.uppercase()) {
            "QIANFAN" -> QianfanChatModel.builder()
                .apiKey(apiToken)
                .secretKey(apiSecretKey)
                .modelName(modelName)
                .build()
            else -> OpenAiChatModel.builder()
                .baseUrl(if (resolvedBaseUrl.isBlank()) "https://api.openai.com" else resolvedBaseUrl)
                .apiKey(apiToken)
                .modelName(modelName)
                .build()
        }
    }

    private fun generateAnswerViaHttp(
        provider: String,
        modelName: String,
        baseUrl: String,
        apiToken: String,
        prompt: String,
        languageCode: String,
        customSystemPrompt: String,
        onLog: (String) -> Unit
    ): String {
        val endpoint = resolveEndpoint(provider, baseUrl)
        val resolvedModelName = resolveModelName(provider, modelName)

        val body = JsonObject().apply {
            addProperty("model", resolvedModelName)
            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", resolveSystemPrompt(languageCode, customSystemPrompt))
                })
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                })
            })
            addProperty("temperature", 0.2)
            addProperty("stream", false)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiToken")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                onLog("AI问答失败: provider=$provider, http=${response.code}, body=${responseText.take(300)}")
                return ""
            }
            val content = extractContent(responseText)
            if (content.isBlank()) {
                onLog("AI问答返回空内容: provider=$provider")
                return ""
            }
            onLog("AI问答完成: ${content.take(120)}")
            return content
        }
    }

    fun fetchModelNames(
        provider: String,
        baseUrl: String,
        apiToken: String,
        onLog: (String) -> Unit
    ): List<String> {
        val endpoint = resolveModelListEndpoint(provider, baseUrl)
        if (endpoint.isBlank()) {
            throw IllegalArgumentException("请先填写 Base URL")
        }
        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Accept", "application/json")
            .get()
        if (apiToken.isNotBlank()) {
            val normalizedProvider = provider.uppercase()
            val token = apiToken.trim()
            requestBuilder.addHeader(
                resolveModelListAuthHeaderName(normalizedProvider),
                resolveModelListAuthHeaderValue(normalizedProvider, token)
            )
            if (normalizedProvider == "ANTHROPIC") {
                requestBuilder.addHeader("anthropic-version", "2023-06-01")
            }
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                onLog("模型列表拉取失败: provider=$provider, http=${response.code}")
                if (response.code == 401) {
                    throw IllegalStateException(
                        "HTTP 401：鉴权失败，请检查当前平台（${provider.uppercase()}）对应的 API Token / Key"
                    )
                }
                throw IllegalStateException("HTTP ${response.code}")
            }
            val models = extractModelNames(responseText)
            onLog("模型列表拉取成功: provider=$provider, count=${models.size}")
            return models
        }
    }

    private fun extractContent(jsonText: String): String {
        return runCatching {
            val root = JsonParser.parseString(jsonText).asJsonObject
            val choices = root.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) {
                return@runCatching ""
            }
            val first = choices[0].asJsonObject
            val message = first.getAsJsonObject("message")
            message?.get("content")?.asString?.trim().orEmpty()
        }.getOrDefault("")
    }

    private fun resolveEndpoint(provider: String, baseUrl: String): String {
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        val defaultBaseUrl = defaultBaseUrlFor(provider)
        if (normalizedBaseUrl.isNotBlank() && provider.uppercase() != "QIANFAN") {
            return appendPath(normalizedBaseUrl, "/v1/chat/completions")
        }
        return when (provider.uppercase()) {
            "OPENAI" -> "https://api.openai.com/v1/chat/completions"
            "DEEPSEEK" -> "https://api.deepseek.com/v1/chat/completions"
            "KIMI" -> "https://api.moonshot.cn/v1/chat/completions"
            "QIANFAN" -> "https://qianfan.baidubce.com/v2/chat/completions"
            else -> appendPath(if (defaultBaseUrl.isBlank()) "https://api.openai.com" else defaultBaseUrl, "/v1/chat/completions")
        }
    }

    private fun resolveModelListEndpoint(provider: String, baseUrl: String): String {
        val normalizedProvider = provider.uppercase()
        val normalizedBaseUrl = normalizeBaseUrl(baseUrl)
        if (normalizedProvider == "QIANFAN") {
            return ""
        }
        if (normalizedBaseUrl.isNotBlank()) {
            return appendPath(normalizedBaseUrl, "/v1/models")
        }
        val defaultBaseUrl = defaultBaseUrlFor(normalizedProvider)
        return if (defaultBaseUrl.isBlank()) "" else appendPath(defaultBaseUrl, "/v1/models")
    }

    private fun extractModelNames(jsonText: String): List<String> {
        return runCatching {
            val root = JsonParser.parseString(jsonText)
            val result = LinkedHashSet<String>()
            if (root.isJsonObject) {
                val obj = root.asJsonObject
                collectModelNames(obj.get("data"), result)
                collectModelNames(obj.get("models"), result)
            } else if (root.isJsonArray) {
                collectModelNames(root.asJsonArray, result)
            }
            result.toList()
        }.getOrDefault(emptyList())
    }

    private fun collectModelNames(element: com.google.gson.JsonElement?, output: LinkedHashSet<String>) {
        if (element == null || element.isJsonNull) {
            return
        }
        if (element.isJsonArray) {
            collectModelNames(element.asJsonArray, output)
            return
        }
        if (element.isJsonObject) {
            readModelName(element.asJsonObject)?.let { output.add(it) }
            return
        }
        if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            val value = element.asString.trim()
            if (value.isNotBlank()) {
                output.add(value)
            }
        }
    }

    private fun collectModelNames(array: JsonArray, output: LinkedHashSet<String>) {
        array.forEach { item ->
            collectModelNames(item, output)
        }
    }

    private fun readModelName(obj: JsonObject): String? {
        val keys = arrayOf("id", "name", "model")
        for (key in keys) {
            val value = obj.get(key)
            if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                val text = value.asString.trim()
                if (text.isNotBlank()) {
                    return text
                }
            }
        }
        return null
    }

    private fun normalizeBaseUrl(raw: String): String {
        var normalized = raw.trim()
        while (normalized.endsWith("/")) {
            normalized = normalized.dropLast(1)
        }
        return normalized
    }

    private fun resolveModelListAuthHeaderName(provider: String): String {
        return when (provider.uppercase()) {
            "ANTHROPIC" -> "x-api-key"
            "AZURE_OPENAI" -> "api-key"
            else -> "Authorization"
        }
    }

    private fun resolveModelListAuthHeaderValue(provider: String, token: String): String {
        return when (provider.uppercase()) {
            "REPLICATE" -> "Token $token"
            "ANTHROPIC", "AZURE_OPENAI" -> token
            else -> "Bearer $token"
        }
    }

    private fun appendPath(baseUrl: String, suffix: String): String {
        if (baseUrl.isBlank()) {
            return ""
        }
        if (suffix == "/v1/models" && baseUrl.endsWith("/v1")) {
            return "$baseUrl/models"
        }
        if (suffix == "/v1/chat/completions" && baseUrl.endsWith("/v1")) {
            return "$baseUrl/chat/completions"
        }
        if (baseUrl.endsWith(suffix)) {
            return baseUrl
        }
        return baseUrl + suffix
    }

    private fun resolveModelName(provider: String, configured: String): String {
        if (configured.isNotBlank()) {
            return configured
        }
        return when (provider.uppercase()) {
            "OPENAI" -> "gpt-4o-mini"
            "OPENAI_COMPATIBLE" -> "gpt-4o-mini"
            "ANTHROPIC" -> "claude-3-5-sonnet-20241022"
            "GEMINI" -> "gemini-2.0-flash"
            "DEEPSEEK" -> "deepseek-chat"
            "KIMI" -> "moonshot-v1-8k"
            "QIANFAN" -> "ernie-4.0-8k"
            "DASHSCOPE" -> "qwen-plus"
            "HUNYUAN" -> "hunyuan-lite"
            "ZHIPU" -> "glm-4-flash"
            "SILICONFLOW" -> "Qwen/Qwen2.5-7B-Instruct"
            "MINIMAX" -> "abab6.5s-chat"
            "MISTRAL" -> "mistral-small-latest"
            "GROQ" -> "llama-3.3-70b-versatile"
            "COHERE" -> "command-r-plus"
            "OPENROUTER" -> "openai/gpt-4o-mini"
            "AZURE_OPENAI" -> "gpt-4o-mini"
            "BAICHUAN" -> "Baichuan4"
            "YI" -> "yi-large"
            "STEPFUN" -> "step-2"
            "XAI" -> "grok-2-1212"
            "FIREWORKS" -> "accounts/fireworks/models/llama-v3p1-70b-instruct"
            "TOGETHER_AI" -> "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo"
            "PERPLEXITY" -> "sonar"
            "NOVITA" -> "meta-llama/llama-3.1-70b-instruct"
            "REPLICATE" -> "meta/meta-llama-3-70b-instruct"
            "CEREBRAS" -> "llama3.1-8b"
            "SAMBANOVA" -> "Meta-Llama-3.1-70B-Instruct"
            "OLLAMA" -> "qwen2.5:7b"
            "LMSTUDIO" -> "local-model"
            else -> "gpt-4o-mini"
        }
    }

    private fun defaultBaseUrlFor(provider: String): String {
        return when (provider.uppercase()) {
            "OPENAI" -> "https://api.openai.com"
            "OPENAI_COMPATIBLE" -> ""
            "ANTHROPIC" -> "https://api.anthropic.com/v1"
            "GEMINI" -> "https://generativelanguage.googleapis.com/v1beta/openai"
            "DEEPSEEK" -> "https://api.deepseek.com"
            "QIANFAN" -> ""
            "KIMI" -> "https://api.moonshot.cn/v1"
            "DASHSCOPE" -> "https://dashscope.aliyuncs.com/compatible-mode/v1"
            "HUNYUAN" -> "https://api.hunyuan.cloud.tencent.com/v1"
            "ZHIPU" -> "https://open.bigmodel.cn/api/paas/v4"
            "SILICONFLOW" -> "https://api.siliconflow.cn/v1"
            "MINIMAX" -> "https://api.minimax.chat/v1"
            "MISTRAL" -> "https://api.mistral.ai/v1"
            "GROQ" -> "https://api.groq.com/openai/v1"
            "COHERE" -> "https://api.cohere.ai/compatibility/v1"
            "OPENROUTER" -> "https://openrouter.ai/api/v1"
            "AZURE_OPENAI" -> ""
            "BAICHUAN" -> "https://api.baichuan-ai.com/v1"
            "YI" -> "https://api.lingyiwanwu.com/v1"
            "STEPFUN" -> "https://api.stepfun.com/v1"
            "XAI" -> "https://api.x.ai/v1"
            "FIREWORKS" -> "https://api.fireworks.ai/inference/v1"
            "TOGETHER_AI" -> "https://api.together.xyz/v1"
            "PERPLEXITY" -> "https://api.perplexity.ai"
            "NOVITA" -> "https://api.novita.ai/v3/openai"
            "REPLICATE" -> "https://api.replicate.com/v1"
            "CEREBRAS" -> "https://api.cerebras.ai/v1"
            "SAMBANOVA" -> "https://api.sambanova.ai/v1"
            "OLLAMA" -> "http://127.0.0.1:11434/v1"
            "LMSTUDIO" -> "http://127.0.0.1:1234/v1"
            else -> ""
        }
    }

    private fun resolveSystemPrompt(languageCode: String): String {
        return resolveSystemPrompt(languageCode, "")
    }

    private fun resolveSystemPrompt(languageCode: String, customSystemPrompt: String): String {
        val custom = customSystemPrompt.trim()
        if (custom.isNotBlank()) {
            return custom
        }
        return if (languageCode.lowercase().startsWith("en")) {
            systemPromptEn
        } else {
            systemPromptZh
        }
    }
}
