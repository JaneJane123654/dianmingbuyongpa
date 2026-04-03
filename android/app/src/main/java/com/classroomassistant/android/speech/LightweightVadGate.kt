package com.classroomassistant.android.speech

import com.classroomassistant.core.audio.AudioFormatSpec
import kotlin.math.max
import kotlin.math.sqrt

internal data class VadRange(
    val startByte: Int,
    val endByteExclusive: Int
)

internal data class VadGateResult(
    val filteredPcm16: ByteArray,
    val hasSpeech: Boolean,
    val originalDurationMs: Int,
    val keptDurationMs: Int,
    val speechRatio: Float,
    val maxRms: Float,
    val noiseFloorRms: Float,
    val speechThresholdRms: Float,
    val segmentCount: Int
)

internal class LightweightVadGate {

    private val frameSamples = (AudioFormatSpec.SAMPLE_RATE / 1000) * FRAME_MS
    private val bytesPerSample = AudioFormatSpec.FRAME_SIZE

    fun filter(pcm16: ByteArray): VadGateResult {
        if (pcm16.isEmpty()) {
            return VadGateResult(
                filteredPcm16 = ByteArray(0),
                hasSpeech = false,
                originalDurationMs = 0,
                keptDurationMs = 0,
                speechRatio = 0f,
                maxRms = 0f,
                noiseFloorRms = 0f,
                speechThresholdRms = 0f,
                segmentCount = 0
            )
        }

        val sampleCount = pcm16.size / bytesPerSample
        if (sampleCount <= 0) {
            return VadGateResult(
                filteredPcm16 = ByteArray(0),
                hasSpeech = false,
                originalDurationMs = 0,
                keptDurationMs = 0,
                speechRatio = 0f,
                maxRms = 0f,
                noiseFloorRms = 0f,
                speechThresholdRms = 0f,
                segmentCount = 0
            )
        }

        val frameCount = ((sampleCount + frameSamples - 1) / frameSamples).coerceAtLeast(1)
        val frameRms = FloatArray(frameCount)
        val frameStartBytes = IntArray(frameCount)
        val frameEndBytes = IntArray(frameCount)

        var maxRms = 0f
        for (frameIndex in 0 until frameCount) {
            val startSample = frameIndex * frameSamples
            val endSample = (startSample + frameSamples).coerceAtMost(sampleCount)
            val startByte = startSample * bytesPerSample
            val endByte = endSample * bytesPerSample
            val rms = computeRms(pcm16, startByte, endByte)
            frameRms[frameIndex] = rms
            frameStartBytes[frameIndex] = startByte
            frameEndBytes[frameIndex] = endByte
            if (rms > maxRms) {
                maxRms = rms
            }
        }

        val noiseFloor = percentile(frameRms, NOISE_PERCENTILE)
        val speechThreshold = computeSpeechThreshold(noiseFloor, maxRms)
        if (maxRms < ABS_MIN_SPEECH_RMS * PEAK_MIN_MULTIPLIER) {
            val durationMs = durationMsFromSamples(sampleCount)
            return VadGateResult(
                filteredPcm16 = ByteArray(0),
                hasSpeech = false,
                originalDurationMs = durationMs,
                keptDurationMs = 0,
                speechRatio = 0f,
                maxRms = maxRms,
                noiseFloorRms = noiseFloor,
                speechThresholdRms = speechThreshold,
                segmentCount = 0
            )
        }

        val detectedSegments = detectSpeechSegments(frameRms, speechThreshold)
        if (detectedSegments.isEmpty()) {
            val durationMs = durationMsFromSamples(sampleCount)
            return VadGateResult(
                filteredPcm16 = ByteArray(0),
                hasSpeech = false,
                originalDurationMs = durationMs,
                keptDurationMs = 0,
                speechRatio = 0f,
                maxRms = maxRms,
                noiseFloorRms = noiseFloor,
                speechThresholdRms = speechThreshold,
                segmentCount = 0
            )
        }

        val paddedRanges = detectedSegments.map { segment ->
            val paddedStartFrame = (segment.first - PRE_PADDING_FRAMES).coerceAtLeast(0)
            val paddedEndFrame = (segment.last + POST_PADDING_FRAMES).coerceAtMost(frameCount - 1)
            VadRange(
                startByte = frameStartBytes[paddedStartFrame],
                endByteExclusive = frameEndBytes[paddedEndFrame]
            )
        }
        val mergedRanges = mergeRanges(paddedRanges)

        val keptBytes = mergedRanges.sumOf { range ->
            (range.endByteExclusive - range.startByte).coerceAtLeast(0)
        }
        if (keptBytes <= 0) {
            val durationMs = durationMsFromSamples(sampleCount)
            return VadGateResult(
                filteredPcm16 = ByteArray(0),
                hasSpeech = false,
                originalDurationMs = durationMs,
                keptDurationMs = 0,
                speechRatio = 0f,
                maxRms = maxRms,
                noiseFloorRms = noiseFloor,
                speechThresholdRms = speechThreshold,
                segmentCount = 0
            )
        }

        val output = if (keptBytes >= pcm16.size) {
            pcm16
        } else {
            ByteArray(keptBytes).also { dst ->
                var offset = 0
                for (range in mergedRanges) {
                    val length = (range.endByteExclusive - range.startByte).coerceAtLeast(0)
                    if (length <= 0) {
                        continue
                    }
                    System.arraycopy(pcm16, range.startByte, dst, offset, length)
                    offset += length
                }
            }
        }

        val originalDurationMs = durationMsFromSamples(sampleCount)
        val keptDurationMs = durationMsFromSamples(output.size / bytesPerSample)
        val speechRatio = if (originalDurationMs <= 0) {
            0f
        } else {
            keptDurationMs.toFloat() / originalDurationMs.toFloat()
        }
        return VadGateResult(
            filteredPcm16 = output,
            hasSpeech = true,
            originalDurationMs = originalDurationMs,
            keptDurationMs = keptDurationMs,
            speechRatio = speechRatio.coerceIn(0f, 1f),
            maxRms = maxRms,
            noiseFloorRms = noiseFloor,
            speechThresholdRms = speechThreshold,
            segmentCount = detectedSegments.size
        )
    }

