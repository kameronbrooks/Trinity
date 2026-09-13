package com.trinity.registry;

import com.trinity.Trinity;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Items added by Trinity — just the block item for the nuclear bomb. */
public final class TrinityItems {
    private TrinityItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Trinity.MOD_ID);

    public static final DeferredItem<BlockItem> NUCLEAR_BOMB =
            ITEMS.registerSimpleBlockItem(TrinityBlocks.NUCLEAR_BOMB);
}
