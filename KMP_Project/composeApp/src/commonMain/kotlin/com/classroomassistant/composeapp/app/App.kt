package com.classroomassistant.composeapp.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.classroomassistant.composeapp.designsystem.ClassroomAssistantAppTheme
import com.classroomassistant.composeapp.navigation.DefaultRootComponent
import com.classroomassistant.composeapp.navigation.RootComponent
import com.classroomassistant.composeapp.screens.AppStartupFailureScreen
import com.classroomassistant.composeapp.screens.LauncherScreen
import com.classroomassistant.composeapp.screens.PlaceholderDestinationScreen
import com.classroomassistant.shared.core.result.AppResult
import com.classroomassistant.shared.core.util.AppLogger
import com.classroomassistant.shared.core.util.tagged
import org.koin.core.context.GlobalContext

private const val ROOT_TAG = "ComposeRootShell"

@Composable
fun ClassroomAssistantApp(
    componentContext: ComponentContext,
    bootstrapper: () -> Unit,
) {
    val startupResult = remember(componentContext) {
        AppResult.catching {
            bootstrapper()
            val koin = GlobalContext.get()
            DefaultRootComponent(
                componentContext = componentContext,
                typedSettingsStore = koin.get(),
                platformCapabilities = koin.get(),
                appLogger = koin.get<AppLogger>().tagged(ROOT_TAG),
            )
        }
    }

    ClassroomAssistantAppTheme {
        when (startupResult) {
            is AppResult.Success -> ClassroomAssistantRootShell(startupResult.value)
            is AppResult.Failure -> AppStartupFailureScreen(startupResult.error.message)
            is AppResult.Cancelled -> AppStartupFailureScreen(startupResult.error.message)
        }
    }
}

@Composable
private fun ClassroomAssistantRootShell(rootComponent: RootComponent) {
    val currentStack = rootComponent.stack.subscribeAsState().value
    val canNavigateBack = currentStack.backStack.isNotEmpty()

    when (val child = currentStack.active.instance) {
        is RootComponent.Child.Launcher -> {
            LauncherScreen(
                uiState = child.presenter.uiState.collectAsState().value,
                onActionSelected = { action -> rootComponent.navigateTo(action.destination) },
            )
        }

        is RootComponent.Child.Monitoring -> {
            PlaceholderDestinationScreen(
                uiState = child.presenter.uiState.collectAsState().value,
                canNavigateBack = canNavigateBack,
                onBackRequested = rootComponent::onBackRequested,
                onActionSelected = { action -> rootComponent.navigateTo(action.destination) },
            )
        }

        is RootComponent.Child.Settings -> {
            PlaceholderDestinationScreen(
                uiState = child.presenter.uiState.collectAsState().value,
                canNavigateBack = canNavigateBack,
                onBackRequested = rootComponent::onBackRequested,
                onActionSelected = { action -> rootComponent.navigateTo(action.destination) },
            )
        }

        is RootComponent.Child.Models -> {
            PlaceholderDestinationScreen(
                uiState = child.presenter.uiState.collectAsState().value,
                canNavigateBack = canNavigateBack,
                onBackRequested = rootComponent::onBackRequested,
                onActionSelected = { action -> rootComponent.navigateTo(action.destination) },
            )
        }

        is RootComponent.Child.Diagnostics -> {
            PlaceholderDestinationScreen(
                uiState = child.presenter.uiState.collectAsState().value,
                canNavigateBack = canNavigateBack,
                onBackRequested = rootComponent::onBackRequested,
                onActionSelected = { action -> rootComponent.navigateTo(action.destination) },
            )
        }
    }
}
