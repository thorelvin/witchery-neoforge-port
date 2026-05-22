package com.emoniph.witchery.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.Level;

/**
 * Ent - ancient tree spirit. Passive until attacked; then fights back hard.
 */
public class EntEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_ATTACK_TIMER =
            SynchedEntityData.defineId(EntEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SCREAMING =
            SynchedEntityData.defineId(EntEntity.class, EntityDataSerializers.BOOLEAN);

    public EntEntity(EntityType<? extends EntEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK_TIMER, 0);
        builder.define(DATA_SCREAMING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }

        int attackTimer = getAttackTimer();
        if (attackTimer > 0) {
            setAttackTimer(attackTimer - 1);
        }
        setScreaming(getTarget() != null);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        setAttackTimer(10);
        boolean result = super.doHurtTarget(level, target);
        if (result) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4, 0.0));
        }
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        return result;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && living.getMainHandItem().getItem() instanceof AxeItem) {
            amount *= 3.0F;
        }
        return super.hurtServer(level, source, Math.min(amount, 15.0F));
    }

    public int getAttackTimer() {
        return entityData.get(DATA_ATTACK_TIMER);
    }

    private void setAttackTimer(int attackTimer) {
        entityData.set(DATA_ATTACK_TIMER, Math.max(0, attackTimer));
    }

    public boolean isScreaming() {
        return entityData.get(DATA_SCREAMING);
    }

    private void setScreaming(boolean screaming) {
        entityData.set(DATA_SCREAMING, screaming);
    }

    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.WOOD_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WOOD_BREAK; }
}
