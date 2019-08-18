package com.chattriggers.ctjs.minecraft.listeners

import com.chattriggers.ctjs.events.ChatReceivedEvent
import com.chattriggers.ctjs.events.Subscriber
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.libs.EventLib
import com.chattriggers.ctjs.print
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.config.Config

object ChatListener {
    val chatHistory = mutableListOf<String>()
    val actionBarHistory = mutableListOf<String>()

    @Subscriber
    fun onReceiveChat(event: ChatReceivedEvent) {
        val type = EventLib.getType(event)

        when (type) {
            in 0..1 -> {
                // save to chatHistory
                chatHistory += ChatLib.getChatMessage(event, true)
                if (chatHistory.size > 1000) chatHistory.removeAt(0)

                // normal Chat Message
                TriggerType.CHAT.triggerAll(ChatLib.getChatMessage(event, false), event)

                // print to console
                if (Config.printChatToConsole) {
                    "[CHAT] ${ChatLib.replaceFormatting(ChatLib.getChatMessage(event, true))}".print()
                }
            }
            2 -> {
                // save to actionbar history
                actionBarHistory += ChatLib.getChatMessage(event, true)
                if (actionBarHistory.size > 1000) actionBarHistory.removeAt(0)

                // action bar
                TriggerType.ACTION_BAR.triggerAll(ChatLib.getChatMessage(event, false), event)
            }
        }
    }
}