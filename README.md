# Classroom Assistant / 学生课堂挂机程序

[中文](#中文简介) | [English](#english-overview)

---

## 中文简介

课堂点名应答挂机助手，是一款专为高校学生打造的课堂音频辅助工具，精准解决「水课不想听、又怕被点名提问」的核心痛点。程序全程后台静默运行，实时监听课堂音频，当检测到用户预设的本人姓名被呼叫时，自动回溯点名前的课堂语音内容，通过语音识别转写为文本，再由 AI 快速生成贴合课堂语境、逻辑通顺的口语化应答话术，学生只需照着文本朗读即可完美应对点名提问，全程无需分心听课，轻松实现课堂 “挂机”、私事学习两不误。

安卓安装包下载：https://github.com/JaneJane123654/dianmingbuyongpa/releases

### 适用场景

- 水课全程挂机：面对内容无意义、学分要求低的水课，无需耗费精力听课，程序后台值守，仅在点名提问时触发响应
- 随机点名应急应答：老师课堂随机抽点学生回答问题，无需提前预习、全程听课，自动回溯提问内容，秒级生成标准应答
- 课堂分身处理私事：课堂上需要处理备考、实习、作业、竞赛等更重要的个人事务，不用分心关注课堂内容也能从容应对抽查

### 核心功能

- 姓名精准唤醒：支持自定义预设本人姓名，精准识别课堂中的姓名呼叫
- 音频智能回溯：内置循环音频缓存机制，检测到姓名呼叫后，自动回溯点名前预设时长的课堂音频，抓取老师的提问内容
- 实时语音识别（ASR）：高精度语音转文字能力，将回溯的课堂音频、提问内容转写为清晰文本
- AI 智能应答生成：基于识别的提问内容，快速生成贴合课程语境、逻辑完整、长度适中的口语化应答文本，适配课堂回答场景
- 静音检测（VAD）：智能识别课堂静音时段，（静音可能是老师在询问）
- 录音按需留存：支持自定义开启录音留存功能，便于课后回看提问与应答内容，复盘课堂情况
- 全平台覆盖：支持桌面端（Windows/macOS，JavaFX）+ Android 移动端，线下线上课堂均可便捷使用

### 功能亮点

- 本地优先隐私保护：核心音频监听、姓名识别、缓存能力均可本地运行，课堂音频数据不上传，充分保护用户隐私
- 统一大模型接入：基于 LangChain4j 框架，无缝对接国内外主流大模型平台，支持自定义模型配置、接口地址、API Token，灵活适配不同用户的使用需求
- 智能流式降级：当所选模型不支持流式输出时，自动切换至普通请求模式，优先保证应答内容稳定、快速返回，不耽误课堂应答
- 高度自定义配置：支持自定义姓名关键词、音频回溯时长、识别灵敏度等
- 开箱即用低门槛：提供一键构建包，无需复杂配置，简单设置姓名与模型参数即可快速上手使用

### 快速开始

#### 环境要求

- JDK 11+
- Maven 3.8+
- Android Studio（仅 Android 开发需要）

#### 构建

# 桌面端 Desktop

mvn -pl desktop -am clean package

# 移动端 Android

# 使用 Android Studio 打开 android/ 目录即可构建

### 项目结构

core/      平台无关核心逻辑（姓名唤醒、音频缓存、语音接口、配置管理）
desktop/   桌面端完整实现（基于JavaFX）
android/   Android移动端完整实现
src/       兼容层与历史兼容模块
---

## English Overview

Classroom Roll Call Response Assistant is a speech-driven tool built for college students, perfectly solving the core pain point: you don’t want to sit through low-value filler courses, but fear being cold-called or asked questions in class. The program runs silently in the background, monitoring classroom audio in real time. When it detects your pre-set full name is called, it automatically backtracks the classroom audio recorded before the roll call, converts the speech to text via ASR, and quickly generates a contextually appropriate, logical, and natural spoken response with AI. You can simply read the generated text aloud to respond perfectly, with no need to pay attention to the lecture at all — easily stay "AFK" in class while handling your own important tasks.

Android installation package download：https://github.com/JaneJane123654/dianmingbuyongpa/releases



### Use Cases

- Full-time AFK for filler courses: For low-value mandatory courses with meaningless content, no need to spend energy on the lecture. The program runs in the background and only triggers a response when your name is called.
- Emergency response to random cold calls: When the teacher randomly calls on students to answer questions, no need to preview in advance or listen to the whole lecture. The program automatically backtracks the question and generates a standard response in seconds.
- Handle personal tasks during class: When you need to focus on exam preparation, internships, homework, competitions or other more important matters in class, you can calmly respond to spot checks without being distracted by the lecture.

### Key Features

- Accurate Name Wake-Up: Supports custom preset of your full name, accurately recognizes name calls in class
- Intelligent Audio Backtracking: Built-in loop audio cache mechanism. After detecting your name is called, it automatically backtracks the classroom audio for a preset duration before the roll call, capturing the teacher’s question content
- Real-time ASR (Automatic Speech Recognition): High-precision speech-to-text capability, converting the backtracked classroom audio and question content into clear text
- AI Smart Response Generation: Based on the recognized question content, quickly generates a colloquial response text that fits the course context, with complete logic and moderate length, perfectly adapted to in-class answering scenarios.
- VAD (Voice Activity Detection): Intelligently identifies silent periods in the classroom (silence may be the teacher asking questions)
- On-demand Recording Retention: Supports custom enabling of recording retention, making it easy to review question and response content after class.
- Multi-platform Support: Full support for Desktop (Windows/macOS, JavaFX) + Android mobile terminals, easy to use for both offline and online classes

### Highlights

- Local-first Privacy Protection: Core audio monitoring, name recognition, and cache functions can all run locally. Classroom audio data is not uploaded, fully protecting user privacy.
- Unified LLM Integration: Based on the LangChain4j framework, seamlessly connects with mainstream large model platforms worldwide, supports custom model configuration, base URL, and API Token, flexibly adapting to different user needs.
- Automatic Streaming Fallback: Automatically switches to standard request mode when the selected model does not support streaming output, prioritizing stable and fast return of response content to avoid delays in classroom answering.
- Highly Customizable Configuration: Supports custom name keywords, audio backtracking duration, recognition sensitivity, response style, etc., to adapt to the questioning scenarios of different courses and teachers.
- Out-of-the-box Usability: Provides one-click build packages, no complex configuration required, you can get started quickly with simple settings of your name and model parameters.

### Quick Start

#### Requirements

- JDK 11+
- Maven 3.8+
- Android Studio (for Android module development only)

#### Build

# Desktop

mvn -pl desktop -am clean package

# Android

# Open the android/ directory in Android Studio to build

### Project Structure

core/      Platform-independent core logic (name wake-up, audio cache, speech interface, configuration management)
desktop/   Full desktop implementation (based on JavaFX)
android/   Full Android mobile implementation
src/       Compatibility layer and legacy compatible modules
