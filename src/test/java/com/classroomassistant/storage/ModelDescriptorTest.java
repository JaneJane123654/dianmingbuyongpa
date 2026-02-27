package com.classroomassistant.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 模型描述符单元测试 (ModelDescriptor Unit Tests)
 *
 * <p>验证模型描述符值对象的正确行为。
 *
 * @author Code Assistant
 * @date 2026-02-01
 */
class ModelDescriptorTest {

    @Test
    @DisplayName("构造函数正确设置所有参数")
    void constructorSetsAllParameters() {
        String name = "KWS Model";
        URI url = URI.create("https://example.com/model.onnx");
        Path path = Path.of("/models/kws");
        Long size = 1024L;
        String md5 = "abc123";

        ModelDescriptor descriptor = new ModelDescriptor(name, url, path, size, md5);

        assertEquals(name, descriptor.name());
        assertEquals(url, descriptor.downloadUrl());
        assertEquals(path, descriptor.targetPath());
        assertEquals(size, descriptor.expectedSizeBytes());
        assertEquals(md5, descriptor.expectedMd5Hex());
    }

    @Test
    @DisplayName("可选参数可以为 null")
    void optionalParametersCanBeNull() {
        ModelDescriptor descriptor = new ModelDescriptor(
            "Test Model",
            URI.create("https://example.com/model.onnx"),
            Path.of("/models/test"),
            null,
            null
        );

        assertNull(descriptor.expectedSizeBytes());
        assertNull(descriptor.expectedMd5Hex());
    }

    @Test
    @DisplayName("equals 相同参数返回 true")
    void equals_sameParameters_true() {
        URI url = URI.create("https://example.com/model.onnx");
        Path path = Path.of("/models/test");
        
        ModelDescriptor d1 = new ModelDescriptor("Model", url, path, 1024L, "abc");
        ModelDescriptor d2 = new ModelDescriptor("Model", url, path, 1024L, "abc");

        assertEquals(d1, d2);
    }

    @Test
    @DisplayName("equals 不同名称返回 false")
    void equals_differentName_false() {
        URI url = URI.create("https://example.com/model.onnx");
        Path path = Path.of("/models/test");
        
        ModelDescriptor d1 = new ModelDescriptor("Model1", url, path, null, null);
        ModelDescriptor d2 = new ModelDescriptor("Model2", url, path, null, null);

        assertNotEquals(d1, d2);
    }

    @Test
    @DisplayName("equals 不同 URL 返回 false")
    void equals_differentUrl_false() {
        Path path = Path.of("/models/test");
        
        ModelDescriptor d1 = new ModelDescriptor("Model", URI.create("https://a.com/m.onnx"), path, null, null);
        ModelDescriptor d2 = new ModelDescriptor("Model", URI.create("https://b.com/m.onnx"), path, null, null);

        assertNotEquals(d1, d2);
    }

    @Test
    @DisplayName("equals 不同路径返回 false")
    void equals_differentPath_false() {
        URI url = URI.create("https://example.com/model.onnx");
        
        ModelDescriptor d1 = new ModelDescriptor("Model", url, Path.of("/path1"), null, null);
        ModelDescriptor d2 = new ModelDescriptor("Model", url, Path.of("/path2"), null, null);

        assertNotEquals(d1, d2);
    }

    @Test
    @DisplayName("equals 与 null 返回 false")
    void equals_null_false() {
        ModelDescriptor d = new ModelDescriptor("Model", URI.create("https://a.com/m.onnx"), Path.of("/p"), null, null);
        assertNotEquals(null, d);
    }

    @Test
    @DisplayName("equals 与自身返回 true")
    void equals_self_true() {
        ModelDescriptor d = new ModelDescriptor("Model", URI.create("https://a.com/m.onnx"), Path.of("/p"), null, null);
        assertEquals(d, d);
    }

    @Test
    @DisplayName("hashCode 相同参数返回相同值")
    void hashCode_sameParameters_sameValue() {
        URI url = URI.create("https://example.com/model.onnx");
        Path path = Path.of("/models/test");
        
        ModelDescriptor d1 = new ModelDescriptor("Model", url, path, 1024L, "abc");
        ModelDescriptor d2 = new ModelDescriptor("Model", url, path, 1024L, "abc");

        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    @DisplayName("toString 包含模型名称")
    void toString_containsName() {
        ModelDescriptor d = new ModelDescriptor(
            "KWS唤醒词模型",
            URI.create("https://example.com/model.onnx"),
            Path.of("/models/kws"),
            null,
            null
        );

        assertTrue(d.toString().contains("KWS唤醒词模型"));
    }

    @Test
    @DisplayName("不同预期大小的 equals 返回 false")
    void equals_differentSize_false() {
        URI url = URI.create("https://example.com/model.onnx");
        Path path = Path.of("/models/test");
        
        ModelDescriptor d1 = new ModelDescriptor("Model", url, path, 1024L, null);
        ModelDescriptor d2 = new ModelDescriptor("Model", url, path, 2048L, null);

        assertNotEquals(d1, d2);
    }

    @Test
    @DisplayName("不同 MD5 的 equals 返回 false")
    void equals_differentMd5_false() {
        URI url = URI.create("https://example.com/model.onnx");
        Path path = Path.of("/models/test");
        
        ModelDescriptor d1 = new ModelDescriptor("Model", url, path, null, "abc123");
        ModelDescriptor d2 = new ModelDescriptor("Model", url, path, null, "def456");

        assertNotEquals(d1, d2);
    }

    @Test
    @DisplayName("空 MD5 字符串与 null 应区分处理")
    void emptyMd5VsNull() {
        URI url = URI.create("https://example.com/model.onnx");
        Path path = Path.of("/models/test");
        
        ModelDescriptor d1 = new ModelDescriptor("Model", url, path, null, "");
        ModelDescriptor d2 = new ModelDescriptor("Model", url, path, null, null);

        // 空字符串和 null 应该不相等
        assertNotEquals(d1, d2);
    }
}
