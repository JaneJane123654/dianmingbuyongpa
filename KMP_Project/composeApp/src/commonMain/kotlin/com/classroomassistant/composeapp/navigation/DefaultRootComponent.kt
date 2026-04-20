package com.classroomassistant.composeapp.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.classroomassistant.composeapp.presenters.DestinationActionUi
import com.classroomassistant.composeapp.presenters.LauncherPresenter
import com.classroomassistant.composeapp.presenters.LauncherUiState
import com.classroomassistant.composeapp.presenters.PlaceholderPresenter
import com.classroomassistant.composeapp.presenters.PlaceholderUiState
import com.classroomassistant.composeapp.presenters.RootDestination
import com.classroomassistant.composeapp.presenters.SummaryLineUi
import com.classroomassistant.shared.core.util.AppLogger
import com.classroomassistant.shared.core.util.info
import com.classroomassistant.shared.data.local.settings.ConfigDefaultSettingKeys
import com.classroomassistant.shared.data.local.settings.PreferenceSettingKeys
import com.classroomassistant.shared.data.local.settings.TypedSettingsStore
import com.classroomassistant.shared.platform.capability.AppCapability
import com.classroomassistant.shared.platform.capability.PlatformCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

private const val ROOT_TAG = "RootComponent"

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val typedSettingsStore: TypedSettingsStore,
    private val platformCapabilities: PlatformCapabilities,
    private val appLogger: AppLogger,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<RootConfig>()

    override val stack: Value<ChildStack<RootConfig, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = RootConfig.serializer(),
            initialConfiguration = RootConfig.Launcher,
            handleBackButton = false,
            childFactory = ::createChild,
        )

    init {
        appLogger.info(
            tag = ROOT_TAG,
            message = "Shared root shell initialized with launcher destination.",
            eventCode = "ROOT_SHELL_READY",
        )
    }

    override fun navigateTo(destination: RootDestination) {
        val config = destination.toConfig()
        appLogger.info(
            tag = ROOT_TAG,
            message = "Navigating to ${destination.name.lowercase()} placeholder.",
            eventCode = "ROOT_NAVIGATE",
        )
        navigation.pushNew(config)
    }

    override fun onBackRequested() {
        appLogger.info(
            tag = ROOT_TAG,
            message = "Popping root navigation stack.",
            eventCode = "ROOT_BACK",
        )
        navigation.pop()
    }

    @Suppress("UnusedParameter")
    private fun createChild(
        config: RootConfig,
        componentContext: ComponentContext,
    ): RootComponent.Child = when (config) {
        RootConfig.Launcher -> RootComponent.Child.Launcher(
            presenter = StaticLauncherPresenter(buildLauncherState()),
        )

        RootConfig.Monitoring -> RootComponent.Child.Monitoring(
            presenter = StaticPlaceholderPresenter(buildMonitoringState()),
        )

        RootConfig.Settings -> RootComponent.Child.Settings(
            presenter = StaticPlaceholderPresenter(buildSettingsState()),
        )

        RootConfig.Models -> RootComponent.Child.Models(
            presenter = StaticPlaceholderPresenter(buildModelsState()),
        )

        RootConfig.Diagnostics -> RootComponent.Child.Diagnostics(
            presenter = StaticPlaceholderPresenter(buildDiagnosticsState()),
        )
    }

    private fun buildLauncherState(): LauncherUiState {
        val audioLookback = typedSettingsStore.get(PreferenceSettingKeys.AudioLookbackSeconds)
        val defaultProvider = typedSettingsStore.get(ConfigDefaultSettingKeys.AiProviderDefault)

        return LauncherUiState(
            title = "Classroom Assistant",
            subtitle = "Decompose root shell with explicit shared top-level destinations.",
            startupDependencies = listOf(
                "Shared Koin bootstrap guarded behind a single startup function.",
                "Typed settings store and SQLDelight database driver are created before the root shell.",
                "Platform capability declarations are available so later screens can hide unsupported flows.",
            ),
            navigationNotes = listOf(
                "Legacy Android booted through LauncherActivity before entering the monitoring screen.",
                "Legacy desktop booted straight into monitoring and opened settings in a modal window.",
                "The KMP shell reserves launcher, monitoring, settings, models, and diagnostics as explicit routes instead of burying everything inside one screen.",
                "Current defaults already visible from shared storage: lookback ${audioLookback}s, AI provider $defaultProvider.",
            ),
            deferredBranches = listOf(
                "Runtime permission prompting stays reserved for the Android and iOS bridge steps.",
                "Crash-summary banners and update dialogs are reserved in the diagnostics destination.",
                "Feature presenters for monitoring, settings, and model management stay isolated to later plan steps.",
            ),
            actions = launcherActions(),
        )
    }

    private fun buildMonitoringState(): PlaceholderUiState {
        val keywords = typedSettingsStore.get(PreferenceSettingKeys.Keywords).ifBlank { "(not configured yet)" }
        val lookbackSeconds = typedSettingsStore.get(PreferenceSettingKeys.AudioLookbackSeconds)
        val vadEnabled = typedSettingsStore.get(PreferenceSettingKeys.VadEnabled)

        return PlaceholderUiState(
            destination = RootDestination.Monitoring,
            title = "Monitoring",
            subtitle = "Reserved shell for the migrated monitoring flow.",
            summary = "Legacy main screens controlled monitoring, logs, direct AI questions, and monitoring-session lifecycle from the root. This placeholder keeps that destination alive without bundling session logic early.",
            quickFacts = listOf(
                SummaryLineUi("Wake keywords", keywords),
                SummaryLineUi("Audio lookback", "$lookbackSeconds seconds"),
                SummaryLineUi("VAD enabled", if (vadEnabled) "Yes" else "No"),
            ),
            legacyBehaviorNotes = listOf(
                "Legacy controllers disabled start/stop actions based on monitoring state.",
                "Android root requested microphone and notification permissions from the main route.",
                "Root-level logs, recognition text, and AI answer panels all lived on the monitoring screen.",
            ),
            reservedScopes = listOf(
                "Session state machine translation from CoreSessionManager and ClassSessionManager.",
                "Monitoring timeline, logs, and AI answer panel presenters.",
                "Permission and crash-summary root banners once bridge layers exist.",
            ),
            actions = siblingActions(current = RootDestination.Monitoring),
        )
    }

    private fun buildSettingsState(): PlaceholderUiState {
        val aiProvider = typedSettingsStore.get(PreferenceSettingKeys.AiProvider)
        val logMode = typedSettingsStore.get(PreferenceSettingKeys.LogMode)
        val backgroundKeepAlive = typedSettingsStore.get(PreferenceSettingKeys.BackgroundKeepAliveEnabled)

        return PlaceholderUiState(
            destination = RootDestination.Settings,
            title = "Settings",
            subtitle = "Shared route reserved for the future settings feature.",
            summary = "Legacy roots opened a large settings surface that mixed AI provider settings, wake-word tuning, logging options, update checks, and model downloads. This shell splits the route out early so later presenter work can stay focused.",
            quickFacts = listOf(
                SummaryLineUi("AI provider", aiProvider),
                SummaryLineUi("Log mode", logMode),
                SummaryLineUi("Background keep-alive", if (backgroundKeepAlive) "Enabled" else "Disabled"),
            ),
            legacyBehaviorNotes = listOf(
                "Desktop opened settings as a modal dialog and reloaded monitoring after closing.",
                "Android pushed settings as a route and popped back after save.",
                "Saving settings in legacy code could reinitialize active monitoring when key inputs changed.",
            ),
            reservedScopes = listOf(
                "Typed settings repository translation and save/apply behavior.",
                "Shared settings presenter and Compose screen migration.",
                "Permission links and secure-credential editing once native bridges are ready.",
            ),
            actions = siblingActions(current = RootDestination.Settings),
        )
    }

    private fun buildModelsState(): PlaceholderUiState {
        val selectedModel = typedSettingsStore.get(PreferenceSettingKeys.KwsCurrentModel).ifBlank { "(default reserved)" }
        val asrLocalEnabled = typedSettingsStore.get(PreferenceSettingKeys.AsrLocalEnabled)
        val localWakeWord = capabilityLine(AppCapability.LocalWakeWord)

        return PlaceholderUiState(
            destination = RootDestination.Models,
            title = "Models",
            subtitle = "Reserved route for model installation and readiness state.",
            summary = "Legacy settings and model-download controllers mixed KWS/ASR/VAD readiness, active model selection, and download progress. This top-level placeholder keeps that surface distinct so the later repository migration has a clear landing zone.",
            quickFacts = listOf(
                SummaryLineUi("Current KWS model", selectedModel),
                SummaryLineUi("Local ASR route", if (asrLocalEnabled) "Enabled" else "Disabled"),
                SummaryLineUi("Local wake-word capability", localWakeWord),
            ),
            legacyBehaviorNotes = listOf(
                "Legacy Android kept model downloads inside settings while desktop also exposed model state alongside settings.",
                "Both legacy roots treated model readiness as a startup blocker for some monitoring flows.",
                "Custom model URLs and failure reasons were visible from the old settings surfaces.",
            ),
            reservedScopes = listOf(
                "Model repository and installation-state translation.",
                "Model selection presenter and download-progress UI.",
                "Custom URL install flows and readiness diagnostics.",
            ),
            actions = siblingActions(current = RootDestination.Models),
        )
    }

    private fun buildDiagnosticsState(): PlaceholderUiState {
        val backgroundMonitoring = capabilityLine(AppCapability.ContinuousBackgroundMonitoring)
        val secureStorage = capabilityLine(AppCapability.SecureCredentialStorage)
        val localStreamingAsr = capabilityLine(AppCapability.LocalStreamingAsr)

        return PlaceholderUiState(
            destination = RootDestination.Diagnostics,
            title = "Diagnostics",
            subtitle = "Reserved route for startup banners, crash summaries, and capability visibility.",
            summary = "Legacy Android surfaced crash summaries, update checks, permission prompts, and runtime pipeline notes from the root. Those root-level concerns are reserved here so later hardening work can attach without rearranging navigation again.",
            quickFacts = listOf(
                SummaryLineUi("Background monitoring capability", backgroundMonitoring),
                SummaryLineUi("Secure credential storage", secureStorage),
                SummaryLineUi("Local streaming ASR", localStreamingAsr),
            ),
            legacyBehaviorNotes = listOf(
                "Android root displayed startup failure content when dependency construction crashed.",
                "Crash summaries and update prompts were emitted from root-level effects.",
                "Desktop startup registered keyboard shortcuts and shutdown hooks at the app shell.",
            ),
            reservedScopes = listOf(
                "Crash summary surface and diagnostics timeline.",
                "Update-check prompts and app-wide notices.",
                "Root-level lifecycle reporting and platform diagnostics hooks.",
            ),
            actions = siblingActions(current = RootDestination.Diagnostics),
        )
    }

    private fun capabilityLine(capability: AppCapability): String {
        val availability = platformCapabilities.availability(capability)
        val prefix = if (availability.supported) "Supported" else "Unsupported"
        val note = availability.note?.takeIf { it.isNotBlank() }
        return if (note == null) {
            prefix
        } else {
            "$prefix. $note"
        }
    }

    private fun launcherActions(): List<DestinationActionUi> = listOf(
        DestinationActionUi(
            destination = RootDestination.Monitoring,
            title = "Enter Monitoring",
            description = "Follow the legacy launcher-to-main transition into the monitoring shell.",
        ),
        DestinationActionUi(
            destination = RootDestination.Settings,
            title = "Open Settings",
            description = "Reserve the route where the shared settings presenter and screen will land.",
        ),
        DestinationActionUi(
            destination = RootDestination.Models,
            title = "Open Models",
            description = "Split model management out from legacy settings before the repository migration.",
        ),
        DestinationActionUi(
            destination = RootDestination.Diagnostics,
            title = "Open Diagnostics",
            description = "Keep a stable place for crash summaries, update prompts, and root notices.",
        ),
    )

    private fun siblingActions(
        current: RootDestination,
    ): List<DestinationActionUi> = launcherActions().filterNot { it.destination == current }

    private fun RootDestination.toConfig(): RootConfig = when (this) {
        RootDestination.Launcher -> RootConfig.Launcher
        RootDestination.Monitoring -> RootConfig.Monitoring
        RootDestination.Settings -> RootConfig.Settings
        RootDestination.Models -> RootConfig.Models
        RootDestination.Diagnostics -> RootConfig.Diagnostics
    }

    @Serializable
    private sealed interface RootConfig {
        @Serializable
        data object Launcher : RootConfig

        @Serializable
        data object Monitoring : RootConfig

        @Serializable
        data object Settings : RootConfig

        @Serializable
        data object Models : RootConfig

        @Serializable
        data object Diagnostics : RootConfig
    }
}

private class StaticLauncherPresenter(
    state: LauncherUiState,
) : LauncherPresenter {
    private val stateFlow = MutableStateFlow(state)

    override val uiState: StateFlow<LauncherUiState> = stateFlow
}

private class StaticPlaceholderPresenter(
    state: PlaceholderUiState,
) : PlaceholderPresenter {
    private val stateFlow = MutableStateFlow(state)

    override val uiState: StateFlow<PlaceholderUiState> = stateFlow
}
