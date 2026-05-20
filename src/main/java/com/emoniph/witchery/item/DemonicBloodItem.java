package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DemonicBloodItem extends Item {

    private static final int MAX_WICKER_CONVERSIONS = 5;

    public DemonicBloodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clickedPos = ctx.getClickedPos();
        if (!isPlainWicker(level.getBlockState(clickedPos))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int converted = bloodifyNearbyWicker(level, clickedPos);
        if (converted <= 0) {
            return InteractionResult.PASS;
        }

        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static int bloodifyNearbyWicker(Level level, BlockPos center) {
        int converted = 0;
        for (int y = center.getY() - 1; y <= center.getY() + 1 && converted < MAX_WICKER_CONVERSIONS; y++) {
            for (int x = center.getX() - 1; x <= center.getX() + 1 && converted < MAX_WICKER_CONVERSIONS; x++) {
                for (int z = center.getZ() - 1; z <= center.getZ() + 1 && converted < MAX_WICKER_CONVERSIONS; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!isPlainWicker(state)) {
                        continue;
                    }
                    BlockState bloodied = WitcheryBlocks.BLOODIED_WICKER_BUNDLE.get().defaultBlockState()
                            .setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
                    level.setBlock(pos, bloodied, Block.UPDATE_ALL);
                    converted++;
                }
            }
        }
        return converted;
    }

    private static boolean isPlainWicker(BlockState state) {
        return state.is(WitcheryBlocks.WICKER_BUNDLE.get());
    }
}
