package com.trinity.registry;

import com.trinity.Trinity;
import com.trinity.block.entity.NuclearBombBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Block-entity type registration. The bomb's block entity runs its fuse countdown. */
public final class TrinityBlockEntities {
    private TrinityBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Trinity.MOD_ID);

    public static final Supplier<BlockEntityType<NuclearBombBlockEntity>> NUCLEAR_BOMB =
            BLOCK_ENTITIES.register("nuclear_bomb", () -> BlockEntityType.Builder
                    .of(NuclearBombBlockEntity::new, TrinityBlocks.NUCLEAR_BOMB.get())
                    .build(null));
}
