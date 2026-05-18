package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.spiritworld.SpiritWorldHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;

/** Thrown (or right-clicked) while in the Spirit World to return to the Overworld. */
public class IcyNeedleItem extends Item {

    public IcyNeedleItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (sp.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) {
                SpiritWorldHelper.exit(sp);
                return InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
            }
        }
        return net.minecraft.world.InteractionResult.SUCCESS.heldItemTransformedTo(player.getItemInHand(hand));
    }
}
