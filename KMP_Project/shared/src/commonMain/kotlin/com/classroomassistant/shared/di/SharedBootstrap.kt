package com.classroomassistant.shared.di

import com.classroomassistant.shared.data.local.db.DatabaseDriverFactory
import com.russhwolf.settings.Settings
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

fun startSharedKoinIfNeeded(
    settings: Settings,
    databaseDriverFactory: DatabaseDriverFactory,
): Koin {
    runCatching { GlobalContext.get() }
        .getOrNull()
        ?.let { return it }

    return startKoin {
        modules(
            sharedModules(
                settings = settings,
                databaseDriverFactory = databaseDriverFactory,
            ),
        )
    }.koin
}
