package com.classroomassistant.android.platform

import android.content.Context
import android.content.SharedPreferences
import com.classroomassistant.core.platform.PlatformPreferences

/**
 * Android 偏好设置存储实现
 * 使用 SharedPreferences
 */
class AndroidPreferences(context: Context) : PlatformPreferences {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "classroom_assistant_prefs",
        Context.MODE_PRIVATE
    )

    override fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    override fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun flush() {
        // SharedPreferences.apply() 已经异步提交
        // 如果需要同步可以用 commit()
        prefs.edit().commit()
    }
}
