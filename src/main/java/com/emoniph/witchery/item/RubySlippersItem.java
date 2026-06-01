package com.emoniph.witchery.item;

import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;
import java.util.List;
import java.util.Optional;

@SuppressWarnings({"deprecation", "removal"})
public class RubySlippersItem extends Item {
    private static final String HOME_COOLDOWN_TAG = "WITCLastRubySlipperTime";
    private static final String WAYSTONE_COOLDOWN_TAG = "WITCLastRubySlipperWayTime";
    private static final long HOME_COOLDOWN_MILLIS = 30L * 60L * 1000L;
    private static final long WAYSTONE_COOLDOWN_MILLIS = 60L * 1000L;
    private static final int HOME_COST_SAME_DIMENSION = 80;
    private static final int HOME_COST_CROSS_DIMENSION = 120;
    private static final int WAYSTONE_COST_SAME_DIMENSION = 40;
    private static final int WAYSTONE_COST_CROSS_DIMENSION = 80;
    private static final int WAYSTONE_SEARCH_RADIUS = 3;
    private static final int WAYSTONE_TELEPORT_RADIUS = 2;
    private static final double WAYSTONE_RANDOM_MISFIRE_CHANCE = 0.01D;
    private static final int WAYSTONE_RANDOM_MISFIRE_DISTANCE = 500;
    private static final int RANDOM_TELEPORT_ATTEMPTS = 16;

