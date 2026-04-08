# KMP 跨平台重构技术架构方案

## 1. 目标与边界

本方案针对当前仓库的旧 Android 项目重新建立一个独立的 KMP 工程，不改动旧代码，只在 `KMP_Project/` 下新建跨平台工程。

当前旧项目的业务边界已经比较清晰，核心域主要有：

- `ai`：大模型请求、提示词、模型目录
- `speech`：唤醒词、ASR、静音检测、识别编排
- `audio`：录音、缓冲、WAV 写入
- `storage`：配置、录音、模型下载、用户偏好
- `session`：课堂监听会话编排
- `ui`：Android Compose UI 与桌面/旧 UI 控制器

重构目标：

- 第一优先级：iOS 可运行、可维护、可持续迁移
- 第二优先级：Android 原生作为 KMP 验证端
- 暂不考虑 Web
- 让 AI 能按固定模板批量翻译 Java 业务逻辑

## 2. 总体模块划分

建议只保留两个 Gradle 主模块，加一个 iOS 壳工程：

- `shared`：纯 Kotlin 业务核心，不放 Compose UI，不直接依赖 Android SDK / UIKit
- `composeApp`：共享 Compose UI、导航、主题、资源、Android 应用入口、iOS Compose 入口
- `iosApp`：Xcode 壳工程，仅负责嵌入 `composeApp` 生成的 framework

### 2.1 模块拓扑图

```text
旧工程（保持不动）
android/ + core/ + src/ + desktop/
            |
            | 仅做人工参考与 AI 翻译输入
            v
KMP_Project/
├─ shared
│  ├─ commonMain
│  │  ├─ core        基础模型、错误、结果包装、调度器接口
│  │  ├─ data        Ktor / SQLDelight / Settings / Repository
│  │  ├─ domain      UseCase、领域服务、编排
│  │  ├─ feature     monitoring / settings / model / answer
│  │  └─ platform    expect 声明：权限、音频会话、安全存储、文件路径、通知
│  ├─ androidMain    Android actual 实现
│  └─ iosMain        iOS actual 实现
│
├─ composeApp
│  ├─ commonMain
│  │  ├─ app         AppRoot、导航宿主、DI 启动
│  │  ├─ designsystem 主题、色板、组件
│  │  ├─ screens     Compose 页面
│  │  └─ presenters  UI state 收口与映射
│  ├─ androidMain    MainActivity、权限桥接、通知宿主
│  └─ iosMain        ComposeUIViewController 入口
│
└─ iosApp
   └─ Xcode wrapper
```

### 2.2 为什么这样划分

`shared` 的职责：

- 承接所有“可翻译为纯 Kotlin”的旧业务逻辑
- 统一数据模型、Repository、UseCase、平台抽象
- 让 AI 迁移时不需要频繁处理 Android Context / Activity / ViewModel / Fragment

`composeApp` 的职责：

- 承接所有共享 UI
- 统一导航栈、页面状态映射、资源组织
- 将 Android 现有 Compose 页面直接迁入 `commonMain`

这样做对 AI 很友好，因为它可以分两类任务执行：

- 任务 A：把 Java 业务翻译进 `shared`
- 任务 B：把旧 Android UI 映射进 `composeApp`

## 3. 推荐技术栈与版本

以下版本以“主流、成熟、对 KMP/iOS 友好、便于 AI 批量生成”为标准锁定：

| 分类 | 选型 | 版本 | 说明 |
| --- | --- | --- | --- |
| Kotlin | Kotlin Multiplatform | `2.2.20` | 采用保守稳定基线，优先兼容 iOS 与主流库 |
| UI | Compose Multiplatform | `1.10.3` | 共享 Android/iOS UI |
| Android Gradle Plugin | AGP | `8.11.1` | 新工程使用较新的 Android 构建基线 |
| 并发 | kotlinx.coroutines | `1.10.2` | 替换 Java 线程/回调/Handler |
| 序列化 | kotlinx.serialization | `1.9.0` | 替换 Gson |
| 网络 | Ktor Client | `3.4.1` | `commonMain + OkHttp + Darwin` 组合 |
| 数据库 | SQLDelight | `2.3.2` | 支持 common schema 与 Android/iOS driver |
| 依赖注入 | Koin | `4.1.1` | 使用当前稳定 BOM 线，避免抢跑新大版本 |
| 图片加载 | Coil 3 | `3.4.0` | 官方支持 Compose Multiplatform |
| 导航/生命周期 | Decompose | `3.3.0` | KMP-first，组件生命周期清晰，适合 iOS |
| 键值配置 | Multiplatform Settings | `1.3.0` | 迁移大量配置项最省力 |

