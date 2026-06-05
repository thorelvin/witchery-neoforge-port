package com.emoniph.witchery.blocks;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntityTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Garlic Garland - a decoration that repels vampires who touch it.
 * Recipe: 5 garlic + 3 string.
 */
public class GarlicGarlandBlock extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape DEFAULT_SHAPE = Block.box(0.0, 0.0, 15.2, 16.0, 16.0, 16.0);
    private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 0.0, 15.2, 16.0, 16.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 0.8);
    private static final VoxelShape WEST_SHAPE = Block.box(15.2, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 0.0, 0.8, 16.0, 16.0);
    private static final double REPEL_RADIUS = 6.0;
    private static final double REPEL_RADIUS_FOURTH = REPEL_RADIUS * REPEL_RADIUS * REPEL_RADIUS * REPEL_RADIUS;
    private static final double REPEL_SCALE = REPEL_RADIUS * REPEL_RADIUS * REPEL_RADIUS;

    public GarlicGarlandBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // Source metadata is chosen from player yaw, but the hanging model sits on the back face of that state.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        return switch (facing) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> DEFAULT_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return getShape(state, level, pos, CollisionContext.empty());
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (isVampire(entity)) {
            repelFromGarland(pos, entity);
        }
    }

    private static boolean isVampire(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            return data.isVampire();
        }
        if (entity instanceof Player) {
            return false;
        }
        return entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }

    private static void repelFromGarland(BlockPos pos, Entity entity) {
        double dx = pos.getX() - entity.getX();
        double dy = pos.getY() - entity.getY();
        double dz = pos.getZ() - entity.getZ();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        double falloff = distanceSquared * distanceSquared;
        if (falloff <= 0.0001 || falloff > REPEL_RADIUS_FOURTH) {
            return;
        }

        double factor = -(0.02 / falloff) * REPEL_SCALE;
        double pushX = clamp(dx * factor, -0.22, 0.22);
        double pushY = dy * factor;
        double pushZ = clamp(dz * factor, -0.22, 0.22);
        if (pushY > 0.2 || pushY < -0.1) {
            pushY = 0.12;
        }

        entity.push(pushX, pushY, pushZ);
        entity.hurtMarked = true;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
