package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public record ScryingCameraPayload(boolean active, double x, double y, double z, float yaw, float pitch) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ScryingCameraPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "scrying_camera"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScryingCameraPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public ScryingCameraPayload decode(RegistryFriendlyByteBuf buf) {
                    return new ScryingCameraPayload(
                            buf.readBoolean(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readFloat(),
                            buf.readFloat());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, ScryingCameraPayload payload) {
                    buf.writeBoolean(payload.active());
                    buf.writeDouble(payload.x());
                    buf.writeDouble(payload.y());
                    buf.writeDouble(payload.z());
                    buf.writeFloat(payload.yaw());
                    buf.writeFloat(payload.pitch());
                }
            };

    public static ScryingCameraPayload start(Entity entity) {
        double cameraY = entity instanceof LivingEntity living ? living.getEyeY() : entity.getY();
        return new ScryingCameraPayload(true, entity.getX(), cameraY, entity.getZ(), entity.getYRot(), entity.getXRot());
    }

    public static ScryingCameraPayload start(double x, double y, double z, float yaw, float pitch) {
        return new ScryingCameraPayload(true, x, y, z, yaw, pitch);
    }

    public static ScryingCameraPayload stop() {
        return new ScryingCameraPayload(false, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
    }

    public static void sendStart(ServerPlayer player, Entity entity) {
        PacketDistributor.sendToPlayer(player, start(entity));
    }

    public static void sendStart(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        PacketDistributor.sendToPlayer(player, start(x, y, z, yaw, pitch));
    }

    public static void sendStop(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, stop());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
