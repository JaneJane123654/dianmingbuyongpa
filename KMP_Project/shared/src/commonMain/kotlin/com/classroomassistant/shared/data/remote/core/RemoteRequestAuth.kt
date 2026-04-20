package com.classroomassistant.shared.data.remote.core

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey

data class RemoteAuthContext(
    val scope: String? = null,
    val providerName: String? = null,
)

fun interface RemoteAuthProvider {
    suspend fun provide(context: RemoteAuthContext): RemoteRequestAuth
}

object NoOpRemoteAuthProvider : RemoteAuthProvider {
    override suspend fun provide(context: RemoteAuthContext): RemoteRequestAuth = RemoteRequestAuth.None
}

data class RemoteHeader(
    val name: String,
    val value: String,
)

sealed interface RemoteRequestAuth {
    data object None : RemoteRequestAuth

    data class Token(
        val token: String,
        val scheme: String = "Bearer",
        val headerName: String = HttpHeaders.Authorization,
    ) : RemoteRequestAuth

    data class Header(
        val name: String,
        val value: String,
    ) : RemoteRequestAuth

    data class Headers(
        val values: List<RemoteHeader>,
    ) : RemoteRequestAuth
}

internal val RemoteRequestAuthAttributeKey = AttributeKey<RemoteRequestAuth>("remote.request.auth")
internal val RemoteAuthContextAttributeKey = AttributeKey<RemoteAuthContext>("remote.auth.context")

fun HttpRequestBuilder.remoteAuth(auth: RemoteRequestAuth) {
    attributes.put(RemoteRequestAuthAttributeKey, auth)
}

fun HttpRequestBuilder.remoteAuthContext(context: RemoteAuthContext) {
    attributes.put(RemoteAuthContextAttributeKey, context)
}

internal fun RemoteRequestAuth.applyTo(builder: HttpRequestBuilder) {
    when (this) {
        RemoteRequestAuth.None -> Unit
        is RemoteRequestAuth.Token -> {
            val normalizedToken = token.trim()
            if (normalizedToken.isBlank()) {
                return
            }
            val normalizedValue = if (scheme.isBlank()) {
                normalizedToken
            } else {
                "${scheme.trim()} $normalizedToken"
            }
            builder.headers.remove(headerName)
            builder.headers.append(headerName, normalizedValue)
        }

        is RemoteRequestAuth.Header -> {
            val normalizedName = name.trim()
            val normalizedValue = value.trim()
            if (normalizedName.isBlank() || normalizedValue.isBlank()) {
                return
            }
            builder.headers.remove(normalizedName)
            builder.headers.append(normalizedName, normalizedValue)
        }

        is RemoteRequestAuth.Headers -> {
            values.forEach { header ->
                val normalizedName = header.name.trim()
                val normalizedValue = header.value.trim()
                if (normalizedName.isBlank() || normalizedValue.isBlank()) {
                    return@forEach
                }
                builder.headers.remove(normalizedName)
                builder.headers.append(normalizedName, normalizedValue)
            }
        }
    }
}
