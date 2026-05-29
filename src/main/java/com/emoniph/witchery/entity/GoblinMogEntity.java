package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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
                .add(Attributes.FOLLOW_RANGE,     48.0)
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
        goalSelector.addGoal(4, new RangedBowAttackGoal<GoblinMogEntity>(this, 1.0, 40, 30.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, GoblinMogEntity.class, GoblinGulgEntity.class));
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
        if (ok) playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
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
        if (getInvulTimer() > 0) return false;

        double dist = distToNearestGulg();
        if (dist <= 3.0)  return false;
        if (dist <= 6.0)  amount *= 0.20f;
        else if (dist <= 9.0)  amount *= 0.50f;
        else if (dist <= 16.0) amount *= 0.80f;

        return super.hurtServer(level, source, Math.min(amount, 15.0f));
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

        // Arrows faster when target is falling/airborne (source parity)
        double speed = target.isFallFlying() ? 2.5 : 1.5;
        arrow.shoot(dx * speed, (dy + flatDist) * speed, dz * speed, 1.6F,
                14 - level().getDifficulty().getId() * 4);
        arrow.setBaseDamage(7 + random.nextInt(15));

        playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        level().addFreshEntity(arrow);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4)  attackTimer = 10;
        else super.handleEntityEvent(id);
    }

    // ── Partner distance helper ───────────────────────────────────────────────

    private double distToNearestGulg() {
        AABB bb = getBoundingBox().inflate(16);
        List<GoblinGulgEntity> gulgs = level().getEntitiesOfClass(GoblinGulgEntity.class, bb);
        return gulgs.stream().mapToDouble(this::distanceTo).min().orElse(Double.MAX_VALUE);
    }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        spawnAtLocation(level, new ItemStack(WitcheryItems.KOBOLDITE_NUGGET.get(), 1 + random.nextInt(3)));

        ItemStack armor = switch (random.nextInt(4)) {
            case 0  -> new ItemStack(Items.IRON_HELMET);
            case 1  -> new ItemStack(Items.IRON_CHESTPLATE);
            case 2  -> new ItemStack(Items.IRON_LEGGINGS);
            default -> new ItemStack(Items.IRON_BOOTS);
        };
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
        setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
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
