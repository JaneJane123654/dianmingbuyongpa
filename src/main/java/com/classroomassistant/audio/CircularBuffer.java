package com.classroomassistant.audio;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 环形缓冲区
 *
 * <p>用于存储固定容量的 PCM 音频字节，支持覆盖写入与回溯读取。
 */
public class CircularBuffer {

    private final byte[] buffer;
    private final ReentrantLock lock = new ReentrantLock();

    private int writeIndex;
    private int size;

    /**
     * 构造环形缓冲区
     *
     * @param capacityBytes 容量（字节）
     */
    public CircularBuffer(int capacityBytes) {
        if (capacityBytes <= 0) {
            throw new IllegalArgumentException("缓冲区容量必须大于 0");
        }
        this.buffer = new byte[capacityBytes];
    }

    /**
     * 写入字节数据（超出容量将覆盖最旧数据）
     *
     * @param data 音频字节数据
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
     * 读取最新 N 字节数据
     *
     * @param lengthBytes 读取长度（字节）
     * @return 最新音频数据
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
     * 当前缓冲区内已写入的字节数
     *
     * @return 已写入字节数
     */
    public int getSizeBytes() {
        lock.lock();
        try {
            return size;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 缓冲区容量（字节）
     *
     * @return 容量
     */
    public int getCapacityBytes() {
        return buffer.length;
    }

    /**
     * 清空缓冲区
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
}
