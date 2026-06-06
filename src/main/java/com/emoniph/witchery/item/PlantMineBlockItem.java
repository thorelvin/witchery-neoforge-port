package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.PlantMineBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

public class PlantMineBlockItem extends BlockItem {
    public PlantMineBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        PlantMineBlock.PlantMineDisguise disguise = getDisguise(stack);
        PlantMineBlock.PlantMineEffect effect = getEffect(stack);
        return Component.translatable("block.witchery.plant_mine."
                + disguise.getSerializedName() + "_" + effect.getSerializedName());
    }

    private static PlantMineBlock.PlantMineDisguise getDisguise(ItemStack stack) {
        BlockItemStateProperties state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        PlantMineBlock.PlantMineDisguise disguise = state.get(PlantMineBlock.DISGUISE);
        return disguise == null ? PlantMineBlock.PlantMineDisguise.ROSE : disguise;
    }

    private static PlantMineBlock.PlantMineEffect getEffect(ItemStack stack) {
        BlockItemStateProperties state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        PlantMineBlock.PlantMineEffect effect = state.get(PlantMineBlock.EFFECT);
        return effect == null ? PlantMineBlock.PlantMineEffect.WEBS : effect;
    }
}
