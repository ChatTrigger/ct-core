package com.chattriggers.ctjs.minecraft.libs

import com.chattriggers.ctjs.events.*
import com.chattriggers.ctjs.utils.kotlin.External
import com.chattriggers.ctjs.utils.kotlin.ITextComponent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

//TODO: figure out what is not needed anymore after the kotlin conversion and remove
@External
object EventLib {
    @JvmStatic
    fun getButtonState(event: MouseEvent): Boolean {
        //#if MC<=10809
        return event.buttonstate
        //#else
        //$$ return event.isButtonstate
        //#endif
    }

    @JvmStatic
    fun getType(event: ChatReceivedEvent): Int {
        //#if MC<=10809
        return event.type.toInt()
        //#else
        //$$ return event.type.id.toInt()
        //#endif
    }

    @JvmStatic
    fun getMessage(event: ChatReceivedEvent): ITextComponent {
        return event.message
    }

    @JvmStatic
    fun getName(event: SoundEvent): String {
        return event.name
    }

    /**
     * Cancel an event. Automatically used with `cancel(event)`.
     *
     * @param event the event to cancel
     * @throws IllegalArgumentException if event can be cancelled "normally"
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun cancel(event: Any) {
        when (event) {
            is CallbackInfoReturnable<*> -> {
                if (!event.isCancellable) return
                event.setReturnValue(null)
            }
            is CallbackInfo -> {
                if (!event.isCancellable) return
                event.cancel()
            }
            is CancellableEvent -> event.cancel()
            else -> throw IllegalArgumentException()
        }
    }
}