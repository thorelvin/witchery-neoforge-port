package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Were-Villager — a cursed villager waiting to transform.
 * During daytime (or non-full-moon nights) it wanders peacefully.
 * Every 100 ticks on a full-moon night it converts to a WolfmanEntity.
 *
 * Tracks formerProfession (the villager profession integer 0–5) and the
 * infectious flag so the resulting WolfmanEntity can store them.
 *
 * Source parity: EntityVillagerWere 1.7.10-0.24.1
 */
public class WereVillagerEntity extends Monster {

    private int  formerProfession = 0;
    private boolean infectious    = false;

    public WereVillagerEntity(EntityType<? extends WereVillagerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    // ── Goals — wanders, does not attack ─────────────────────────────────────

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // ── Tick — full-moon transformation ───────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 100 == 7 && isFullMoonNight()) {
            convertToWolfman();
        }
    }

    private boolean isFullMoonNight() {
        if (!(level() instanceof ServerLevel sl)) return false;
        long dayTime = sl.getDayTime() % 24000L;
        return (sl.getDayTime() / 24000L) % 8L == 0 && dayTime > 13000L && dayTime < 23000L;
    }

    private void convertToWolfman() {
        if (!(level() instanceof ServerLevel sl)) return;
        WolfmanEntity wolfman = WitcheryEntities.WOLFMAN.get()
                .create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (wolfman == null) return;
        wolfman.setPos(getX(), getY(), getZ());
        wolfman.setYRot(getYRot());
        wolfman.setFormerProfession(formerProfession);
        if (infectious) wolfman.setInfectious();
        wolfman.setPersistenceRequired();
        wolfman.finalizeSpawn(sl,
                new net.minecraft.world.DifficultyInstance(sl.getDifficulty(), 0L, 0L, 0.0f),
                EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(wolfman);
        sl.levelEvent(null, 1017, blockPosition(), 0);
        discard();
    }

    // ── Profession / infection ────────────────────────────────────────────────

    public int getFormerProfession() { return formerProfession; }
    public void setFormerProfession(int p) { this.formerProfession = p; }
    public boolean isInfectious() { return infectious; }
    public void setInfectious(boolean b) { this.infectious = b; }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.VILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VILLAGER_DEATH; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FormerProfession", formerProfession);
        tag.putBoolean("Infectious", infectious);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        formerProfession = tag.getIntOr("FormerProfession", 0);
        infectious = tag.getBooleanOr("Infectious", false);
    }
}
