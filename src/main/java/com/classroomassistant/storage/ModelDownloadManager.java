package com.classroomassistant.storage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模型下载管理器 (Model Download Manager)
 *
 * <p>负责协调模型文件的检测、下载和校验流程。
 * 提供 JavaFX Property 以便 UI 层绑定显示下载进度和状态。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class ModelDownloadManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ModelDownloadManager.class);

    private final ModelRepository modelRepository;
    private final ModelDownloader downloader;
    private final ConfigManager configManager;
    private final List<KwsModelOption> kwsModelOptions = List.of(
        new KwsModelOption(
            "sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01",
            "Zipformer WenetSpeech 3.3M (2024-01-01)",
            "中文唤醒模型，覆盖常见课堂场景，体积适中"
        ),
        new KwsModelOption(
            "sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01",
            "Zipformer GigaSpeech 3.3M (2024-01-01)",
            "英语为主的唤醒模型，适合英文课堂或双语环境"
        ),
        new KwsModelOption(
            "sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20",
            "Zipformer 中英 3M (2025-12-20)",
            "中英双语唤醒模型，优先推荐给混合语言场景"
        )
    );

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "model-download");
        thread.setDaemon(true);
        return thread;
    });

    // UI 可绑定属性
    private final BooleanProperty downloadingProperty = new SimpleBooleanProperty(false);
    private final DoubleProperty progressProperty = new SimpleDoubleProperty(0.0);
    private final StringProperty statusTextProperty = new SimpleStringProperty("就绪");
    private final StringProperty currentModelProperty = new SimpleStringProperty("");

    /**
     * 构造下载管理器
     *
     * @param modelRepository 模型仓库
     * @param configManager   配置管理器
     */
    public ModelDownloadManager(ModelRepository modelRepository, ConfigManager configManager) {
        this.modelRepository = Objects.requireNonNull(modelRepository, "模型仓库不能为空");
        this.configManager = Objects.requireNonNull(configManager, "配置管理器不能为空");
        this.downloader = new ModelDownloader();
    }

    /**
     * 检查并返回缺失的模型列表
     *
     * @return 缺失的模型描述列表
     */
    public List<ModelDescriptor> checkMissingModels() {
        List<ModelDescriptor> missing = new ArrayList<>();
        String baseUrl = configManager.getModelDownloadBaseUrl();

        // KWS 模型
        Path kwsDir = modelRepository.getKwsModelDir();
        if (!kwsDir.toFile().exists() || !kwsDir.resolve("tokens.txt").toFile().exists()) {
            String kwsModel = configManager.getKwsModelName();
            missing.add(new ModelDescriptor(
                "KWS 唤醒词模型",
                URI.create(baseUrl + "/kws-models/" + kwsModel + ".tar.bz2"),
                configManager.getCacheDir().resolve(kwsModel + ".tar.bz2"),
                null, null,
                modelRepository.getKwsModelDir(kwsModel)
            ));
        }

        // ASR 模型
        Path asrDir = modelRepository.getAsrModelDir();
        if (!asrDir.toFile().exists() || !asrDir.resolve("tokens.txt").toFile().exists()) {
            String asrModel = configManager.getAsrModelName();
            missing.add(new ModelDescriptor(
                "ASR 语音识别模型",
                URI.create(baseUrl + "/asr-models/" + asrModel + ".tar.bz2"),
                configManager.getCacheDir().resolve(asrModel + ".tar.bz2"),
                null, null,
                modelRepository.getAsrModelDir()
            ));
        }

        // VAD 模型
        Path vadFile = modelRepository.getVadModelFile();
        if (!vadFile.toFile().exists()) {
            missing.add(new ModelDescriptor(
                "VAD 静音检测模型",
                URI.create(baseUrl + "/vad-models/" + configManager.getVadModelName()),
                vadFile,
                null, null
            ));
        }

        return missing;
    }

    /**
     * 异步下载所有缺失的模型
     *
     * @param callback 下载完成回调（成功/失败）
     */
    public void downloadMissingModels(DownloadCallback callback) {
        List<ModelDescriptor> missing = checkMissingModels();
        if (missing.isEmpty()) {
            updateStatus("所有模型已就绪");
            if (callback != null) {
                Platform.runLater(() -> callback.onComplete(true, "所有模型已就绪"));
            }
            return;
        }

        executor.submit(() -> {
            downloader.reset();
            Platform.runLater(() -> downloadingProperty.set(true));
            boolean allSuccess = true;
            StringBuilder errorMsg = new StringBuilder();

            for (int i = 0; i < missing.size(); i++) {
                ModelDescriptor model = missing.get(i);
                int index = i;
                int total = missing.size();

                Platform.runLater(() -> {
                    currentModelProperty.set(model.name());
                    statusTextProperty.set("正在下载 (" + (index + 1) + "/" + total + "): " + model.name());
                });

                try {
                    downloadModel(model);
                } catch (Exception e) {
                    allSuccess = false;
                    errorMsg.append(model.name()).append(": ").append(e.getMessage()).append("\n");
                    logger.error("下载模型失败: {}", model.name(), e);
                }
            }

            boolean finalSuccess = allSuccess;
            String finalMsg = allSuccess ? "所有模型下载完成" : "部分模型下载失败:\n" + errorMsg;
            Platform.runLater(() -> {
                downloadingProperty.set(false);
                progressProperty.set(1.0);
                statusTextProperty.set(finalMsg);
                currentModelProperty.set("");
                if (callback != null) {
                    callback.onComplete(finalSuccess, finalMsg);
                }
            });
        });
    }

    /**
     * 同步下载单个模型
     */
    private void downloadModel(ModelDescriptor model) throws Exception {
        final Object lock = new Object();
        final Exception[] error = {null};

        downloader.download(model, new ModelDownloader.DownloadListener() {
            @Override
            public void onProgress(String name, long downloadedBytes, long totalBytes) {
                double progress = totalBytes > 0 ? (double) downloadedBytes / totalBytes : 0;
                Platform.runLater(() -> progressProperty.set(progress));
            }

            @Override
            public void onFinished(String name, Path targetPath) {
                synchronized (lock) {
                    lock.notify();
                }
            }

            @Override
            public void onError(String name, String errorMessage) {
                synchronized (lock) {
                    error[0] = new RuntimeException(errorMessage);
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            lock.wait(300000); // 最长等待5分钟
        }

        if (error[0] != null) {
            throw error[0];
        }
        postProcessModel(model);
    }

    private void updateStatus(String text) {
        Platform.runLater(() -> statusTextProperty.set(text));
    }

    // Property Getters
    public BooleanProperty downloadingProperty() {
        return downloadingProperty;
    }

    public DoubleProperty progressProperty() {
        return progressProperty;
    }

    public StringProperty statusTextProperty() {
        return statusTextProperty;
    }

    public StringProperty currentModelProperty() {
        return currentModelProperty;
    }

    private void postProcessModel(ModelDescriptor model) throws IOException {
        String url = model.downloadUrl().toString().toLowerCase();
        if (!url.endsWith(".tar.bz2") && !url.endsWith(".tar.gz") && !url.endsWith(".tgz")) {
            return;
        }
        Path extractDir = model.extractDir();
        if (extractDir == null) {
            extractDir = resolveExtractDir(model);
        }
        if (extractDir == null) {
            return;
        }
        deleteRecursively(extractDir);
        Files.createDirectories(extractDir);
        extractArchive(model.targetPath(), extractDir);
        Files.deleteIfExists(model.targetPath());
    }

    private Path resolveExtractDir(ModelDescriptor model) {
        String name = model.name();
        if (name.contains("KWS") || name.contains("唤醒")) {
            return modelRepository.getKwsModelDir();
        }
        if (name.contains("ASR") || name.contains("识别")) {
            return modelRepository.getAsrModelDir();
        }
        return null;
    }

    private void extractArchive(Path archive, Path targetDir) throws IOException {
        String fileName = archive.getFileName().toString().toLowerCase();
        try (InputStream fileInput = Files.newInputStream(archive);
             BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
             InputStream compressorInput = openCompressorStream(bufferedInput, fileName);
             TarArchiveInputStream tarInput = new TarArchiveInputStream(compressorInput)) {
            TarArchiveEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = tarInput.getNextTarEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("非法文件路径: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    continue;
                }
                Path parent = entryPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (OutputStream outputStream = Files.newOutputStream(entryPath)) {
                    int read;
                    while ((read = tarInput.read(buffer)) >= 0) {
                        outputStream.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private InputStream openCompressorStream(BufferedInputStream bufferedInput, String fileName) throws IOException {
        if (fileName.endsWith(".tar.bz2") || fileName.endsWith(".tbz2")) {
            return new BZip2CompressorInputStream(bufferedInput);
        }
        if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
            return new GzipCompressorInputStream(bufferedInput);
        }
        return bufferedInput;
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        List<Path> paths;
        try (Stream<Path> stream = Files.walk(path)) {
            paths = stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        }
        for (Path item : paths) {
            Files.deleteIfExists(item);
        }
    }


    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 下载完成回调接口（简单版）
     */
    @FunctionalInterface
    public interface DownloadCallback {
        void onComplete(boolean success, String message);
    }

    /**
     * 详细下载回调接口
     */
    public interface DetailedDownloadCallback {
        void onProgress(String modelName, double progress, long downloadedBytes, long totalBytes);
        void onModelComplete(String modelName);
        void onAllComplete();
        void onError(String modelName, String error);
    }

    /**
     * 异步下载指定的模型列表（带详细回调）
     *
     * @param models   要下载的模型列表
     * @param callback 详细下载回调
     */
    public void downloadAllAsync(List<ModelDescriptor> models, DetailedDownloadCallback callback) {
        if (models == null || models.isEmpty()) {
            if (callback != null) {
                Platform.runLater(callback::onAllComplete);
            }
            return;
        }

        executor.submit(() -> {
            downloader.reset();
            Platform.runLater(() -> downloadingProperty.set(true));
            boolean allSuccess = true;

            for (int i = 0; i < models.size(); i++) {
                ModelDescriptor model = models.get(i);
                int index = i;
                int total = models.size();

                Platform.runLater(() -> {
                    currentModelProperty.set(model.name() + " (" + (index + 1) + "/" + total + ")");
                    statusTextProperty.set("正在下载: " + model.name());
                    progressProperty.set(0);
                });

                try {
                    downloadModelWithCallback(model, callback);
                    if (callback != null) {
                        Platform.runLater(() -> callback.onModelComplete(model.name()));
                    }
                } catch (Exception e) {
                    allSuccess = false;
                    if (callback != null) {
                        String error = e.getMessage();
                        Platform.runLater(() -> callback.onError(model.name(), error));
                    }
                    logger.error("下载模型失败: {}", model.name(), e);
                }
            }

            boolean finalSuccess = allSuccess;
            Platform.runLater(() -> {
                downloadingProperty.set(false);
                progressProperty.set(1.0);
                statusTextProperty.set(finalSuccess ? "下载完成" : "部分下载失败");
                currentModelProperty.set("");
                if (callback != null && finalSuccess) {
                    callback.onAllComplete();
                }
            });
        });
    }

    /**
     * 同步下载单个模型（带详细回调）
     */
    private void downloadModelWithCallback(ModelDescriptor model, DetailedDownloadCallback callback) throws Exception {
        final Object lock = new Object();
        final Exception[] error = {null};

        downloader.download(model, new ModelDownloader.DownloadListener() {
            @Override
            public void onProgress(String name, long downloadedBytes, long totalBytes) {
                double progress = totalBytes > 0 ? (double) downloadedBytes / totalBytes : 0;
                Platform.runLater(() -> {
                    progressProperty.set(progress);
                    if (callback != null) {
                        callback.onProgress(name, progress, downloadedBytes, totalBytes);
                    }
                });
            }

            @Override
            public void onFinished(String name, Path targetPath) {
                synchronized (lock) {
                    lock.notify();
                }
            }

            @Override
            public void onError(String name, String errorMessage) {
                synchronized (lock) {
                    error[0] = new RuntimeException(errorMessage);
                    lock.notify();
                }
            }
        });

        synchronized (lock) {
            lock.wait(300000); // 最长等待5分钟
        }

        if (error[0] != null) {
            throw error[0];
        }
        postProcessModel(model);
    }

    /**
     * 取消当前下载任务
     */
    public void cancelDownload() {
        downloader.cancel();
        Platform.runLater(() -> {
            downloadingProperty.set(false);
            statusTextProperty.set("下载已取消");
        });
    }

    public List<KwsModelOption> getKwsModelOptions() {
        return kwsModelOptions;
    }

    public String getDefaultKwsModelId() {
        return kwsModelOptions.isEmpty() ? configManager.getKwsModelName() : kwsModelOptions.get(0).getId();
    }

    public boolean isKwsModelReady(String modelId) {
        return modelRepository.isKwsModelReady(modelId);
    }

    public boolean isAsrModelReady() {
        return modelRepository.isAsrModelReady();
    }

    public boolean isVadModelReady() {
        return modelRepository.isVadModelReady();
    }

    public ModelDescriptor buildKwsModelDescriptor(KwsModelOption option) {
        String baseUrl = configManager.getModelDownloadBaseUrl();
        String modelId = option.getId();
        return new ModelDescriptor(
            option.getName(),
            URI.create(baseUrl + "/kws-models/" + modelId + ".tar.bz2"),
            configManager.getCacheDir().resolve(modelId + ".tar.bz2"),
            null,
            null,
            modelRepository.getKwsModelDir(modelId)
        );
    }

    public ModelDescriptor buildAsrModelDescriptor() {
        String baseUrl = configManager.getModelDownloadBaseUrl();
        String asrModel = configManager.getAsrModelName();
        return new ModelDescriptor(
            "ASR 语音识别模型",
            URI.create(baseUrl + "/asr-models/" + asrModel + ".tar.bz2"),
            configManager.getCacheDir().resolve(asrModel + ".tar.bz2"),
            null,
            null,
            modelRepository.getAsrModelDir()
        );
    }

    public ModelDescriptor buildVadModelDescriptor() {
        String baseUrl = configManager.getModelDownloadBaseUrl();
        String vadModel = configManager.getVadModelName();
        return new ModelDescriptor(
            "VAD 静音检测模型",
            URI.create(baseUrl + "/vad-models/" + vadModel),
            modelRepository.getVadModelFile(),
            null,
            null
        );
    }

    public static final class KwsModelOption {
        private final String id;
        private final String name;
        private final String description;

        public KwsModelOption(String id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
