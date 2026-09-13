package com.trinity.client;

import com.trinity.Trinity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Draws and fades the nuke flash: a full-screen white overlay whose opacity is {@link ClientFlash}'s
 * current brightness. That is what actually reads as a nuclear flash — the whole view whites out and
 * decays, with no lightning bolt in sight.
 */
@EventBusSubscriber(modid = Trinity.MOD_ID, value = Dist.CLIENT)
public final class TrinityHud {
    private TrinityHud() {}

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        float a = ClientFlash.alpha();
        if (a <= 0.01f) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int alpha = (int) (Mth.clamp(a, 0.0f, 1.0f) * 255.0f);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0xFFFFFF);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        ClientFlash.tick();
    }
}