### 3.1 核心库最终选型结论

- 网络请求：`Ktor 3.4.1`
- 数据库：`SQLDelight 2.3.2`
- 依赖注入：`Koin 4.1.1`
- 图片加载：`Coil 3.4.0`
- 生命周期/导航：`Decompose 3.3.0`

### 3.2 为什么不用 Voyager

Voyager 更轻，但本项目更适合 `Decompose`，原因是：

- iOS 优先时，组件生命周期、子栈、保活与状态恢复比“轻导航”更关键
- Decompose 的 `ComponentContext` 更适合把业务状态与页面生命周期收进 `shared`
- 对 AI 批量生成更稳定，因为导航模型是显式的组件树，不依赖 Android 风格习惯

## 4. 源码结构设计

建议在 `shared` 内部采用“包内分层 + feature 收口”的结构，不要一开始就拆太多 Gradle 子模块，否则 AI 会在跨模块跳转中损失效率。

```text
shared/src/commonMain/kotlin/com/classroomassistant/shared/
├─ core/
│  ├─ model/
│  ├─ result/
│  ├─ error/
│  └─ util/
├─ data/
│  ├─ remote/
│  │  ├─ llm/
│  │  ├─ modelcatalog/
│  │  └─ speech/
│  ├─ local/
│  │  ├─ db/
│  │  ├─ settings/
│  │  └─ secure/
│  └─ repository/
├─ domain/
│  ├─ model/
│  ├─ usecase/
│  └─ service/
├─ feature/
│  ├─ monitoring/
│  ├─ settings/
│  ├─ answer/
│  └─ models/
├─ di/
└─ platform/
   ├─ permission/
   ├─ audio/
   ├─ secure/
   ├─ storage/
   ├─ notification/
   └─ capability/
```

`composeApp` 建议结构：

```text
composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/
├─ app/
├─ navigation/
├─ designsystem/
├─ components/
├─ screens/
│  ├─ launcher/
│  ├─ monitoring/
│  ├─ settings/
│  ├─ models/
│  └─ diagnostics/
└─ preview/
```

## 5. 旧项目到新项目的包映射

| 旧包 | 新位置 | 迁移说明 |
| --- | --- | --- |
| `src/main/java/.../ai` | `shared/data/remote/llm` + `shared/domain/usecase` | 拆分为 API client、DTO、UseCase |
| `src/main/java/.../audio` | `shared/domain/service/audio` + `shared/platform/audio` | 共通算法进 commonMain，设备采集进 actual |
| `src/main/java/.../speech` | `shared/feature/monitoring` + `shared/platform/audio` | 识别编排共通，底层引擎平台化 |
| `src/main/java/.../storage` | `shared/data/local` + `shared/data/repository` | 配置、模型状态、录音元数据集中 |
| `src/main/java/.../session` | `shared/feature/monitoring` | 课堂会话编排核心迁入 commonMain |
| `android/.../ui` | `composeApp/screens` | 现有 Compose 页面优先直接搬迁 |
| `android/.../platform` | `shared/androidMain/platform` | 改写为 actual 实现 |

## 6. Java 业务逻辑迁移策略

这是整个项目成败的关键。AI 的迁移策略不能按“文件复制”，必须按“领域批处理”执行。

### 6.1 迁移总原则

- 先迁模型与接口，再迁实现
- 先迁纯逻辑，再迁平台能力
- 先把 callback/listener 改成 `suspend` / `Flow`
- 先把数据结构 Kotlin 化，再做业务重构
- 不把任何 `Context`、`Activity`、`Handler`、`Toast`、`SharedPreferences` 带进 `commonMain`

