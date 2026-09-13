package com.trinity;

import com.trinity.registry.TrinityBlockEntities;
import com.trinity.registry.TrinityBlocks;
import com.trinity.registry.TrinityCreativeTabs;
import com.trinity.registry.TrinityEffects;
import com.trinity.registry.TrinityItems;
import com.trinity.registry.TrinityParticles;
import com.trinity.network.TrinityNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Trinity — a nuclear bomb for Minecraft.
 *
 * <p>The mod adds one block, the {@code Nuclear Bomb}. Arm it with flint and steel (or a redstone
 * signal) and it counts down a short fuse before detonating. The detonation is deliberately
 * <em>not</em> a swarm of primed-TNT entities — that is what freezes or crashes the game. Instead a
 * single {@link com.trinity.blast.Blast} runs on the server tick loop and does the work in bounded
 * slices spread over many ticks: an expanding shockwave that carves a crater, levels and scorches
 * everything in range (wood is fused into coal blocks), sets scattered fires, and kills every living
 * thing caught in it — all with block updates and drops suppressed so the world edits stay cheap.
 * A tall campfire-smoke mushroom cloud and a blinding flash play out above it, and a lingering green
 * radiation cloud poisons the crater for minutes afterwards.
 *
 * <p>This class only wires registration onto the mod event bus; the actual content lives in the
 * {@code com.trinity.registry} classes and {@code com.trinity.blast}.
 */
@Mod(Trinity.MOD_ID)
public final class Trinity {
    public static final String MOD_ID = "trinity";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Trinity(IEventBus modBus, ModContainer container) {
        TrinityBlocks.BLOCKS.register(modBus);
        TrinityItems.ITEMS.register(modBus);
        TrinityBlockEntities.BLOCK_ENTITIES.register(modBus);
        TrinityEffects.EFFECTS.register(modBus);
        TrinityParticles.PARTICLE_TYPES.register(modBus);
        TrinityCreativeTabs.TABS.register(modBus);

        // BuildCreativeModeTabContentsEvent fires on the mod bus; register the vanilla-tab injection
        // there explicitly rather than relying on @EventBusSubscriber's default bus.
        modBus.addListener(TrinityCreativeTabs::addToVanillaTabs);
        // Network payloads (the nuke-flash packet) also register on the mod bus.
        modBus.addListener(TrinityNetwork::register);

        LOGGER.info("Trinity initialising — arming the device.");
    }
}
