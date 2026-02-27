package com.classroomassistant.speech;

import com.classroomassistant.audio.WavWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API 云端语音识别器 (Cloud Speech Recognizer via API)
 *
 * <p>基于 OpenAI Whisper API（或兼容接口）实现的语音识别器。
 * 支持将音频数据通过 HTTP 调用云端服务进行语音转文字。
 *
 * <p>支持的后端：
 * <ul>
 *   <li>OpenAI Whisper API (api.openai.com)</li>
 *   <li>兼容 OpenAI 接口的第三方服务（如 Groq whisper-large-v3 等）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * ApiSpeechRecognizer recognizer = new ApiSpeechRecognizer(
 *     "https://api.openai.com/v1/audio/transcriptions",
 *     "sk-xxx",
 *     "whisper-1",
 *     16000,
 *     1,
 *     16
 * );
 * recognizer.initialize(null); // API 模式无需本地模型目录
 * String text = recognizer.recognize(pcmData);
 * </pre>
 *
 * @author Code Assistant
 * @date 2026-01-31
 */
public class ApiSpeechRecognizer implements SpeechRecognizer, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ApiSpeechRecognizer.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"");

    private final String apiUrl;
    private final String apiKey;
    private final String modelName;
    private final int sampleRate;
    private final int channels;
    private final int bitsPerSample;
    private final Duration timeout;

    private final HttpClient httpClient;
    private final ExecutorService executor;

    private volatile boolean initialized;

    /**
     * 构造 API 语音识别器
     *
     * @param apiUrl       API 端点地址（如 https://api.openai.com/v1/audio/transcriptions）
     * @param apiKey       API 密钥
     * @param modelName    模型名称（如 whisper-1）
     * @param sampleRate   音频采样率（Hz）
     * @param channels     音频声道数
     * @param bitsPerSample 每个采样点的位数
     */
    public ApiSpeechRecognizer(
        String apiUrl,
        String apiKey,
        String modelName,
        int sampleRate,
        int channels,
        int bitsPerSample
    ) {
        this(apiUrl, apiKey, modelName, sampleRate, channels, bitsPerSample, DEFAULT_TIMEOUT);
    }

    /**
     * 构造 API 语音识别器（含自定义超时）
     *
     * @param apiUrl       API 端点地址
     * @param apiKey       API 密钥
     * @param modelName    模型名称
     * @param sampleRate   音频采样率
     * @param channels     音频声道数
     * @param bitsPerSample 每个采样点的位数
     * @param timeout      HTTP 请求超时时间
     */
    public ApiSpeechRecognizer(
        String apiUrl,
        String apiKey,
        String modelName,
        int sampleRate,
        int channels,
        int bitsPerSample,
        Duration timeout
    ) {
        this.apiUrl = Objects.requireNonNull(apiUrl, "API URL 不能为空");
        this.apiKey = Objects.requireNonNull(apiKey, "API Key 不能为空");
        this.modelName = Objects.requireNonNullElse(modelName, "whisper-1");
        this.sampleRate = sampleRate > 0 ? sampleRate : 16000;
        this.channels = channels > 0 ? channels : 1;
        this.bitsPerSample = bitsPerSample > 0 ? bitsPerSample : 16;
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "api-speech-recognizer");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 初始化 API 识别器
     * <p>对于 API 模式，无需加载本地模型，此方法仅标记初始化完成。
     *
     * @param modelDir 本方法忽略此参数（API 模式无需本地模型）
     */
    @Override
    public void initialize(Path modelDir) {
        this.initialized = true;
        logger.info("API 语音识别器已初始化，端点: {}, 模型: {}", apiUrl, modelName);
    }

    /**
     * 同步识别音频数据
     * <p>将 PCM 数据转换为 WAV 格式后发送至云端 API 进行识别。
     *
     * @param pcm 原始 16-bit PCM 字节数据
     * @return 识别出的文本结果。如果识别失败或无内容，则返回空字符串。
     */
    @Override
    public String recognize(byte[] pcm) {
        if (!initialized || pcm == null || pcm.length == 0) {
            return "";
        }

        Path tempFile = null;
        try {
            // 将 PCM 数据转换为 WAV 格式临时文件
            tempFile = Files.createTempFile("speech_" + UUID.randomUUID(), ".wav");
            byte[] wavData = WavWriter.pcmToWav(pcm, sampleRate, channels, bitsPerSample);
            Files.write(tempFile, wavData);

            // 构建 multipart/form-data 请求
            String boundary = "----ApiSpeechRecognizer" + UUID.randomUUID().toString().replace("-", "");
            byte[] requestBody = buildMultipartBody(tempFile, boundary);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warn("API 调用失败，状态码: {}, 响应: {}", response.statusCode(), response.body());
                return "";
            }

            return extractText(response.body());

        } catch (IOException e) {
            logger.error("API 语音识别 IO 异常: {}", e.getMessage());
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("API 语音识别被中断");
            return "";
        } catch (Exception e) {
            logger.error("API 语音识别失败: {}", e.getMessage());
            return "";
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // 忽略临时文件删除失败
                }
            }
        }
    }

    /**
     * 异步识别音频数据
     *
     * @param pcm      原始 PCM 字节数据
     * @param listener 识别结果监听器
     */
    @Override
    public void recognizeAsync(byte[] pcm, RecognitionListener listener) {
        executor.submit(() -> {
            if (listener == null) {
                return;
            }
            try {
                String result = recognize(pcm);
                listener.onResult(result);
            } catch (Exception e) {
                listener.onError("API 识别失败: " + e.getMessage());
            }
        });
    }

    /**
     * 关闭识别器，释放资源
     */
    @Override
    public void close() {
        initialized = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("API 语音识别器已关闭");
    }

    /**
     * 构建 multipart/form-data 请求体
     */
    private byte[] buildMultipartBody(Path audioFile, String boundary) throws IOException {
        String fileName = audioFile.getFileName().toString();
        byte[] fileContent = Files.readAllBytes(audioFile);

        StringBuilder builder = new StringBuilder();

        // file 字段
        builder.append("--").append(boundary).append("\r\n");
        builder.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        builder.append("Content-Type: audio/wav\r\n\r\n");

        byte[] headerBytes = builder.toString().getBytes(StandardCharsets.UTF_8);

        // model 字段
        String modelPart = "\r\n--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"model\"\r\n\r\n" +
            modelName + "\r\n";

        // language 字段 (可选，设置为中文以提升识别准确度)
        String languagePart = "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"language\"\r\n\r\n" +
            "zh\r\n";

        // response_format 字段
        String formatPart = "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"response_format\"\r\n\r\n" +
            "json\r\n";

        String endBoundary = "--" + boundary + "--\r\n";

        byte[] modelBytes = modelPart.getBytes(StandardCharsets.UTF_8);
        byte[] languageBytes = languagePart.getBytes(StandardCharsets.UTF_8);
        byte[] formatBytes = formatPart.getBytes(StandardCharsets.UTF_8);
        byte[] endBytes = endBoundary.getBytes(StandardCharsets.UTF_8);

        // 合并所有部分
        int totalLength = headerBytes.length + fileContent.length + modelBytes.length +
            languageBytes.length + formatBytes.length + endBytes.length;
        byte[] result = new byte[totalLength];

        int offset = 0;
        System.arraycopy(headerBytes, 0, result, offset, headerBytes.length);
        offset += headerBytes.length;
        System.arraycopy(fileContent, 0, result, offset, fileContent.length);
        offset += fileContent.length;
        System.arraycopy(modelBytes, 0, result, offset, modelBytes.length);
        offset += modelBytes.length;
        System.arraycopy(languageBytes, 0, result, offset, languageBytes.length);
        offset += languageBytes.length;
        System.arraycopy(formatBytes, 0, result, offset, formatBytes.length);
        offset += formatBytes.length;
        System.arraycopy(endBytes, 0, result, offset, endBytes.length);

        return result;
    }

    /**
     * 从 JSON 响应中提取识别文本
     */
    private String extractText(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            return "";
        }
        Matcher matcher = TEXT_PATTERN.matcher(jsonResponse);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return "";
    }

    /**
     * 反转义 JSON 字符串中的特殊字符
     */
    private String unescapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
}
