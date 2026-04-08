package com.classroomassistant.shared.platform.audio

actual class PlatformAudioSessionController actual constructor() : AudioSessionController {
    override suspend fun prepareForMonitoring() {
        TODO("Configure AVAudioSession category, mode, route, and interruption observers.")
    }

    override suspend fun activateRecordingSession() {
        TODO("Activate AVAudioSession before microphone capture.")
    }

    override suspend fun deactivateRecordingSession() {
        TODO("Deactivate AVAudioSession when monitoring stops.")
    }
}
