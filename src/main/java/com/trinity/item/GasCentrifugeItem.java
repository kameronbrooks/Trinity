package com.trinity.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The gas centrifuge: a reusable crafting tool for uranium enrichment. Put it in the crafting grid
 * with uranium hexafluoride (or low-enriched uranium) and it comes back out after the craft, one
 * point more worn — the same trick a bucket uses to survive a cake recipe. It breaks when its
 * durability runs out, so a full enrichment cascade takes real investment.
 */
public class GasCentrifugeItem extends Item {
    public GasCentrifugeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack worn = stack.copy();
        worn.setCount(1);
        int damage = worn.getDamageValue() + 1;
        if (damage >= worn.getMaxDamage()) {
            return ItemStack.EMPTY; // spun itself apart
        }
        worn.setDamageValue(damage);
        return worn;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trinity.gas_centrifuge").withStyle(ChatFormatting.GRAY));
    }
}
