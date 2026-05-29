package com.emoniph.witchery.entity;

import com.emoniph.witchery.blocks.entity.BloodCrucibleBlockEntity;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Vampire mob entity — source parity with EntityVampire 1.7.10-0.24.1.
 *
 * Behaviour summary:
 * - Daytime: flees sunlight, burns every 20 ticks in sun, clears target,
 *   teleports back to coffinPos if >16 blocks away.
 * - Nighttime: targets non-vampire players and nearby villagers.
 *   Bites villagers for blood (10% chance/hit, heals 4 HP, increments
 *   damageDone). Once damageDone ≥ 20 returns home and fills a nearby
 *   BloodCrucibleBlockEntity.
 * - Every 500 ticks if no target found, tries to stay near players.
 * - Wears random vampire clothing set (male or female variant).
 * - UNDEAD creature type.
 */
public class VampireEntity extends Monster {

    private static final EntityDataAccessor<Integer> GUARD_TYPE =
            SynchedEntityData.defineId(VampireEntity.class, EntityDataSerializers.INT);

    /** Position the vampire spawned at / considers its "coffin" home. */
    private BlockPos coffinPos = BlockPos.ZERO;

    /** Cumulative blood drained from villagers this night. Resets at dawn. */
    private float damageDone = 0.0F;

