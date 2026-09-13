package com.trinity.registry;

import com.trinity.Trinity;
import com.trinity.effect.RadiationEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Mob effects added by Trinity. */
public final class TrinityEffects {
    private TrinityEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Trinity.MOD_ID);

    /**
     * Radiation sickness — a harmful, sickly-green effect left behind by a blast. It steadily hurts
     * the victim and is paired with vanilla nausea/weakness/hunger/mining-fatigue when applied so it
     * genuinely feels like radiation poisoning. Colour is a radioactive green.
     */
    public static final Holder<MobEffect> RADIATION =
            EFFECTS.register("radiation", () -> new RadiationEffect(0xFF4CDD3A));
}
