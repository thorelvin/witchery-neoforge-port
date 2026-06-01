package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.SunGrenadeEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.function.Consumer;

public class DuplicationGrenadeItem extends Item {
    private static final String TAG_OWNER_UUID = "OwnerUUID";
    private static final String TAG_OWNER_NAME = "Owner";

    public DuplicationGrenadeItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            UUID ownerUUID = getOwnerUUID(stack);
            String ownerName = getOwnerName(stack);
            if (ownerUUID == null && player instanceof ServerPlayer serverPlayer) {
                ownerUUID = serverPlayer.getUUID();
                ownerName = serverPlayer.getGameProfile().name();
            }

            SunGrenadeEntity grenade = new SunGrenadeEntity(level, player);
            grenade.setDuplicationMode(ownerUUID, ownerName);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(grenade);
            level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW,
                    SoundSource.NEUTRAL, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        String ownerName = getOwnerName(stack);
        if (ownerName.isBlank()) {
            tooltip.accept(Component.translatable("item.witchery.duplication_grenade.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltip.accept(Component.translatable("item.witchery.duplication_grenade.owner", ownerName)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void setOwner(ItemStack stack, ServerPlayer player) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_OWNER_UUID, player.getUUID().toString());
            tag.putString(TAG_OWNER_NAME, player.getGameProfile().name());
        });
    }

    public static UUID getOwnerUUID(ItemStack stack) {
        CompoundTag tag = customData(stack);
        String uuid = tag.getStringOr(TAG_OWNER_UUID, "");
        if (uuid.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String getOwnerName(ItemStack stack) {
        return customData(stack).getStringOr(TAG_OWNER_NAME, "");
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
