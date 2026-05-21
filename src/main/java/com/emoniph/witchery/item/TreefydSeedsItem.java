package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.TreefydEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Treefyd Seeds - planted on grass to awaken an owned Treefyd guardian. */
public class TreefydSeedsItem extends Item {
    public TreefydSeedsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos groundPos = ctx.getClickedPos();
        BlockPos spawnPos = groundPos.above();
        BlockState groundState = level.getBlockState(groundPos);

        if (player == null || ctx.getClickedFace() != Direction.UP || !groundState.is(Blocks.GRASS_BLOCK)) {
            return InteractionResult.PASS;
        }
        if (!level.getBlockState(spawnPos).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        TreefydEntity treefyd = WitcheryEntities.TREEFYD.get().create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (treefyd == null) {
            return InteractionResult.PASS;
        }
        treefyd.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot(), 0.0F);
        treefyd.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.MOB_SUMMONED, null);
        treefyd.setOwner(player);
        treefyd.setPersistenceRequired();
        serverLevel.addFreshEntity(treefyd);
        serverLevel.levelEvent(2005, spawnPos, 0);

        ItemStack stack = ctx.getItemInHand();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
