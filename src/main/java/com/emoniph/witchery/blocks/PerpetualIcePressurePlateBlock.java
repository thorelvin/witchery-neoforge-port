package com.emoniph.witchery.blocks;

import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public class PerpetualIcePressurePlateBlock extends PressurePlateBlock {
    private final boolean icySlippersOnly;

    public PerpetualIcePressurePlateBlock(BlockSetType type, BlockBehaviour.Properties properties,
                                          boolean icySlippersOnly) {
        super(type, properties);
        this.icySlippersOnly = icySlippersOnly;
    }

    @Override
    protected int getSignalStrength(Level level, BlockPos pos) {
        if (!icySlippersOnly) {
            return super.getSignalStrength(level, pos);
        }

        return level.getEntitiesOfClass(LivingEntity.class, TOUCH_AABB.move(pos),
                entity -> !entity.isSpectator()
                        && !entity.isIgnoringBlockTriggers()
                        && wearsIcySlippers(entity)).isEmpty() ? 0 : 15;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos)
                || level.getBlockState(pos.below()).is(WitcheryBlocks.ICE_FENCE.get());
    }

    private static boolean wearsIcySlippers(LivingEntity entity) {
        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        return WitcheryItems.isIcySlippers(boots);
    }
}
