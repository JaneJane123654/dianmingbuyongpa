package com.classroomassistant.shared.data.remote.core

import com.classroomassistant.shared.core.error.AppError
import com.classroomassistant.shared.core.error.AppErrorCategory
import com.classroomassistant.shared.core.error.SimpleAppErrorCode
import com.classroomassistant.shared.core.error.toAppError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

object RemoteErrorCodes {
    val MissingBaseUrl = SimpleAppErrorCode(
        key = "remote.missing_base_url",
        defaultMessage = "Base URL must not be blank",
    )
    val MissingCredentials = SimpleAppErrorCode(
        key = "remote.missing_credentials",
        defaultMessage = "Remote credentials are missing",
    )
    val UnknownProvider = SimpleAppErrorCode(
        key = "remote.unknown_provider",
        defaultMessage = "Remote provider is not recognized",
    )
    val Unauthorized = SimpleAppErrorCode(
        key = "remote.unauthorized",
        numericCode = 401,
        defaultMessage = "Authentication failed",
    )
    val Forbidden = SimpleAppErrorCode(
        key = "remote.forbidden",
        numericCode = 403,
        defaultMessage = "Remote access was denied",
    )
    val RateLimited = SimpleAppErrorCode(
        key = "remote.rate_limited",
        numericCode = 429,
        defaultMessage = "Remote request was rate limited",
    )
    val HttpFailure = SimpleAppErrorCode(
        key = "remote.http_failure",
        defaultMessage = "Remote request failed",
    )
    val Timeout = SimpleAppErrorCode(
        key = "remote.timeout",
        defaultMessage = "Remote request timed out",
    )
    val TransportFailure = SimpleAppErrorCode(
        key = "remote.transport_failure",
        defaultMessage = "Remote transport failed",
    )
    val ParseFailure = SimpleAppErrorCode(
        key = "remote.parse_failure",
        defaultMessage = "Remote response could not be parsed",
    )
    val UnknownFailure = SimpleAppErrorCode(
        key = "remote.unknown_failure",
        defaultMessage = "Remote request failed unexpectedly",
    )
}

sealed class RemoteException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause) {
    class MissingBaseUrl(
        serviceName: String? = null,
    ) : RemoteException(
        if (serviceName.isNullOrBlank()) {
            "Base URL must not be blank"
        } else {
            "$serviceName base URL must not be blank"
        },
    )

    class MissingCredentials(
        credentialLabel: String = "Remote credentials",
    ) : RemoteException("$credentialLabel must not be blank")

    class UnknownProvider(
        providerName: String,
    ) : RemoteException("Unknown remote provider: $providerName")

    class RateLimited(
        val retryAfterSeconds: Long? = null,
        message: String = if (retryAfterSeconds == null) {
            "Remote request was rate limited"
        } else {
            "Remote request was rate limited, retry after $retryAfterSeconds seconds"
        },
    ) : RemoteException(message)

    class UnexpectedStatus(
        val statusCode: Int,
        message: String = "Remote request failed with HTTP $statusCode",
    ) : RemoteException(message)

    class ParseFailure(
        message: String,
        cause: Throwable? = null,
    ) : RemoteException(message, cause)
}

fun HttpStatusCode.toRemoteException(message: String? = null): RemoteException = when (value) {
    HttpStatusCode.TooManyRequests.value -> RemoteException.RateLimited(message = message ?: "Remote request was rate limited")
    else -> RemoteException.UnexpectedStatus(
        statusCode = value,
        message = message ?: "Remote request failed with HTTP $value",
    )
}

