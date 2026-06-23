package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Locale;
import java.util.function.Consumer;

public class BiomeBookItem extends Item {
    private static final String TAG_CURRENT_PAGE = "CurrentPage";

    public BiomeBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int current = getSelectedPage(stack);
        int delta = player.isShiftKeyDown() ? -1 : 1;
        int next = Math.floorMod(current + delta, BiomeNoteItem.BIOME_PAGES.size());
        if (!level.isClientSide()) {
            setSelectedPage(stack, next);
            ResourceKey<Biome> biome = BiomeNoteItem.BIOME_PAGES.get(next);
            player.displayClientMessage(Component.translatable("item.witchery.extended_biomes_book.page",
                    next + 1, BiomeNoteItem.BIOME_PAGES.size(), BiomeNoteItem.displayName(biome)), true);
            player.displayClientMessage(describeBiome(level, biome), false);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        ResourceKey<Biome> biome = getSelectedBiomeKey(stack);
        tooltip.accept(Component.translatable("item.witchery.extended_biomes_book.page",
                getSelectedPage(stack) + 1, BiomeNoteItem.BIOME_PAGES.size(), BiomeNoteItem.displayName(biome))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.translatable("item.witchery.extended_biomes_book.tip")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public static ResourceKey<Biome> getSelectedBiomeKey(ItemStack stack) {
        return BiomeNoteItem.BIOME_PAGES.get(getSelectedPage(stack));
    }

    public static int getSelectedPage(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int page = tag.getIntOr(TAG_CURRENT_PAGE, 0);
        return Math.clamp(page, 0, BiomeNoteItem.BIOME_PAGES.size() - 1);
    }

    public static void setSelectedPage(ItemStack stack, int page) {
        int clamped = Math.clamp(page, 0, BiomeNoteItem.BIOME_PAGES.size() - 1);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_CURRENT_PAGE, clamped));
    }

    private static Component describeBiome(Level level, ResourceKey<Biome> biomeKey) {
        var lookup = level.registryAccess().lookupOrThrow(Registries.BIOME);
        return lookup.get(biomeKey)
                .map(holder -> {
                    Biome biome = holder.value();
                    Component yes = Component.translatable("witchery.yes");
                    Component no = Component.translatable("witchery.no");
                    Component rainfall = biome.hasPrecipitation() ? yes : no;
                    Component snows = biome.coldEnoughToSnow(net.minecraft.core.BlockPos.ZERO, 0) ? yes : no;
                    Component lightning = biome.hasPrecipitation() ? yes : no;
                    String temp = String.format(Locale.ROOT, "%.2f", biome.getBaseTemperature());
                    return Component.translatable("item.witchery.extended_biomes_book.stats",
                            rainfall, snows, lightning, temp).withStyle(ChatFormatting.GRAY);
                })
                .orElse(Component.translatable("item.witchery.extended_biomes_book.stats_missing")
                        .withStyle(ChatFormatting.RED));
    }
}
