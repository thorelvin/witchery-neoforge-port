package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Waystone item — blank until bound by the Rite of Binding.
 * After binding it stores the target coordinates and dimension.
 * Use as ingredient in Rite of Transposition to teleport there.
 */
public class WaystoneItem extends Item {

    private static final int SCRY_USE_DURATION = 1200;
    private static final int SCRY_FEEDBACK_INTERVAL = 20;

    private static final String TAG_X   = "wx";
    private static final String TAG_Y   = "wy";
    private static final String TAG_Z   = "wz";
    private static final String TAG_DIM = "wdim";

    public WaystoneItem(Properties props) { super(props); }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return isBound(stack) ? 1 : stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isBound(stack) ? SCRY_USE_DURATION : super.getUseDuration(stack, entity);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof net.minecraft.world.entity.player.Player player) || !isBound(stack)) {
            return;
        }

        int elapsedTicks = getUseDuration(stack, entity) - remainingUseDuration;
        if (elapsedTicks % SCRY_FEEDBACK_INTERVAL != 0) {
            return;
        }

        Optional<BlockPos> targetPos = getBoundPos(stack);
        Optional<ResourceKey<Level>> targetDim = getBoundDim(stack);
        if (targetPos.isEmpty() || targetDim.isEmpty() || !targetDim.get().equals(level.dimension())) {
            player.stopUsingItem();
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos pos = targetPos.get();
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                6, 0.35, 0.4, 0.35, 0.05);
        if (elapsedTicks == 0) {
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.4f);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (!level.isClientSide() && entity instanceof net.minecraft.world.entity.player.Player player && isBound(stack)) {
            int elapsedTicks = getUseDuration(stack, entity) - timeCharged;
            if (elapsedTicks > 0) {
                player.displayClientMessage(Component.literal("The waystone vision fades.")
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
        return true;
    }

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
        return hasCompleteBinding(getTag(stack));
    }

    public static boolean isWaystone(ItemStack stack) {
        return stack.getItem() instanceof WaystoneItem;
    }

    public static boolean isBoundWaystone(ItemStack stack) {
        return isWaystone(stack) && isBound(stack);
    }

    public static boolean isBlankWaystone(ItemStack stack) {
        return stack.is(WitcheryItems.WAYSTONE.get()) && !isBound(stack);
    }

    public static void bindTo(ItemStack stack, BlockPos pos, ResourceKey<Level> dim) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(TAG_X, pos.getX());
            tag.putInt(TAG_Y, pos.getY());
            tag.putInt(TAG_Z, pos.getZ());
            tag.putString(TAG_DIM, dim.identifier().toString());
        });
        stack.set(DataComponents.RARITY, Rarity.UNCOMMON);
    }

    public static Optional<BlockPos> getBoundPos(ItemStack stack) {
        var tag = getTag(stack);
        if (!hasCompleteBinding(tag)) return Optional.empty();
        return Optional.of(new BlockPos(tag.getIntOr(TAG_X, 0), tag.getIntOr(TAG_Y, 0), tag.getIntOr(TAG_Z, 0)));
    }

    public static Optional<ResourceKey<Level>> getBoundDim(ItemStack stack) {
        var tag = getTag(stack);
        if (!hasCompleteBinding(tag)) return Optional.empty();
        try {
            return Optional.of(ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    Identifier.parse(tag.getStringOr(TAG_DIM, ""))));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static boolean hasCompleteBinding(net.minecraft.nbt.CompoundTag tag) {
        return tag.contains(TAG_X) && tag.contains(TAG_Y) && tag.contains(TAG_Z) && tag.contains(TAG_DIM);
    }

    private static net.minecraft.nbt.CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
