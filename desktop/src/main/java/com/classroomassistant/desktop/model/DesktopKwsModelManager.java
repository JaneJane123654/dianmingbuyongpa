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
 * 桌面端 KWS 本地模型管理器。
 */
public class DesktopKwsModelManager {

    public static final String CUSTOM_MODEL_ID = "custom-kws-url-model";

    private static final List<String> REQUIRED_FILES = List.of("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt");
    private static final List<String> OPTIONAL_KEYWORD_FILES = List.of("keywords.txt", "test_keywords.txt");

    private static final List<DownloadSource> DOWNLOAD_SOURCES = List.of(
            new DownloadSource("GitHub 官方源", "https://github.com/k2-fsa/sherpa-onnx/releases/download"),
            new DownloadSource("中国镜像源(kkgithub)", "https://kkgithub.com/k2-fsa/sherpa-onnx/releases/download"));

    private static final List<KwsModelOption> MODEL_OPTIONS = List.of(
            new KwsModelOption(
                    "sherpa-onnx-kws-zipformer-wenetspeech-3.3M-2024-01-01",
                    "Zipformer WenetSpeech 3.3M (2024-01-01)",
                    "中文唤醒模型，覆盖常见课堂场景，体积适中",
                    "约 3.3 MB"),
            new KwsModelOption(
                    "sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01",
                    "Zipformer GigaSpeech 3.3M (2024-01-01)",
                    "英语为主的唤醒模型，适合英文课堂或双语环境",
                    "约 3.3 MB"),
            new KwsModelOption(
                    "sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20",
                    "Zipformer 中英 3M (2025-12-20)",
                    "中英双语唤醒模型，优先推荐给混合语言场景",
                    "约 3 MB"),
            new KwsModelOption(
                    CUSTOM_MODEL_ID,
                    "自定义链接模型 / Custom URL Model",
                    "通过设置页填写下载链接后可下载并使用",
                    "大小取决于下载链接"));

    private final PlatformStorage storage;
    private final OkHttpClient downloadClient;

    public DesktopKwsModelManager(PlatformStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.downloadClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .callTimeout(15, TimeUnit.MINUTES)
                .build();
    }

    public List<KwsModelOption> getAvailableModels() {
        return MODEL_OPTIONS;
    }

    public String getDefaultModelId() {
        return MODEL_OPTIONS.get(0).id();
    }

    public KwsModelOption getOptionById(String modelId) {
        if (modelId == null) {
            return null;
        }
        for (KwsModelOption option : MODEL_OPTIONS) {
            if (option.id().equals(modelId)) {
                return option;
            }
        }
        return null;
    }

    public File getModelDir(String modelId) {
        String resolvedId = (modelId == null || modelId.isBlank()) ? getDefaultModelId() : modelId.trim();
        File root = new File(storage.getModelsDir(), "sherpa-onnx-kws");
        return new File(root, resolvedId);
    }

    public boolean isModelReady(String modelId) {
        return missingFiles(getModelDir(modelId)).isEmpty();
    }