    public RubySlippersItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.witchery.ruby_slippers.tip.1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.witchery.ruby_slippers.tip.2")
                .withStyle(ChatFormatting.RED));
        tooltip.accept(Component.translatable("item.witchery.ruby_slippers.tip.3")
                .withStyle(ChatFormatting.RED));
    }

    public static boolean tryActivate(ServerPlayer player, String message) {
        if (!message.toLowerCase().replace("'", "").startsWith("theres no place like home")) return false;

        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (!(feet.getItem() instanceof RubySlippersItem)) return false;
        if (player.level().dimension().equals(WitcheryDimensions.SPIRIT_WORLD)) return false;

        Optional<ItemEntity> waystoneEntity = findNearbyBoundWaystone(player);
        if (waystoneEntity.isPresent()) {
            return activateWaystone(player, waystoneEntity.get());
        }
        return activateHome(player);
    }

    private static boolean activateWaystone(ServerPlayer player, ItemEntity waystoneEntity) {
        long now = System.currentTimeMillis();
        long last = getLastUseTick(player, WAYSTONE_COOLDOWN_TAG);
        if (now - last < WAYSTONE_COOLDOWN_MILLIS) {
            showCooldown(player, WAYSTONE_COOLDOWN_MILLIS - (now - last));
            return true;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) return true;
        ItemStack waystone = waystoneEntity.getItem();
        Optional<BlockPos> boundPos = WaystoneItem.getBoundPos(waystone);
        Optional<net.minecraft.resources.ResourceKey<Level>> boundDim = WaystoneItem.getBoundDim(waystone);
        if (boundPos.isEmpty() || boundDim.isEmpty()) return true;

        ServerLevel dest = server.getLevel(boundDim.get());
        if (dest == null) return true;

        int cost = dest.dimension().equals(player.level().dimension())
                ? WAYSTONE_COST_SAME_DIMENSION
                : WAYSTONE_COST_CROSS_DIMENSION;
        if (!spendInfusionEnergy(player, cost)) {
            return true;
        }

        waystone.shrink(1);
        if (waystone.isEmpty()) {
            waystoneEntity.discard();
        } else {
            waystoneEntity.setItem(waystone);
        }

        setLastUseTick(player, WAYSTONE_COOLDOWN_TAG, now);
        if (player.getRandom().nextDouble() < WAYSTONE_RANDOM_MISFIRE_CHANCE
                && teleportRandomlyLikeVoidBramble(player, (ServerLevel) player.level(), WAYSTONE_RANDOM_MISFIRE_DISTANCE)) {
            player.displayClientMessage(Component.translatable("item.witchery.ruby_slippers.activated"), false);
            return true;
        }

        BlockPos target = randomNear(player, boundPos.get(), WAYSTONE_TELEPORT_RADIUS);
        teleportTo(player, dest, findSafePos(dest, target));
        player.displayClientMessage(Component.translatable("item.witchery.ruby_slippers.activated"), false);
        return true;
    }

    private static boolean activateHome(ServerPlayer player) {
        long now = System.currentTimeMillis();
        long last = getLastUseTick(player, HOME_COOLDOWN_TAG);
        if (now - last < HOME_COOLDOWN_MILLIS && !player.getAbilities().instabuild) {
            showCooldown(player, HOME_COOLDOWN_MILLIS - (now - last));
            return true;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) return true;

        ServerLevel dest = server.getLevel(Level.OVERWORLD);
        BlockPos target = null;

        // Try bed/respawn position via 1.21.1 API
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        if (respawnConfig != null && respawnConfig.respawnData() != null) {
            ServerLevel respawnDim = server.getLevel(respawnConfig.respawnData().dimension());
            if (respawnDim != null) {
                dest = respawnDim;
                target = respawnConfig.respawnData().pos();
            }
        }

        if (dest == null) return true;
        if (target == null) {
            // Fall back to overworld spawn
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) return true;
            dest = overworld;
            target = new BlockPos(0, 64, 0);
        }

        int cost = dest.dimension().equals(player.level().dimension())
                ? HOME_COST_SAME_DIMENSION
                : HOME_COST_CROSS_DIMENSION;
        if (!spendInfusionEnergy(player, cost)) {
            return true;
        }

        setLastUseTick(player, HOME_COOLDOWN_TAG, now);
        teleportTo(player, dest, findSafePos(dest, target));
        player.displayClientMessage(Component.translatable("item.witchery.ruby_slippers.activated"), false);
        return true;
    }

    private static Optional<ItemEntity> findNearbyBoundWaystone(ServerPlayer player) {
        AABB bounds = player.getBoundingBox().inflate(WAYSTONE_SEARCH_RADIUS);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, bounds,
                item -> WaystoneItem.isBoundWaystone(item.getItem()));
        return items.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
    }

    private static BlockPos findSafePos(ServerLevel level, BlockPos start) {
        for (int dy = 0; dy <= 4; dy++) {
            BlockPos above = start.above(dy);
            if (isSafeLanding(level, above)) {
                return above;
            }

            if (dy > 0) {
                BlockPos below = start.below(dy);
                if (isSafeLanding(level, below)) {
                    return below;
                }
            }
        }
        return start.above();
    }

    private static boolean teleportRandomlyLikeVoidBramble(ServerPlayer player, ServerLevel level, int distance) {
        BlockPos origin = player.blockPosition();
        int doubleDistance = distance * 2;
        for (int attempt = 0; attempt < RANDOM_TELEPORT_ATTEMPTS; attempt++) {
            int targetX = origin.getX() + player.getRandom().nextInt(doubleDistance) - distance;
            int targetZ = origin.getZ() + player.getRandom().nextInt(doubleDistance) - distance;
            BlockPos heightmapPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(targetX, origin.getY(), targetZ));
            BlockPos safe = findSafePos(level, heightmapPos.above());
            if (isSafeLanding(level, safe)) {
                teleportTo(player, level, safe);
                return true;
            }
        }
        return false;
    }

    private static long getLastUseTick(ServerPlayer player, String tag) {
        return player.getPersistentData().getLong(tag).orElse(0L);
    }

    private static void setLastUseTick(ServerPlayer player, String tag, long tick) {
        player.getPersistentData().putLong(tag, tick);
    }

    private static boolean isSafeLanding(ServerLevel level, BlockPos feet) {
        if (!level.getWorldBorder().isWithinBounds(feet)) {
            return false;
        }
        BlockState floor = level.getBlockState(feet.below());
        return !floor.isAir()
                && !floor.liquid()
                && floor.isSolid()
                && isOpenForPlayer(level.getBlockState(feet))
                && isOpenForPlayer(level.getBlockState(feet.above()));
    }

    private static boolean isOpenForPlayer(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && !state.liquid());
    }

    private static BlockPos randomNear(ServerPlayer player, BlockPos start, int radius) {
        int spread = radius * 2 + 1;
        return start.offset(
                player.getRandom().nextInt(spread) - radius,
                0,
                player.getRandom().nextInt(spread) - radius);
    }

    private static void teleportTo(ServerPlayer player, ServerLevel dest, BlockPos safe) {
        dest.playSound(null, safe, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
        player.teleportTo(dest, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), false);
    }

    private static boolean spendInfusionEnergy(ServerPlayer player, int amount) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.getInfusionType() == InfusionType.NONE || data.getInfusionMaxEnergy() <= 0) {
            player.displayClientMessage(Component.translatable("witchery.infuse.infusionrequired")
                    .withStyle(ChatFormatting.RED), false);
            failSound(player);
            return false;
        }
        if (!player.getAbilities().instabuild && data.getInfusionEnergy() < amount) {
            player.displayClientMessage(Component.translatable("witchery.infuse.nocharges")
                    .withStyle(ChatFormatting.RED), false);
            failSound(player);
            return false;
        }
        if (!player.getAbilities().instabuild) {
            data.setInfusionEnergy(data.getInfusionEnergy() - amount, data.getInfusionMaxEnergy());
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        }
        return true;
    }

    private static void showCooldown(ServerPlayer player, long millisLeft) {
        int minutesLeft = (int) Math.max(1, millisLeft / 60000L);
        player.displayClientMessage(Component.translatable(
                "item.witchery.ruby_slippers.cooldown", minutesLeft), false);
    }

    private static void failSound(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.PLAYERS, 0.6F, 0.7F);
    }
}
