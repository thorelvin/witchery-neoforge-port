package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.item.EarmuffsItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
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

/**
 * Banshee — flying undead horror that screams near dying players.
 * Now restores a first practical scream-damage loop with Earmuffs protection.
 */
public class BansheeEntity extends Monster {
    private static final int SCREAM_START_INTERVAL_TICKS = 100;
    private static final int SCREAM_REPEAT_INTERVAL_TICKS = 20;
    private static final int SCREAM_SOUND_INTERVAL_TICKS = 60;
    private static final double SCREAM_RADIUS = 6.0;
    private static final float MIN_SCREAM_DAMAGE = 1.0F;
    private static final float SCREAM_DAMAGE_RATIO = 0.1F;

    private UUID ownerUuid;
    private boolean screaming;

    public BansheeEntity(EntityType<? extends BansheeEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1f);
        this.setPathfindingMalus(PathType.WATER, -1f);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        
        return nav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FLYING_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8f));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<net.minecraft.world.entity.player.Player>(this, Player.class, true));
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

        if (level().isClientSide()) {
            return;
        }

        int screamInterval = screaming ? SCREAM_REPEAT_INTERVAL_TICKS : SCREAM_START_INTERVAL_TICKS;
        if (tickCount % screamInterval != 0) {
            return;
        }

        boolean startedScreaming = false;
        boolean foundVictim = false;
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(SCREAM_RADIUS),
                LivingEntity::isAlive)) {
            if (isOwner(player) || distanceToSqr(player) > SCREAM_RADIUS * SCREAM_RADIUS) {
                continue;
            }

            foundVictim = true;
            if (!screaming) {
                screaming = true;
                startedScreaming = true;
            }

            if (!EarmuffsItem.isWorn(player)) {
                float damage = Math.max(player.getMaxHealth() * SCREAM_DAMAGE_RATIO, MIN_SCREAM_DAMAGE);
                player.hurt(player.damageSources().magic(), damage);
            }
        }

        if (!foundVictim) {
            screaming = false;
            return;
        }

        if (startedScreaming || tickCount % SCREAM_SOUND_INTERVAL_TICKS == 0) {
            level().playSound(null, blockPosition(), WitcherySoundEvents.ENTITY_BANSHEE_SCREAM.get(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0f, 1.0f + random.nextFloat() * 0.3f);
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

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.PHANTOM_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.PHANTOM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PHANTOM_DEATH; }
    @Override public boolean isOnFire() { return false; }
}
