package com.emoniph.witchery.entity;

import com.emoniph.witchery.events.SilverEvents;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Wolfman — the werewolf mob form. Spawns from WereVillagerEntity on full-moon
 * nights, or via Wolftrap / infection.
 *
 * Source parity: EntityWolfman 1.7.10-0.24.1
 *   - 40 HP, 0.4 speed, 5.0 attack damage, 80 follow range
 *   - Targets players, villagers, witch hunters
 *   - Silver weapons deal 1.5× damage (max 15); all other damage capped at 1.0
 *   - Every 100 ticks when not full-moon night: reverts to WereVillagerEntity
 *   - Retains the complete villager snapshot and infectious flag through transformations
 */
public class WolfmanEntity extends Monster {

    private boolean infectious   = false; // biting spreads lycanthropy
    private int attackTimer      = 0;

    public WolfmanEntity(EntityType<? extends WolfmanEntity> type, Level level) {
        super(type, level);
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,   40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE,  5.0)
                .add(Attributes.FOLLOW_RANGE,  80.0);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BreakDoorGoal(this, diff -> true));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, false));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, WitchHunterEntity.class, false));
    }

    // ── Tick — moon check + attack timer ─────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (attackTimer > 0) attackTimer--;
        if (!level().isClientSide()) {
            if (tickCount % 100 == 3 && VillagerWerewolfConversion.hasVillagerSnapshot(this) && !isFullMoonNight()
                    && !hasEffect(WitcheryMobEffects.WOLFSBANE)) {
                convertToWereVillager();
            } else if (tickCount % 40 == 4 && hasEffect(MobEffects.WEAKNESS)) {
                removeEffect(MobEffects.WEAKNESS);
            }
        }
    }

    // ── Damage ────────────────────────────────────────────────────────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Silver wounds freely; everything else is barely a scratch
        if (isSilverDamage(source)) {
            return super.hurtServer(level, source, Math.min(amount * 1.5f, 15.0f));
        }
        return super.hurtServer(level, source, Math.min(amount, 1.0f));
    }

    private boolean isSilverDamage(DamageSource source) {
        return SilverEvents.isSilverSource(source);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        attackTimer = 10;
        level.broadcastEntityEvent(this, (byte) 4);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) attackTimer = 10;
        else super.handleEntityEvent(id);
    }

    public int getAttackTimer() { return attackTimer; }

    // ── Moon reversion ────────────────────────────────────────────────────────

    private boolean isFullMoonNight() {
        if (!(level() instanceof ServerLevel sl)) return false;
        long dayTime = sl.getDayTime() % 24000L;
        return (sl.getDayTime() / 24000L) % 8L == 0 && dayTime > 13000L && dayTime < 23000L;
    }

    private void convertToWereVillager() {
        if (!(level() instanceof ServerLevel sl)) return;
        VillagerWerewolfConversion.restoreWereVillager(sl, this);
    }

    // ── Profession / infection tracking ──────────────────────────────────────

    public boolean isInfectious() { return infectious; }
    public void setInfectious() { this.infectious = true; }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        switch (random.nextInt(3)) {
            case 0 -> spawnAtLocation(level, new ItemStack(WitcheryItems.SILVER_DEPOSITS.get(),
                                          1 + random.nextInt(2)));
            case 1 -> spawnAtLocation(level, new ItemStack(Items.ROTTEN_FLESH));
            case 2 -> spawnAtLocation(level, new ItemStack(Items.BONE));
        }
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(20) == 0
                ? WitcherySoundEvents.MOB_WOLFMAN_HOWL.get()
                : WitcherySoundEvents.MOB_WOLFMAN_SAY.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return super.getAmbientSoundInterval() * 4;
    }

    @Override protected SoundEvent getHurtSound(DamageSource src) { return WitcherySoundEvents.MOB_WOLFMAN_HIT.get(); }
    @Override protected SoundEvent getDeathSound() { return WitcherySoundEvents.MOB_WOLFMAN_DEATH.get(); }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Infectious", infectious);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        infectious = tag.getBooleanOr("Infectious", false);
    }
}
