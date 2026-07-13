package com.emoniph.witchery.entity;

import com.emoniph.witchery.blocks.entity.MirrorBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Guards Mirror World rooms. 100 HP, player-only target.
 * Mimics the owner player's skin and held/worn equipment visually.
 * On death, marks the room's mirror as cleared (demonKilled=true).
 * Drops equipment copies on death.
 */
public class ReflectionEntity extends Monster {
    private static final EntityDataAccessor<String> DATA_OWNER_UUID =
            SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.STRING);

    private UUID ownerUUID = null;
    private String ownerName = "";
    private int temporaryTicks;

    // Equipment snapshots (updated when owner is nearby, used by renderer)
    private ItemStack[] equipmentSnapshot = new ItemStack[6]; // slots: feet,legs,chest,head,mainhand,offhand

    public ReflectionEntity(EntityType<? extends ReflectionEntity> type, Level level) {
        super(type, level);
        for (int i = 0; i < equipmentSnapshot.length; i++) equipmentSnapshot[i] = ItemStack.EMPTY;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
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
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void setOwner(ServerPlayer player) {
        setOwnerIdentity(player.getUUID(), player.getName().getString());
        snapshotEquipment(player);
    }

    public void setOwnerIdentity(UUID ownerUUID, String ownerName) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName == null ? "" : ownerName;
        entityData.set(DATA_OWNER_UUID, ownerUUID == null ? "" : ownerUUID.toString());
        entityData.set(DATA_OWNER_NAME, this.ownerName);
    }

    public void setTemporaryDecoy(int ticks) {
        this.temporaryTicks = Math.max(1, ticks);
    }

    private void snapshotEquipment(Player player) {
        equipmentSnapshot[0] = player.getItemBySlot(EquipmentSlot.FEET).copy();
        equipmentSnapshot[1] = player.getItemBySlot(EquipmentSlot.LEGS).copy();
        equipmentSnapshot[2] = player.getItemBySlot(EquipmentSlot.CHEST).copy();
        equipmentSnapshot[3] = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        equipmentSnapshot[4] = player.getItemBySlot(EquipmentSlot.MAINHAND).copy();
        equipmentSnapshot[5] = player.getItemBySlot(EquipmentSlot.OFFHAND).copy();
    }

    public UUID getOwnerUUID() {
        String syncedUuid = entityData.get(DATA_OWNER_UUID);
        if (!syncedUuid.isEmpty()) {
            try {
                return UUID.fromString(syncedUuid);
            } catch (IllegalArgumentException ignored) {
                return ownerUUID;
            }
        }
        return ownerUUID;
    }
    public String getOwnerName() {
        String syncedName = entityData.get(DATA_OWNER_NAME);
        return syncedName.isEmpty() ? ownerName : syncedName;
    }
    public ItemStack[] getEquipmentSnapshot() { return equipmentSnapshot; }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        if (temporaryTicks > 0 && ownerUUID != null && target.getUUID().equals(ownerUUID)) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && temporaryTicks > 0 && --temporaryTicks <= 0) {
            discard();
            return;
        }
        // Refresh equipment snapshot every 20t if owner is in same level
        if (!level().isClientSide() && ownerUUID != null && tickCount % 20 == 0) {
            Player owner = level().getPlayerByUUID(ownerUUID);
            if (owner != null) snapshotEquipment(owner);
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (!level().isClientSide() && temporaryTicks <= 0) {
            dropEquipmentCopies();
            notifyMirrorCleared();
        }
    }

    private void dropEquipmentCopies() {
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            for (ItemStack stack : equipmentSnapshot) {
                if (!stack.isEmpty()) {
                    spawnAtLocation(sl, stack.copy());
                }
            }
        }
    }

    private void notifyMirrorCleared() {
        AABB searchBox = new AABB(
                blockPosition().getX() - 12, blockPosition().getY() - 4, blockPosition().getZ() - 12,
                blockPosition().getX() + 12, blockPosition().getY() + 12, blockPosition().getZ() + 12
        );
        BlockPos.betweenClosedStream(BlockPos.containing(searchBox.minX, searchBox.minY, searchBox.minZ),
                BlockPos.containing(searchBox.maxX, searchBox.maxY, searchBox.maxZ))
                .forEach(checkPos -> {
                    var state = level().getBlockState(checkPos);
                    if (state.is(WitcheryBlocks.MIRROR.get())) {
                        BlockPos footPos = state.hasProperty(
                                com.emoniph.witchery.blocks.MirrorBlock.PART)
                                && state.getValue(com.emoniph.witchery.blocks.MirrorBlock.PART) == BedPart.HEAD
                                ? checkPos.below() : checkPos;
                        if (level().getBlockEntity(footPos) instanceof MirrorBlockEntity be) {
                            be.setDemonKilled(true);
                        }
                    }
                });
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) {
            tag.putString("OwnerUUID", ownerUUID.toString());
            tag.putString("OwnerName", ownerName);
        }
        if (temporaryTicks > 0) {
            tag.putInt("TemporaryTicks", temporaryTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String uuidStr = tag.getStringOr("OwnerUUID", "");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUUID = UUID.fromString(uuidStr);
                ownerName = tag.getStringOr("OwnerName", "");
                entityData.set(DATA_OWNER_UUID, ownerUUID.toString());
                entityData.set(DATA_OWNER_NAME, ownerName);
            } catch (IllegalArgumentException ignored) {}
        }
        temporaryTicks = tag.getIntOr("TemporaryTicks", 0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return WitcherySoundEvents.MOB_REFLECTION_SAY.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WitcherySoundEvents.MOB_REFLECTION_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WitcherySoundEvents.MOB_REFLECTION_DEATH.get();
    }
}
