package com.ordis.app.data.model

enum class LogLevel { INFO, WARN, ERROR }

data class ConsoleLog(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
)
