package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;
import java.util.UUID;

public class PoppetItem extends Item {

    private static final String TAG_UUID = "bound_uuid";
    private static final String TAG_NAME = "bound_name";
    private static final String TAG_ENTITY_TYPE = "bound_entity_type";

    public PoppetItem(Properties props) {
        super(props);
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide()) return InteractionResult.SUCCESS;

        if (isBound(stack)) {
            user.displayClientMessage(Component.translatable(
                    "witchery.poppet.already_bound", getBoundName(stack))
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        if (target instanceof Player targetPlayer) {
            bind(stack, targetPlayer);
        } else {
            bindEntity(stack, target);
        }

        user.displayClientMessage(Component.translatable(
                "witchery.poppet.bound", target.getDisplayName())
                .withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.CONSUME;
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        if (isBound(stack)) {
            tooltip.accept(Component.translatable("witchery.poppet.tooltip.bound", getBoundName(stack))
                    .withStyle(ChatFormatting.GRAY));
            net.minecraft.nbt.CompoundTag tag = getTag(stack);
            int charges = tag.getIntOr("sympathy_charges", 5);
            tooltip.accept(Component.translatable("witchery.poppet.tooltip.charges", charges)
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.accept(Component.translatable("witchery.poppet.tooltip.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Static helpers (used by subclasses and event handlers) ────────────────

    public static boolean isBound(ItemStack stack) {
        return getTag(stack).contains(TAG_UUID);
    }

    public static boolean isMobBound(ItemStack stack) {
        return isBound(stack) && getTag(stack).contains(TAG_ENTITY_TYPE);
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
            tag.remove(TAG_ENTITY_TYPE);
        });
    }

    public static void bindEntity(ItemStack stack, LivingEntity entity) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_UUID, entity.getStringUUID());
            tag.putString(TAG_NAME, entity.getDisplayName().getString());
            tag.putString(TAG_ENTITY_TYPE, typeId.toString());
        });
    }

    public static Optional<EntityType<?>> getBoundEntityType(ItemStack stack) {
        String typeStr = getTag(stack).getStringOr(TAG_ENTITY_TYPE, "");
        if (typeStr.isEmpty()) return Optional.empty();
        try {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(typeStr));
        } catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    protected static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
