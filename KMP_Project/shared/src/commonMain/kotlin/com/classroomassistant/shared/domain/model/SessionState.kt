package com.classroomassistant.shared.domain.model

enum class SessionState {
    IDLE,
    MONITORING,
    TRIGGER_HANDLING,
    ERROR,
    ;

    val isActive: Boolean
        get() = this == MONITORING || this == TRIGGER_HANDLING

    val requiresManualRecovery: Boolean
        get() = this == ERROR
}
