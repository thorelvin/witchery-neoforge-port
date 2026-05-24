package com.emoniph.witchery.client.model;

import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import com.emoniph.witchery.item.PoppetShelfCompassItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record PoppetShelfCompassAngleProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<PoppetShelfCompassAngleProperty> MAP_CODEC =
            MapCodec.unit(new PoppetShelfCompassAngleProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        if (owner == null) {
            return 0.0F;
        }
        if (level == null) {
            if (!(owner.level() instanceof ClientLevel clientLevel)) {
                return 0.0F;
            }
            level = clientLevel;
        }

        Vec3 ownerPos = owner.position();
        double closest = Double.MAX_VALUE;
        for (PoppetShelfBlockEntity shelf : PoppetShelfBlockEntity.getActiveShelves(level)) {
            double dx = ownerPos.x - shelf.getBlockPos().getX();
            double dz = ownerPos.z - shelf.getBlockPos().getZ();
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq < closest) {
                closest = distanceSq;
            }
        }
        return PoppetShelfCompassItem.signalStrength(closest);
    }

    @Override
    public MapCodec<PoppetShelfCompassAngleProperty> type() {
        return MAP_CODEC;
    }
}
