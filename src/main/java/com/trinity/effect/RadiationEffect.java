package com.trinity.effect;

import com.trinity.registry.TrinityDamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Radiation sickness. While active it periodically deals a little unavoidable damage to the victim.
 * The nausea/weakness/hunger side of "feeling sick" is layered on by whoever applies the effect (see
 * {@link com.trinity.blast.Blast}), which also stacks vanilla effects for the full poisoned feel.
 */
public class RadiationEffect extends MobEffect {
    public RadiationEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    /** Tick damage roughly once a second, a touch faster at higher amplifiers. */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(5, 25 - amplifier * 5);
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Don't let radiation alone drop a victim below half a heart — it sickens, the blast kills.
        if (entity.getHealth() > 1.0F) {
            entity.hurt(TrinityDamageTypes.radiation(entity.level()), 1.0F + amplifier);
        }
        return true;
    }
}
