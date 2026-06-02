package com.emoniph.witchery.blocks;

import com.emoniph.witchery.item.PolynesiaCharmItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Catches the same small critter set as Witchery 1.7.10 and stores the catch in
 * block state so the existing caught textures can represent it in-world.
 */
public class CritterSnareBlock extends Block {
    public static final IntegerProperty CAUGHT = IntegerProperty.create("caught", 0, 9);

    private static final String CAUGHT_TAG = "Caught";
    private static final int EMPTY = 0;
    private static final int BAT = 1;
    private static final int SILVERFISH = 2;
    private static final int SLIME = 3;
    private static final int MAGMA_CUBE = 4;
    private static final int STOCK_FLAG = 8;
    private static final int STOCKED_BAT = STOCK_FLAG | BAT;
    private static final VoxelShape SHAPE = box(0.8, 0.0, 0.8, 15.2, 16.0, 15.2);

    public CritterSnareBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CAUGHT, EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CAUGHT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(CAUGHT, caughtFromStack(context.getItemInHand()));
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP)
                || level.getFluidState(pos.below()).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState,
                                     RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                @Nullable Orientation orientation, boolean movedByPiston) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (level.isClientSide() || state.getValue(CAUGHT) != EMPTY || entity == null || !entity.isAlive()) {
            return;
        }

        int caught = caughtType(entity);
        if (caught == EMPTY) {
            return;
        }

        level.setBlock(pos, state.setValue(CAUGHT, caught), 3);
        level.playSound(null, pos, caughtSound(caught), SoundSource.BLOCKS, 0.5F, soundPitch(level.getRandom()));
        entity.discard();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        int caught = state.getValue(CAUGHT);
        if (caught == EMPTY) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown()) {
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(CAUGHT, EMPTY), 3);
            releaseCritter(level, pos, player, caught);
            level.playSound(null, pos, caughtSound(caught), SoundSource.BLOCKS, 0.5F, soundPitch(level.getRandom()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int caught = state.getValue(CAUGHT);
        if (caught == EMPTY || random.nextInt(24) != 0) {
            return;
        }

        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                caughtSound(caught), SoundSource.BLOCKS, 0.5F, soundPitch(random), false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return createStackForState(state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(createStackForState(state));
    }

    private static int caughtType(Entity entity) {
        if (entity instanceof Bat bat) {
            return PolynesiaCharmItem.hasStockInventory(bat) ? STOCKED_BAT : BAT;
        }
        if (entity instanceof Silverfish) {
            return SILVERFISH;
        }
        if (entity instanceof MagmaCube cube && cube.getSize() == 1) {
            return MAGMA_CUBE;
        }
        if (entity instanceof Slime slime && slime.getSize() == 1) {
            return SLIME;
        }
        return EMPTY;
    }

    private static void releaseCritter(Level level, BlockPos pos, Player player, int caught) {
        int critter = critterFromCaught(caught);
        Entity entity = switch (critter) {
            case BAT -> EntityType.BAT.create(level, EntitySpawnReason.TRIGGERED);
            case SILVERFISH -> EntityType.SILVERFISH.create(level, EntitySpawnReason.TRIGGERED);
            case SLIME -> EntityType.SLIME.create(level, EntitySpawnReason.TRIGGERED);
            case MAGMA_CUBE -> EntityType.MAGMA_CUBE.create(level, EntitySpawnReason.TRIGGERED);
            default -> null;
        };
        if (entity == null) {
            return;
        }

        if (caught == STOCKED_BAT && entity instanceof Bat bat) {
            PolynesiaCharmItem.setEmptyStockInventory(level, bat);
        }

        if (critter == BAT) {
            entity.setPos(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
        } else {
            double x = player.getX() < pos.getX() ? pos.getX() - 0.5 : pos.getX() + 1.5;
            double z = player.getZ() < pos.getZ() ? pos.getZ() - 0.5 : pos.getZ() + 1.5;
            entity.setPos(x, player.getY() + 0.5, z);
        }

        level.addFreshEntity(entity);
    }

    private static SoundEvent caughtSound(int caught) {
        return switch (critterFromCaught(caught)) {
            case BAT -> SoundEvents.BAT_AMBIENT;
            case SILVERFISH -> SoundEvents.SILVERFISH_AMBIENT;
            case SLIME -> SoundEvents.SLIME_SQUISH_SMALL;
            case MAGMA_CUBE -> SoundEvents.MAGMA_CUBE_SQUISH_SMALL;
            default -> SoundEvents.WOOL_PLACE;
        };
    }

    private static float soundPitch(RandomSource random) {
        return 0.4F / (random.nextFloat() * 0.4F + 0.8F);
    }

    private static int critterFromCaught(int caught) {
        int critter = caught & 7;
        return critter >= EMPTY && critter <= MAGMA_CUBE ? critter : EMPTY;
    }

    private static boolean isValidCaught(int caught) {
        return caught >= EMPTY && caught <= MAGMA_CUBE || caught == STOCKED_BAT;
    }

    private ItemStack createStackForState(BlockState state) {
        ItemStack stack = new ItemStack(asItem());
        int caught = state.getValue(CAUGHT);
        if (caught != EMPTY) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(CAUGHT_TAG, caught));
        }
        return stack;
    }

    private static int caughtFromStack(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int caught = tag.getIntOr(CAUGHT_TAG, EMPTY);
        return isValidCaught(caught) ? caught : EMPTY;
    }
}
