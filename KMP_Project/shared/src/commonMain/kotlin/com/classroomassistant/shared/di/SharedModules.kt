package com.classroomassistant.shared.di

import com.classroomassistant.shared.core.util.AppDispatchers
import com.classroomassistant.shared.core.util.AppLogger
import com.classroomassistant.shared.core.util.DefaultAppDispatchers
import com.classroomassistant.shared.core.util.NoOpAppLogger
import com.classroomassistant.shared.data.local.db.DatabaseDriverFactory
import com.classroomassistant.shared.platform.audio.AudioSessionController
import com.classroomassistant.shared.platform.audio.PlatformAudioSessionController
import com.classroomassistant.shared.platform.capability.PlatformCapabilities
import com.classroomassistant.shared.platform.permission.PermissionGateway
import com.classroomassistant.shared.platform.permission.PlatformPermissionGateway
import com.classroomassistant.shared.platform.secure.PlatformSecureStore
import com.classroomassistant.shared.platform.secure.SecureStore
import com.classroomassistant.shared.platform.storage.PlatformDirectories
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module

fun coreModule(): Module = module {
    single<AppDispatchers> { DefaultAppDispatchers }
    single<AppLogger> { NoOpAppLogger }
}

fun platformModule(): Module = module {
    single<PermissionGateway> { PlatformPermissionGateway() }
    single<SecureStore> { PlatformSecureStore() }
    single { PlatformDirectories() }
    single<AudioSessionController> { PlatformAudioSessionController() }
    single { PlatformCapabilities() }
}

fun dataModule(): Module = module {
    includes(remoteDataModule())
}

fun dataModule(
    settings: Settings,
    databaseDriverFactory: DatabaseDriverFactory,
): Module = module {
    includes(
        localDataModule(
            settings = settings,
            databaseDriverFactory = databaseDriverFactory,
        ),
        remoteDataModule(),
    )
}

fun featureModule(): Module = module { }

fun sharedModules(): List<Module> = listOf(
    coreModule(),
    platformModule(),
    dataModule(),
    featureModule(),
)

fun sharedModules(
    settings: Settings,
    databaseDriverFactory: DatabaseDriverFactory,
): List<Module> = listOf(
    coreModule(),
    platformModule(),
    dataModule(
        settings = settings,
        databaseDriverFactory = databaseDriverFactory,
    ),
    featureModule(),
)
