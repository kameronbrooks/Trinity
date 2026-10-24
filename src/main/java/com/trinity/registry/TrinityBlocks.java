package com.trinity.registry;

import com.trinity.Trinity;
import com.trinity.block.NuclearBombBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks added by Trinity: the bomb itself and the uranium ores it's ultimately made from. */
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

    /**
     * Uranium ore: an emerald-ore lookalike that glows faintly, generated deep underground (see
     * {@code data/trinity/worldgen}). Needs an iron pickaxe; drops raw uranium, or itself with silk touch.
     */
    public static final DeferredBlock<DropExperienceBlock> URANIUM_ORE = BLOCKS.register("uranium_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.EMERALD_ORE).lightLevel(state -> 3)));

    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_URANIUM_ORE = BLOCKS.register("deepslate_uranium_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_EMERALD_ORE).lightLevel(state -> 3)));
}
