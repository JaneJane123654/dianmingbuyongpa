# Speech 模块

## 模块描述

语音模块提供唤醒词检测、安静检测与语音识别的统一接口，当前提供占位实现以便工程可运行。

## 类/接口说明

| 类/接口 | 说明 |
| --- | --- |
| `WakeWordDetector` | 唤醒词检测接口 |
| `SilenceDetector` | 安静检测接口 |
| `SpeechRecognizer` | 语音识别接口 |
| `SpeechServices` | 服务集合 |
| `SpeechEngineFactory` | 语音引擎工厂 |
| `SherpaOnnxJNI` | Sherpa JNI 接口声明 |
| `SherpaWakeWordDetector` | Sherpa 唤醒词检测实现 |
| `SherpaSilenceDetector` | Sherpa 安静检测实现 |
| `SherpaSpeechRecognizer` | Sherpa 语音识别实现 |
| `WakeWordListener` | 唤醒词回调 |
| `SilenceListener` | 安静回调 |
| `RecognitionListener` | 识别回调 |

## 更新日志

| 版本 | 日期 | 更新内容 |
| --- | --- | --- |
| 1.0 | 2026-01-30 | 初始化语音模块骨架 |
| 1.1 | 2026-01-30 | 增加 Sherpa 适配层骨架 |
