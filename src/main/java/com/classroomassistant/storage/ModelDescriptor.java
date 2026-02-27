package com.classroomassistant.storage;

import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;

/**
 * 模型下载描述（值对象）
 *
 * <p>封装了一个模型文件的下载信息，包括名称、源地址、本地目标路径以及用于校验的预期大小和 MD5。
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public final class ModelDescriptor {

    private final String name;
    private final URI downloadUrl;
    private final Path targetPath;
    private final Long expectedSizeBytes;
    private final String expectedMd5Hex;

    /**
     * 构造模型下载描述
     *
     * @param name              模型显示名称
     * @param downloadUrl       模型下载 URL
     * @param targetPath        模型下载后的存储路径
     * @param expectedSizeBytes 预期文件大小（字节），为 null 则跳过大小校验
     * @param expectedMd5Hex    预期 MD5 值（16进制字符串），为 null 或空则跳过 MD5 校验
     */
    public ModelDescriptor(String name, URI downloadUrl, Path targetPath, Long expectedSizeBytes, String expectedMd5Hex) {
        this.name = name;
        this.downloadUrl = downloadUrl;
        this.targetPath = targetPath;
        this.expectedSizeBytes = expectedSizeBytes;
        this.expectedMd5Hex = expectedMd5Hex;
    }

    public String name() {
        return name;
    }

    public URI downloadUrl() {
        return downloadUrl;
    }

    public Path targetPath() {
        return targetPath;
    }

    public Long expectedSizeBytes() {
        return expectedSizeBytes;
    }

    public String expectedMd5Hex() {
        return expectedMd5Hex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelDescriptor)) return false;
        ModelDescriptor that = (ModelDescriptor) o;
        return Objects.equals(name, that.name)
                && Objects.equals(downloadUrl, that.downloadUrl)
                && Objects.equals(targetPath, that.targetPath)
                && Objects.equals(expectedSizeBytes, that.expectedSizeBytes)
                && Objects.equals(expectedMd5Hex, that.expectedMd5Hex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, downloadUrl, targetPath, expectedSizeBytes, expectedMd5Hex);
    }

    @Override
    public String toString() {
        return "ModelDescriptor{" +
                "name='" + name + '\'' +
                ", downloadUrl=" + downloadUrl +
                ", targetPath=" + targetPath +
                ", expectedSizeBytes=" + expectedSizeBytes +
                ", expectedMd5Hex='" + expectedMd5Hex + '\'' +
                '}';
    }
}

