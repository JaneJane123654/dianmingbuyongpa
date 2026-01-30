package com.classroomassistant.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CircularBufferTest {

    @Test
    void testWriteAndRead() {
        CircularBuffer buffer = new CircularBuffer(8);
        byte[] data = new byte[] {1, 2, 3, 4};
        buffer.write(data);

        byte[] read = buffer.readLatestBytes(4);
        assertArrayEquals(data, read);
        assertEquals(4, buffer.getSizeBytes());
    }

    @Test
    void testOverwrite() {
        CircularBuffer buffer = new CircularBuffer(5);
        buffer.write(new byte[] {1, 2, 3});
        buffer.write(new byte[] {4, 5, 6, 7});

        byte[] read = buffer.readLatestBytes(5);
        assertArrayEquals(new byte[] {3, 4, 5, 6, 7}, read);
    }

    @Test
    void testReadMoreThanAvailable() {
        CircularBuffer buffer = new CircularBuffer(10);
        buffer.write(new byte[] {8, 9, 10});

        byte[] read = buffer.readLatestBytes(20);
        assertArrayEquals(new byte[] {8, 9, 10}, read);
    }

    @Test
    void testWriteWithNullData() {
        CircularBuffer buffer = new CircularBuffer(5);
        assertThrows(NullPointerException.class, () -> buffer.write(null));
    }

    @Test
    void testReadWithInvalidLength() {
        CircularBuffer buffer = new CircularBuffer(5);
        assertThrows(IllegalArgumentException.class, () -> buffer.readLatestBytes(0));
    }
}
