package com.chattriggers.ctjs

import com.chattriggers.ctjs.commands.CTCommand
import com.chattriggers.ctjs.commands.ClientCommandHandler
import com.chattriggers.ctjs.engine.ModuleManager
import com.chattriggers.ctjs.engine.langs.js.JSLoader
import com.chattriggers.ctjs.events.EventBus
import com.chattriggers.ctjs.minecraft.libs.FileLib
import com.chattriggers.ctjs.minecraft.listeners.ChatListener
import com.chattriggers.ctjs.minecraft.listeners.ClientListener
import com.chattriggers.ctjs.minecraft.listeners.WorldListener
import com.chattriggers.ctjs.minecraft.objects.Sound
import com.chattriggers.ctjs.minecraft.objects.gui.GuiHandler
import com.chattriggers.ctjs.minecraft.wrappers.CPS
import com.chattriggers.ctjs.minecraft.wrappers.Player
import com.chattriggers.ctjs.triggers.TriggerType
import com.chattriggers.ctjs.utils.UpdateChecker
import com.chattriggers.ctjs.utils.config.Config
import com.google.gson.JsonParser
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.FileReader
import java.net.UnknownHostException
import kotlin.concurrent.thread

object CTJS {
    lateinit var assetsDir: File
    private lateinit var configLocation: File
    val sounds = mutableListOf<Sound>()

    fun preInit() {
        this.configLocation = File("./config")
        val pictures = File(configLocation, "ChatTriggers/images/")
        pictures.mkdirs()
        assetsDir = pictures

        thread(start = true) {
            loadConfig()
        }

        listOf(ChatListener, WorldListener, CPS, GuiHandler, ClientListener, UpdateChecker).forEach {
            EventBus.register(it)
        }

        listOf(JSLoader).forEach {
            ModuleManager.loaders.add(it)
        }

        val sha256uuid = DigestUtils.sha256Hex(Player.getUUID())

        try {
            FileLib.getUrlContent("https://www.chattriggers.com/tracker/?uuid=$sha256uuid")
        } catch (e: UnknownHostException) {
            e.print()
        }
    }

    fun init() {
        Reference.conditionalThread {
            ModuleManager.load(true)
        }

        registerHooks()
    }

    fun saveConfig() = Config.save(File(this.configLocation, "ChatTriggers.json"))

    fun loadConfig(): Boolean {
        try {
            val parser = JsonParser()
            val obj = parser.parse(
                    FileReader(
                            File(this.configLocation, "ChatTriggers.json")
                    )
            ).asJsonObject

            Config.load(obj)

            return true
        } catch (exception: Exception) {
            val place = File(this.configLocation, "ChatTriggers.json")
            place.delete()
            place.createNewFile()
            saveConfig()
        }

        return false
    }

    private fun registerHooks() {
        ClientCommandHandler.registerCommand(CTCommand)

        Runtime.getRuntime().addShutdownHook(
                Thread { TriggerType.GAME_UNLOAD::triggerAll }
        )
    }
}