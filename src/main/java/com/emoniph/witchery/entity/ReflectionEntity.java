package com.emoniph.witchery.entity;

import com.emoniph.witchery.blocks.entity.MirrorBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

    private UUID ownerUUID = null;
    private String ownerName = "";

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
        this.ownerUUID = player.getUUID();
        this.ownerName = player.getName().getString();
        snapshotEquipment(player);
    }

    private void snapshotEquipment(Player player) {
        equipmentSnapshot[0] = player.getItemBySlot(EquipmentSlot.FEET).copy();
        equipmentSnapshot[1] = player.getItemBySlot(EquipmentSlot.LEGS).copy();
        equipmentSnapshot[2] = player.getItemBySlot(EquipmentSlot.CHEST).copy();
        equipmentSnapshot[3] = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        equipmentSnapshot[4] = player.getItemBySlot(EquipmentSlot.MAINHAND).copy();
        equipmentSnapshot[5] = player.getItemBySlot(EquipmentSlot.OFFHAND).copy();
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public ItemStack[] getEquipmentSnapshot() { return equipmentSnapshot; }

    @Override
    public void tick() {
        super.tick();
        // Refresh equipment snapshot every 20t if owner is in same level
        if (!level().isClientSide() && ownerUUID != null && tickCount % 20 == 0) {
            Player owner = level().getPlayerByUUID(ownerUUID);
            if (owner != null) snapshotEquipment(owner);
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        super.die(source);
        if (!level().isClientSide()) {
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
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String uuidStr = tag.getStringOr("OwnerUUID", "");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUUID = UUID.fromString(uuidStr);
                ownerName = tag.getStringOr("OwnerName", "");
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
