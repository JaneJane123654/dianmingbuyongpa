package com.classroomassistant.shared.data.local.settings

import com.russhwolf.settings.Settings

interface TypedSettingsStore {
    fun has(key: TypedSettingKey<*>): Boolean

    fun get(key: StringSettingKey): String
    fun put(key: StringSettingKey, value: String)

    fun get(key: IntSettingKey): Int
    fun put(key: IntSettingKey, value: Int)

    fun get(key: LongSettingKey): Long
    fun put(key: LongSettingKey, value: Long)

    fun get(key: BooleanSettingKey): Boolean
    fun put(key: BooleanSettingKey, value: Boolean)

    fun remove(key: TypedSettingKey<*>)
}

class DefaultTypedSettingsStore(
    private val settings: Settings,
) : TypedSettingsStore {
    override fun has(key: TypedSettingKey<*>): Boolean = settings.hasKey(key.storageKey)

    override fun get(key: StringSettingKey): String =
        settings.getString(key.storageKey, key.defaultValue)

    override fun put(key: StringSettingKey, value: String) {
        settings.putString(key.storageKey, value)
    }

    override fun get(key: IntSettingKey): Int =
        settings.getInt(key.storageKey, key.defaultValue)

    override fun put(key: IntSettingKey, value: Int) {
        settings.putInt(key.storageKey, value)
    }

    override fun get(key: LongSettingKey): Long =
        settings.getLong(key.storageKey, key.defaultValue)

    override fun put(key: LongSettingKey, value: Long) {
        settings.putLong(key.storageKey, value)
    }

    override fun get(key: BooleanSettingKey): Boolean =
        settings.getBoolean(key.storageKey, key.defaultValue)

    override fun put(key: BooleanSettingKey, value: Boolean) {
        settings.putBoolean(key.storageKey, value)
    }

    override fun remove(key: TypedSettingKey<*>) {
        settings.remove(key.storageKey)
    }
}
