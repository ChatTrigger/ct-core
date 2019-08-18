package com.chattriggers.ctjs

import com.chattriggers.ctjs.commands.ClientCommandHandler
import com.chattriggers.ctjs.commands.CommandHandler
import com.chattriggers.ctjs.engine.ModuleManager
import com.chattriggers.ctjs.minecraft.libs.ChatLib
import com.chattriggers.ctjs.minecraft.objects.display.DisplayHandler
import com.chattriggers.ctjs.minecraft.objects.gui.GuiHandler
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.config.Config
import net.minecraft.launchwrapper.Launch
import kotlin.concurrent.thread

object Reference {
    const val MODID = "ct.js"
    const val MODNAME = "ChatTriggers"
    const val MODVERSION = "@MOD_VERSION@"

    private var isLoaded = true

    fun reloadCT() = loadCT(true)

    fun unloadCT(asCommand: Boolean = true) {
        TriggerType.WORLD_UNLOAD.triggerAll()
        TriggerType.GAME_UNLOAD.triggerAll()

        DisplayHandler.clearDisplays()
        GuiHandler.clearGuis()
        CommandHandler.getCommandList().clear()
        ModuleManager.unload()

        if (Config.clearConsoleOnLoad)
            ModuleManager.loaders.forEach { it.console.clearConsole() }

        if (asCommand) {
            ChatLib.chat("&7Unloaded all of ChatTriggers")
            this.isLoaded = true
        }
    }

    @JvmOverloads
    fun loadCT(updateCheck: Boolean = false) {
        if (!this.isLoaded) return
        this.isLoaded = false

        unloadCT(false)

        ChatLib.chat("&cReloading ct.js scripts...")
        conditionalThread {
            ClientCommandHandler.removeCTCommands()

            CTJS.loadConfig()

            ModuleManager.load(updateCheck)

            ChatLib.chat("&aDone reloading scripts!")

            TriggerType.GAME_LOAD.triggerAll()
            if (World.isLoaded())
                TriggerType.WORLD_LOAD.triggerAll()

            this.isLoaded = true
        }
    }

    fun conditionalThread(block: () -> Unit) {
        if (Config.threadedLoading) {
            thread { block() }
        } else {
            block()
        }
    }
}

fun Exception.print() {
    try {
        ModuleManager.generalConsole.printStackTrace(this)
    } catch (exception: Exception) {
        this.printStackTrace()
    }
}

fun String.print() {
    try {
        ModuleManager.generalConsole.out.println(this)
    } catch (exception: Exception) {
        System.out.println(this)
    }
}