package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

public class GlassGobletItem extends Item {
    private static final int FILL_BLOOD_COST = 125;
    private static final int STARTING_VAMPIRE_BLOOD = 250;
    private static final String TAG_BLOOD_NAME = "witchery_blood_name";
    private static final String TAG_BLOOD_UUID = "witchery_blood_uuid";
    private static final String TAG_CHICKEN_BLOOD = "witchery_chicken_blood";
    private static final int[] RITUAL_STRING_OFFSETS = {
            0, -3, 1, -3, 2, -3, 2, -2, 3, -2, 3, -1, 3, 0, 3, 1, 3, 2, 2, 2,
            2, 3, 1, 3, 0, 3, -1, 3, -2, 3, -2, 2, -3, 2, -3, 1, -3, 0
    };
    private static final int[] RITUAL_REDSTONE_OFFSETS = {
            -1, 0, 1, 0, 0, -1, 0, 1, -1, -1, -1, 1, 1, -1, 1, 1
    };

    private final boolean full;

    public GlassGobletItem(Properties properties, boolean full) {
        super(properties.stacksTo(1));
        this.full = full;
    }

    @Override
    public InteractionResult use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, net.minecraft.world.level.Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return stack;
        }
        return full ? drinkFullGoblet(stack, serverLevel, player) : fillFromVampire(stack, serverLevel, player);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        if (full && hasBloodOwner(stack)) {
            tooltip.accept(Component.translatable("item.witchery.full_glass_goblet.tip", getBloodName(stack))
                    .withStyle(ChatFormatting.DARK_RED));
        }
    }

    private static ItemStack fillFromVampire(ItemStack stack, ServerLevel level, ServerPlayer player) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isVampire()) {
            fail(player, "item.witchery.glass_goblet.failed");
            return stack;
        }
        if (data.getBloodLevel() < FILL_BLOOD_COST) {
            fail(player, "item.witchery.glass_goblet.needs_blood");
            return stack;
        }

        data.addBlood(-FILL_BLOOD_COST);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        level.playSound(null, player.getX(), player.getY() + player.getBbHeight() * 0.85D, player.getZ(),
                SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.5F,
                0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + player.getBbHeight() * 0.85D,
                player.getZ(), 8, player.getBbWidth() * 0.35D, player.getBbHeight() * 0.15D,
                player.getBbWidth() * 0.35D, 0.02D);

        return withBloodOwner(new ItemStack(WitcheryItems.FULL_GLASS_GOBLET.get()), player);
    }

    private static ItemStack drinkFullGoblet(ItemStack stack, ServerLevel level, ServerPlayer player) {
        if (!hasBloodOwner(stack)) {
            fail(player, "item.witchery.glass_goblet.failed");
            return stack;
        }
        if (hasChickenBlood(stack)) {
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS,
                    0.5F, level.random.nextFloat() * 0.1F + 0.9F);
            return new ItemStack(WitcheryItems.GLASS_GOBLET.get());
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isVampire()) {
            data.setVampire(true);
            if (data.getBloodLevel() == 0) {
                data.setBloodLevel(STARTING_VAMPIRE_BLOOD);
            }
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
            player.displayClientMessage(Component.translatable("witchery.vampire.goblet.transformed")
                    .withStyle(ChatFormatting.DARK_RED), false);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS,
                0.5F, level.random.nextFloat() * 0.1F + 0.9F);
        return new ItemStack(WitcheryItems.GLASS_GOBLET.get());
    }

    public static void tryFillChickenBloodFromRitual(ServerLevel level, ServerPlayer player, LivingEntity victim) {
        if (victim.getType() != EntityType.CHICKEN || !player.getMainHandItem().is(WitcheryItems.BOLINE.get())) {
            return;
        }

        int gobletSlot = findEmptyGobletInHotbar(player);
        if (gobletSlot < 0) {
            return;
        }

        BlockPos center = victim.blockPosition();
        for (BlockPos scan : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            if (isCreaturesOfNightRitual(level, scan)) {
                player.getInventory().setItem(gobletSlot, withBloodOwner(new ItemStack(WitcheryItems.FULL_GLASS_GOBLET.get()), victim, true));
                player.getInventory().setChanged();
                level.playSound(null, victim.getX(), victim.getY() + victim.getBbHeight() * 0.85D, victim.getZ(),
                        SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.5F,
                        0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, victim.getX(), victim.getY() + victim.getBbHeight() * 0.85D,
                        victim.getZ(), 8, victim.getBbWidth() * 0.35D, victim.getBbHeight() * 0.15D,
                        victim.getBbWidth() * 0.35D, 0.02D);
                return;
            }
        }
    }

    private static int findEmptyGobletInHotbar(ServerPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(WitcheryItems.GLASS_GOBLET.get())) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isCreaturesOfNightRitual(ServerLevel level, BlockPos skullPos) {
        if (!level.getBlockState(skullPos).is(Blocks.SKELETON_SKULL)) {
            return false;
        }
        for (int i = 0; i < RITUAL_STRING_OFFSETS.length; i += 2) {
            if (!level.getBlockState(skullPos.offset(RITUAL_STRING_OFFSETS[i], 0, RITUAL_STRING_OFFSETS[i + 1])).is(Blocks.TRIPWIRE)) {
                return false;
            }
        }
        for (int i = 0; i < RITUAL_REDSTONE_OFFSETS.length; i += 2) {
            if (!level.getBlockState(skullPos.offset(RITUAL_REDSTONE_OFFSETS[i], 0, RITUAL_REDSTONE_OFFSETS[i + 1])).is(Blocks.REDSTONE_WIRE)) {
                return false;
            }
        }
        if (!level.getBlockState(skullPos.offset(-3, 0, -3)).is(Blocks.TORCH)
                || !level.getBlockState(skullPos.offset(-3, 0, 3)).is(Blocks.TORCH)
                || !level.getBlockState(skullPos.offset(3, 0, -3)).is(Blocks.TORCH)
                || !level.getBlockState(skullPos.offset(3, 0, 3)).is(Blocks.TORCH)) {
            return false;
        }
        for (BlockPos scan : BlockPos.betweenClosed(skullPos.offset(-3, 0, -3), skullPos.offset(3, 0, 3))) {
            if (!level.getBlockState(scan.below()).isSolid()
                    || !level.isEmptyBlock(scan.above())
                    || !level.isEmptyBlock(scan.above(2))) {
                return false;
            }
        }
        return true;
    }

    private static ItemStack withBloodOwner(ItemStack stack, ServerPlayer player) {
        return withBloodOwner(stack, player, false);
    }

    private static ItemStack withBloodOwner(ItemStack stack, LivingEntity entity, boolean chickenBlood) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_BLOOD_UUID, entity.getStringUUID());
            tag.putString(TAG_BLOOD_NAME, entity.getName().getString());
            if (chickenBlood) {
                tag.putBoolean(TAG_CHICKEN_BLOOD, true);
            }
        });
        return stack;
    }

    private static boolean hasBloodOwner(ItemStack stack) {
        return getTag(stack).contains(TAG_BLOOD_NAME);
    }

    private static boolean hasChickenBlood(ItemStack stack) {
        return getTag(stack).getBooleanOr(TAG_CHICKEN_BLOOD, false);
    }

    private static String getBloodName(ItemStack stack) {
        return getTag(stack).getStringOr(TAG_BLOOD_NAME, "");
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void fail(ServerPlayer player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey).withStyle(ChatFormatting.RED), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.PLAYERS, 0.5F, 0.5F);
    }
}
