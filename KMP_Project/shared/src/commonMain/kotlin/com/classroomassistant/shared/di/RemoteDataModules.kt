package com.classroomassistant.shared.di

import com.classroomassistant.shared.data.remote.core.HttpClientFactory
import com.classroomassistant.shared.data.remote.core.RemoteCallExecutor
import com.classroomassistant.shared.data.remote.core.SharedRemoteJson
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

fun remoteDataModule(): Module = module {
    single<Json> { SharedRemoteJson }
    single { HttpClientFactory(get(), get()) }
    single { RemoteCallExecutor(get()) }
}