### 6.2 AI 批量翻译的五步法

1. 建立迁移清单  
   先按领域列出旧文件，不按目录散打。

2. 先翻 DTO / model / enum  
   这些最稳定，适合作为 AI 上下文锚点。

3. 翻接口层  
   例如 `LLMClient`、`SpeechRecognizer`、`ModelRepository`，统一签名。

4. 翻服务实现  
   把 Java 里的状态机、控制流迁成 Kotlin 协程与不可变 state。

5. 最后补平台 actual  
   权限、存储、音频、通知、后台能力全部最后填。

### 6.3 Java -> KMP Kotlin 的强制替换规则

| 旧写法 | 新写法 |
| --- | --- |
| `interface Callback` | `suspend fun` 或 `Flow<T>` |
| `CopyOnWriteArrayList + listener` | `StateFlow` / `SharedFlow` |
| `AtomicReference<State>` | `MutableStateFlow<State>` |
| `ExecutorService / Thread` | `CoroutineScope + Dispatchers` |
| `Handler/Looper` | `CoroutineDispatcher` |
| `Gson` | `kotlinx.serialization` |
| `OkHttp` 直接调用 | `Ktor Client` |
| `Properties` 配置读取 | `SettingsRepository` + typed config model |
| `java.io.File`/`Path` 直连 commonMain | 通过 `platform/storage` 抽象 |

### 6.4 推荐迁移批次

批次 1：

- `constants`
- `core model`
- `PromptTemplate`
- 配置数据类

批次 2：

- `ConfigManager`
- `UserPreferences`
- `ModelDescriptor`
- `ModelRepository` 的纯数据逻辑

批次 3：

- `LLMClient`
- `OpenAiModelCatalogService`
- AI provider 路由逻辑

批次 4：

- `SessionState`
- `ClassSessionManager` / `CoreSessionManager`
- 监听流程状态机

批次 5：

- 平台相关录音、权限、文件、通知、后台执行

## 7. UI 转换路径

### 7.1 当前项目的现实情况

当前 Android 端主 UI 已经基本是 Compose，几乎没有传统 `layout/` XML 页面负担。这意味着：

- 现有 `SettingsScreen.kt`、`MainActivity.kt` 中的大量 Compose 代码可以直接迁
- 优先把 Android Compose 页面抽离到 `composeApp/commonMain`
- 原生 Android 代码只保留 Activity、权限回调、通知、文件分享等壳层

### 7.2 XML -> Compose Multiplatform 的映射规则

| Android 旧控件 | Compose Multiplatform |
| --- | --- |
| `LinearLayout(vertical)` | `Column` |
| `LinearLayout(horizontal)` | `Row` |
| `FrameLayout` / `RelativeLayout` | `Box` |
| `RecyclerView` | `LazyColumn` / `LazyRow` |
| `TextView` | `Text` |
| `EditText` | `OutlinedTextField` |
| `Button` | `Button` / `OutlinedButton` |
| `SwitchCompat` | `Switch` |
| `RadioGroup` | `Column + RadioButton` |
| `ProgressBar` | `CircularProgressIndicator` / `LinearProgressIndicator` |

### 7.3 Android Compose -> Compose Multiplatform 的迁移规则

以下内容可以基本直接搬：

- `Column` / `Row` / `LazyColumn`
- `Material3`
- 页面状态模型
- 大部分表单和卡片布局

以下内容必须抽象：

- `LocalContext`
- `Toast`
- `NotificationManager`
- `rememberLauncherForActivityResult`
- `ContextCompat.checkSelfPermission`
- `FileProvider`
- `Intent`

统一做法：

- 页面只触发 `intent`
- 页面只读取 `UiState`
- 所有系统调用通过 `shared/platform/*` 抽象下沉

### 7.4 导航改造

旧 Android 用 `navigation-compose` 的部分，新工程统一迁到 `Decompose`：

