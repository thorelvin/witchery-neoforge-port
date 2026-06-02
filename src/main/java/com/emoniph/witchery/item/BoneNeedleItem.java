package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public class BoneNeedleItem extends Item {
    private static final int BLOOD_COST = 125;

    public BoneNeedleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isVampire()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (state.is(Blocks.WHITE_WOOL) && data.getBloodLevel() >= BLOOD_COST) {
            data.addBlood(-BLOOD_COST);
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.from(serverPlayer, data));
            }

            level.setBlock(pos, WitcheryBlocks.BLOODED_WOOL.get().defaultBlockState(), Block.UPDATE_ALL);
            playStainEffects((ServerLevel) level, pos);
            return InteractionResult.SUCCESS;
        }

        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.5F, 0.6F);
        if (state.is(Blocks.WHITE_WOOL) && data.getBloodLevel() < BLOOD_COST) {
            player.displayClientMessage(Component.translatable("witchery.bone_needle.not_enough_blood", BLOOD_COST), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static void playStainEffects(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.5F,
                0.8F + level.random.nextFloat() * 0.4F);
        level.sendParticles(DustParticleOptions.REDSTONE,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                24, 0.35D, 0.35D, 0.35D, 0.0D);
    }
}
