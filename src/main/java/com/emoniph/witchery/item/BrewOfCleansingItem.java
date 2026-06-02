package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.BlightHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class BrewOfCleansingItem extends BrewItem {
    private static final int BLOCK_RADIUS = 4;

    public BrewOfCleansingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        int cleansed = BlightHelper.cleanseArea(serverLevel, context.getClickedPos(), BLOCK_RADIUS);
        if (cleansed <= 0) {
            return InteractionResult.PASS;
        }

        consumeBrewBottle(context.getPlayer(), context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (level instanceof ServerLevel serverLevel) {
            BlightHelper.cleanseArea(serverLevel, player.blockPosition(), BLOCK_RADIUS);
        }
    }
}
