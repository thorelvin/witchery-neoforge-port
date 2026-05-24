package com.emoniph.witchery.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;

import java.util.UUID;

/** Spectre — fast flying undead that passes through walls. Fire-immune. */
public class SpectreEntity extends Monster {
    private static final float MELEE_HEALTH_FRACTION = 0.15F;
    private UUID ownerUuid;

    public SpectreEntity(EntityType<? extends SpectreEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1f);
        this.setPathfindingMalus(PathType.WATER, -1f);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        
        return nav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<net.minecraft.world.entity.player.Player>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (target instanceof LivingEntity living) {
            damage = Math.max(living.getMaxHealth() * MELEE_HEALTH_FRACTION, 1.0F);
        }
        target.hurt(damageSources().magic(), damage);
        setLastHurtMob(target);
        return true;
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
    }

    private boolean isOwner(LivingEntity entity) {
        return ownerUuid != null && entity != null && ownerUuid.equals(entity.getUUID());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && getTarget() != null && isOwner(getTarget())) {
            setTarget(null);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isOwner(target) && super.canAttack(target);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) {
            tag.putString("OwnerUUID", ownerUuid.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String ownerStr = tag.getStringOr("OwnerUUID", "");
        ownerUuid = ownerStr.isEmpty() ? null : UUID.fromString(ownerStr);
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean isOnFire()   { return false; }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VEX_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.VEX_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VEX_DEATH; }
}
