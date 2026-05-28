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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class WitchCauldronBlock extends Block {

    public static final MapCodec<WitchCauldronBlock> CODEC = simpleCodec(WitchCauldronBlock::new);
    public static final int MAX_FILL_LEVEL = 3;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_FILL_LEVEL);
    private static final VoxelShape SHAPE = Shapes.or(
            box(3.4, 1.6, 3.4, 5.4, 3.2, 5.4),
            box(2.4, 0.8, 2.4, 4.4, 2.2, 4.4),
            box(1.4, 0.0, 1.4, 3.4, 1.2, 3.4),
            box(10.6, 1.6, 3.4, 12.6, 3.2, 5.4),
            box(11.6, 0.8, 2.4, 13.6, 2.2, 4.4),
            box(12.6, 0.0, 1.4, 14.6, 1.2, 3.4),
            box(3.4, 1.6, 10.6, 5.4, 3.2, 12.6),
            box(2.4, 0.8, 11.6, 4.4, 2.2, 13.6),
            box(1.4, 0.0, 12.6, 3.4, 1.2, 14.6),
            box(10.6, 1.6, 10.6, 12.6, 3.2, 12.6),
            box(11.6, 0.8, 11.6, 13.6, 2.2, 13.6),
            box(12.6, 0.0, 12.6, 14.6, 1.2, 14.6),
            box(3.0, 1.0, 3.0, 13.0, 2.0, 13.0),
            box(3.0, 2.0, 3.0, 13.0, 3.0, 13.0),
            box(0.0, 2.0, 0.0, 16.0, 10.0, 3.0),
            box(0.0, 2.0, 13.0, 16.0, 10.0, 16.0),
            box(0.0, 2.0, 3.0, 3.0, 10.0, 13.0),
            box(13.0, 2.0, 3.0, 16.0, 10.0, 13.0),
            box(1.0, 10.0, 1.0, 15.0, 12.0, 3.0),
            box(1.0, 10.0, 13.0, 15.0, 12.0, 15.0),
            box(1.0, 10.0, 3.0, 3.0, 12.0, 13.0),
            box(13.0, 10.0, 3.0, 15.0, 12.0, 13.0),
            box(0.0, 12.0, 0.0, 16.0, 14.0, 3.0),
            box(0.0, 12.0, 13.0, 16.0, 14.0, 16.0),
            box(0.0, 12.0, 3.0, 3.0, 14.0, 13.0),
            box(13.0, 12.0, 3.0, 16.0, 14.0, 13.0)
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

        InteractionResult extremisResult = tryUseMutandisExtremisInCauldron(stack, state, level, pos, player);
        if (extremisResult.consumesAction()) {
            return extremisResult;
        }

        InteractionResult mutandisOfferingResult = tryOfferMutandisIngredient(stack, state, level, pos, player);
        if (mutandisOfferingResult.consumesAction()) {
            return mutandisOfferingResult;
        }

        InteractionResult mutandisResult = tryBrewMutandis(stack, state, level, pos, player);
        if (mutandisResult.consumesAction()) {
            return mutandisResult;
        }

        InteractionResult mutandisExtremisResult = tryBrewMutandisExtremis(stack, state, level, pos, player);
        if (mutandisExtremisResult.consumesAction()) {
            return mutandisExtremisResult;
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
        return switch (level) {
            case 1 -> 5.0 / 16.0;
            case 2 -> 9.0 / 16.0;
            case 3 -> 13.0 / 16.0;
            default -> 0.0;
        };
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

    private InteractionResult tryUseMutandisExtremisInCauldron(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (!stack.is(WitcheryItems.MUTANDIS_EXTREMIS.get()) || state.getValue(LEVEL) <= 0 || !isHeated(level.getBlockState(pos.below()))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Optional<ItemEntity> mandrake = findCauldronOffering(level, pos, WitcheryItems.MANDRAKE_ROOT.get());
        Optional<ItemEntity> wart = findCauldronOffering(level, pos, Items.NETHER_WART);
        Optional<ItemEntity> tear = findCauldronOffering(level, pos, WitcheryItems.TEAR_OF_GODDESS.get());
        Optional<ItemEntity> evil = findCauldronOffering(level, pos, WitcheryItems.REFINED_EVIL.get());
        if (mandrake.isPresent() && wart.isPresent() && tear.isPresent() && evil.isPresent()) {
            return brewDropOfLuck(stack, state, level, pos, player, mandrake.get(), wart.get(), tear.get(), evil.get());
        }

        Optional<ItemEntity> sprigWart = findCauldronOffering(level, pos, Items.NETHER_WART);
        Optional<ItemEntity> entTwig = findCauldronOffering(level, pos, WitcheryItems.ENT_TWIG.get());
        if (sprigWart.isPresent() && entTwig.isPresent()) {
            return brewMutatingSprig(stack, state, level, pos, player, sprigWart.get(), entTwig.get());
        }

        player.displayClientMessage(Component.translatable("witchery.cauldron.extremis.support"), false);
        return InteractionResult.CONSUME;
    }

    private InteractionResult brewDropOfLuck(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             ItemEntity mandrake, ItemEntity wart, ItemEntity tear, ItemEntity evil) {
        AltarBlockEntity altar = findNearbyAltar(level, pos, CHALK_ALTAR_RADIUS);
        if (altar == null || !altar.hasValidStructure() || altar.getPower() < 6930) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.drop_of_luck.no_power"), false);
            return InteractionResult.CONSUME;
        }
        if (!altar.consumePower(6930)) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.drop_of_luck.no_power"), false);
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        for (ItemEntity offering : List.of(mandrake, wart, tear, evil)) {
            ItemStack offeringStack = offering.getItem();
            offeringStack.shrink(1);
            if (offeringStack.isEmpty()) {
                offering.discard();
            } else {
                offering.setItem(offeringStack);
            }
        }
        lowerFillLevel(state, level, pos);

        ItemEntity resultEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, new ItemStack(WitcheryItems.DROP_OF_LUCK.get()));
        resultEntity.setDefaultPickUpDelay();
        level.addFreshEntity(resultEntity);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.9F);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5F, 1.1F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        player.displayClientMessage(Component.translatable("witchery.cauldron.drop_of_luck.success"), false);
        return InteractionResult.CONSUME;
    }

    private InteractionResult brewMutatingSprig(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                                ItemEntity wart, ItemEntity entTwig) {
        AltarBlockEntity altar = findNearbyAltar(level, pos, CHALK_ALTAR_RADIUS);
        if (altar == null || !altar.hasValidStructure() || altar.getPower() < 3080) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.mutating_sprig.no_power"), false);
            return InteractionResult.CONSUME;
        }
        if (!altar.consumePower(3080)) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.mutating_sprig.no_power"), false);
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        for (ItemEntity offering : List.of(wart, entTwig)) {
            ItemStack offeringStack = offering.getItem();
            offeringStack.shrink(1);
            if (offeringStack.isEmpty()) {
                offering.discard();
            } else {
                offering.setItem(offeringStack);
            }
        }
        lowerFillLevel(state, level, pos);

        ItemEntity resultEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, new ItemStack(WitcheryItems.MUTATOR.get()));
        resultEntity.setDefaultPickUpDelay();
        level.addFreshEntity(resultEntity);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.05F);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        player.displayClientMessage(Component.translatable("witchery.cauldron.mutating_sprig.success"), false);
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

    private InteractionResult tryOfferMutandisIngredient(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if ((!stack.is(WitcheryItems.MANDRAKE_ROOT.get()) && !stack.is(Items.EGG))
                || state.getValue(LEVEL) <= 0
                || !isHeated(level.getBlockState(pos.below()))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack offering = stack.copyWithCount(1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        ItemEntity offeringEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, offering);
        offeringEntity.setPickUpDelay(40);
        offeringEntity.setDeltaMovement(0.0, 0.0, 0.0);
        level.addFreshEntity(offeringEntity);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.35F, 1.4F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.CONSUME;
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

    // Mandrake Root + Exhale of the Horned One (trigger) + Egg -> 6x Mutandis
    private InteractionResult tryBrewMutandis(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (!stack.is(WitcheryItems.EXHALE_OF_HORNED_ONE.get()) || state.getValue(LEVEL) <= 0 || !isHeated(level.getBlockState(pos.below()))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Optional<ItemEntity> mandrake = findCauldronOffering(level, pos, WitcheryItems.MANDRAKE_ROOT.get());
        Optional<ItemEntity> egg = findCauldronOffering(level, pos, Items.EGG);
        if (mandrake.isEmpty() || egg.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.mutandis.support"), false);
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        for (ItemEntity offering : List.of(mandrake.get(), egg.get())) {
            ItemStack offeringStack = offering.getItem();
            offeringStack.shrink(1);
            if (offeringStack.isEmpty()) {
                offering.discard();
            } else {
                offering.setItem(offeringStack);
            }
        }
        lowerFillLevel(state, level, pos);

        ItemEntity resultEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, new ItemStack(WitcheryItems.MUTANDIS.get(), 6));
        resultEntity.setDefaultPickUpDelay();
        level.addFreshEntity(resultEntity);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.1F);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        player.displayClientMessage(Component.translatable("witchery.cauldron.mutandis.success"), false);
        return InteractionResult.CONSUME;
    }

    // Nether Wart + Mutandis (trigger) → Mutandis Extremis
    private InteractionResult tryBrewMutandisExtremis(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (!stack.is(WitcheryItems.MUTANDIS.get()) || state.getValue(LEVEL) <= 0 || !isHeated(level.getBlockState(pos.below()))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        Optional<ItemEntity> wart = findCauldronOffering(level, pos, Items.NETHER_WART);
        if (wart.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.cauldron.mutandis_extremis.support"), false);
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        ItemStack wartStack = wart.get().getItem();
        wartStack.shrink(1);
        if (wartStack.isEmpty()) {
            wart.get().discard();
        } else {
            wart.get().setItem(wartStack);
        }
        lowerFillLevel(state, level, pos);

        ItemEntity resultEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, new ItemStack(WitcheryItems.MUTANDIS_EXTREMIS.get()));
        resultEntity.setDefaultPickUpDelay();
        level.addFreshEntity(resultEntity);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.95F);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.5F, 1.15F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        player.displayClientMessage(Component.translatable("witchery.cauldron.mutandis_extremis.success"), false);
        return InteractionResult.CONSUME;
    }
}
