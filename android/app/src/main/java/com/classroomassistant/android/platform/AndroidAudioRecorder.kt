package com.classroomassistant.android.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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

    private val bufferSize = AudioRecord.getMinBufferSize(
        AudioFormatSpec.SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(AudioFormatSpec.BUFFER_SIZE)

    override fun start(listener: PlatformAudioRecorder.AudioDataListener): Boolean {
        if (isRecording.get()) {
            return false
        }

        // 检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            listener.onError("没有录音权限")
            return false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                AudioFormatSpec.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                listener.onError("AudioRecord 初始化失败")
                return false
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingThread = Thread {
                val buffer = ByteArray(bufferSize)
                while (isRecording.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        listener.onAudioData(buffer, bytesRead)
                    }
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

    override fun stop() {
        isRecording.set(false)
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        recordingThread?.join(1000)
        recordingThread = null
    }

    override fun isRecording(): Boolean = isRecording.get()

    override fun release() {
        stop()
    }
}
