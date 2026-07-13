package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.SummonedUndeadEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Graveyard Dust can strengthen the player's enslaved summoned spectral undead. */
public class GraveyardDustItem extends Item {
    public GraveyardDustItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (target instanceof SummonedUndeadEntity summoned && summoned.applyGraveyardDust(player, stack)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
