package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Gulg — goblin warrior boss. Source parity: EntityGoblinGulg 1.7.10-0.24.1.
 *
 * Damage resistance scales with distance to nearest GoblinMogEntity:
 *   ≤3 blocks  → immune
 *   ≤6 blocks  → 20% damage taken
 *   ≤9 blocks  → 50% damage taken
 *   ≤16 blocks → 80% damage taken
 *   >16 blocks → full damage (capped at 15 per hit)
 *
 * Melee attack power is also stronger when close to Mog.
 * Invulnerability phase: when invulTimer > 0 heals rapidly (20 HP/10 ticks).
 * Teleports toward target every 200 ticks when chasing.
 */
public class GoblinGulgEntity extends Monster {

    private static final EntityDataAccessor<Integer> INVUL_TIMER =
            SynchedEntityData.defineId(GoblinGulgEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.witchery.goblin_gulg"),
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.PROGRESS);

    private int attackTimer = 0;
    private long ticksSinceTeleport = 0L;

    public GoblinGulgEntity(EntityType<? extends GoblinGulgEntity> type, Level level) {
        super(type, level);
        this.xpReward = 35;
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,     400.0)
                .add(Attributes.MOVEMENT_SPEED,   0.35)
                .add(Attributes.ATTACK_DAMAGE,     6.0)
                .add(Attributes.FOLLOW_RANGE,     48.0)
                .add(Attributes.ARMOR,             8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
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
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        // Priority 3: seek nearest Mog to stay close (source: EntityAIMoveTowardsEntityClass)
        goalSelector.addGoal(3, seekMogGoal());
        // Priority 4: move toward current combat target
        goalSelector.addGoal(4, new MoveTowardsTargetGoal(this, 1.0, 48.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, GoblinMogEntity.class, GoblinGulgEntity.class));
        // Priority 1 (same as HurtBy) — both target selectors active simultaneously (source parity)
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    /** Moves toward nearest GoblinMog within 64 blocks, stops when within 6 blocks. */
    private Goal seekMogGoal() {
        return new Goal() {
            private GoblinMogEntity targetMog;

            @Override
            public boolean canUse() {
                if (getTarget() != null) return false; // combat takes priority
                AABB search = getBoundingBox().inflate(64, 16, 64);
                java.util.List<GoblinMogEntity> mogs = level().getEntitiesOfClass(GoblinMogEntity.class, search);
                if (mogs.isEmpty()) return false;
                targetMog = mogs.stream().min(java.util.Comparator.comparingDouble(GoblinGulgEntity.this::distanceTo)).orElse(null);
                return targetMog != null && distanceTo(targetMog) > 6.0;
            }

            @Override
            public boolean canContinueToUse() {
                return targetMog != null && targetMog.isAlive() && distanceTo(targetMog) > 6.0 && getTarget() == null;
            }

            @Override
            public void start() {
                setFlags(java.util.EnumSet.of(Flag.MOVE));
            }

            @Override
            public void tick() {
                if (targetMog != null) {
                    getNavigation().moveTo(targetMog, 1.0);
                }
            }

            @Override
            public void stop() {
                getNavigation().stop();
                targetMog = null;
            }
        };
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
            // Invulnerability + rapid heal phase
            setInvulTimer(invul - 1);
            if (tickCount % 10 == 0) heal(20.0F);
            if (invul == 1) level().broadcastEntityEvent(this, (byte) 10);
        } else {
            // Normal passive regen
            if (tickCount % 20 == 0) heal(2.0F);
            // Teleport toward target when chasing
            LivingEntity target = getTarget();
            if (target != null && navigation.isStuck() &&
                    (long) tickCount - ticksSinceTeleport > 200L) {
                ticksSinceTeleport = tickCount;
                teleportToward(target);
            }
        }
    }

    private void teleportToward(Entity target) {
        Vec3 dir = new Vec3(getX() - target.getX(), 0, getZ() - target.getZ()).normalize();
        double range = 16.0;
        double nx = getX() + (random.nextDouble() - 0.5) * 8 - dir.x * range;
        double ny = getY() + (random.nextInt(16) - 8);
        double nz = getZ() + (random.nextDouble() - 0.5) * 8 - dir.z * range;
        boolean ok = tryTeleportTo(nx, ny, nz);
        if (ok) {
            playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    private boolean tryTeleportTo(double x, double y, double z) {
        double ox = getX(), oy = getY(), oz = getZ();
        setPos(x, y, z);
        if (!level().noCollision(this)) { setPos(ox, oy, oz); return false; }
        return true;
    }

    // ── Damage — partner-distance scaling ─────────────────────────────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (getInvulTimer() > 0) return false; // Invulnerability phase

        double dist = distToNearestMog();
        if (dist <= 3.0)  return false;        // Immune when Mog adjacent
        if (dist <= 6.0)  amount *= 0.20f;
        else if (dist <= 9.0)  amount *= 0.50f;
        else if (dist <= 16.0) amount *= 0.80f;

        return super.hurtServer(level, source, Math.min(amount, 15.0f));
    }

    // ── Melee — attack scales with distance to Mog ────────────────────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        attackTimer = 10;
        level.broadcastEntityEvent(this, (byte) 4);

        double dist = distToNearestMog();
        int force = dist <= 3.0 ? 20 : dist <= 6.0 ? 15 : dist <= 9.0 ? 10 : dist <= 16.0 ? 6 : 4;
        float baseDmg = 6 + random.nextInt(force);
        double yBoost  = dist <= 3.0 ? 1.0 : dist <= 6.0 ? 0.8 : dist <= 9.0 ? 0.5 : dist <= 16.0 ? 0.2 : 0;

        boolean hit = false;
        if (target instanceof net.minecraft.world.entity.LivingEntity lt) {
            hit = lt.hurtServer(level, level.damageSources().mobAttack(this), baseDmg);
        }
        if (hit && yBoost > 0 && target instanceof LivingEntity le) {
            le.setDeltaMovement(le.getDeltaMovement().add(0, yBoost, 0));
        }
        playSound(SoundEvents.IRON_GOLEM_HURT, 1.0f, 1.0f);
        return hit;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4)  attackTimer = 10;
        else super.handleEntityEvent(id);
    }

    // ── Partner distance helper ───────────────────────────────────────────────

    private double distToNearestMog() {
        AABB bb = getBoundingBox().inflate(16);
        List<GoblinMogEntity> mogs = level().getEntitiesOfClass(GoblinMogEntity.class, bb);
        return mogs.stream().mapToDouble(this::distanceTo).min().orElse(Double.MAX_VALUE);
    }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // Koboldite nuggets
        spawnAtLocation(level, new ItemStack(WitcheryItems.KOBOLDITE_NUGGET.get(), 1 + random.nextInt(3)));

        // Random enchanted iron armor
        ItemStack armor = switch (random.nextInt(4)) {
            case 0  -> new ItemStack(Items.IRON_HELMET);
            case 1  -> new ItemStack(Items.IRON_CHESTPLATE);
            case 2  -> new ItemStack(Items.IRON_LEGGINGS);
            default -> new ItemStack(Items.IRON_BOOTS);
        };
        // Apply basic enchantment via enchanting helper
        net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(
                random, armor, 30, level.registryAccess(), java.util.Optional.empty());
        spawnAtLocation(level, armor);

        // 50% chance: source-facing Gulg's Gurdle reward.
        if (random.nextInt(2) == 0) {
            spawnAtLocation(level, new ItemStack(WitcheryItems.GULGS_GRUDLE.get()));
        }
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }

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
