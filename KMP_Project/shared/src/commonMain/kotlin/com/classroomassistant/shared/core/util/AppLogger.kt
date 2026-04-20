package com.classroomassistant.shared.core.util

enum class AppLogLevel {
    Debug,
    Info,
    Warn,
    Error,
}

data class AppLogEvent(
    val level: AppLogLevel,
    val tag: String,
    val message: String,
    val eventCode: String? = null,
    val throwable: Throwable? = null,
)

interface AppLogger {
    fun log(event: AppLogEvent)
}

object NoOpAppLogger : AppLogger {
    override fun log(event: AppLogEvent) = Unit
}

fun AppLogger.tagged(defaultTag: String): AppLogger = TaggedAppLogger(
    delegate = this,
    defaultTag = defaultTag,
)

fun AppLogger.debug(
    tag: String,
    message: String,
    eventCode: String? = null,
    throwable: Throwable? = null,
) {
    log(
        AppLogEvent(
            level = AppLogLevel.Debug,
            tag = tag,
            message = message,
            eventCode = eventCode,
            throwable = throwable,
        ),
    )
}

fun AppLogger.info(
    tag: String,
    message: String,
    eventCode: String? = null,
    throwable: Throwable? = null,
) {
    log(
        AppLogEvent(
            level = AppLogLevel.Info,
            tag = tag,
            message = message,
            eventCode = eventCode,
            throwable = throwable,
        ),
    )
}

fun AppLogger.warn(
    tag: String,
    message: String,
    eventCode: String? = null,
    throwable: Throwable? = null,
) {
    log(
        AppLogEvent(
            level = AppLogLevel.Warn,
            tag = tag,
            message = message,
            eventCode = eventCode,
            throwable = throwable,
        ),
    )
}

fun AppLogger.error(
    tag: String,
    message: String,
    eventCode: String? = null,
    throwable: Throwable? = null,
) {
    log(
        AppLogEvent(
            level = AppLogLevel.Error,
            tag = tag,
            message = message,
            eventCode = eventCode,
            throwable = throwable,
        ),
    )
}

private class TaggedAppLogger(
    private val delegate: AppLogger,
    private val defaultTag: String,
) : AppLogger {
    override fun log(event: AppLogEvent) {
        val resolvedTag = if (event.tag.isBlank()) defaultTag else event.tag
        delegate.log(event.copy(tag = resolvedTag))
    }
}
