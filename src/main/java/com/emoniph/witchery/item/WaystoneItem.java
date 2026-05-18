package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;

/**
 * Waystone item — blank until bound by the Rite of Binding.
 * After binding it stores the target coordinates and dimension.
 * Use as ingredient in Rite of Transposition to teleport there.
 */
public class WaystoneItem extends Item {

    private static final String TAG_X   = "wx";
    private static final String TAG_Y   = "wy";
    private static final String TAG_Z   = "wz";
    private static final String TAG_DIM = "wdim";

    public WaystoneItem(Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        Optional<BlockPos> pos = getBoundPos(stack);
        if (pos.isPresent()) {
            BlockPos p = pos.get();
            String dim = getBoundDim(stack).map(k -> k.identifier().getPath()).orElse("?");
            tooltip.accept(Component.literal(p.getX() + ", " + p.getY() + ", " + p.getZ()
                    + " [" + dim + "]").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("witchery.waystone.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    public static boolean isBound(ItemStack stack) {
        return getTag(stack).contains(TAG_X);
    }

    public static void bindTo(ItemStack stack, BlockPos pos, ResourceKey<Level> dim) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(TAG_X, pos.getX());
            tag.putInt(TAG_Y, pos.getY());
            tag.putInt(TAG_Z, pos.getZ());
            tag.putString(TAG_DIM, dim.identifier().toString());
        });
    }

    public static Optional<BlockPos> getBoundPos(ItemStack stack) {
        var tag = getTag(stack);
        if (!tag.contains(TAG_X)) return Optional.empty();
        return Optional.of(new BlockPos(tag.getIntOr(TAG_X, 0), tag.getIntOr(TAG_Y, 0), tag.getIntOr(TAG_Z, 0)));
    }

    public static Optional<ResourceKey<Level>> getBoundDim(ItemStack stack) {
        var tag = getTag(stack);
        if (!tag.contains(TAG_DIM)) return Optional.empty();
        return Optional.of(ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                Identifier.parse(tag.getStringOr(TAG_DIM, ""))));
    }

    private static net.minecraft.nbt.CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
