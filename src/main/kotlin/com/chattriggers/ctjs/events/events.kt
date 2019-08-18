package com.chattriggers.ctjs.events

import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.SoundCategory
import net.minecraft.client.audio.SoundManager
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.IChatComponent
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.World
import org.lwjgl.input.Mouse
import java.util.ArrayList

class ChatReceivedEvent(val type: Byte, val message: IChatComponent) : CancellableEvent()

class MouseEvent : Event() {
    val x: Int = Mouse.getEventX()
    val y: Int = Mouse.getEventY()
    val dx: Int = Mouse.getEventDX()
    val dy: Int = Mouse.getEventDY()
    val dwheel: Int = Mouse.getEventDWheel()
    val button: Int = Mouse.getEventButton()
    val buttonstate: Boolean = Mouse.getEventButtonState()
    val nanoseconds: Long = Mouse.getEventNanoseconds()
}

class SoundEvent(manager: SoundManager, val sound: ISound, val category: SoundCategory) : CancellableEvent() {
    val name: String = sound.soundLocation.resourcePath
}

open class RenderGameOverlayEvent(val type: ElementType) : CancellableEvent() {
    enum class ElementType {
        ALL,
        HELMET,
        PORTAL,
        CROSSHAIRS,
        BOSSHEALTH,
        ARMOR,
        HEALTH,
        FOOD,
        AIR,
        HOTBAR,
        EXPERIENCE,
        TEXT,
        HEALTHMOUNT,
        JUMPBAR,
        CHAT,
        PLAYER_LIST,
        DEBUG
    }
}

class TickEvent : Event()

class FinishRenderWorldEvent(val partialTicks: Float) : Event()

class GuiOpenEvent(val gui: GuiScreen) : CancellableEvent()

class BlockHighlightEvent(val target: MovingObjectPosition) : CancellableEvent()

class ItemPickupEvent(val player: EntityPlayer, val item: EntityItem) : Event()

class ItemDropEvent(val player: EntityPlayer, val item: ItemStack) : CancellableEvent()

/**
 * Will be tricky to make cancellable. Probably requires throwing an exception to bubble out of the draw call.
 */
class GuiDrawEvent(val mouseX: Int, val mouseY: Int, val gui: GuiScreen) : CancellableEvent()

class PlayerInteractEvent(val action: Action?, val pos: BlockPos?) : CancellableEvent() {
    enum class Action {
        RIGHT_CLICK_AIR,
        RIGHT_CLICK_BLOCK,
        LEFT_CLICK_BLOCK
    }
}

class WorldLoadEvent : Event()

class WorldUnloadEvent : Event()

//class NoteBlockPlayEvent(val pos: BlockPos, val note: Note) : CancellableEvent()

class BlockBreakEvent(val player: EntityPlayer, val pos: BlockPos) : Event()