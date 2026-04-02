package com.ordis.app.data.repo

import com.ordis.app.data.model.ConsoleLog
import com.ordis.app.data.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

class ConsoleRepository {

    private val idGen = AtomicLong(1L)

    private val _logs = MutableStateFlow<List<ConsoleLog>>(emptyList())
    val logs: StateFlow<List<ConsoleLog>> = _logs

    fun info(tag: String, message: String) = add(LogLevel.INFO, tag, message)
    fun warn(tag: String, message: String) = add(LogLevel.WARN, tag, message)
    fun error(tag: String, message: String) = add(LogLevel.ERROR, tag, message)

    private fun add(level: LogLevel, tag: String, message: String) {
        val log = ConsoleLog(
            id = idGen.getAndIncrement(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )
        _logs.update { old -> (old + log).takeLast(500) }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
