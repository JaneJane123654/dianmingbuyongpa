package com.classroomassistant.shared.platform.audio

actual class PlatformAudioSessionController actual constructor() : AudioSessionController {
    override suspend fun prepareForMonitoring() {
        TODO("Prepare audio focus, recorder pipeline, and foreground service hooks when needed.")
    }

    override suspend fun activateRecordingSession() {
        TODO("Activate Android recording session and audio focus.")
    }

    override suspend fun deactivateRecordingSession() {
        TODO("Release Android audio focus and recorder session.")
    }
}
