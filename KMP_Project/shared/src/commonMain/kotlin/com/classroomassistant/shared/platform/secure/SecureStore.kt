package com.classroomassistant.shared.platform.secure

interface SecureStore {
    suspend fun readString(key: String): String?
    suspend fun writeString(key: String, value: String)
    suspend fun remove(key: String)
}

expect class PlatformSecureStore() : SecureStore
