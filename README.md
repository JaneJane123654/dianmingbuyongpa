# 课堂助手 - 多平台版本

学生课堂挂机程序，支持桌面（Windows/macOS/Linux）和安卓平台。

## 项目结构

```
classroom-assistant/
├── pom.xml                    # 父 POM（多模块管理）
├── core/                      # 核心模块（平台无关）
│   ├── pom.xml
│   └── src/main/java/
│       └── com/classroomassistant/core/
│           ├── ai/            # AI 模块（LLM 客户端）
│           ├── audio/         # 音频格式定义
│           ├── constants/     # 常量定义
│           ├── platform/      # 平台抽象接口
│           ├── session/       # 会话管理
│           ├── speech/        # 语音识别接口
│           └── storage/       # 存储管理
├── desktop/                   # 桌面模块（JavaFX）
│   ├── pom.xml
│   └── src/main/java/
│       └── com/classroomassistant/desktop/
│           ├── platform/      # 桌面平台实现
│           ├── ui/            # JavaFX UI
│           ├── MainApp.java
│           └── Launcher.java
├── android/                   # 安卓模块（Gradle）
│   ├── build.gradle
│   ├── settings.gradle
│   └── app/
│       └── src/main/java/
│           └── com/classroomassistant/android/
│               ├── platform/  # 安卓平台实现
│               ├── ui/        # Compose UI
│               └── MainActivity.kt
└── src/                       # 原始代码（待迁移完成后可删除）
```

## 构建说明

### 前置条件

- JDK 11+
- Maven 3.8+
- Android Studio (用于安卓模块)

注：本项目以 JDK 11 为构建与运行目标。

### 构建 Core 模块

```bash
cd core
mvn clean install
```

### 构建桌面版

```bash
cd desktop
mvn clean package

# 运行
java -jar target/classroom-assistant-desktop-3.0.0-all.jar
```

### 构建安卓版

1. 先构建并安装 core 模块到本地 Maven 仓库：
   ```bash
   cd core
   mvn clean install
   ```

2. 用 Android Studio 打开 `android` 目录

3. 同步 Gradle 后构建 APK：
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

4. APK 位于 `android/app/build/outputs/apk/debug/`

## 平台抽象接口

| 接口 | 说明 | 桌面实现 | 安卓实现 |
|------|------|---------|---------|
| `PlatformAudioRecorder` | 音频录制 | javax.sound | AudioRecord |
| `PlatformPreferences` | 偏好设置 | java.util.prefs | SharedPreferences |
| `PlatformStorage` | 文件存储 | 标准文件系统 | Context.getFilesDir |
| `PlatformSecureStorage` | 安全存储 | AES-GCM | EncryptedSharedPreferences |
| `PlatformProvider` | 平台服务 | DesktopPlatformProvider | AndroidPlatformProvider |

## 开发指南

### 添加新功能

1. 在 `core` 模块中定义接口和通用逻辑
2. 在 `desktop` 模块中添加 JavaFX 实现
3. 在 `android` 模块中添加 Android 实现

### 语音识别

- **API 模式**：使用 Groq/OpenAI Whisper API（推荐，无需下载模型）
- **本地模式**：使用 Sherpa-ONNX（需下载模型文件）

### API Key 配置

1. 在设置界面配置 LLM API Key（支持 OpenAI、千帆、DeepSeek、Kimi）
2. 语音识别 API Key（Groq 提供免费额度）

## 版本历史

- v3.0.0 - 多平台架构重构，支持安卓

## 许可证

MIT License
