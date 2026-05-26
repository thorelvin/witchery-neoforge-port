package com.emoniph.witchery.entity;

import com.emoniph.witchery.item.EarmuffsItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Mandrake mob — tiny screaming creature that emerges from harvesting mandrake at night.
 * Chases players and sickens them on contact, matching the old source behavior.
 */
public class MandrakeEntity extends Monster {
    private static final int CONTACT_NAUSEA_TICKS = 300;
    private static final int SCREAM_AMPLIFIER = 1;

    public MandrakeEntity(EntityType<? extends MandrakeEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.65)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof Player player && !level.isClientSide() && !EarmuffsItem.isWorn(player)
                && !player.hasEffect(MobEffects.NAUSEA)) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, CONTACT_NAUSEA_TICKS, SCREAM_AMPLIFIER, false, true));
        }
        setLastHurtMob(target);
        return true;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.GHAST_SCREAM; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.GHAST_SCREAM; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GHAST_DEATH; }
}
