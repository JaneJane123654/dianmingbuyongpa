# iosApp

这里建议保留一个极薄的 Xcode 壳工程。

职责只包括：

- 嵌入 `composeApp` 生成的 iOS framework
- 提供应用图标、签名、Bundle 配置
- 如有必要，托管少量 Swift 入口代码

不建议把业务逻辑写回 Swift 层，除非是：

- Apple 审核相关的系统入口
- 无法优雅通过 KMP 暴露的极小型原生胶水层

推荐做法：

- 由 `composeApp` 暴露 `MainViewController()`
- `iosApp` 的 `ContentView.swift` 或 `AppDelegate` 只负责挂载它
