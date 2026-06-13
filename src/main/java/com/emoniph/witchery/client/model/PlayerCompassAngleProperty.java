package com.emoniph.witchery.client.model;

import com.emoniph.witchery.item.PlayerCompassItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record PlayerCompassAngleProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<PlayerCompassAngleProperty> MAP_CODEC =
            MapCodec.unit(new PlayerCompassAngleProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        if (!PlayerCompassItem.isBound(stack) || owner == null) {
            return -1.0F;
        }
        if (level == null) {
            if (!(owner.level() instanceof ClientLevel clientLevel)) {
                return -1.0F;
            }
            level = clientLevel;
        }

        UUID targetId = PlayerCompassItem.getBoundUUID(stack).orElse(null);
        if (targetId == null) {
            return -1.0F;
        }

        LivingEntity ownerLiving = owner.asLivingEntity();
        if (ownerLiving != null && targetId.equals(ownerLiving.getUUID())) {
            return 0.0F;
        }

        Player target = level.getPlayerByUUID(targetId);
        if (target == null || !target.isAlive()) {
            return wanderingAngle(level, seed);
        }

        return PlayerCompassItem.compassAngle(owner.getVisualRotationYInDegrees(), owner.position(), target.position());
    }

    private static float wanderingAngle(ClientLevel level, int seed) {
        return Mth.positiveModulo((level.getGameTime() + (seed & 31)) / (float) PlayerCompassItem.COMPASS_FRAMES, 1.0F);
    }

    @Override
    public MapCodec<PlayerCompassAngleProperty> type() {
        return MAP_CODEC;
    }
}
