package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.init.WitcheryBlockTags;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.WaystoneItem;
import com.emoniph.witchery.ritual.RitualCircleDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;
import java.util.Optional;

/**
 * Restores the old EntityItemWaystone passive circle behavior for dropped blank Waystones.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class WaystoneItemEntityEvents {
    private static final int CHECK_START_AGE = 40;
    private static final int CHECK_INTERVAL = 40;
    private static final int BLOODED_POWER_COST = 4000;

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)
                || !(itemEntity.level() instanceof ServerLevel level)
                || itemEntity.isRemoved()
                || itemEntity.getAge() <= CHECK_START_AGE
                || itemEntity.getAge() % CHECK_INTERVAL != 0) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (!WaystoneItem.isBlankWaystone(stack)) {
            return;
        }

        Optional<BlockPos> center = findSmallOtherwhereCircle(level, itemEntity.getOnPos());
        center.ifPresent(blockPos -> convertBlankWaystone(level, itemEntity, blockPos));
    }

    private static Optional<BlockPos> findSmallOtherwhereCircle(ServerLevel level, BlockPos near) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                BlockPos candidate = near.offset(dx, 0, dz);
                if (RitualCircleDetector.hasRing(level, candidate, RitualCircleDetector.CircleSize.SMALL)
                        && RitualCircleDetector.detectVariant(level, candidate, RitualCircleDetector.CircleSize.SMALL) == 2) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private static void convertBlankWaystone(ServerLevel level, ItemEntity itemEntity, BlockPos center) {
        ItemStack original = itemEntity.getItem();
        Optional<LivingEntity> target = findTarget(level, center);
        if (target.isPresent()) {
            AltarBlockEntity altar = findNearbyAltar(level, center, 16);
            if (altar == null || !altar.consumePower(BLOODED_POWER_COST)) {
                fail(level, center);
                return;
            }
            spawnBoundWaystone(level, itemEntity, target.get());
            spawnRemainder(level, itemEntity, original.getCount() - 1);
        } else {
            int converted = Math.min(original.getCount(), 8);
            for (int i = 0; i < converted; i++) {
                spawnLocationWaystone(level, itemEntity, center);
            }
            spawnRemainder(level, itemEntity, original.getCount() - converted);
        }

        clearCircle(level, center);
        itemEntity.discard();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ(),
                3, 0.25, 0.25, 0.25, 0.0);
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.45f, 1.2f);
        WitcheryMod.LOGGER.info("Dropped Waystone at {} bound itself through an Otherwhere circle.", center);
    }

    private static Optional<LivingEntity> findTarget(ServerLevel level, BlockPos center) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        AABB bounds = new AABB(center).inflate(2.0, 2.0, 2.0);
        Optional<Player> player = level.getEntitiesOfClass(Player.class, bounds,
                        entity -> horizontalDistanceSqr(entity, centerPoint) <= 4.0D)
                .stream()
                .min(Comparator.comparingDouble(entity -> horizontalDistanceSqr(entity, centerPoint)));
        if (player.isPresent()) {
            return Optional.of(player.get());
        }
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                        entity -> !(entity instanceof Player) && horizontalDistanceSqr(entity, centerPoint) <= 4.0D)
                .stream()
                .min(Comparator.comparingDouble(entity -> horizontalDistanceSqr(entity, centerPoint)));
    }

    private static double horizontalDistanceSqr(LivingEntity entity, Vec3 centerPoint) {
        double dx = entity.getX() - centerPoint.x;
        double dz = entity.getZ() - centerPoint.z;
        return dx * dx + dz * dz;
    }

    private static void spawnBoundWaystone(ServerLevel level, ItemEntity source, LivingEntity target) {
        ItemStack blooded = new ItemStack(WitcheryItems.BLOODED_WAYSTONE.get());
        WaystoneItem.bindToEntity(blooded, target);
        spawnStack(level, source, blooded);
    }

    private static void spawnLocationWaystone(ServerLevel level, ItemEntity source, BlockPos center) {
        ItemStack bound = new ItemStack(WitcheryItems.BOUND_WAYSTONE.get());
        WaystoneItem.bindTo(bound, center, level.dimension());
        spawnStack(level, source, bound);
    }

    private static void spawnRemainder(ServerLevel level, ItemEntity source, int count) {
        if (count <= 0) {
            return;
        }
        spawnStack(level, source, new ItemStack(WitcheryItems.WAYSTONE.get(), count));
    }

    private static void spawnStack(ServerLevel level, ItemEntity source, ItemStack stack) {
        ItemEntity drop = new ItemEntity(level, source.getX(), source.getY(), source.getZ(), stack);
        drop.setDeltaMovement(source.getDeltaMovement().scale(0.2).add(0.0, 0.15, 0.0));
        level.addFreshEntity(drop);
    }

    private static AltarBlockEntity findNearbyAltar(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos checkPos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (level.getBlockEntity(checkPos) instanceof AltarBlockEntity altar && altar.hasValidStructure()) {
                return altar;
            }
        }
        return null;
    }

    private static void clearCircle(ServerLevel level, BlockPos center) {
        for (BlockPos pos : RitualCircleDetector.ringPositions(center, RitualCircleDetector.CircleSize.SMALL)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(WitcheryBlockTags.CHALK_CIRCLES)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void fail(ServerLevel level, BlockPos center) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                center.getX() + 0.5, center.getY() + 1.2, center.getZ() + 0.5,
                12, 0.35, 0.2, 0.35, 0.02);
        level.playSound(null, center, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.7f);
    }
}
