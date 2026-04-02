package com.ordis.app.core

object AppActions {
    var onToggleListening: (() -> Unit)? = null
    var onExportMemory: (() -> Unit)? = null
    var onClearMemory: (() -> Unit)? = null
}
