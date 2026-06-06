package com.emoniph.witchery.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;

public class FortuneMobEffect extends MobEffect {
    public FortuneMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    public static boolean shouldReplaceDrops(boolean silkTouching, boolean hasBlockEntity, boolean hasDrops) {
        return !silkTouching && !hasBlockEntity && hasDrops;
    }

    public static int sourceFortuneBonus(int amplifier) {
        return amplifier > 2 ? 2 : 1;
    }

    public static int boostedFortuneLevel(int currentFortune, int amplifier) {
        return currentFortune + sourceFortuneBonus(amplifier);
    }

    public static void replaceDrops(BlockDropsEvent event, Entity breaker, int amplifier) {
        ServerLevel level = event.getLevel();
        ItemStack boostedTool = boostedTool(level, event.getTool(), amplifier);
        List<ItemStack> replacement = Block.getDrops(event.getState(), level, event.getPos(),
                event.getBlockEntity(), breaker, boostedTool);
        Vec3 dropPos = event.getDrops().isEmpty() ? Vec3.atCenterOf(event.getPos()) : event.getDrops().get(0).position();
        event.getDrops().clear();
        for (ItemStack stack : replacement) {
            if (!stack.isEmpty()) {
                event.getDrops().add(new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, stack));
            }
        }
    }

    public static boolean hasSilkTouch(ServerLevel level, ItemStack tool) {
        Holder<Enchantment> silkTouch = enchantment(level, Enchantments.SILK_TOUCH);
        return tool.getEnchantmentLevel(silkTouch) > 0;
    }

    private static ItemStack boostedTool(ServerLevel level, ItemStack tool, int amplifier) {
        Holder<Enchantment> fortune = enchantment(level, Enchantments.FORTUNE);
        ItemStack boosted = tool.isEmpty() ? new ItemStack(Items.STICK) : tool.copy();
        int boostedLevel = boostedFortuneLevel(boosted.getEnchantmentLevel(fortune), amplifier);
        EnchantmentHelper.updateEnchantments(boosted, enchantments -> enchantments.set(fortune, boostedLevel));
        return boosted;
    }

    private static Holder<Enchantment> enchantment(ServerLevel level, net.minecraft.resources.ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
