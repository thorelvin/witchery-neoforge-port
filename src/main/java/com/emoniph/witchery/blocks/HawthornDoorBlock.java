package com.emoniph.witchery.blocks;

import com.emoniph.witchery.blocks.entity.HawthornDoorBlockEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.TaglockKitItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class HawthornDoorBlock extends DoorBlock implements EntityBlock {
    public HawthornDoorBlock(BlockBehaviour.Properties props) {
        super(BlockSetType.OAK, props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new HawthornDoorBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(WitcheryItems.TAGLOCK_KIT.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (TaglockKitItem.isFilled(stack)) {
            player.displayClientMessage(Component.translatable("witchery.taglock_kit.already_full",
                    TaglockKitItem.getBoundName(stack)).withStyle(ChatFormatting.RED), false);
            return openDoorAfterTaglock(state, level, pos, player, hit);
        }

        if (!(level.getBlockEntity(lowerPos(pos, state)) instanceof HawthornDoorBlockEntity door)) {
            return InteractionResult.PASS;
        }

        HawthornDoorBlockEntity.DrainResult result = door.drainStoredPlayerExcept(player);
        if (result.player() != null) {
            bindSinglePlayerTaglock(player, stack, result.player());
            player.displayClientMessage(Component.translatable("witchery.taglock_kit.collected_hawthorn_door",
                    result.player().name()).withStyle(ChatFormatting.GREEN), false);
            return openDoorAfterTaglock(state, level, pos, player, hit);
        }
        if (result.onlyOwnTaglock()) {
            player.displayClientMessage(Component.translatable("witchery.taglock_kit.only_own_taglock")
                    .withStyle(ChatFormatting.RED), false);
            return openDoorAfterTaglock(state, level, pos, player, hit);
        }
        if (!result.missingPlayers().isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.taglock_kit.player_not_logged_in",
                    String.join(", ", result.missingPlayers())).withStyle(ChatFormatting.RED), false);
            return openDoorAfterTaglock(state, level, pos, player, hit);
        }

        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        player.displayClientMessage(Component.translatable("witchery.taglock_kit.no_hawthorn_door_sample")
                .withStyle(ChatFormatting.RED), false);
        return openDoorAfterTaglock(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && level.getBlockEntity(lowerPos(pos, state)) instanceof HawthornDoorBlockEntity door) {
            door.storePlayer(player);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    private static BlockPos lowerPos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    private InteractionResult openDoorAfterTaglock(BlockState state, Level level, BlockPos pos,
                                                   Player player, BlockHitResult hit) {
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    private static void bindSinglePlayerTaglock(Player user, ItemStack sourceStack,
                                                HawthornDoorBlockEntity.StoredPlayer player) {
        if (sourceStack.getCount() == 1) {
            TaglockKitItem.bind(sourceStack, player.uuid(), player.name());
            return;
        }

        ItemStack filledStack = sourceStack.copyWithCount(1);
        TaglockKitItem.bind(filledStack, player.uuid(), player.name());
        sourceStack.shrink(1);
        if (!user.getInventory().add(filledStack)) {
            user.drop(filledStack, false);
        }
    }
}
