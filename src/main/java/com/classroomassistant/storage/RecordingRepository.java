package com.classroomassistant.storage;

import com.classroomassistant.audio.AudioFormatSpec;
import com.classroomassistant.audio.WavWriter;
import com.classroomassistant.utils.AppPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 录音仓库
 *
 * <p>负责保存与清理录音文件。
 */
public class RecordingRepository {

    private static final Logger logger = LoggerFactory.getLogger(RecordingRepository.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AppPaths appPaths;
    private final AudioConfig audioConfig;

    public RecordingRepository(AppPaths appPaths, AudioConfig audioConfig) {
        this.appPaths = Objects.requireNonNull(appPaths, "路径配置不能为空");
        this.audioConfig = Objects.requireNonNull(audioConfig, "音频配置不能为空");
    }

    /**
     * 保存录音
     *
     * @param pcm PCM 数据
     * @param namePrefix 文件前缀
     * @return 保存路径
     */
    public Path saveRecording(byte[] pcm, String namePrefix) {
        Objects.requireNonNull(pcm, "PCM 数据不能为空");
        String safePrefix = namePrefix == null || namePrefix.isBlank() ? "recording" : namePrefix.trim();
        String dateDir = LocalDate.now().format(DATE_FORMATTER);
        Path dir = appPaths.getRecordingsDir().resolve(dateDir);
        String fileName = safePrefix + "_" + System.currentTimeMillis() + ".wav";
        Path target = dir.resolve(fileName);
        try {
            AudioFormatSpec format = new AudioFormatSpec(
                audioConfig.sampleRate(),
                audioConfig.channels(),
                audioConfig.bitsPerSample(),
                audioConfig.frameMillis()
            );
            WavWriter.write(target, pcm, format);
            return target;
        } catch (IOException e) {
            logger.warn("保存录音失败: {}", e.getMessage());
            throw new IllegalStateException("保存录音失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理过期录音
     *
     * @param retentionDays 保留天数
     */
    public void cleanupOldRecordings(int retentionDays) {
        if (retentionDays < 0) {
            return;
        }
        try {
            Files.list(appPaths.getRecordingsDir()).filter(Files::isDirectory).forEach(path -> {
                String name = path.getFileName().toString();
                try {
                    LocalDate date = LocalDate.parse(name, DATE_FORMATTER);
                    if (date.isBefore(LocalDate.now().minusDays(retentionDays))) {
                        deleteDirectory(path);
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (IOException e) {
            logger.warn("清理录音失败: {}", e.getMessage());
        }
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        } catch (IOException ignored) {
        }
    }
}
