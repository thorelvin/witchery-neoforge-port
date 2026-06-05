package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.StatueHobgoblinPatronBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

public class StatueHobgoblinPatronItem extends BlockItem {
    public StatueHobgoblinPatronItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        String owner = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getStringOr(StatueHobgoblinPatronBlockEntity.ITEM_OWNER_KEY, "");
        if (owner.isEmpty()) {
            return super.getName(stack);
        }
        return super.getName(stack).copy().append(Component.literal(" (" + owner + ")"));
    }
}
