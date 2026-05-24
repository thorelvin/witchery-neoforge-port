package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.SunGrenadeEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SunGrenadeItem extends Item {

    public SunGrenadeItem(Properties props) { super(props); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            SunGrenadeEntity grenade = new SunGrenadeEntity(level, player);
            grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(grenade);
            level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW,
                    SoundSource.NEUTRAL, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
