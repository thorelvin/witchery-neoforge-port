package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

/** A cursed villager that retains its ordinary villager life between full-moon transformations. */
public class WereVillagerEntity extends Villager {
    private boolean infectious;

    public WereVillagerEntity(EntityType<? extends WereVillagerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Villager.createAttributes();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 100 == 3 && !isBaby()
                && !hasEffect(WitcheryMobEffects.WOLFSBANE) && isFullMoonNight()) {
            VillagerWerewolfConversion.convertToWolfman((ServerLevel) level(), this);
        }
    }

    private boolean isFullMoonNight() {
        ServerLevel level = (ServerLevel) level();
        long dayTime = level.getDayTime() % 24000L;
        return (level.getDayTime() / 24000L) % 8L == 0 && dayTime > 13000L && dayTime < 23000L;
    }

    public boolean isInfectious() {
        return infectious;
    }

    public void setInfectious(boolean infectious) {
        this.infectious = infectious;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Infectious", infectious);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        infectious = tag.getBooleanOr("Infectious", false);
    }
}
