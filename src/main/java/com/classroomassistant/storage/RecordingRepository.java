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
 * 录音仓库 (Recording Repository)
 *
 * <p>负责录音文件的存储管理。主要功能包括：
 * <ul>
 *   <li>将 PCM 音频数据转换为标准的 WAV 格式文件并保存。</li>
 *   <li>根据日期自动创建存储目录。</li>
 *   <li>定期清理过期的录音文件，节省磁盘空间。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class RecordingRepository {

    private static final Logger logger = LoggerFactory.getLogger(RecordingRepository.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AppPaths appPaths;
    private final AudioConfig audioConfig;

    /**
     * 构造录音仓库
     *
     * @param appPaths    路径管理工具，用于确定录音存储根目录
     * @param audioConfig 音频参数配置，用于生成 WAV 文件头
     * @throws NullPointerException 如果任一参数为 null
     */
    public RecordingRepository(AppPaths appPaths, AudioConfig audioConfig) {
        this.appPaths = Objects.requireNonNull(appPaths, "路径配置不能为空");
        this.audioConfig = Objects.requireNonNull(audioConfig, "音频配置不能为空");
    }

    /**
     * 将 PCM 数据保存为 WAV 录音文件
     * <p>文件将保存在以当前日期命名的子目录下。
     *
     * @param pcm        要保存的原始 PCM 字节数据
     * @param namePrefix 文件名可选前缀（例如 "class_math"），如果为空则使用默认值 "recording"
     * @return 返回保存成功后的文件绝对路径 {@link Path}
     * @throws NullPointerException  如果 pcm 为 null
     * @throws IllegalStateException 如果保存过程中发生 IO 错误
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
     * 清理过期的录音文件
     * <p>根据目录名解析日期，并删除所有早于指定天数的录音目录。
     *
     * @param retentionDays 允许保留的最大天数。如果为 0，则只保留当天录音；如果小于 0，则不执行清理。
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

    /**
     * 递归删除目录及其内容
     *
     * @param dir 要删除的目录路径
     */
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
