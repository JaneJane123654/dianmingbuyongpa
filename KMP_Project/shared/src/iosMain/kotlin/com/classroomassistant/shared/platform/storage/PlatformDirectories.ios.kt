package com.classroomassistant.shared.platform.storage

actual class PlatformDirectories actual constructor() {
    actual fun resolve(): AppDirectories =
        TODO("Resolve NSSearchPath directories and create models/recordings subfolders.")
}
