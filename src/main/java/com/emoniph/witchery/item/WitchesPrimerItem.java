package com.emoniph.witchery.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.minecraft.world.InteractionResult;

/** The Witches' Primer — opens the Patchouli guidebook when Patchouli is installed. */
public class WitchesPrimerItem extends Item {

    public WitchesPrimerItem(Properties props) { super(props); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && ModList.get().isLoaded("patchouli")) {
            PatchouliCompat.openBook((ServerPlayer) player);
        }
        return (level.isClientSide() ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.SUCCESS_SERVER);
    }
}
