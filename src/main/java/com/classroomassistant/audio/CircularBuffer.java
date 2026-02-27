package com.classroomassistant.audio;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 环形缓冲区 (Circular Buffer)
 *
 * <p>用于高效存储固定容量的 PCM 音频字节数据。支持并发安全的覆盖写入与回溯读取。
 * 当写入数据超过缓冲区容量时，会自动覆盖最旧的数据，保持缓冲区始终包含最新的音频流。
 *
 * <p>使用示例：
 * <pre>
 * CircularBuffer buffer = new CircularBuffer(16000 * 2 * 10); // 存储 10 秒 16kHz 16bit 音频
 * buffer.write(audioData);
 * byte[] latest3Seconds = buffer.readLatestBytes(16000 * 2 * 3);
 * </pre>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class CircularBuffer {

    private final byte[] buffer;
    private final ReentrantLock lock = new ReentrantLock();

    private int writeIndex;
    private int size;

    /**
     * 构造一个具有指定容量的环形缓冲区
     *
     * @param capacityBytes 缓冲区容量（字节数）
     * @throws IllegalArgumentException 如果 capacityBytes <= 0
     */
    public CircularBuffer(int capacityBytes) {
        if (capacityBytes <= 0) {
            throw new IllegalArgumentException("缓冲区容量必须大于 0");
        }
        this.buffer = new byte[capacityBytes];
    }

    /**
     * 向缓冲区写入音频字节数据
     * <p>如果数据长度超过缓冲区容量，将仅保留数据末尾符合容量的部分。
     * 该操作是线程安全的。
     *
     * @param data 要写入的原始音频字节数组
     * @throws NullPointerException 如果 data 为 null
     */
    public void write(byte[] data) {
        Objects.requireNonNull(data, "写入数据不能为空");
        if (data.length == 0) {
            return;
        }

        lock.lock();
        try {
            if (data.length >= buffer.length) {
                int start = data.length - buffer.length;
                System.arraycopy(data, start, buffer, 0, buffer.length);
                writeIndex = 0;
                size = buffer.length;
                return;
            }

            int remaining = buffer.length - writeIndex;
            if (data.length <= remaining) {
                System.arraycopy(data, 0, buffer, writeIndex, data.length);
            } else {
                System.arraycopy(data, 0, buffer, writeIndex, remaining);
                System.arraycopy(data, remaining, buffer, 0, data.length - remaining);
            }

            writeIndex = (writeIndex + data.length) % buffer.length;
            size = Math.min(buffer.length, size + data.length);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从缓冲区读取最新的 N 字节数据
     * <p>该操作是线程安全的，且不会清除缓冲区内的数据。
     *
     * @param lengthBytes 期望读取的长度（字节数）
     * @return 包含最新音频数据的字节数组。如果缓冲区内可用数据不足，则返回实际可用的所有数据。
     * @throws IllegalArgumentException 如果 lengthBytes <= 0
     */
    public byte[] readLatestBytes(int lengthBytes) {
        if (lengthBytes <= 0) {
            throw new IllegalArgumentException("读取长度必须大于 0");
        }

        lock.lock();
        try {
            int actualLength = Math.min(lengthBytes, size);
            if (actualLength == 0) {
                return new byte[0];
            }

            int startIndex = writeIndex - actualLength;
            if (startIndex < 0) {
                startIndex += buffer.length;
            }

            byte[] result = new byte[actualLength];
            int firstPart = Math.min(actualLength, buffer.length - startIndex);
            System.arraycopy(buffer, startIndex, result, 0, firstPart);
            if (firstPart < actualLength) {
                System.arraycopy(buffer, 0, result, firstPart, actualLength - firstPart);
            }

            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清空缓冲区数据
     */
    public void clear() {
        lock.lock();
        try {
            writeIndex = 0;
            size = 0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取缓冲区当前已存储的数据大小
     *
     * @return 当前存储的字节数
     */
    public int getSize() {
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取缓冲区的总容量
     *
     * @return 总容量（字节数）
     */
    public int getCapacity() {
        return buffer.length;
    }
}
