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

public class PoppetItem extends Item {

    private static final String TAG_UUID = "bound_uuid";
    private static final String TAG_NAME = "bound_name";

    public PoppetItem(Properties props) {
        super(props);
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    /** Right-click a player to bind the poppet to them. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player targetPlayer)) return InteractionResult.PASS;
        if (user.level().isClientSide()) return InteractionResult.SUCCESS;

        if (isBound(stack)) {
            user.displayClientMessage(Component.translatable(
                    "witchery.poppet.already_bound", getBoundName(stack))
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        bind(stack, targetPlayer);
        user.displayClientMessage(Component.translatable(
                "witchery.poppet.bound", targetPlayer.getName())
                .withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.CONSUME;
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        if (isBound(stack)) {
            tooltip.accept(Component.translatable("witchery.poppet.tooltip.bound", getBoundName(stack))
                    .withStyle(ChatFormatting.GRAY));
            // Show sympathy charges if this is the plain sympathetic poppet
            net.minecraft.nbt.CompoundTag tag = getTag(stack);
            int charges = tag.getIntOr("sympathy_charges", 5);
            tooltip.accept(Component.translatable("witchery.poppet.tooltip.charges", charges)
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.accept(Component.translatable("witchery.poppet.tooltip.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Static helpers (used by event handler) ────────────────────────────────

    public static boolean isBound(ItemStack stack) {
        return getTag(stack).contains(TAG_UUID);
    }

    public static Optional<UUID> getBoundUUID(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(TAG_UUID)) return Optional.empty();
        try { return Optional.of(UUID.fromString(tag.getStringOr(TAG_UUID, ""))); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    public static String getBoundName(ItemStack stack) {
        return getTag(stack).getStringOr(TAG_NAME, "");
    }

    public static void bind(ItemStack stack, Player player) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_UUID, player.getStringUUID());
            tag.putString(TAG_NAME, player.getName().getString());
        });
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
