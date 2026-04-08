package com.classroomassistant.shared.platform.secure

actual class PlatformSecureStore actual constructor() : SecureStore {
    override suspend fun readString(key: String): String? =
        TODO("Back with Keychain Services.")

    override suspend fun writeString(key: String, value: String) {
        TODO("Back with Keychain Services.")
    }

    override suspend fun remove(key: String) {
        TODO("Delete value from Keychain.")
    }
}
