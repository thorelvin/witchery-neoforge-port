package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Brew of Vines - source-style side-hit vine strips in practical item form. */
public class BrewOfVinesItem extends BrewItem {
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public BrewOfVinesItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isVertical()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        int placed = growVines(serverLevel, context.getClickedPos(), face, player, hasBrewMastery(player));
        if (placed <= 0) {
            return InteractionResult.PASS;
        }

        consumeBrew(player, context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean brewMastery = hasBrewMastery(player);
        BlockPos center = player.blockPosition();
        int range = brewMastery ? 4 : 3;
        for (Direction wallDirection : HORIZONTAL_DIRECTIONS) {
            Direction faceTowardPlayer = wallDirection.getOpposite();
            for (int distance = 1; distance <= range; distance++) {
                BlockPos support = center.relative(wallDirection, distance);
                if (canSupportVine(serverLevel.getBlockState(support))
                        && growVines(serverLevel, support, faceTowardPlayer, player, brewMastery) > 0) {
                    return;
                }
            }
        }
    }

    private static boolean hasBrewMastery(Player player) {
        return player != null && FamiliarHelper.hasActiveToadFamiliar(player);
    }

    private static int growVines(ServerLevel level, BlockPos clickedPos, Direction face, Player player, boolean enhanced) {
        int placed = 0;

        BlockPos support = clickedPos;
        if (!canTargetVine(level.getBlockState(support.relative(face)))
                || !canSupportVine(level.getBlockState(support))) {
            support = support.relative(face);
        }

        while (support.getY() > level.getMinY()
                && canTargetVine(level.getBlockState(support.relative(face)))
                && canSupportVine(level.getBlockState(support))) {
            if (placeVine(level, support, face, player)) {
                placed++;
            }

            support = support.below();
            if (!canTargetVine(level.getBlockState(support.relative(face)))
                    || !canSupportVine(level.getBlockState(support))) {
                support = support.relative(face);
                if (enhanced
                        && (!canTargetVine(level.getBlockState(support.relative(face)))
                        || !canSupportVine(level.getBlockState(support)))) {
                    support = support.relative(face);
                }
            }
        }

        support = clickedPos.above();
        if (!canSupportVine(level.getBlockState(support))) {
            support = support.relative(face.getOpposite());
            if (enhanced && !canSupportVine(level.getBlockState(support))) {
                support = support.relative(face.getOpposite());
            }
        }

        while (support.getY() < level.getMaxY()
                && canTargetVine(level.getBlockState(support.relative(face)))
                && canSupportVine(level.getBlockState(support))) {
            if (placeVine(level, support, face, player)) {
                placed++;
            }

            support = support.above();
            if (!canSupportVine(level.getBlockState(support))) {
                support = support.relative(face.getOpposite());
                if (enhanced && !canSupportVine(level.getBlockState(support))) {
                    support = support.relative(face.getOpposite());
                }
            }
        }

        if (placed > 0) {
            level.playSound(null, clickedPos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return placed;
    }

    private static boolean placeVine(ServerLevel level, BlockPos support, Direction face, Player player) {
        BlockPos vinePos = support.relative(face);
        BlockState current = level.getBlockState(vinePos);
        if (!canTargetVine(current) || !canSupportVine(level.getBlockState(support))) {
            return false;
        }

        BooleanProperty side = vinePropertyFor(face.getOpposite());
        BlockState vine = current.is(Blocks.VINE) ? current : Blocks.VINE.defaultBlockState();
        if (vine.hasProperty(side) && vine.getValue(side)) {
            return false;
        }

        vine = vine.setValue(side, true);
        if (!vine.canSurvive(level, vinePos)) {
            return false;
        }
        if (!(current.isAir()
                ? WitcheryTerrainProtection.canPlace(player, level, vinePos)
                : WitcheryTerrainProtection.canTransform(player, level, vinePos))) {
            return false;
        }

        level.setBlock(vinePos, vine, 3);
        level.sendParticles(ParticleTypes.POOF,
                vinePos.getX() + 0.5D, vinePos.getY() + 0.5D, vinePos.getZ() + 0.5D,
                6, 0.2D, 0.2D, 0.2D, 0.02D);
        return true;
    }

    private static boolean canSupportVine(BlockState state) {
        return state.isSolid();
    }

    private static boolean canTargetVine(BlockState state) {
        return state.is(Blocks.VINE)
                || (state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && (!state.isSolid() || state.is(BlockTags.LEAVES)));
    }

    private static BooleanProperty vinePropertyFor(Direction supportDirection) {
        return switch (supportDirection) {
            case NORTH -> VineBlock.NORTH;
            case SOUTH -> VineBlock.SOUTH;
            case WEST -> VineBlock.WEST;
            case EAST -> VineBlock.EAST;
            default -> throw new IllegalArgumentException("Vines only attach to horizontal faces");
        };
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
