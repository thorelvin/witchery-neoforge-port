package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Syncs the source-style 0-500 Villager blood pool to tracking vampire clients. */
public record SyncVampireTargetBloodPayload(int entityId, int blood) implements CustomPacketPayload {
    public static final Type<SyncVampireTargetBloodPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "sync_vampire_target_blood"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncVampireTargetBloodPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeInt(payload.entityId());
                        buf.writeInt(payload.blood());
                    },
                    buf -> new SyncVampireTargetBloodPayload(buf.readInt(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
