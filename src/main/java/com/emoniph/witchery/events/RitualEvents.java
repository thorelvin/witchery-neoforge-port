package com.emoniph.witchery.events;

import com.emoniph.witchery.blocks.entity.GrassperBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.ritual.RitualCircleDetector;
import com.emoniph.witchery.ritual.RitualExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@EventBusSubscriber
public class RitualEvents {
    private static final int VARIANT_RITUAL = 1;
    private static final int VARIANT_INFERNAL = 3;
    private static final double CHARGE_STONE_ITEM_RADIUS = 6.0D;
    private static final int GRASSPER_OFFERING_RADIUS = 5;

    /**
     * Right-clicking the golden center circle attempts ritual activation.
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
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        if (event.getItemStack().is(WitcheryItems.DEBUG_INSPECTOR.get())) return;

        BlockPos clickedPos = event.getPos();
        if (!level.getBlockState(clickedPos).is(WitcheryBlocks.CIRCLE.get())) return;

        BlockPos floorCenter = clickedPos.below();
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
        // Items mid staged-consumption from a rite that just fired must not be re-selected as a trigger.
        nearbyItems.removeIf(item -> RitualExecutor.isPendingSacrifice(level, item));

        ItemStack ingredient = ItemStack.EMPTY;
        IngredientSource ingredientSource = selectIngredientSource(level, floorCenter, nearbyItems);
        if (ingredientSource != null) {
            ingredient = ingredientSource.stack().copyWithCount(1); // only consume 1
        }

        RitualExecutor.ActivationResult activation = RitualExecutor.execute(player, level, floorCenter, presentCircles, ingredient);
        if (activation == RitualExecutor.ActivationResult.NO_MATCH) {
            // Source-faithful toggle (BlockCircle.deactivate): re-clicking a circle that is already
            // running an ongoing rite, with no new rite to start, ends that rite and refunds its
            // sacrifices. A valid new rite (e.g. an instant Summon Imp on a concentric/second ring)
            // matches above and runs instead, so ongoing rites can coexist with later castings.
            if (level instanceof ServerLevel serverLevel
                    && RitualExecutor.cancelActiveRitualsAt(serverLevel, floorCenter)) {
                level.playSound(null, floorCenter, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
                player.displayClientMessage(Component.translatable("witchery.ritual.cancelled"), false);
                return;
            }
            level.playSound(null, floorCenter, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.translatable("witchery.rite.unknownritual"), false);
            return;
        }
        if (!activation.consumeDroppedIngredient() || ingredientSource == null) {
            return;
        }

        ingredientSource.consumeOne();
    }

    private static IngredientSource selectIngredientSource(Level level, BlockPos floorCenter, List<ItemEntity> nearbyItems) {
        if (hasChargeStoneRings(level, floorCenter)) {
            ItemEntity attunedStone = nearestDroppedItem(level, floorCenter, CHARGE_STONE_ITEM_RADIUS, item ->
                    item.getItem().is(WitcheryItems.ATTUNED_STONE.get()));
            if (attunedStone != null) {
                return new IngredientSource(attunedStone, null);
            }
            GrassperBlockEntity grassper = nearestGrassper(floorCenter, level,
                    stack -> stack.is(WitcheryItems.ATTUNED_STONE.get()));
            if (grassper != null) {
                return new IngredientSource(null, grassper);
            }
        }

        if (isRitualRing(level, floorCenter, RitualCircleDetector.CircleSize.SMALL)) {
            IngredientSource obsidianSource = nearestOfferingSource(level, floorCenter, nearbyItems,
                    RitualEvents::isObsidianStack);
            if (obsidianSource != null) {
                return obsidianSource;
            }

            IngredientSource smallRitualSource = nearestOfferingSource(level, floorCenter, nearbyItems,
                    stack -> stack.is(Items.FEATHER)
                            || stack.is(Items.SLIME_BALL)
                            || stack.is(Items.BLAZE_POWDER)
                            || stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get())
                            // Sky's Wrath focus swords — preferred over the Wood Ash offering as trigger.
                            || stack.is(Items.WOODEN_SWORD)
                            || stack.is(Items.STONE_SWORD)
                            || stack.is(Items.IRON_SWORD));
            if (smallRitualSource != null) {
                return smallRitualSource;
            }
        }

        if (isRitualRing(level, floorCenter, RitualCircleDetector.CircleSize.MEDIUM)) {
            // The Rite of the Creakening's trigger (Creaking Heart) is NOT one of its own offerings,
            // so it must be preferred explicitly — otherwise a nearer offering would be selected as
            // the ingredient and the rite would silently fail to match.
            IngredientSource creakingHeartSource = nearestOfferingSource(level, floorCenter, nearbyItems,
                    stack -> stack.is(Blocks.CREAKING_HEART.asItem()));
            if (creakingHeartSource != null) {
                return creakingHeartSource;
            }

            IngredientSource mediumProtectionSource = nearestOfferingSource(level, floorCenter, nearbyItems,
                    RitualEvents::isObsidianStack);
            if (mediumProtectionSource != null) {
                return mediumProtectionSource;
            }
        }

        ItemEntity nearest = nearestDroppedItem(floorCenter, nearbyItems, item -> true);
        if (nearest != null) {
            return new IngredientSource(nearest, null);
        }

        GrassperBlockEntity grassper = nearestGrassper(floorCenter, level, stack -> true);
        return grassper == null ? null : new IngredientSource(null, grassper);
    }

    /** The seven items a Blight rite consumes as offerings (not its trigger — that is a Wither Rose). */
    private static IngredientSource nearestOfferingSource(Level level, BlockPos floorCenter, List<ItemEntity> nearbyItems,
                                                          Predicate<ItemStack> matcher) {
        ItemEntity dropped = nearestDroppedItem(floorCenter, nearbyItems, item -> matcher.test(item.getItem()));
        if (dropped != null) {
            return new IngredientSource(dropped, null);
        }
        GrassperBlockEntity grassper = nearestGrassper(floorCenter, level, matcher);
        return grassper == null ? null : new IngredientSource(null, grassper);
    }

