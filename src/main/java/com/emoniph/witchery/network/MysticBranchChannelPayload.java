package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-created token for one short-lived Mystic Branch channel session. */
public record MysticBranchChannelPayload(long nonce) implements CustomPacketPayload {

    public static final Type<MysticBranchChannelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "mystic_branch_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MysticBranchChannelPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_LONG, MysticBranchChannelPayload::nonce,
                    MysticBranchChannelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
