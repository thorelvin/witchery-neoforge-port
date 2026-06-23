package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.ParasyticLouseEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ParasyticLouseItem extends Item {
    public ParasyticLouseItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        return spawnLouse(level, player, context.getItemInHand(), placePos);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }

        BlockPos waterPos = hit.getBlockPos();
        BlockState waterState = level.getBlockState(waterPos);
        if (!waterState.is(Blocks.WATER) || !waterState.getFluidState().isSource()) {
            return InteractionResult.PASS;
        }
        return spawnLouse(level, player, stack, waterPos.above());
    }

    private static InteractionResult spawnLouse(Level level, Player player, ItemStack stack, BlockPos spawnPos) {
        if (!level.mayInteract(player, spawnPos) || !level.getBlockState(spawnPos).canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        ParasyticLouseEntity louse = WitcheryEntities.PARASYTIC_LOUSE.get().create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (louse == null) {
            return InteractionResult.PASS;
        }

        louse.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot(), 0.0F);
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        louse.setBitePotionContents(contents);
        louse.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.MOB_SUMMONED, null);
        louse.setPersistenceRequired();
        serverLevel.addFreshEntity(louse);
        serverLevel.playSound(null, spawnPos, SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE,
                contents.hasEffects() ? 0.7F : 0.5F, contents.hasEffects() ? 0.8F : 1.1F);
        serverLevel.sendParticles(contents.hasEffects() ? ParticleTypes.WITCH : ParticleTypes.POOF,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 0.25D, spawnPos.getZ() + 0.5D,
                contents.hasEffects() ? 14 : 8, 0.25D, 0.2D, 0.25D, 0.02D);
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
