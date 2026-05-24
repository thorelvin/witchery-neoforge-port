package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.UUID;

/**
 * Vampiric Poppet — requires two taglocks.
 * First right-click on a living entity: binds the SOURCE (the witch who is protected).
 * Second right-click on a living entity: binds the VICTIM (who absorbs the redirected damage).
 * When the source player takes damage, it is fully redirected to the victim.
 * Costs 66 durability per activation; consumed at 1000.
 */
public class VampiricPoppetItem extends PoppetItem {

    private static final String TAG_VICTIM_UUID = "victim_uuid";
    private static final String TAG_VICTIM_NAME = "victim_name";
    private static final String TAG_VICTIM_ENTITY_TYPE = "victim_entity_type";

    public VampiricPoppetItem(Properties props) { super(props); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide()) return InteractionResult.SUCCESS;

        if (!isBound(stack)) {
            // First click: bind the source using parent binding logic
            return super.interactLivingEntity(stack, user, target, hand);
        }

        if (getVictimUUID(stack).isPresent()) {
            user.displayClientMessage(Component.translatable("witchery.poppet.vampiric.fully_bound",
                    getBoundName(stack), getVictimName(stack)).withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        // Second click: bind the victim
        bindVictim(stack, target);
        user.displayClientMessage(Component.translatable("witchery.poppet.vampiric.victim_bound",
                target.getDisplayName()).withStyle(ChatFormatting.DARK_RED), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx,
                                net.minecraft.world.item.component.TooltipDisplay _display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("witchery.poppet.vampiric.tooltip")
                .withStyle(ChatFormatting.DARK_RED));
        if (isBound(stack)) {
            tooltip.accept(Component.translatable("witchery.poppet.vampiric.tooltip.source", getBoundName(stack))
                    .withStyle(ChatFormatting.GRAY));
            if (getVictimUUID(stack).isPresent()) {
                tooltip.accept(Component.translatable("witchery.poppet.vampiric.tooltip.victim", getVictimName(stack))
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltip.accept(Component.translatable("witchery.poppet.vampiric.tooltip.no_victim")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        } else {
            tooltip.accept(Component.translatable("witchery.poppet.tooltip.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Static helpers ─────────────────────────────────────────────────────────

    public static Optional<UUID> getVictimUUID(ItemStack stack) {
        String raw = getTag(stack).getStringOr(TAG_VICTIM_UUID, "");
        if (raw.isEmpty()) return Optional.empty();
        try { return Optional.of(UUID.fromString(raw)); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    public static String getVictimName(ItemStack stack) {
        return getTag(stack).getStringOr(TAG_VICTIM_NAME, "");
    }

    public static void bindVictim(ItemStack stack, LivingEntity entity) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_VICTIM_UUID, entity.getStringUUID());
            tag.putString(TAG_VICTIM_NAME, entity.getDisplayName().getString());
            if (entity instanceof Player) {
                tag.remove(TAG_VICTIM_ENTITY_TYPE);
            } else {
                tag.putString(TAG_VICTIM_ENTITY_TYPE, typeId.toString());
            }
        });
    }

    public static void bindVictimFromTaglock(ItemStack stack, ItemStack taglock) {
        Optional<UUID> uuid = TaglockKitItem.getUUID(taglock);
        if (uuid.isEmpty()) {
            return;
        }
        Optional<Identifier> typeId = TaglockKitItem.getBoundEntityTypeId(taglock);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_VICTIM_UUID, uuid.get().toString());
            tag.putString(TAG_VICTIM_NAME, TaglockKitItem.getBoundName(taglock));
            if (typeId.isPresent()) {
                tag.putString(TAG_VICTIM_ENTITY_TYPE, typeId.get().toString());
            } else {
                tag.remove(TAG_VICTIM_ENTITY_TYPE);
            }
        });
    }
}
