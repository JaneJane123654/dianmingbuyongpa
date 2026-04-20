package com.classroomassistant.shared.core.error

import kotlinx.coroutines.CancellationException

enum class AppErrorCategory {
    Unknown,
    Validation,
    Configuration,
    Permission,
    Capability,
    Network,
    Storage,
    Audio,
    Speech,
    AiService,
    Cancelled,
}

sealed interface AppError {
    val category: AppErrorCategory
    val code: AppErrorCode?
    val message: String
    val userMessage: String?
    val cause: Throwable?
    val retryable: Boolean

    data class Standard(
        override val category: AppErrorCategory = AppErrorCategory.Unknown,
        override val message: String,
        override val code: AppErrorCode? = null,
        override val userMessage: String? = null,
        override val cause: Throwable? = null,
        override val retryable: Boolean = false,
    ) : AppError

    data class UnsupportedFeature(
        val feature: String,
        override val message: String = "$feature is not supported on this platform",
        override val code: AppErrorCode? = null,
        override val userMessage: String? = null,
        override val cause: Throwable? = null,
    ) : AppError {
        override val category: AppErrorCategory = AppErrorCategory.Capability
        override val retryable: Boolean = false
    }

    data class Cancelled(
        override val message: String = "Operation was cancelled",
        override val code: AppErrorCode? = null,
        override val userMessage: String? = null,
        override val cause: Throwable? = null,
    ) : AppError {
        override val category: AppErrorCategory = AppErrorCategory.Cancelled
        override val retryable: Boolean = false
    }
}

fun Throwable.toAppError(
    category: AppErrorCategory = AppErrorCategory.Unknown,
    code: AppErrorCode? = null,
    userMessage: String? = null,
    retryable: Boolean = false,
): AppError = when (this) {
    is CancellationException -> AppError.Cancelled(
        message = message ?: "Operation was cancelled",
        code = code,
        userMessage = userMessage,
        cause = this,
    )

    else -> AppError.Standard(
        category = category,
        message = message ?: "Unexpected error",
        code = code,
        userMessage = userMessage,
        cause = this,
        retryable = retryable,
    )
}
