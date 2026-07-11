package com.emoniph.witchery.client.color;

import com.emoniph.witchery.item.BrewBagItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class BrewBagTint implements ItemTintSource {
    public static final BrewBagTint INSTANCE = new BrewBagTint();
    public static final MapCodec<BrewBagTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    private BrewBagTint() {}

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
        return 0xFF000000 | (BrewBagItem.getColor(stack) & 0xFFFFFF);
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
