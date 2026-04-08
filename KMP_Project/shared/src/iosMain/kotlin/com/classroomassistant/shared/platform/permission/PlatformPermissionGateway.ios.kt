package com.classroomassistant.shared.platform.permission

actual class PlatformPermissionGateway actual constructor() : PermissionGateway {
    override suspend fun status(permission: AppPermission): PermissionStatus =
        TODO("Query AVAudioSession, UNUserNotificationCenter, and SFSpeechRecognizer authorization states.")

    override suspend fun request(permission: AppPermission): PermissionStatus =
        TODO("Request iOS runtime authorization and map native states to PermissionStatus.")

    override fun openSystemSettings() {
        TODO("Open UIApplication settings URL when user must change authorization manually.")
    }
}
