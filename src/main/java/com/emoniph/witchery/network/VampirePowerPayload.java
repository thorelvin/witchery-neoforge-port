package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.events.VampireEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VampirePowerPayload(int power, boolean trigger) implements CustomPacketPayload {
    public static final Type<VampirePowerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "vampire_power"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VampirePowerPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public VampirePowerPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new VampirePowerPayload(buffer.readVarInt(), buffer.readBoolean());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, VampirePowerPayload payload) {
                    buffer.writeVarInt(payload.power());
                    buffer.writeBoolean(payload.trigger());
                }
            };

    public static VampirePowerPayload select(WitcheryPlayerData.VampirePower power) {
        return new VampirePowerPayload(power.ordinal(), false);
    }

    public static VampirePowerPayload trigger(WitcheryPlayerData.VampirePower power) {
        return new VampirePowerPayload(power.ordinal(), true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(VampirePowerPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            VampireEvents.handleVampirePowerRequest(player,
                    WitcheryPlayerData.VampirePower.fromOrdinal(payload.power()), payload.trigger());
        }
    }
}
