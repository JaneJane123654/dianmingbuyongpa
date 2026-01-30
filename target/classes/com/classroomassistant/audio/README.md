# Audio 模块

## 模块描述

音频模块负责桌面端麦克风采集、PCM 数据环形缓冲以及回溯读取，为后续唤醒词检测、安静检测和语音识别提供稳定的输入。

## 类/接口说明

| 类/接口 | 说明 |
| --- | --- |
| `AudioRecorder` | 录音接口，定义启动/停止/回溯读取 |
| `AudioRecorderDesktop` | 桌面端实现，基于 Java Sound API |
| `AudioListener` | 音频帧监听器接口 |
| `CircularBuffer` | 固定容量的环形缓冲区 |
| `AudioFormatSpec` | 音频格式规格描述 |

## 使用示例

```java
AudioRecorder recorder = new AudioRecorderDesktop(audioConfig, healthMonitor);
recorder.addListener(new AudioListener() {
    @Override
    public void onAudioReady(byte[] data) {
        // 处理音频帧
    }

    @Override
    public void onError(String error) {
        // 处理错误
    }
});
recorder.startRecording();
```

## 注意事项

1. 音频采样率统一使用 16kHz，16-bit PCM 小端序。
2. 回溯秒数范围为 1-300 秒。
3. 录音停止后需及时释放资源。

## 依赖关系

- `com.classroomassistant.storage.AudioConfig`
- `com.classroomassistant.utils.Validator`
- Java Sound API（`javax.sound.sampled`）

## 更新日志

| 版本 | 日期 | 更新内容 |
| --- | --- | --- |
| 1.0 | 2026-01-29 | 初始化音频采集与环形缓冲区实现 |
