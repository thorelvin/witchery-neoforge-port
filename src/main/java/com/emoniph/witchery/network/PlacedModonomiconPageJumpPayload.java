package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.PlacedModonomiconBlock;
import com.emoniph.witchery.blocks.entity.PlacedModonomiconBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PlacedModonomiconPageJumpPayload(BlockPos pos, int openPagesIndex) implements CustomPacketPayload {
    private static final double MAX_DISTANCE_SQR = 36.0D;

    public static final CustomPacketPayload.Type<PlacedModonomiconPageJumpPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "placed_modonomicon_page_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacedModonomiconPageJumpPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PlacedModonomiconPageJumpPayload::pos,
                    ByteBufCodecs.VAR_INT, PlacedModonomiconPageJumpPayload::openPagesIndex,
                    PlacedModonomiconPageJumpPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(PlacedModonomiconPageJumpPayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }

        Level level = player.level();
        if (player.distanceToSqr(payload.pos().getX() + 0.5D, payload.pos().getY() + 0.5D, payload.pos().getZ() + 0.5D)
                > MAX_DISTANCE_SQR) {
            return;
        }

        BlockState state = level.getBlockState(payload.pos());
        if (!state.is(WitcheryBlocks.PLACED_MODONOMICON.get())
                || !(level.getBlockEntity(payload.pos()) instanceof PlacedModonomiconBlockEntity be)
                || !PlacedModonomiconBlock.isLookingAtReadablePage(state, payload.pos(), player)) {
            return;
        }

        if (be.jumpPlacedPage(payload.openPagesIndex(), player)) {
            level.playSound(null, payload.pos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS,
                    0.8F, level.random.nextFloat() * 0.25F + 0.85F);
        }
    }
}
