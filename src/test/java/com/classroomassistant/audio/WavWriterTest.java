package com.classroomassistant.audio;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * WAV 写入器单元测试 (WavWriter Unit Tests)
 *
 * <p>验证 WAV 文件头生成、PCM 数据写入以及边界条件处理的正确性。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
class WavWriterTest {

    @TempDir
    Path tempDir;

    private AudioFormatSpec format;

    @BeforeEach
    void setUp() {
        format = new AudioFormatSpec(16000, 1, 16, 20);
    }

    @Test
    @DisplayName("写入有效 PCM 数据生成正确的 WAV 文件")
    void writesValidWavFile() throws IOException {
        Path target = tempDir.resolve("test.wav");
        byte[] pcm = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

        WavWriter.write(target, pcm, format);

        assertTrue(Files.exists(target), "文件应被创建");
        byte[] content = Files.readAllBytes(target);
        assertEquals(44 + pcm.length, content.length, "文件长度应为头部(44) + PCM数据长度");
    }

    @Test
    @DisplayName("WAV 文件头包含正确的 RIFF 标识")
    void wavHeaderContainsRiffIdentifier() throws IOException {
        Path target = tempDir.resolve("riff.wav");
        byte[] pcm = new byte[100];

        WavWriter.write(target, pcm, format);

        byte[] content = Files.readAllBytes(target);
        assertEquals('R', content[0]);
        assertEquals('I', content[1]);
        assertEquals('F', content[2]);
        assertEquals('F', content[3]);
    }

    @Test
    @DisplayName("WAV 文件头包含正确的 WAVE 格式标识")
    void wavHeaderContainsWaveFormat() throws IOException {
        Path target = tempDir.resolve("wave.wav");
        byte[] pcm = new byte[100];

        WavWriter.write(target, pcm, format);

        byte[] content = Files.readAllBytes(target);
        assertEquals('W', content[8]);
        assertEquals('A', content[9]);
        assertEquals('V', content[10]);
        assertEquals('E', content[11]);
    }

    @Test
    @DisplayName("WAV 文件头包含正确的 fmt 块")
    void wavHeaderContainsFmtChunk() throws IOException {
        Path target = tempDir.resolve("fmt.wav");
        byte[] pcm = new byte[100];

        WavWriter.write(target, pcm, format);

        byte[] content = Files.readAllBytes(target);
        assertEquals('f', content[12]);
        assertEquals('m', content[13]);
        assertEquals('t', content[14]);
        assertEquals(' ', content[15]);
    }

    @Test
    @DisplayName("WAV 文件头包含正确的 data 块标识")
    void wavHeaderContainsDataChunk() throws IOException {
        Path target = tempDir.resolve("data.wav");
        byte[] pcm = new byte[100];

        WavWriter.write(target, pcm, format);

        byte[] content = Files.readAllBytes(target);
        assertEquals('d', content[36]);
        assertEquals('a', content[37]);
        assertEquals('t', content[38]);
        assertEquals('a', content[39]);
    }

    @Test
    @DisplayName("WAV 文件包含正确的采样率信息")
    void wavHeaderContainsCorrectSampleRate() throws IOException {
        Path target = tempDir.resolve("samplerate.wav");
        byte[] pcm = new byte[100];
        AudioFormatSpec customFormat = new AudioFormatSpec(44100, 2, 16, 20);

        WavWriter.write(target, pcm, customFormat);

        byte[] content = Files.readAllBytes(target);
        // 采样率位于偏移量 24-27，小端序
        int sampleRate = (content[24] & 0xFF) |
                         ((content[25] & 0xFF) << 8) |
                         ((content[26] & 0xFF) << 16) |
                         ((content[27] & 0xFF) << 24);
        assertEquals(44100, sampleRate, "采样率应正确写入");
    }

    @Test
    @DisplayName("WAV 文件包含正确的声道数")
    void wavHeaderContainsCorrectChannels() throws IOException {
        Path target = tempDir.resolve("channels.wav");
        byte[] pcm = new byte[100];
        AudioFormatSpec stereoFormat = new AudioFormatSpec(16000, 2, 16, 20);

        WavWriter.write(target, pcm, stereoFormat);

        byte[] content = Files.readAllBytes(target);
        // 声道数位于偏移量 22-23，小端序
        int channels = (content[22] & 0xFF) | ((content[23] & 0xFF) << 8);
        assertEquals(2, channels, "声道数应正确写入");
    }

    @Test
    @DisplayName("空 PCM 数据生成有效的空 WAV 文件")
    void writesEmptyPcmAsValidWav() throws IOException {
        Path target = tempDir.resolve("empty.wav");
        byte[] pcm = new byte[0];

        WavWriter.write(target, pcm, format);

        assertTrue(Files.exists(target));
        byte[] content = Files.readAllBytes(target);
        assertEquals(44, content.length, "空 PCM 应只生成 44 字节头部");
    }

    @Test
    @DisplayName("自动创建不存在的父目录")
    void createsParentDirectoriesIfMissing() throws IOException {
        Path target = tempDir.resolve("a/b/c/nested.wav");
        byte[] pcm = new byte[] {1, 2, 3, 4};

        WavWriter.write(target, pcm, format);

        assertTrue(Files.exists(target), "嵌套目录中的文件应被创建");
    }

    @Test
    @DisplayName("null 目标路径抛出 NullPointerException")
    void throwsOnNullTarget() {
        byte[] pcm = new byte[10];
        assertThrows(NullPointerException.class, () -> WavWriter.write(null, pcm, format));
    }

    @Test
    @DisplayName("null PCM 数据抛出 NullPointerException")
    void throwsOnNullPcm() {
        Path target = tempDir.resolve("null.wav");
        assertThrows(NullPointerException.class, () -> WavWriter.write(target, null, format));
    }

    @Test
    @DisplayName("null 格式参数抛出 NullPointerException")
    void throwsOnNullFormat() {
        Path target = tempDir.resolve("null.wav");
        byte[] pcm = new byte[10];
        assertThrows(NullPointerException.class, () -> WavWriter.write(target, pcm, null));
    }

    @Test
    @DisplayName("数据块大小字段正确写入")
    void dataChunkSizeIsCorrect() throws IOException {
        Path target = tempDir.resolve("datasize.wav");
        byte[] pcm = new byte[256];

        WavWriter.write(target, pcm, format);

        byte[] content = Files.readAllBytes(target);
        // 数据块大小位于偏移量 40-43，小端序
        int dataSize = (content[40] & 0xFF) |
                       ((content[41] & 0xFF) << 8) |
                       ((content[42] & 0xFF) << 16) |
                       ((content[43] & 0xFF) << 24);
        assertEquals(256, dataSize, "数据块大小应等于 PCM 长度");
    }

    @Test
    @DisplayName("RIFF ChunkSize 字段正确写入 (36 + dataLength)")
    void riffChunkSizeIsCorrect() throws IOException {
        Path target = tempDir.resolve("chunksize.wav");
        byte[] pcm = new byte[100];

        WavWriter.write(target, pcm, format);

        byte[] content = Files.readAllBytes(target);
        // ChunkSize 位于偏移量 4-7，小端序
        int chunkSize = (content[4] & 0xFF) |
                        ((content[5] & 0xFF) << 8) |
                        ((content[6] & 0xFF) << 16) |
                        ((content[7] & 0xFF) << 24);
        assertEquals(36 + 100, chunkSize, "ChunkSize 应等于 36 + PCM 长度");
    }
}
