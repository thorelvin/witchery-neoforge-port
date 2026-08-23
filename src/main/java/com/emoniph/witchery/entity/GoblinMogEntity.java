package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Mog — goblin archer boss. Source parity: EntityGoblinMog 1.7.10-0.24.1.
 *
 * Mirror of Gulg but ranged: damage resistance checks distance to nearest
 * GoblinGulgEntity instead. Heals passively (1 HP/20 ticks). Randomly
 * self-equips a bow if unarmed. Arrows travel faster when target is airborne.
 */
public class GoblinMogEntity extends Monster implements RangedAttackMob {

    private static final EntityDataAccessor<Integer> INVUL_TIMER =
            SynchedEntityData.defineId(GoblinMogEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.witchery.goblin_mog"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    private int attackTimer = 0;
    private long ticksSinceTeleport = 0L;

    public GoblinMogEntity(EntityType<? extends GoblinMogEntity> type, Level level) {
        super(type, level);
        this.xpReward = 35;
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,     400.0)
                .add(Attributes.MOVEMENT_SPEED,   0.35)
                .add(Attributes.ATTACK_DAMAGE,     7.0)
                .add(Attributes.FOLLOW_RANGE,     50.0)
                .add(Attributes.ARMOR,              5.0);
    }

    // ── Synced data ───────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(INVUL_TIMER, 0);
    }

    public int getInvulTimer() { return entityData.get(INVUL_TIMER); }
    public void setInvulTimer(int t) { entityData.set(INVUL_TIMER, t); }
    public int getAttackTimer() { return attackTimer; }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(4, new RangedBowAttackGoal<GoblinMogEntity>(this, 1.0, 40, 30.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this,
                HobgoblinEntity.class, GoblinMogEntity.class, GoblinGulgEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    // ── Boss bar ──────────────────────────────────────────────────────────────

    @Override
    public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossEvent.addPlayer(player); }
    @Override
    public void stopSeenByPlayer(ServerPlayer player)  { super.stopSeenByPlayer(player); bossEvent.removePlayer(player); }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (attackTimer > 0) attackTimer--;
        if (level().isClientSide()) return;

        bossEvent.setProgress(getHealth() / getMaxHealth());

        int invul = getInvulTimer();
        if (invul > 0) {
            setInvulTimer(invul - 1);
            if (tickCount % 10 == 0) heal(20.0F);
        } else {
            // Passive regen
            if (tickCount % 20 == 0) heal(1.0F);

            // Randomly self-equip a bow if unarmed
            if (getMainHandItem().isEmpty() && random.nextInt(100) == 0) {
                setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
            }

            // Teleport toward target
            LivingEntity target = getTarget();
            if (target != null && navigation.isStuck() &&
                    (long) tickCount - ticksSinceTeleport > 300L) {
                ticksSinceTeleport = tickCount;
                GoblinBossTeleportHelper.teleportToward(this, target);
            }
        }
    }

    // ── Damage — partner-distance scaling ─────────────────────────────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (getInvulTimer() > 0) return false;

        float scaled = GoblinBossContracts.scaledAndCappedDamage(amount, distanceSqrToNearestGulg());
        return scaled > 0.0F && super.hurtServer(level, source, scaled);
    }

    // ── Ranged attack ─────────────────────────────────────────────────────────

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (getMainHandItem().isEmpty()) return;

        attackTimer = 10;
        level().broadcastEntityEvent(this, (byte) 4);

        ItemStack arrowStack = new ItemStack(Items.ARROW);
        var arrow = ProjectileUtil.getMobArrow(this, arrowStack, power, getMainHandItem());
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - getZ();
        double flatDist = Math.sqrt(dx * dx + dz * dz) * 0.2;

        double speedFactor = GoblinBossContracts.mogArrowSpeedFactor(!target.onGround());
        arrow.shoot(dx, dy + flatDist, dz, (float) (1.6F * speedFactor),
                14 - level().getDifficulty().getId() * 4);
        arrow.setBaseDamage(GoblinBossContracts.mogArrowDamage(
                power, random.nextGaussian(), level().getDifficulty().getId()));

        playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4)  attackTimer = 10;
        else super.handleEntityEvent(id);
    }

    // ── Partner distance helper ───────────────────────────────────────────────

    private double distanceSqrToNearestGulg() {
        AABB bb = getBoundingBox().inflate(16);
        List<GoblinGulgEntity> gulgs = level().getEntitiesOfClass(GoblinGulgEntity.class, bb);
        return gulgs.stream().mapToDouble(this::distanceToSqr).min().orElse(Double.MAX_VALUE);
    }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        spawnAtLocation(level, new ItemStack(WitcheryItems.KOBOLDITE_NUGGET.get(), 1 + random.nextInt(3)));

        ItemStack armor = new ItemStack(GoblinBossContracts.armorDrop(random.nextInt(4)));
        net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(
                random, armor, 30, level.registryAccess(), java.util.Optional.empty());
        spawnAtLocation(level, armor);

        if (random.nextInt(2) == 0) {
            spawnAtLocation(level, new ItemStack(WitcheryItems.MOGS_QUIVER.get()));
        }
    }

    // ── Spawn init — equip bow ────────────────────────────────────────────────

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        EntitySpawnReason spawnType,
                                        net.minecraft.world.entity.@org.jetbrains.annotations.Nullable SpawnGroupData spawnGroupData) {
        setPersistenceRequired();
        setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && GoblinBossContracts.isGoblinKin(target)) return;
        super.setTarget(target);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (!state.is(Blocks.COBWEB)) super.makeStuckInBlock(state, motionMultiplier);
    }

    @Override
    public ItemEntity spawnAtLocation(ServerLevel level, ItemStack stack, float yOffset) {
        ItemEntity item = super.spawnAtLocation(level, stack, yOffset);
        if (item != null && stack.is(Items.BOW)) {
            item.setPickUpDelay(100);
            item.lifespan = 100;
        }
        return item;
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return WitcherySoundEvents.MOB_GOBLIN_MOG_IDLE.get(); }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.ZOMBIE_HORSE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Invul", getInvulTimer());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setInvulTimer(tag.getIntOr("Invul", 0));
    }
}
