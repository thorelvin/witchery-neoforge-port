package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.BrewOfInkItem;
import com.emoniph.witchery.item.BrewOfSproutingItem;
import com.emoniph.witchery.item.BrewOfThornsItem;
import com.emoniph.witchery.item.BrewOfWebsItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Source BlockPlantMine parity: disguised flowers/shrub that disappear and
 * trigger one of four brew-style trap effects when anything touches them.
 */
public class PlantMineBlock extends Block {
    public static final EnumProperty<PlantMineDisguise> DISGUISE =
            EnumProperty.create("disguise", PlantMineDisguise.class);
    public static final EnumProperty<PlantMineEffect> EFFECT =
            EnumProperty.create("effect", PlantMineEffect.class);

    private static final VoxelShape SHAPE = Block.box(5.8, 0.0, 5.8, 10.2, 8.6, 10.2);

    public PlantMineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(DISGUISE, PlantMineDisguise.ROSE)
                .setValue(EFFECT, PlantMineEffect.WEBS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISGUISE, EFFECT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState soil = level.getBlockState(below);
        return (level.getRawBrightness(pos, 0) >= 8 || level.canSeeSky(pos))
                && canPlaceBlockOn(soil, level, below);
    }

    private static boolean canPlaceBlockOn(BlockState soil, LevelReader level, BlockPos pos) {
        return soil.is(Blocks.GRASS_BLOCK)
                || soil.is(Blocks.DIRT)
                || soil.is(Blocks.COARSE_DIRT)
                || soil.is(Blocks.PODZOL)
                || soil.is(Blocks.FARMLAND)
                || soil.is(Blocks.MYCELIUM)
                || soil.is(Blocks.SAND)
                || soil.is(Blocks.SOUL_SAND);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                @Nullable Orientation orientation, boolean movedByPiston) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, false);
            return;
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, false);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (!(level instanceof ServerLevel serverLevel) || entity == null) {
            return;
        }

        level.removeBlock(pos, false);
        serverLevel.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.5F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                pos.getX() + 0.5D, pos.getY() + 0.05D, pos.getZ() + 0.5D,
                16, 0.5D, 1.0D, 0.5D, 0.02D);

        switch (state.getValue(EFFECT)) {
            case WEBS -> BrewOfWebsItem.explodeWeb(serverLevel, pos, false);
            case INK -> BrewOfInkItem.explodeInk(serverLevel,
                    pos.getX(), pos.getY(), pos.getZ(),
                    entity instanceof LivingEntity living ? living : null,
                    new AABB(pos), false, false);
            case THORNS -> BrewOfThornsItem.plantCactus(serverLevel, pos, 4, false);
            case SPROUTING -> BrewOfSproutingItem.growBranch(serverLevel, pos, Direction.UP, 10, false);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return stackFor(state.getValue(DISGUISE), state.getValue(EFFECT));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    public static ItemStack stackFor(PlantMineDisguise disguise, PlantMineEffect effect) {
        ItemStack stack = WitcheryItems.PLANT_MINE.get().getDefaultInstance();
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
                .with(DISGUISE, disguise)
                .with(EFFECT, effect));
        return stack;
    }

    public enum PlantMineDisguise implements StringRepresentable {
        ROSE,
        DANDELION,
        SHRUB;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum PlantMineEffect implements StringRepresentable {
        WEBS,
        INK,
        THORNS,
        SPROUTING;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
