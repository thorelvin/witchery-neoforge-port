package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.BloodRoseBlockEntity;
import com.emoniph.witchery.blocks.entity.LeechChestBlockEntity;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.FlyingMonkeyEntity;
import com.emoniph.witchery.entity.TreefydEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.context.UseOnContext;
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
    private static final String TAG_TYPE = "taglock_entity_type";

    public TaglockKitItem(Properties props) { super(props); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (target instanceof TreefydEntity || target instanceof FlameImpEntity || target instanceof FlyingMonkeyEntity) {
            return InteractionResult.PASS;
        }
        if (user.level().isClientSide()) return InteractionResult.SUCCESS;

        if (isFilled(stack)) {
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.already_full",
                    getBoundName(stack)).withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        if (target instanceof Player targetPlayer) {
            bindSinglePlayerTaglock(user, stack, targetPlayer.getUUID(), targetPlayer.getName().getString());

            user.displayClientMessage(Component.translatable("witchery.taglock_kit.collected",
                    targetPlayer.getName()).withStyle(ChatFormatting.GREEN), false);
            return InteractionResult.CONSUME;
        }

        bindSingleEntityTaglock(user, stack, target);
        user.displayClientMessage(Component.translatable("witchery.taglock_kit.collected",
                target.getDisplayName()).withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        boolean bloodRose = ctx.getLevel().getBlockState(ctx.getClickedPos()).is(WitcheryBlocks.BLOOD_ROSE.get());
        boolean leechChest = ctx.getLevel().getBlockState(ctx.getClickedPos()).is(WitcheryBlocks.LEECH_CHEST.get());
        if (!bloodRose && !leechChest) {
            return InteractionResult.PASS;
        }
        if (ctx.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = ctx.getItemInHand();
        Player user = ctx.getPlayer();
        if (user == null) {
            return InteractionResult.PASS;
        }
        if (isFilled(stack)) {
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.already_full",
                    getBoundName(stack)).withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        if (bloodRose) {
            if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof BloodRoseBlockEntity be)) {
                return InteractionResult.PASS;
            }

            BloodRoseBlockEntity.DrainResult drainResult = be.drainStoredPlayerExcept(user);
            if (drainResult.player() != null) {
                bindSinglePlayerTaglock(user, stack, drainResult.player().uuid(), drainResult.player().name());
                user.displayClientMessage(Component.translatable("witchery.taglock_kit.collected_rose",
                        drainResult.player().name()).withStyle(ChatFormatting.GREEN), false);
                return InteractionResult.CONSUME;
            }

            if (drainResult.onlyOwnTaglock()) {
                user.displayClientMessage(Component.translatable("witchery.taglock_kit.only_own_taglock")
                        .withStyle(ChatFormatting.RED), false);
                return InteractionResult.CONSUME;
            }
            if (!drainResult.missingPlayers().isEmpty()) {
                user.displayClientMessage(Component.translatable("witchery.taglock_kit.player_not_logged_in",
                        String.join(", ", drainResult.missingPlayers())).withStyle(ChatFormatting.RED), false);
                return InteractionResult.CONSUME;
            }

            user.displayClientMessage(Component.translatable("witchery.taglock_kit.no_blood_rose_sample")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof LeechChestBlockEntity be)) {
            return InteractionResult.PASS;
        }

        LeechChestBlockEntity.DrainResult drainResult = be.drainStoredPlayerExcept(user);
        if (drainResult.player() != null) {
            bindSinglePlayerTaglock(user, stack, drainResult.player().uuid(), drainResult.player().name());
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.collected_leech_chest",
                    drainResult.player().name()).withStyle(ChatFormatting.GREEN), false);
            return InteractionResult.CONSUME;
        }

        if (drainResult.onlyOwnTaglock()) {
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.only_own_taglock")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }
        if (!drainResult.missingPlayers().isEmpty()) {
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.player_not_logged_in",
                    String.join(", ", drainResult.missingPlayers())).withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        user.displayClientMessage(Component.translatable("witchery.taglock_kit.no_leech_chest_sample")
                .withStyle(ChatFormatting.RED), false);
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

    public static boolean isPlayerTaglock(ItemStack stack) {
        return getBoundEntityTypeId(stack).isEmpty();
    }

    public static Optional<Identifier> getBoundEntityTypeId(ItemStack stack) {
        String typeId = getTag(stack).getStringOr(TAG_TYPE, "");
        if (typeId.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Identifier.parse(typeId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<EntityType<?>> getBoundEntityType(ItemStack stack) {
        Optional<Identifier> typeId = getBoundEntityTypeId(stack);
        if (typeId.isEmpty()) {
            return Optional.empty();
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId.get()).orElse(null);
        return Optional.ofNullable(type);
    }

    public static void bind(ItemStack stack, UUID uuid, String name) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_UUID, uuid.toString());
            tag.putString(TAG_NAME, name);
            tag.remove(TAG_TYPE);
        });
    }

    public static void bindEntity(ItemStack stack, LivingEntity entity) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_UUID, entity.getStringUUID());
            tag.putString(TAG_NAME, entity.getDisplayName().getString());
            tag.putString(TAG_TYPE, typeId.toString());
        });
    }

    private static void bindSinglePlayerTaglock(Player user, ItemStack sourceStack, UUID uuid, String name) {
        if (sourceStack.getCount() == 1) {
            bind(sourceStack, uuid, name);
            return;
        }

        ItemStack filledStack = sourceStack.copyWithCount(1);
        bind(filledStack, uuid, name);
        sourceStack.shrink(1);

        if (!user.getInventory().add(filledStack)) {
            user.drop(filledStack, false);
        }
    }

    private static void bindSingleEntityTaglock(Player user, ItemStack sourceStack, LivingEntity entity) {
        if (sourceStack.getCount() == 1) {
            bindEntity(sourceStack, entity);
            return;
        }

        ItemStack filledStack = sourceStack.copyWithCount(1);
        bindEntity(filledStack, entity);
        sourceStack.shrink(1);

        if (!user.getInventory().add(filledStack)) {
            user.drop(filledStack, false);
        }
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
