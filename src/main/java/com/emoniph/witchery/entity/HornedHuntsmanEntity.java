package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HornedHuntsmanEntity extends Monster implements RangedAttackMob {
    private static final int MIN_MELEE_DAMAGE = 7;
    private static final int MELEE_DAMAGE_ROLL = 15;
    private static final float ARROW_VELOCITY = 1.6F;
    private static final float ARROW_INACCURACY_BASE = 14.0F;
    private static final double ARROW_DAMAGE_SCALE = 8.0D;
    private static final double ARROW_DAMAGE_GAUSSIAN = 0.25D;
    private static final double ARROW_DIFFICULTY_BONUS = 0.11D;

    private static final EntityDataAccessor<Boolean> PLAYER_CREATED =
            SynchedEntityData.defineId(HornedHuntsmanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> INVULNERABLE_TICKS =
            SynchedEntityData.defineId(HornedHuntsmanEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.witchery.horned_huntsman"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.PROGRESS);

    private int attackTimer;
    private long ticksSinceTeleport;

    public HornedHuntsmanEntity(EntityType<? extends HornedHuntsmanEntity> type, Level level) {
        super(type, level);
        this.xpReward = 70;
        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WitcheryItems.HUNTSMAN_SPEAR.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PLAYER_CREATED, false);
        builder.define(INVULNERABLE_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, 20, 60, 30.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (attackTimer > 0) {
            attackTimer--;
        }
        if (level().isClientSide()) {
            return;
        }

        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        int invulnerableTicks = getInvulnerableTicks();
        if (invulnerableTicks > 0) {
            invulnerableTicks--;
            setInvulnerableTicks(invulnerableTicks);
            if (invulnerableTicks == 0) {
                level().levelEvent(1013, blockPosition(), 0);
            }
            if (tickCount % 10 == 0) {
                heal(20.0F);
            }
            return;
        }

        if (tickCount % 20 == 0) {
            heal(1.0F);
        }

        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (tickCount % (200 + random.nextInt(4) * 100) == 0
                && distanceToSqr(target) <= 256.0
                && getSensing().hasLineOfSight(target)) {
            summonWolf(target);
        }
        if (getNavigation().isStuck() && tickCount - ticksSinceTeleport > 100) {
            ticksSinceTeleport = tickCount;
            teleportNear(target);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        var projectile = ProjectileUtil.getMobArrow(this, arrowStack, distanceFactor, this.getMainHandItem());
        projectile.setBaseDamage(distanceFactor * ARROW_DAMAGE_SCALE
                + random.nextGaussian() * ARROW_DAMAGE_GAUSSIAN
                + level().getDifficulty().getId() * ARROW_DIFFICULTY_BONUS);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333) - projectile.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz) * 0.2;
        projectile.shoot(dx, dy + horizontal, dz, ARROW_VELOCITY,
                ARROW_INACCURACY_BASE - this.level().getDifficulty().getId() * 4);
        playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (random.nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(projectile);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        attackTimer = 10;
        DamageSource source = damageSources().mobAttack(this);
        target.hurt(source, MIN_MELEE_DAMAGE + random.nextInt(MELEE_DAMAGE_ROLL));
        setLastHurtMob(target);
        target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4, 0.0));
        playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, Math.min(amount, 15.0F));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(level, new ItemStack(Items.WITHER_SKELETON_SKULL, random.nextInt(3) == 0 ? 3 : 2));
        this.spawnAtLocation(level, new ItemStack(Items.ENCHANTED_BOOK));
        this.spawnAtLocation(level, new ItemStack(WitcheryItems.DEMONIC_BLOOD.get()));
        if (random.nextInt(4) == 0) {
            this.spawnAtLocation(level, new ItemStack(WitcheryItems.HUNTSMAN_SPEAR.get()));
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PlayerCreated", isPlayerCreated());
        tag.putInt("Invul", getInvulnerableTicks());
        tag.putLong("TeleTicks", ticksSinceTeleport);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setPlayerCreated(tag.getBooleanOr("PlayerCreated", false));
        setInvulnerableTicks(tag.getIntOr("Invul", 0));
        ticksSinceTeleport = tag.getLongOr("TeleTicks", 0L);
    }

    public boolean isPlayerCreated() {
        return entityData.get(PLAYER_CREATED);
    }

    public void setPlayerCreated(boolean playerCreated) {
        setPersistenceRequired();
        entityData.set(PLAYER_CREATED, playerCreated);
    }

    public int getInvulnerableTicks() {
        return entityData.get(INVULNERABLE_TICKS);
    }

    public void setInvulnerableTicks(int ticks) {
        entityData.set(INVULNERABLE_TICKS, Math.max(0, ticks));
    }

    public int getAttackTimer() {
        return attackTimer;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENDER_DRAGON_GROWL; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ZOMBIE_HORSE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }

    private void summonWolf(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Wolf wolf = EntityType.WOLF.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (wolf == null) {
            return;
        }
        wolf.setPos(getX() - 0.5 + random.nextDouble(), getY(), getZ() - 0.5 + random.nextDouble());
        wolf.setYRot(yBodyRot);
        wolf.setXRot(getXRot());
        wolf.setTarget(target);
        wolf.addEffect(new MobEffectInstance(MobEffects.SPEED, 20000, 1));
        serverLevel.addFreshEntity(wolf);
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                wolf.getX(), wolf.getY() + 0.5, wolf.getZ(),
                10, 0.4, 0.4, 0.4, 0.0);
        serverLevel.playSound(null, wolf.blockPosition(), SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 2.0F);
    }

    private void teleportNear(LivingEntity target) {
        Vec3 away = new Vec3(
                this.getX() - target.getX(),
                this.getBoundingBox().minY + this.getBbHeight() * 0.5F - target.getY() + target.getEyeHeight(),
                this.getZ() - target.getZ()).normalize();
        double x = this.getX() + (random.nextDouble() - 0.5) * 8.0 - away.x * 8.0;
        double y = this.getY() + random.nextInt(16) - 8 - away.y * 8.0;
        double z = this.getZ() + (random.nextDouble() - 0.5) * 8.0 - away.z * 8.0;
        if (randomTeleport(x, y, z, true)) {
            level().playSound(null, xo, yo, zo, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1.0F, 1.0F);
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
    }
}
