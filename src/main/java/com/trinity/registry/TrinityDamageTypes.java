package com.trinity.registry;

import com.trinity.Trinity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/**
 * Trinity's damage types. The {@link DamageType} definitions themselves live in data files
 * ({@code data/trinity/damage_type/*.json}); this class just holds the {@link ResourceKey}s and
 * helpers that build a {@link DamageSource} from the current level's registry.
 */
public final class TrinityDamageTypes {
    private TrinityDamageTypes() {}

    /** The killing blast at ground zero. */
    public static final ResourceKey<DamageType> NUCLEAR_BLAST = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Trinity.MOD_ID, "nuclear_blast"));

    /** Slow damage from radiation sickness. */
    public static final ResourceKey<DamageType> RADIATION = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Trinity.MOD_ID, "radiation"));

    public static DamageSource nuclearBlast(Level level) {
        return source(level, NUCLEAR_BLAST);
    }

    public static DamageSource radiation(Level level) {
        return source(level, RADIATION);
    }

    private static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }
}
