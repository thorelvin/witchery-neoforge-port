package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class NpcBrainEvents {
    private static final double CHAT_RANGE = 10.0;
    private static final double CASUAL_CHAT_RANGE = 6.0;
    private static final double LEONARD_CHAT_RANGE = 12.0;
    private static final double LEONARD_CASUAL_CHAT_RANGE = 8.0;
    private static final double LEONARD_GAZE_RANGE = 7.0;
    private static final long LEONARD_GAZE_COOLDOWN_TICKS = 20L * 45L;
    private static final ConcurrentHashMap<String, Long> LEONARD_GAZE_COOLDOWNS = new ConcurrentHashMap<>();

    private NpcBrainEvents() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        String message = event.getRawText();
        if (message == null || message.isBlank() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        LeonardEntity leonard = findLeonardChatTarget(level, player, message);
        if (leonard != null) {
            if (!NpcBrainService.INSTANCE.requestLeonardChat(leonard, player, message)) {
                leonard.nudgePlayerAttention(player);
            }
            return;
        }
        CovenWitchEntity witch = findChatTarget(level, player, message);
        if (witch == null) {
            return;
        }
        if (isStayRequest(message)) {
            witch.beginPlayerConversation(player);
            String witchName = witch.hasCustomName() ? witch.getCustomName().getString() : "Witch";
            String speech = "I'll stay a moment, " + player.getName().getString() + ".";
            player.displayClientMessage(Component.literal("\u00a79<" + witchName + "> \u00a7r" + speech), false);
            NpcBrainService.INSTANCE.rememberCovenExchange(witch, player, message, speech);
            return;
        }
        if (!NpcBrainService.INSTANCE.requestCovenChat(witch, player, message)) {
            witch.nudgePlayerAttention(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || player.tickCount % 20 != 0) {
            return;
        }
        LeonardEntity leonard = findGazedLeonard(level, player);
        if (leonard == null) {
            return;
        }
        long now = level.getGameTime();
        String key = leonard.getUUID() + ":" + player.getUUID() + ":gaze";
        Long blockedUntil = LEONARD_GAZE_COOLDOWNS.get(key);
        if (blockedUntil != null && blockedUntil > now) {
            return;
        }
        LEONARD_GAZE_COOLDOWNS.put(key, now + LEONARD_GAZE_COOLDOWN_TICKS);
        String name = leonard.getDisplayName().getString();
        Component fallback = Component.literal("\u00a79<" + name + "> \u00a7r")
                .append(Component.literal("You look at the bargain, and the bargain looks back."));
        NpcBrainService.INSTANCE.requestLeonardGaze(leonard, player, fallback);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        NpcBrainService.INSTANCE.prewarm();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LEONARD_GAZE_COOLDOWNS.clear();
        NpcBrainService.INSTANCE.shutdown();
    }

    private static CovenWitchEntity findChatTarget(ServerLevel level, ServerPlayer player, String message) {
        List<CovenWitchEntity> witches = level.getEntitiesOfClass(
                CovenWitchEntity.class, player.getBoundingBox().inflate(CHAT_RANGE, 4.0, CHAT_RANGE));
        if (witches.isEmpty()) {
            return null;
        }
        witches.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (CovenWitchEntity witch : witches) {
            if (isNamedAddress(witch, message)) {
                return witch;
            }
        }
        CovenWitchEntity nearest = witches.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= CASUAL_CHAT_RANGE * CASUAL_CHAT_RANGE
                && (isCasualAddress(message) || isShortFollowUp(message))) {
            return nearest;
        }
        if (distance <= CHAT_RANGE * CHAT_RANGE && isLookingAt(player, nearest) && isConversational(message)) {
            return nearest;
        }
        return null;
    }

    private static LeonardEntity findLeonardChatTarget(ServerLevel level, ServerPlayer player, String message) {
        List<LeonardEntity> shades = level.getEntitiesOfClass(
                LeonardEntity.class, player.getBoundingBox().inflate(LEONARD_CHAT_RANGE, 6.0, LEONARD_CHAT_RANGE));
        if (shades.isEmpty()) {
            return null;
        }
        shades.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (LeonardEntity shade : shades) {
            if (isNamedLeonardAddress(shade, message)) {
                return shade;
            }
        }
        LeonardEntity nearest = shades.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= LEONARD_CASUAL_CHAT_RANGE * LEONARD_CASUAL_CHAT_RANGE
                && isLookingAt(player, nearest)
                && isConversational(message)) {
            return nearest;
        }
        return null;
    }

    private static LeonardEntity findGazedLeonard(ServerLevel level, ServerPlayer player) {
        List<LeonardEntity> shades = level.getEntitiesOfClass(
                LeonardEntity.class, player.getBoundingBox().inflate(LEONARD_GAZE_RANGE, 4.0, LEONARD_GAZE_RANGE));
        if (shades.isEmpty()) {
            return null;
        }
        shades.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (LeonardEntity shade : shades) {
            if (!shade.isInCombat() && player.hasLineOfSight(shade) && isLookingAt(player, shade)) {
                return shade;
            }
        }
        return null;
    }

    private static boolean isNamedAddress(CovenWitchEntity witch, String message) {
        String normalized = normalize(message);
        if (normalized.contains("witch")) {
            return true;
        }
        String name = witch.hasCustomName() ? witch.getCustomName().getString() : "";
        if (name.isBlank()) {
            return false;
        }
        String normalizedName = normalize(name);
        if (normalized.contains(normalizedName)) {
            return true;
        }
        String first = normalizedName.split("\\s+")[0];
        return first.length() >= 3 && normalized.contains(first);
    }

    private static boolean isNamedLeonardAddress(LeonardEntity leonard, String message) {
        String normalized = normalize(message);
        if (normalized.contains("leonard") || normalized.contains("shade")) {
            return true;
        }
        String name = leonard.hasCustomName() ? leonard.getCustomName().getString() : "";
        if (name.isBlank()) {
            return false;
        }
        String normalizedName = normalize(name);
        if (normalized.contains(normalizedName)) {
            return true;
        }
        String first = normalizedName.split("\\s+")[0];
        return first.length() >= 3 && normalized.contains(first);
    }

    private static boolean isCasualAddress(String message) {
        String normalized = normalize(message);
        return normalized.matches("^(hi|hello|hey|hail|greetings|yo|sup)\\b.*")
                || normalized.contains("?")
                || normalized.contains("trade")
                || normalized.contains("quest")
                || normalized.contains("help");
    }

    private static boolean isConversational(String message) {
        String normalized = normalize(message);
        return normalized.length() <= 160 && !normalized.startsWith("/");
    }

    private static boolean isShortFollowUp(String message) {
        String normalized = normalize(message);
        return normalized.matches("^(how|why|what|where|when|who|yes|no|nah|ok|okay|sure|thanks|thank you)\\b.*")
                || normalized.length() <= 18;
    }

    private static boolean isStayRequest(String message) {
        String normalized = message.toLowerCase(Locale.ROOT)
                .replace("'", "")
                .replace("\u2019", "")
                .replaceAll("[^a-z0-9_ ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.equals("stay")
                || normalized.equals("wait")
                || normalized.equals("stop")
                || normalized.contains("dont run")
                || normalized.contains("do not run")
                || normalized.contains("stop running")
                || normalized.contains("dont leave")
                || normalized.contains("do not leave")
                || normalized.contains("dont wander")
                || normalized.contains("do not wander")
                || normalized.contains("stop wandering")
                || normalized.contains("dont move")
                || normalized.contains("do not move")
                || normalized.contains("stop moving")
                || normalized.contains("stay here")
                || normalized.contains("stay still")
                || normalized.contains("stand still")
                || normalized.contains("hold still")
                || normalized.contains("wait here")
                || normalized.contains("come back");
    }

    private static boolean isLookingAt(ServerPlayer player, LivingEntity witch) {
        Vec3 eye = player.getEyePosition();
        Vec3 witchCenter = witch.position().add(0.0, witch.getBbHeight() * 0.55, 0.0);
        Vec3 offset = witchCenter.subtract(eye);
        if (offset.lengthSqr() < 1.0E-4) {
            return true;
        }
        return player.getLookAngle().normalize().dot(offset.normalize()) > 0.82 && player.hasLineOfSight(witch);
    }

    private static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_ ]+", " ").replaceAll("\\s+", " ").trim();
    }
}
