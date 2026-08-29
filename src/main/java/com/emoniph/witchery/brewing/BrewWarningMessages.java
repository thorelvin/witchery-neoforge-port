package com.emoniph.witchery.brewing;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Transient public-chat memory used by warning brews. Only normal chat text is remembered; commands,
 * private messages, and already-decorated chat components are ignored by listening to ServerChatEvent's
 * raw text.
 */
public final class BrewWarningMessages {
    public static final int CHAT_MEMORY_TICKS = 20 * 60;
    public static final int CHAT_BIND_RADIUS = 12;

    private static final Map<ResourceKey<Level>, Map<UUID, RecentMessage>> RECENT = new HashMap<>();

    private BrewWarningMessages() {}

    public static void clearSession() {
        RECENT.clear();
    }

    public static boolean isBindingItem(Item item) {
        return item == Items.WRITABLE_BOOK;
    }

    public static boolean hasBinding(Iterable<Item> items) {
        for (Item item : items) {
            if (isBindingItem(item)) {
                return true;
            }
        }
        return false;
    }

    public static void remember(ServerPlayer player, String rawText) {
        BrewWarningText warning = BrewWarningText.sanitize(rawText);
        if (warning.isEmpty()) {
            return;
        }
        RECENT.computeIfAbsent(player.level().dimension(), k -> new HashMap<>())
                .put(player.getUUID(), new RecentMessage(player.blockPosition(), warning, player.level().getGameTime()));
    }

    public static Optional<BrewWarningText> findNearby(ServerLevel level, BlockPos pos, @Nullable Player preferred) {
        Map<UUID, RecentMessage> messages = RECENT.get(level.dimension());
        if (messages == null || messages.isEmpty()) {
            return Optional.empty();
        }
        long now = level.getGameTime();
        prune(messages, now);
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        if (preferred != null) {
            RecentMessage preferredMessage = messages.get(preferred.getUUID());
            if (preferredMessage != null && preferredMessage.isNear(pos)) {
                return Optional.of(preferredMessage.warning());
            }
        }

        RecentMessage best = null;
        double bestDistance = Double.MAX_VALUE;
        for (RecentMessage message : messages.values()) {
            if (!message.isNear(pos)) {
                continue;
            }
            double distance = message.pos().distSqr(pos);
            if (best == null || distance < bestDistance || distance == bestDistance && message.gameTime() > best.gameTime()) {
                best = message;
                bestDistance = distance;
            }
        }
        return best == null ? Optional.empty() : Optional.of(best.warning());
    }

    private static void prune(Map<UUID, RecentMessage> messages, long now) {
        Iterator<Map.Entry<UUID, RecentMessage>> iterator = messages.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().gameTime() > CHAT_MEMORY_TICKS) {
                iterator.remove();
            }
        }
    }

    private record RecentMessage(BlockPos pos, BrewWarningText warning, long gameTime) {
        boolean isNear(BlockPos target) {
            return pos.distSqr(target) <= CHAT_BIND_RADIUS * CHAT_BIND_RADIUS;
        }
    }
}
