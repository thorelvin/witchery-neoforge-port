package com.emoniph.witchery.entity.familiar;

import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Owl familiar — follows the owner and periodically warns them of nearby hostile mobs.
 */
public class OwlFamiliarEntity extends FamiliarEntity {
    private static final EntityDataAccessor<Integer> FEATHER_COLOR =
            SynchedEntityData.defineId(OwlFamiliarEntity.class, EntityDataSerializers.INT);

    private static final int ALERT_INTERVAL = 100; // ticks between scans
    private static final double ALERT_RANGE  = 20.0;

    public OwlFamiliarEntity(EntityType<? extends OwlFamiliarEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide()) {
            setFeatherColor(random.nextInt(100) == 0 ? 0 : random.nextInt(15) + 1);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FEATHER_COLOR, 0);
    }

    public int getFeatherColor() {
        return entityData.get(FEATHER_COLOR) & 15;
    }

    public void setFeatherColor(int color) {
        entityData.set(FEATHER_COLOR, color & 15);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isOwnedBy(player) && isOrderedToSit() && player.isShiftKeyDown()) {
            return super.mobInteract(player, hand);
        }
        ItemStack held = player.getItemInHand(hand);
        if (isTame() && isOwnedBy(player) && held.getItem() instanceof DyeItem dye) {
            DyeColor color = dye.getDyeColor();
            setFeatherColor(color.getId());
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && getPersistentData().contains("WitcheryBodegaTicks")) {
            int remaining = getPersistentData().getIntOr("WitcheryBodegaTicks", 0) - 1;
            if (remaining <= 0) {
                discard();
                return;
            }
            getPersistentData().putInt("WitcheryBodegaTicks", remaining);
        }
        if (!level().isClientSide() && tickCount % ALERT_INTERVAL == 0 && isTame()) {
            if (getOwner() instanceof ServerPlayer owner) {
                AABB area = getBoundingBox().inflate(ALERT_RANGE);
                List<Monster> threats = level().getEntitiesOfClass(Monster.class, area);
                if (!threats.isEmpty()) {
                    owner.displayClientMessage(
                            Component.translatable("witchery.familiar.owl.warning", threats.size()), false);
                }
            }
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return super.getAmbientSoundInterval() * 2;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return WitcherySoundEvents.MOB_OWL_HOOT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WitcherySoundEvents.MOB_OWL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WitcherySoundEvents.MOB_OWL_HURT.get();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FeatherColor", getFeatherColor());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setFeatherColor(tag.getIntOr("FeatherColor", 0));
    }
}
