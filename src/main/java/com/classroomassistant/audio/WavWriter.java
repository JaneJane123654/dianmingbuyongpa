package com.classroomassistant.audio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * WAV 文件写入器 (WAV File Writer Utility)
 *
 * <p>该类提供静态工具方法，用于将原始 PCM (Pulse Code Modulation) 音频数据保存为标准的 RIFF/WAVE 格式文件。
 * 它负责生成正确的 WAV 文件头（包括 RIFF 标识、格式块、数据块等），确保生成的音频文件能被标准播放器识别。
 *
 * <p>主要功能：
 * <ul>
 *   <li>自动创建目标文件所在的目录。</li>
 *   <li>生成 44 字节的标准 WAV 文件头。</li>
 *   <li>支持 16-bit PCM 数据的写入。</li>
 *   <li>处理小端序 (Little-Endian) 字节序转换，符合 WAV 规范。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public final class WavWriter {

    private WavWriter() {
        // 工具类私有构造函数，防止实例化
    }

    /**
     * 将 PCM 数据写入指定的 WAV 文件
     *
     * <p>如果目标文件的父目录不存在，将自动创建。
     * 写入过程采用流式操作，先写入 44 字节头部，紧接着写入原始 PCM 数据。
     *
     * @param target 目标文件路径，不能为 null
     * @param pcm    原始 PCM 音频数据，字节数组格式，不能为 null
     * @param format 音频格式规范，用于确定采样率、声道数等头部信息，不能为 null
     * @throws IOException 如果文件创建失败、磁盘空间不足或写入过程中发生错误
     * @throws NullPointerException 如果任一参数为 null
     */
    public static void write(Path target, byte[] pcm, AudioFormatSpec format) throws IOException {
        Objects.requireNonNull(target, "目标文件不能为空");
        Objects.requireNonNull(pcm, "PCM 数据不能为空");
        Objects.requireNonNull(format, "音频格式不能为空");

        Files.createDirectories(target.getParent());
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            writeHeader(outputStream, pcm.length, format);
            outputStream.write(pcm);
        }
    }

    /**
     * 写入标准的 44 字节 WAV 文件头
     *
     * <p>WAV 头部结构 (RIFF/WAVE):
     * <pre>
     * Offset  Size  Name             Description
     * 0       4     ChunkID          "RIFF"
     * 4       4     ChunkSize        36 + SubChunk2Size
     * 8       4     Format           "WAVE"
     * 12      4     SubChunk1ID      "fmt "
     * 16      4     SubChunk1Size    16 for PCM
     * 20      2     AudioFormat      1 for PCM
     * 22      2     NumChannels      Mono = 1, Stereo = 2
     * 24      4     SampleRate       e.g. 16000, 44100
     * 28      4     ByteRate         SampleRate * NumChannels * BitsPerSample/8
     * 32      2     BlockAlign       NumChannels * BitsPerSample/8
     * 34      2     BitsPerSample    8, 16, etc.
     * 36      4     SubChunk2ID      "data"
     * 40      4     SubChunk2Size    NumSamples * NumChannels * BitsPerSample/8
     * </pre>
     *
     * @param outputStream 目标输出流
     * @param dataLength   PCM 数据的总长度（字节）
     * @param format       音频格式参数
     * @throws IOException 如果写入流失败
     */
    private static void writeHeader(OutputStream outputStream, int dataLength, AudioFormatSpec format) throws IOException {
        int byteRate = format.bytesPerSecond();
        int blockAlign = format.channels() * format.bitsPerSample() / 8;
        int chunkSize = 36 + dataLength;

        writeString(outputStream, "RIFF");
        writeIntLE(outputStream, chunkSize);
        writeString(outputStream, "WAVE");
        writeString(outputStream, "fmt ");
        writeIntLE(outputStream, 16);
        writeShortLE(outputStream, (short) 1);
        writeShortLE(outputStream, (short) format.channels());
        writeIntLE(outputStream, format.sampleRate());
        writeIntLE(outputStream, byteRate);
        writeShortLE(outputStream, (short) blockAlign);
        writeShortLE(outputStream, (short) format.bitsPerSample());
        writeString(outputStream, "data");
        writeIntLE(outputStream, dataLength);
    }

    private static void writeString(OutputStream outputStream, String value) throws IOException {
        outputStream.write(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static void writeIntLE(OutputStream outputStream, int value) throws IOException {
        outputStream.write(value & 0xFF);
        outputStream.write((value >> 8) & 0xFF);
        outputStream.write((value >> 16) & 0xFF);
        outputStream.write((value >> 24) & 0xFF);
    }

    private static void writeShortLE(OutputStream outputStream, short value) throws IOException {
        outputStream.write(value & 0xFF);
        outputStream.write((value >> 8) & 0xFF);
    }
}
