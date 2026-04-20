package com.classroomassistant.shared.data.remote.core

import com.classroomassistant.shared.core.error.AppError
import com.classroomassistant.shared.core.error.toAppError
import com.classroomassistant.shared.core.result.AppResult
import com.classroomassistant.shared.core.util.AppLogger
import com.classroomassistant.shared.core.util.debug
import com.classroomassistant.shared.core.util.warn
import kotlinx.coroutines.CancellationException

class RemoteCallExecutor(
    private val logger: AppLogger,
) {
    suspend fun <T> execute(
        operationName: String,
        tag: String = "RemoteCall",
        block: suspend () -> T,
    ): AppResult<T> = try {
        AppResult.success(block())
    } catch (cancelled: CancellationException) {
        logger.debug(
            tag = tag,
            message = "$operationName was cancelled",
            eventCode = "REMOTE_CALL_CANCELLED",
            throwable = cancelled,
        )
        AppResult.cancelled(cancelled.toAppError() as AppError.Cancelled)
    } catch (throwable: Throwable) {
        val error = throwable.toRemoteAppError()
        logger.warn(
            tag = tag,
            message = "$operationName failed: ${error.message}",
            eventCode = "REMOTE_CALL_FAILED",
            throwable = throwable,
        )
        AppResult.failure(error)
    }
}
