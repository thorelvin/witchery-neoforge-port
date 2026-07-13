package com.emoniph.witchery.entity.familiar;

import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Owl familiar — follows the owner and periodically warns them of nearby hostile mobs.
 */
public class OwlFamiliarEntity extends FamiliarEntity {

    private static final int ALERT_INTERVAL = 100; // ticks between scans
    private static final double ALERT_RANGE  = 20.0;

    public OwlFamiliarEntity(EntityType<? extends OwlFamiliarEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
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
}
