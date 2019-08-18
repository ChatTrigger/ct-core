package com.chattriggers.ctjs.minecraft.objects.display

import com.chattriggers.ctjs.events.EventBus
import com.chattriggers.ctjs.events.RenderGameOverlayEvent
import com.chattriggers.ctjs.events.Subscriber
import com.chattriggers.ctjs.utils.kotlin.External
import org.lwjgl.opengl.GL11

@External
object DisplayHandler {
    private var displays = mutableListOf<Display>()

    init {
        EventBus.register(this)
    }

    fun registerDisplay(display: Display) = this.displays.add(display)
    fun clearDisplays() = this.displays.clear()

    @Subscriber
    fun renderDisplays(event: RenderGameOverlayEvent) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return

        GL11.glPushMatrix()
        this.displays.forEach(Display::render)
        GL11.glPopMatrix()
    }

    enum class Background {
        NONE, FULL, PER_LINE;
    }

    enum class Align {
        NONE, LEFT, CENTER, RIGHT;
    }

    enum class Order {
        UP, DOWN;
    }
}