package com.classroomassistant.shared.platform.storage

actual class PlatformDirectories actual constructor() {
    actual fun resolve(): AppDirectories =
        TODO("Resolve Android filesDir/cacheDir and create models/recordings subfolders.")
}
