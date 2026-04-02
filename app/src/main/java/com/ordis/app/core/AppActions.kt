package com.ordis.app.core

/**
 * Глобальные действия UI -> Activity
 * Заполняются в MainActivity, вызываются из Compose-экранов.
 */
object AppActions {

    /** Старт/стоп голосового прослушивания */
    var onToggleListening: (() -> Unit)? = null

    /** Отправка текстового сообщения из ChatScreen */
    var onChatSend: ((String) -> Unit)? = null

    /** Экспорт памяти ассистента (история, факты, темы, профиль) */
    var onExportMemory: (() -> Unit)? = null

    /** Очистка памяти ассистента */
    var onClearMemory: (() -> Unit)? = null

    /** Универсальная команда (если понадобится из UI) */
    var onRunCommand: ((String) -> Unit)? = null
}
