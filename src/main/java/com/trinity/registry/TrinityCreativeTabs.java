package com.trinity.registry;

import com.trinity.Trinity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Creative-mode tabs. Trinity gets its own one-item tab, and the bomb is also injected into the
 * vanilla Redstone Blocks tab so it sits alongside TNT.
 */
public final class TrinityCreativeTabs {
    private TrinityCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Trinity.MOD_ID);

    public static final Supplier<CreativeModeTab> TRINITY = TABS.register("trinity", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.trinity"))
            .icon(() -> new ItemStack(TrinityItems.NUCLEAR_BOMB.get()))
            // Every DeferredItem, in registration order — which is production order (ore → bomb).
            .displayItems((parameters, output) ->
                    TrinityItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
            .build());

    /** Registered on the mod bus from {@link Trinity}. */
    public static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = event.getTabKey();
        if (tab == CreativeModeTabs.REDSTONE_BLOCKS || tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TrinityItems.NUCLEAR_BOMB.get());
        } else if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(TrinityItems.URANIUM_ORE.get());
            event.accept(TrinityItems.DEEPSLATE_URANIUM_ORE.get());
        } else if (tab == CreativeModeTabs.INGREDIENTS) {
            event.accept(TrinityItems.RAW_URANIUM.get());
            event.accept(TrinityItems.YELLOWCAKE.get());
            event.accept(TrinityItems.ENRICHED_URANIUM_INGOT.get());
        }
    }
}
