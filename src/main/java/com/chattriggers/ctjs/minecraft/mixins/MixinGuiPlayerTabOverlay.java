package com.chattriggers.ctjs.minecraft.mixins;

import com.chattriggers.ctjs.events.EventBus;
import com.chattriggers.ctjs.events.RenderGameOverlayEvent;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {
    @Inject(
            method = "renderPlayerlist",
            at = @At("HEAD"),
            cancellable = true
    )
    private void injectDrawEvent(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn, CallbackInfo ci) {
        if (EventBus.INSTANCE.post(new RenderGameOverlayEvent(RenderGameOverlayEvent.ElementType.PLAYER_LIST))) {
            ci.cancel();
        }
    }
}
