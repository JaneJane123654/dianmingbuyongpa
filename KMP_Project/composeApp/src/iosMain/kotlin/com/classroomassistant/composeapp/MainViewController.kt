package com.classroomassistant.composeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.classroomassistant.composeapp.app.ClassroomAssistantApp
import com.classroomassistant.shared.data.local.db.IosDatabaseDriverFactory
import com.classroomassistant.shared.di.startSharedKoinIfNeeded
import com.russhwolf.settings.Settings

fun MainViewController() = ComposeUIViewController {
    val componentContext = rememberIosComponentContext()
    ClassroomAssistantApp(
        componentContext = componentContext,
        bootstrapper = {
            startSharedKoinIfNeeded(
                settings = Settings(),
                databaseDriverFactory = IosDatabaseDriverFactory(),
            )
        },
    )
}

@Composable
private fun rememberIosComponentContext(): ComponentContext {
    val lifecycle = remember { LifecycleRegistry() }
    val componentContext = remember(lifecycle) {
        DefaultComponentContext(lifecycle = lifecycle)
    }

    DisposableEffect(lifecycle) {
        lifecycle.create()
        lifecycle.start()
        lifecycle.resume()

        onDispose {
            lifecycle.stop()
            lifecycle.destroy()
        }
    }

    return componentContext
}
