package com.classroomassistant.shared.platform.capability

enum class AppCapability {
    ContinuousBackgroundMonitoring,
    LocalWakeWord,
    LocalStreamingAsr,
    SecureCredentialStorage,
}

data class CapabilityAvailability(
    val supported: Boolean,
    val note: String? = null,
)

expect class PlatformCapabilities() {
    fun availability(capability: AppCapability): CapabilityAvailability
}
