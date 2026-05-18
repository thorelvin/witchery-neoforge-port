package com.emoniph.witchery.entity.familiar;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Toad familiar — follows the owner and passively collects nearby dropped item entities.
 */
public class ToadFamiliarEntity extends FamiliarEntity {

    private static final int COLLECT_INTERVAL = 200;
    private static final double COLLECT_RANGE  = 6.0;

    public ToadFamiliarEntity(EntityType<? extends ToadFamiliarEntity> type, Level level) {
        super(type, level);
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
}
