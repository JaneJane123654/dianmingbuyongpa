package com.classroomassistant.shared.platform.permission

enum class AppPermission {
    Microphone,
    Notifications,
    SpeechRecognition,
}

enum class PermissionStatus {
    Granted,
    Denied,
    NotDetermined,
    Restricted,
    Unsupported,
}
