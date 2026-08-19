package com.emoniph.witchery.network;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.events.WerewolfEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.item.WerewolfProgression;
import com.emoniph.witchery.item.WerewolfShiftUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
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

        ServerLevel level = (ServerLevel) player.level();
        int wolfLevel = data.getWerewolfLevel();

        // The level-six pilgrimage precedes the power cooldown in the source.
        if (wolfLevel == 6
                && data.getWolfQuestState() == WitcheryPlayerData.WolfQuestState.STARTED
                && isNight(level.getDayTime())) {
            playHowl(level, player);
            long chunk = new ChunkPos(player.blockPosition()).toLong();
            if (data.addHowledChunk(chunk)) {
                data.incrementWolfQuestCounter();
                int target = WerewolfProgression.questTarget(6);
                player.displayClientMessage(Component.translatable("witchery.werewolf.level7progress",
                        target, Math.max(0, target - data.getWolfQuestCounter())), true);
            } else {
                player.displayClientMessage(Component.translatable("witchery.werewolf.chunkvisited"), true);
            }
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
            return;
        }

        long nowMillis = System.currentTimeMillis();
        if (!WerewolfProgression.canPowerHowl(nowMillis, data.getLastHowl(), player.isCreative())) {
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }
        if (WerewolfShiftUtil.inWolfmanForm(data) && wolfLevel >= 7) {
            playHowl(level, player);
            int seconds = 4 + level.getRandom().nextInt(Math.max(1, wolfLevel - 6));
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(16))) {
                if (entity == player || WerewolfEvents.isWerewolf(entity)
                        || WerewolfEvents.isVampire(entity)) continue;
                entity.addEffect(new MobEffectInstance(WitcheryMobEffects.PARALYSED, seconds * 20, 3));
            }
        } else if (WerewolfShiftUtil.inWolfForm(data) && wolfLevel >= 8) {
            playHowl(level, player);
            int count = 2 + level.getRandom().nextInt(Math.max(1, wolfLevel - 7));
            summonWolves(level, player, count);
        } else {
            return;
        }

        data.setLastHowl(nowMillis);
    }

    private static void playHowl(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), WitcherySoundEvents.MOB_WOLFMAN_HOWL.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    static boolean isNight(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time > 13000L && time < 23000L;
    }

    private static void summonWolves(ServerLevel level, ServerPlayer player, int count) {
        for (int i = 0; i < count; i++) {
            Wolf wolf = EntityType.WOLF.create(level, EntitySpawnReason.TRIGGERED);
            if (wolf == null) continue;
            double dx = (level.getRandom().nextDouble() - 0.5D) * 4.0D;
            double dz = (level.getRandom().nextDouble() - 0.5D) * 4.0D;
            wolf.setPos(player.getX() + dx, player.getY(), player.getZ() + dz);
            wolf.tame(player);
            wolf.addEffect(new MobEffectInstance(WitcheryMobEffects.MORTAL_COIL, 200));
            wolf.getPersistentData().putBoolean("WITCNoDrops", true);
            level.addFreshEntity(wolf);
        }
    }
}
