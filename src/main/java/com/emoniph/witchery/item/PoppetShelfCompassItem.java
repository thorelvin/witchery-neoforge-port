package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

public class PoppetShelfCompassItem extends Item {
    public PoppetShelfCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
        }

        Optional<ShelfSignal> signal = findNearestShelf(level, player);
        if (signal.isEmpty()) {
            player.displayClientMessage(Component.translatable("witchery.poppet_shelf_compass.none")
                    .withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.SUCCESS;
        }

        ShelfSignal nearest = signal.get();
        Vec3 diff = Vec3.atCenterOf(nearest.pos()).subtract(player.position());
        int distance = (int) Math.sqrt(nearest.distanceSq());
        String direction = getCardinal(Math.atan2(diff.z, diff.x));

        player.displayClientMessage(Component.translatable("witchery.poppet_shelf_compass.locate",
                distance, direction, Component.translatable("witchery.poppet_shelf_compass.signal." + nearest.band()))
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("witchery.poppet_shelf_compass.tooltip")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private static Optional<ShelfSignal> findNearestShelf(Level level, Player player) {
        if (level.getServer() == null) {
            return Optional.empty();
        }

        PoppetShelfBlockEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (PoppetShelfBlockEntity shelf : PoppetShelfBlockEntity.getActiveShelves(level.getServer())) {
            if (shelf.getLevel() != level) {
                continue;
            }
            BlockPos pos = shelf.getBlockPos();
            double distSq = player.distanceToSqr(pos.getX() + 0.5, player.getY(), pos.getZ() + 0.5);
            if (distSq < nearestDistSq) {
                nearest = shelf;
                nearestDistSq = distSq;
            }
        }

        return nearest == null
                ? Optional.empty()
                : Optional.of(new ShelfSignal(nearest.getBlockPos(), nearestDistSq, signalBand(nearestDistSq)));
    }

    private static int signalBand(double distanceSq) {
        if (distanceSq < 64.0) {
            return 5;
        }
        if (distanceSq < 256.0) {
            return 4;
        }
        if (distanceSq < 1024.0) {
            return 3;
        }
        if (distanceSq < 4096.0) {
            return 2;
        }
        if (distanceSq < 16384.0) {
            return 1;
        }
        return 0;
    }

    private static String getCardinal(double atan2) {
        double deg = Math.toDegrees(atan2);
        if (deg < 0) {
            deg += 360;
        }
        if (deg >= 337.5 || deg < 22.5) {
            return "E";
        }
        if (deg < 67.5) {
            return "SE";
        }
        if (deg < 112.5) {
            return "S";
        }
        if (deg < 157.5) {
            return "SW";
        }
        if (deg < 202.5) {
            return "W";
        }
        if (deg < 247.5) {
            return "NW";
        }
        if (deg < 292.5) {
            return "N";
        }
        return "NE";
    }

    private record ShelfSignal(BlockPos pos, double distanceSq, int band) {
    }
}
