package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
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

import javax.annotation.Nullable;
import java.util.UUID;

public class ElleEntity extends PathfinderMob {

    private static final int LAVA_TRANSFORM_TICKS = 20;

    @Nullable private UUID ownerUUID;
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
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && ownerUUID != null) {
            if (isNearLava()) {
                lavaTimer++;
                if (lavaTimer >= LAVA_TRANSFORM_TICKS) {
                    spawnLilith();
                    discard();
                    return;
                }
            } else {
                lavaTimer = Math.max(0, lavaTimer - 1);
            }
        }
    }

    private boolean isNearLava() {
        BlockPos myPos = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(myPos.offset(-8, -4, -8), myPos.offset(8, 4, 8))) {
            if (level().getBlockState(pos).is(Blocks.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private void spawnLilith() {
        LilithEntity lilith = WitcheryEntities.LILITH.get().create((ServerLevel) level(), EntitySpawnReason.TRIGGERED);
        if (lilith != null) {
            lilith.setPos(getX(), getY(), getZ());
            level().addFreshEntity(lilith);
        }
        level().playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0f, 0.8f);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.ownerUUID = uuid;
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
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // Elle drops nothing
    }
}