    public VampireEntity(EntityType<? extends VampireEntity> type, Level level) {
        super(type, level);
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,      40.0)
                .add(Attributes.MOVEMENT_SPEED,   0.4)
                .add(Attributes.FOLLOW_RANGE,    20.0)
                .add(Attributes.ATTACK_DAMAGE,    5.0);
    }

    // Undead entity type is declared via data/witchery/tags/entity_type/undead.json
    // and/or via the minecraft:undead entity type tag in vanilla tags.

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1,  new FloatGoal(this));
        goalSelector.addGoal(2,  new RestrictSunGoal(this));
        goalSelector.addGoal(3,  new FleeSunGoal(this, 1.0));
        goalSelector.addGoal(8,  new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(9,  new OpenDoorGoal(this, true));
        goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 0, false, true,
                (entity, sl) -> isValidVampireTarget(entity)));
    }

    private boolean isValidVampireTarget(LivingEntity entity) {
        if (entity instanceof Villager) return true;
        if (entity instanceof Player player) {
            if (!(player instanceof ServerPlayer sp)) return false;
            WitcheryPlayerData data = WitcheryAttachments.get(sp);
            return !data.isVampire(); // attack non-vampire players only
        }
        return false;
    }

    // ── Synced data ───────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GUARD_TYPE, 0);
    }

    public int getGuardType() { return entityData.get(GUARD_TYPE); }
    public void setGuardType(int t) { entityData.set(GUARD_TYPE, t); }

    // ── Tick — daytime burning + coffin home logic ────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        // Sunlight burn
        if (tickCount % 20 == 2 && isInSunlight()) {
            igniteForSeconds(2.0f);
        }

        // Daytime: clear target, reset feeding, return home if far
        long dayTime = level().getDayTime() % 24000L;
        boolean isDay = dayTime < 12300L;
        if (isDay) {
            setTarget(null);
            if (tickCount % 100 == 2) {
                damageDone = 0.0F;
                if (distanceToSqr(coffinPos.getX(), coffinPos.getY(), coffinPos.getZ()) > 16.0 * 16.0) {
                    smokeParticles();
                    teleportTo(coffinPos.getX() + 0.5, coffinPos.getY(), coffinPos.getZ() + 0.5);
                    smokeParticles();
                }
            }
        } else {
            // Nighttime: fed enough → go home and fill blood crucible
            if (damageDone >= 20.0F) {
                setTarget(null);
                damageDone = 0.0F;
                if (level() instanceof ServerLevel sl) {
                    smokeParticles();
                    teleportTo(coffinPos.getX() + 0.5, coffinPos.getY(), coffinPos.getZ() + 0.5);
                    smokeParticles();
                    fillNearbyBloodCrucible(sl);
                }
            }
        }
    }

    private boolean isInSunlight() {
        long dayTime = level().getDayTime() % 24000L;
        return dayTime < 12300L && level().canSeeSky(blockPosition());
    }

    private void smokeParticles() {
        for (int i = 0; i < 8; i++) {
            level().addParticle(ParticleTypes.POOF,
                    getX() + random.nextGaussian() * 0.3,
                    getY() + 0.5 + random.nextGaussian() * 0.2,
                    getZ() + random.nextGaussian() * 0.3,
                    0, 0.02, 0);
        }
    }

    private void fillNearbyBloodCrucible(ServerLevel level) {
        int r = 6;
        BlockPos home = coffinPos;
        for (BlockPos pos : BlockPos.betweenClosed(home.offset(-r,-r,-r), home.offset(r,r,r))) {
            if (level.getBlockState(pos).is(WitcheryBlocks.BLOOD_CRUCIBLE.get())) {
                if (level.getBlockEntity(pos) instanceof BloodCrucibleBlockEntity be) {
                    be.increaseBloodLevel(level);
                    return;
                }
            }
        }
    }

    // ── Melee — blood drain from villagers ────────────────────────────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof Villager villager) {
            // 10% chance to drain blood on hit
            if (random.nextInt(10) == 0) {
                damageDone += 4.0F;
                heal(4.0F);
                // Red dust particles at villager
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        villager.getX(), villager.getY() + villager.getBbHeight() * 0.8, villager.getZ(),
                        6, 0.3, 0.2, 0.3, 0.02);
                playSound(SoundEvents.PLAYER_HURT, 0.6f, 1.2f);
            }
            // Still register the attack
            return villager.hurtServer(level, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        return super.doHurtTarget(level, target);
    }

    // ── Drops — none ──────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // Vampires drop nothing per source
    }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.VILLAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.VILLAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.VILLAGER_DEATH; }

    // ── Spawn init — equipment + coffinPos ────────────────────────────────────

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        EntitySpawnReason spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        // Record spawn point as home coffin
        coffinPos = blockPosition();

        // Random male (0) or female (1) variant
        boolean male = random.nextBoolean();
        setGuardType(male ? 0 : 1);

        // Vampire boots always
        setItemSlot(EquipmentSlot.FEET, new ItemStack(WitcheryItems.VAMPIRE_BOOTS.get()));

        if (male) {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(
                    random.nextInt(3) == 0
                            ? WitcheryItems.VAMPIRE_CHAIN_COAT.get()
                            : WitcheryItems.VAMPIRE_COAT.get()));
            setItemSlot(EquipmentSlot.LEGS, new ItemStack(
                    random.nextInt(3) == 0
                            ? WitcheryItems.VAMPIRE_SKIRTED_TROUSERS.get()
                            : WitcheryItems.VAMPIRE_TROUSERS.get()));
        } else {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(
                    random.nextInt(3) == 0
                            ? WitcheryItems.VAMPIRE_CHAIN_COAT_LADIES.get()
                            : WitcheryItems.VAMPIRE_CORSET.get()));
            setItemSlot(EquipmentSlot.LEGS, new ItemStack(
                    random.nextInt(4) != 0
                            ? WitcheryItems.VAMPIRE_SKIRTED_TROUSERS.get()
                            : WitcheryItems.VAMPIRE_TROUSERS.get()));
        }

        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GuardType", getGuardType());
        tag.putInt("BaseX", coffinPos.getX());
        tag.putInt("BaseY", coffinPos.getY());
        tag.putInt("BaseZ", coffinPos.getZ());
        tag.putFloat("DamageDone", damageDone);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setGuardType(tag.getIntOr("GuardType", 0));
        int x = tag.getIntOr("BaseX", (int)getX());
        int y = tag.getIntOr("BaseY", (int)getY());
        int z = tag.getIntOr("BaseZ", (int)getZ());
        coffinPos = new BlockPos(x, y, z);
        damageDone = tag.getIntOr("DamageDone", 0); // stored as int for simplicity
    }
}
