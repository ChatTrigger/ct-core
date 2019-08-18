package com.chattriggers.ctjs.events

abstract class Event

abstract class CancellableEvent : Event() {
    private var cancelled = false

    fun cancel() = setCancelled(true)

    fun setCancelled(toCancel: Boolean) {
        cancelled = toCancel
    }

    fun isCancelled() = cancelled
}