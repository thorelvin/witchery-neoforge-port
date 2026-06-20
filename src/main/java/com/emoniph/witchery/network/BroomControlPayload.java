package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.BroomEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BroomControlPayload(boolean up, boolean down) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BroomControlPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "broom_control"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BroomControlPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public BroomControlPayload decode(RegistryFriendlyByteBuf buf) {
                    return new BroomControlPayload(buf.readBoolean(), buf.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, BroomControlPayload payload) {
                    buf.writeBoolean(payload.up());
                    buf.writeBoolean(payload.down());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(BroomControlPayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getVehicle() instanceof BroomEntity broom) {
            broom.setRiderVerticalInput(payload.up(), payload.down());
        }
    }
}
