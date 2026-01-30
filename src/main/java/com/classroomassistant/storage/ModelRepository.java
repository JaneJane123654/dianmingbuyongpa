package com.classroomassistant.storage;

import com.classroomassistant.utils.AppPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 模型文件仓库
 *
 * <p>负责统一管理模型路径与完整性检查，模型路径必须使用绝对路径。
 */
public class ModelRepository {

    private final AppPaths appPaths;
    private final ConfigManager configManager;

    public ModelRepository(AppPaths appPaths, ConfigManager configManager) {
        this.appPaths = Objects.requireNonNull(appPaths);
        this.configManager = Objects.requireNonNull(configManager);
    }

    public Path getModelsDir() {
        return configManager.getModelsDir().toAbsolutePath();
    }

    public Path getKwsModelDir() {
        return getModelsDir().resolve("sherpa-onnx-kws");
    }

    public Path getAsrModelDir() {
        return getModelsDir().resolve("sherpa-onnx-asr");
    }

    public Path getVadModelFile() {
        return getModelsDir().resolve("sherpa-onnx-vad").resolve("silero_vad.onnx");
    }

    public ModelCheckResult checkRequiredModels(boolean requireSherpaModels) {
        if (!requireSherpaModels) {
            return new ModelCheckResult(true, List.of());
        }

        List<String> missing = new ArrayList<>();
        if (!Files.isDirectory(getKwsModelDir())) {
            missing.add("KWS 模型目录: " + getKwsModelDir());
        }
        if (!Files.isDirectory(getAsrModelDir())) {
            missing.add("ASR 模型目录: " + getAsrModelDir());
        }
        if (!Files.exists(getVadModelFile())) {
            missing.add("VAD 模型文件: " + getVadModelFile());
        }
        return new ModelCheckResult(missing.isEmpty(), missing);
    }
}

