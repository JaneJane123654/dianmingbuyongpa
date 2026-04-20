package com.classroomassistant.shared.data.remote.core

data class RemoteHttpClientProfile(
    val name: String,
    val connectTimeoutMillis: Long? = null,
    val requestTimeoutMillis: Long? = null,
    val socketTimeoutMillis: Long? = null,
    val enableNetworkLogs: Boolean = true,
)

object RemoteHttpClientProfiles {
    val Default = RemoteHttpClientProfile(
        name = "default",
        connectTimeoutMillis = 20_000,
        requestTimeoutMillis = 30_000,
        socketTimeoutMillis = 30_000,
    )

    val AiInference = RemoteHttpClientProfile(
        name = "ai_inference",
        connectTimeoutMillis = 20_000,
        requestTimeoutMillis = 180_000,
        socketTimeoutMillis = 120_000,
    )

    val ModelCatalog = RemoteHttpClientProfile(
        name = "model_catalog",
        connectTimeoutMillis = 8_000,
        requestTimeoutMillis = 12_000,
        socketTimeoutMillis = 12_000,
    )
}
