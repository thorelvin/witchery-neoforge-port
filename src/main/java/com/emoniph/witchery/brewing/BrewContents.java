package com.emoniph.witchery.brewing;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The composed contents of a custom brew — the ordered list of ingredient items that were stirred
 * into the cauldron. Stored on the bottle as a data component; all brew behaviour (effects, name,
 * colour, dispersal) is derived on demand via {@link Brews}, exactly like the source which persists
 * only the trigger items and rebuilds the action list.
 */
public record BrewContents(List<Item> items) {

    public static final BrewContents EMPTY = new BrewContents(List.of());

    public static final Codec<BrewContents> CODEC =
            BuiltInRegistries.ITEM.byNameCodec().listOf().xmap(BrewContents::new, BrewContents::items);

    public static final StreamCodec<RegistryFriendlyByteBuf, BrewContents> STREAM_CODEC =
            ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list()).map(BrewContents::new, BrewContents::items);

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public Brews.Dispersal dispersal() {
        return Brews.dispersal(items);
    }

    public boolean hasEffect() {
        return Brews.hasEffect(items);
    }

    public int color() {
        return Brews.color(items);
    }

    public int power() {
        return Brews.power(items);
    }

    public int powerCost() {
        return Brews.powerCost(items);
    }

    public int drinkDuration() {
        return Brews.drinkDuration(items);
    }

    public Component displayName() {
        return Brews.name(items);
    }

    public void applyToEntity(LivingEntity target) {
        Brews.applyToEntity(target, items);
    }

    public void applyToEntity(LivingEntity target, Entity source) {
        Brews.applyToEntity(target, items, source);
    }

    public void applyToArea(ServerLevel level, Vec3 pos, Brews.Dispersal dispersal) {
        Brews.applyToArea(level, pos, items, dispersal);
    }

    public void applyToArea(ServerLevel level, Vec3 pos, Brews.Dispersal dispersal, LivingEntity directTarget) {
        Brews.applyToArea(level, pos, items, dispersal, directTarget);
    }

    public void applyToArea(ServerLevel level, Vec3 pos, Brews.Dispersal dispersal, LivingEntity directTarget, Entity source) {
        Brews.applyToArea(level, pos, items, dispersal, directTarget, source);
    }

    public void applyToArea(ServerLevel level, Vec3 pos, Brews.Dispersal dispersal,
                            LivingEntity directTarget, Entity source, boolean blockHit) {
        Brews.applyToArea(level, pos, items, dispersal, directTarget, source, blockHit);
    }
}
