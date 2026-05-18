package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A poppet that intercepts a specific lethal damage source, saving the bound player once.
 * All logic is handled in PoppetEvents — this class just tags the item with its type.
 */
public class ProtectionPoppetItem extends PoppetItem {

    public enum ProtectionType {
        EARTH,       // lethal fall damage
        WATER,       // lethal drowning
        FIRE,        // lethal fire/lava
        HUNGER,      // lethal starvation
        VOODOO,      // lethal magic/curse
        POPPET_SHIELD // protects other voodoo-protection poppets from Corrupt Poppet curse
    }

    private final ProtectionType protectionType;

    public ProtectionPoppetItem(ProtectionType type, Properties props) {
        super(props);
        this.protectionType = type;
    }

    public ProtectionType getProtectionType() { return protectionType; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, _display, tooltip, flag);
        tooltip.accept(Component.translatable("witchery.poppet.type." + protectionType.name().toLowerCase())
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
