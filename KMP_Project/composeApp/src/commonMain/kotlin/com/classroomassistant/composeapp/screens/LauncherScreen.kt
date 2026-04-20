package com.classroomassistant.composeapp.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.classroomassistant.composeapp.designsystem.ActionButtonList
import com.classroomassistant.composeapp.designsystem.DestinationScaffold
import com.classroomassistant.composeapp.designsystem.SectionCard
import com.classroomassistant.composeapp.presenters.DestinationActionUi
import com.classroomassistant.composeapp.presenters.LauncherUiState

@Composable
fun LauncherScreen(
    uiState: LauncherUiState,
    onActionSelected: (DestinationActionUi) -> Unit,
) {
    DestinationScaffold(
        title = uiState.title,
        subtitle = uiState.subtitle,
        canNavigateBack = false,
    ) {
        SectionCard(
            title = "Startup Dependencies",
            body = "These are the root-level building blocks the KMP shell now initializes before feature presenters attach.",
        ) {
            uiState.startupDependencies.forEach { line ->
                Text(line)
            }
        }

        SectionCard(
            title = "Navigation Translation",
            body = "The shell reflects how the legacy roots entered monitoring and settings while reserving future top-level destinations explicitly.",
        ) {
            uiState.navigationNotes.forEach { line ->
                Text(line)
            }
        }

        SectionCard(
            title = "Deferred Root Branches",
            body = "These branches stay visible in the architecture, but their detailed implementations belong to later plan steps.",
        ) {
            uiState.deferredBranches.forEach { line ->
                Text(line)
            }
        }

        SectionCard(
            title = "Continue",
            body = "Use the real root destinations below to walk the future feature areas without bundling their logic into this shell step.",
        ) {
            ActionButtonList(
                actions = uiState.actions,
                onActionSelected = onActionSelected,
            )
        }
    }
}
