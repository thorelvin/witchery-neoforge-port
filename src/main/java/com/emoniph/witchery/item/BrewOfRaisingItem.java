package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Brew of Raising - source-style grave raising in practical item form. */
public class BrewOfRaisingItem extends BrewItem {
    private static final String SOURCE_ENSLAVER_TAG = "WITCEnslaverName";
    private static final String PORT_ENSLAVER_UUID_TAG = "witchery_enslaver_uuid";

    public BrewOfRaisingItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        raiseDead(serverLevel, context.getClickedPos(), context.getPlayer());
        consumeBrew(context.getPlayer(), context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos floor = findNearestFloor(serverLevel, player.blockPosition());
        if (floor != null) {
            raiseDead(serverLevel, floor, player);
        }
    }

    private static void raiseDead(ServerLevel level, BlockPos clickedPos, Player raiser) {
        BlockPos floor = level.getBlockState(clickedPos).isSolid() ? clickedPos : clickedPos.below();
        raiseUndead(level, floor, raiser);

        int extraCount = 0;
        double chance = level.random.nextDouble();
        if (chance < 0.1D) {
            extraCount = 2;
        } else if (chance < 0.4D) {
            extraCount = 1;
        }

        for (int i = 0; i < extraCount; i++) {
            BlockPos nearby = floor.offset(level.random.nextInt(6) - 2, 0, level.random.nextInt(6) - 2);
            BlockPos extraFloor = findNearbyFloor(level, nearby, floor.getY());
            if (extraFloor != null) {
                raiseUndead(level, extraFloor, raiser);
            }
        }
    }

    private static BlockPos findNearbyFloor(ServerLevel level, BlockPos base, int floorY) {
        for (int dy = -6; dy < 6; dy++) {
            BlockPos pos = new BlockPos(base.getX(), floorY - dy, base.getZ());
            if (level.getBlockState(pos).isSolid()) {
                return pos;
            }
        }
        return null;
    }

    private static BlockPos findNearestFloor(ServerLevel level, BlockPos center) {
        if (level.getBlockState(center).isSolid()) {
            return center;
        }
        for (int dy = 1; dy <= 6; dy++) {
            BlockPos below = center.below(dy);
            if (level.getBlockState(below).isSolid()) {
                return below;
            }
        }
        return null;
    }

    private static void raiseUndead(ServerLevel level, BlockPos floor, Player raiser) {
        BlockPos spawnPos = isSourceGraveFloor(level.getBlockState(floor)) ? floor : floor.above();
        if (!clearForSpawn(level, spawnPos) || !clearForSpawn(level, spawnPos.above())) {
            return;
        }

        level.sendParticles(ParticleTypes.SMOKE,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D,
                16, 0.35D, 0.35D, 0.35D, 0.02D);

        Mob undead = createUndeadCreature(level);
        if (undead == null) {
            return;
        }

        undead.setPos(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D);
        undead.setYRot(level.random.nextFloat() * 360.0F);
        undead.setXRot(0.0F);
        undead.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.TRIGGERED, null);
        undead.setPersistenceRequired();
        if (raiser != null) {
            undead.getPersistentData().putString(SOURCE_ENSLAVER_TAG, raiser.getName().getString());
            undead.getPersistentData().putString(PORT_ENSLAVER_UUID_TAG, raiser.getUUID().toString());
            undead.setTarget(null);
        }
        level.addFreshEntity(undead);
    }

    private static boolean clearForSpawn(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.liquid() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        level.destroyBlock(pos, false);
        return true;
    }

    private static boolean isSourceGraveFloor(BlockState state) {
        return state.is(Blocks.DIRT)
                || state.is(Blocks.STONE)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SAND)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.GRAVEL);
    }

    private static Mob createUndeadCreature(ServerLevel level) {
        double value = level.random.nextDouble();
        EntityType<? extends Mob> type;
        if (value < 0.6D) {
            type = EntityType.ZOMBIE;
        } else if (value < 0.97D) {
            type = EntityType.SKELETON;
        } else {
            type = EntityType.ZOMBIFIED_PIGLIN;
        }
        return type.create(level, EntitySpawnReason.TRIGGERED);
    }

    private static void consumeBrew(Player player, InteractionHand hand, ItemStack stack) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (player == null || !player.getInventory().add(bottle)) {
            if (player != null) {
                player.drop(bottle, false);
            }
        }
        if (player != null) {
            player.setItemInHand(hand, stack);
        }
    }
}
