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

    private val systemPrompt = "你是课堂助手，请用简洁清晰的语言回答问题。"

    fun generateAnswer(
        provider: String,
        modelName: String,
        apiToken: String,
        apiSecretKey: String,
        prompt: String,
        onLog: (String) -> Unit
    ): String {
        if (apiToken.isBlank()) {
            onLog("AI问答跳过：未配置 API Token")
            return ""
        }
        val normalizedProvider = provider.uppercase()
        if (normalizedProvider == "QIANFAN" && apiSecretKey.isBlank()) {
            return generateAnswerViaHttp(provider, modelName, apiToken, prompt, onLog)
        }
        return generateAnswerViaLangChain4j(
            provider = normalizedProvider,
            modelName = modelName,
            apiToken = apiToken,
            apiSecretKey = apiSecretKey,
            prompt = prompt,
            onLog = onLog
        )
    }

    private fun generateAnswerViaLangChain4j(
        provider: String,
        modelName: String,
        apiToken: String,
        apiSecretKey: String,
        prompt: String,
        onLog: (String) -> Unit
    ): String {
        val resolvedModelName = resolveModelName(provider, modelName)
        val promptText = "$systemPrompt\n\n$prompt"
        val model = buildLangChain4jModel(provider, resolvedModelName, apiToken, apiSecretKey)
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
        apiToken: String,
        apiSecretKey: String
    ): ChatLanguageModel {
        return when (provider.uppercase()) {
            "QIANFAN" -> QianfanChatModel.builder()
                .apiKey(apiToken)
                .secretKey(apiSecretKey)
                .modelName(modelName)
                .build()
            "DEEPSEEK" -> OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiToken)
                .modelName(modelName)
                .build()
            "KIMI" -> OpenAiChatModel.builder()
                .baseUrl("https://api.moonshot.cn/v1")
                .apiKey(apiToken)
                .modelName(modelName)
                .build()
            "OPENAI" -> OpenAiChatModel.builder()
                .apiKey(apiToken)
                .modelName(modelName)
                .build()
            else -> OpenAiChatModel.builder()
                .apiKey(apiToken)
                .modelName(modelName)
                .build()
        }
    }

    private fun generateAnswerViaHttp(
        provider: String,
        modelName: String,
        apiToken: String,
        prompt: String,
        onLog: (String) -> Unit
    ): String {
        val endpoint = resolveEndpoint(provider)
        val resolvedModelName = resolveModelName(provider, modelName)

        val body = JsonObject().apply {
            addProperty("model", resolvedModelName)
            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", systemPrompt)
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

    private fun resolveEndpoint(provider: String): String {
        return when (provider.uppercase()) {
            "OPENAI" -> "https://api.openai.com/v1/chat/completions"
            "DEEPSEEK" -> "https://api.deepseek.com/v1/chat/completions"
            "KIMI" -> "https://api.moonshot.cn/v1/chat/completions"
            "QIANFAN" -> "https://qianfan.baidubce.com/v2/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }

    private fun resolveModelName(provider: String, configured: String): String {
        if (configured.isNotBlank()) {
            return configured
        }
        return when (provider.uppercase()) {
            "OPENAI" -> "gpt-4o-mini"
            "DEEPSEEK" -> "deepseek-chat"
            "KIMI" -> "moonshot-v1-8k"
            "QIANFAN" -> "ernie-4.0-8k"
            else -> "gpt-4o-mini"
        }
    }
}