    private fun detectSpeechSegments(
        frameRms: FloatArray,
        threshold: Float
    ): List<IntRange> {
        if (frameRms.isEmpty()) {
            return emptyList()
        }

        val segments = mutableListOf<IntRange>()
        var segmentStart = -1
        var lastSpeechFrame = -1
        var consecutiveSpeech = 0

        for (index in frameRms.indices) {
            val isSpeech = frameRms[index] >= threshold
            if (isSpeech) {
                consecutiveSpeech++
                if (segmentStart < 0 && consecutiveSpeech >= MIN_SPEECH_ONSET_FRAMES) {
                    segmentStart = index - MIN_SPEECH_ONSET_FRAMES + 1
                }
                if (segmentStart >= 0) {
                    lastSpeechFrame = index
                }
            } else {
                consecutiveSpeech = 0
                if (segmentStart >= 0 && lastSpeechFrame >= segmentStart) {
                    val silenceFrames = index - lastSpeechFrame
                    if (silenceFrames > MAX_INTERNAL_SILENCE_FRAMES) {
                        if ((lastSpeechFrame - segmentStart + 1) >= MIN_SEGMENT_FRAMES) {
                            segments += segmentStart..lastSpeechFrame
                        }
                        segmentStart = -1
                        lastSpeechFrame = -1
                    }
                }
            }
        }

        if (segmentStart >= 0 && lastSpeechFrame >= segmentStart) {
            if ((lastSpeechFrame - segmentStart + 1) >= MIN_SEGMENT_FRAMES) {
                segments += segmentStart..lastSpeechFrame
            }
        }

        return segments
    }

    private fun mergeRanges(ranges: List<VadRange>): List<VadRange> {
        if (ranges.isEmpty()) {
            return emptyList()
        }

        val sorted = ranges.sortedBy { it.startByte }
        val merged = ArrayList<VadRange>(sorted.size)
        var current = sorted.first()

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startByte <= current.endByteExclusive) {
                current = VadRange(
                    startByte = current.startByte,
                    endByteExclusive = max(current.endByteExclusive, next.endByteExclusive)
                )
            } else {
                merged += current
                current = next
            }
        }
        merged += current
        return merged
    }

    private fun computeSpeechThreshold(noiseFloor: Float, maxRms: Float): Float {
        val byNoise = noiseFloor * NOISE_SCALE + NOISE_OFFSET
        val byPeak = maxRms * PEAK_SCALE
        return max(max(byNoise, byPeak), ABS_MIN_SPEECH_RMS).coerceAtMost(ABS_MAX_SPEECH_RMS)
    }

    private fun computeRms(pcm16: ByteArray, startByte: Int, endByteExclusive: Int): Float {
        val validStart = startByte.coerceAtLeast(0)
        val validEnd = endByteExclusive.coerceAtMost(pcm16.size)
        val sampleCount = ((validEnd - validStart) / bytesPerSample).coerceAtLeast(0)
        if (sampleCount == 0) {
            return 0f
        }

        var sumSquare = 0.0
        var cursor = validStart
        repeat(sampleCount) {
            val low = pcm16[cursor].toInt() and 0xff
            val high = pcm16[cursor + 1].toInt()
            val sample = (high shl 8) or low
            val normalized = sample / 32768.0
            sumSquare += normalized * normalized
            cursor += bytesPerSample
        }
        return sqrt(sumSquare / sampleCount).toFloat()
    }

    private fun percentile(values: FloatArray, percentile: Float): Float {
        if (values.isEmpty()) {
            return 0f
        }
        if (values.size == 1) {
            return values[0]
        }
        val sorted = values.copyOf()
        sorted.sort()
        val clamped = percentile.coerceIn(0f, 1f)
        val index = ((sorted.size - 1) * clamped).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }

    private fun durationMsFromSamples(samples: Int): Int {
        if (samples <= 0) {
            return 0
        }
        return ((samples * 1000L) / AudioFormatSpec.SAMPLE_RATE).toInt()
    }

    private companion object {
        private const val FRAME_MS = 20
        private const val NOISE_PERCENTILE = 0.20f

        private const val ABS_MIN_SPEECH_RMS = 0.0045f
        private const val ABS_MAX_SPEECH_RMS = 0.05f
        private const val NOISE_SCALE = 1.90f
        private const val NOISE_OFFSET = 0.0010f
        private const val PEAK_SCALE = 0.10f
        private const val PEAK_MIN_MULTIPLIER = 1.10f

        private const val MIN_SPEECH_ONSET_FRAMES = 2
        private const val MIN_SEGMENT_FRAMES = 2
        private const val MAX_INTERNAL_SILENCE_FRAMES = 12
        private const val PRE_PADDING_FRAMES = 5
        private const val POST_PADDING_FRAMES = 8
    }
}
