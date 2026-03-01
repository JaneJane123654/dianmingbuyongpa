package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserPreferences 单元测试
 *
 * <p>验证 UserPreferences 值对象的构建和属性访问。
 */
class UserPreferencesTest {

    @Test
    @DisplayName("Builder 应正确构建对象")
    void builder_createsCorrectObject() {
        UserPreferences prefs = UserPreferences.builder()
            .keywords("张三")
            .vadEnabled(true)
            .vadQuietThresholdSeconds(8)
            .audioLookbackSeconds(180)
            .recordingSaveEnabled(true)
            .recordingRetentionDays(7)
            .aiModelType(LLMConfig.ModelType.DEEPSEEK)
            .aiModelName("deepseek-chat")
            .aiTokenPlainText("token123")
            .speechApiKey("speech-key-456")
            .selectedKwsModelIds(Set.of("model-a", "model-b"))
            .currentKwsModelId("model-b")
            .asrModelSelected(false)
            .vadModelSelected(true)
            .build();

        assertEquals("张三", prefs.getKeywords());
        assertTrue(prefs.isVadEnabled());
        assertEquals(8, prefs.getVadQuietThresholdSeconds());
        assertEquals(180, prefs.getAudioLookbackSeconds());
        assertTrue(prefs.isRecordingSaveEnabled());
        assertEquals(7, prefs.getRecordingRetentionDays());
        assertEquals(LLMConfig.ModelType.DEEPSEEK, prefs.getAiModelType());
        assertEquals("deepseek-chat", prefs.getAiModelName());
        assertEquals("token123", prefs.getAiTokenPlainText());
        assertEquals("speech-key-456", prefs.getSpeechApiKey());
        assertEquals(Set.of("model-a", "model-b"), prefs.getSelectedKwsModelIds());
        assertEquals("model-b", prefs.getCurrentKwsModelId());
        assertFalse(prefs.isAsrModelSelected());
        assertTrue(prefs.isVadModelSelected());
    }

    @Test
    @DisplayName("Builder 默认值应合理")
    void builder_hasReasonableDefaults() {
        UserPreferences prefs = UserPreferences.builder().build();

        assertEquals("", prefs.getKeywords());
        assertTrue(prefs.isVadEnabled());
        assertEquals(5, prefs.getVadQuietThresholdSeconds());
        assertEquals(240, prefs.getAudioLookbackSeconds());
        assertFalse(prefs.isRecordingSaveEnabled());
        assertEquals(7, prefs.getRecordingRetentionDays());
        assertEquals(LLMConfig.ModelType.QIANFAN, prefs.getAiModelType());
        assertEquals("", prefs.getAiModelName());
        assertEquals("", prefs.getAiTokenPlainText());
        assertEquals("", prefs.getSpeechApiKey());
        assertEquals(Set.of(), prefs.getSelectedKwsModelIds());
        assertEquals("", prefs.getCurrentKwsModelId());
        assertTrue(prefs.isAsrModelSelected());
        assertTrue(prefs.isVadModelSelected());
    }

    @Test
    @DisplayName("空字符串应被 trim 处理")
    void nullValues_shouldBeTrimmed() {
        UserPreferences prefs = UserPreferences.builder()
            .keywords("  张三  ")
            .aiModelName("  ")
            .aiTokenPlainText(null)
            .speechApiKey(null)
            .build();

        assertEquals("张三", prefs.getKeywords());
        assertEquals("", prefs.getAiModelName());
        assertEquals("", prefs.getAiTokenPlainText());
        assertEquals("", prefs.getSpeechApiKey());
    }
}
