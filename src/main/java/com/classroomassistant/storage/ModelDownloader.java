package com.classroomassistant.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模型下载器 (Model Downloader)
 *
 * <p>负责从远程 URL 下载大型模型文件。
 * 支持以下特性：
 * <ul>
 *   <li>断点续传：使用 HTTP Range 请求从上次中断的地方继续下载。</li>
 *   <li>下载进度监听：通过 {@link DownloadListener} 实时回调下载进度。</li>
 *   <li>完整性校验：下载完成后自动校验文件大小和 MD5 哈希值。</li>
 *   <li>原子移动：下载完成后将临时文件原子重命名为目标文件名，确保文件可用性。</li>
 *   <li>可取消：支持通过 {@link #cancel()} 方法中断下载。</li>
 * </ul>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class ModelDownloader {

    private static final Logger logger = LoggerFactory.getLogger(ModelDownloader.class);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * 下载进度与状态监听器
     */
    public interface DownloadListener {
        /**
         * 下载进度回调
         *
         * @param name            模型名称
         * @param downloadedBytes 已下载字节数
         * @param totalBytes      总字节数
         */
        void onProgress(String name, long downloadedBytes, long totalBytes);

        /**
         * 下载完成回调
         *
         * @param name       模型名称
         * @param targetPath 最终存储路径
         */
        void onFinished(String name, Path targetPath);

        /**
         * 下载错误回调
         *
         * @param name  模型名称
         * @param error 错误详情描述
         */
        void onError(String name, String error);
    }

    private final HttpClient httpClient;

    /**
     * 初始化下载器
     */
    public ModelDownloader() {
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * 取消当前下载任务
     */
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * 重置取消状态（用于开始新下载）
     */
    public void reset() {
        cancelled.set(false);
    }

    /**
     * 启动异步下载任务
     *
     * @param descriptor 模型描述信息
     * @param listener   状态监听器
     * @throws NullPointerException 如果参数为 null
     */
    public void download(ModelDescriptor descriptor, DownloadListener listener) {
        Objects.requireNonNull(descriptor);
        Objects.requireNonNull(listener);

        try {
            Path target = descriptor.targetPath();
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".part");

            long existing = Files.exists(tmp) ? Files.size(tmp) : 0L;
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(descriptor.downloadUrl()).GET();
            if (existing > 0) {
                requestBuilder.header("Range", "bytes=" + existing + "-");
            }

            HttpResponse<InputStream> response =
                httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());

            int statusCode = response.statusCode();
            if (statusCode != 200 && statusCode != 206) {
                listener.onError(descriptor.name(), "HTTP 状态码异常: " + statusCode);
                return;
            }

            long totalBytes = resolveTotalBytes(existing, response, descriptor.expectedSizeBytes());
            try (InputStream inputStream = response.body();
                 OutputStream outputStream = Files.newOutputStream(tmp, java.nio.file.StandardOpenOption.CREATE,
                     java.nio.file.StandardOpenOption.APPEND)) {
                byte[] buffer = new byte[64 * 1024];
                long downloaded = existing;
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    if (cancelled.get()) {
                        listener.onError(descriptor.name(), "下载已取消");
                        return;
                    }
                    outputStream.write(buffer, 0, read);
                    downloaded += read;
                    listener.onProgress(descriptor.name(), downloaded, totalBytes);
                }
            }

            if (descriptor.expectedSizeBytes() != null) {
                long size = Files.size(tmp);
                if (size != descriptor.expectedSizeBytes()) {
                    listener.onError(descriptor.name(), "文件大小校验失败，期望 " + descriptor.expectedSizeBytes() + "，实际 " + size);
                    return;
                }
            }

            if (descriptor.expectedMd5Hex() != null && !descriptor.expectedMd5Hex().isBlank()) {
                String md5 = md5Hex(tmp);
                if (!descriptor.expectedMd5Hex().equalsIgnoreCase(md5)) {
                    listener.onError(descriptor.name(), "MD5 校验失败，期望 " + descriptor.expectedMd5Hex() + "，实际 " + md5);
                    return;
                }
            }

            Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            listener.onFinished(descriptor.name(), target);
        } catch (Exception e) {
            logger.warn("下载模型失败: {}", e.getMessage(), e);
            listener.onError(descriptor.name(), "下载失败: " + e.getMessage());
        }
    }

    private static long resolveTotalBytes(long existing, HttpResponse<?> response, Long expectedSize) {
        if (expectedSize != null) {
            return expectedSize;
        }

        String contentRange = response.headers().firstValue("Content-Range").orElse("");
        if (!contentRange.isBlank() && contentRange.contains("/")) {
            String total = contentRange.substring(contentRange.lastIndexOf('/') + 1).trim();
            try {
                return Long.parseLong(total);
            } catch (NumberFormatException ignored) {
            }
        }

        return existing + response.headers().firstValueAsLong("Content-Length").orElse(0L);
    }

    private static String md5Hex(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream inputStream = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    md.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IOException("计算 MD5 失败: " + e.getMessage(), e);
        }
    }
}
