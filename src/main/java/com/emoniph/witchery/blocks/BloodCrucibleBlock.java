package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.BloodCrucibleBlockEntity;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BloodCrucibleBlock extends BaseEntityBlock {

    public static final MapCodec<BloodCrucibleBlock> CODEC = simpleCodec(BloodCrucibleBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 5, 12);

    public BloodCrucibleBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloodCrucibleBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof BloodCrucibleBlockEntity be)) {
            return InteractionResult.PASS;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        boolean hasVampireAccess = data.isVampire() && data.getVampireLevel() >= 10;

        if (!hasVampireAccess) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("witchery.blood_crucible.not_ready"),
                    true);
            return InteractionResult.CONSUME;
        }

        if (!be.isFull() && !player.getAbilities().instabuild) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("witchery.blood_crucible.not_full"),
                    true);
            return InteractionResult.CONSUME;
        }

        WitcheryPlayerData.VampireUltimate chosen = null;
        if (stack.is(WitcheryItems.ARTICHOKE.get())) {
            chosen = WitcheryPlayerData.VampireUltimate.STORM;
        } else if (stack.is(WitcheryItems.BAT_WOOL.get())) {
            chosen = WitcheryPlayerData.VampireUltimate.SWARM;
        } else if (stack.is(net.minecraft.world.item.Items.WRITTEN_BOOK)) {
            chosen = WitcheryPlayerData.VampireUltimate.FARM;
        }

        if (chosen == null) return InteractionResult.PASS;

        data.setVampireUltimate(chosen, 5);
        be.drainAll(level);

        if (!player.getAbilities().instabuild) stack.shrink(1);

        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.5F);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        "witchery.blood_crucible.ultimate." + chosen.name().toLowerCase(java.util.Locale.ROOT)),
                false);

        com.emoniph.witchery.network.SyncPlayerDataPayload payload =
                com.emoniph.witchery.network.SyncPlayerDataPayload.from(player, data);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                (net.minecraft.server.level.ServerPlayer) player, payload);

        return InteractionResult.CONSUME;
    }
}
