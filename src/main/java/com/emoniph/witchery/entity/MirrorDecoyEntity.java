package com.emoniph.witchery.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Ten-second passive player double produced by the source Duplication Grenade. */
public class MirrorDecoyEntity extends PathfinderMob {
    public static final int SOURCE_LIFETIME_TICKS = 200;

    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(MirrorDecoyEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(MirrorDecoyEntity.class, EntityDataSerializers.STRING);

    @Nullable private UUID ownerUUID;
    private String ownerName = "";
    private int ticksToLive = SOURCE_LIFETIME_TICKS;

    public MirrorDecoyEntity(EntityType<? extends MirrorDecoyEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, "");
        builder.define(DATA_OWNER_NAME, "");
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public void setOwner(@Nullable UUID uuid, @Nullable String name) {
        ownerUUID = uuid;
        ownerName = name == null ? "" : name;
        entityData.set(DATA_OWNER_UUID, uuid == null ? "" : uuid.toString());
        entityData.set(DATA_OWNER_NAME, ownerName);
        Player owner = uuid == null ? null : level().getPlayerByUUID(uuid);
        if (owner != null) copyEquipment(owner);
        setCustomName(ownerName.isBlank() ? null : net.minecraft.network.chat.Component.literal(ownerName));
    }

    public void setOwner(ServerPlayer owner) {
        setOwner(owner.getUUID(), owner.getScoreboardName());
    }

    @Nullable
    public UUID getOwnerUUID() {
        String value = entityData.get(DATA_OWNER_UUID);
        try {
            return value.isBlank() ? ownerUUID : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return ownerUUID;
        }
    }

    public String getOwnerName() {
        String value = entityData.get(DATA_OWNER_NAME);
        return value.isBlank() ? ownerName : value;
    }

    public int getTicksToLive() {
        return ticksToLive;
    }

    public void setLifetimeTicks(int ticks) {
        ticksToLive = Math.max(1, ticks);
    }

    private void copyEquipment(Player owner) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = owner.getItemBySlot(slot);
            setItemSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            setDropChance(slot, 0.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (--ticksToLive <= 0) {
            discard();
            return;
        }
        if (tickCount == 1) {
            Player owner = ownerUUID == null ? null : level().getPlayerByUUID(ownerUUID);
            if (owner != null) copyEquipment(owner);
        }
        if (tickCount % 40 == 5) attractAttention();
    }

    public void attractAttention() {
        Player owner = ownerUUID == null ? null : level().getPlayerByUUID(ownerUUID);
        if (owner == null) return;
        AABB bounds = getBoundingBox().inflate(16.0D, 8.0D, 16.0D);
        for (Monster monster : level().getEntitiesOfClass(Monster.class, bounds)) {
            if (monster.getTarget() == owner) {
                monster.setTarget(this);
                monster.setLastHurtByMob(this);
            }
        }
    }

    @Override
    protected void dropFromLootTable(net.minecraft.server.level.ServerLevel level,
                                     net.minecraft.world.damagesource.DamageSource source,
                                     boolean causedByPlayer) {
        // Source follower type 5 has no drops.
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putString("OwnerUUID", ownerUUID.toString());
        tag.putString("OwnerName", ownerName);
        tag.putInt("TicksToLive", ticksToLive);
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String value = tag.getStringOr("OwnerUUID", "");
        try {
            ownerUUID = value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            ownerUUID = null;
        }
        ownerName = tag.getStringOr("OwnerName", "");
        ticksToLive = tag.getIntOr("TicksToLive", SOURCE_LIFETIME_TICKS);
        setOwner(ownerUUID, ownerName);
    }
}
