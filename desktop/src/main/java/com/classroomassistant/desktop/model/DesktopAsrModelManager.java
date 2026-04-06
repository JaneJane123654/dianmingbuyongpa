package com.classroomassistant.desktop.model;

import com.classroomassistant.core.platform.PlatformStorage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * 桌面端 ASR 本地模型管理器。
 */
public class DesktopAsrModelManager {

    public static final String CUSTOM_MODEL_ID = "custom-asr-url-model";

    private static final List<String> REQUIRED_FILES = List.of("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt");

    private static final Map<String, Long> MIN_FILE_SIZE_BYTES = Map.of(
            "encoder.onnx", 16L * 1024L,
            "decoder.onnx", 16L * 1024L,
            "joiner.onnx", 16L * 1024L,
            "tokens.txt", 32L);

    private static final List<DownloadSource> DOWNLOAD_SOURCES = List.of(
            new DownloadSource("GitHub 官方源", "https://github.com/k2-fsa/sherpa-onnx/releases/download"),
            new DownloadSource("中国镜像源(kkgithub)", "https://kkgithub.com/k2-fsa/sherpa-onnx/releases/download"));

    private static final List<AsrModelOption> MODEL_OPTIONS = List.of(
            new AsrModelOption(
                    "sherpa-onnx-streaming-zipformer-small-bilingual-zh-en-2023-02-16",
                    "中英双语极速模型（默认）",
                    "小体积+低延迟优先，适合移动端 3 秒内出结果",
                    "约 40~60 MB"),
            new AsrModelOption(
                    "sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20-mobile",
                    "中英双语均衡模型",
                    "速度与效果平衡，适合课堂实时识别",
                    "约 70~100 MB"),
            new AsrModelOption(
                    "sherpa-onnx-streaming-zipformer-multi-zh-hans-2023-12-12-mobile",
                    "中文增强模型（高精度）",
                    "中文识别效果优先，体积更大，移动端可用",
                    "约 100~150 MB"),
            new AsrModelOption(
                    "sherpa-onnx-streaming-zipformer-zh-int8-2025-06-30",
                    "中文加速模型 INT8",
                    "量化加速，中文识别更快，适合性能较好的手机",
                    "约 50~80 MB"),
            new AsrModelOption(
                    "sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23",
                    "中文小模型 14M（轻量）",
                    "体积较小，优先下载，适合快速启动本机语音识别",
                    "约 14 MB"),
            new AsrModelOption(
                    "sherpa-onnx-streaming-zipformer-en-20M-2023-02-17",
                    "英文小模型 20M",
                    "英文识别优先选择，移动端常用轻量配置",
                    "约 20 MB"),
            new AsrModelOption(
                    "sherpa-onnx-streaming-zipformer-en-2023-06-26-mobile",
                    "英文增强模型（zipformer）",
                    "英文识别效果优先，体积更大，建议在较新设备上使用",
                    "约 80~120 MB"),
            new AsrModelOption(
                    CUSTOM_MODEL_ID,
                    "自定义链接模型 / Custom URL Model",
                    "通过设置页填写下载链接后可下载并使用",
                    "大小取决于下载链接"));

    private final PlatformStorage storage;
    private final OkHttpClient downloadClient;

