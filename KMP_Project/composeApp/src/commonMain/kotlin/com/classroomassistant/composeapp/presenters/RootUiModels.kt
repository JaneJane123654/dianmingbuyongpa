package com.classroomassistant.composeapp.presenters

import kotlinx.coroutines.flow.StateFlow

enum class RootDestination {
    Launcher,
    Monitoring,
    Settings,
    Models,
    Diagnostics,
}

data class DestinationActionUi(
    val destination: RootDestination,
    val title: String,
    val description: String,
)

data class SummaryLineUi(
    val label: String,
    val value: String,
)

data class LauncherUiState(
    val title: String,
    val subtitle: String,
    val startupDependencies: List<String>,
    val navigationNotes: List<String>,
    val deferredBranches: List<String>,
    val actions: List<DestinationActionUi>,
)

data class PlaceholderUiState(
    val destination: RootDestination,
    val title: String,
    val subtitle: String,
    val summary: String,
    val quickFacts: List<SummaryLineUi>,
    val legacyBehaviorNotes: List<String>,
    val reservedScopes: List<String>,
    val actions: List<DestinationActionUi>,
)

interface LauncherPresenter {
    val uiState: StateFlow<LauncherUiState>
}

interface PlaceholderPresenter {
    val uiState: StateFlow<PlaceholderUiState>
}
