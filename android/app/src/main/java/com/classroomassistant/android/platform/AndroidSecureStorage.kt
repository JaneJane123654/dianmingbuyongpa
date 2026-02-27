package com.classroomassistant.android.platform

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.classroomassistant.core.platform.PlatformSecureStorage

/**
 * Android 安全存储实现
 * 使用 EncryptedSharedPreferences (Android Keystore)
 */
class AndroidSecureStorage(context: Context) : PlatformSecureStorage {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "classroom_assistant_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun storeSecure(key: String, value: String?) {
        if (value.isNullOrEmpty()) {
            securePrefs.edit().remove(key).apply()
        } else {
            securePrefs.edit().putString(key, value).apply()
        }
    }

    override fun retrieveSecure(key: String): String? {
        return securePrefs.getString(key, null)
    }

    override fun deleteSecure(key: String) {
        securePrefs.edit().remove(key).apply()
    }

    override fun hasSecure(key: String): Boolean {
        return securePrefs.contains(key)
    }
}
