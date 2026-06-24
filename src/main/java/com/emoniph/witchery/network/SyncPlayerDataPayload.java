package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPlayerDataPayload(
        int entityId,
        boolean vampire,
        int vampireLevel,
        int bloodPower,
        int bloodReserve,
        boolean werewolf,
        int bloodLevel,
        float transformationLevel,
        String familiarUUID,
        String familiarType,
        String familiarName,
        boolean familiarSitting,
        boolean familiarNameVisible,
        String infusionType,
        int infusionEnergy,
        int infusionMaxEnergy,
        boolean inSpiritWorld,
        boolean manifestedGhost,
        boolean spiritWorldDream,
        boolean demonicNightmare
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncPlayerDataPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "sync_player_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerDataPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SyncPlayerDataPayload decode(RegistryFriendlyByteBuf buf) {
                    return new SyncPlayerDataPayload(
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readInt(),
                            buf.readFloat(),
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readUtf(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readBoolean(),
                            buf.readBoolean()
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, SyncPlayerDataPayload payload) {
                    buf.writeInt(payload.entityId());
                    buf.writeBoolean(payload.vampire());
                    buf.writeInt(payload.vampireLevel());
                    buf.writeInt(payload.bloodPower());
                    buf.writeInt(payload.bloodReserve());
                    buf.writeBoolean(payload.werewolf());
                    buf.writeInt(payload.bloodLevel());
                    buf.writeFloat(payload.transformationLevel());
                    buf.writeUtf(payload.familiarUUID());
                    buf.writeUtf(payload.familiarType());
                    buf.writeUtf(payload.familiarName());
                    buf.writeBoolean(payload.familiarSitting());
                    buf.writeBoolean(payload.familiarNameVisible());
                    buf.writeUtf(payload.infusionType());
                    buf.writeInt(payload.infusionEnergy());
                    buf.writeInt(payload.infusionMaxEnergy());
                    buf.writeBoolean(payload.inSpiritWorld());
                    buf.writeBoolean(payload.manifestedGhost());
                    buf.writeBoolean(payload.spiritWorldDream());
                    buf.writeBoolean(payload.demonicNightmare());
                }
            };

    public static SyncPlayerDataPayload from(Player player, WitcheryPlayerData data) {
        return new SyncPlayerDataPayload(
                player.getId(),
                data.isVampire(),
                data.getVampireLevel(),
                data.getBloodPower(),
                data.getBloodReserve(),
                data.isWerewolf(),
                data.getBloodLevel(), data.getTransformationLevel(),
                data.getFamiliarUUID().map(java.util.UUID::toString).orElse(""),
                data.getFamiliarType(),
                data.getFamiliarName(),
                data.isFamiliarSitting(),
                data.isFamiliarNameVisible(),
                data.getInfusionType().name(),
                data.getInfusionEnergy(),
                data.getInfusionMaxEnergy(),
                data.isInSpiritWorld(),
                data.isManifestedGhost(),
                data.isSpiritWorldDream(),
                data.isDemonicNightmare()
        );
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    // Client handler lives in WitcheryClient to keep Minecraft.getInstance() out of shared code.
}
