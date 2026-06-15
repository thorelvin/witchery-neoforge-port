package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.item.WerewolfShiftUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class HowlPayload implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<HowlPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "howl"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HowlPayload> STREAM_CODEC =
            StreamCodec.unit(new HowlPayload());

    public static final HowlPayload INSTANCE = new HowlPayload();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(HowlPayload payload, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);

        if (!data.isWerewolf() || !WerewolfShiftUtil.isTransformed(data)) return;
        if (data.getHowlCooldown() > 0) return;

        ServerLevel level = ((net.minecraft.server.level.ServerLevel) player.level());

        // Howl sound heard up to 64 blocks away
        level.playSound(null, player.blockPosition(), SoundEvents.WOLF_SHAKE, SoundSource.PLAYERS, 2f, 0.8f);

        // Frighten nearby non-werewolf living entities
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(16))) {
            if (entity == player) continue;
            if (entity instanceof ServerPlayer sp && WitcheryAttachments.get(sp).isWerewolf()) continue;
            if (entity instanceof Wolf) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
        }

        // Summon 2 tamed wolves near the player
        for (int i = 0; i < 2; i++) {
            Wolf wolf = EntityType.WOLF.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (wolf != null) {
                double dx = (level.getRandom().nextDouble() - 0.5) * 4;
                double dz = (level.getRandom().nextDouble() - 0.5) * 4;
                wolf.setPos(player.getX() + dx, player.getY(), player.getZ() + dz);
                wolf.tame(player);
                level.addFreshEntity(wolf);
            }
        }

        // Level 6→7 quest: howling in distinct chunks at night counts toward the climb.
        if (data.getWerewolfLevel() == 6
                && data.getWolfQuestState() == WitcheryPlayerData.WolfQuestState.STARTED
                && WerewolfShiftUtil.inWolfForm(data)
                && (level.getDayTime() % 24000L) >= 13000L) {  // night (source: !isDaytime)
            long chunk = new ChunkPos(player.blockPosition()).toLong();
            if (data.addHowledChunk(chunk)) {
                data.incrementWolfQuestCounter();
                player.displayClientMessage(Component.translatable("witchery.werewolf.level7progress",
                        16, Math.max(0, 16 - data.getWolfQuestCounter())), true);
            } else {
                player.displayClientMessage(Component.translatable("witchery.werewolf.chunkvisited"), true);
            }
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        }

        data.setHowlCooldown(WitcheryConfig.WEREWOLF_HOWL_COOLDOWN_SECONDS.get() * 20);
    }
}
