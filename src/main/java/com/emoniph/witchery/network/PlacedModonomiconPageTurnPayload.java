package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
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

public record PlacedModonomiconPageTurnPayload(BlockPos pos, int direction) implements CustomPacketPayload {
    private static final double MAX_DISTANCE_SQR = 36.0D;

    public static final CustomPacketPayload.Type<PlacedModonomiconPageTurnPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "placed_modonomicon_page_turn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlacedModonomiconPageTurnPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, PlacedModonomiconPageTurnPayload::pos,
                    ByteBufCodecs.VAR_INT, PlacedModonomiconPageTurnPayload::direction,
                    PlacedModonomiconPageTurnPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(PlacedModonomiconPageTurnPayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }

        Level level = player.level();
        if (player.distanceToSqr(payload.pos().getX() + 0.5D, payload.pos().getY() + 0.5D, payload.pos().getZ() + 0.5D)
                > MAX_DISTANCE_SQR) {
            return;
        }

        int direction = Integer.compare(payload.direction(), 0);
        BlockState state = level.getBlockState(payload.pos());
        if (direction == 0 || !state.is(WitcheryBlocks.PLACED_MODONOMICON.get())
                || !(level.getBlockEntity(payload.pos()) instanceof PlacedModonomiconBlockEntity be)) {
            return;
        }
        if (be.turnPlacedPage(direction, player)) {
            level.playSound(null, payload.pos(), SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS,
                    0.8F, level.random.nextFloat() * 0.25F + 0.85F);
        }
    }
}
