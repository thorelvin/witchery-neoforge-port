package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.function.Consumer;

public class HornOfTheHuntItem extends Item {
    private static final int SOURCE_DURABILITY = 1;
    private static final int USE_DURATION = 40;
    private static final int MIN_SPAWN_RANGE = 2;
    private static final int MAX_SPAWN_RANGE = 8;
    private static final int SPAWN_ATTEMPTS = 16;

    public HornOfTheHuntItem(Properties properties) {
        super(properties.stacksTo(1).durability(SOURCE_DURABILITY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || remainingUseDuration != 1 || !(entity instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!summonHuntsman(serverLevel, player)) {
            player.displayClientMessage(Component.translatable("item.witchery.horn_of_the_hunt.failed")
                    .withStyle(ChatFormatting.GRAY), false);
            return;
        }

        if (!player.isCreative()) {
            EquipmentSlot slot = player.getUsedItemHand() == InteractionHand.OFF_HAND
                    ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            stack.hurtAndBreak(2, player, slot);
        }
    }

    private static boolean summonHuntsman(ServerLevel level, ServerPlayer player) {
        HornedHuntsmanEntity huntsman = WitcheryEntities.HORNED_HUNTSMAN.get()
                .create(level, EntitySpawnReason.TRIGGERED);
        if (huntsman == null) {
            return false;
        }

        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            BlockPos pos = randomSpawnPos(level, player);
            huntsman.snapTo(pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(huntsman)) {
                continue;
            }

            huntsman.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, null);
            huntsman.setPersistenceRequired();
            huntsman.setPlayerCreated(true);
            huntsman.causeExplosiveEntrance();
            huntsman.setInvulnerableTicks(150);
            huntsman.setHealth(huntsman.getMaxHealth() * 0.25F);
            huntsman.setTarget(player);
            level.addFreshEntity(huntsman);
            level.sendParticles(ParticleTypes.EXPLOSION,
                    huntsman.getX(), huntsman.getY() + 1.0D, huntsman.getZ(),
                    12, 1.0D, 1.0D, 1.0D, 0.0D);
            level.playSound(null, huntsman.blockPosition(), SoundEvents.WITHER_SPAWN,
                    SoundSource.HOSTILE, 1.0F, 1.0F);
            return true;
        }

        huntsman.discard();
        return false;
    }

    private static BlockPos randomSpawnPos(ServerLevel level, ServerPlayer player) {
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double radius = MIN_SPAWN_RANGE + level.random.nextInt(MAX_SPAWN_RANGE - MIN_SPAWN_RANGE + 1);
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, player.blockPosition().getY(), z));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".tip")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
