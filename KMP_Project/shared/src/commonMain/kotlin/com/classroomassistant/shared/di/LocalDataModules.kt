package com.classroomassistant.shared.di

import com.classroomassistant.shared.data.local.db.AppDatabaseFactory
import com.classroomassistant.shared.data.local.db.DatabaseDriverFactory
import com.classroomassistant.shared.data.local.settings.DefaultTypedSettingsStore
import com.classroomassistant.shared.data.local.settings.TypedSettingsStore
import com.classroomassistant.shared.db.AppDatabase
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module

fun localDataModule(
    settings: Settings,
    databaseDriverFactory: DatabaseDriverFactory,
): Module = module {
    single<Settings> { settings }
    single<DatabaseDriverFactory> { databaseDriverFactory }
    single { AppDatabaseFactory(get()) }
    single<AppDatabase> { get<AppDatabaseFactory>().create() }
    single { get<AppDatabase>().appDatabaseQueries }
    single<TypedSettingsStore> { DefaultTypedSettingsStore(get()) }
}
