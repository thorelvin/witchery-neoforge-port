package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.BinkyEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Binky's Skull summons Binky when used on open ground. */
public class BinkysSkullItem extends Item {
    public BinkysSkullItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }

        BlockPos groundPos = context.getClickedPos();
        BlockPos spawnPos = groundPos.above();
        BlockState spawnState = level.getBlockState(spawnPos);
        if (!spawnState.canBeReplaced() || !level.getBlockState(spawnPos.above()).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BinkyEntity binky = WitcheryEntities.BINKY.get().create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (binky == null) {
            return InteractionResult.PASS;
        }

        binky.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot(), 0.0F);
        if (!serverLevel.noCollision(binky)) {
            return InteractionResult.FAIL;
        }

        binky.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.MOB_SUMMONED, null);
        binky.setOwner(player);
        binky.setTamed(true);
        binky.setTemper(100);
        binky.setPersistenceRequired();
        serverLevel.addFreshEntity(binky);
        player.awardStat(Stats.ITEM_USED.get(context.getItemInHand().getItem()));

        ItemStack stack = context.getItemInHand();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
