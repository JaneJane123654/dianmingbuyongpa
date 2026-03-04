package com.classroomassistant.android.platform

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AppCrashMonitor {
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private const val STAGE_FILE_NAME = "listening_stage.txt"
    private const val STAGE_TRACE_FILE_NAME = "listening_stage_trace.txt"
    private const val CRASH_FILE_NAME = "last_crash_summary.txt"
    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) {
            return
        }
        synchronized(this) {
            if (installed) {
                return
            }
            val appContext = context.applicationContext
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                runCatching {
                    writeCrashSummary(appContext, thread.name, throwable)
                }
                previousHandler?.uncaughtException(thread, throwable)
            }
            installed = true
        }
    }

    fun markListeningStage(context: Context, stage: String) {
        val appContext = context.applicationContext
        val file = stageFile(appContext)
        file.parentFile?.mkdirs()
        val line = "${now()}|$stage"
        file.writeText(line)
        appendStageTrace(appContext, line)
    }

    fun clearListeningStage(context: Context) {
        val file = stageFile(context.applicationContext)
        if (file.exists()) {
            file.delete()
        }
    }

    fun recordHandledError(context: Context, title: String, throwable: Throwable) {
        val summary = buildString {
            append("时间: ").append(now()).append('\n')
            append("类型: ").append(title).append('\n')
            append("异常: ").append(throwable.javaClass.name).append('\n')
            append("信息: ").append(throwable.message ?: "无").append('\n')
            append("阶段: ").append(readStage(context.applicationContext) ?: "未知").append('\n')
            append("堆栈:\n")
            throwable.stackTrace.take(30).forEach { frame ->
                append("  at ").append(frame.toString()).append('\n')
            }
        }
        crashFile(context.applicationContext).apply {
            parentFile?.mkdirs()
            writeText(summary)
        }
    }

    fun consumePendingSummary(context: Context): String? {
        val appContext = context.applicationContext
        val crashFile = crashFile(appContext)
        if (crashFile.exists()) {
            val text = crashFile.readText().trim()
            crashFile.delete()
            return if (text.isNotBlank()) {
                "检测到上次异常退出，摘要如下：\n$text"
            } else {
                null
            }
        }
        val stage = readStage(appContext)
        if (!stage.isNullOrBlank()) {
            val trace = readStageTrace(appContext)
            clearListeningStage(appContext)
            clearStageTrace(appContext)
            return if (trace.isBlank()) {
                "检测到上次在监听阶段异常退出：$stage"
            } else {
                "检测到上次在监听阶段异常退出：$stage\n阶段链路:\n$trace"
            }
        }
        return null
    }

    private fun writeCrashSummary(context: Context, threadName: String, throwable: Throwable) {
        val summary = buildString {
            append("时间: ").append(now()).append('\n')
            append("线程: ").append(threadName).append('\n')
            append("异常: ").append(throwable.javaClass.name).append('\n')
            append("信息: ").append(throwable.message ?: "无").append('\n')
            append("阶段: ").append(readStage(context) ?: "未知").append('\n')
            append("堆栈:\n")
            throwable.stackTrace.take(40).forEach { frame ->
                append("  at ").append(frame.toString()).append('\n')
            }
        }
        crashFile(context).apply {
            parentFile?.mkdirs()
            writeText(summary)
        }
    }

    private fun readStage(context: Context): String? {
        val file = stageFile(context)
        if (!file.exists()) {
            return null
        }
        return file.readText().trim().ifBlank { null }
    }

    private fun stageFile(context: Context): File {
        return File(File(context.filesDir, "logs"), STAGE_FILE_NAME)
    }

    private fun crashFile(context: Context): File {
        return File(File(context.filesDir, "logs"), CRASH_FILE_NAME)
    }

    private fun stageTraceFile(context: Context): File {
        return File(File(context.filesDir, "logs"), STAGE_TRACE_FILE_NAME)
    }

    private fun appendStageTrace(context: Context, line: String) {
        val file = stageTraceFile(context)
        file.parentFile?.mkdirs()
        val newLines = mutableListOf<String>()
        if (file.exists()) {
            newLines.addAll(file.readLines().takeLast(80))
        }
        newLines.add(line)
        file.writeText(newLines.takeLast(80).joinToString("\n"))
    }

    private fun readStageTrace(context: Context): String {
        val file = stageTraceFile(context)
        if (!file.exists()) {
            return ""
        }
        return file.readLines().takeLast(30).joinToString("\n")
    }

    private fun clearStageTrace(context: Context) {
        val file = stageTraceFile(context)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun now(): String = LocalDateTime.now().format(timeFormatter)
}
