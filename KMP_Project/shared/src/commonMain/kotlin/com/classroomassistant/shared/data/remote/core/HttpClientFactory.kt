package com.classroomassistant.shared.data.remote.core

import com.classroomassistant.shared.core.util.AppLogger
import com.classroomassistant.shared.core.util.debug
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class HttpClientFactory(
    private val json: Json,
    private val appLogger: AppLogger,
) {
    fun create(
        profile: RemoteHttpClientProfile = RemoteHttpClientProfiles.Default,
        authProvider: RemoteAuthProvider = NoOpRemoteAuthProvider,
        configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {},
    ): HttpClient = HttpClient {
        configureCommon(profile, authProvider)
        configure()
    }

    fun <T : HttpClientEngineConfig> create(
        engineFactory: HttpClientEngineFactory<T>,
        profile: RemoteHttpClientProfile = RemoteHttpClientProfiles.Default,
        authProvider: RemoteAuthProvider = NoOpRemoteAuthProvider,
        configure: io.ktor.client.HttpClientConfig<T>.() -> Unit = {},
    ): HttpClient = HttpClient(engineFactory) {
        configureCommon(profile, authProvider)
        configure()
    }

    private fun <T : HttpClientEngineConfig> io.ktor.client.HttpClientConfig<T>.configureCommon(
        profile: RemoteHttpClientProfile,
        authProvider: RemoteAuthProvider,
    ) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(json)
        }

        install(DefaultRequest) {
            headers.remove(HttpHeaders.Accept)
            headers.append(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        install(HttpTimeout) {
            profile.connectTimeoutMillis?.let { connectTimeoutMillis = it }
            profile.requestTimeoutMillis?.let { requestTimeoutMillis = it }
            profile.socketTimeoutMillis?.let { socketTimeoutMillis = it }
        }

        install(Logging) {
            logger = KtorAppLogger(
                appLogger = appLogger,
                profileName = profile.name,
            )
            level = if (profile.enableNetworkLogs) {
                LogLevel.INFO
            } else {
                LogLevel.NONE
            }
            sanitizeHeader { header ->
                header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                    header.equals("x-api-key", ignoreCase = true) ||
                    header.equals("api-key", ignoreCase = true)
            }
        }

        install(RemoteAuthPlugin) {
            this.authProvider = authProvider
        }
    }
}

private class KtorAppLogger(
    private val appLogger: AppLogger,
    private val profileName: String,
) : Logger {
    override fun log(message: String) {
        appLogger.debug(
            tag = "RemoteHttpClient",
            message = "[$profileName] $message",
            eventCode = "REMOTE_HTTP",
        )
    }
}
