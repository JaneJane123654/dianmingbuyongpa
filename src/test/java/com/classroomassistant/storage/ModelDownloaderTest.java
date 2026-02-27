package com.classroomassistant.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 模型下载器单元测试 (ModelDownloader Unit Tests)
 *
 * <p>验证下载器的取消、重置功能，以及监听器回调机制。
 * 注意：实际网络下载测试应使用集成测试环境。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
class ModelDownloaderTest {

    @TempDir
    Path tempDir;

    private ModelDownloader downloader;

    @BeforeEach
    void setUp() {
        downloader = new ModelDownloader();
    }

    @Test
    @DisplayName("cancel 方法不抛异常")
    void cancel_doesNotThrow() {
        assertDoesNotThrow(() -> downloader.cancel());
    }

    @Test
    @DisplayName("reset 方法不抛异常")
    void reset_doesNotThrow() {
        assertDoesNotThrow(() -> downloader.reset());
    }

    @Test
    @DisplayName("cancel 后 reset 可正常工作")
    void cancelThenReset_works() {
        downloader.cancel();
        assertDoesNotThrow(() -> downloader.reset());
    }

    @Test
    @DisplayName("download 方法不接受 null descriptor")
    void download_nullDescriptor_throws() {
        MockDownloadListener listener = new MockDownloadListener();
        assertThrows(NullPointerException.class, () -> downloader.download(null, listener));
    }

    @Test
    @DisplayName("download 方法不接受 null listener")
    void download_nullListener_throws() {
        ModelDescriptor descriptor = new ModelDescriptor(
            "Test",
            URI.create("https://example.com/test.onnx"),
            tempDir.resolve("test.onnx"),
            null,
            null
        );
        assertThrows(NullPointerException.class, () -> downloader.download(descriptor, null));
    }

    @Test
    @DisplayName("连续多次 cancel 不抛异常")
    void multipleCancel_doesNotThrow() {
        assertDoesNotThrow(() -> {
            downloader.cancel();
            downloader.cancel();
            downloader.cancel();
        });
    }

    @Test
    @DisplayName("连续多次 reset 不抛异常")
    void multipleReset_doesNotThrow() {
        assertDoesNotThrow(() -> {
            downloader.reset();
            downloader.reset();
            downloader.reset();
        });
    }

    @Test
    @DisplayName("可以创建多个 ModelDownloader 实例")
    void multipleInstances_work() {
        ModelDownloader d1 = new ModelDownloader();
        ModelDownloader d2 = new ModelDownloader();

        assertNotNull(d1);
        assertNotNull(d2);
        assertNotSame(d1, d2);
    }

    @Test
    @DisplayName("下载无效 URL 会调用 onError")
    void downloadInvalidUrl_callsOnError() {
        ModelDescriptor descriptor = new ModelDescriptor(
            "Invalid",
            URI.create("http://invalid.local.test.url/nonexistent.onnx"),
            tempDir.resolve("invalid.onnx"),
            null,
            null
        );

        MockDownloadListener listener = new MockDownloadListener();
        downloader.download(descriptor, listener);

        // 由于是同步下载，错误应该已经发生
        assertTrue(listener.errorCalled, "无效 URL 应触发 onError");
        assertNotNull(listener.errorMessage);
    }

    // ================== Mock 类 ==================

    private static class MockDownloadListener implements ModelDownloader.DownloadListener {
        boolean progressCalled = false;
        boolean finishedCalled = false;
        boolean errorCalled = false;
        String errorMessage = null;
        long lastDownloadedBytes = 0;
        long lastTotalBytes = 0;
        Path finishedPath = null;

        @Override
        public void onProgress(String name, long downloadedBytes, long totalBytes) {
            progressCalled = true;
            lastDownloadedBytes = downloadedBytes;
            lastTotalBytes = totalBytes;
        }

        @Override
        public void onFinished(String name, Path targetPath) {
            finishedCalled = true;
            finishedPath = targetPath;
        }

        @Override
        public void onError(String name, String error) {
            errorCalled = true;
            errorMessage = error;
        }
    }
}