    private static ItemEntity nearestDroppedItem(Level level, BlockPos floorCenter, double radius,
                                                 Predicate<ItemEntity> matcher) {
        Vec3 center = new Vec3(floorCenter.getX(), floorCenter.getY(), floorCenter.getZ());
        double radiusSq = radius * radius;
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(center, center).inflate(radius)).stream()
                .filter(matcher)
                .filter(item -> item.position().distanceToSqr(center) <= radiusSq)
                .min(Comparator.comparingDouble(item -> item.position().distanceToSqr(center)))
                .orElse(null);
    }

    private static boolean hasChargeStoneRings(Level level, BlockPos floorCenter) {
        return isRitualRing(level, floorCenter, RitualCircleDetector.CircleSize.SMALL)
                && isRitualRing(level, floorCenter, RitualCircleDetector.CircleSize.MEDIUM);
    }

    private static boolean isRitualRing(Level level, BlockPos floorCenter, RitualCircleDetector.CircleSize size) {
        return isVariantRing(level, floorCenter, size, VARIANT_RITUAL);
    }

    private static boolean isVariantRing(Level level, BlockPos floorCenter, RitualCircleDetector.CircleSize size, int variant) {
        return RitualCircleDetector.hasRing(level, floorCenter, size)
                && RitualCircleDetector.detectVariant(level, floorCenter, size) == variant;
    }

    private static boolean isObsidianStack(ItemStack stack) {
        return stack.is(Items.OBSIDIAN) || stack.is(Items.CRYING_OBSIDIAN);
    }

    private static ItemEntity nearestDroppedItem(BlockPos floorCenter, List<ItemEntity> nearbyItems,
                                                 Predicate<ItemEntity> matcher) {
        return nearbyItems.stream()
                .filter(matcher)
                .min(Comparator.comparingDouble(item -> item.position().distanceToSqr(
                        floorCenter.getX() + 0.5,
                        floorCenter.getY() + 1.0,
                        floorCenter.getZ() + 0.5)))
                .orElse(null);
    }

    private static GrassperBlockEntity nearestGrassper(BlockPos floorCenter, Level level, Predicate<ItemStack> matcher) {
        Vec3 centerPoint = Vec3.atCenterOf(floorCenter);
        BlockPos grassperLevel = floorCenter.above();
        GrassperBlockEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (int dx = -GRASSPER_OFFERING_RADIUS; dx <= GRASSPER_OFFERING_RADIUS; dx++) {
            for (int dz = -GRASSPER_OFFERING_RADIUS; dz <= GRASSPER_OFFERING_RADIUS; dz++) {
                BlockPos pos = grassperLevel.offset(dx, 0, dz);
                if (!(level.getBlockEntity(pos) instanceof GrassperBlockEntity grassper)) {
                    continue;
                }
                ItemStack stack = grassper.getItem();
                if (stack.isEmpty() || !matcher.test(stack)) {
                    continue;
                }
                double distance = Vec3.atCenterOf(pos).distanceToSqr(centerPoint);
                if (distance < closestDistance) {
                    closest = grassper;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    private record IngredientSource(ItemEntity itemEntity, GrassperBlockEntity grassper) {
        ItemStack stack() {
            return itemEntity != null ? itemEntity.getItem() : grassper.getItem();
        }

        void consumeOne() {
            // Stage the trigger like every other sacrifice — source consumes it one-per-20-ticks with a pop.
            if (itemEntity != null) {
                if (!itemEntity.isAlive()) {
                    return;
                }
                RitualExecutor.stageSacrificeConsumption(itemEntity);
                return;
            }
            RitualExecutor.stageSacrificeConsumption(grassper);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            RitualExecutor.tickActiveRituals(serverLevel);
            RitualExecutor.tickStagedSacrifices(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            RitualExecutor.prepareForServerStop(level);
        }
    }
}
