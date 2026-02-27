package com.classroomassistant.audio;

import static org.junit.jupiter.api.Assertions.*;

import javax.sound.sampled.AudioFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 音频格式规范单元测试 (AudioFormatSpec Unit Tests)
 *
 * <p>验证音频参数计算、格式转换和值对象行为的正确性。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
class AudioFormatSpecTest {

    @Test
    @DisplayName("构造函数正确设置所有参数")
    void constructorSetsAllParameters() {
        AudioFormatSpec spec = new AudioFormatSpec(16000, 1, 16, 20);
        
        assertEquals(16000, spec.sampleRate());
        assertEquals(1, spec.channels());
        assertEquals(16, spec.bitsPerSample());
        assertEquals(20, spec.frameMillis());
    }

    @Test
    @DisplayName("bytesPerSecond 计算单声道 16bit 正确")
    void bytesPerSecond_mono16bit() {
        AudioFormatSpec spec = new AudioFormatSpec(16000, 1, 16, 20);
        
        // 16000 samples/s * 1 channel * 16 bits/sample / 8 bits/byte = 32000 bytes/s
        assertEquals(32000, spec.bytesPerSecond());
    }

    @Test
    @DisplayName("bytesPerSecond 计算双声道 16bit 正确")
    void bytesPerSecond_stereo16bit() {
        AudioFormatSpec spec = new AudioFormatSpec(44100, 2, 16, 20);
        
        // 44100 * 2 * 16 / 8 = 176400 bytes/s
        assertEquals(176400, spec.bytesPerSecond());
    }

    @Test
    @DisplayName("bytesPerSecond 计算单声道 8bit 正确")
    void bytesPerSecond_mono8bit() {
        AudioFormatSpec spec = new AudioFormatSpec(8000, 1, 8, 20);
        
        // 8000 * 1 * 8 / 8 = 8000 bytes/s
        assertEquals(8000, spec.bytesPerSecond());
    }

    @Test
    @DisplayName("frameBytes 计算 20ms 帧正确")
    void frameBytes_20ms() {
        AudioFormatSpec spec = new AudioFormatSpec(16000, 1, 16, 20);
        
        // 32000 bytes/s * 20ms / 1000 = 640 bytes
        assertEquals(640, spec.frameBytes());
    }

    @Test
    @DisplayName("frameBytes 计算 10ms 帧正确")
    void frameBytes_10ms() {
        AudioFormatSpec spec = new AudioFormatSpec(16000, 1, 16, 10);
        
        // 32000 bytes/s * 10ms / 1000 = 320 bytes
        assertEquals(320, spec.frameBytes());
    }

    @Test
    @DisplayName("frameBytes 最小值为 1")
    void frameBytes_minimumIsOne() {
        // 极端情况：非常低的参数可能导致 0，但应返回最小 1
        AudioFormatSpec spec = new AudioFormatSpec(100, 1, 8, 1);
        
        assertTrue(spec.frameBytes() >= 1);
    }

    @Test
    @DisplayName("toAudioFormat 返回正确的 AudioFormat 对象")
    void toAudioFormat_returnsCorrectFormat() {
        AudioFormatSpec spec = new AudioFormatSpec(16000, 1, 16, 20);
        
        AudioFormat format = spec.toAudioFormat();
        
        assertEquals(16000.0f, format.getSampleRate(), 0.001f);
        assertEquals(16, format.getSampleSizeInBits());
        assertEquals(1, format.getChannels());
        assertTrue(format.isBigEndian() == false, "应为小端序");
    }

    @Test
    @DisplayName("toAudioFormat 双声道格式正确")
    void toAudioFormat_stereo() {
        AudioFormatSpec spec = new AudioFormatSpec(44100, 2, 16, 20);
        
        AudioFormat format = spec.toAudioFormat();
        
        assertEquals(44100.0f, format.getSampleRate(), 0.001f);
        assertEquals(2, format.getChannels());
    }

    @Test
    @DisplayName("equals 相同参数返回 true")
    void equals_sameParameters_true() {
        AudioFormatSpec spec1 = new AudioFormatSpec(16000, 1, 16, 20);
        AudioFormatSpec spec2 = new AudioFormatSpec(16000, 1, 16, 20);
        
        assertEquals(spec1, spec2);
    }

    @Test
    @DisplayName("equals 不同参数返回 false")
    void equals_differentParameters_false() {
        AudioFormatSpec spec1 = new AudioFormatSpec(16000, 1, 16, 20);
        AudioFormatSpec spec2 = new AudioFormatSpec(44100, 1, 16, 20);
        
        assertNotEquals(spec1, spec2);
    }

    @Test
    @DisplayName("equals 与 null 返回 false")
    void equals_null_false() {
        AudioFormatSpec spec = new AudioFormatSpec(16000, 1, 16, 20);
        
        assertNotEquals(null, spec);
    }

    @Test
    @DisplayName("equals 与自身返回 true")
    void equals_self_true() {
        AudioFormatSpec spec = new AudioFormatSpec(16000, 1, 16, 20);
        
        assertEquals(spec, spec);
    }

    @Test
    @DisplayName("hashCode 相同参数返回相同值")
    void hashCode_sameParameters_sameValue() {
        AudioFormatSpec spec1 = new AudioFormatSpec(16000, 1, 16, 20);
        AudioFormatSpec spec2 = new AudioFormatSpec(16000, 1, 16, 20);
        
        assertEquals(spec1.hashCode(), spec2.hashCode());
    }

    @Test
    @DisplayName("toString 包含所有参数信息")
    void toString_containsAllParameters() {
        AudioFormatSpec spec = new AudioFormatSpec(16000, 1, 16, 20);
        
        String str = spec.toString();
        
        assertTrue(str.contains("16000"), "应包含采样率");
        assertTrue(str.contains("1") || str.contains("channels"), "应包含声道信息");
        assertTrue(str.contains("16"), "应包含位深");
        assertTrue(str.contains("20"), "应包含帧时长");
    }

    @Test
    @DisplayName("不同声道数的 equals 返回 false")
    void equals_differentChannels_false() {
        AudioFormatSpec mono = new AudioFormatSpec(16000, 1, 16, 20);
        AudioFormatSpec stereo = new AudioFormatSpec(16000, 2, 16, 20);
        
        assertNotEquals(mono, stereo);
    }

    @Test
    @DisplayName("不同位深的 equals 返回 false")
    void equals_differentBitsPerSample_false() {
        AudioFormatSpec bits16 = new AudioFormatSpec(16000, 1, 16, 20);
        AudioFormatSpec bits8 = new AudioFormatSpec(16000, 1, 8, 20);
        
        assertNotEquals(bits16, bits8);
    }

    @Test
    @DisplayName("不同帧时长的 equals 返回 false")
    void equals_differentFrameMillis_false() {
        AudioFormatSpec ms20 = new AudioFormatSpec(16000, 1, 16, 20);
        AudioFormatSpec ms10 = new AudioFormatSpec(16000, 1, 16, 10);
        
        assertNotEquals(ms20, ms10);
    }

    @Test
    @DisplayName("高采样率格式计算正确")
    void highSampleRate_calculatesCorrectly() {
        AudioFormatSpec spec = new AudioFormatSpec(48000, 2, 24, 10);
        
        // 48000 * 2 * 24 / 8 = 288000 bytes/s
        assertEquals(288000, spec.bytesPerSecond());
        
        // 288000 * 10 / 1000 = 2880 bytes
        assertEquals(2880, spec.frameBytes());
    }
}
