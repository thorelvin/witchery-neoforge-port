package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Shade of Leonard — neutral boss, becomes hostile when attacked.
 * Spawns Lost Souls at 300 HP; immune while Lost Souls are alive nearby.
 * Applies Wither II on melee hit (Mortal Coil approximation — replace with
 * a custom MortalCoil MobEffect when that system is ready).
 */
public class LeonardEntity extends Monster {

    private static final float SOULS_THRESHOLD = 300.0F;
    private static final int   SOUL_COUNT = 6; // 2 green, 2 red, 2 black
    private static final float SOUL_RADIUS = 20.0F;
    private static final float SOURCE_DEMONBANE_HEALTH_GATE = 0.4F;
    private static final float SOURCE_DEMONBANE_DAMAGE_CAP = 15.0F;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.witchery.leonard"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    private boolean soulsSpawned = false;

    public LeonardEntity(EntityType<? extends LeonardEntity> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 12.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        // Neutral: only retaliates when attacked
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    // ── Boss bar ──────────────────────────────────────────────────────────────

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

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            bossEvent.setProgress(getHealth() / getMaxHealth());
        }
    }

    // ── Damage ────────────────────────────────────────────────────────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Immune while Lost Souls are alive nearby
        AABB searchBox = getBoundingBox().inflate(SOUL_RADIUS, SOUL_RADIUS * 0.5, SOUL_RADIUS);
        List<LostSoulEntity> souls = level.getEntitiesOfClass(LostSoulEntity.class, searchBox);
        if (!souls.isEmpty()) {
            return false;
        }

        boolean hurt = super.hurtServer(level, source, amount);

        // Spawn Lost Souls at the 300 HP threshold
        if (hurt && !soulsSpawned && getHealth() <= SOULS_THRESHOLD) {
            soulsSpawned = true;
            spawnLostSouls(level);
        }
        return hurt;
    }

    public boolean hurtFromDemonbaneWeakness(ServerLevel level, int damage) {
        if (damage <= 0 || !sourceDemonbaneWeaknessCanHurt(getHealth(), getMaxHealth())) {
            return false;
        }
        return super.hurtServer(level, level.damageSources().magic(),
                Math.min((float) damage, sourceDemonbaneWeaknessDamageCap(false)));
    }

    public static boolean sourceDemonbaneWeaknessCanHurt(float health, float maxHealth) {
        return health < maxHealth * SOURCE_DEMONBANE_HEALTH_GATE;
    }

    public static float sourceDemonbaneWeaknessDamageCap(boolean largeResizing) {
        return largeResizing ? 8.0F : SOURCE_DEMONBANE_DAMAGE_CAP;
    }

    private void spawnLostSouls(ServerLevel level) {
        int[] types = {
            LostSoulEntity.GREEN, LostSoulEntity.GREEN,
            LostSoulEntity.RED,   LostSoulEntity.RED,
            LostSoulEntity.BLACK, LostSoulEntity.BLACK
        };
        for (int type : types) {
            LostSoulEntity soul = WitcheryEntities.LOST_SOUL.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (soul == null) continue;
            soul.setSoulType(type);
            double angle  = random.nextDouble() * Math.PI * 2;
            double radius = 2.0 + random.nextDouble() * 3.0;
            soul.setPos(getX() + Math.cos(angle) * radius, getY() + 1.0, getZ() + Math.sin(angle) * radius);
            soul.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
            LivingEntity target = getTarget();
            if (target != null) soul.setTarget(target);
            level.addFreshEntity(soul);
        }
    }

    // ── Melee — Mortal Coil (Wither II approximation) ────────────────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            // Mortal Coil: Wither II for 10 seconds. Replace with custom MobEffect later.
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
        }
        return hit;
    }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        spawnAtLocation(level, new ItemStack(WitcheryItems.LEONARDS_URN.get()));
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WITHER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SoulsSpawned", soulsSpawned);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        soulsSpawned = tag.getBooleanOr("SoulsSpawned", false);
    }
}
