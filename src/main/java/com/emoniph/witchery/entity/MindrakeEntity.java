package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Mindrake — a tameable "living bomb" plant-creature. Port of EntityMindrake (1.7.10-0.24.1).
 *
 *  - Hatches from a Mindrake Bulb left on the ground (see {@code MindrakeEvents}); if a player
 *    threw it, the hatchling is tamed to them and fights at their side.
 *  - 4 HP, 0.4 speed, breathes underwater. Screams (ghast voice).
 *  - It does not deal melee damage — instead it DETONATES on contact: a small explosion, then it
 *    is gone, leaving a flower behind on grass/dirt. Killing it instead makes it burst (smaller
 *    explosion) and drop Mandrake Seeds.
 *  - Untamed mindrakes hunt players; tamed ones defend their owner. Not breedable.
 */
public class MindrakeEntity extends TamableAnimal {

    public MindrakeEntity(EntityType<? extends MindrakeEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setTame(false, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,     4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE,  1.0)   // unused (it detonates) but keeps the melee goal happy
                .add(Attributes.FOLLOW_RANGE,  16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0, 10f, 2f));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // Only an untamed mindrake hunts players unprompted.
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                (living, level) -> !this.isTame()));
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    // ── Detonation ────────────────────────────────────────────────────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        detonate(level, 1.5F);
        return true;
    }

    private void detonate(ServerLevel level, float power) {
        level.explode(this, getX(), getY(), getZ(), power, Level.ExplosionInteraction.MOB);
        BlockPos at = blockPosition();
        BlockState below = level.getBlockState(at.below());
        if ((below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT)) && level.getBlockState(at).isAir()) {
            level.setBlock(at, (random.nextInt(2) == 0 ? Blocks.POPPY : Blocks.DANDELION).defaultBlockState(), 3);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl && !isRemoved()) {
            sl.explode(this, getX(), getY(), getZ(), 1.0F, Level.ExplosionInteraction.MOB);
        }
        super.die(source);
    }

    // ── Drops (only when killed, not when it detonates) ──────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        spawnAtLocation(level, new ItemStack(WitcheryItems.MANDRAKE_SEEDS.get(), random.nextDouble() <= 0.25 ? 2 : 1));
    }

    // ── Not breedable ─────────────────────────────────────────────────────────

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    // ── Sounds (the screaming root) ──────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.GHAST_SCREAM; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GHAST_DEATH; }
}
