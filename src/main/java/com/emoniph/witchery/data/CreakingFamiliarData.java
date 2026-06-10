package com.emoniph.witchery.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent familiar state for a vanilla {@link net.minecraft.world.entity.monster.creaking.Creaking}.
 */
public record CreakingFamiliarData(
        Optional<UUID> owner,
        boolean sitting,
        String customName,
        boolean nameVisible
) {
    public static final CreakingFamiliarData EMPTY = new CreakingFamiliarData(Optional.empty(), false, "", false);

    public static final MapCodec<CreakingFamiliarData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.xmap(
                    s -> s.isBlank() ? Optional.<UUID>empty() : Optional.of(UUID.fromString(s)),
                    opt -> opt.map(UUID::toString).orElse("")
            ).fieldOf("owner").forGetter(CreakingFamiliarData::owner),
            Codec.BOOL.fieldOf("sitting").forGetter(CreakingFamiliarData::sitting),
            Codec.STRING.fieldOf("custom_name").forGetter(CreakingFamiliarData::customName),
            Codec.BOOL.fieldOf("name_visible").forGetter(CreakingFamiliarData::nameVisible)
    ).apply(instance, CreakingFamiliarData::new));

    public boolean isBound() {
        return owner.isPresent();
    }

    public CreakingFamiliarData withOwner(UUID ownerUuid) {
        return new CreakingFamiliarData(Optional.of(ownerUuid), sitting, customName, nameVisible);
    }

    public CreakingFamiliarData withSitting(boolean sitting) {
        return new CreakingFamiliarData(owner, sitting, customName, nameVisible);
    }

    public CreakingFamiliarData withCustomName(@Nullable String name, boolean visible) {
        return new CreakingFamiliarData(owner, sitting, name == null ? "" : name, visible);
    }

    public CreakingFamiliarData cleared() {
        return EMPTY;
    }
}
