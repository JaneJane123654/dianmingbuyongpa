package com.classroomassistant.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 模型下载管理器单元测试 (ModelDownloadManager Unit Tests)
 *
 * <p>验证模型检测、下载流程控制和状态管理的正确性。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
class ModelDownloadManagerTest {

    @TempDir
    Path tempDir;

    private MockModelRepository mockModelRepository;
    private MockConfigManager mockConfigManager;
    private ModelDownloadManager downloadManager;

    @BeforeEach
    void setUp() {
        mockModelRepository = new MockModelRepository(tempDir);
        mockConfigManager = new MockConfigManager();
        downloadManager = new ModelDownloadManager(mockModelRepository, mockConfigManager);
    }

    @Test
    @DisplayName("构造函数不接受 null ModelRepository")
    void constructorThrowsOnNullModelRepository() {
        assertThrows(NullPointerException.class, 
            () -> new ModelDownloadManager(null, mockConfigManager));
    }

    @Test
    @DisplayName("构造函数不接受 null ConfigManager")
    void constructorThrowsOnNullConfigManager() {
        assertThrows(NullPointerException.class, 
            () -> new ModelDownloadManager(mockModelRepository, null));
    }

    @Test
    @DisplayName("当所有模型存在时返回空缺失列表")
    void checkMissingModels_allExist_returnsEmpty() throws Exception {
        // 创建所有必需的模型目录和文件
        Path kwsDir = mockModelRepository.getKwsModelDir();
        Files.createDirectories(kwsDir);
        Files.createFile(kwsDir.resolve("tokens.txt"));

        Path asrDir = mockModelRepository.getAsrModelDir();
        Files.createDirectories(asrDir);
        Files.createFile(asrDir.resolve("tokens.txt"));

        Path vadFile = mockModelRepository.getVadModelFile();
        Files.createDirectories(vadFile.getParent());
        Files.createFile(vadFile);

        List<ModelDescriptor> missing = downloadManager.checkMissingModels();
        assertTrue(missing.isEmpty(), "所有模型存在时应返回空列表");
    }

    @Test
    @DisplayName("当 KWS 模型缺失时返回正确的缺失项")
    void checkMissingModels_kwsMissing_returnsKws() throws Exception {
        // 只创建 ASR 和 VAD
        Path asrDir = mockModelRepository.getAsrModelDir();
        Files.createDirectories(asrDir);
        Files.createFile(asrDir.resolve("tokens.txt"));

        Path vadFile = mockModelRepository.getVadModelFile();
        Files.createDirectories(vadFile.getParent());
        Files.createFile(vadFile);

        List<ModelDescriptor> missing = downloadManager.checkMissingModels();
        assertEquals(1, missing.size());
        assertTrue(missing.get(0).name().contains("KWS"));
    }

    @Test
    @DisplayName("当所有模型缺失时返回完整缺失列表")
    void checkMissingModels_allMissing_returnsAll() {
        List<ModelDescriptor> missing = downloadManager.checkMissingModels();
        assertEquals(3, missing.size(), "应返回 KWS、ASR、VAD 三个缺失项");
    }

    @Test
    @DisplayName("downloadingProperty 初始值为 false")
    void downloadingProperty_initiallyFalse() {
        assertFalse(downloadManager.downloadingProperty().get());
    }

    @Test
    @DisplayName("progressProperty 初始值为 0")
    void progressProperty_initiallyZero() {
        assertEquals(0.0, downloadManager.progressProperty().get(), 0.001);
    }

    @Test
    @DisplayName("statusTextProperty 初始值为就绪")
    void statusTextProperty_initiallyReady() {
        assertEquals("就绪", downloadManager.statusTextProperty().get());
    }

    @Test
    @DisplayName("currentModelProperty 初始值为空")
    void currentModelProperty_initiallyEmpty() {
        assertEquals("", downloadManager.currentModelProperty().get());
    }

