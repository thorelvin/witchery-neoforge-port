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
import java.util.HashSet;
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

        public static final MapCodec<LearnedSymbols> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.listOf().fieldOf("keys").forGetter(learned -> new ArrayList<>(learned.keys))
        ).apply(i, list -> {
            LearnedSymbols learned = new LearnedSymbols();
            learned.keys.addAll(list);
            return learned;
        }));
    }

    public static final Supplier<AttachmentType<LearnedSymbols>> LEARNED_SYMBOLS =
            ATTACHMENT_TYPES.register("learned_symbols", () ->
                    AttachmentType.builder(LearnedSymbols::new)
                            .serialize(LearnedSymbols.CODEC)
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
}
