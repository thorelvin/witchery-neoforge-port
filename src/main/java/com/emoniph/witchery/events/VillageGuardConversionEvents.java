package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class VillageGuardConversionEvents {
    private VillageGuardConversionEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || event.isCanceled()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !player.isShiftKeyDown()
                || !player.getItemInHand(event.getHand()).is(Items.BELL)) {
            return;
        }

        if (event.getTarget() instanceof Villager villager) {
            finishEvent(event);
            promoteVillager(level, player, villager);
        } else if (event.getTarget() instanceof VillageGuardEntity guard) {
            finishEvent(event);
            dismissGuard(level, player, guard);
        }
    }

    private static void promoteVillager(ServerLevel level, ServerPlayer player, Villager villager) {
        if (!canPromote(level, player, villager)) {
            return;
        }
        VillageGuardEntity guard = WitcheryEntities.VILLAGE_GUARD.get().create(level, EntitySpawnReason.CONVERSION);
        if (guard == null) {
            player.displayClientMessage(Component.literal("No guard answered the bell.").withStyle(ChatFormatting.RED),
                    true);
            return;
        }
        copyPose(villager, guard);
        if (villager.hasCustomName()) {
            guard.setCustomName(villager.getCustomName());
            guard.setCustomNameVisible(villager.isCustomNameVisible());
        }
        guard.captureVillagerOrigin(villager);
        guard.finalizeSpawn(level, level.getCurrentDifficultyAt(villager.blockPosition()),
                EntitySpawnReason.CONVERSION, null);
        guard.setPersistenceRequired();
        level.addFreshEntity(guard);
        villager.discard();
        conversionEffects(level, guard.position());
        player.displayClientMessage(Component.literal("The bell appoints a village guard.").withStyle(ChatFormatting.GOLD),
                true);
    }

    private static void dismissGuard(ServerLevel level, ServerPlayer player, VillageGuardEntity guard) {
        if (!canDismiss(level, player, guard)) {
            return;
        }
        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.CONVERSION);
        if (villager == null) {
            player.displayClientMessage(Component.literal("The guard cannot stand down here.").withStyle(ChatFormatting.RED),
                    true);
            return;
        }
        copyPose(guard, villager);
        guard.restoreVillagerOrigin(villager);
        villager.setPersistenceRequired();
        villager.refreshBrain(level);
        level.addFreshEntity(villager);
        guard.discard();
        conversionEffects(level, villager.position());
        player.displayClientMessage(Component.literal("The guard returns to village life.").withStyle(ChatFormatting.YELLOW),
                true);
    }

    private static boolean canPromote(ServerLevel level, ServerPlayer player, Villager villager) {
        if (!villager.isAlive() || villager.isBaby()) {
            player.displayClientMessage(Component.literal("Only an adult villager can take the post.")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (villager.getTradingPlayer() != null) {
            player.displayClientMessage(Component.literal("Let the trade finish before appointing a guard.")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (level.getRaidAt(villager.blockPosition()) != null) {
            player.displayClientMessage(Component.literal("The village is already under alarm.")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    private static boolean canDismiss(ServerLevel level, ServerPlayer player, VillageGuardEntity guard) {
        if (!guard.isAlive() || !guard.hasVillagerOrigin()) {
            player.displayClientMessage(Component.literal("Only a guard appointed by bell can stand down.")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (guard.getTarget() != null) {
            player.displayClientMessage(Component.literal("The guard will not stand down while fighting.")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (level.getRaidAt(guard.blockPosition()) != null) {
            player.displayClientMessage(Component.literal("The guard stays posted until the alarm passes.")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    private static void finishEvent(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void copyPose(net.minecraft.world.entity.Entity from, net.minecraft.world.entity.Entity to) {
        to.setPos(from.getX(), from.getY(), from.getZ());
        to.setYRot(from.getYRot());
        to.setXRot(from.getXRot());
        to.setDeltaMovement(from.getDeltaMovement());
    }

    private static void conversionEffects(ServerLevel level, Vec3 position) {
        level.playSound(null, position.x, position.y, position.z, SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL,
                0.8F, 1.05F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, position.x, position.y + 1.0D, position.z,
                12, 0.45D, 0.45D, 0.45D, 0.01D);
    }
}
