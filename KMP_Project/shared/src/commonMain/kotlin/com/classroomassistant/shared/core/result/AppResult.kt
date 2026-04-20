package com.classroomassistant.shared.core.result

import com.classroomassistant.shared.core.error.AppError
import com.classroomassistant.shared.core.error.toAppError
import kotlinx.coroutines.CancellationException

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>

    data class Cancelled(
        val error: AppError.Cancelled = AppError.Cancelled(),
    ) : AppResult<Nothing>

    val isSuccess: Boolean
        get() = this is Success<*>

    val isFailure: Boolean
        get() = this is Failure

    val isCancelled: Boolean
        get() = this is Cancelled

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
        is Cancelled -> null
    }

    fun errorOrNull(): AppError? = when (this) {
        is Success -> null
        is Failure -> error
        is Cancelled -> error
    }

    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
        is Cancelled -> this
    }

    inline fun <R> flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
        is Cancelled -> this
    }

    inline fun onSuccess(action: (T) -> Unit): AppResult<T> = apply {
        if (this is Success) {
            action(value)
        }
    }

    inline fun onFailure(action: (AppError) -> Unit): AppResult<T> = apply {
        if (this is Failure) {
            action(error)
        }
    }

    inline fun onCancelled(action: (AppError.Cancelled) -> Unit): AppResult<T> = apply {
        if (this is Cancelled) {
            action(error)
        }
    }

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (AppError) -> R,
        onCancelled: (AppError.Cancelled) -> R,
    ): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
        is Cancelled -> onCancelled(error)
    }

    companion object {
        fun <T> success(value: T): AppResult<T> = Success(value)

        fun failure(error: AppError): AppResult<Nothing> = Failure(error)

        fun cancelled(error: AppError.Cancelled = AppError.Cancelled()): AppResult<Nothing> = Cancelled(error)

        inline fun <T> catching(block: () -> T): AppResult<T> = try {
            Success(block())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            Failure(throwable.toAppError())
        }

        inline fun <T> catchingIncludingCancellation(block: () -> T): AppResult<T> = try {
            Success(block())
        } catch (cancelled: CancellationException) {
            Cancelled(cancelled.toAppError() as AppError.Cancelled)
        } catch (throwable: Throwable) {
            Failure(throwable.toAppError())
        }
    }
}
