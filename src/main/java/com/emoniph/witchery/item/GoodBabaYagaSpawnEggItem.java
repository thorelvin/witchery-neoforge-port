package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/** Creative helper egg for the friendly Baba Yaga prophecy encounter. */
public class GoodBabaYagaSpawnEggItem extends SpawnEggItem {
    public GoodBabaYagaSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockState state = level.getBlockState(clicked);
        BlockPos spawnPos = state.getCollisionShape(level, clicked).isEmpty()
                ? clicked
                : clicked.relative(direction);

        return spawnFriendly((ServerLevel) level, player, context.getItemInHand(), spawnPos, clicked);
    }

    private InteractionResult spawnFriendly(ServerLevel level, Player player, ItemStack stack,
                                            BlockPos spawnPos, BlockPos eventPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        BabaYagaEntity baba = WitcheryEntities.BABA_YAGA.get().create(level, EntitySpawnReason.TRIGGERED);
        if (baba == null) {
            return InteractionResult.PASS;
        }
        baba.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot(), 0.0F);
        if (!level.noCollision(baba)) {
            return InteractionResult.FAIL;
        }

        baba.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.MOB_SUMMONED, null);
        level.addFreshEntity(baba);
        baba.beginFriendlyVisit(serverPlayer);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, eventPos);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
