package com.classroomassistant.audio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * WAV 文件写入器
 */
public final class WavWriter {

    private WavWriter() {
    }

    /**
     * 写入 WAV 文件
     *
     * @param target 目标文件
     * @param pcm PCM 数据
     * @param format 音频格式
     * @throws IOException IO 异常
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
