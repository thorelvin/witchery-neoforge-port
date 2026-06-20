package com.emoniph.witchery.brewing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A triggered warning stored on an item. It mirrors {@link BrewCurse}'s charge/cooldown behaviour,
 * but releases player-authored actionbar text instead of potion effects.
 */
public record BrewWarningCurse(BrewWarningText warning, int charges, long readyAt) {

    public static final Codec<BrewWarningCurse> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BrewWarningText.CODEC.fieldOf("warning").forGetter(BrewWarningCurse::warning),
            Codec.INT.fieldOf("charges").forGetter(BrewWarningCurse::charges),
            Codec.LONG.fieldOf("ready_at").forGetter(BrewWarningCurse::readyAt)
    ).apply(instance, BrewWarningCurse::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BrewWarningCurse> STREAM_CODEC = StreamCodec.composite(
            BrewWarningText.STREAM_CODEC, BrewWarningCurse::warning,
            ByteBufCodecs.VAR_INT, BrewWarningCurse::charges,
            ByteBufCodecs.VAR_LONG, BrewWarningCurse::readyAt,
            BrewWarningCurse::new);

    public boolean ready(long gameTime) {
        return charges > 0 && warning != null && !warning.isEmpty() && gameTime >= readyAt;
    }
}
