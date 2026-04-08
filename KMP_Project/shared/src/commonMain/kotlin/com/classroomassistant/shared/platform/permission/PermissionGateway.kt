package com.classroomassistant.shared.platform.permission

interface PermissionGateway {
    suspend fun status(permission: AppPermission): PermissionStatus
    suspend fun request(permission: AppPermission): PermissionStatus
    fun openSystemSettings()
}

expect class PlatformPermissionGateway() : PermissionGateway
