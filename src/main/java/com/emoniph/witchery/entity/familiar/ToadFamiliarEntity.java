package com.emoniph.witchery.entity.familiar;

import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Toad familiar — follows the owner and passively collects nearby dropped item entities.
 */
public class ToadFamiliarEntity extends FamiliarEntity {
    private static final EntityDataAccessor<Integer> SKIN_COLOR =
            SynchedEntityData.defineId(ToadFamiliarEntity.class, EntityDataSerializers.INT);

    private static final int COLLECT_INTERVAL = 200;
    private static final double COLLECT_RANGE  = 6.0;

    public ToadFamiliarEntity(EntityType<? extends ToadFamiliarEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            setSkinColor(random.nextInt(16));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_COLOR, 0);
    }

    public int getSkinColor() {
        return entityData.get(SKIN_COLOR) & 15;
    }

    public void setSkinColor(int color) {
        entityData.set(SKIN_COLOR, color & 15);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isOwnedBy(player) && isOrderedToSit() && player.isShiftKeyDown()) {
            return super.mobInteract(player, hand);
        }
        ItemStack held = player.getItemInHand(hand);
        if (isTame() && isOwnedBy(player) && held.getItem() instanceof DyeItem dye) {
            int color = dye.getDyeColor().getId();
            if (color != getSkinColor()) {
                setSkinColor(color);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % COLLECT_INTERVAL == 0 && isTame()) {
            if (getOwner() instanceof ServerPlayer owner) {
                collectNearbyItems(owner);
            }
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return super.getAmbientSoundInterval() * 2;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return WitcherySoundEvents.MOB_TOAD_CROAK.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WitcherySoundEvents.MOB_TOAD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WitcherySoundEvents.MOB_TOAD_HURT.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    private void collectNearbyItems(ServerPlayer owner) {
        AABB area = getBoundingBox().inflate(COLLECT_RANGE);
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, area);
        for (ItemEntity ie : items) {
            if (ie.isAlive() && !ie.hasPickUpDelay()) {
                ItemStack stack = ie.getItem().copy();
                if (owner.getInventory().add(stack)) {
                    ie.discard();
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SkinColor", getSkinColor());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setSkinColor(tag.getIntOr("SkinColor", 0));
    }
}
