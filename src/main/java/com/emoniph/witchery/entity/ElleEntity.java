package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class ElleEntity extends PathfinderMob {

    private static final int LAVA_SEARCH_INTERVAL = 10;
    private static final int LAVA_HOME_RADIUS = 2;
    private static final int LAVA_TRANSFORM_TICKS = 40;
    private static final double OWNER_FOLLOW_START = 4.0D;
    private static final double OWNER_FOLLOW_STOP = 2.0D;
    private static final double OWNER_TELEPORT_DISTANCE = 28.0D;
    private static final double LAVA_HOME_DRIFT = 0.08D;

    @Nullable private UUID ownerUUID;
    @Nullable private BlockPos lavaHome;
    private int lavaTimer = 0;

    public ElleEntity(EntityType<? extends ElleEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel) || ownerUUID == null) {
            return;
        }

        Player owner = getOwner(serverLevel);
        if (tickCount % LAVA_SEARCH_INTERVAL == 0
                && (lavaHome == null || !ElleLavaPool.isPool(lavaHome, lavaPoolReader(serverLevel)))) {
            lavaHome = findNearbyLavaPool(serverLevel);
        }

        if (lavaHome == null) {
            if (owner != null) {
                followOwner(serverLevel, owner);
            }
            lavaTimer = Math.max(0, lavaTimer - 1);
            return;
        }

        if (!isAtLavaHome()) {
            moveTowardLavaHome();
            lavaTimer = Math.max(0, lavaTimer - 1);
            return;
        }

        getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.75D, 0.0D, 0.75D));
        lavaTimer++;
        if (lavaTimer == LAVA_TRANSFORM_TICKS / 2) {
            notifyOwner(serverLevel, "item.witchery.glass_goblet.lilith_summon_warn");
        }
        if (lavaTimer >= LAVA_TRANSFORM_TICKS) {
            spawnLilith(serverLevel);
            discard();
        }
    }

    @Nullable
    private Player getOwner(ServerLevel level) {
        return ownerUUID == null ? null : level.getPlayerByUUID(ownerUUID);
    }

    private void followOwner(ServerLevel level, Player owner) {
        double distanceSqr = distanceToSqr(owner);
        if (distanceSqr > OWNER_TELEPORT_DISTANCE * OWNER_TELEPORT_DISTANCE && tryTeleportNearOwner(level, owner)) {
            getNavigation().stop();
            return;
        }
        if (distanceSqr > OWNER_FOLLOW_START * OWNER_FOLLOW_START) {
            getNavigation().moveTo(owner, 1.0D);
        } else if (distanceSqr < OWNER_FOLLOW_STOP * OWNER_FOLLOW_STOP) {
            getNavigation().stop();
        }
    }

    private boolean tryTeleportNearOwner(ServerLevel level, Player owner) {
        BlockPos ownerPos = owner.blockPosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = random.nextInt(7) - 3;
            int dz = random.nextInt(7) - 3;
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
                continue;
            }
            BlockPos candidate = ownerPos.offset(dx, 0, dz);
            BlockPos feet = findSafeTeleportFeet(level, candidate);
            if (feet != null) {
                teleportTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
                level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.5D, getZ(),
                        12, 0.25D, 0.25D, 0.25D, 0.02D);
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static BlockPos findSafeTeleportFeet(ServerLevel level, BlockPos near) {
        for (int y = near.getY() + 2; y >= near.getY() - 4; y--) {
            BlockPos feet = new BlockPos(near.getX(), y, near.getZ());
            if (!level.hasChunkAt(feet)) {
                continue;
            }
            if (level.getBlockState(feet.below()).isSolid()
                    && level.isEmptyBlock(feet)
                    && level.isEmptyBlock(feet.above())
                    && !level.getBlockState(feet.below()).liquid()) {
                return feet;
            }
        }
        return null;
    }

    @Nullable
    private BlockPos findNearbyLavaPool(ServerLevel level) {
        return ElleLavaPool.findNearest(blockPosition(), lavaPoolReader(level));
    }

    private static ElleLavaPool.Reader lavaPoolReader(ServerLevel level) {
        return new ElleLavaPool.Reader() {
            @Override
            public boolean isLoaded(BlockPos pos) {
                return level.hasChunkAt(pos);
            }

            @Override
            public boolean isLava(BlockPos pos) {
                return level.getBlockState(pos).is(Blocks.LAVA);
            }

            @Override
            public boolean isAir(BlockPos pos) {
                return level.isEmptyBlock(pos);
            }
        };
    }

    private boolean isAtLavaHome() {
        if (lavaHome == null) {
            return false;
        }
        return distanceToSqr(lavaHome.getX() + 0.5D, lavaHome.getY() + 1.0D, lavaHome.getZ() + 0.5D)
                <= LAVA_HOME_RADIUS * LAVA_HOME_RADIUS;
    }

    private void moveTowardLavaHome() {
        if (lavaHome == null) {
            return;
        }
        double targetX = lavaHome.getX() + 0.5D;
        double targetY = lavaHome.getY() + 1.0D;
        double targetZ = lavaHome.getZ() + 0.5D;
        getNavigation().moveTo(targetX, targetY, targetZ, 1.0D);
        getLookControl().setLookAt(targetX, targetY, targetZ, 30.0F, 30.0F);

        Vec3 toward = new Vec3(targetX - getX(), targetY - getY(), targetZ - getZ());
        if (toward.lengthSqr() > 0.01D) {
            Vec3 drift = toward.normalize().scale(LAVA_HOME_DRIFT);
            setDeltaMovement(getDeltaMovement().add(drift.x, Math.max(drift.y, -0.05D), drift.z));
        }
    }

    private void spawnLilith(ServerLevel level) {
        LilithEntity lilith = WitcheryEntities.LILITH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (lilith != null) {
            BlockPos pos = lavaHome == null ? blockPosition() : lavaHome;
            lilith.setPos(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
            lilith.setInvulnerableStart();
            level.addFreshEntity(lilith);
            notifyOwner(level, "item.witchery.glass_goblet.lilith_summon");
        }
        if (lavaHome != null) {
            level.explode(this, lavaHome.getX() + 0.5D, lavaHome.getY() + 1.0D, lavaHome.getZ() + 0.5D,
                    6.0F, Level.ExplosionInteraction.MOB);
            level.sendParticles(ParticleTypes.LAVA,
                    lavaHome.getX() + 0.5D, lavaHome.getY() + 1.0D, lavaHome.getZ() + 0.5D,
                    32, 1.0D, 0.4D, 1.0D, 0.05D);
        }
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0f, 0.8f);
    }

    private void notifyOwner(ServerLevel level, String messageKey) {
        Player owner = getOwner(level);
        if (owner != null) {
            owner.displayClientMessage(Component.translatable(messageKey)
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
        }
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
        if (uuid != null) {
            setPersistenceRequired();
        }
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public static boolean isElleNear(Level level, BlockPos center) {
        AABB bounds = new AABB(center).inflate(32);
        return !level.getEntitiesOfClass(ElleEntity.class, bounds).isEmpty();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) {
            tag.putString("Owner", ownerUUID.toString());
        }
        if (lavaHome != null) {
            tag.putLong("LavaHome", lavaHome.asLong());
        }
        tag.putInt("LavaTimer", lavaTimer);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String owner = tag.getStringOr("Owner", "");
        try {
            ownerUUID = owner.isEmpty() ? null : UUID.fromString(owner);
        } catch (IllegalArgumentException ignored) {
            ownerUUID = null;
        }
        if (ownerUUID != null) {
            setPersistenceRequired();
        }
        long packedHome = tag.getLongOr("LavaHome", Long.MIN_VALUE);
        lavaHome = packedHome == Long.MIN_VALUE ? null : BlockPos.of(packedHome);
        lavaTimer = tag.getIntOr("LavaTimer", 0);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // Elle drops nothing
    }
}