    public File downloadAndPrepare(String modelId,
            BiConsumer<Long, Long> onProgress,
            Consumer<String> onEvent) throws IOException {
        String resolvedModelId = resolveModelId(modelId);
        File archive = new File(storage.getCacheDir(), "kws_" + resolvedModelId + "_" + UUID.randomUUID() + ".tar.bz2");
        File tmpArchive = new File(archive.getAbsolutePath() + ".part");
        try {
            downloadArchiveWithFallback(tmpArchive, resolvedModelId, safeProgress(onProgress), safeEvent(onEvent));
            moveTempArchive(tmpArchive, archive, "模型包移动失败");

            File extractDir = new File(storage.getModelsDir(), "kws_extract_" + UUID.randomUUID());
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
                throw new IOException("模型文件不完整: " + String.join(", ", missing));
            }

            for (String requiredFile : REQUIRED_FILES) {
                copyRecursively(collected.get(requiredFile), new File(targetDir, requiredFile));
            }
            for (String optionalFile : OPTIONAL_KEYWORD_FILES) {
                File source = collected.get(optionalFile);
                if (source != null) {
                    copyRecursively(source, new File(targetDir, optionalFile));
                }
            }

            deleteRecursively(extractDir);

            List<String> stillMissing = missingFiles(targetDir);
            if (!stillMissing.isEmpty()) {
                throw new IOException("模型文件不完整: " + String.join(", ", stillMissing));
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
        String normalizedUrl = normalizeArchiveUrl(archiveUrl, "请先填写唤醒模型下载链接");

        File archive = new File(storage.getCacheDir(), "kws_custom_" + UUID.randomUUID() + ".tar.bz2");
        File tmpArchive = new File(archive.getAbsolutePath() + ".part");
        Consumer<String> event = safeEvent(onEvent);
        try {
            event.accept("自定义唤醒模型下载: 使用外部链接");
            downloadArchiveWithRetry(tmpArchive, normalizedUrl, "自定义链接", safeProgress(onProgress), event);
            moveTempArchive(tmpArchive, archive, "模型包移动失败");

            File extractDir = new File(storage.getModelsDir(), "kws_extract_" + UUID.randomUUID());
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
                throw new IOException("模型文件不完整: " + String.join(", ", missing));
            }

            for (String requiredFile : REQUIRED_FILES) {
                copyRecursively(collected.get(requiredFile), new File(targetDir, requiredFile));
            }
            for (String optionalFile : OPTIONAL_KEYWORD_FILES) {
                File source = collected.get(optionalFile);
                if (source != null) {
                    copyRecursively(source, new File(targetDir, optionalFile));
                }
            }
            deleteRecursively(extractDir);

            List<String> stillMissing = missingFiles(targetDir);
            if (!stillMissing.isEmpty()) {
                throw new IOException("模型文件不完整: " + String.join(", ", stillMissing));
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

    public boolean migrateLegacyModelIfNeeded(String modelId) {
        String resolvedModelId = resolveModelId(modelId);
        if (!resolvedModelId.equals(getDefaultModelId())) {
            return false;
        }

        File baseDir = new File(storage.getModelsDir(), "sherpa-onnx-kws");
        File targetDir = new File(baseDir, resolvedModelId);
        if (!baseDir.exists() || !baseDir.isDirectory() || targetDir.exists()) {
            return false;
        }
        File legacyTokens = new File(baseDir, "tokens.txt");
        if (!legacyTokens.exists()) {
            return false;
        }
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            return false;
        }
        File[] children = baseDir.listFiles();
        if (children == null) {
            return false;
        }
        for (File child : children) {
            if (resolvedModelId.equals(child.getName())) {
                continue;
            }
            copyRecursively(child, new File(targetDir, child.getName()));
        }
        return true;
    }

    private List<String> missingFiles(File dir) {
        List<String> missing = new ArrayList<>();
        for (String requiredFile : REQUIRED_FILES) {
            if (!new File(dir, requiredFile).isFile()) {
                missing.add(requiredFile);
            }
        }
        return missing;
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

    private void downloadArchiveWithFallback(File target,
            String modelId,
            BiConsumer<Long, Long> onProgress,
            Consumer<String> onEvent) throws IOException {
        List<String> sourceErrors = new ArrayList<>();
        for (int index = 0; index < DOWNLOAD_SOURCES.size(); index++) {
            DownloadSource source = DOWNLOAD_SOURCES.get(index);
            String url = source.getBaseUrl() + "/kws-models/" + modelId + ".tar.bz2";
            try {
                onEvent.accept("模型下载源尝试(" + (index + 1) + "/" + DOWNLOAD_SOURCES.size() + "): " + source.getName());
                downloadArchiveWithRetry(target, url, source.getName(), onProgress, onEvent);
                if (index > 0) {
                    onEvent.accept("已切换至" + source.getName() + "并下载成功");
                }
                return;
            } catch (IOException error) {
                String reason = simplifyError(error);
                sourceErrors.add(source.getName() + ": " + reason);
                onEvent.accept("模型下载源失败: " + source.getName() + "，原因: " + reason);
                if (index == 0 && DOWNLOAD_SOURCES.size() > 1) {
                    onEvent.accept("GitHub 下载失败，自动切换到中国镜像源重试");
                }
            }
        }
        throw new IOException("模型下载失败，已尝试全部下载源: " + String.join(" | ", sourceErrors));
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
            if (REQUIRED_FILES.contains(entry.getName()) && !found.containsKey(entry.getName())) {
                found.put(entry.getName(), entry);
            }
        }

        for (String optionalName : OPTIONAL_KEYWORD_FILES) {
            for (File entry : allFiles) {
                if (entry.getName().equalsIgnoreCase(optionalName)) {
                    found.put(optionalName, entry);
                    break;
                }
            }
        }

        if (!found.containsKey("tokens.txt")) {
            for (File entry : allFiles) {
                if (entry.getName().equalsIgnoreCase("tokens.txt")) {
                    found.put("tokens.txt", entry);
                    break;
                }
            }
        }
        if (!found.containsKey("encoder.onnx")) {
            File candidate = selectOnnxCandidate(allFiles, "encoder");
            if (candidate != null) {
                found.put("encoder.onnx", candidate);
            }
        }
        if (!found.containsKey("decoder.onnx")) {
            File candidate = selectOnnxCandidate(allFiles, "decoder");
            if (candidate != null) {
                found.put("decoder.onnx", candidate);
            }
        }
        if (!found.containsKey("joiner.onnx")) {
            File candidate = selectOnnxCandidate(allFiles, "joiner");
            if (candidate != null) {
                found.put("joiner.onnx", candidate);
            }
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
        for (File file : files) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".onnx") && name.contains(keyword.toLowerCase(Locale.ROOT))) {
                candidates.add(file);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        File best = candidates.get(0);
        int bestScore = scoreOnnxCandidate(best.getName());
        for (int i = 1; i < candidates.size(); i++) {
            File file = candidates.get(i);
            int score = scoreOnnxCandidate(file.getName());
            if (score > bestScore) {
                best = file;
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
        if (lower.contains("epoch-12-avg-2")) {
            score += 50;
        }
        if (lower.contains("epoch-99-avg-1")) {
            score += 40;
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

    public static final class KwsModelOption {

        private final String id;
        private final String name;
        private final String description;
        private final String sizeLabel;

        public KwsModelOption(String id, String name, String description, String sizeLabel) {
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
