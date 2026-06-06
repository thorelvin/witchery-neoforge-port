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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
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
    public static final int QUEASY_EXPIRY_DURATION_TICKS = 1800;
    public static final double SOURCE_SPEED_MODIFIER_BASE = -40.0D;
    public static final double PINNED_Y_VELOCITY = -0.2D;
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
