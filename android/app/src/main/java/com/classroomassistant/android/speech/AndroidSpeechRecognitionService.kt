package com.classroomassistant.android.speech

import com.classroomassistant.android.model.LocalAsrModelManager
import com.classroomassistant.core.audio.AudioFormatSpec
import com.google.gson.JsonParser
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class AndroidSpeechRecognitionService(
    private val asrModelManager: LocalAsrModelManager,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .callTimeout(3, TimeUnit.MINUTES)
        .build()
) {
    private val asrVadGate = LightweightVadGate()

    fun recognize(
        pcm16: ByteArray,
        useCloudWhisper: Boolean,
        localAsrEnabled: Boolean,
        speechApiKey: String,
        localModelId: String,
        cloudWhisperLanguage: String = "zh",
        enableVadGate: Boolean = true,
        onLog: (String) -> Unit
    ): String {
        if (pcm16.isEmpty()) {
            onLog("语音识别跳过：输入音频为空")
            return ""
        }

        if (!useCloudWhisper && !localAsrEnabled) {
            onLog("语音识别已关闭：本机ASR关闭且未启用云端Whisper")
            return ""
        }

        val pcmForRecognition = if (enableVadGate) {
            val filtered = applyAsrVadGate(pcm16, onLog)
            if (filtered == null) {
                return ""
            }
            filtered
        } else {
            pcm16
        }

        if (useCloudWhisper) {
            return recognizeByCloudWhisper(pcmForRecognition, speechApiKey, cloudWhisperLanguage, onLog)
        }
        return recognizeByLocalModel(pcmForRecognition, localModelId, onLog)
    }

    private fun applyAsrVadGate(
        pcm16: ByteArray,
        onLog: (String) -> Unit
    ): ByteArray? {
        val startedAt = System.currentTimeMillis()
        val result = asrVadGate.filter(pcm16)
        val costMs = System.currentTimeMillis() - startedAt

        if (!result.hasSpeech) {
            onLog(
                "ASR前置VAD: 未检测到明确人声，跳过识别（音频=${result.originalDurationMs}ms, " +
                    "maxRms=${formatRms(result.maxRms)}, noise=${formatRms(result.noiseFloorRms)}, " +
                    "阈值=${formatRms(result.speechThresholdRms)}, 用时=${costMs}ms）"
            )
            return null
        }

        val speechPercent = String.format(Locale.ROOT, "%.1f", result.speechRatio * 100f)
        onLog(
            "ASR前置VAD: 保留语音 ${result.keptDurationMs}ms/${result.originalDurationMs}ms " +
                "(${speechPercent}%), 分段=${result.segmentCount}, " +
                "maxRms=${formatRms(result.maxRms)}, noise=${formatRms(result.noiseFloorRms)}, " +
                "阈值=${formatRms(result.speechThresholdRms)}, 用时=${costMs}ms"
        )
        return result.filteredPcm16
    }

    private fun formatRms(value: Float): String {
        return String.format(Locale.ROOT, "%.4f", value)
    }

    private fun recognizeByLocalModel(
        pcm16: ByteArray,
        modelId: String,
        onLog: (String) -> Unit
    ): String {
        val useStreamingRecognizer = isStreamingModel(modelId)
        onLog("本机ASR引擎: ${if (useStreamingRecognizer) "OnlineRecognizer(streaming)" else "OfflineRecognizer"}")
        return if (useStreamingRecognizer) {
            recognizeByLocalStreamingModel(pcm16, modelId, onLog)
        } else {
            recognizeByLocalOfflineModel(pcm16, modelId, onLog)
        }
    }

    private fun recognizeByLocalOfflineModel(
        pcm16: ByteArray,
        modelId: String,
        onLog: (String) -> Unit
    ): String {
        val startedAt = System.currentTimeMillis()
        val modelDir = asrModelManager.getModelDir(modelId)
        if (!asrModelManager.isModelReady(modelId)) {
            onLog("本机ASR模型未就绪: $modelId")
            return ""
        }

        val encoder = File(modelDir, "encoder.onnx")
        val decoder = File(modelDir, "decoder.onnx")
        val joiner = File(modelDir, "joiner.onnx")
        val tokens = File(modelDir, "tokens.txt")
        val missing = listOf(encoder, decoder, joiner, tokens).filterNot { it.exists() }
        if (missing.isNotEmpty()) {
            onLog("本机ASR模型文件缺失: ${missing.joinToString { it.name }}")
            return ""
        }

        val transducerConfig = OfflineTransducerModelConfig(
            encoder = encoder.absolutePath,
            decoder = decoder.absolutePath,
            joiner = joiner.absolutePath
        )
        val modelConfig = OfflineModelConfig(
            transducer = transducerConfig,
            tokens = tokens.absolutePath,
            numThreads = recommendedThreadCount(),
            debug = false,
            provider = "cpu"
        )
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(AudioFormatSpec.SAMPLE_RATE, 80, 0f),
            modelConfig = modelConfig,
            decodingMethod = "greedy_search"
        )

        val recognizer = OfflineRecognizer(null, config)
        val stream = recognizer.createStream()
        return try {
            val samples = pcm16ToFloat(pcm16)
            stream.acceptWaveform(samples, AudioFormatSpec.SAMPLE_RATE)
            recognizer.decode(stream)
            val text = recognizer.getResult(stream).text.trim()
            onLog(
                "本机ASR完成(${System.currentTimeMillis() - startedAt}ms): ${if (text.isBlank()) "(空结果)" else text.take(120)}"
            )
            text
        } finally {
            runCatching { stream.release() }
            runCatching { recognizer.release() }
        }
    }

    private fun recognizeByLocalStreamingModel(
        pcm16: ByteArray,
        modelId: String,
        onLog: (String) -> Unit
    ): String {
        val startedAt = System.currentTimeMillis()
        val modelDir = asrModelManager.getModelDir(modelId)
        if (!asrModelManager.isModelReady(modelId)) {
            onLog("本机ASR模型未就绪: $modelId")
            return ""
        }

        val encoder = File(modelDir, "encoder.onnx")
        val decoder = File(modelDir, "decoder.onnx")
        val joiner = File(modelDir, "joiner.onnx")
        val tokens = File(modelDir, "tokens.txt")
        val missing = listOf(encoder, decoder, joiner, tokens).filterNot { it.exists() }
        if (missing.isNotEmpty()) {
            onLog("本机ASR模型文件缺失: ${missing.joinToString { it.name }}")
            return ""
        }

        val transducerConfig = OnlineTransducerModelConfig(
            encoder = encoder.absolutePath,
            decoder = decoder.absolutePath,
            joiner = joiner.absolutePath
        )
        val modelConfig = OnlineModelConfig(
            transducer = transducerConfig,
            tokens = tokens.absolutePath,
            numThreads = recommendedThreadCount(),
            debug = false,
            provider = "cpu"
        )
        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(AudioFormatSpec.SAMPLE_RATE, 80, 0f),
            modelConfig = modelConfig,
            enableEndpoint = true,
            decodingMethod = "greedy_search"
        )

        val recognizer = OnlineRecognizer(null, config)
        val stream = recognizer.createStream()
        return try {
            val samples = pcm16ToFloat(pcm16)
            stream.acceptWaveform(samples, AudioFormatSpec.SAMPLE_RATE)
            stream.inputFinished()
            while (recognizer.isReady(stream)) {
                recognizer.decode(stream)
            }
            val text = recognizer.getResult(stream).text.trim()
            onLog(
                "本机ASR完成(${System.currentTimeMillis() - startedAt}ms): ${if (text.isBlank()) "(空结果)" else text.take(120)}"
            )
            text
        } finally {
            runCatching { stream.release() }
            runCatching { recognizer.release() }
        }
    }

    private fun isStreamingModel(modelId: String): Boolean {
        return modelId.contains("streaming", ignoreCase = true)
    }

    private fun recognizeByCloudWhisper(
        pcm16: ByteArray,
        apiKey: String,
        language: String,
        onLog: (String) -> Unit
    ): String {
        if (apiKey.isBlank()) {
            onLog("云端Whisper未配置 Speech API Key")
            return ""
        }
        val wavBytes = pcm16ToWav(pcm16)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", "whisper-large-v3-turbo")
            .addFormDataPart("language", language.ifBlank { "zh" })
            .addFormDataPart("response_format", "json")
            .addFormDataPart("temperature", "0")
            .addFormDataPart(
                "file",
                "audio.wav",
                wavBytes.toRequestBody("audio/wav".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/audio/transcriptions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val startedAt = System.currentTimeMillis()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                onLog("云端Whisper失败: HTTP ${response.code}, body=${responseBody.take(300)}")
                return ""
            }
            val json = JsonParser.parseString(responseBody).asJsonObject
            val text = json.get("text")?.asString?.trim().orEmpty()
            onLog("云端Whisper完成(${System.currentTimeMillis() - startedAt}ms): ${if (text.isBlank()) "(空结果)" else text.take(120)}")
            return text
        }
    }

    private fun recommendedThreadCount(): Int {
        val available = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        return available.coerceIn(2, 4)
    }

    private fun pcm16ToFloat(data: ByteArray): FloatArray {
        val sampleCount = data.size / AudioFormatSpec.FRAME_SIZE
        val result = FloatArray(sampleCount)
        var i = 0
        var idx = 0
        while (i + 1 < data.size) {
            val low = data[i].toInt() and 0xff
            val high = data[i + 1].toInt()
            val sample = (high shl 8) or low
            result[idx] = (sample / 32768.0f).coerceIn(-1f, 1f)
            i += 2
            idx++
        }
        return result
    }

    private fun pcm16ToWav(pcm16: ByteArray): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val sampleRate = AudioFormatSpec.SAMPLE_RATE
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcm16.size
        val riffSize = 36 + dataSize

        val out = ByteArrayOutputStream(44 + dataSize)
        out.write("RIFF".toByteArray())
        out.write(intToLe(riffSize))
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.write(intToLe(16))
        out.write(shortToLe(1))
        out.write(shortToLe(channels))
        out.write(intToLe(sampleRate))
        out.write(intToLe(byteRate))
        out.write(shortToLe(blockAlign))
        out.write(shortToLe(bitsPerSample))
        out.write("data".toByteArray())
        out.write(intToLe(dataSize))
        out.write(pcm16)
        return out.toByteArray()
    }

    private fun intToLe(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        )
    }

    private fun shortToLe(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte()
        )
    }
}
