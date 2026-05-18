package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Taglock Kit — right-click a player to collect their taglock (UUID + name).
 * Used as a ritual ingredient and to identify target players for poppets.
 */
public class TaglockKitItem extends Item {

    private static final String TAG_UUID = "taglock_uuid";
    private static final String TAG_NAME = "taglock_name";

    public TaglockKitItem(Properties props) { super(props); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player targetPlayer)) return InteractionResult.PASS;
        if (user.level().isClientSide()) return InteractionResult.SUCCESS;

        if (isFilled(stack)) {
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.already_full",
                    getBoundName(stack)).withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        // Collect the taglock
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_UUID, targetPlayer.getStringUUID());
            tag.putString(TAG_NAME, targetPlayer.getName().getString());
        });

        user.displayClientMessage(Component.translatable("witchery.taglock_kit.collected",
                targetPlayer.getName()).withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        if (isFilled(stack)) {
            tooltip.accept(Component.translatable("witchery.taglock_kit.tooltip.filled", getBoundName(stack))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("witchery.taglock_kit.tooltip.empty")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    public static boolean isFilled(ItemStack stack) {
        return getTag(stack).contains(TAG_UUID);
    }

    public static Optional<UUID> getUUID(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(TAG_UUID)) return Optional.empty();
        try { return Optional.of(UUID.fromString(tag.getStringOr(TAG_UUID, ""))); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    public static String getBoundName(ItemStack stack) {
        return getTag(stack).getStringOr(TAG_NAME, "");
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
