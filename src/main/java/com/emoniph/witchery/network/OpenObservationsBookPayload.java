package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenObservationsBookPayload(int visiblePages) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenObservationsBookPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "open_observations_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenObservationsBookPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public OpenObservationsBookPayload decode(RegistryFriendlyByteBuf buf) {
                    return new OpenObservationsBookPayload(buf.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, OpenObservationsBookPayload payload) {
                    buf.writeVarInt(payload.visiblePages());
                }
            };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
