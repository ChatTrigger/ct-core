package com.chattriggers.ctjs.minecraft.mixins;

import com.chattriggers.ctjs.events.EventBus;
import com.chattriggers.ctjs.events.RenderGameOverlayEvent;
import net.minecraft.client.gui.GuiOverlayDebug;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiOverlayDebug.class)
public class MixinGuiOverlayDebug {
    @Inject(
            method = "renderDebugInfo",
            at = @At("HEAD"),
            cancellable = true
    )
    private void draw(ScaledResolution scaledResolutionIn, CallbackInfo ci) {
        if (EventBus.INSTANCE.post(new RenderGameOverlayEvent(RenderGameOverlayEvent.ElementType.DEBUG))) {
            ci.cancel();
        }
    }
}
