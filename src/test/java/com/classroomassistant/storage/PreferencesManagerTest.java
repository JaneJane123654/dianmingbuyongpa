package com.classroomassistant.storage;

import com.classroomassistant.ai.LLMConfig;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PreferencesManager 单元测试
 *
 * <p>验证用户配置的加载、保存和加密功能。
 */
class PreferencesManagerTest {

    private PreferencesManager preferencesManager;

    @BeforeEach
    void setUp() {
        preferencesManager = new PreferencesManager();
    }

    @Test
    @DisplayName("加载默认配置时应返回合理默认值")
    void load_returnsDefaultValues() {
        UserPreferences prefs = preferencesManager.load();

        assertNotNull(prefs);
        assertEquals(240, prefs.getAudioLookbackSeconds());
        assertTrue(prefs.isVadEnabled());
        assertEquals(5, prefs.getVadQuietThresholdSeconds());
    }

    @Test
    @DisplayName("保存并加载配置应一致")
    void saveAndLoad_shouldPreserveValues() {
        UserPreferences toSave = UserPreferences.builder()
            .keywords("张三,李四")
            .vadEnabled(false)
            .vadQuietThresholdSeconds(10)
            .audioLookbackSeconds(120)
            .recordingSaveEnabled(true)
            .recordingRetentionDays(14)
            .aiModelType(LLMConfig.ModelType.OPENAI)
            .aiModelName("gpt-4")
            .aiTokenPlainText("test-token")
            .speechApiKey("speech-key")
            .selectedKwsModelIds(Set.of("model-a", "model-b"))
            .currentKwsModelId("model-b")
            .asrModelSelected(false)
            .vadModelSelected(true)
            .build();

        preferencesManager.save(toSave);
        UserPreferences loaded = preferencesManager.load();

        assertEquals("张三,李四", loaded.getKeywords());
        assertFalse(loaded.isVadEnabled());
        assertEquals(10, loaded.getVadQuietThresholdSeconds());
        assertEquals(120, loaded.getAudioLookbackSeconds());
        assertTrue(loaded.isRecordingSaveEnabled());
        assertEquals(14, loaded.getRecordingRetentionDays());
        assertEquals(LLMConfig.ModelType.OPENAI, loaded.getAiModelType());
        assertEquals("gpt-4", loaded.getAiModelName());
        assertEquals(Set.of("model-a", "model-b"), loaded.getSelectedKwsModelIds());
        assertEquals("model-b", loaded.getCurrentKwsModelId());
        assertFalse(loaded.isAsrModelSelected());
        assertTrue(loaded.isVadModelSelected());
    }

    @Test
    @DisplayName("加密的 AI Token 应能正确解密")
    void aiToken_encryptionAndDecryption() {
        String originalToken = "sk-test-12345-abcdef";
        UserPreferences prefs = UserPreferences.builder()
            .aiTokenPlainText(originalToken)
            .build();

        preferencesManager.save(prefs);
        String decrypted = preferencesManager.loadAiTokenPlainText();

        assertEquals(originalToken, decrypted);
    }

    @Test
    @DisplayName("加密的 Speech API Key 应能正确解密")
    void speechApiKey_encryptionAndDecryption() {
        String originalKey = "gsk-test-speech-key";
        UserPreferences prefs = UserPreferences.builder()
            .speechApiKey(originalKey)
            .build();

        preferencesManager.save(prefs);
        String decrypted = preferencesManager.loadSpeechApiKey();

        assertEquals(originalKey, decrypted);
    }

    @Test
    @DisplayName("空 Token 保存后加载应返回空字符串")
    void emptyToken_shouldReturnEmptyString() {
        UserPreferences prefs = UserPreferences.builder()
            .aiTokenPlainText("")
            .build();

        preferencesManager.save(prefs);
        String decrypted = preferencesManager.loadAiTokenPlainText();

        assertEquals("", decrypted);
    }
}
