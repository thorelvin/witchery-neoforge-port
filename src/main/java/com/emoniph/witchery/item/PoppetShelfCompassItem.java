package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
            playCompassFail(level, player);
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
        playShelfSignal(level, player, nearest);
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

    public static float signalStrength(double distanceSq) {
        return signalBand(distanceSq) / 5.0F;
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

    private static void playCompassFail(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.PLAYERS, 0.45F, 0.7F);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 0.8D, player.getZ(),
                6, 0.18D, 0.2D, 0.18D, 0.01D);
    }

    private static void playShelfSignal(Level level, Player player, ShelfSignal signal) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.55F, 1.2F + signal.band() * 0.06F);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                player.getX(), player.getY() + 0.85D, player.getZ(),
                8 + signal.band(), 0.22D, 0.28D, 0.22D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                signal.pos().getX() + 0.5D, signal.pos().getY() + 0.75D, signal.pos().getZ() + 0.5D,
                8, 0.35D, 0.35D, 0.35D, 0.02D);
    }

    private record ShelfSignal(BlockPos pos, double distanceSq, int band) {
    }
}