- 根组件：`RootComponent`
- 页面栈：`ChildStack<Config, Child>`
- 页面状态：`Component + StateFlow`
- Compose 层只负责 `Children(stack = ...)`

这样做的好处：

- 业务状态可以在 `shared` 管
- Android/iOS 导航模型一致
- AI 迁移时不会混入 Fragment/NavController 思维

## 8. 原生能力对接方案

### 8.1 通用原则

所有平台差异都收敛到 `shared/platform/*` 下的 `expect/actual`。

`commonMain` 只知道“能力”，不知道“Android / iOS API 名字”。

### 8.2 必须抽象的平台能力

- 权限：麦克风、通知、语音识别
- 安全存储：API Token、Secret、Speech Key
- 普通配置：用户偏好、开关项
- 文件路径：models、recordings、cache
- 音频会话：录音前后切换、路由、会话激活
- 通知/提醒：唤醒提示、静音提醒
- 能力探测：是否支持后台持续监听、本地 ASR、本地唤醒词

### 8.3 iOS 特殊风险必须前置设计

这一点必须在架构层说清楚：

- iOS 不应假设能像 Android 前台服务那样长期后台麦克风监听
- iOS 的后台执行受系统强约束，很多场景会被挂起
- 如果未来要做 iOS 端持续监听，产品能力应设计为“前台监听优先，后台能力降级为可选”

因此必须引入 `PlatformCapabilities`：

- `ContinuousBackgroundMonitoring`
- `LocalWakeWord`
- `LocalStreamingAsr`
- `SecureCredentialStorage`

业务层永远先查 capability，再决定是否展示或启用功能。

## 9. 数据层架构

### 9.1 网络层

使用 `Ktor` 的原因：

- KMP 原生支持 Android/iOS
- `OkHttp` 只留给 Android engine
- 统一插件体系，AI 容易批量生成 provider client

推荐组织方式：

```text
shared/data/remote/
├─ llm/
│  ├─ LlmApi.kt
│  ├─ OpenAiCompatibleApi.kt
│  ├─ dto/
│  └─ mapper/
├─ speech/
└─ modelcatalog/
```

### 9.2 数据库层

使用 `SQLDelight` 的原因：

- schema 显式，方便 AI 理解和生成
- SQL 文件天然是迁移契约
- iOS/Android driver 成熟

推荐表：

- `setting_entry`
- `monitoring_event`
- `recording_entry`
- `model_installation`
- `answer_history`

第一阶段不要把所有配置都塞数据库：

- 高频配置先走 `Multiplatform Settings`
- 结构化历史记录、模型状态、录音元数据进 `SQLDelight`

## 10. DI 与初始化

Koin 只做三层模块：

- `platformModule`
- `dataModule`
- `featureModule`

初始化路径：

1. Android `Application` / `MainActivity` 启动 Koin
2. iOS `ComposeUIViewController` 入口启动 Koin
3. `composeApp` 拿到 `RootComponent`
4. `RootComponent` 组装各子组件

## 11. 迁移里程碑

### M1：基础设施可运行

- `shared` 编译通过
- Ktor / SQLDelight / Koin 初始化通过
- Android 与 iOS 都能打开空白 Compose 壳

### M2：配置与模型管理迁移

- 设置页可读写
- 模型状态可展示
- Token 可安全存储

### M3：AI 回答链路打通

- iOS 与 Android 均可发起文本问答
- 模型目录与 provider 切换可运行

### M4：监听流程迁移

- 先 Android 验证本地监听链路
- 再根据 iOS capability 做前台监听版本

### M5：体验对齐

- 日志、诊断、权限引导、错误恢复

## 12. 结论

最终建议是：

- 用 `shared` 承接所有纯 Kotlin 业务与平台抽象
- 用 `composeApp` 承接共享 UI 与导航
- 用 `Decompose + Koin + Ktor + SQLDelight + Coil 3`
- 用 `expect/actual` 兜住 iOS 权限、音频会话、安全存储与能力差异
- 用“按领域分批”的 AI 翻译流程替代零散文件迁移

这会比“直接把旧 Android 改成 KMP”更稳，也更适合后续大规模 AI 自动生成。
