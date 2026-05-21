package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class WitchCauldronBlock extends Block {

    public static final MapCodec<WitchCauldronBlock> CODEC = simpleCodec(WitchCauldronBlock::new);
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_CAULDRON;
    public static final int MAX_FILL_LEVEL = 3;
    private static final VoxelShape INSIDE = box(2.0, 4.0, 2.0, 14.0, 16.0, 14.0);
    private static final VoxelShape SHAPE = Shapes.join(
            Shapes.block(),
            Shapes.or(box(0.0, 0.0, 4.0, 16.0, 3.0, 12.0), box(4.0, 0.0, 0.0, 12.0, 3.0, 16.0), box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0), INSIDE),
            BooleanOp.ONLY_FIRST
    );
    private static final float RAIN_FILL_CHANCE = 0.05F;
    private static final int CHALK_ALTAR_RADIUS = 8;

    public WitchCauldronBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int fillLevel = state.getValue(LEVEL);
        if (stack.is(Items.WATER_BUCKET)) {
            if (fillLevel >= MAX_FILL_LEVEL) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                player.awardStat(Stats.FILL_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                setFillLevel(state, level, pos, MAX_FILL_LEVEL);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(Items.BUCKET)) {
            if (fillLevel < MAX_FILL_LEVEL) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                setFillLevel(state, level, pos, 0);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(Items.GLASS_BOTTLE)) {
            if (fillLevel <= 0) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                lowerFillLevel(state, level, pos);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.SUCCESS;
        }

        PotionContents potioncontents = stack.get(DataComponents.POTION_CONTENTS);
        if (stack.is(Items.POTION) && potioncontents != null && potioncontents.is(Potions.WATER)) {
            if (fillLevel >= MAX_FILL_LEVEL) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
                setFillLevel(state, level, pos, Math.min(MAX_FILL_LEVEL, fillLevel + 1));
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.SUCCESS;
        }

        InteractionResult chalkResult = tryUpgradeChalk(stack, state, level, pos, player);
        if (chalkResult.consumesAction()) {
            return chalkResult;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        if (precipitation != Biome.Precipitation.RAIN || state.getValue(LEVEL) >= MAX_FILL_LEVEL || level.getRandom().nextFloat() >= RAIN_FILL_CHANCE) {
            return;
        }
        setFillLevel(state, level, pos, state.getValue(LEVEL) + 1);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier _effectApplier, boolean _isMoved) {
        if (level.isClientSide() || state.getValue(LEVEL) <= 0 || !entity.isOnFire() || !isEntityInsideContent(state, pos, entity)) {
            return;
        }
        entity.clearFire();
        if (entity.mayInteract((ServerLevel) level, pos)) {
            lowerFillLevel(state, level, pos);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LEVEL) <= 0 || !isHeated(level.getBlockState(pos.below()))) {
            return;
        }

        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double y = pos.getY() + getContentHeight(state) + 0.02;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;

        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.01, 0.0);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, y + 0.12, pos.getZ() + 0.5, 0.0, 0.015, 0.0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(Items.CAULDRON);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.CAULDRON));
    }

    public static void lowerFillLevel(BlockState state, Level level, BlockPos pos) {
        setFillLevel(state, level, pos, Math.max(0, state.getValue(LEVEL) - 1));
    }

    private static void setFillLevel(BlockState state, Level level, BlockPos pos, int amount) {
        BlockState nextState = state.setValue(LEVEL, amount);
        level.setBlockAndUpdate(pos, nextState);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(nextState));
    }

    private double getContentHeight(BlockState state) {
        int level = state.getValue(LEVEL);
        return level <= 0 ? 0.0 : (6.0 + level * 3.0) / 16.0;
    }

    private boolean isEntityInsideContent(BlockState state, BlockPos pos, Entity entity) {
        return entity.getY() < pos.getY() + getContentHeight(state) && entity.getBoundingBox().maxY > pos.getY() + 0.25;
    }

    private static boolean isHeated(BlockState state) {
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
            return true;
        }
        return state.hasProperty(CampfireBlock.LIT) && state.is(BlockTags.CAMPFIRES) && state.getValue(CampfireBlock.LIT);
    }

    private InteractionResult tryUpgradeChalk(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (!stack.is(WitcheryItems.CHALK.get()) || state.getValue(LEVEL) <= 0 || !isHeated(level.getBlockState(pos.below()))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ChalkUpgradeMatch match = findChalkUpgradeMatch(level, pos);
        if (match == null) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.chalk.support"), false);
            return InteractionResult.CONSUME;
        }

        AltarBlockEntity altar = findNearbyAltar(level, pos, CHALK_ALTAR_RADIUS);
        if (altar == null || !altar.hasValidStructure() || altar.getPower() < match.powerCost()) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.chalk.no_power"), false);
            return InteractionResult.CONSUME;
        }
        if (!altar.consumePower(match.powerCost())) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.chalk.no_power"), false);
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        for (ItemEntity offering : match.offerings()) {
            ItemStack offeringStack = offering.getItem();
            offeringStack.shrink(1);
            if (offeringStack.isEmpty()) {
                offering.discard();
            } else {
                offering.setItem(offeringStack);
            }
        }
        lowerFillLevel(state, level, pos);

        ItemEntity resultEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, new ItemStack(match.result()));
        resultEntity.setDefaultPickUpDelay();
        level.addFreshEntity(resultEntity);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5F, 0.95F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        player.displayClientMessage(Component.translatable(match.successKey()), false);
        return InteractionResult.CONSUME;
    }

    @Nullable
    private ChalkUpgradeMatch findChalkUpgradeMatch(Level level, BlockPos pos) {
        Optional<ItemEntity> goldenRoot = findCauldronOffering(level, pos, WitcheryItems.MANDRAKE_ROOT.get());
        Optional<ItemEntity> goldenNugget = findCauldronOffering(level, pos, Items.GOLD_NUGGET);
        if (goldenRoot.isPresent() && goldenNugget.isPresent()) {
            return new ChalkUpgradeMatch(
                    WitcheryItems.CHALK_GOLDEN.get(),
                    2870,
                    List.of(goldenRoot.get(), goldenNugget.get()),
                    "witchery.cauldron.chalk.golden.success");
        }

        Optional<ItemEntity> otherwhereWart = findCauldronOffering(level, pos, Items.NETHER_WART);
        Optional<ItemEntity> otherwhereTear = findCauldronOffering(level, pos, WitcheryItems.TEAR_OF_GODDESS.get());
        Optional<ItemEntity> otherwherePearl = findCauldronOffering(level, pos, Items.ENDER_PEARL);
        if (otherwhereWart.isPresent() && otherwhereTear.isPresent() && otherwherePearl.isPresent()) {
            return new ChalkUpgradeMatch(
                    WitcheryItems.CHALK_OTHERWHERE.get(),
                    4410,
                    List.of(otherwhereWart.get(), otherwhereTear.get(), otherwherePearl.get()),
                    "witchery.cauldron.chalk.otherwhere.success");
        }

        Optional<ItemEntity> infernalWart = findCauldronOffering(level, pos, Items.NETHER_WART);
        Optional<ItemEntity> infernalBlaze = findCauldronOffering(level, pos, Items.BLAZE_POWDER);
        if (infernalWart.isPresent() && infernalBlaze.isPresent()) {
            return new ChalkUpgradeMatch(
                    WitcheryItems.CHALK_INFERNAL.get(),
                    3150,
                    List.of(infernalWart.get(), infernalBlaze.get()),
                    "witchery.cauldron.chalk.infernal.success");
        }

        return null;
    }

    private static Optional<ItemEntity> findCauldronOffering(Level level, BlockPos pos, Item item) {
        AABB bowl = new AABB(
                pos.getX() + 0.125, pos.getY() + 0.2, pos.getZ() + 0.125,
                pos.getX() + 0.875, pos.getY() + 1.0, pos.getZ() + 0.875);
        return level.getEntitiesOfClass(ItemEntity.class, bowl).stream()
                .filter(entity -> entity.isAlive())
                .filter(entity -> entity.getItem().is(item))
                .findFirst();
    }

    @Nullable
    private static AltarBlockEntity findNearbyAltar(Level world, BlockPos center, int radius) {
        for (BlockPos bp : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (world.getBlockEntity(bp) instanceof AltarBlockEntity altar) {
                return altar;
            }
        }
        return null;
    }

    private record ChalkUpgradeMatch(Item result, int powerCost, List<ItemEntity> offerings, String successKey) {}
}
