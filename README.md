# 课堂助手 (Classroom Assistant) - 学生课堂挂机程序

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java Version](https://img.shields.io/badge/Java-11%2B-orange.svg)]()

学生课堂挂机程序（Classroom Assistant）是一款支持多平台（Windows/macOS/Linux/Android）的实时辅助工具。它能够实时录制课堂音频，自动识别唤醒词（如学生姓名），并在被提问时通过 AI 大模型生成回答建议，帮助学生在远程或线下课堂中更轻松地应对。

## 核心功能

- **??? 实时录音与回溯**：点击“上课”开始录音，支持环形缓冲区回溯，确保触发时能获取前 N 分钟的音频（P0）。
- **?? 唤醒词检测**：支持配置多个唤醒词变体，实时监测并在命中时立即通知（P0）。
- **?? AI 问答助手**：触发唤醒后，自动将语音转文字，并结合 AI 大模型生成回复建议，支持流式输出（P0）。
- **?? 自动静音检测 (VAD)**：连续安静超过阈值时自动提醒，防止错过重要信息（P1）。
- **?? 多语言支持**：内置中英双语切换，支持国际化配置。
- **?? 性能监控**：内置详尽的性能测试套件，确保 ASR、VAD 及 AI 模块在高并发下的稳定性。

## 项目结构

`
classroom-assistant/
├── core/                      # 核心模块：平台无关的业务逻辑、AI 客户端、语音接口
├── desktop/                   # 桌面模块：基于 JavaFX 的桌面端 UI 及其实现
├── android/                   # 安卓模块：基于 Compose 的移动端实现
└── changelog/                 # 项目变更日志与迭代记录
`

## 技术栈

| 领域 | 技术选型 | 备注 |
|---|---|---|
| **开发语言** | Java 11+ / Kotlin | 核心逻辑使用 Java 11 |
| **UI 框架** | JavaFX 21+ (桌面) / Compose (安卓) | 界面与逻辑分离 |
| **AI 框架** | LangChain4j 0.34.0+ | 统一接入多种大模型 |
| **音频处理** | Java Sound API / AudioRecord | 跨平台音频抽象 |
| **日志框架** | SLF4J + Logback | 统一日志规范 |
| **构建工具** | Maven 3.8+ / Gradle | 多模块管理 |

## 快速开始

### 前置要求

- JDK 11 或更高版本
- Maven 3.8+
- Android Studio (仅构建安卓版需要)

### 构建与运行

1. **构建核心模块**：
   `ash
   cd core
   mvn clean install
   `

2. **启动桌面版**：
   `ash
   cd desktop
   mvn clean package
   # 运行 Launcher
   java -jar target/classroom-assistant-desktop-3.0.0-all.jar
   `

3. **构建安卓版**：
   - 使用 Android Studio 打开 \ndroid\ 目录。
   - 同步 Gradle 并运行 \ssembleDebug\。

## 配置说明

项目配置文件位于 \src/main/resources/config/application.properties\：

- \pp.language\: 语言设置（\zh\ 为中文，\en\ 为英文）。
- \i.model.name\: 指定使用的 AI 模型。
- \udio.wake-word\: 配置唤醒词及其变体（逗号分隔）。

## 开发与贡献

请在开发前参考 \changelog/\ 目录下的最新迭代记录。本项目遵循严格的编码规范，核心业务逻辑的单测覆盖率要求达到 100%。

---
*GitHub Copilot 辅助生成 - 2026-03-03*
