package com.classroomassistant.shared.domain.model

data class AudioConfig(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val frameMillis: Int,
    val defaultLookbackSeconds: Int,
) {
    companion object {
        const val DEFAULT_SAMPLE_RATE = 16000
        const val DEFAULT_CHANNELS = 1
        const val DEFAULT_BITS_PER_SAMPLE = 16
        const val DEFAULT_FRAME_MILLIS = 20
        const val DEFAULT_LOOKBACK_SECONDS = 240

        val Default = AudioConfig(
            sampleRate = DEFAULT_SAMPLE_RATE,
            channels = DEFAULT_CHANNELS,
            bitsPerSample = DEFAULT_BITS_PER_SAMPLE,
            frameMillis = DEFAULT_FRAME_MILLIS,
            defaultLookbackSeconds = DEFAULT_LOOKBACK_SECONDS,
        )
    }
}
