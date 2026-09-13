package com.trinity.blast;

import com.trinity.Trinity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns every in-progress {@link Blast} and advances them one slice per server tick. Keeping the work
 * on the main server thread (and off any block/entity's own ticker, since the bomb block is gone the
 * instant it detonates) is what lets a blast outlive its block and spread its cost over time.
 */
@EventBusSubscriber(modid = Trinity.MOD_ID)
public final class BlastManager {
    private BlastManager() {}

    private static final List<Blast> ACTIVE = new ArrayList<>();

    /** Begin a full-size detonation at {@code pos}. */
    public static void detonate(ServerLevel level, BlockPos pos) {
        ACTIVE.add(new Blast(level, pos.immutable(), Blast.RADIUS));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (!ACTIVE.isEmpty()) {
            ACTIVE.removeIf(Blast::tick);
        }
    }

    /** Drop any leftover blasts so a stale {@link ServerLevel} is never held across a world reload. */
    @SubscribeEvent
    static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.clear();
    }
}
