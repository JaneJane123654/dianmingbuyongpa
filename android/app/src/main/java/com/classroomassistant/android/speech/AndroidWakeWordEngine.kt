package com.classroomassistant.android.speech

import android.content.Context
import android.icu.text.Transliterator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.classroomassistant.android.platform.AndroidAudioRecorder
import com.classroomassistant.android.platform.AppCrashMonitor
import com.classroomassistant.core.audio.AudioFormatSpec
import com.classroomassistant.core.platform.PlatformAudioRecorder
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import java.io.File
import java.io.RandomAccessFile
import java.lang.System
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AndroidWakeWordEngine(
    private val context: Context,
    private val audioRecorder: AndroidAudioRecorder
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val hanToLatinTransliterator: Transliterator by lazy {
        Transliterator.getInstance("Han-Latin")
    }
    private var keywordSpotter: KeywordSpotter? = null
    private var stream: OnlineStream? = null
    private var keywordFile: File? = null
    private var running = false
    private var lastTriggerMs = 0L
    private var totalAudioChunks = 0L
    private var totalDecodeCalls = 0L
    private var lastDiagLogMs = 0L
    private var consecutiveLowEnergyWindows = 0
    private var lastLowEnergyWarningMs = 0L
    private var lastGainLogMs = 0L
    private var sessionMaxRawRms = 0f
    private var sessionMaxBoostedRms = 0f
    private var speechDetectedCount = 0
    private var lastSpeechLogMs = 0L
    private var audioDataListener: PlatformAudioRecorder.AudioDataListener? = null
    private var autoSwitchInProgress = false
    private var autoSwitchAttempts = 0
    private var lastAutoSwitchMs = 0L
    private var currentTestKeyword: String? = null

    fun start(
        modelDir: File,
        keywords: List<String>,
        keywordThreshold: Float,
        onWake: () -> Unit,
        onLog: (String) -> Unit,
        onAudioLevel: ((Float) -> Unit)? = null,
        enableTtsSelfTest: Boolean = false
    ): Boolean {
        if (running) {
            return true
        }
        if (keywords.isEmpty()) {
            onLog("唤醒词为空")
            return false
        }
        val encoder = File(modelDir, "encoder.onnx")
        val decoder = File(modelDir, "decoder.onnx")
        val joiner = File(modelDir, "joiner.onnx")
        val tokens = File(modelDir, "tokens.txt")
        val missing = listOf(encoder, decoder, joiner, tokens).filterNot { it.exists() }
        if (missing.isNotEmpty()) {
            onLog("模型文件缺失: ${missing.joinToString { it.name }}")
            AppCrashMonitor.markListeningStage(context, "MODEL_FILES_MISSING")
            AppCrashMonitor.clearListeningStage(context)
            return false
        }
        onLog("KWS 模型目录: ${modelDir.absolutePath}")
        logFileState("encoder", encoder, onLog)
        logFileState("decoder", decoder, onLog)
        logFileState("joiner", joiner, onLog)
        logFileState("tokens", tokens, onLog)

        try {
            AppCrashMonitor.markListeningStage(context, "KWS_INIT")
            AppCrashMonitor.markListeningStage(context, "KWS_INIT_PREPARE_KEYWORDS")
            val validTokens = loadValidTokens(tokens)
            if (validTokens.isEmpty()) {
                onLog("tokens.txt 解析失败或为空")
                AppCrashMonitor.markListeningStage(context, "KWS_TOKENS_INVALID")
                AppCrashMonitor.clearListeningStage(context)
                return false
            }
            onLog("tokens.txt 词表大小: ${validTokens.size}, 样本: ${validTokens.take(10).joinToString(", ")}")
            val predefinedKeywordsFile = findPredefinedKeywordsFile(modelDir)
            val expandedKeywords = expandKeywordsForRuntime(keywords)
            if (expandedKeywords.size > keywords.size) {
                val autoAdded = expandedKeywords - keywords.toSet()
                onLog("关键词自动扩展: 原始=${keywords.size}, 自动追加=${autoAdded.size}, 候选=${expandedKeywords.size}")
                if (autoAdded.isNotEmpty()) {
                    onLog("自动追加词: ${autoAdded.joinToString("、")}")
                }
            }
            val runtimeKeywordLines = buildKeywordLinesForFile(expandedKeywords, validTokens)
            val mappedKeywords = extractMappedKeywords(runtimeKeywordLines)
            val unresolvedKeywords = expandedKeywords.filterNot {
                mappedKeywords.contains(it.replace(Regex("\\s+"), "_"))
            }
            if (unresolvedKeywords.isNotEmpty()) {
                onLog("以下关键词未能映射到模型词表，当前模型下无法直接触发: ${unresolvedKeywords.joinToString("、")}")
            }
            if (predefinedKeywordsFile != null) {
                onLog("使用模型内置关键词文件: ${predefinedKeywordsFile.absolutePath}")
                logFileState("keywords", predefinedKeywordsFile, onLog)
                val predefinedLines = loadKeywordsFileLines(predefinedKeywordsFile)
                val mergedLines = linkedSetOf<String>().apply {
                    addAll(predefinedLines)
                    addAll(runtimeKeywordLines)
                }.toList()

                if (mergedLines.isNotEmpty()) {
                    AppCrashMonitor.markListeningStage(context, "KWS_INIT_WRITE_KEYWORDS_FILE")
                    keywordFile = writeKeywordsFile(mergedLines)
                    onLog("关键词已合并: 内置=${predefinedLines.size}, 运行时=${runtimeKeywordLines.size}, 去重后=${mergedLines.size}")
                    onLog("关键词文件: ${keywordFile?.absolutePath}")
                    onLog("关键词预览: ${mergedLines.take(3).joinToString(" | ")}")
                } else {
                    keywordFile = null
                }
            } else {
                if (runtimeKeywordLines.isEmpty()) {
                    onLog("唤醒词无法映射到模型词表，请检查关键词或更换模型")
                    AppCrashMonitor.markListeningStage(context, "KWS_KEYWORDS_INVALID")
                    AppCrashMonitor.clearListeningStage(context)
                    return false
                }
                onLog("未找到内置关键词文件，回退到运行时关键词，候选数量: ${runtimeKeywordLines.size}")
                AppCrashMonitor.markListeningStage(context, "KWS_INIT_WRITE_KEYWORDS_FILE")
                keywordFile = writeKeywordsFile(runtimeKeywordLines)
                onLog("关键词文件: ${keywordFile?.absolutePath}")
                onLog("关键词预览: ${runtimeKeywordLines.take(3).joinToString(" | ")}")
            }
            val activeKeywordsFile = keywordFile ?: predefinedKeywordsFile
            if (activeKeywordsFile == null || !activeKeywordsFile.exists()) {
                onLog("关键词文件不可用")
                AppCrashMonitor.markListeningStage(context, "KWS_KEYWORDS_FILE_MISSING")
                AppCrashMonitor.clearListeningStage(context)
                return false
            }

            AppCrashMonitor.markListeningStage(context, "KWS_INIT_MODEL_CONFIG")
            val transducerModelConfig = OnlineTransducerModelConfig(
                encoder.absolutePath,
                decoder.absolutePath,
                joiner.absolutePath
            )
            val modelConfig = OnlineModelConfig().apply {
                transducer = transducerModelConfig
                this.tokens = tokens.absolutePath
                numThreads = 2
                debug = false
                provider = "cpu"
            }
            AppCrashMonitor.markListeningStage(context, "KWS_INIT_SPOTTER_CONFIG")
            val kwsConfig = KeywordSpotterConfig().apply {
                featConfig = FeatureConfig(AudioFormatSpec.SAMPLE_RATE, 80, 0f)
                this.modelConfig = modelConfig
                maxActivePaths = 4
                keywordsFile = activeKeywordsFile.absolutePath
                keywordsScore = 6.0f
                keywordsThreshold = keywordThreshold.coerceIn(0.05f, 0.9f)
                numTrailingBlanks = 0
            }
            onLog("KWS 配置: provider=${modelConfig.provider}, threads=${modelConfig.numThreads}, score=${kwsConfig.keywordsScore}, threshold=${kwsConfig.keywordsThreshold}, trailingBlanks=${kwsConfig.numTrailingBlanks}, maxActivePaths=${kwsConfig.maxActivePaths}")
            onLog("KWS 使用关键词文件: ${kwsConfig.keywordsFile}")

            AppCrashMonitor.markListeningStage(context, "KWS_INIT_NEW_FROM_FILE")
            onLog("KWS 初始化阶段: 开始创建 KeywordSpotter")
            val spotterCreateStartMs = System.currentTimeMillis()
            keywordSpotter = KeywordSpotter(null, kwsConfig)
            onLog("KWS 初始化阶段: KeywordSpotter 创建完成，耗时=${System.currentTimeMillis() - spotterCreateStartMs}ms")
            AppCrashMonitor.markListeningStage(context, "KWS_INIT_CREATE_STREAM")
            onLog("KWS 初始化阶段: 开始创建 Stream")
            val streamCreateStartMs = System.currentTimeMillis()
            stream = keywordSpotter?.createStream()
            onLog("KWS 初始化阶段: Stream 创建完成，耗时=${System.currentTimeMillis() - streamCreateStartMs}ms")

            // 模型管道自检：用合成音频验证 KWS 引擎能否正常解码
            runModelPipelineTest(onLog)
        } catch (t: Throwable) {
            onLog("初始化唤醒词引擎异常: ${t.message}")
            AppCrashMonitor.recordHandledError(context, "初始化 KWS 异常", t)
            releaseKwsResources()
            AppCrashMonitor.clearListeningStage(context)
            return false
        }

        if (keywordSpotter == null || stream == null) {
            onLog("本地唤醒词引擎初始化失败")
            releaseKwsResources()
            AppCrashMonitor.clearListeningStage(context)
            return false
        }

        onLog("KWS 初始化阶段: 开始启动录音")
        val audioStartMs = System.currentTimeMillis()
        val listener = object : PlatformAudioRecorder.AudioDataListener {
            private var loopMarked = false
            private var recentRmsAvg = 0f
            private var recentPeakMax = 0f
            private var recentChunks = 0

            override fun onAudioData(data: ByteArray, length: Int) {
                if (!running) {
                    return
                }
                try {
                    if (!loopMarked) {
                        AppCrashMonitor.markListeningStage(context, "KWS_DETECT_LOOP")
                        loopMarked = true
                    }
                    val localSpotter = keywordSpotter ?: return
                    val localStream = stream ?: return
                    val rawFloats = pcm16ToFloat(data, length)
                    if (rawFloats.isEmpty()) {
                        return
                    }

                    val rawRms = computeRms(rawFloats)
                    onAudioLevel?.invoke(rawRms)
                    val gain = computeAdaptiveGain(rawRms)
                    val floats = if (gain > 1.01f) amplifySamples(rawFloats, gain) else rawFloats
                    totalAudioChunks++
                    val rms = computeRms(floats)
                    val peak = computePeak(floats)
                    recentRmsAvg += rms
                    if (peak > recentPeakMax) {
                        recentPeakMax = peak
                    }
                    recentChunks++

                    if (rawRms > sessionMaxRawRms) sessionMaxRawRms = rawRms
                    if (rms > sessionMaxBoostedRms) sessionMaxBoostedRms = rms

                    // 语音活动检测：rawRms 明显超过噪声底噪时即时记录
                    if (rawRms > 0.003f) {
                        speechDetectedCount++
                        val now2 = System.currentTimeMillis()
                        if (now2 - lastSpeechLogMs > 2000L) {
                            lastSpeechLogMs = now2
                            onLog("语音活动: rawRms=${
                                "%.4f".format(Locale.ROOT, rawRms)
                            }, boostedRms=${
                                "%.4f".format(Locale.ROOT, rms)
                            }, peak=${
                                "%.4f".format(Locale.ROOT, peak)
                            }, gain=${
                                "%.1f".format(Locale.ROOT, gain)
                            }")
                        }
                    }

                    maybeLogGain(onLog, gain, rawRms, rms)

                    localStream.acceptWaveform(floats, AudioFormatSpec.SAMPLE_RATE)
                    while (localSpotter.isReady(localStream)) {
                        localSpotter.decode(localStream)
                        totalDecodeCalls++
                    }
                    val result = localSpotter.getResult(localStream)
                    if (result.keyword.isNotBlank()) {
                        onLog("检测结果: keyword=${result.keyword}, tokens=${result.tokens.joinToString(" ")}")
                        val now = System.currentTimeMillis()
                        if (now - lastTriggerMs > 1500) {
                            lastTriggerMs = now
                            mainHandler.post(onWake)
                        }
                        localSpotter.reset(localStream)
                    } else {
                        val diagLogged = maybeLogRuntimeDiagnostics(onLog, recentChunks, recentRmsAvg, recentPeakMax)
                        if (diagLogged) {
                            recentRmsAvg = 0f
                            recentPeakMax = 0f
                            recentChunks = 0
                        }
                        maybeAutoSwitchAudioSource(onLog)
                    }
                } catch (t: Throwable) {
                    running = false
                    AppCrashMonitor.recordHandledError(context, "唤醒检测异常", t)
                    mainHandler.post {
                        onLog("唤醒检测异常: ${t.message ?: t.javaClass.simpleName}")
                        stop(onLog)
                    }
                }
            }

            override fun onError(message: String) {
                AppCrashMonitor.markListeningStage(context, "AUDIO_RECORDER_ERROR")
                mainHandler.post { onLog(message) }
            }
        }
        audioDataListener = listener
        val started = audioRecorder.start(listener)
        running = started
        if (!started) {
            releaseKwsResources()
            AppCrashMonitor.clearListeningStage(context)
            onLog("KWS 初始化阶段: 录音启动失败，耗时=${System.currentTimeMillis() - audioStartMs}ms")
        } else {
            AppCrashMonitor.markListeningStage(context, "ENGINE_RUNNING")
            onLog("KWS 初始化阶段: 录音启动完成，耗时=${System.currentTimeMillis() - audioStartMs}ms")
            onLog("录音参数: source=${audioRecorder.getActiveAudioSourceName()}, sampleRate=${AudioFormatSpec.SAMPLE_RATE}, buffer=${AudioFormatSpec.BUFFER_SIZE}")

            // 诊断：输出音频输入设备列表
            val inputDevices = audioRecorder.queryAudioInputDevices()
            onLog("音频输入设备: ${inputDevices.joinToString("; ")}")

            // 诊断：模拟器环境提示
            val emulatorHint = audioRecorder.getEmulatorAudioHint()
            if (emulatorHint != null) {
                onLog("ℹ $emulatorHint")
            }

            // 在后台运行 TTS 合成自检：用 TTS 合成唤醒词音频直接送入模型检测
            val testKeyword = keywords.firstOrNull()?.trim() ?: ""
            currentTestKeyword = if (enableTtsSelfTest) testKeyword.ifBlank { null } else null
            if (enableTtsSelfTest && testKeyword.isNotBlank()) {
                Thread {
                    try {
                        runTtsSpeechTest(testKeyword, onLog)
                    } catch (t: Throwable) {
                        mainHandler.post {
                            onLog("TTS合成自检异常: ${t.message}")
                        }
                    }
                }.apply {
                    name = "KWS-TTS-Test"
                    isDaemon = true
                    start()
                }
            }
        }
        return started
    }

    fun stop() {
        stop(null)
    }

    fun stop(onLog: ((String) -> Unit)?) {
        if (!running) {
            AppCrashMonitor.clearListeningStage(context)
            return
        }
        running = false
        onLog?.invoke(
            "会话统计: 总chunks=$totalAudioChunks, 总decodes=$totalDecodeCalls, " +
            "最大rawRms=${
                "%.6f".format(Locale.ROOT, sessionMaxRawRms)
            }, 最大boostedRms=${
                "%.4f".format(Locale.ROOT, sessionMaxBoostedRms)
            }, 语音活动chunks=$speechDetectedCount, " +
            "音频源=${audioRecorder.getActiveAudioSourceName()}"
        )
        if (speechDetectedCount == 0) {
            val emulatorHint = audioRecorder.getEmulatorAudioHint()
            onLog?.invoke(
                "⚠ 整个监听期间未检测到任何语音活动(rawRms始终<0.003)。" +
                "当前音频源: ${audioRecorder.getActiveAudioSourceName()}。" +
                if (emulatorHint != null) {
                    "模拟器环境提示: 请在模拟器 Extended Controls > Microphone 中开启 " +
                    "\"Virtual microphone uses host audio input\"。" +
                    "同时确保电脑本机麦克风正常工作且未被静音。"
                } else {
                    "可能原因: 麦克风被系统静音/被其他应用占用/设备音频处理过于激进。" +
                    "建议: 关闭其他可能使用麦克风的应用后重试。"
                }
            )
        } else if (sessionMaxRawRms < 0.01f) {
            onLog?.invoke(
                "⚠ 监听期间检测到微弱语音(maxRawRms=${
                    "%.6f".format(Locale.ROOT, sessionMaxRawRms)
                })，信号可能不足以触发唤醒。请尝试靠近麦克风说话。"
            )
        }
        audioRecorder.stop()
        audioDataListener = null
        autoSwitchInProgress = false
        autoSwitchAttempts = 0
        lastAutoSwitchMs = 0L
        currentTestKeyword = null
        releaseKwsResources()
        AppCrashMonitor.clearListeningStage(context)
    }

    fun isRunning(): Boolean = running

    fun runTtsSelfTestNow(keyword: String, onLog: (String) -> Unit): Boolean {
        val normalizedKeyword = keyword.trim()
        if (!running) {
            onLog("TTS合成自检: 引擎未运行，请先开始监听")
            return false
        }
        if (normalizedKeyword.isBlank()) {
            onLog("TTS合成自检: 唤醒词为空，无法执行")
            return false
        }
        if (keywordSpotter == null) {
            onLog("TTS合成自检: KeywordSpotter 未初始化，无法执行")
            return false
        }

        Thread {
            runCatching { runTtsSpeechTest(normalizedKeyword, onLog) }
                .onFailure { throwable ->
                    mainHandler.post {
                        onLog("TTS合成自检异常: ${throwable.message ?: throwable.javaClass.simpleName}")
                    }
                }
        }.apply {
            name = "KWS-TTS-Manual-Test"
            isDaemon = true
            start()
        }
        return true
    }

    /**
     * 综合 TTS 自测：对所有关键词执行全面的测试套件，包括：
     * 1. 多关键词逐一测试
     * 2. 多语速测试（0.7/0.85/1.0/1.2）
     * 3. 音量变化测试（低/中/高）
     * 4. 误触发测试（用无关文本验证不会误报）
     * 5. 噪声鲁棒性测试（在 TTS 音频上叠加噪声）
     * 6. 耗时指标统计
     * 7. 综合结果摘要
     */
    fun runComprehensiveTtsSelfTest(keywords: List<String>, onLog: (String) -> Unit): Boolean {
        if (!running) {
            onLog("TTS综合自检: 引擎未运行，请先开始监听")
            return false
        }
        val validKeywords = keywords.map { it.trim() }.filter { it.isNotBlank() }
        if (validKeywords.isEmpty()) {
            onLog("TTS综合自检: 唤醒词为空，无法执行")
            return false
        }
        if (keywordSpotter == null) {
            onLog("TTS综合自检: KeywordSpotter 未初始化，无法执行")
            return false
        }

        Thread {
            runCatching {
                doComprehensiveTtsSelfTest(validKeywords, onLog)
            }.onFailure { throwable ->
                mainHandler.post {
                    onLog("TTS综合自检异常: ${throwable.message ?: throwable.javaClass.simpleName}")
                }
            }
        }.apply {
            name = "KWS-TTS-Comprehensive-Test"
            isDaemon = true
            start()
        }
        return true
    }

    /** 综合测试的内部执行体（在后台线程中运行） */
    private fun doComprehensiveTtsSelfTest(keywords: List<String>, onLog: (String) -> Unit) {
        val overallStartMs = System.currentTimeMillis()
        onLog("═══════════════════════════════════════════")
        onLog("TTS综合自检 开始 (${keywords.size} 个关键词)")
        onLog("═══════════════════════════════════════════")

        // ------ 结果收集 ------
        val results = mutableListOf<ComprehensiveTestResult>()

        // ------ Step 0: TTS 引擎初始化（复用同一个 TTS 实例） ------
        val ttsInitStartMs = System.currentTimeMillis()
        val localTts = initTtsEngine(onLog)
        if (localTts == null) {
            onLog("TTS综合自检: TTS 引擎初始化失败，终止测试")
            return
        }
        val ttsInitMs = System.currentTimeMillis() - ttsInitStartMs
        onLog("TTS引擎初始化耗时: ${ttsInitMs}ms")

        try {
            // ------ Step 1: 多关键词基础测试（默认语速 0.85） ------
            onLog("───────────────────────────────────────────")
            onLog("▶ 阶段1: 多关键词基础测试 (语速=0.85)")
            onLog("───────────────────────────────────────────")
            localTts.setSpeechRate(0.85f)
            for (kw in keywords) {
                val r = synthesizeAndDetect(localTts, kw, 1.0f, onLog, "基础测试")
                results.add(r)
            }

            // ------ Step 2: 多语速测试（仅第一个关键词） ------
            val primaryKeyword = keywords.first()
            onLog("───────────────────────────────────────────")
            onLog("▶ 阶段2: 多语速测试 (关键词='$primaryKeyword')")
            onLog("───────────────────────────────────────────")
            val speechRates = listOf(0.7f, 1.0f, 1.2f)
            for (rate in speechRates) {
                localTts.setSpeechRate(rate)
                val r = synthesizeAndDetect(localTts, primaryKeyword, 1.0f, onLog, "语速=${rate}")
                results.add(r)
            }

            // ------ Step 3: 音量变化测试（第一个关键词，语速恢复 0.85） ------
            onLog("───────────────────────────────────────────")
            onLog("▶ 阶段3: 音量变化测试 (关键词='$primaryKeyword')")
            onLog("───────────────────────────────────────────")
            localTts.setSpeechRate(0.85f)
            val volumeScales = listOf(0.15f to "极低(0.15x)", 0.4f to "低(0.4x)", 2.0f to "高(2.0x)", 5.0f to "极高(5.0x)")
            for ((scale, label) in volumeScales) {
                val r = synthesizeAndDetect(localTts, primaryKeyword, scale, onLog, "音量=$label")
                results.add(r)
            }

            // ------ Step 4: 噪声鲁棒性测试 ------
            onLog("───────────────────────────────────────────")
            onLog("▶ 阶段4: 噪声鲁棒性测试 (关键词='$primaryKeyword')")
            onLog("───────────────────────────────────────────")
            localTts.setSpeechRate(0.85f)
            val noiseSnrLevels = listOf(20.0f to "SNR=20dB(轻微噪声)", 10.0f to "SNR=10dB(中等噪声)", 5.0f to "SNR=5dB(强噪声)")
            for ((snrDb, label) in noiseSnrLevels) {
                val r = synthesizeAndDetectWithNoise(localTts, primaryKeyword, snrDb, onLog, label)
                results.add(r)
            }

            // ------ Step 5: 误触发测试（用无关文本） ------
            onLog("───────────────────────────────────────────")
            onLog("▶ 阶段5: 误触发测试 (验证无关文本不会命中)")
            onLog("───────────────────────────────────────────")
            localTts.setSpeechRate(0.85f)
            val irrelevantTexts = listOf("今天天气真好", "请翻到第三十页", "下课了同学们")
            for (text in irrelevantTexts) {
                val r = synthesizeAndDetect(localTts, text, 1.0f, onLog, "误触发-'$text'")
                results.add(r)
            }

            // ------ Step 6: 重复稳定性测试（第一个关键词重复3次） ------
            onLog("───────────────────────────────────────────")
            onLog("▶ 阶段6: 重复稳定性测试 (关键词='$primaryKeyword', 3次)")
            onLog("───────────────────────────────────────────")
            localTts.setSpeechRate(0.85f)
            for (i in 1..3) {
                val r = synthesizeAndDetect(localTts, primaryKeyword, 1.0f, onLog, "重复#$i")
                results.add(r)
            }

        } finally {
            localTts.shutdown()
        }

        // ------ 汇总报告 ------
        val overallMs = System.currentTimeMillis() - overallStartMs
        onLog("═══════════════════════════════════════════")
        onLog("TTS综合自检 结果汇总 (总耗时=${overallMs}ms)")
        onLog("═══════════════════════════════════════════")

        // 分类分析
        val basicResults = results.filter { it.testName.startsWith("基础测试") }
        val speedResults = results.filter { it.testName.startsWith("语速=") }
        val volumeResults = results.filter { it.testName.startsWith("音量=") }
        val noiseResults = results.filter { it.testName.startsWith("SNR=") || it.testName.contains("噪声") }
        val falseResults = results.filter { it.testName.startsWith("误触发") }
        val repeatResults = results.filter { it.testName.startsWith("重复#") }

        val basicPass = basicResults.count { it.detected }
        onLog("✦ 基础测试: ${basicPass}/${basicResults.size} 通过 (${keywords.joinToString("、")})")
        for (r in basicResults) {
            onLog("  ${if (r.detected) "✓" else "✗"} '${r.keyword}' → 检测='${r.detectedKeyword.ifBlank { "(无)" }}' (解码=${r.decodes}, ${r.durationMs}ms)")
        }

        val speedPass = speedResults.count { it.detected }
        onLog("✦ 语速测试: ${speedPass}/${speedResults.size} 通过")
        for (r in speedResults) {
            onLog("  ${if (r.detected) "✓" else "✗"} ${r.testName} → 检测='${r.detectedKeyword.ifBlank { "(无)" }}' (${r.durationMs}ms)")
        }

        val volPass = volumeResults.count { it.detected }
        onLog("✦ 音量测试: ${volPass}/${volumeResults.size} 通过")
        for (r in volumeResults) {
            onLog("  ${if (r.detected) "✓" else "✗"} ${r.testName} → 检测='${r.detectedKeyword.ifBlank { "(无)" }}' (${r.durationMs}ms)")
        }

        val noisePass = noiseResults.count { it.detected }
        onLog("✦ 噪声测试: ${noisePass}/${noiseResults.size} 通过")
        for (r in noiseResults) {
            onLog("  ${if (r.detected) "✓" else "✗"} ${r.testName} → 检测='${r.detectedKeyword.ifBlank { "(无)" }}' (${r.durationMs}ms)")
        }

        val falseTriggers = falseResults.count { it.detected }
        onLog("✦ 误触发测试: ${falseResults.size - falseTriggers}/${falseResults.size} 通过 (${falseTriggers} 次误触发)")
        for (r in falseResults) {
            onLog("  ${if (!r.detected) "✓" else "✗误报"} ${r.testName} → 检测='${r.detectedKeyword.ifBlank { "(无)" }}'")
        }

        val repeatPass = repeatResults.count { it.detected }
        onLog("✦ 稳定性测试: ${repeatPass}/${repeatResults.size} 通过")
        for (r in repeatResults) {
            onLog("  ${if (r.detected) "✓" else "✗"} ${r.testName} → 检测='${r.detectedKeyword.ifBlank { "(无)" }}' (${r.durationMs}ms)")
        }

        // 总体评估
        val totalPositiveTests = basicResults.size + speedResults.size + volumeResults.size + noiseResults.size + repeatResults.size
        val totalPositivePass = basicPass + speedPass + volPass + noisePass + repeatPass
        val falsePositiveClean = falseTriggers == 0
        val overallScore = if (totalPositiveTests > 0) (totalPositivePass * 100 / totalPositiveTests) else 0

        onLog("───────────────────────────────────────────")
        val grade = when {
            overallScore >= 80 && falsePositiveClean -> "优秀 ★★★"
            overallScore >= 50 && falsePositiveClean -> "良好 ★★"
            overallScore >= 30 -> "一般 ★ (TTS 样本与模型差异大，建议以真人测试为准)"
            else -> "较差 (建议检查唤醒词拼写或更换关键词/模型)"
        }
        onLog("总评: 命中率=${overallScore}% ($totalPositivePass/$totalPositiveTests), 误触发=${falseTriggers}, 等级=$grade")
        if (!falsePositiveClean) {
            onLog("⚠ 存在误触发，可能需要提高 keywordsThreshold 或更换关键词")
        }
        onLog("提示: TTS合成语音与真人发音存在差异，命中率偏低属正常范围，仅当真人发音也无法触发时才需调整")
        onLog("═══════════════════════════════════════════")
    }

    /**
     * 初始化 TTS 引擎（含重试），返回可用的 TextToSpeech 实例或 null。
     */
    private fun initTtsEngine(onLog: (String) -> Unit): TextToSpeech? {
        val initLatch = CountDownLatch(1)
        var ttsReady = false
        var tts: TextToSpeech? = null

        mainHandler.post {
            tts = TextToSpeech(context) { status ->
                ttsReady = (status == TextToSpeech.SUCCESS)
                initLatch.countDown()
            }
        }

        if (!initLatch.await(12, TimeUnit.SECONDS) || !ttsReady) {
            tts?.shutdown()
            onLog("TTS引擎初始化失败，准备重试...")

            val retryLatch = CountDownLatch(1)
            var retryReady = false
            var retryTts: TextToSpeech? = null
            mainHandler.post {
                retryTts = TextToSpeech(context) { status ->
                    retryReady = (status == TextToSpeech.SUCCESS)
                    retryLatch.countDown()
                }
            }
            if (!retryLatch.await(12, TimeUnit.SECONDS) || !retryReady) {
                onLog("TTS引擎初始化失败（已重试）")
                retryTts?.shutdown()
                return null
            }
            tts = retryTts
        }

        val localTts = tts ?: return null
        val langResult = localTts.setLanguage(Locale.CHINESE)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            onLog("设备不支持中文TTS(code=$langResult)")
            localTts.shutdown()
            return null
        }
        return localTts
    }

    /**
     * 用 TTS 合成指定文本，可选施加音量缩放，然后送入 KWS 模型检测。
     * 返回检测结果结构。
     */
    private fun synthesizeAndDetect(
        tts: TextToSpeech,
        text: String,
        volumeScale: Float,
        onLog: (String) -> Unit,
        testLabel: String
    ): ComprehensiveTestResult {
        val startMs = System.currentTimeMillis()
        val spotter = keywordSpotter ?: return ComprehensiveTestResult(text, testLabel, false, "", 0, 0L)

        // 合成
        val synthStartMs = System.currentTimeMillis()
        val audioSamples = synthesizeTtsToSamples(tts, text, onLog)
        val synthMs = System.currentTimeMillis() - synthStartMs

        if (audioSamples == null || audioSamples.isEmpty()) {
            onLog("  [$testLabel] '$text': TTS合成失败 (${synthMs}ms)")
            return ComprehensiveTestResult(text, testLabel, false, "", 0, System.currentTimeMillis() - startMs)
        }

        // 音量缩放
        val scaledSamples = if (volumeScale != 1.0f) {
            FloatArray(audioSamples.size) { (audioSamples[it] * volumeScale).coerceIn(-1f, 1f) }
        } else {
            audioSamples
        }

        val durationSec = scaledSamples.size.toFloat() / AudioFormatSpec.SAMPLE_RATE
        val rms = computeRms(scaledSamples)

        // 检测
        val detectStartMs = System.currentTimeMillis()
        val (detected, detectedKeyword, decodes) = feedAudioToSpotter(spotter, scaledSamples)
        val detectMs = System.currentTimeMillis() - detectStartMs
        val totalMs = System.currentTimeMillis() - startMs

        onLog(
            "  [$testLabel] '$text': " +
            "${if (detected) "✓命中" else "✗未命中"} " +
            "(检测='${detectedKeyword.ifBlank { "(无)" }}', " +
            "合成=${synthMs}ms, 检测=${detectMs}ms, 总耗时=${totalMs}ms, " +
            "时长=${"%.2f".format(Locale.ROOT, durationSec)}s, " +
            "RMS=${"%.4f".format(Locale.ROOT, rms)}, 解码=$decodes)"
        )

        return ComprehensiveTestResult(text, testLabel, detected, detectedKeyword, decodes, totalMs)
    }

    /**
     * 合成 TTS 音频后叠加高斯噪声，模拟带噪环境检测。
     */
    private fun synthesizeAndDetectWithNoise(
        tts: TextToSpeech,
        keyword: String,
        snrDb: Float,
        onLog: (String) -> Unit,
        testLabel: String
    ): ComprehensiveTestResult {
        val startMs = System.currentTimeMillis()
        val spotter = keywordSpotter ?: return ComprehensiveTestResult(keyword, testLabel, false, "", 0, 0L)

        val synthStartMs = System.currentTimeMillis()
        val audioSamples = synthesizeTtsToSamples(tts, keyword, onLog)
        val synthMs = System.currentTimeMillis() - synthStartMs

        if (audioSamples == null || audioSamples.isEmpty()) {
            onLog("  [$testLabel] '$keyword': TTS合成失败 (${synthMs}ms)")
            return ComprehensiveTestResult(keyword, testLabel, false, "", 0, System.currentTimeMillis() - startMs)
        }

        // 叠加噪声：根据 SNR(dB) 计算噪声幅度
        val signalRms = computeRms(audioSamples).coerceAtLeast(1e-6f)
        val snrLinear = Math.pow(10.0, snrDb.toDouble() / 20.0)
        val noiseRms = (signalRms.toDouble() / snrLinear).toFloat()
        val random = java.util.Random()
        val noisySamples = FloatArray(audioSamples.size) { i ->
            (audioSamples[i] + random.nextGaussian().toFloat() * noiseRms).coerceIn(-1f, 1f)
        }

        val durationSec = noisySamples.size.toFloat() / AudioFormatSpec.SAMPLE_RATE
        val rms = computeRms(noisySamples)

        val detectStartMs = System.currentTimeMillis()
        val (detected, detectedKeyword, decodes) = feedAudioToSpotter(spotter, noisySamples)
        val detectMs = System.currentTimeMillis() - detectStartMs
        val totalMs = System.currentTimeMillis() - startMs

        onLog(
            "  [$testLabel] '$keyword': " +
            "${if (detected) "✓命中" else "✗未命中"} " +
            "(检测='${detectedKeyword.ifBlank { "(无)" }}', " +
            "合成=${synthMs}ms, 检测=${detectMs}ms, 总耗时=${totalMs}ms, " +
            "时长=${"%.2f".format(Locale.ROOT, durationSec)}s, " +
            "RMS=${"%.4f".format(Locale.ROOT, rms)}, noiseRms=${"%.4f".format(Locale.ROOT, noiseRms)}, 解码=$decodes)"
        )

        return ComprehensiveTestResult(keyword, testLabel, detected, detectedKeyword, decodes, totalMs)
    }

    /**
     * 用 TTS 合成文本到 WAV 文件并读取为 16kHz Float PCM 采样点。
     */
    private fun synthesizeTtsToSamples(tts: TextToSpeech, text: String, onLog: (String) -> Unit): FloatArray? {
        val wavFile = File(context.cacheDir, "kws_tts_test_${System.currentTimeMillis()}.wav")
        val synthLatch = CountDownLatch(1)
        var synthOk = false

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                synthOk = true
                synthLatch.countDown()
            }
            @Deprecated("Use onError(String?, Int)")
            override fun onError(utteranceId: String?) {
                synthLatch.countDown()
            }
        })

        @Suppress("DEPRECATION")
        tts.synthesizeToFile(text, null, wavFile, "kws_tts_test_${System.currentTimeMillis()}")

        if (!synthLatch.await(15, TimeUnit.SECONDS) || !synthOk) {
            wavFile.delete()
            return null
        }

        val audioSamples = readWavAsPcmFloat16k(wavFile, onLog)
        wavFile.delete()
        return audioSamples
    }

    /**
     * 将 Float PCM 音频送入 KWS 引擎进行检测（使用独立 stream）。
     * 返回 Triple(detected, detectedKeyword, decodeCount)
     */
    private fun feedAudioToSpotter(spotter: KeywordSpotter, samples: FloatArray): Triple<Boolean, String, Int> {
        var testStream: OnlineStream? = null
        try {
            testStream = spotter.createStream()

            // 前置 0.3 秒静音
            val preSilence = FloatArray((AudioFormatSpec.SAMPLE_RATE * 0.3).toInt())
            testStream.acceptWaveform(preSilence, AudioFormatSpec.SAMPLE_RATE)

            // 分 chunk 送入（模拟实时，100ms/chunk）
            val chunkSize = AudioFormatSpec.SAMPLE_RATE / 10
            var offset = 0
            while (offset < samples.size) {
                val end = minOf(offset + chunkSize, samples.size)
                val chunk = samples.copyOfRange(offset, end)
                testStream.acceptWaveform(chunk, AudioFormatSpec.SAMPLE_RATE)
                offset = end
            }

            // 后置 0.8 秒静音（触发尾部帧结果输出）
            val postSilence = FloatArray((AudioFormatSpec.SAMPLE_RATE * 0.8).toInt())
            testStream.acceptWaveform(postSilence, AudioFormatSpec.SAMPLE_RATE)

            // 解码
            var totalDecodes = 0
            while (spotter.isReady(testStream)) {
                spotter.decode(testStream)
                totalDecodes++
            }

            val result = spotter.getResult(testStream)
            val detected = result.keyword.isNotBlank()
            return Triple(detected, result.keyword, totalDecodes)
        } catch (e: Throwable) {
            return Triple(false, "", 0)
        } finally {
            runCatching { testStream?.release() }
        }
    }

    /** 综合测试单条结果（内部使用，同 data class SingleTestResult 结构） */
    private data class ComprehensiveTestResult(
        val keyword: String,
        val testName: String,
        val detected: Boolean,
        val detectedKeyword: String,
        val decodes: Int,
        val durationMs: Long
    )

    private fun pcm16ToFloat(data: ByteArray, length: Int): FloatArray {
        val sampleCount = length / AudioFormatSpec.FRAME_SIZE
        val result = FloatArray(sampleCount)
        var i = 0
        var idx = 0
        while (i + 1 < length) {
            val low = data[i].toInt() and 0xff
            val high = data[i + 1].toInt()
            val sample = (high shl 8) or low
            result[idx] = (sample / 32768.0f).coerceIn(-1f, 1f)
            i += 2
            idx++
        }
        return result
    }

    /**
     * 模型管道自检：生成合成音频并送入 KWS 引擎，验证解码管道是否正常工作。
     * 此测试独立于麦克风，用于区分"模型/代码问题"和"麦克风输入问题"。
     */
    private fun runModelPipelineTest(onLog: (String) -> Unit) {
        val spotter = keywordSpotter ?: return
        var testStream: OnlineStream? = null
        try {
            testStream = spotter.createStream()
            // 生成 0.8 秒高斯白噪声，模拟语音级别信号（RMS≈0.1）
            val numSamples = (AudioFormatSpec.SAMPLE_RATE * 0.8).toInt()
            val random = java.util.Random(42)
            val testAudio = FloatArray(numSamples) {
                (random.nextGaussian() * 0.1).toFloat().coerceIn(-1f, 1f)
            }
            testStream.acceptWaveform(testAudio, AudioFormatSpec.SAMPLE_RATE)
            var decodes = 0
            while (spotter.isReady(testStream)) {
                spotter.decode(testStream)
                decodes++
            }
            val result = spotter.getResult(testStream)
            val keyword = result.keyword
            val pipelineOk = decodes > 0
            onLog(
                "模型管道自检: 合成音频(0.8s)解码次数=$decodes, " +
                "检测关键词='${keyword.ifBlank { "(无)" }}', " +
                "管道状态=${if (pipelineOk) "正常✓" else "异常✗"}"
            )
            if (!pipelineOk) {
                onLog("模型管道自检警告: 合成音频未能触发解码，模型可能有异常")
            }
        } catch (e: Throwable) {
            onLog("模型管道自检异常: ${e.message}")
        } finally {
            runCatching { testStream?.release() }
        }
    }

    /**
     * TTS 合成自检：用 Android TTS 引擎合成唤醒词音频，直接送入 KWS 模型检测。
     * 此测试完全绕开麦克风，验证"唤醒词->模型->检测"的完整链路。
     * 在后台线程运行，不影响正常录音监听。
     */
    private fun runTtsSpeechTest(keyword: String, onLog: (String) -> Unit) {
        val spotter = keywordSpotter ?: run {
            onLog("TTS合成自检: KeywordSpotter 不可用，跳过")
            return
        }

        onLog("TTS合成自检: 开始合成 '$keyword' 的语音...")

        // Step 1: 初始化 TTS 引擎
        val initLatch = CountDownLatch(1)
        var ttsReady = false
        var tts: TextToSpeech? = null

        mainHandler.post {
            tts = TextToSpeech(context) { status ->
                ttsReady = (status == TextToSpeech.SUCCESS)
                initLatch.countDown()
            }
        }

        if (!initLatch.await(12, TimeUnit.SECONDS) || !ttsReady) {
            tts?.shutdown()
            onLog("TTS合成自检: TTS 引擎初始化失败，准备重试...")

            val retryLatch = CountDownLatch(1)
            var retryReady = false
            var retryTts: TextToSpeech? = null
            mainHandler.post {
                retryTts = TextToSpeech(context) { status ->
                    retryReady = (status == TextToSpeech.SUCCESS)
                    retryLatch.countDown()
                }
            }
            if (!retryLatch.await(12, TimeUnit.SECONDS) || !retryReady) {
                onLog("TTS合成自检: TTS 引擎初始化失败或超时（已重试）")
                retryTts?.shutdown()
                return
            }
            tts = retryTts
        }

        val localTts = tts ?: run {
            onLog("TTS合成自检: TTS 对象为null")
            return
        }

        // Step 2: 设置中文语言
        val langResult = localTts.setLanguage(Locale.CHINESE)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            onLog("TTS合成自检: 设备不支持中文TTS(code=$langResult)")
            localTts.shutdown()
            return
        }
        // 降低语速使发音更清晰
        localTts.setSpeechRate(0.85f)

        // Step 3: 合成语音到 WAV 文件
        val wavFile = File(context.cacheDir, "kws_tts_test_${System.currentTimeMillis()}.wav")
        val synthLatch = CountDownLatch(1)
        var synthOk = false

        localTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                synthOk = true
                synthLatch.countDown()
            }
            @Deprecated("Use onError(String?, Int)")
            override fun onError(utteranceId: String?) {
                synthLatch.countDown()
            }
        })

        @Suppress("DEPRECATION")
        localTts.synthesizeToFile(keyword, null, wavFile, "kws_tts_test")

        if (!synthLatch.await(15, TimeUnit.SECONDS) || !synthOk) {
            onLog("TTS合成自检: 语音合成失败或超时")
            localTts.shutdown()
            wavFile.delete()
            return
        }

        onLog("TTS合成自检: WAV文件生成完成, 大小=${wavFile.length()} 字节")
        localTts.shutdown()

        // Step 4: 读取 WAV 文件并转为 16kHz float PCM
        val audioSamples = readWavAsPcmFloat16k(wavFile, onLog)
        wavFile.delete()

        if (audioSamples == null || audioSamples.isEmpty()) {
            onLog("TTS合成自检: WAV 文件读取或转换失败")
            return
        }

        val durationSec = audioSamples.size.toFloat() / AudioFormatSpec.SAMPLE_RATE
        onLog("TTS合成自检: 音频准备完成, ${audioSamples.size} 采样点(${
            "%.2f".format(Locale.ROOT, durationSec)
        }秒), RMS=${
            "%.4f".format(Locale.ROOT, computeRms(audioSamples))
        }")

        // Step 5: 送入 KWS 模型检测（使用独立的 test stream）
        var testStream: OnlineStream? = null
        try {
            testStream = spotter.createStream()

            // 前置 0.3 秒静音
            val preSilence = FloatArray((AudioFormatSpec.SAMPLE_RATE * 0.3).toInt())
            testStream.acceptWaveform(preSilence, AudioFormatSpec.SAMPLE_RATE)

            // 分 chunk 送入（模拟实时送入）
            val chunkSize = AudioFormatSpec.SAMPLE_RATE / 10 // 100ms per chunk
            var offset = 0
            while (offset < audioSamples.size) {
                val end = minOf(offset + chunkSize, audioSamples.size)
                val chunk = audioSamples.copyOfRange(offset, end)
                testStream.acceptWaveform(chunk, AudioFormatSpec.SAMPLE_RATE)
                offset = end
            }

            // 后置 0.8 秒静音（模型需要尾部空白帧触发结果输出）
            val postSilence = FloatArray((AudioFormatSpec.SAMPLE_RATE * 0.8).toInt())
            testStream.acceptWaveform(postSilence, AudioFormatSpec.SAMPLE_RATE)

            // 解码所有可用帧
            var totalDecodes = 0
            while (spotter.isReady(testStream)) {
                spotter.decode(testStream)
                totalDecodes++
            }

            val result = spotter.getResult(testStream)
            val detected = result.keyword.isNotBlank()

            onLog(
                "TTS合成自检结果: 合成'$keyword', 解码次数=$totalDecodes, " +
                "检测到='${result.keyword.ifBlank { "(无)" }}', " +
                "结论=${if (detected) "✓ 模型可从该TTS样本中识别关键词" else "△ 该TTS样本未命中（不代表真人发音无效）"}"
            )

            if (!detected) {
                onLog(
                    "TTS合成自检分析: 未命中仅代表“该TTS样本”与模型不匹配，可能原因: " +
                    "1)TTS合成语音与模型训练语料差异大(模型基于人声训练) " +
                    "2)该唤醒词在此模型(wenetspeech-3.3M)中识别灵敏度较低 " +
                    "3)若真人发音可稳定触发，应以真人结果为准；仅当真人也无法触发时再考虑更换词/模型"
                )
            }
        } catch (e: Throwable) {
            onLog("TTS合成自检检测异常: ${e.message}")
        } finally {
            runCatching { testStream?.release() }
        }
    }

    /**
     * 读取 WAV 文件并转为 16kHz 单声道 Float PCM。
     * 支持 8/16/32 位 PCM，任意采样率（自动线性插值重采样到 16kHz）。
     */
    private fun readWavAsPcmFloat16k(wavFile: File, onLog: (String) -> Unit): FloatArray? {
        try {
            val raf = RandomAccessFile(wavFile, "r")

            // 验证 RIFF/WAVE 头
            val riffTag = ByteArray(4)
            raf.read(riffTag)
            if (String(riffTag) != "RIFF") {
                onLog("TTS-WAV: 非RIFF格式")
                raf.close()
                return null
            }
            raf.skipBytes(4) // file size
            val waveTag = ByteArray(4)
            raf.read(waveTag)
            if (String(waveTag) != "WAVE") {
                onLog("TTS-WAV: 非WAVE格式")
                raf.close()
                return null
            }

            var sampleRate = 0
            var channels = 0
            var bitsPerSample = 0
            var dataSize = 0
            var dataOffset = 0L

            // 解析 chunks
            while (raf.filePointer < raf.length()) {
                val chunkId = ByteArray(4)
                if (raf.read(chunkId) < 4) break
                val chunkSizeBytes = ByteArray(4)
                raf.read(chunkSizeBytes)
                val chunkSize = ByteBuffer.wrap(chunkSizeBytes).order(ByteOrder.LITTLE_ENDIAN).int

                when (String(chunkId)) {
                    "fmt " -> {
                        val fmtData = ByteArray(chunkSize.coerceAtMost(40))
                        raf.read(fmtData)
                        val bb = ByteBuffer.wrap(fmtData).order(ByteOrder.LITTLE_ENDIAN)
                        bb.short // audioFormat
                        channels = bb.short.toInt()
                        sampleRate = bb.int
                        bb.int   // byteRate
                        bb.short // blockAlign
                        bitsPerSample = bb.short.toInt()
                        if (chunkSize > fmtData.size) {
                            raf.skipBytes(chunkSize - fmtData.size)
                        }
                    }
                    "data" -> {
                        dataSize = chunkSize
                        dataOffset = raf.filePointer
                        break
                    }
                    else -> {
                        raf.skipBytes(chunkSize)
                    }
                }
            }

            if (sampleRate == 0 || channels == 0 || bitsPerSample == 0 || dataSize == 0) {
                onLog("TTS-WAV: 格式信息不完整(sr=$sampleRate, ch=$channels, bits=$bitsPerSample, dataSize=$dataSize)")
                raf.close()
                return null
            }

            onLog("TTS-WAV: sampleRate=$sampleRate, channels=$channels, bits=$bitsPerSample, dataBytes=$dataSize")

            // 读取 PCM 数据（最多 10 秒）
            raf.seek(dataOffset)
            val maxBytes = sampleRate * channels * (bitsPerSample / 8) * 10
            val rawPcm = ByteArray(dataSize.coerceAtMost(maxBytes))
            val actualRead = raf.read(rawPcm)
            raf.close()

            // 转换为单声道 float
            val bytesPerSample = bitsPerSample / 8
            val frameSize = bytesPerSample * channels
            val numFrames = actualRead / frameSize
            val monoFloat = FloatArray(numFrames)

            val bb = ByteBuffer.wrap(rawPcm).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until numFrames) {
                var sum = 0f
                for (ch in 0 until channels) {
                    val sample = when (bitsPerSample) {
                        16 -> bb.short.toFloat() / 32768f
                        32 -> bb.int.toFloat() / 2147483648f
                        else -> (bb.get().toInt() and 0xFF).toFloat() / 128f - 1f
                    }
                    sum += sample
                }
                monoFloat[i] = (sum / channels).coerceIn(-1f, 1f)
            }

            // 如果采样率已是 16kHz 则直接返回
            if (sampleRate == AudioFormatSpec.SAMPLE_RATE) {
                return monoFloat
            }

            // 线性插值重采样到 16kHz
            val ratio = sampleRate.toDouble() / AudioFormatSpec.SAMPLE_RATE.toDouble()
            val newLen = (monoFloat.size / ratio).toInt()
            val resampled = FloatArray(newLen)
            for (i in resampled.indices) {
                val srcIdx = i * ratio
                val srcIdxInt = srcIdx.toInt()
                val frac = (srcIdx - srcIdxInt).toFloat()
                val s0 = monoFloat[srcIdxInt.coerceIn(0, monoFloat.size - 1)]
                val s1 = monoFloat[(srcIdxInt + 1).coerceIn(0, monoFloat.size - 1)]
                resampled[i] = (s0 + frac * (s1 - s0)).coerceIn(-1f, 1f)
            }

            onLog("TTS-WAV: 重采样 $sampleRate -> ${AudioFormatSpec.SAMPLE_RATE}, ${monoFloat.size} -> ${resampled.size} 采样点")
            return resampled

        } catch (e: Throwable) {
            onLog("TTS-WAV 解析异常: ${e.message}")
            return null
        }
    }

    private fun releaseKwsResources() {
        runCatching { stream?.release() }
        stream = null
        runCatching { keywordSpotter?.release() }
        keywordSpotter = null
        runCatching { keywordFile?.delete() }
        keywordFile = null
        totalAudioChunks = 0L
        totalDecodeCalls = 0L
        lastDiagLogMs = 0L
        consecutiveLowEnergyWindows = 0
        lastLowEnergyWarningMs = 0L
        lastGainLogMs = 0L
        sessionMaxRawRms = 0f
        sessionMaxBoostedRms = 0f
        speechDetectedCount = 0
        lastSpeechLogMs = 0L
        autoSwitchInProgress = false
        autoSwitchAttempts = 0
        lastAutoSwitchMs = 0L
        currentTestKeyword = null
    }

    private fun maybeLogRuntimeDiagnostics(
        onLog: (String) -> Unit,
        recentChunks: Int,
        recentRmsSum: Float,
        recentPeakMax: Float
    ): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastDiagLogMs < 5000L || recentChunks <= 0) {
            return false
        }
        lastDiagLogMs = now
        val avgRms = recentRmsSum / recentChunks.toFloat()
        val isLowEnergy = avgRms < 0.003f && recentPeakMax < 0.01f
        consecutiveLowEnergyWindows = if (isLowEnergy) {
            consecutiveLowEnergyWindows + 1
        } else {
            0
        }

        onLog(
            "监听诊断: chunks=$totalAudioChunks, decodes=$totalDecodeCalls, windowChunks=$recentChunks, avgRms=${"%.4f".format(Locale.ROOT, avgRms)}, peak=${"%.4f".format(Locale.ROOT, recentPeakMax)}, maxRawRms=${"%.6f".format(Locale.ROOT, sessionMaxRawRms)}"
        )

        if (consecutiveLowEnergyWindows >= 3 && now - lastLowEnergyWarningMs >= 15000L) {
            lastLowEnergyWarningMs = now
            val emulatorHint = audioRecorder.getEmulatorAudioHint()
            if (emulatorHint != null) {
                onLog("音频输入电平持续为零。$emulatorHint")
            } else {
                onLog("音频输入电平持续过低，疑似麦克风无输入/被系统静音/被其他应用占用；请检查录音权限、系统麦克风开关并贴近麦克风重试")
            }
        }
        return true
    }

    private fun maybeAutoSwitchAudioSource(onLog: (String) -> Unit) {
        if (!running || autoSwitchInProgress) {
            return
        }
        if (consecutiveLowEnergyWindows < 3) {
            return
        }
        if (autoSwitchAttempts >= 6) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastAutoSwitchMs < 15000L) {
            return
        }

        val listener = audioDataListener ?: return
        autoSwitchInProgress = true
        autoSwitchAttempts++
        lastAutoSwitchMs = now

        onLog("检测到持续低电平，尝试自动切换音频源(第${autoSwitchAttempts}次)...")

        Thread {
            val switched = runCatching {
                audioRecorder.restartWithNextSource(listener)
            }.getOrElse { throwable ->
                mainHandler.post {
                    onLog("自动切换音频源异常: ${throwable.message ?: throwable.javaClass.simpleName}")
                }
                false
            }

            mainHandler.post {
                autoSwitchInProgress = false
                if (!running) {
                    return@post
                }
                if (switched) {
                    consecutiveLowEnergyWindows = 0
                    onLog("自动切换音频源成功，当前source=${audioRecorder.getActiveAudioSourceName()}，继续监听")
                    val keywordForRetest = currentTestKeyword
                    if (!keywordForRetest.isNullOrBlank() && autoSwitchAttempts <= 3) {
                        onLog("音频源切换后触发TTS合成复测: '$keywordForRetest'")
                        Thread {
                            runCatching { runTtsSpeechTest(keywordForRetest, onLog) }
                                .onFailure { throwable ->
                                    mainHandler.post {
                                        onLog("切源后TTS复测异常: ${throwable.message ?: throwable.javaClass.simpleName}")
                                    }
                                }
                        }.apply {
                            name = "KWS-TTS-Retest"
                            isDaemon = true
                            start()
                        }
                    }
                } else {
                    onLog("自动切换音频源失败，将继续使用当前音频链路")
                }
            }
        }.apply {
            name = "KWS-AudioSource-Switch"
            isDaemon = true
            start()
        }
    }

    private fun computeRms(samples: FloatArray): Float {
        if (samples.isEmpty()) {
            return 0f
        }
        var sum = 0.0
        samples.forEach { value ->
            sum += (value * value).toDouble()
        }
        return kotlin.math.sqrt(sum / samples.size.toDouble()).toFloat()
    }

    private fun computePeak(samples: FloatArray): Float {
        var peak = 0f
        samples.forEach { value ->
            val abs = kotlin.math.abs(value)
            if (abs > peak) {
                peak = abs
            }
        }
        return peak
    }

    private fun buildKeywordLinesForFile(keywords: List<String>, validTokens: Set<String>): List<String> {
        // 注意：中文唤醒词在 sherpa-onnx KWS 中并不是“直接放汉字触发”，
        // 官方要求是先转为 ppinyin token（例如：j iǎng y ǒu b ó @蒋友伯）。
        // 因此这里保留并强化“中文 -> ppinyin token”的转换逻辑，避免后续误认为是错误实现。
        val lines = linkedSetOf<String>()
        keywords
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { raw ->
                val normalizedKeyword = raw.replace(Regex("\\s+"), "_")
                val candidates = linkedSetOf<List<String>>()

                val hanTokens = raw
                    .toCharArray()
                    .map { it.toString().trim() }
                    .filter { it.isNotBlank() }
                if (hanTokens.isNotEmpty()) {
                    candidates.add(hanTokens)
                }

                val pinyinSyllables = normalizeKeywordForEngine(raw, keepTone = true)
                    .split(Regex("\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                if (pinyinSyllables.isNotEmpty()) {
                    candidates.add(normalizePinyinSyllables(pinyinSyllables, keepTone = true))
                    val ppinyinTokens = pinyinSyllables.flatMap { splitPinyinSyllable(it) }
                    if (ppinyinTokens.isNotEmpty()) {
                        candidates.add(ppinyinTokens)
                    }
                }

                val pinyinNoToneSyllables = normalizeKeywordForEngine(raw, keepTone = false)
                    .split(Regex("\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                if (pinyinNoToneSyllables.isNotEmpty()) {
                    candidates.add(normalizePinyinSyllables(pinyinNoToneSyllables, keepTone = false))
                    val pinyinNoToneTokens = pinyinNoToneSyllables.flatMap { splitPinyinSyllable(it) }
                    if (pinyinNoToneTokens.isNotEmpty()) {
                        candidates.add(pinyinNoToneTokens)
                    }
                }

                val debugCandidates = mutableListOf<Pair<String, Boolean>>()
                candidates.forEach { tokenList ->
                    addKeywordLineIfValid(
                        lines = lines,
                        tokenList = tokenList,
                        validTokens = validTokens,
                        normalizedKeyword = normalizedKeyword,
                        debugCandidates = debugCandidates
                    )
                }
                android.util.Log.d("KWS-Keywords", "关键词候选 '$raw': ${debugCandidates.joinToString(" | ") { (repr, ok) ->
                    val missingTokens = if (!ok) {
                        val tokens = repr.split(" ")
                        tokens.filter { it !in validTokens }.take(3).joinToString(",")
                    } else ""
                    if (ok) "[✓] $repr" else "[✗] $repr (缺:$missingTokens)"
                }}")
            }

        return lines.toList()
    }

    private fun expandKeywordsForRuntime(inputKeywords: List<String>): List<String> {
        val expanded = linkedSetOf<String>()
        inputKeywords
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { keyword ->
                expanded.add(keyword)
                extractChineseSuffixKeyword(keyword)?.let { expanded.add(it) }
            }
        return expanded.toList()
    }

    private fun addKeywordLineIfValid(
        lines: MutableSet<String>,
        tokenList: List<String>,
        validTokens: Set<String>,
        normalizedKeyword: String,
        debugCandidates: MutableList<Pair<String, Boolean>>
    ) {
        if (tokenList.isEmpty()) {
            return
        }
        val repr = tokenList.joinToString(" ")
        val allMatch = tokenList.all { it in validTokens }
        debugCandidates.add(repr to allMatch)
        if (allMatch) {
            lines.add("$repr :8.0 @$normalizedKeyword")
            return
        }
        val firstToken = tokenList.first()
        if (firstToken.startsWith("▁")) {
            return
        }
        val boundaryToken = "▁$firstToken"
        if (boundaryToken !in validTokens) {
            return
        }
        val withBoundary = tokenList.toMutableList()
        withBoundary[0] = boundaryToken
        val withBoundaryRepr = withBoundary.joinToString(" ")
        val boundaryMatch = withBoundary.all { it in validTokens }
        debugCandidates.add(withBoundaryRepr to boundaryMatch)
        if (boundaryMatch) {
            lines.add("$withBoundaryRepr :8.0 @$normalizedKeyword")
        }
    }

    private fun extractChineseSuffixKeyword(keyword: String): String? {
        val chineseChars = keyword.filter { it.code in 0x4E00..0x9FFF }
        if (chineseChars.length < 3) {
            return null
        }
        return chineseChars.takeLast(2)
    }

    private fun splitPinyinSyllable(syllable: String): List<String> {
        val token = syllable.trim().lowercase(Locale.ROOT)
        if (token.isBlank()) {
            return emptyList()
        }
        val initials = listOf("zh", "ch", "sh", "b", "p", "m", "f", "d", "t", "n", "l", "g", "k", "h", "j", "q", "x", "r", "z", "c", "s", "y", "w")
        val initial = initials.firstOrNull { token.startsWith(it) }
        return if (initial == null || token.length == initial.length) {
            listOf(token)
        } else {
            listOf(initial, token.substring(initial.length))
        }
    }

    private fun normalizePinyinSyllables(syllables: List<String>, keepTone: Boolean): List<String> {
        return syllables
            .asSequence()
            .map { Normalizer.normalize(it.trim(), Normalizer.Form.NFC) }
            .map { item ->
                if (keepTone) item else {
                    Normalizer.normalize(item, Normalizer.Form.NFD)
                        .replace(Regex("\\p{M}+"), "")
                }
            }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun loadValidTokens(tokensFile: File): Set<String> {
        return runCatching {
            tokensFile.readLines()
                .asSequence()
                .mapIndexed { idx, line -> if (idx == 0) line.trimStart('\uFEFF') else line }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    line.split(Regex("\\s+"), limit = 2)
                        .firstOrNull()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }
                .toSet()
        }.getOrElse { emptySet() }
    }

    private fun normalizeKeywordForEngine(keyword: String, keepTone: Boolean): String {
        // 这里的归一化用于稳定中文拼音（含声调）的 token 形态，
        // 与官方 text2token 生成 keywords.txt 的目标一致，属于必要预处理。
        val transliterated = runCatching {
            hanToLatinTransliterator.transliterate(keyword)
        }.getOrDefault(keyword)

        val normalizedTransliterated = Normalizer.normalize(transliterated, Normalizer.Form.NFC)

        val processed = if (keepTone) {
            normalizedTransliterated
        } else {
            Normalizer.normalize(normalizedTransliterated, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
        }

        val allowedPattern = if (keepTone) "[^\\p{L}\\p{M}\\p{N}\\s]" else "[^\\p{L}\\p{N}\\s]"
        val normalized = processed
            .replace(Regex(allowedPattern), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase(Locale.ROOT)

        return normalized
    }

    private fun computeAdaptiveGain(rawRms: Float): Float {
        if (rawRms <= 0f) {
            return 1f
        }
        val targetRms = 0.03f
        val maxGain = 80f
        return (targetRms / rawRms).coerceIn(1f, maxGain)
    }

    private fun amplifySamples(samples: FloatArray, gain: Float): FloatArray {
        val result = FloatArray(samples.size)
        for (i in samples.indices) {
            result[i] = (samples[i] * gain).coerceIn(-1f, 1f)
        }
        return result
    }

    private fun maybeLogGain(onLog: (String) -> Unit, gain: Float, rawRms: Float, boostedRms: Float) {
        val now = System.currentTimeMillis()
        if (gain <= 1.1f || now - lastGainLogMs < 5000L) {
            return
        }
        lastGainLogMs = now
        onLog(
            "输入增益已启用: gain=${"%.1f".format(Locale.ROOT, gain)}, rawRms=${"%.4f".format(Locale.ROOT, rawRms)}, boostedRms=${"%.4f".format(Locale.ROOT, boostedRms)}"
        )
    }

    private fun writeKeywordsFile(lines: List<String>): File {
        val file = File(context.cacheDir, "kws_runtime_keywords.txt")
        file.parentFile?.mkdirs()
        file.writeText(lines.joinToString("\n") + "\n")
        return file
    }

    private fun loadKeywordsFileLines(file: File): List<String> {
        if (!file.exists() || !file.isFile || file.length() <= 0L) {
            return emptyList()
        }
        return runCatching {
            file.readLines()
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filterNot { it.startsWith("#") }
                .toList()
        }.getOrElse { emptyList() }
    }

    private fun extractMappedKeywords(lines: List<String>): Set<String> {
        return lines
            .asSequence()
            .mapNotNull { line ->
                val idx = line.lastIndexOf('@')
                if (idx < 0 || idx >= line.length - 1) {
                    null
                } else {
                    line.substring(idx + 1).trim().takeIf { it.isNotBlank() }
                }
            }
            .toSet()
    }

    private fun findPredefinedKeywordsFile(modelDir: File): File? {
        val candidates = listOf(
            File(modelDir, "keywords.txt"),
            File(modelDir, "test_keywords.txt"),
            File(modelDir, "test_wavs/keywords.txt"),
            File(modelDir, "test_wavs/test_keywords.txt")
        )
        return candidates.firstOrNull { it.exists() && it.isFile && it.length() > 0 }
    }

    private fun logFileState(label: String, file: File, onLog: (String) -> Unit) {
        val exists = file.exists()
        val size = if (exists) file.length() else -1L
        onLog("文件检查[$label]: path=${file.absolutePath}, exists=$exists, size=$size")
    }
}
