package com.trinity.registry;

import com.trinity.Trinity;
import com.trinity.block.NuclearBombBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks added by Trinity. Currently just the nuclear bomb. */
public final class TrinityBlocks {
    private TrinityBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Trinity.MOD_ID);

    public static final DeferredBlock<NuclearBombBlock> NUCLEAR_BOMB = BLOCKS.register("nuclear_bomb",
            () -> new NuclearBombBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    // Glow while it is armed and counting down.
                    .lightLevel(state -> state.getValue(NuclearBombBlock.ARMED) ? 15 : 0)));
}
