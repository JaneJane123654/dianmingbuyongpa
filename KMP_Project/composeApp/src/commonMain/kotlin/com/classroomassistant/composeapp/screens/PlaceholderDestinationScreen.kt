package com.classroomassistant.composeapp.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.classroomassistant.composeapp.designsystem.ActionButtonList
import com.classroomassistant.composeapp.designsystem.DestinationScaffold
import com.classroomassistant.composeapp.designsystem.SectionCard
import com.classroomassistant.composeapp.designsystem.SummaryLine
import com.classroomassistant.composeapp.presenters.DestinationActionUi
import com.classroomassistant.composeapp.presenters.PlaceholderUiState

@Composable
fun PlaceholderDestinationScreen(
    uiState: PlaceholderUiState,
    canNavigateBack: Boolean,
    onBackRequested: () -> Unit,
    onActionSelected: (DestinationActionUi) -> Unit,
) {
    DestinationScaffold(
        title = uiState.title,
        subtitle = uiState.subtitle,
        canNavigateBack = canNavigateBack,
        onBackRequested = onBackRequested,
    ) {
        SectionCard(
            title = "Route Summary",
            body = uiState.summary,
        )

        SectionCard(
            title = "Current Shared Facts",
            body = "These values already come from shared settings defaults or platform capability declarations.",
        ) {
            uiState.quickFacts.forEach { fact ->
                SummaryLine(
                    label = fact.label,
                    value = fact.value,
                )
            }
        }

        SectionCard(
            title = "Legacy Behavior",
            body = "The placeholder keeps the old root behavior visible so later migrations land in the right place.",
        ) {
            uiState.legacyBehaviorNotes.forEach { line ->
                Text(line)
            }
        }

        SectionCard(
            title = "Reserved Follow-Up Scope",
            body = "These items intentionally stay out of Step 04 so later plan files can translate them cleanly.",
        ) {
            uiState.reservedScopes.forEach { line ->
                Text(line)
            }
        }

        if (uiState.actions.isNotEmpty()) {
            SectionCard(
                title = "Navigate",
                body = "Jump across the reserved top-level destinations without changing the placeholder scope of this step.",
            ) {
                ActionButtonList(
                    actions = uiState.actions,
                    onActionSelected = onActionSelected,
                )
            }
        }
    }
}
