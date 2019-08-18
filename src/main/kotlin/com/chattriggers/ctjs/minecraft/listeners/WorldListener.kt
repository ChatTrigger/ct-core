package com.chattriggers.ctjs.minecraft.listeners

import com.chattriggers.ctjs.CTJS
import com.chattriggers.ctjs.events.*
import com.chattriggers.ctjs.minecraft.wrappers.World
import com.chattriggers.ctjs.minecraft.wrappers.objects.PlayerMP
import com.chattriggers.ctjs.triggers.TriggerType
import javax.vecmath.Vector3f

object WorldListener {
    private var shouldTriggerWorldLoad: Boolean = false
    private var playerList: MutableList<String> = mutableListOf()

    @Subscriber
    fun onWorldLoad(event: WorldLoadEvent) {
        this.playerList.clear()
        this.shouldTriggerWorldLoad = true
    }

    @Subscriber
    fun onRenderGameOverlay(event: RenderGameOverlayEvent) {
        // world loadExtra trigger
        if (!shouldTriggerWorldLoad) return

        TriggerType.WORLD_LOAD.triggerAll()
        shouldTriggerWorldLoad = false

        CTJS.sounds
                .stream()
                .filter { it.isListening }
                .forEach { it.onWorldLoad() }

        CTJS.sounds.clear()
    }

    @Subscriber
    fun onWorldUnload(event: WorldUnloadEvent) {
        TriggerType.WORLD_UNLOAD.triggerAll()
    }

    @Subscriber
    fun onSoundPlay(event: SoundEvent) {
        val position = Vector3f(
                event.sound.xPosF,
                event.sound.yPosF,
                event.sound.zPosF
        )

        val vol = try { event.sound.volume } catch (ignored: Exception) { 0 }
        val pitch = try { event.sound.volume } catch (ignored: Exception) { 1 }

        TriggerType.SOUND_PLAY.triggerAll(
                position,
                event.name,
                vol,
                pitch,
                //#if MC<=10809
                event.category ?: event.category?.categoryName,
                //#else
                //$$ event.sound.category ?: event.sound.category.name,
                //#endif
                event
        )
    }

//    @Subscriber
//    fun noteBlockEventPlay(event: NoteBlockEvent.Play) {
//        val position = Vector3d(
//                event.pos.x.toDouble(),
//                event.pos.y.toDouble(),
//                event.pos.z.toDouble()
//        )
//
//        TriggerType.NOTE_BLOCK_PLAY.triggerAll(
//                position,
//                event.note.name,
//                event.octave,
//                event
//        )
//    }

    @Subscriber
    fun updatePlayerList(event: TickEvent) {
        World.getAllPlayers().filter {
            !playerList.contains(it.getName())
        }.forEach {
            playerList.add(it.getName())
            TriggerType.PLAYER_JOIN.triggerAll(it)
            return@forEach
        }

        val ite = playerList.listIterator()

        while (ite.hasNext()) {
            val it = ite.next()

            try {
                World.getPlayerByName(it)
            } catch (exception: Exception) {
                this.playerList.remove(it)
                TriggerType.PLAYER_LEAVE.triggerAll(it)
                break
            }
        }
    }

    @Subscriber
    fun blockBreak(event: BlockBreakEvent) {
        TriggerType.BLOCK_BREAK.triggerAll(
            World.getBlockAt(event.pos.x, event.pos.y, event.pos.z),
            PlayerMP(event.player),
            event
        )
    }
}