package com.emoniph.witchery.events;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.ritual.RitualCircleDetector;
import com.emoniph.witchery.ritual.RitualExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber
public class RitualEvents {

    /**
     * Right-clicking any block with the arthana attempts ritual activation.
     *
     * Dispatch order:
     *  1. Detect every complete chalk ring at the center
     *  2. Scan for a dropped ItemEntity within 1.5 blocks of center — used as the rite ingredient
     *  3. Consume the ingredient entity, then execute the matching rite
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        if (!event.getItemStack().is(WitcheryItems.ARTHANA.get())) return;

        BlockPos floorCenter = event.getPos();
        List<RitualCircleDetector.DetectionResult> presentCircles = RitualCircleDetector.detectPresent(level, floorCenter);

        if (presentCircles.isEmpty()) {
            if (!player.isCrouching()) {
                player.displayClientMessage(Component.translatable("witchery.ritual.no_circle"), false);
            }
            return;
        }

        event.setCanceled(true);

        // Scan for a dropped ingredient item at the center (1.5-block radius, same Y ± 1)
        AABB ingredientArea = new AABB(floorCenter.above()).inflate(1.5, 1.0, 1.5);
        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, ingredientArea);

        ItemStack ingredient = ItemStack.EMPTY;
        ItemEntity ingredientEntity = null;
        if (!nearbyItems.isEmpty()) {
            ingredientEntity = nearbyItems.stream()
                    .min(Comparator.comparingDouble(item -> item.position().distanceToSqr(
                            floorCenter.getX() + 0.5,
                            floorCenter.getY() + 1.0,
                            floorCenter.getZ() + 0.5)))
                    .orElse(null);
        }
        if (ingredientEntity != null) {
            ingredient = ingredientEntity.getItem().copyWithCount(1); // only consume 1
        }

        boolean consumeIngredient = RitualExecutor.execute(player, level, floorCenter, presentCircles, ingredient);
        if (!consumeIngredient || ingredientEntity == null) {
            return;
        }

        // Consume one ingredient from the entity (or discard if stack size = 1)
        if (ingredientEntity.isAlive()) {
            ItemStack current = ingredientEntity.getItem();
            if (current.getCount() > 1) {
                current.shrink(1);
                ingredientEntity.setItem(current);
            } else {
                ingredientEntity.discard();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            RitualExecutor.tickActiveRituals(serverLevel);
        }
    }
}
