package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class CrazyCatLadyEntity extends PathfinderMob {
    private static final int MAX_CAT_THROWS = 7;
    private static final int THROW_INTERVAL_TICKS = 20 * 5;
    private static final int CAT_READY_LEAD_TICKS = 20 * 2;
    private static final int RETREAT_TICKS = 20 * 60;
    private static final double SHOUT_RADIUS = 18.0D;
    private static final double ATTACK_RANGE = 18.0D;
    private static final EntityDataAccessor<Boolean> CAT_READY =
            SynchedEntityData.defineId(CrazyCatLadyEntity.class, EntityDataSerializers.BOOLEAN);

    private static final String[] Q_AGGRO = {
            "entity.witchery.crazy_cat_lady.aggro.1",
            "entity.witchery.crazy_cat_lady.aggro.2",
            "entity.witchery.crazy_cat_lady.aggro.3"
    };
    private static final String[] Q_THROW = {
            "entity.witchery.crazy_cat_lady.throw.1",
            "entity.witchery.crazy_cat_lady.throw.2",
            "entity.witchery.crazy_cat_lady.throw.3"
    };
    private static final String[] Q_MUMBLE = {
            "entity.witchery.crazy_cat_lady.mumble.1",
            "entity.witchery.crazy_cat_lady.mumble.2",
            "entity.witchery.crazy_cat_lady.mumble.3"
    };

    private BlockPos hutHome = BlockPos.ZERO;
    private boolean hasHutHome = false;
    private int catsThrown = 0;
    private int throwCooldown = THROW_INTERVAL_TICKS;
    private int retreatTicks = 0;
    private int shoutCooldown = 0;
    @Nullable
    private LivingEntity previousTarget = null;

    public CrazyCatLadyEntity(EntityType<? extends CrazyCatLadyEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAT_READY, false);
    }

    public boolean hasCatReady() {
        return this.entityData.get(CAT_READY);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, catThrowGoal());
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, Player.class, 0, false, true,
                (entity, serverLevel) -> retreatTicks <= 0 && catsThrown < MAX_CAT_THROWS));
    }

    private Goal catThrowGoal() {
        return new Goal() {
            {
                setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            }

            @Override
            public boolean canUse() {
                LivingEntity target = getTarget();
                return retreatTicks <= 0 && catsThrown < MAX_CAT_THROWS
                        && target != null && target.isAlive();
            }

            @Override
            public boolean canContinueToUse() {
                return canUse();
            }

            @Override
            public void tick() {
                LivingEntity target = getTarget();
                if (target == null) {
                    return;
                }
                getLookControl().setLookAt(target, 30.0F, 30.0F);
                double distance = distanceTo(target);
                if (distance > 9.0D) {
                    getNavigation().moveTo(target, 0.9D);
                } else {
                    getNavigation().stop();
                }

                if (throwCooldown <= 0 && distance <= ATTACK_RANGE && hasLineOfSight(target)) {
                    throwCooldown = THROW_INTERVAL_TICKS;
                    throwCatAt(target);
                    if (catsThrown >= MAX_CAT_THROWS) {
                        beginRetreat();
                    }
                }
            }
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (throwCooldown > 0) {
            throwCooldown--;
        }
        if (shoutCooldown > 0) {
            shoutCooldown--;
        }

        LivingEntity target = getTarget();
        if (target != null && target != previousTarget) {
            shout(pick(Q_AGGRO), 80);
            playSound(SoundEvents.WITCH_AMBIENT, 1.0F, 0.8F + random.nextFloat() * 0.3F);
        } else if (target != null && tickCount % 160 == 13) {
            shout(pick(Q_MUMBLE), 80);
            playSound(SoundEvents.WITCH_AMBIENT, 0.7F, 0.7F + random.nextFloat() * 0.5F);
        }
        previousTarget = target;

        if (retreatTicks > 0) {
            setTarget(null);
            retreatTicks--;
            moveTowardHome();
            if (retreatTicks == 0) {
                catsThrown = 0;
                throwCooldown = THROW_INTERVAL_TICKS;
            }
        }
        updateCatReadyState();
    }

    public void setHutHome(BlockPos pos) {
        this.hutHome = pos.immutable();
        this.hasHutHome = true;
    }

    private void throwCatAt(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ThrownCatEntity cat = WitcheryEntities.THROWN_CAT.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
        if (cat == null) {
            return;
        }
        Vec3 start = new Vec3(getX(), getEyeY() - 0.15D, getZ());
        Vec3 aim = new Vec3(target.getX() - getX(),
                target.getEyeY() - 0.25D - start.y,
                target.getZ() - getZ());
        double flat = Mth.sqrt((float) (aim.x * aim.x + aim.z * aim.z));
        Vec3 motion = new Vec3(aim.x, aim.y + flat * 0.18D, aim.z).normalize().scale(0.95D);
        cat.setOwner(this);
        cat.snapTo(start.x, start.y, start.z, getYRot(), 0.0F);
        cat.launch(motion);
        serverLevel.addFreshEntity(cat);
        catsThrown++;
        entityData.set(CAT_READY, false);
        shout(pick(Q_THROW), 70);
        playSound(SoundEvents.WITCH_THROW, 1.0F, 0.75F + random.nextFloat() * 0.25F);
        playSound(SoundEvents.CAT_DEATH, 0.8F, 1.55F + random.nextFloat() * 0.25F);
    }

    private void beginRetreat() {
        retreatTicks = RETREAT_TICKS;
        previousTarget = null;
        setTarget(null);
        entityData.set(CAT_READY, false);
        shout("entity.witchery.crazy_cat_lady.retreat", 100);
        playSound(SoundEvents.WITCH_CELEBRATE, 1.0F, 0.9F + random.nextFloat() * 0.2F);
        moveTowardHome();
    }

    private void updateCatReadyState() {
        LivingEntity target = getTarget();
        boolean ready = retreatTicks <= 0
                && catsThrown < MAX_CAT_THROWS
                && target != null
                && target.isAlive()
                && throwCooldown <= CAT_READY_LEAD_TICKS;
        entityData.set(CAT_READY, ready);
    }

    private void moveTowardHome() {
        if (!hasHutHome) {
            return;
        }
        if (distanceToSqr(hutHome.getX() + 0.5D, hutHome.getY(), hutHome.getZ() + 0.5D) > 2.0D) {
            getNavigation().moveTo(hutHome.getX() + 0.5D, hutHome.getY(), hutHome.getZ() + 0.5D, 0.9D);
        } else {
            getNavigation().stop();
        }
    }

    private void shout(String translationKey, int cooldownTicks) {
        if (shoutCooldown > 0 || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        shoutCooldown = cooldownTicks;
        Component msg = Component.translatable(translationKey);
        AABB area = getBoundingBox().inflate(SHOUT_RADIUS, 6.0D, SHOUT_RADIUS);
        List<ServerPlayer> nearby = serverLevel.getEntitiesOfClass(ServerPlayer.class, area);
        nearby.forEach(player -> player.displayClientMessage(msg, true));
    }

    private String pick(String[] pool) {
        return pool[random.nextInt(pool.length)];
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITCH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.WITCH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITCH_DEATH;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason spawnReason,
                                        @Nullable SpawnGroupData spawnGroupData) {
        if (!hasHutHome) {
            setHutHome(blockPosition());
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("HasHutHome", hasHutHome);
        tag.putInt("HutHomeX", hutHome.getX());
        tag.putInt("HutHomeY", hutHome.getY());
        tag.putInt("HutHomeZ", hutHome.getZ());
        tag.putInt("CatsThrown", catsThrown);
        tag.putInt("ThrowCooldown", throwCooldown);
        tag.putInt("RetreatTicks", retreatTicks);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        hasHutHome = tag.getBooleanOr("HasHutHome", false);
        hutHome = new BlockPos(tag.getIntOr("HutHomeX", 0),
                tag.getIntOr("HutHomeY", 0),
                tag.getIntOr("HutHomeZ", 0));
        catsThrown = tag.getIntOr("CatsThrown", 0);
        throwCooldown = tag.getIntOr("ThrowCooldown", THROW_INTERVAL_TICKS);
        retreatTicks = tag.getIntOr("RetreatTicks", 0);
    }
}
