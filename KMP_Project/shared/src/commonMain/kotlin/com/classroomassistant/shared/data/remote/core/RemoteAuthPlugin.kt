package com.classroomassistant.shared.data.remote.core

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.Attributes
import io.ktor.util.AttributeKey

class RemoteAuthPluginConfig {
    var authProvider: RemoteAuthProvider = NoOpRemoteAuthProvider
}

val RemoteAuthPlugin = createClientPlugin(
    name = "RemoteAuthPlugin",
    createConfiguration = ::RemoteAuthPluginConfig,
) {
    val authProvider = pluginConfig.authProvider

    onRequest { request, _ ->
        val explicitAuth = request.attributes.getOrNull(RemoteRequestAuthAttributeKey)
        val authContext = request.attributes.getOrNull(RemoteAuthContextAttributeKey) ?: RemoteAuthContext()
        val resolvedAuth = explicitAuth ?: authProvider.provide(authContext)
        resolvedAuth.applyTo(request)
    }
}

private fun <T : Any> Attributes.getOrNull(key: AttributeKey<T>): T? =
    if (contains(key)) {
        get(key)
    } else {
        null
    }
