package com.classroomassistant.shared.domain.model

data class VadDefaults(
    val enabledDefault: Boolean,
    val quietThresholdSecondsDefault: Int,
) {
    companion object {
        const val DEFAULT_ENABLED = true
        const val DEFAULT_QUIET_THRESHOLD_SECONDS = 5

        val Default = VadDefaults(
            enabledDefault = DEFAULT_ENABLED,
            quietThresholdSecondsDefault = DEFAULT_QUIET_THRESHOLD_SECONDS,
        )
    }
}
