package com.classroomassistant.shared.platform.audio

interface AudioSessionController {
    suspend fun prepareForMonitoring()
    suspend fun activateRecordingSession()
    suspend fun deactivateRecordingSession()
}

expect class PlatformAudioSessionController() : AudioSessionController
