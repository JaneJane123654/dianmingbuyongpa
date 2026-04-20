package com.classroomassistant.composeapp.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ClassroomAssistantLightColors = lightColorScheme()
private val ClassroomAssistantDarkColors = darkColorScheme()

@Composable
fun ClassroomAssistantAppTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isSystemInDarkTheme()) {
        ClassroomAssistantDarkColors
    } else {
        ClassroomAssistantLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
