package com.classroomassistant.android.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import com.classroomassistant.core.audio.AudioFormatSpec
import com.classroomassistant.core.platform.PlatformAudioRecorder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android 音频录制器实现
 * 使用 AudioRecord API
 */
class AndroidAudioRecorder(private val context: Context) : PlatformAudioRecorder {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private var activeAudioSourceName: String = "UNKNOWN"
    private var activeSourceIndex: Int = -1
    private var sourceCandidates: List<Pair<Int, String>> = emptyList()
    private val ringBufferLock = Any()
    private val maxLookbackBytes = AudioFormatSpec.SAMPLE_RATE * AudioFormatSpec.FRAME_SIZE * 300
    private val ringBuffer = ByteArray(maxLookbackBytes)
    private var ringWritePos = 0
    private var ringTotalWritten = 0L

    private val bufferSize = AudioRecord.getMinBufferSize(
        AudioFormatSpec.SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(AudioFormatSpec.BUFFER_SIZE)

    override fun start(listener: PlatformAudioRecorder.AudioDataListener): Boolean {
        val preferredIndex = if (activeSourceIndex >= 0) activeSourceIndex else 0
        return startWithPreferredIndex(listener, preferredIndex)
    }

    fun restartWithNextSource(listener: PlatformAudioRecorder.AudioDataListener): Boolean {
        if (sourceCandidates.isEmpty()) {
            sourceCandidates = buildSourceCandidates()
        }
        if (sourceCandidates.isEmpty()) {
            listener.onError("无可用音频源")
            return false
        }

        val currentIndex = if (activeSourceIndex >= 0) {
            activeSourceIndex
        } else {
            0
        }

        val nextIndex = if (activeSourceIndex >= 0) {
            (activeSourceIndex + 1) % sourceCandidates.size
        } else {
            0
        }

        stop()
        val switched = startWithPreferredIndex(listener, nextIndex)
        if (switched) {
            return true
        }

        // 切换失败时尽量回退到上一可用音频源，避免引擎处于“看似运行、实际无录音”的状态。
        listener.onError("切换音频源失败，尝试回退到上一音频源")
        return startWithPreferredIndex(listener, currentIndex.coerceIn(0, sourceCandidates.size - 1))
    }

    override fun stop() {
        isRecording.set(false)

        val currentRecord = audioRecord
        if (currentRecord != null) {
            try {
                if (currentRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    currentRecord.stop()
                }
            } catch (_: IllegalStateException) {
            }
            currentRecord.release()
        }
        audioRecord = null

        try {
            recordingThread?.join(1000)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        recordingThread = null
    }

    override fun isRecording(): Boolean = isRecording.get()

    override fun release() {
        stop()
    }

    fun getAudioBefore(seconds: Int): ByteArray {
        val safeSeconds = seconds.coerceIn(1, 300)
        val bytesNeeded = safeSeconds * AudioFormatSpec.SAMPLE_RATE * AudioFormatSpec.FRAME_SIZE
        synchronized(ringBufferLock) {
            val available = ringTotalWritten.coerceAtMost(maxLookbackBytes.toLong()).toInt()
            if (available <= 0) {
                return ByteArray(0)
            }
            val readSize = bytesNeeded.coerceAtMost(available)
            val start = (ringWritePos - readSize + maxLookbackBytes) % maxLookbackBytes
            val result = ByteArray(readSize)
            if (start + readSize <= maxLookbackBytes) {
                System.arraycopy(ringBuffer, start, result, 0, readSize)
            } else {
                val firstPart = maxLookbackBytes - start
                System.arraycopy(ringBuffer, start, result, 0, firstPart)
                System.arraycopy(ringBuffer, 0, result, firstPart, readSize - firstPart)
            }
            return result
        }
    }

    fun getActiveAudioSourceName(): String = activeAudioSourceName

    private fun startWithPreferredIndex(
        listener: PlatformAudioRecorder.AudioDataListener,
        preferredIndex: Int
    ): Boolean {
        if (isRecording.get()) {
            return false
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            listener.onError("没有录音权限")
            return false
        }

        try {
            synchronized(ringBufferLock) {
                ringWritePos = 0
                ringTotalWritten = 0L
            }
            if (sourceCandidates.isEmpty()) {
                sourceCandidates = buildSourceCandidates()
            }
            if (sourceCandidates.isEmpty()) {
                listener.onError("无可用音频源")
                return false
            }

            var initializedRecord: AudioRecord? = null
            var initializedSourceName = "UNKNOWN"
            var initializedSourceIndex = -1
            val triedSources = mutableListOf<String>()
            val safePreferredIndex = preferredIndex.coerceIn(0, sourceCandidates.size - 1)

            for (offset in sourceCandidates.indices) {
                val index = (safePreferredIndex + offset) % sourceCandidates.size
                val (source, sourceName) = sourceCandidates[index]
                try {
                    val candidate = AudioRecord(
                        source,
                        AudioFormatSpec.SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                    if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                        initializedRecord = candidate
                        initializedSourceName = sourceName
                        initializedSourceIndex = index
                        break
                    }
                    candidate.release()
                    triedSources.add("$sourceName(未初始化)")
                } catch (e: Exception) {
                    triedSources.add("$sourceName(异常:${e.message?.take(30)})")
                }
            }

            if (triedSources.isNotEmpty()) {
                android.util.Log.w("AudioRecorder", "跳过的音频源: ${triedSources.joinToString(", ")}")
            }

            audioRecord = initializedRecord
            activeAudioSourceName = initializedSourceName
            activeSourceIndex = initializedSourceIndex

            if (audioRecord == null) {
                listener.onError("AudioRecord 初始化失败")
                return false
            }

            audioRecord?.startRecording()
            if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                listener.onError("AudioRecord 启动失败")
                audioRecord?.release()
                audioRecord = null
                return false
            }
            isRecording.set(true)

            recordingThread = Thread {
                val buffer = ByteArray(bufferSize)
                var hasReadErrorReported = false
                try {
                    while (isRecording.get()) {
                        val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (bytesRead > 0) {
                            appendToRingBuffer(buffer, bytesRead)
                            listener.onAudioData(buffer, bytesRead)
                        } else if (bytesRead < 0 && !hasReadErrorReported) {
                            hasReadErrorReported = true
                            listener.onError("AudioRecord 读取失败: code=$bytesRead")
                        }
                    }
                } catch (e: Exception) {
                    listener.onError("录音线程异常: ${e.message}")
                }
            }.apply {
                name = "AudioRecorder-Thread"
                start()
            }

            return true
        } catch (e: Exception) {
            listener.onError("启动录音失败: ${e.message}")
            return false
        }
    }

    private fun appendToRingBuffer(data: ByteArray, length: Int) {
        if (length <= 0) {
            return
        }
        val safeLength = length.coerceAtMost(data.size)
        synchronized(ringBufferLock) {
            if (safeLength >= maxLookbackBytes) {
                System.arraycopy(data, safeLength - maxLookbackBytes, ringBuffer, 0, maxLookbackBytes)
                ringWritePos = 0
                ringTotalWritten += safeLength.toLong()
                return
            }
            val firstPart = (maxLookbackBytes - ringWritePos).coerceAtMost(safeLength)
            System.arraycopy(data, 0, ringBuffer, ringWritePos, firstPart)
            val secondPart = safeLength - firstPart
            if (secondPart > 0) {
                System.arraycopy(data, firstPart, ringBuffer, 0, secondPart)
            }
            ringWritePos = (ringWritePos + safeLength) % maxLookbackBytes
            ringTotalWritten += safeLength.toLong()
        }
    }

    private fun buildSourceCandidates(): List<Pair<Int, String>> {
        return if (isLikelyEmulator()) {
            listOf(
                MediaRecorder.AudioSource.MIC to "MIC",
                MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION",
                MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
                MediaRecorder.AudioSource.UNPROCESSED to "UNPROCESSED"
            )
        } else {
            listOf(
                MediaRecorder.AudioSource.MIC to "MIC",
                MediaRecorder.AudioSource.UNPROCESSED to "UNPROCESSED",
                MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
                MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION"
            )
        }
    }

    /**
     * 查询系统音频输入设备信息，用于诊断麦克风问题。
     * 返回可读的设备信息列表。
     */
    fun queryAudioInputDevices(): List<String> {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return listOf("AudioManager不可用")
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            if (devices.isEmpty()) {
                return listOf("未检测到任何音频输入设备")
            }
            devices.map { device ->
                val typeName = when (device.type) {
                    AudioDeviceInfo.TYPE_BUILTIN_MIC -> "内置麦克风"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET -> "有线耳机麦克风"
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "蓝牙SCO"
                    AudioDeviceInfo.TYPE_USB_DEVICE -> "USB设备"
                    AudioDeviceInfo.TYPE_USB_HEADSET -> "USB耳机"
                    AudioDeviceInfo.TYPE_TELEPHONY -> "电话"
                    AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "远程子混音"
                    else -> "类型${device.type}"
                }
                "$typeName(id=${device.id}, channels=${device.channelCounts.contentToString()}, " +
                    "sampleRates=${device.sampleRates.contentToString()})"
            }
        } catch (e: Exception) {
            listOf("查询音频输入设备异常: ${e.message}")
        }
    }

    /**
     * 检查模拟器环境下是否配置了音频输入。
     * Android 模拟器默认禁用麦克风输入，需要手动在扩展控件中启用。
     */
    fun getEmulatorAudioHint(): String? {
        if (!isLikelyEmulator()) return null
        return "检测到模拟器环境。Android 模拟器默认禁用麦克风输入，" +
            "请在模拟器的 Extended Controls（扩展控件）> Microphone 中，" +
            "开启 \"Virtual microphone uses host audio input\"（虚拟麦克风使用主机音频输入）开关。" +
            "同时确保电脑本机麦克风正常工作且未被静音。"
    }

    fun isEmulator(): Boolean = isLikelyEmulator()

    private fun isLikelyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()

        return fingerprint.contains("generic") ||
            fingerprint.contains("emulator") ||
            model.contains("sdk") ||
            model.contains("emulator") ||
            brand.startsWith("generic") ||
            device.contains("emulator") ||
            product.contains("sdk") ||
            product.contains("emulator")
    }
}
