package com.classroomassistant.shared.platform.storage

data class AppDirectories(
    val appSupportDir: String,
    val cacheDir: String,
    val modelDir: String,
    val recordingDir: String,
)

expect class PlatformDirectories() {
    fun resolve(): AppDirectories
}
