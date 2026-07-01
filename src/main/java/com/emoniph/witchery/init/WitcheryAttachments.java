package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.CreakingFamiliarData;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class WitcheryAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, WitcheryMod.MODID);

    public static final Supplier<AttachmentType<WitcheryPlayerData>> PLAYER_DATA =
            ATTACHMENT_TYPES.register("player_data", () ->
                    AttachmentType.builder(WitcheryPlayerData::new)
                            .serialize(WitcheryPlayerData.CODEC)
                            .build());

    /** Mystic Branch spell knowledge learned from Demon souls (a set of {@code knowledgeKey}s). */
    public static final class LearnedSymbols {
        public final Set<String> keys = new HashSet<>();
        public final Map<String, Long> cooldownUntil = new HashMap<>();

        public static final MapCodec<LearnedSymbols> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.listOf().fieldOf("keys").forGetter(learned -> new ArrayList<>(learned.keys)),
                Codec.unboundedMap(Codec.STRING, Codec.LONG)
                        .optionalFieldOf("cooldown_until", Map.of())
                        .forGetter(learned -> new HashMap<>(learned.cooldownUntil))
        ).apply(i, (list, cooldowns) -> {
            LearnedSymbols learned = new LearnedSymbols();
            learned.keys.addAll(list);
            learned.cooldownUntil.putAll(cooldowns);
            return learned;
        }));

        public long cooldownRemaining(String key, long gameTime) {
            if (key == null || key.isBlank()) {
                return 0L;
            }
            Long until = cooldownUntil.get(key);
            if (until == null) {
                return 0L;
            }
            long remaining = until - gameTime;
            if (remaining <= 0L) {
                cooldownUntil.remove(key);
                return 0L;
            }
            return remaining;
        }

        public void setCooldownUntil(String key, long gameTime) {
            if (key != null && !key.isBlank() && gameTime > 0L) {
                cooldownUntil.put(key, gameTime);
            }
        }
    }

    public static final Supplier<AttachmentType<LearnedSymbols>> LEARNED_SYMBOLS =
            ATTACHMENT_TYPES.register("learned_symbols", () ->
                    AttachmentType.builder(LearnedSymbols::new)
                            .serialize(LearnedSymbols.CODEC)
                            .copyOnDeath()
                            .build());

    /**
     * One-shot flag: has this player already been given the Witches' Primer on first join? Kept as a
     * standalone attachment (not a {@link WitcheryPlayerData} field) to avoid the RecordCodecBuilder
     * 16-field cap, and copied across death so a respawn+relog never re-gifts.
     */
    public static final Supplier<AttachmentType<Boolean>> RECEIVED_PRIMER =
            ATTACHMENT_TYPES.register("received_primer", () ->
                    AttachmentType.builder(() -> Boolean.FALSE)
                            .serialize(Codec.BOOL.fieldOf("value"))
                            .copyOnDeath()
                            .build());

    /**
     * Bitmask of which of the 12 in-world story books a player has found (bit {@code 1 << (id-1)}).
     * Finding 5 distinct stories unlocks the Spectral Coven (Rite of the Gathered Lives). Copied across
     * death so progress is never lost on respawn.
     */
    public static final Supplier<AttachmentType<Integer>> STORIES_FOUND =
            ATTACHMENT_TYPES.register("stories_found", () ->
                    AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT.fieldOf("value"))
                            .copyOnDeath()
                            .build());

    /** One-shot flag: has this player crossed the 5-story threshold and unlocked the Gathered Lives rite? */
    public static final Supplier<AttachmentType<Boolean>> GATHERED_LIVES_UNLOCKED =
            ATTACHMENT_TYPES.register("gathered_lives_unlocked", () ->
                    AttachmentType.builder(() -> Boolean.FALSE)
                            .serialize(Codec.BOOL.fieldOf("value"))
                            .copyOnDeath()
                            .build());

    /** Bound-familiar state for vanilla {@code minecraft:creaking} mobs. */
    public static final Supplier<AttachmentType<CreakingFamiliarData>> CREAKING_FAMILIAR =
            ATTACHMENT_TYPES.register("creaking_familiar", () ->
                    AttachmentType.builder(() -> CreakingFamiliarData.EMPTY)
                            .serialize(CreakingFamiliarData.CODEC)
                            .build());

    // Convenience helpers — call these server-side only unless data has been synced
    public static WitcheryPlayerData get(net.minecraft.world.entity.player.Player player) {
        return player.getData(PLAYER_DATA.get());
    }

    public static LearnedSymbols learned(net.minecraft.world.entity.player.Player player) {
        return player.getData(LEARNED_SYMBOLS.get());
    }

    public static boolean hasReceivedPrimer(net.minecraft.world.entity.player.Player player) {
        return player.getData(RECEIVED_PRIMER.get());
    }

    public static void setReceivedPrimer(net.minecraft.world.entity.player.Player player) {
        player.setData(RECEIVED_PRIMER.get(), Boolean.TRUE);
    }

    /** Number of distinct story books (1-12) this player has found. */
    public static int storiesFoundCount(net.minecraft.world.entity.player.Player player) {
        return Integer.bitCount(player.getData(STORIES_FOUND.get()));
    }

    /** Records story book {@code id} (1-12) as found; returns true if it was newly added. */
    public static boolean addStoryFound(net.minecraft.world.entity.player.Player player, int id) {
        if (id < 1 || id > 12) return false;
        int mask = player.getData(STORIES_FOUND.get());
        int bit = 1 << (id - 1);
        if ((mask & bit) != 0) return false;
        player.setData(STORIES_FOUND.get(), mask | bit);
        return true;
    }

    public static boolean isGatheredLivesUnlocked(net.minecraft.world.entity.player.Player player) {
        return player.getData(GATHERED_LIVES_UNLOCKED.get());
    }

    public static void setGatheredLivesUnlocked(net.minecraft.world.entity.player.Player player) {
        player.setData(GATHERED_LIVES_UNLOCKED.get(), Boolean.TRUE);
    }
}
