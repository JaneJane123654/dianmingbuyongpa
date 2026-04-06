package com.classroomassistant.desktop.speech;

import com.classroomassistant.desktop.session.DesktopSettingsSnapshot;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 桌面端语音识别服务（本地 Sherpa + 云端 Whisper）。
 */
public class DesktopSpeechRecognitionService {

    private final SherpaOnnxJNI jni = new SherpaOnnxJNI();
    private final DesktopAsrVadGate vadGate = new DesktopAsrVadGate();
    private final OkHttpClient cloudClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .callTimeout(3, TimeUnit.MINUTES)
            .build();

    public String recognize(byte[] pcm16,
            DesktopSettingsSnapshot settings,
            DesktopModelLocator modelLocator,
            String localAsrModelId,
            Consumer<String> onLog) {
        Objects.requireNonNull(onLog, "onLog");
        if (pcm16 == null || pcm16.length == 0) {
            onLog.accept("语音识别跳过：输入音频为空");
            return "";
        }

        if (!settings.isCloudWhisperEnabled() && !settings.isLocalAsrEnabled()) {
            onLog.accept("语音识别已关闭：本机ASR关闭且未启用云端Whisper");
            return "";
        }

        DesktopAsrVadGate.Result vad = vadGate.filter(pcm16);
        onLog.accept("ASR前置" + vad.summary());
        if (!vad.hasSpeech) {
            onLog.accept("ASR前置VAD: 未检测到明确人声，跳过识别");
            return "";
        }

        byte[] filtered = vad.filteredPcm16;
        if (settings.isCloudWhisperEnabled()) {
            return recognizeByCloudWhisper(filtered, settings.getSpeechApiKey(), onLog);
        }
        String resolvedModelId = (localAsrModelId == null || localAsrModelId.isBlank())
                ? settings.getLocalAsrModelId()
                : localAsrModelId;
        return recognizeByLocalSherpa(filtered, resolvedModelId, modelLocator, onLog);
    }

    private String recognizeByCloudWhisper(byte[] pcm16, String speechApiKey, Consumer<String> onLog) {
        if (speechApiKey == null || speechApiKey.isBlank()) {
            onLog.accept("云端Whisper未配置 Speech API Key");
            return "";
        }

        byte[] wav = pcm16ToWav(pcm16);
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "whisper-large-v3-turbo")
                .addFormDataPart("language", "zh")
                .addFormDataPart("response_format", "json")
                .addFormDataPart("temperature", "0")
                .addFormDataPart(
                        "file",
                        "audio.wav",
                    RequestBody.create(wav, MediaType.parse("audio/wav")))
                .build();

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/audio/transcriptions")
                .addHeader("Authorization", "Bearer " + speechApiKey.trim())
                .post(body)
                .build();

        long startedAt = System.currentTimeMillis();
        try (okhttp3.Response response = cloudClient.newCall(request).execute()) {
            String payload = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                onLog.accept("云端Whisper失败: HTTP " + response.code() + ", body=" + truncate(payload, 280));
                return "";
            }
            JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
            String text = root.has("text") && !root.get("text").isJsonNull()
                    ? root.get("text").getAsString().trim()
                    : "";
            onLog.accept("云端Whisper完成(" + (System.currentTimeMillis() - startedAt) + "ms): "
                    + (text.isBlank() ? "(空结果)" : truncate(text, 120)));
            return text;
        } catch (Exception error) {
            onLog.accept("云端Whisper异常: " + simplifyError(error));
            return "";
        }
    }

    private String recognizeByLocalSherpa(byte[] pcm16,
            String preferredModelId,
            DesktopModelLocator modelLocator,
            Consumer<String> onLog) {
        File modelDir = modelLocator.findAsrModelDir(preferredModelId);
        if (modelDir == null) {
            onLog.accept("本机ASR模型未就绪: models/sherpa-onnx-asr");
            return "";
        }

        long startedAt = System.currentTimeMillis();
        long handle = 0L;
        try {
            SherpaOnnxJNI.ensureLoaded();
            handle = jni.initializeAsr(modelDir.toPath());
            if (handle == 0L) {
                onLog.accept("本机ASR初始化失败: " + modelDir.getAbsolutePath());
                return "";
            }
            String text = jni.recognize(handle, pcm16);
            String normalized = text == null ? "" : text.trim();
            onLog.accept("本机ASR完成(" + (System.currentTimeMillis() - startedAt) + "ms): "
                    + (normalized.isBlank() ? "(空结果)" : truncate(normalized, 120)));
            return normalized;
        } catch (Throwable error) {
            onLog.accept("本机ASR异常: " + simplifyError(error));
            return "";
        } finally {
            if (handle != 0L) {
                try {
                    jni.release(handle);
                } catch (Throwable ignore) {
                    // ignore
                }
            }
        }
    }

    private byte[] pcm16ToWav(byte[] pcm16) {
        int channels = 1;
        int bitsPerSample = 16;
        int sampleRate = 16000;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcm16.length;
        int riffSize = 36 + dataSize;

        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataSize);
        writeAscii(out, "RIFF");
        writeIntLe(out, riffSize);
        writeAscii(out, "WAVE");
        writeAscii(out, "fmt ");
        writeIntLe(out, 16);
        writeShortLe(out, 1);
        writeShortLe(out, channels);
        writeIntLe(out, sampleRate);
        writeIntLe(out, byteRate);
        writeShortLe(out, blockAlign);
        writeShortLe(out, bitsPerSample);
        writeAscii(out, "data");
        writeIntLe(out, dataSize);
        out.writeBytes(pcm16);
        return out.toByteArray();
    }

    private void writeAscii(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private void writeIntLe(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }

    private void writeShortLe(ByteArrayOutputStream out, int value) {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength)) + "...";
    }

    private String simplifyError(Throwable error) {
        String message = error.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return error.getClass().getSimpleName();
    }
}
