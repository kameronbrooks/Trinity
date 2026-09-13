package com.trinity.registry;

import com.trinity.Trinity;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Custom particle types. */
public final class TrinityParticles {
    private TrinityParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Trinity.MOD_ID);

    /**
     * A vastly oversized smoke puff used to build the mushroom cloud. Vanilla smoke/campfire
     * particles render at a fixed size you can't scale from the spawn call, so we register our own
     * and give it a large quad size on the client (see {@code com.trinity.client.BigSmokeParticle}).
     * {@code overrideLimiter = true} so it still shows even with reduced-particles settings.
     */
    public static final Supplier<SimpleParticleType> BIG_SMOKE =
            PARTICLE_TYPES.register("big_smoke", () -> new SimpleParticleType(true));
}
