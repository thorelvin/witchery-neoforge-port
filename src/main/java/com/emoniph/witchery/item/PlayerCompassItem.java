package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Player Compass — right-click a player to bind; right-click in air to reveal
 * direction and distance to the bound player (server-side lookup).
 */
public class PlayerCompassItem extends Item {

    private static final String TAG_UUID = "compass_uuid";
    private static final String TAG_NAME = "compass_name";

    public PlayerCompassItem(Properties props) { super(props); }

    // ── Binding ───────────────────────────────────────────────────────────────

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide()) return InteractionResult.SUCCESS;

        if (isBound(stack)) {
            user.displayClientMessage(Component.translatable("witchery.player_compass.already_bound", getBoundName(stack))
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        if (!(target instanceof Player targetPlayer)) {
            user.displayClientMessage(Component.translatable("witchery.player_compass.players_only")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        bind(stack, targetPlayer);
        user.displayClientMessage(Component.translatable("witchery.player_compass.bound", targetPlayer.getName())
                .withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.CONSUME;
    }

    // ── Use (in air) ──────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS.heldItemTransformedTo(stack);

        if (!isBound(stack)) {
            user.displayClientMessage(Component.translatable("witchery.player_compass.unbound")
                    .withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.PASS;
        }

        UUID targetUUID = getBoundUUID(stack).orElse(null);
        if (targetUUID == null) return InteractionResult.PASS;

        ServerPlayer target = ((ServerLevel) level).getServer().getPlayerList().getPlayer(targetUUID);
        if (target == null) {
            user.displayClientMessage(Component.translatable("witchery.player_compass.offline", getBoundName(stack))
                    .withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.SUCCESS;
        }

        if (target.level() != level) {
            user.displayClientMessage(Component.translatable("witchery.player_compass.other_dimension", getBoundName(stack))
                    .withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.SUCCESS;
        }

        Vec3 diff = target.position().subtract(user.position());
        int dist = (int) Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        String cardinal = getCardinal(Math.atan2(diff.z, diff.x));

        user.displayClientMessage(Component.translatable("witchery.player_compass.locate",
                getBoundName(stack), dist, cardinal).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return InteractionResult.SUCCESS;
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx,
                                net.minecraft.world.item.component.TooltipDisplay _display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (isBound(stack)) {
            tooltip.accept(Component.translatable("witchery.player_compass.tooltip.bound", getBoundName(stack))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("witchery.player_compass.tooltip.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static boolean bindFromTaglock(ItemStack stack, ItemStack taglock) {
        Optional<UUID> uuid = TaglockKitItem.getUUID(taglock);
        if (uuid.isEmpty() || !TaglockKitItem.isPlayerTaglock(taglock)) {
            return false;
        }
        bind(stack, uuid.get(), TaglockKitItem.getBoundName(taglock));
        return true;
    }

    private static void bind(ItemStack stack, Player player) {
        bind(stack, player.getUUID(), player.getName().getString());
    }

    private static void bind(ItemStack stack, UUID uuid, String name) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_UUID, uuid.toString());
            tag.putString(TAG_NAME, name);
        });
    }

    public static boolean isBound(ItemStack stack) {
        return getTag(stack).contains(TAG_UUID);
    }

    public static Optional<UUID> getBoundUUID(ItemStack stack) {
        String raw = getTag(stack).getStringOr(TAG_UUID, "");
        if (raw.isEmpty()) return Optional.empty();
        try { return Optional.of(UUID.fromString(raw)); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    public static String getBoundName(ItemStack stack) {
        return getTag(stack).getStringOr(TAG_NAME, "");
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static String getCardinal(double atan2) {
        // atan2(z, x): East=0, South=π/2, West=π, North=-π/2
        double deg = Math.toDegrees(atan2);
        if (deg < 0) deg += 360;
        if (deg >= 337.5 || deg < 22.5)  return "E";
        if (deg < 67.5)  return "SE";
        if (deg < 112.5) return "S";
        if (deg < 157.5) return "SW";
        if (deg < 202.5) return "W";
        if (deg < 247.5) return "NW";
        if (deg < 292.5) return "N";
        return "NE";
    }
}
