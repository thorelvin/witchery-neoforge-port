package com.emoniph.witchery.effect;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.entity.DeathEntity;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import com.emoniph.witchery.entity.GoblinMogEntity;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Source 1.7.10 Paralysed: a harmful, incurable movement lock. The old source
 * conditionally applies the speed modifier, so this port manages it per entity.
 */
public class ParalyzedMobEffect extends MobEffect {
    public static final int BREW_DURATION_TICKS = 200;
    public static final int PLAYER_REQUIRED_AMPLIFIER = 2;
    public static final int VILLAGER_ESCAPE_AMPLIFIER = 5;
    public static final int DAMAGE_BREAK_AMPLIFIER = 4;
    public static final int QUEASY_EXPIRY_DURATION_TICKS = QueasyMobEffect.PARALYSED_EXPIRY_DURATION_TICKS;
    public static final double SOURCE_SPEED_MODIFIER_BASE = -40.0D;
    public static final double PINNED_Y_VELOCITY = -0.2D;
    public static final double VAMPIRE_ATTRACTION_SPEED = 0.8D;
    public static final double VAMPIRE_ATTRACTION_MIN_DISTANCE = 1.0D;
    public static final double VAMPIRE_ATTRACTION_MAX_DISTANCE = 16.0D;
    public static final int VAMPIRE_ATTRACTION_REPATH_TICKS = 20;
    private static final Identifier SPEED_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "effect.paralysed");

    public ParalyzedMobEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        syncSourceSpeedModifier(entity, amplifier);
        if (!canApplyToEntity(entity, amplifier)) {
            directSourceVillagerTowardVampire(level, entity, amplifier);
            return true;
        }

        if (entity instanceof Creeper creeper) {
            creeper.setSwellDir(-1);
        }
        if (shouldPinVertical(entity.tickCount, isSourceVillager(entity), amplifier)) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, PINNED_Y_VELOCITY, motion.z);
            entity.hurtMarked = true;
        }
        return true;
    }

    public static void syncSourceSpeedModifier(LivingEntity entity, int amplifier) {
        if (!canApplyToEntity(entity, amplifier)) {
            removeSourceSpeedModifier(entity);
            return;
        }

        AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.addOrUpdateTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID,
                    sourceSpeedModifierAmount(amplifier), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void removeSourceSpeedModifier(LivingEntity entity) {
        AttributeInstance movement = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    public static boolean canApplyToEntity(LivingEntity entity, int amplifier) {
        if (isSourceBoss(entity)) {
            return false;
        }
        if (amplifier >= VILLAGER_ESCAPE_AMPLIFIER && isSourceVillager(entity)) {
            return false;
        }
        return !(entity instanceof Player) || amplifier >= PLAYER_REQUIRED_AMPLIFIER;
    }

    public static boolean shouldPinVertical(int tickCount, boolean villager, int amplifier) {
        return tickCount % 20 != 2 || !villager || amplifier < VILLAGER_ESCAPE_AMPLIFIER;
    }

    public static boolean shouldBreakOnHurt(boolean clientSide, int amplifier, float damage) {
        return !clientSide && amplifier >= DAMAGE_BREAK_AMPLIFIER && damage >= 1.0F;
    }

    public static boolean shouldApplyQueasyOnExpiry(boolean clientSide, boolean player, int amplifier) {
        return !clientSide && player && amplifier >= DAMAGE_BREAK_AMPLIFIER;
    }

    public static boolean directSourceVillagerTowardVampire(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!shouldDirectVillagerToVampire(isSourceVillager(entity), amplifier) || !(entity instanceof Mob mob)) {
            return false;
        }
        ServerPlayer vampire = findNearestSourceVampire(level, entity);
        if (vampire == null || !sourceVampireAttractionDistanceAllowed(entity.distanceToSqr(vampire))) {
            return false;
        }

        mob.setTarget(null);
        if (entity.tickCount % VAMPIRE_ATTRACTION_REPATH_TICKS == 0) {
            mob.getNavigation().moveTo(vampire.getX(), vampire.getY(), vampire.getZ(), VAMPIRE_ATTRACTION_SPEED);
        }
        return true;
    }

    private static ServerPlayer findNearestSourceVampire(ServerLevel level, LivingEntity entity) {
        AABB bounds = entity.getBoundingBox().inflate(VAMPIRE_ATTRACTION_MAX_DISTANCE);
        ServerPlayer closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, bounds)) {
            var data = WitcheryAttachments.get(player);
            if (!sourceVampireAttractionPlayer(data.isVampire(), data.getVampireLevel())) {
                continue;
            }
            double distance = entity.distanceToSqr(player);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = player;
            }
        }
        return closest;
    }

    public static boolean shouldDirectVillagerToVampire(boolean villager, int amplifier) {
        return villager && amplifier >= VILLAGER_ESCAPE_AMPLIFIER;
    }

    public static boolean sourceVampireAttractionPlayer(boolean vampire, int vampireLevel) {
        return vampire && vampireLevel >= 8;
    }

    public static boolean sourceVampireAttractionDistanceAllowed(double distanceSq) {
        double min = VAMPIRE_ATTRACTION_MIN_DISTANCE * VAMPIRE_ATTRACTION_MIN_DISTANCE;
        double max = VAMPIRE_ATTRACTION_MAX_DISTANCE * VAMPIRE_ATTRACTION_MAX_DISTANCE;
        return distanceSq >= min && distanceSq <= max;
    }

    public static double sourceSpeedModifierAmount(int amplifier) {
        return SOURCE_SPEED_MODIFIER_BASE * (amplifier + 1);
    }

    public static boolean isSourceVillager(LivingEntity entity) {
        return entity instanceof Villager || entity instanceof VillageGuardEntity;
    }

    private static boolean isSourceBoss(LivingEntity entity) {
        return entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof BabaYagaEntity
                || entity instanceof DeathEntity
                || entity instanceof GoblinGulgEntity
                || entity instanceof GoblinMogEntity
                || entity instanceof HornedHuntsmanEntity
                || entity instanceof LeonardEntity
                || entity instanceof LordOfTormentEntity;
    }
}
