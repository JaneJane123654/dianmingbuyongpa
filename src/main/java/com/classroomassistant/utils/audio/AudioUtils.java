package com.classroomassistant.utils.audio;

/**
 * 音频工具
 *
 * <p>提供 PCM 转浮点等基础工具。
 */
public final class AudioUtils {

    private AudioUtils() {
    }

    /**
     * 将 16-bit PCM（小端序）转换为浮点数组
     *
     * @param pcm 16-bit PCM 数据
     * @return 浮点数组（-1.0~1.0）
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