    @Test
    @DisplayName("close 方法正常执行不抛异常")
    void close_doesNotThrow() {
        assertDoesNotThrow(() -> downloadManager.close());
    }

    @Test
    @DisplayName("缺失模型的描述符包含正确的名称")
    void missingModelDescriptor_hasCorrectName() {
        List<ModelDescriptor> missing = downloadManager.checkMissingModels();
        
        boolean hasKws = missing.stream().anyMatch(m -> m.name().contains("KWS") || m.name().contains("唤醒"));
        boolean hasAsr = missing.stream().anyMatch(m -> m.name().contains("ASR") || m.name().contains("语音识别"));
        boolean hasVad = missing.stream().anyMatch(m -> m.name().contains("VAD") || m.name().contains("静音"));
        
        assertTrue(hasKws, "应包含 KWS 模型");
        assertTrue(hasAsr, "应包含 ASR 模型");
        assertTrue(hasVad, "应包含 VAD 模型");
    }

    @Test
    @DisplayName("缺失模型的描述符包含有效的下载 URL")
    void missingModelDescriptor_hasValidDownloadUrl() {
        List<ModelDescriptor> missing = downloadManager.checkMissingModels();
        
        for (ModelDescriptor model : missing) {
            assertNotNull(model.downloadUrl(), "下载 URL 不应为空");
            assertTrue(model.downloadUrl().toString().startsWith("http"), "应为 HTTP URL");
        }
    }

    @Test
    @DisplayName("缺失模型的描述符包含有效的目标路径")
    void missingModelDescriptor_hasValidTargetPath() {
        List<ModelDescriptor> missing = downloadManager.checkMissingModels();
        
        for (ModelDescriptor model : missing) {
            assertNotNull(model.targetPath(), "目标路径不应为空");
            assertTrue(model.targetPath().isAbsolute(), "应为绝对路径");
        }
    }

    // ================== Mock 类 ==================

    private static class MockModelRepository extends ModelRepository {
        private final Path tempDir;

        MockModelRepository(Path tempDir) {
            super(new com.classroomassistant.utils.AppPaths("Test"), new MockConfigManager());
            this.tempDir = tempDir;
        }

        @Override
        public Path getModelsDir() {
            return tempDir.resolve("models");
        }

        @Override
        public Path getKwsModelDir() {
            return getModelsDir().resolve("sherpa-onnx-kws");
        }

        @Override
        public Path getAsrModelDir() {
            return getModelsDir().resolve("sherpa-onnx-asr");
        }

        @Override
        public Path getVadModelFile() {
            return getModelsDir().resolve("sherpa-onnx-vad").resolve("silero_vad.onnx");
        }
    }

    private static class MockConfigManager extends ConfigManager {
        MockConfigManager() {
            super(new com.classroomassistant.utils.AppPaths("Test"));
        }

        @Override
        public String getModelDownloadBaseUrl() {
            return "https://example.com/models";
        }

        @Override
        public String getKwsModelName() {
            return "sherpa-onnx-kws-test";
        }

        @Override
        public String getAsrModelName() {
            return "sherpa-onnx-asr-test";
        }

        @Override
        public String getVadModelName() {
            return "silero_vad.onnx";
        }

        @Override
        public AudioConfig getAudioConfig() {
            return new AudioConfig(16000, 1, 16, 20, 240);
        }

        @Override
        public VadDefaults getVadDefaults() {
            return new VadDefaults(true, 5);
        }

        @Override
        public AiDefaults getAiDefaults() {
            return new AiDefaults(30, 3, "ERNIE-Bot-turbo");
        }

        @Override
        public String getSpeechEngineDefault() {
            return "FAKE";
        }

        @Override
        public int getTriggerCooldownSeconds() {
            return 10;
        }

        @Override
        public java.nio.file.Path getModelsDir() {
            return java.nio.file.Path.of("models");
        }
    }
}
