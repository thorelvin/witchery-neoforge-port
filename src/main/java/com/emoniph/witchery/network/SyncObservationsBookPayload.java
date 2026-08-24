package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.item.ImmortalsObservationsBookItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncObservationsBookPayload(int pageIndex, boolean offhand) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncObservationsBookPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                    WitcheryMod.MODID, "sync_observations_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncObservationsBookPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SyncObservationsBookPayload decode(RegistryFriendlyByteBuf buf) {
                    return new SyncObservationsBookPayload(buf.readVarInt(), buf.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SyncObservationsBookPayload payload) {
                    buf.writeVarInt(payload.pageIndex());
                    buf.writeBoolean(payload.offhand());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(SyncObservationsBookPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (ImmortalsObservationsBookItem.isObservationsBook(stack)) {
            ImmortalsObservationsBookItem.finishReading(player, stack, payload.pageIndex());
        }
    }
}
