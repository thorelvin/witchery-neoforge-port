package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Demon — large infernal creature summoned through the Infernal ritual circle.
 * Uses melee and throws fireballs at range.
 */
public class DemonEntity extends Monster implements RangedAttackMob {
    private static final int MIN_MELEE_DAMAGE = 7;
    private static final int MELEE_DAMAGE_ROLL = 15;

    public DemonEntity(EntityType<? extends DemonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedBowAttackGoal<DemonEntity>(this, 1.0, 30, 20f));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, 10, true, false,
                (entity, serverLevel) -> !hasDevilsTongueCharm(entity)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (hasDevilsTongueCharm(target) && random.nextDouble() >= 0.05D) {
            playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 0.4F, 0.7F);
            return;
        }
        double dx = target.getX() - this.getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 2.0 - (this.getY() + this.getBbHeight() / 2.0);
        double dz = target.getZ() - this.getZ();
        float spread = (float) Math.sqrt(distanceFactor) * 0.5F;
        Vec3 shot = new Vec3(
                dx + random.nextGaussian() * spread,
                dy,
                dz + random.nextGaussian() * spread);
        Vec3 look = this.getLookAngle();
        LargeFireball fireball = new LargeFireball(level(), this, shot, 1);
        fireball.setPos(getX() + look.x, getY() + getBbHeight() / 2.0 + 0.5, getZ() + look.z);
        level().addFreshEntity(fireball);
        level().playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT,
                net.minecraft.sounds.SoundSource.HOSTILE, 1f, 1f);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof LivingEntity living
                && hasDevilsTongueCharm(living)
                && random.nextDouble() >= 0.05D) {
            playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 0.4F, 0.7F);
            setTarget(null);
            return false;
        }
        target.hurt(damageSources().mobAttack(this), MIN_MELEE_DAMAGE + random.nextInt(MELEE_DAMAGE_ROLL));
        setLastHurtMob(target);
        target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4, 0.0));
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, Math.min(amount, 15.0F));
    }

    @Override public boolean fireImmune() { return true; }
    @Override public boolean isOnFire()   { return false; }

    private static boolean hasDevilsTongueCharm(LivingEntity entity) {
        return isDevilsTongueCharm(entity.getMainHandItem()) || isDevilsTongueCharm(entity.getOffhandItem());
    }

    private static boolean isDevilsTongueCharm(ItemStack stack) {
        return stack.is(WitcheryItems.DEVILS_TONGUE_CHARM.get());
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }
}
