package com.ordis.app.data.model

data class AppVersion(
    val id: String,
    val name: String,
    val createdAt: Long,
    val description: String,
    val isCurrent: Boolean = false
)
