package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server notification that the player has drawn a recognised Mystic Branch symbol.
 *
 * Source parity: {@code PacketSpellPrepared} (1.7.10-0.24.1). Stroke recognition happens on the
 * client (it tracks the local look deltas); this stores the prepared spell server-side so it can
 * be cast when the branch is released.
 */
public record SpellPreparedPayload(int effectId, int level, long channelNonce) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SpellPreparedPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "spell_prepared"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpellPreparedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SpellPreparedPayload::effectId,
                    ByteBufCodecs.VAR_INT, SpellPreparedPayload::level,
                    ByteBufCodecs.VAR_LONG, SpellPreparedPayload::channelNonce,
                    SpellPreparedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(SpellPreparedPayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        // Only accept while the player is actually channelling the branch.
        if (!player.isUsingItem()
                || !(player.getUseItem().getItem() instanceof com.emoniph.witchery.item.MysticBranchItem)) {
            return;
        }
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        long now = player.level().getGameTime();
        if (data.validateMysticBranchPreparation(payload.channelNonce(), now)
                != WitcheryPlayerData.MysticBranchPreparationResult.ACCEPT) {
            return;
        }
        var registry = com.emoniph.witchery.symbol.EffectRegistry.instance();
        if (!registry.isCastableLevel(payload.effectId(), payload.level())) {
            return;
        }
        var effect = registry.getById(payload.effectId());
        if (!com.emoniph.witchery.item.MysticBranchItem.canPrepareSpell(player, effect, payload.level())) {
            return;
        }
        data.setPreparedSpell(payload.effectId(), payload.level());
        data.acceptMysticBranchPreparation();
    }
}
