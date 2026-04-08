package com.classroomassistant.shared.platform.capability

actual class PlatformCapabilities actual constructor() {
    actual fun availability(capability: AppCapability): CapabilityAvailability =
        when (capability) {
            AppCapability.ContinuousBackgroundMonitoring -> {
                CapabilityAvailability(
                    supported = true,
                    note = "Android can support long-running monitoring with a foreground microphone service."
                )
            }
            AppCapability.LocalWakeWord -> CapabilityAvailability(supported = true)
            AppCapability.LocalStreamingAsr -> CapabilityAvailability(supported = true)
            AppCapability.SecureCredentialStorage -> CapabilityAvailability(supported = true)
        }
}
