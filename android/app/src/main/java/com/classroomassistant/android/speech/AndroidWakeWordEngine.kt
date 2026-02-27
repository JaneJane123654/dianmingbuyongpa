package com.classroomassistant.android.speech

import com.classroomassistant.android.platform.AndroidAudioRecorder
import com.classroomassistant.core.audio.AudioFormatSpec
import com.classroomassistant.core.platform.PlatformAudioRecorder
import com.classroomassistant.speech.SherpaOnnxJNI
import java.io.File
import java.lang.System

class AndroidWakeWordEngine(
    private val audioRecorder: AndroidAudioRecorder
) {
    private val jni = SherpaOnnxJNI()
    private var handle: Long = 0
    private var running = false
    private var lastTriggerMs = 0L

    fun start(
        modelDir: File,
        keywords: List<String>,
        onWake: () -> Unit,
        onLog: (String) -> Unit
    ): Boolean {
        if (running) {
            return true
        }
        if (keywords.isEmpty()) {
            onLog("唤醒词为空")
            return false
        }
        try {
            SherpaOnnxJNI.ensureLoaded()
        } catch (e: UnsatisfiedLinkError) {
            onLog("本地引擎加载失败: ${e.message}")
            return false
        }
        handle = jni.initializeKws(modelDir.toPath(), keywords.joinToString(","))
        if (handle == 0L) {
            onLog("本地唤醒词引擎初始化失败")
            return false
        }
        val started = audioRecorder.start(object : PlatformAudioRecorder.AudioDataListener {
            override fun onAudioData(data: ByteArray, length: Int) {
                val floats = pcm16ToFloat(data, length)
                val detected = jni.detectWakeWord(handle, floats, floats.size)
                if (detected) {
                    val now = System.currentTimeMillis()
                    if (now - lastTriggerMs > 1500) {
                        lastTriggerMs = now
                        onWake()
                    }
                }
            }

            override fun onError(message: String) {
                onLog(message)
            }
        })
        running = started
        if (!started) {
            jni.release(handle)
            handle = 0
        }
        return started
    }

    fun stop() {
        if (!running) {
            return
        }
        audioRecorder.stop()
        if (handle != 0L) {
            jni.release(handle)
            handle = 0
        }
        running = false
    }

    fun isRunning(): Boolean = running

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
}
