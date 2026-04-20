package com.classroomassistant.shared.data.remote.core

fun normalizeRemoteBaseUrl(raw: String?): String {
    var normalized = raw?.trim().orEmpty()
    while (normalized.endsWith("/")) {
        normalized = normalized.dropLast(1)
    }
    return normalized
}

fun requireRemoteBaseUrl(
    baseUrl: String?,
    serviceName: String? = null,
): String {
    val normalized = normalizeRemoteBaseUrl(baseUrl)
    if (normalized.isBlank()) {
        throw RemoteException.MissingBaseUrl(serviceName)
    }
    return normalized
}

fun appendRemotePath(
    baseUrl: String,
    suffix: String,
): String {
    val normalizedBaseUrl = normalizeRemoteBaseUrl(baseUrl)
    if (normalizedBaseUrl.isBlank()) {
        return ""
    }
    val normalizedSuffix = if (suffix.startsWith("/")) suffix else "/$suffix"
    if (normalizedBaseUrl.endsWith(normalizedSuffix)) {
        return normalizedBaseUrl
    }
    if (normalizedSuffix.startsWith("/v1/") && normalizedBaseUrl.endsWith("/v1")) {
        return normalizedBaseUrl + normalizedSuffix.removePrefix("/v1")
    }
    return normalizedBaseUrl + normalizedSuffix
}
