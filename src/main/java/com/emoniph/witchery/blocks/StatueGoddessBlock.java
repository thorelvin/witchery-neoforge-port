package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.StatueGoddessBlockEntity;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.events.WitcheryPlayerEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class StatueGoddessBlock extends BaseEntityBlock {
    public static final MapCodec<StatueGoddessBlock> CODEC = simpleCodec(StatueGoddessBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(0.0, 0.0, 1.6, 16.0, 32.0, 14.4);

    public StatueGoddessBlock(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StatueGoddessBlockEntity(pos, state); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(placer instanceof Player player) || !(level.getBlockEntity(pos) instanceof StatueGoddessBlockEntity statue)) {
            return;
        }
        statue.setOwner(player.getScoreboardName());
        level.sendBlockUpdated(pos, state, state, 3);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof StatueGoddessBlockEntity statue)) {
            return;
        }
        if (!player.isCreative() && !(player.isShiftKeyDown() && statue.isOwner(player))) {
            return;
        }

        for (BlockPos cursor = pos; level.getBlockState(cursor).is(this); cursor = cursor.above()) {
            level.destroyBlock(cursor, false, player);
            Block.popResource(level, cursor, asItem().getDefaultInstance());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        boolean cured = false;

        if (player.hasEffect(MobEffects.UNLUCK)) {
            player.removeEffect(MobEffects.UNLUCK);
            sendCureMessage(player, "curse.witchery.misfortune.goddess_removal");
            cured = true;
        }

        boolean hadInsanity = player.hasEffect(MobEffects.NAUSEA) || player.hasEffect(MobEffects.BLINDNESS);
        if (hadInsanity) {
            player.removeEffect(MobEffects.NAUSEA);
            player.removeEffect(MobEffects.BLINDNESS);
            sendCureMessage(player, "curse.witchery.insanity.goddess_removal");
            cured = true;
        }

        boolean hadSinking = player.hasEffect(MobEffects.SLOWNESS)
                || player.hasEffect(MobEffects.MINING_FATIGUE)
                || player.hasEffect(MobEffects.LEVITATION);
        if (hadSinking) {
            player.removeEffect(MobEffects.SLOWNESS);
            player.removeEffect(MobEffects.MINING_FATIGUE);
            player.removeEffect(MobEffects.LEVITATION);
            sendCureMessage(player, "curse.witchery.sinking.goddess_removal");
            cured = true;
        }

        boolean hadOverheating = player.hasEffect(MobEffects.WEAKNESS) || player.isOnFire();
        if (hadOverheating) {
            player.removeEffect(MobEffects.WEAKNESS);
            player.clearFire();
            sendCureMessage(player, "curse.witchery.overheating.goddess_removal");
            cured = true;
        }

        boolean hadNightmare = data.isDemonicNightmare() || (data.isInSpiritWorld() && !data.isSpiritWorldDream());
        if (hadNightmare) {
            data.setSpiritWorldIsDream(true);
            data.setDemonicNightmare(false);
            if (player instanceof ServerPlayer serverPlayer) {
                WitcheryPlayerEvents.syncToClient(serverPlayer);
            }
            sendCureMessage(player, "curse.witchery.waking_nightmare.goddess_removal");
            cured = true;
        }

        if (cured) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 0.4f);
            level.levelEvent(2005, pos.above(), 0);
            return InteractionResult.CONSUME;
        }

        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5f, 0.6f);
        return InteractionResult.CONSUME;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private static void sendCureMessage(Player player, String key) {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.BLUE), false);
    }
}
