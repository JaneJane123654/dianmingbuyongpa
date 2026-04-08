# KMP_Project

这是为旧 Android 项目设计的全新 Kotlin Multiplatform + Compose Multiplatform 重构工程骨架。

目标原则：

- 旧仓库代码保持不动，只在 `KMP_Project/` 下新建工程。
- `shared` 只放纯 Kotlin 业务、数据、平台抽象，避免 Android/iOS UI 代码污染。
- `composeApp` 只放共享 Compose UI、导航、主题与平台入口。
- iOS 作为第一优先级设计目标，Android 作为第二优先级验证端。
- 目录和分层要适合 AI 按“包 -> 领域 -> 文件”批量翻译与生成。

核心文档：

- [架构方案](./docs/ARCHITECTURE.md)
- [AI 迁移手册](./docs/MIGRATION_PLAYBOOK.md)
- [iOS 壳工程说明](./iosApp/README.md)

建议的首批落地顺序：

1. 先跑通 `shared` 的网络、数据库、配置与权限抽象。
2. 再把现有 Android Compose 页面迁到 `composeApp/commonMain`。
3. 然后逐批翻译 Java 业务逻辑到 `shared/commonMain`。
4. 最后补齐 `shared/iosMain` 的权限、音频会话、通知与安全存储实现。

建议工程树：

```text
KMP_Project/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
├─ gradle/libs.versions.toml
├─ shared/
├─ composeApp/
├─ iosApp/
└─ docs/
```
