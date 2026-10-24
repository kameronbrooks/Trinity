package com.trinity.item;

import com.trinity.registry.TrinityEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A material that slowly irradiates whoever carries it. Every few seconds each stack in an inventory
 * rolls a small chance, scaled by its {@link #radioactivity} (1 = ore/yellowcake, 3 = weapons-grade),
 * to give the holder a short dose of {@link TrinityEffects#RADIATION}. Radiation can't drop anyone
 * below half a heart, so this is a pressure to stash the stuff in a chest rather than a death sentence.
 * Creative and spectator players are immune.
 */
public class RadioactiveItem extends Item {
    /** How often (ticks) each stack rolls for an exposure. */
    private static final int CHECK_INTERVAL = 80;
    /** Exposure chance per roll, per point of radioactivity. */
    private static final float CHANCE_PER_LEVEL = 0.08F;

    private final int radioactivity;

    public RadioactiveItem(Properties properties, int radioactivity) {
        super(properties);
        this.radioactivity = radioactivity;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if ((level.getGameTime() + slot) % CHECK_INTERVAL != 0) {
            return;
        }
        if (level.random.nextFloat() < CHANCE_PER_LEVEL * radioactivity) {
            living.addEffect(new MobEffectInstance(TrinityEffects.RADIATION, 40 + 30 * radioactivity, 0));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trinity.radioactive." + radioactivity)
                .withStyle(ChatFormatting.GREEN));
    }
}
