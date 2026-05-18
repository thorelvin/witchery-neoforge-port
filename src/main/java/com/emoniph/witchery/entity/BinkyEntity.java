package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

/**
 * Binky — Death's spectral horse.
 * Spawned when Death is summoned. Survives Death's defeat.
 * Can be tamed by a player using Binky's Skull.
 */
public class BinkyEntity extends AbstractHorse {

    public BinkyEntity(EntityType<? extends BinkyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractHorse.createBaseHorseAttributes();
    }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    public boolean removeWhenFarAway(double dist) { return false; }

    // Tame with Binky's Skull
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isTamed() && held.is(WitcheryItems.BINKYS_SKULL.get())) {
            if (!level().isClientSide()) {
                setOwner(player);
                setTamed(true);
                if (!player.getAbilities().instabuild) held.shrink(1);
                player.displayClientMessage(Component.literal("§5Binky has accepted you as their rider."), false);
            }
            return (level().isClientSide() ? net.minecraft.world.InteractionResult.SUCCESS_SERVER : net.minecraft.world.InteractionResult.SUCCESS);
        }
        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }
}
