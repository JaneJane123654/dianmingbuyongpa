package com.classroomassistant.shared.core.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AppDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

object DefaultAppDispatchers : AppDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}

suspend inline fun <T> AppDispatchers.withIo(
    crossinline block: suspend () -> T,
): T = withContext(io) { block() }

suspend inline fun <T> AppDispatchers.withDefault(
    crossinline block: suspend () -> T,
): T = withContext(default) { block() }

suspend inline fun <T> AppDispatchers.withMain(
    crossinline block: suspend () -> T,
): T = withContext(main) { block() }
