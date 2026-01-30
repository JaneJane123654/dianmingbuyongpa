package com.classroomassistant.storage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.classroomassistant.utils.AppPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RecordingRepositoryTest {

    private Path tempDir;

    @AfterEach
    void tearDown() throws Exception {
        if (tempDir != null) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception ignored) {
                    }
                });
        }
    }

    @Test
    void testSaveRecording() throws Exception {
        tempDir = Files.createTempDirectory("ca-test");
        AppPaths appPaths = new AppPaths("ClassroomAssistant") {
            @Override
            public Path getRecordingsDir() {
                return tempDir.resolve("recordings");
            }
        };
        AudioConfig audioConfig = new AudioConfig(16000, 1, 16, 20, 240);
        RecordingRepository repository = new RecordingRepository(appPaths, audioConfig);

        byte[] pcm = new byte[3200];
        Path saved = repository.saveRecording(pcm, "unit");
        assertTrue(Files.exists(saved));
    }
}
