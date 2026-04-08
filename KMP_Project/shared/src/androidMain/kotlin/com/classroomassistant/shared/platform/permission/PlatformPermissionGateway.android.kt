package com.classroomassistant.shared.platform.permission

actual class PlatformPermissionGateway actual constructor() : PermissionGateway {
    override suspend fun status(permission: AppPermission): PermissionStatus =
        TODO("Bind to Android runtime permission APIs inside composeApp/androidMain.")

    override suspend fun request(permission: AppPermission): PermissionStatus =
        TODO("Use ActivityResultContracts or a dedicated Android permission coordinator.")

    override fun openSystemSettings() {
        TODO("Open Android application details settings screen.")
    }
}
