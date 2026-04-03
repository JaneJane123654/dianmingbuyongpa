# Classroom Assistant / 学生课堂挂机程序

[中文](#中文简介) | [English](#english-overview)

---

## 中文简介

课堂助手是一个面向远程课堂的语音辅助工具。它可以自动监听课堂内容，在关键时刻快速给出可读文本和 AI 建议，减少漏听、漏记、反应慢的问题。

### 适用场景

- 远程上课时，需要自动记录课堂重点
- 临时离开电脑时，希望不中断课堂信息跟踪
- 课后复盘时，希望快速回看核心内容

### 核心功能

- 唤醒词检测：检测到关键词后自动触发处理
- 语音识别（ASR）：将课堂语音转换为文本
- 安静检测（VAD）：静音超时自动触发识别
- AI 问答：根据识别内容生成简洁回答建议
- 录音留存：按需保留录音，便于复盘
- 多平台支持：Desktop（JavaFX）+ Android

### 功能亮点

- 统一模型接入：基于 LangChain4j，对接多个模型平台
- 自动流式降级：模型不支持流式时自动回退，优先保证成功返回
- 本地优先：核心音频能力可本地运行，降低依赖
- 配置清晰：支持自定义模型平台、模型名、Base URL、Token

### 快速开始

#### 环境要求

- JDK 11+
- Maven 3.8+
- Android Studio（仅 Android 开发需要）

#### 构建

```bash
# Desktop
mvn -pl desktop -am clean package

# Android
# 使用 Android Studio 打开 android/ 目录
```

### 项目结构

```text
core/      平台无关核心逻辑（会话、语音接口、配置接口）
desktop/   桌面端实现（JavaFX）
android/   Android 端实现
src/       兼容层与历史模块
```

---

## English Overview

Classroom Assistant is a speech-driven helper for remote classes. It listens to class audio, converts speech to text, and generates concise AI suggestions so you can miss less and respond faster.

### Use Cases

- Capture key points automatically during online classes
- Keep class tracking active when you step away briefly
- Review important class content quickly after class

### Key Features

- Wake-word trigger for automatic processing
- ASR (speech-to-text) for classroom audio
- VAD-based silence timeout trigger
- AI answer generation from recognized content
- Optional recording retention for replay
- Multi-platform support: Desktop + Android

### Highlights

- Unified model integration with LangChain4j
- Automatic fallback when streaming is unsupported
- Local-first audio pipeline for better stability
- Simple configuration for provider/model/base URL/token

### Quick Start

Requirements:

- JDK 11+
- Maven 3.8+
- Android Studio (for Android module)

Build:

```bash
# Desktop
mvn -pl desktop -am clean package

# Android
# Open android/ in Android Studio
```
