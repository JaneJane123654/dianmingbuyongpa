package com.classroomassistant.utils.audio;

/**
 * 音频处理工具类 (Audio Utilities)
 *
 * <p>提供音频格式转换、音量计算、采样率转换等基础音频处理功能。
 * 核心逻辑包括将 16-bit PCM 原始字节转换为适合模型输入的浮点数数组。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public final class AudioUtils {

    private AudioUtils() {
        // 工具类禁止实例化
    }

    /**
     * 将 16-bit PCM（小端序）字节数组转换为归一化后的浮点数组
     * <p>转换逻辑：将两个连续字节组合为一个 16 位整数（short），然后除以 32768.0 使其范围落在 [-1.0, 1.0] 之间。
     *
     * <p>使用示例：
     * <pre>
     * byte[] pcmData = ...;
     * float[] normalizedSamples = AudioUtils.pcmToFloat(pcmData);
     * </pre>
     *
     * @param pcm 原始 16-bit PCM 字节数据，长度应为偶数
     * @return 归一化后的浮点数组。如果输入字节数为奇数，最后一个不完整的采样将被忽略。
     * @throws IllegalArgumentException 如果 pcm 为 null
     */
    public static float[] pcmToFloat(byte[] pcm) {
        if (pcm == null) {
            throw new IllegalArgumentException("PCM 数据不能为空");
        }
        if (pcm.length == 0) {
            return new float[0];
        }
        int samples = pcm.length / 2;
        float[] result = new float[samples];
        for (int i = 0; i < samples; i++) {
            int low = pcm[i * 2] & 0xFF;
            int high = pcm[i * 2 + 1];
            short sample = (short) ((high << 8) | low);
            result[i] = sample / 32768.0f;
        }
        return result;
    }
}
