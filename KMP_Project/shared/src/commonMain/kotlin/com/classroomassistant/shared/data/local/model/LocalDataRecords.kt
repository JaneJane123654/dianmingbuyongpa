package com.classroomassistant.shared.data.local.model

enum class LocalSettingValueType {
    STRING,
    BOOLEAN,
    INT,
    LONG,
}

enum class LocalModelKind {
    KWS,
    ASR,
    VAD,
}

enum class LocalModelInstallationStatus {
    PENDING,
    DOWNLOADING,
    EXTRACTING,
    INSTALLED,
    FAILED,
    CORRUPTED,
    REMOVED,
}

enum class LocalModelInstallationSource {
    DEFAULT_CATALOG,
    CUSTOM_URL,
    LEGACY_MIGRATION,
    MANUAL_IMPORT,
}

enum class LocalAnswerHistoryStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class LocalSettingEntryRecord(
    val namespace: String,
    val key: String,
    val valueType: LocalSettingValueType,
    val stringValue: String?,
    val boolValue: Boolean?,
    val intValue: Long?,
    val longValue: Long?,
    val updatedAtEpochMs: Long,
)

data class LocalMonitoringEventRecord(
    val id: String,
    val sessionId: String?,
    val kind: String,
    val eventCode: String?,
    val payloadJson: String,
    val createdAtEpochMs: Long,
)

data class LocalRecordingEntryRecord(
    val id: String,
    val namePrefix: String,
    val relativePath: String,
    val recordingDayKey: String,
    val transcriptText: String?,
    val byteSize: Long?,
    val durationMs: Long?,
    val sampleRateHz: Long,
    val channelCount: Long,
    val bitsPerSample: Long,
    val frameMillis: Long,
    val createdAtEpochMs: Long,
    val retentionExpiresAtEpochMs: Long?,
    val deletedAtEpochMs: Long?,
)

data class LocalModelInstallationRecord(
    val modelKind: LocalModelKind,
    val modelId: String,
    val displayName: String?,
    val installRelativePath: String,
    val archiveRelativePath: String?,
    val sourceUrl: String?,
    val sourceKind: LocalModelInstallationSource,
    val status: LocalModelInstallationStatus,
    val isSelected: Boolean,
    val lastErrorMessage: String?,
    val installedAtEpochMs: Long?,
    val verifiedAtEpochMs: Long?,
    val updatedAtEpochMs: Long,
)

data class LocalAnswerHistoryRecord(
    val id: String,
    val sessionId: String?,
    val recordingEntryId: String?,
    val lectureText: String,
    val promptText: String,
    val answerText: String?,
    val provider: String?,
    val modelName: String?,
    val status: LocalAnswerHistoryStatus,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long?,
)
