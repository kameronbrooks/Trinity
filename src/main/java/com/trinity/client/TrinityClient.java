package com.trinity.client;

import com.trinity.Trinity;
import com.trinity.registry.TrinityParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Client-only setup: binds the {@code big_smoke} particle to its renderer. */
@EventBusSubscriber(modid = Trinity.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TrinityClient {
    private TrinityClient() {}

    @SubscribeEvent
    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(TrinityParticles.BIG_SMOKE.get(), BigSmokeParticle.Provider::new);
    }
}
