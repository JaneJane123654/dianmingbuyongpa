package com.classroomassistant.shared.data.remote.core

import kotlinx.serialization.json.Json

val SharedRemoteJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
}
