package com.classroomassistant.shared.core.error

interface AppErrorCode {
    val key: String
    val numericCode: Int?
    val defaultMessage: String?
}

data class SimpleAppErrorCode(
    override val key: String,
    override val numericCode: Int? = null,
    override val defaultMessage: String? = null,
) : AppErrorCode
