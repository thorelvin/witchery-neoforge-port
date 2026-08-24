package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative request for one source-style airborne jump. */
public final class DoubleJumpPayload implements CustomPacketPayload {
    public static final String USED_JUMPS_TAG = "WitcheryDoubleJumpsUsed";
    public static final Type<DoubleJumpPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "double_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPayload> STREAM_CODEC =
            StreamCodec.unit(new DoubleJumpPayload());
    public static final DoubleJumpPayload INSTANCE = new DoubleJumpPayload();

    private DoubleJumpPayload() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(DoubleJumpPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        MobEffectInstance effect = player.getEffect(WitcheryMobEffects.DOUBLE_JUMP);
        if (effect == null || player.onGround() || player.isInWater()
                || player.isPassenger() || player.getAbilities().flying) {
            return;
        }
        int used = player.getPersistentData().getIntOr(USED_JUMPS_TAG, 0);
        if (used >= sourceExtraJumps(effect.getAmplifier())) {
            return;
        }
        player.getPersistentData().putInt(USED_JUMPS_TAG, used + 1);
        MobEffectInstance jumpBoost = player.getEffect(MobEffects.JUMP_BOOST);
        double vertical = sourceJumpVelocity(jumpBoost == null ? -1 : jumpBoost.getAmplifier());
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, vertical, velocity.z);
        player.resetFallDistance();
        player.hurtMarked = true;
    }

    public static int sourceExtraJumps(int amplifier) {
        return 1 + Math.max(0, amplifier);
    }

    public static double sourceJumpVelocity(int jumpBoostAmplifier) {
        return 0.42D + (jumpBoostAmplifier < 0 ? 0.0D : 0.1D * (jumpBoostAmplifier + 1));
    }
}
