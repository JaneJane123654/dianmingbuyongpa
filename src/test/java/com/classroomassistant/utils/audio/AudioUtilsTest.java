package com.classroomassistant.utils.audio;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AudioUtilsTest {

    @Test
    void testPcmToFloat() {
        byte[] pcm = new byte[] {0, 0, 0, 64};
        float[] floats = AudioUtils.pcmToFloat(pcm);
        assertArrayEquals(new float[] {0.0f, 0.5f}, floats, 0.001f);
    }

    @Test
    void testPcmToFloatWithNull() {
        assertThrows(IllegalArgumentException.class, () -> AudioUtils.pcmToFloat(null));
    }
}
