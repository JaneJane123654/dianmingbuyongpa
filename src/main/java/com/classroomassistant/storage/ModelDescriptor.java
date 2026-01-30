package com.classroomassistant.storage;

import java.net.URI;
import java.nio.file.Path;

/**
 * 模型下载描述（值对象）
 */
public record ModelDescriptor(
    String name,
    URI downloadUrl,
    Path targetPath,
    Long expectedSizeBytes,
    String expectedMd5Hex
) {
}

