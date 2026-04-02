package com.ordis.app.data.repo

import com.ordis.app.data.model.AppVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class VersionRepository {

    private val initial = listOf(
        AppVersion(
            id = "v0",
            name = "v0.1.0",
            createdAt = System.currentTimeMillis(),
            description = "Стартовая версия Ордис",
            isCurrent = true
        )
    )

    private val _versions = MutableStateFlow(initial)
    val versions: StateFlow<List<AppVersion>> = _versions

    fun addVersion(name: String, description: String) {
        _versions.update { old ->
            val reset = old.map { it.copy(isCurrent = false) }
            reset + AppVersion(
                id = UUID.randomUUID().toString(),
                name = name,
                createdAt = System.currentTimeMillis(),
                description = description,
                isCurrent = true
            )
        }
    }

    fun rollbackTo(versionId: String): Boolean {
        var changed = false
        _versions.update { old ->
            if (old.none { it.id == versionId }) return@update old
            changed = true
            old.map { it.copy(isCurrent = it.id == versionId) }
        }
        return changed
    }

    fun switchToLatest(): Boolean {
        val list = _versions.value
        if (list.isEmpty()) return false
        val latest = list.maxByOrNull { it.createdAt } ?: return false
        return rollbackTo(latest.id)
    }

    fun getCurrentVersion(): AppVersion? = _versions.value.firstOrNull { it.isCurrent }
}
