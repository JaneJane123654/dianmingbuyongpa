package com.classroomassistant.shared.platform.capability

actual class PlatformCapabilities actual constructor() {
    actual fun availability(capability: AppCapability): CapabilityAvailability =
        when (capability) {
            AppCapability.ContinuousBackgroundMonitoring -> {
                CapabilityAvailability(
                    supported = false,
                    note = "Do not assume Android-style always-on background microphone monitoring on iOS."
                )
            }
            AppCapability.LocalWakeWord -> CapabilityAvailability(
                supported = true,
                note = "Treat as hardware/model dependent and verify on target devices."
            )
            AppCapability.LocalStreamingAsr -> CapabilityAvailability(
                supported = true,
                note = "Prefer foreground-only support first, then validate latency and battery."
            )
            AppCapability.SecureCredentialStorage -> CapabilityAvailability(supported = true)
        }
}
