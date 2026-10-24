package com.trinity.gametest;

import com.trinity.Trinity;
import com.trinity.registry.TrinityBlocks;
import com.trinity.registry.TrinityItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Headless checks for the 0.2.0 production chain, run via {@code runGameTestServer} (which exits when
 * done). They drive the real recipe manager and loot tables, so they catch data typos as well as the
 * gas centrifuge's reusable-tool behaviour. Inert in normal play.
 */
@GameTestHolder(Trinity.MOD_ID)
@PrefixGameTestTemplate(false) // template "empty" as-is, not "<class>.empty"
public final class TrinityGameTests {
    private TrinityGameTests() {}

    /** Enrichment consumes the UF6 but hands the centrifuge back, one point more worn. */
    @GameTest(template = "empty")
    public static void centrifugeReturnsWornAfterEnrichment(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack centrifuge = new ItemStack(TrinityItems.GAS_CENTRIFUGE.get());
        CraftingInput input = grid(centrifuge, TrinityItems.URANIUM_HEXAFLUORIDE_CANISTER.get(), 4);

        RecipeHolder<CraftingRecipe> recipe = craft(helper, level, input, "low_enriched_uranium");
        ItemStack out = recipe.value().assemble(input, level.registryAccess());
        helper.assertTrue(out.is(TrinityItems.LOW_ENRICHED_URANIUM.get()), "should make low-enriched uranium");

        ItemStack returned = remainderOf(recipe, input);
        helper.assertTrue(returned.is(TrinityItems.GAS_CENTRIFUGE.get()), "centrifuge should be returned");
        helper.assertTrue(returned.getDamageValue() == 1, "returned centrifuge should be worn by exactly 1");
        helper.succeed();
    }

    /** A centrifuge on its last point of durability is used up by the craft. */
    @GameTest(template = "empty")
    public static void centrifugeBreaksAtMaxWear(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack centrifuge = new ItemStack(TrinityItems.GAS_CENTRIFUGE.get());
        centrifuge.setDamageValue(centrifuge.getMaxDamage() - 1);
        CraftingInput input = grid(centrifuge, TrinityItems.LOW_ENRICHED_URANIUM.get(), 4);

        RecipeHolder<CraftingRecipe> recipe = craft(helper, level, input, "highly_enriched_uranium");
        helper.assertTrue(remainderOf(recipe, input).isEmpty(), "worn-out centrifuge should break");
        helper.succeed();
    }

    /** Firing set / implosion assembly / housing / fins in the right shape make the bomb. */
    @GameTest(template = "empty")
    public static void finalAssemblyMakesBomb(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack e = ItemStack.EMPTY;
        ItemStack fin = new ItemStack(TrinityItems.TAIL_FIN.get());
        CraftingInput input = CraftingInput.of(3, 3, List.of(
                e, new ItemStack(TrinityItems.FIRING_SET.get()), e,
                e, new ItemStack(TrinityItems.IMPLOSION_ASSEMBLY.get()), e,
                fin, new ItemStack(TrinityItems.BOMB_HOUSING.get()), fin));

        RecipeHolder<CraftingRecipe> recipe = craft(helper, level, input, "nuclear_bomb");
        helper.assertTrue(recipe.value().assemble(input, level.registryAccess()).is(TrinityItems.NUCLEAR_BOMB.get()),
                "should make a nuclear bomb");
        helper.succeed();
    }

    /** HEU only becomes an ingot in a blast furnace — a plain furnace won't take it. */
    @GameTest(template = "empty")
    public static void ingotNeedsBlastFurnace(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SingleRecipeInput heu = new SingleRecipeInput(new ItemStack(TrinityItems.HIGHLY_ENRICHED_URANIUM.get()));
        var blasting = level.getRecipeManager().getRecipeFor(RecipeType.BLASTING, heu, level);
        helper.assertTrue(blasting.isPresent(), "blast furnace should accept highly enriched uranium");
        helper.assertTrue(blasting.get().value().assemble(heu, level.registryAccess())
                .is(TrinityItems.ENRICHED_URANIUM_INGOT.get()), "should cast an enriched uranium ingot");
        helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, heu, level).isEmpty(),
                "a regular furnace must not accept highly enriched uranium");
        helper.succeed();
    }

    /** Mining uranium ore with an iron pickaxe drops raw uranium; silk touch is covered by the loot table. */
    @GameTest(template = "empty")
    public static void uraniumOreDropsRawUranium(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        for (var ore : List.of(TrinityBlocks.URANIUM_ORE.get(), TrinityBlocks.DEEPSLATE_URANIUM_ORE.get())) {
            BlockState state = ore.defaultBlockState();
            List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, new ItemStack(Items.IRON_PICKAXE));
            helper.assertTrue(drops.stream().anyMatch(s -> s.is(TrinityItems.RAW_URANIUM.get()) && s.getCount() >= 1),
                    ore + " should drop raw uranium, got " + drops);
        }
        helper.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** A 3x3 grid: the tool in the first slot, then `count` of `material`, the rest empty. */
    private static CraftingInput grid(ItemStack tool, Item material, int count) {
        List<ItemStack> items = new ArrayList<>();
        items.add(tool);
        for (int i = 0; i < 8; i++) {
            items.add(i < count ? new ItemStack(material) : ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, items);
    }

    private static RecipeHolder<CraftingRecipe> craft(GameTestHelper helper, ServerLevel level,
                                                      CraftingInput input, String expectedId) {
        Optional<RecipeHolder<CraftingRecipe>> recipe =
                level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        helper.assertTrue(recipe.isPresent(), "no crafting recipe matched for " + expectedId);
        ResourceLocation id = recipe.get().id();
        helper.assertTrue(id.equals(ResourceLocation.fromNamespaceAndPath(Trinity.MOD_ID, expectedId)),
                "expected trinity:" + expectedId + " but matched " + id);
        return recipe.get();
    }

    private static ItemStack remainderOf(RecipeHolder<CraftingRecipe> recipe, CraftingInput input) {
        NonNullList<ItemStack> remaining = recipe.value().getRemainingItems(input);
        return remaining.stream().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
    }
}