fun Throwable.toRemoteAppError(): AppError = when (this) {
    is CancellationException -> toAppError(
        category = AppErrorCategory.Network,
        code = RemoteErrorCodes.UnknownFailure,
    )

    is RemoteException.MissingBaseUrl -> AppError.Standard(
        category = AppErrorCategory.Configuration,
        message = message ?: RemoteErrorCodes.MissingBaseUrl.defaultMessage.orEmpty(),
        code = RemoteErrorCodes.MissingBaseUrl,
        userMessage = "Please configure a Base URL before continuing.",
    )

    is RemoteException.MissingCredentials -> AppError.Standard(
        category = AppErrorCategory.Configuration,
        message = message ?: RemoteErrorCodes.MissingCredentials.defaultMessage.orEmpty(),
        code = RemoteErrorCodes.MissingCredentials,
        userMessage = "Please provide the required API token or key.",
    )

    is RemoteException.UnknownProvider -> AppError.Standard(
        category = AppErrorCategory.Configuration,
        message = message ?: RemoteErrorCodes.UnknownProvider.defaultMessage.orEmpty(),
        code = RemoteErrorCodes.UnknownProvider,
        userMessage = "The selected provider is not supported yet.",
    )

    is RemoteException.RateLimited -> AppError.Standard(
        category = AppErrorCategory.Network,
        message = message ?: RemoteErrorCodes.RateLimited.defaultMessage.orEmpty(),
        code = RemoteErrorCodes.RateLimited,
        userMessage = "The remote service asked us to slow down. Please retry shortly.",
        retryable = true,
        cause = this,
    )

    is RemoteException.UnexpectedStatus -> when (statusCode) {
        HttpStatusCode.Unauthorized.value -> AppError.Standard(
            category = AppErrorCategory.Configuration,
            message = message ?: "Remote request failed with HTTP 401",
            code = RemoteErrorCodes.Unauthorized,
            userMessage = "Authentication failed. Please check the configured token or key.",
            cause = this,
        )

        HttpStatusCode.Forbidden.value -> AppError.Standard(
            category = AppErrorCategory.Configuration,
            message = message ?: "Remote request failed with HTTP 403",
            code = RemoteErrorCodes.Forbidden,
            userMessage = "The remote service rejected this request.",
            cause = this,
        )

        HttpStatusCode.TooManyRequests.value -> AppError.Standard(
            category = AppErrorCategory.Network,
            message = message ?: "Remote request failed with HTTP 429",
            code = RemoteErrorCodes.RateLimited,
            userMessage = "The remote service asked us to slow down. Please retry shortly.",
            retryable = true,
            cause = this,
        )

        in 500..599 -> AppError.Standard(
            category = AppErrorCategory.Network,
            message = message ?: "Remote server failed with HTTP $statusCode",
            code = RemoteErrorCodes.HttpFailure,
            userMessage = "The remote service is temporarily unavailable.",
            retryable = true,
            cause = this,
        )

        else -> AppError.Standard(
            category = AppErrorCategory.Network,
            message = message ?: RemoteErrorCodes.HttpFailure.defaultMessage.orEmpty(),
            code = RemoteErrorCodes.HttpFailure,
            cause = this,
        )
    }

    is RemoteException.ParseFailure,
    is SerializationException,
    -> AppError.Standard(
        category = AppErrorCategory.Network,
        message = message ?: RemoteErrorCodes.ParseFailure.defaultMessage.orEmpty(),
        code = RemoteErrorCodes.ParseFailure,
        cause = this,
    )

    is HttpRequestTimeoutException -> AppError.Standard(
        category = AppErrorCategory.Network,
        message = message ?: RemoteErrorCodes.Timeout.defaultMessage.orEmpty(),
        code = RemoteErrorCodes.Timeout,
        userMessage = "The remote request timed out.",
        retryable = true,
        cause = this,
    )

    is IOException -> AppError.Standard(
        category = AppErrorCategory.Network,
        message = message ?: RemoteErrorCodes.TransportFailure.defaultMessage.orEmpty(),
        code = RemoteErrorCodes.TransportFailure,
        userMessage = "The network request could not be completed.",
        retryable = true,
        cause = this,
    )

    else -> toAppError(
        category = AppErrorCategory.Network,
        code = RemoteErrorCodes.UnknownFailure,
        retryable = false,
    )
}