    public DesktopAsrModelManager(PlatformStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.downloadClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.MINUTES)
                .writeTimeout(3, TimeUnit.MINUTES)
                .callTimeout(20, TimeUnit.MINUTES)
                .build();
    }

    public List<AsrModelOption> getAvailableModels() {
        return MODEL_OPTIONS;
    }

    public String getDefaultModelId() {
        return MODEL_OPTIONS.get(0).id();
    }

    public AsrModelOption getOptionById(String modelId) {
        if (modelId == null) {
            return null;
        }
        for (AsrModelOption option : MODEL_OPTIONS) {
            if (option.id().equals(modelId)) {
                return option;
            }
        }
        return null;
    }

    public File getModelDir(String modelId) {
        String resolvedId = (modelId == null || modelId.isBlank()) ? getDefaultModelId() : modelId.trim();
        File root = new File(storage.getModelsDir(), "sherpa-onnx-asr");
        return new File(root, resolvedId);
    }

    public boolean isModelReady(String modelId) {
        return invalidFiles(getModelDir(modelId)).isEmpty();
    }

    public File downloadAndPrepare(String modelId,
            BiConsumer<Long, Long> onProgress,
            Consumer<String> onEvent) throws IOException {
        String resolvedModelId = resolveModelId(modelId);
        File archive = new File(storage.getCacheDir(), "asr_" + resolvedModelId + "_" + UUID.randomUUID() + ".tar.bz2");
        File tmpArchive = new File(archive.getAbsolutePath() + ".part");
        try {
            downloadArchiveWithFallback(tmpArchive, resolvedModelId, safeProgress(onProgress), safeEvent(onEvent));
            moveTempArchive(tmpArchive, archive, "ASR模型包移动失败");

            File extractDir = new File(storage.getModelsDir(), "asr_extract_" + UUID.randomUUID());
            if (!extractDir.exists() && !extractDir.mkdirs()) {
                throw new IOException("无法创建解压目录: " + extractDir.getAbsolutePath());
            }
            extractTarBz2(archive, extractDir);

            File targetDir = getModelDir(resolvedModelId);
            deleteRecursively(targetDir);
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                throw new IOException("无法创建模型目录: " + targetDir.getAbsolutePath());
            }

            Map<String, File> collected = collectRequiredFiles(extractDir);
            List<String> missing = new ArrayList<>();
            for (String requiredFile : REQUIRED_FILES) {
                if (!collected.containsKey(requiredFile)) {
                    missing.add(requiredFile);
                }
            }
            if (!missing.isEmpty()) {
                throw new IOException("ASR模型文件不完整: " + String.join(", ", missing));
            }

            for (String requiredFile : REQUIRED_FILES) {
                copyRecursively(collected.get(requiredFile), new File(targetDir, requiredFile));
            }

            deleteRecursively(extractDir);

            List<String> stillInvalid = invalidFiles(targetDir);
            if (!stillInvalid.isEmpty()) {
                throw new IOException("ASR模型文件不完整: " + String.join(", ", stillInvalid));
            }
            return targetDir;
        } finally {
            if (archive.exists()) {
                archive.delete();
            }
            if (tmpArchive.exists()) {
                tmpArchive.delete();
            }
        }
    }

    public File downloadAndPrepareFromUrl(String modelId,
            String archiveUrl,
            BiConsumer<Long, Long> onProgress,
            Consumer<String> onEvent) throws IOException {
        String resolvedModelId = resolveModelId(modelId);
        String normalizedUrl = normalizeArchiveUrl(archiveUrl, "请先填写语音识别模型下载链接");

        File archive = new File(storage.getCacheDir(), "asr_custom_" + UUID.randomUUID() + ".tar.bz2");
        File tmpArchive = new File(archive.getAbsolutePath() + ".part");
        Consumer<String> event = safeEvent(onEvent);
        try {
            event.accept("自定义ASR模型下载: 使用外部链接");
            downloadArchiveWithRetry(tmpArchive, normalizedUrl, "自定义链接", safeProgress(onProgress), event);
            moveTempArchive(tmpArchive, archive, "ASR模型包移动失败");

            File extractDir = new File(storage.getModelsDir(), "asr_extract_" + UUID.randomUUID());
            if (!extractDir.exists() && !extractDir.mkdirs()) {
                throw new IOException("无法创建解压目录: " + extractDir.getAbsolutePath());
            }
            extractTarBz2(archive, extractDir);

            File targetDir = getModelDir(resolvedModelId);
            deleteRecursively(targetDir);
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                throw new IOException("无法创建模型目录: " + targetDir.getAbsolutePath());
            }

            Map<String, File> collected = collectRequiredFiles(extractDir);
            List<String> missing = new ArrayList<>();
            for (String requiredFile : REQUIRED_FILES) {
                if (!collected.containsKey(requiredFile)) {
                    missing.add(requiredFile);
                }
            }
            if (!missing.isEmpty()) {
                throw new IOException("ASR模型文件不完整: " + String.join(", ", missing));
            }

            for (String requiredFile : REQUIRED_FILES) {
                copyRecursively(collected.get(requiredFile), new File(targetDir, requiredFile));
            }

            deleteRecursively(extractDir);

            List<String> stillInvalid = invalidFiles(targetDir);
            if (!stillInvalid.isEmpty()) {
                throw new IOException("ASR模型文件不完整: " + String.join(", ", stillInvalid));
            }
            return targetDir;
        } finally {
            if (archive.exists()) {
                archive.delete();
            }
            if (tmpArchive.exists()) {
                tmpArchive.delete();
            }
        }
    }

    private String resolveModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return getDefaultModelId();
        }
        return modelId.trim();
    }

    private String normalizeArchiveUrl(String archiveUrl, String emptyError) {
        String normalizedUrl = archiveUrl == null ? "" : archiveUrl.trim();
        if (normalizedUrl.isBlank()) {
            throw new IllegalArgumentException(emptyError);
        }
        boolean isHttp = normalizedUrl.toLowerCase(Locale.ROOT).startsWith("https://")
                || normalizedUrl.toLowerCase(Locale.ROOT).startsWith("http://");
        if (!isHttp) {
            throw new IllegalArgumentException("下载链接必须以 http:// 或 https:// 开头");
        }
        if (!normalizedUrl.toLowerCase(Locale.ROOT).endsWith(".tar.bz2")) {
            throw new IllegalArgumentException("当前仅支持 .tar.bz2 模型包链接");
        }
        return normalizedUrl;
    }

    private List<String> invalidFiles(File dir) {
        List<String> invalid = new ArrayList<>();
        for (String requiredFile : REQUIRED_FILES) {
            File file = new File(dir, requiredFile);
            if (!file.isFile()) {
                invalid.add(requiredFile);
                continue;
            }
            long minSize = MIN_FILE_SIZE_BYTES.getOrDefault(requiredFile, 1L);
            if (file.length() < minSize) {
                invalid.add(requiredFile);
            }
        }
        return invalid;
    }

    private void downloadArchiveWithFallback(File target,
            String modelId,
            BiConsumer<Long, Long> onProgress,
            Consumer<String> onEvent) throws IOException {
        List<String> sourceErrors = new ArrayList<>();
        for (int index = 0; index < DOWNLOAD_SOURCES.size(); index++) {
            DownloadSource source = DOWNLOAD_SOURCES.get(index);
            String url = source.getBaseUrl() + "/asr-models/" + modelId + ".tar.bz2";
            try {
                onEvent.accept("ASR模型下载源尝试(" + (index + 1) + "/" + DOWNLOAD_SOURCES.size() + "): " + source.getName());
                downloadArchiveWithRetry(target, url, source.getName(), onProgress, onEvent);
                if (index > 0) {
                    onEvent.accept("ASR已切换至" + source.getName() + "并下载成功");
                }
                return;
            } catch (IOException error) {
                String reason = simplifyError(error);
                sourceErrors.add(source.getName() + ": " + reason);
                onEvent.accept("ASR模型下载源失败: " + source.getName() + "，原因: " + reason);
                if (index == 0 && DOWNLOAD_SOURCES.size() > 1) {
                    onEvent.accept("ASR GitHub 下载失败，自动切换到中国镜像源重试");
                }
            }
        }
        throw new IOException("ASR模型下载失败，已尝试全部下载源: " + String.join(" | ", sourceErrors));
    }

    private void downloadArchiveWithRetry(File target,
            String url,
            String sourceName,
            BiConsumer<Long, Long> onProgress,
            Consumer<String> onEvent) throws IOException {
        int maxAttempts = 3;
        IOException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (target.exists()) {
                    target.delete();
                }
                downloadArchive(target, url, onProgress);
                return;
            } catch (IOException error) {
                lastError = error;
                if (attempt < maxAttempts) {
                    onEvent.accept("下载重试: " + sourceName + " 第" + attempt + "次失败，准备第" + (attempt + 1) + "次");
                    sleepSilently(1200L * attempt);
                }
            }
        }
        throw lastError == null ? new IOException("下载失败") : lastError;
    }

    private void downloadArchive(File target,
            String url,
            BiConsumer<Long, Long> onProgress) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (okhttp3.Response response = downloadClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载失败: " + response.code());
            }
            okhttp3.ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("下载内容为空");
            }
            long total = Math.max(body.contentLength(), 0L);
            target.getParentFile().mkdirs();
            try (FileOutputStream output = new FileOutputStream(target);
                    BufferedInputStream input = new BufferedInputStream(body.byteStream())) {
                byte[] buffer = new byte[64 * 1024];
                long downloaded = 0L;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                    downloaded += read;
                    onProgress.accept(downloaded, total);
                }
            }
        }
    }

    private Map<String, File> collectRequiredFiles(File root) {
        List<File> allFiles = collectAllFiles(root);
        Map<String, File> found = new LinkedHashMap<>();

        for (File entry : allFiles) {
            if (entry.getName().equalsIgnoreCase("tokens.txt")) {
                found.put("tokens.txt", entry);
                break;
            }
        }
        File encoder = selectOnnxCandidate(allFiles, "encoder");
        if (encoder != null) {
            found.put("encoder.onnx", encoder);
        }
        File decoder = selectOnnxCandidate(allFiles, "decoder");
        if (decoder != null) {
            found.put("decoder.onnx", decoder);
        }
        File joiner = selectOnnxCandidate(allFiles, "joiner");
        if (joiner != null) {
            found.put("joiner.onnx", joiner);
        }
        return found;
    }

    private List<File> collectAllFiles(File root) {
        List<File> result = new ArrayList<>();
        ArrayDeque<FileDepthPair> queue = new ArrayDeque<>();
        queue.add(new FileDepthPair(root, 0));
        while (!queue.isEmpty()) {
            FileDepthPair current = queue.removeFirst();
            File[] children = current.getFile().listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (child.isDirectory()) {
                    if (current.getDepth() < 12) {
                        queue.add(new FileDepthPair(child, current.getDepth() + 1));
                    }
                } else {
                    result.add(child);
                }
            }
        }
        return result;
    }

    private File selectOnnxCandidate(List<File> files, String keyword) {
        List<File> candidates = new ArrayList<>();
        String needle = keyword.toLowerCase(Locale.ROOT);
        for (File file : files) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".onnx") && name.contains(needle)) {
                candidates.add(file);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        File best = candidates.get(0);
        int bestScore = scoreOnnxCandidate(best.getName());
        for (int i = 1; i < candidates.size(); i++) {
            File candidate = candidates.get(i);
            int score = scoreOnnxCandidate(candidate.getName());
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private int scoreOnnxCandidate(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        int score = 0;
        if (!lower.contains("int8")) {
            score += 100;
        }
        if (lower.contains("epoch-99-avg-1")) {
            score += 30;
        }
        if (lower.contains("chunk")) {
            score += 5;
        }
        score -= lower.length() / 10;
        return score;
    }

    private void extractTarBz2(File archive, File targetDir) throws IOException {
        try (FileInputStream fileInput = new FileInputStream(archive);
                BufferedInputStream bufferedInput = new BufferedInputStream(fileInput);
                BZip2CompressorInputStream bzip2Input = new BZip2CompressorInputStream(bufferedInput);
                TarArchiveInputStream tarInput = new TarArchiveInputStream(bzip2Input)) {
            org.apache.commons.compress.archivers.ArchiveEntry entry;
            byte[] buffer = new byte[64 * 1024];
            String targetCanonical = targetDir.getCanonicalPath();
            while ((entry = tarInput.getNextEntry()) != null) {
                File outFile = new File(targetDir, entry.getName());
                String outCanonical = outFile.getCanonicalPath();
                if (!outCanonical.equals(targetCanonical)
                        && !outCanonical.startsWith(targetCanonical + File.separator)) {
                    throw new IOException("非法文件路径: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        throw new IOException("创建目录失败: " + outFile.getAbsolutePath());
                    }
                    continue;
                }
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("创建目录失败: " + parent.getAbsolutePath());
                }
                try (FileOutputStream output = new FileOutputStream(outFile)) {
                    int read;
                    while ((read = tarInput.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private void moveTempArchive(File tempFile, File archiveFile, String errorMessage) throws IOException {
        if (archiveFile.exists() && !archiveFile.delete()) {
            throw new IOException(errorMessage);
        }
        if (!tempFile.renameTo(archiveFile)) {
            throw new IOException(errorMessage);
        }
    }

    private BiConsumer<Long, Long> safeProgress(BiConsumer<Long, Long> onProgress) {
        return onProgress == null ? (downloaded, total) -> {
        } : onProgress;
    }

    private Consumer<String> safeEvent(Consumer<String> onEvent) {
        return onEvent == null ? message -> {
        } : onEvent;
    }

    private String simplifyError(Throwable error) {
        String message = error.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return error.getClass().getSimpleName();
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private void copyRecursively(File source, File target) {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) {
                return;
            }
            File[] children = source.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                copyRecursively(child, new File(target, child.getName()));
            }
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileInputStream input = new FileInputStream(source);
                FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } catch (IOException ignored) {
            // 复制失败在最终校验中会暴露。
        }
    }

    private static final class DownloadSource {

        private final String name;
        private final String baseUrl;

        private DownloadSource(String name, String baseUrl) {
            this.name = name;
            this.baseUrl = baseUrl;
        }

        private String getName() {
            return name;
        }

        private String getBaseUrl() {
            return baseUrl;
        }
    }

    private static final class FileDepthPair {

        private final File file;
        private final int depth;

        private FileDepthPair(File file, int depth) {
            this.file = file;
            this.depth = depth;
        }

        private File getFile() {
            return file;
        }

        private int getDepth() {
            return depth;
        }
    }

    public static final class AsrModelOption {

        private final String id;
        private final String name;
        private final String description;
        private final String sizeLabel;

        public AsrModelOption(String id, String name, String description, String sizeLabel) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.sizeLabel = sizeLabel;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public String description() {
            return description;
        }

        public String sizeLabel() {
            return sizeLabel;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
