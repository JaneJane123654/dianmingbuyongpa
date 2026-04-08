package com.classroomassistant.shared.platform.secure

actual class PlatformSecureStore actual constructor() : SecureStore {
    override suspend fun readString(key: String): String? =
        TODO("Back with Android Keystore or EncryptedSharedPreferences.")

    override suspend fun writeString(key: String, value: String) {
        TODO("Back with Android Keystore or EncryptedSharedPreferences.")
    }

    override suspend fun remove(key: String) {
        TODO("Delete secure key from Android-backed encrypted store.")
    }
}
