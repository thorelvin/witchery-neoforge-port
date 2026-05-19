package com.emoniph.witchery.item;

import com.emoniph.witchery.data.CreaturePowerType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;

/**
 * Activates the player's current creature power (consuming 1 charge).
 * Right-click to use; power type and charges are stored in WitcheryPlayerData.
 */
public class MysticBranchItem extends Item {

    public MysticBranchItem(Properties props) { super(props); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return AltarPlacedItemSupport.placeOnAltar(ctx);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        String powerName = data.getCreaturePowerType();
        CreaturePowerType power = CreaturePowerType.fromString(powerName);

        if (power == null || data.getCreaturePowerCharges() <= 0) {
            player.displayClientMessage(Component.literal("§7No creature power stored. Sacrifice a mob near an altar."), false);
            return net.minecraft.world.InteractionResult.FAIL;
        }

        if (!data.spendCreaturePowerCharge()) {
            player.displayClientMessage(Component.literal("§7No charges remaining."), false);
            return net.minecraft.world.InteractionResult.FAIL;
        }

        power.activate(player);
        player.displayClientMessage(Component.literal(
                "§5" + power.displayName() + " power activated! Charges remaining: " + data.getCreaturePowerCharges()), false);

        return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
    }
}
