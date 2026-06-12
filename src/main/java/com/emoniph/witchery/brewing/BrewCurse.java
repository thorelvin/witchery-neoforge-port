package com.emoniph.witchery.brewing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A delayed brew "curse" stored on an item by a triggered-dispersal brew (source {@code WITCCurse} NBT).
 * Using the cursed item releases one charge of the brew onto the user, then goes on a short cooldown until
 * {@code readyAt} (game time). When the last charge is spent the component is removed.
 */
public record BrewCurse(BrewContents brew, int charges, long readyAt) {

    public static final Codec<BrewCurse> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BrewContents.CODEC.fieldOf("brew").forGetter(BrewCurse::brew),
            Codec.INT.fieldOf("charges").forGetter(BrewCurse::charges),
            Codec.LONG.fieldOf("ready_at").forGetter(BrewCurse::readyAt)
    ).apply(instance, BrewCurse::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BrewCurse> STREAM_CODEC = StreamCodec.composite(
            BrewContents.STREAM_CODEC, BrewCurse::brew,
            ByteBufCodecs.VAR_INT, BrewCurse::charges,
            ByteBufCodecs.VAR_LONG, BrewCurse::readyAt,
            BrewCurse::new);

    public boolean ready(long gameTime) {
        return charges > 0 && gameTime >= readyAt;
    }
}
