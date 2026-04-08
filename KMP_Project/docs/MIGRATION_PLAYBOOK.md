# AI 批量迁移执行手册

## 1. 迁移目标

把旧项目中的 Java/Android 业务逻辑，系统性迁移到 `shared` 与 `composeApp`，保证：

- 每一批迁移都可回归
- AI 的输入边界固定
- 不把平台代码误搬进 `commonMain`

## 2. AI 任务拆分规则

每次只给 AI 一个明确、单域、单输出目录任务。

推荐任务粒度：

- 一个 feature 一次
- 一个 repository 一次
- 一个 service 一次
- 一个 screen 一次

不要这样做：

- 一次性翻整个 `src/main/java`
- 一次性同时改 `shared` 与 `composeApp` 与 `iosMain`

## 3. 文件级迁移顺序

### 第一轮：稳定数据模型

优先迁移：

- `ErrorCode`
- `SessionState`
- `AudioConfig`
- `VadDefaults`
- `AiDefaults`
- `ModelDescriptor`
- `UserPreferences`

输出位置：

- `shared/domain/model`
- `shared/core/error`
- `shared/data/local/model`

### 第二轮：配置与存储

优先迁移：

- `ConfigManager`
- `PreferencesManager`
- `RecordingRepository`
- `ModelRepository`

迁移规则：

- 配置读取改成 typed repository
- `Properties` 改成 `Multiplatform Settings`
- 文件系统操作改成 `platform/storage` 抽象

### 第三轮：网络与 AI

优先迁移：

- `LLMConfig`
- `LLMClient`
- `DefaultLLMClient`
- `OpenAiModelCatalogService`
- `PromptTemplate`

迁移规则：

- 所有 DTO 改成 `@Serializable`
- 所有 API 改用 `Ktor`
- provider 兼容逻辑拆成 mapper + service

### 第四轮：会话编排

优先迁移：

- `CoreSessionManager`
- `ClassSessionManager`
- 监听/识别/回答状态机

迁移规则：

- listener 改为 `StateFlow`
- 事件流改为 `SharedFlow`
- 可取消任务改为 `CoroutineScope`

### 第五轮：UI 与导航

优先迁移：

- `SettingsScreen.kt`
- `MainScreen` 相关 Compose
- 启动页、日志页、模型页

迁移规则：

- Android 依赖全部抽到桥接层
- 页面只保留 `UiState + Intent`
- 导航统一改用 `Decompose`

## 4. Java -> Kotlin 翻译规范

### 4.1 类与接口

- Java POJO -> Kotlin `data class`
- Java enum -> Kotlin `enum class`
- util static method -> `object` 或顶层函数
- manager/service -> `class` + constructor injection

### 4.2 并发

- `AtomicBoolean` -> `MutableStateFlow<Boolean>`
- `AtomicReference<T>` -> `MutableStateFlow<T>`
- `synchronized` 代码块 -> `Mutex`
- 监听器回调 -> `Flow`

### 4.3 空值与错误

- Java `null` 宽松返回值 -> Kotlin 明确可空类型
- 异常横飞 -> `Result` 或领域错误封装

### 4.4 日志

- 日志语义保留
- 但不要把日志输出 API 带入业务核心
- 使用 `AppLogger` 接口注入

## 5. UI 映射规范

### 5.1 Android Compose 页面搬运规则

可以原样保留的部分：

- Material3 组件
- 表单和卡片层级
- 文本/按钮/列表布局

必须抽离的部分：

- `Toast`
- `Context`
- 权限请求
- 文件分享/安装
- Notification

### 5.2 推荐页面模式

每个页面都拆成三层：

1. `Component`  
   负责业务状态和 intent 处理

2. `UiState`
   负责可渲染数据

3. `Screen`
   纯 Compose，无平台逻辑

## 6. expect/actual 批量填充规范

AI 在填 `iosMain` 时，必须严格按下面顺序：

1. 先实现权限查询
2. 再实现权限请求
3. 再实现音频会话切换
4. 再实现安全存储
5. 最后实现通知与能力探测

不要一开始就把 iOS 业务写死到页面里。

## 7. 验收标准

每一批迁移完成后都要满足：

- `commonMain` 不引用 Android/iOS 类
- 页面可以用假数据预览
- repository 可以被单测
- DI 可独立初始化

## 8. 推荐 AI 提示模板

### 8.1 迁移业务类

```text
请将旧文件 X 迁移到 KMP_Project/shared/commonMain。
要求：
1. 去除所有 Android/Java 平台 API。
2. 使用 Kotlin 协程、Flow、data class、sealed interface。
3. 依赖通过构造函数注入。
4. 若遇到平台能力，改成 expect/actual 抽象，不要写 Android 实现。
5. 输出新文件、依赖关系和需要后续补 actual 的点。
```

### 8.2 迁移 Compose 页面

```text
请将旧 Android Compose 页面 X 迁移到 KMP_Project/composeApp/commonMain。
要求：
1. UI 结构尽量保留。
2. 删除所有 LocalContext/Toast/Intent/ActivityResult 直接调用。
3. 页面只接受 UiState 和 onIntent 回调。
4. 导航改成 Decompose 组件调用。
```

### 8.3 实现 iOS actual

```text
请为 shared/commonMain 下的 expect 声明补齐 iosMain actual。
要求：
1. 优先使用 Foundation / AVFoundation / UserNotifications / Security。
2. 返回明确的 capability 状态。
3. 对 iOS 不支持的能力不要伪实现，直接返回 unsupported。
4. 保持函数签名与 commonMain 完全一致。
```
