package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.events.VampireEvents;
import com.emoniph.witchery.data.WitcheryPlayerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class VampireUltimatePayload implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VampireUltimatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vampire_ultimate"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VampireUltimatePayload> STREAM_CODEC =
            StreamCodec.unit(new VampireUltimatePayload());

    public static final VampireUltimatePayload INSTANCE = new VampireUltimatePayload();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(VampireUltimatePayload payload, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            VampireEvents.handleVampirePowerRequest(player, WitcheryPlayerData.VampirePower.ULTIMATE, true);
        }
    }
}
