package com.classroomassistant.shared.di

import com.classroomassistant.shared.platform.audio.AudioSessionController
import com.classroomassistant.shared.platform.audio.PlatformAudioSessionController
import com.classroomassistant.shared.platform.capability.PlatformCapabilities
import com.classroomassistant.shared.platform.permission.PermissionGateway
import com.classroomassistant.shared.platform.permission.PlatformPermissionGateway
import com.classroomassistant.shared.platform.secure.PlatformSecureStore
import com.classroomassistant.shared.platform.secure.SecureStore
import com.classroomassistant.shared.platform.storage.PlatformDirectories
import org.koin.core.module.Module
import org.koin.dsl.module

fun sharedModules(): List<Module> = listOf(
    module {
        single<PermissionGateway> { PlatformPermissionGateway() }
        single<SecureStore> { PlatformSecureStore() }
        single { PlatformDirectories() }
        single<AudioSessionController> { PlatformAudioSessionController() }
        single { PlatformCapabilities() }
    }
)
