package com.classroomassistant.composeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.classroomassistant.composeapp.app.ClassroomAssistantApp
import com.classroomassistant.shared.data.local.db.AndroidDatabaseDriverFactory
import com.classroomassistant.shared.di.startSharedKoinIfNeeded
import com.russhwolf.settings.Settings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val componentContext = rememberAndroidComponentContext()
            ClassroomAssistantApp(
                componentContext = componentContext,
                bootstrapper = {
                    startSharedKoinIfNeeded(
                        settings = Settings(),
                        databaseDriverFactory = AndroidDatabaseDriverFactory(applicationContext),
                    )
                },
            )
        }
    }
}

@Composable
private fun rememberAndroidComponentContext(): ComponentContext {
    val lifecycle = remember { LifecycleRegistry() }
    val componentContext = remember(lifecycle) {
        DefaultComponentContext(lifecycle = lifecycle)
    }

    LaunchedEffect(lifecycle) {
        lifecycle.create()
        lifecycle.start()
        lifecycle.resume()
    }

    DisposableEffect(lifecycle) {
        onDispose {
            lifecycle.stop()
            lifecycle.destroy()
        }
    }

    return componentContext
}
