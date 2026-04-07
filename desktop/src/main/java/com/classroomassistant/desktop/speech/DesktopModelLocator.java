package com.classroomassistant.desktop.speech;

import com.classroomassistant.core.platform.PlatformStorage;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * 桌面端本地模型目录定位器。
 */
public class DesktopModelLocator {

    private static final String[] KWS_REQUIRED_FILES = { "encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt" };
    private static final String[] ASR_REQUIRED_FILES = { "encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt" };

    private final PlatformStorage storage;

    public DesktopModelLocator(PlatformStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public File findKwsModelDir(String preferredModelId) {
        File root = new File(storage.getModelsDir(), "sherpa-onnx-kws");
        return findModelDir(root, preferredModelId, KWS_REQUIRED_FILES);
    }

    public File findAsrModelDir(String preferredModelId) {
        File root = new File(storage.getModelsDir(), "sherpa-onnx-asr");
        return findModelDir(root, preferredModelId, ASR_REQUIRED_FILES);
    }

    private File findModelDir(File root, String preferredModelId, String[] requiredFiles) {
        if (!root.exists() || !root.isDirectory()) {
            return null;
        }

        if (preferredModelId != null && !preferredModelId.isBlank()) {
            File preferred = new File(root, preferredModelId.trim());
            if (hasRequiredFiles(preferred, requiredFiles)) {
                return preferred;
            }
        }

        if (hasRequiredFiles(root, requiredFiles)) {
            return root;
        }

        File[] children = root.listFiles(File::isDirectory);
        if (children == null || children.length == 0) {
            return null;
        }

        Arrays.sort(children, Comparator.comparing(File::getName));
        for (File child : children) {
            if (hasRequiredFiles(child, requiredFiles)) {
                return child;
            }
        }
        return null;
    }

    private boolean hasRequiredFiles(File dir, String[] requiredFiles) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        for (String file : requiredFiles) {
            if (!new File(dir, file).isFile()) {
                return false;
            }
        }
        return true;
    }
}
