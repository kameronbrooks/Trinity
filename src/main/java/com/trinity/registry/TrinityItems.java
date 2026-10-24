package com.trinity.registry;

import com.trinity.Trinity;
import com.trinity.item.GasCentrifugeItem;
import com.trinity.item.RadioactiveItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Every item Trinity adds, in production order. Building a bomb is a ten-phase chain:
 *
 * <ol>
 *   <li><b>Mining</b> — uranium ore → raw uranium</li>
 *   <li><b>Milling</b> — raw uranium → yellowcake (furnace)</li>
 *   <li><b>Conversion</b> — yellowcake → uranium hexafluoride canisters</li>
 *   <li><b>Enrichment</b> — gas centrifuge: UF6 → low-enriched → highly enriched uranium</li>
 *   <li><b>Metallurgy</b> — HEU → enriched uranium ingot (blast furnace only)</li>
 *   <li><b>Core</b> — neutron initiator + ingots + gold → fissile core</li>
 *   <li><b>Implosion</b> — explosive lenses around the core → implosion assembly</li>
 *   <li><b>Detonator</b> — detonator circuits → firing set</li>
 *   <li><b>Housing</b> — casing plates → bomb housing + tail fins</li>
 *   <li><b>Assembly</b> — everything → nuclear bomb</li>
 * </ol>
 *
 * The recipes themselves are data ({@code data/trinity/recipe}); the advancement tree in
 * {@code data/trinity/advancement} walks players through the phases.
 */
public final class TrinityItems {
    private TrinityItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Trinity.MOD_ID);

    // ------------------------------------------------------------------ 1. mining
    public static final DeferredItem<BlockItem> URANIUM_ORE = ITEMS.registerSimpleBlockItem(TrinityBlocks.URANIUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_URANIUM_ORE =
            ITEMS.registerSimpleBlockItem(TrinityBlocks.DEEPSLATE_URANIUM_ORE);
    public static final DeferredItem<RadioactiveItem> RAW_URANIUM =
            ITEMS.registerItem("raw_uranium", p -> new RadioactiveItem(p, 1));

    // ----------------------------------------------------------------- 2. milling
    public static final DeferredItem<RadioactiveItem> YELLOWCAKE =
            ITEMS.registerItem("yellowcake", p -> new RadioactiveItem(p, 1));

    // -------------------------------------------------------------- 3. conversion
    /** Sealed in steel, so (unlike the powders either side of it) it's safe to carry. */
    public static final DeferredItem<Item> URANIUM_HEXAFLUORIDE_CANISTER =
            ITEMS.registerSimpleItem("uranium_hexafluoride_canister");

    // -------------------------------------------------------------- 4. enrichment
    public static final DeferredItem<GasCentrifugeItem> GAS_CENTRIFUGE =
            ITEMS.registerItem("gas_centrifuge", GasCentrifugeItem::new, new Item.Properties().durability(32));
    public static final DeferredItem<RadioactiveItem> LOW_ENRICHED_URANIUM =
            ITEMS.registerItem("low_enriched_uranium", p -> new RadioactiveItem(p, 2));
    public static final DeferredItem<RadioactiveItem> HIGHLY_ENRICHED_URANIUM =
            ITEMS.registerItem("highly_enriched_uranium", p -> new RadioactiveItem(p, 3),
                    new Item.Properties().rarity(Rarity.UNCOMMON));

    // ------------------------------------------------------------- 5. metallurgy
    public static final DeferredItem<RadioactiveItem> ENRICHED_URANIUM_INGOT =
            ITEMS.registerItem("enriched_uranium_ingot", p -> new RadioactiveItem(p, 3),
                    new Item.Properties().rarity(Rarity.UNCOMMON));

    // -------------------------------------------------------------------- 6. core
    public static final DeferredItem<Item> NEUTRON_INITIATOR = ITEMS.registerSimpleItem("neutron_initiator");
    public static final DeferredItem<RadioactiveItem> FISSILE_CORE =
            ITEMS.registerItem("fissile_core", p -> new RadioactiveItem(p, 3),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    // --------------------------------------------------------------- 7. implosion
    public static final DeferredItem<Item> EXPLOSIVE_LENS = ITEMS.registerSimpleItem("explosive_lens");
    /** The core is wrapped in explosives now, which shields most of its radiation. */
    public static final DeferredItem<RadioactiveItem> IMPLOSION_ASSEMBLY =
            ITEMS.registerItem("implosion_assembly", p -> new RadioactiveItem(p, 1),
                    new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    // --------------------------------------------------------------- 8. detonator
    public static final DeferredItem<Item> DETONATOR_CIRCUIT = ITEMS.registerSimpleItem("detonator_circuit");
    public static final DeferredItem<Item> FIRING_SET =
            ITEMS.registerSimpleItem("firing_set", new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    // ----------------------------------------------------------------- 9. housing
    public static final DeferredItem<Item> CASING_PLATE = ITEMS.registerSimpleItem("casing_plate");
    public static final DeferredItem<Item> TAIL_FIN = ITEMS.registerSimpleItem("tail_fin");
    public static final DeferredItem<Item> BOMB_HOUSING =
            ITEMS.registerSimpleItem("bomb_housing", new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    // --------------------------------------------------------------- 10. assembly
    public static final DeferredItem<BlockItem> NUCLEAR_BOMB =
            ITEMS.registerSimpleBlockItem(TrinityBlocks.NUCLEAR_BOMB, new Item.Properties().rarity(Rarity.EPIC));
}
