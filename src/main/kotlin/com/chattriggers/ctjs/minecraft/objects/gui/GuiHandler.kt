package com.chattriggers.ctjs.minecraft.objects.gui

import com.chattriggers.ctjs.events.Subscriber
import com.chattriggers.ctjs.events.TickEvent
import com.chattriggers.ctjs.utils.kotlin.External
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen

@External
object GuiHandler {
    private val GUIs: MutableMap<GuiScreen, Int> = mutableMapOf()

    fun openGui(gui: GuiScreen) {
        this.GUIs[gui] = 1
    }

    fun clearGuis() {
        this.GUIs.clear()
    }

    @Subscriber
    fun onTick(event: TickEvent) {
        this.GUIs.forEach {
            if (it.value == 0) {
                Minecraft.getMinecraft().displayGuiScreen(it.key)
                this.GUIs[it.key] = -1
            } else {
                this.GUIs[it.key] = 0
            }
        }

        this.GUIs.entries.removeIf {
            it.value == -1
        }
    }
}