package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.blocks.LeechChestBlock;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
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
    private static final double VILLAGER_CHAT_RANGE = 10.0;
    private static final double VILLAGER_CASUAL_CHAT_RANGE = 5.0;
    private static final double LEONARD_GAZE_RANGE = 7.0;
    private static final long LEONARD_GAZE_COOLDOWN_TICKS = 20L * 45L;
    private static final double WITCH_AMBIENT_RANGE = 8.0;
    private static final long WITCH_AMBIENT_COOLDOWN_TICKS = 20L * 55L;
    private static final long WITCH_IDLE_COOLDOWN_TICKS = 20L * 120L;
    private static final int WITCH_IDLE_TRIGGER_TICKS = 20 * 25;
    private static final int WITCH_DARKNESS_CURSE_TICKS = 20 * 45;
    private static final ConcurrentHashMap<String, Long> LEONARD_GAZE_COOLDOWNS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> WITCH_AMBIENT_COOLDOWNS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<java.util.UUID, PlayerAmbientState> PLAYER_AMBIENT_STATES = new ConcurrentHashMap<>();

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
        ChatTarget target = findNearestChatTarget(level, player, message);
        if (target == null) {
            return;
        }
        if (target.kind() == ChatPersona.COVEN_WITCH && target.entity() instanceof CovenWitchEntity witch
                && isStayRequest(message)) {
            witch.beginPlayerConversation(player);
            String witchName = witch.hasCustomName() ? witch.getCustomName().getString() : "Witch";
            String speech = "I'll stay a moment, " + player.getName().getString() + ".";
            player.displayClientMessage(Component.literal("\u00a79<" + witchName + "> \u00a7r" + speech), false);
            NpcBrainService.INSTANCE.rememberCovenExchange(witch, player, message, speech);
            return;
        }
        if (!requestTargetChat(target, player, message)) {
            nudgeTarget(target, player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!player.onGround() && player.getDeltaMovement().y > 0.22D) {
            CovenWitchEntity witch = findNearbyCovenWitch(level, player, WITCH_AMBIENT_RANGE);
            if (witch != null) {
                dispatchWitchAmbient(level, witch, player,
                        "jumping or hopping around near the witch",
                        "If you're trying to fly, start with ointment, not my floorboards.",
                        WITCH_AMBIENT_COOLDOWN_TICKS);
            }
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        handleLeonardGaze(level, player);
        handleWitchAmbientTick(level, player);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || event.isCanceled()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        if (!isChestLike(state)) {
            return;
        }
        CovenWitchEntity witch = findNearbyCovenWitch(level, player, WITCH_AMBIENT_RANGE);
        if (witch == null) {
            return;
        }
        if (player.isShiftKeyDown()) {
            applyWitchDarknessCurse(level, player);
            dispatchWitchAmbient(level, witch, player,
                    "sneaking into the witch's chest; the Darkness curse was applied",
                    "Take from my chest and the dark takes from your eyes.",
                    WITCH_AMBIENT_COOLDOWN_TICKS);
            return;
        }
        dispatchWitchAmbient(level, witch, player,
                "looking into the witch's chest",
                "Careful with that chest, " + player.getName().getString() + ". It remembers fingers.",
                WITCH_AMBIENT_COOLDOWN_TICKS);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || event.isCanceled()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getLevel() instanceof ServerLevel level)
                || !event.getItemStack().is(WitcheryItems.WITCHES_PRIMER.get())) {
            return;
        }
        CovenWitchEntity witch = findNearbyCovenWitch(level, player, WITCH_AMBIENT_RANGE);
        if (witch == null) {
            return;
        }
        dispatchWitchAmbient(level, witch, player,
                "reading the Witches' Primer near the witch",
                "Read slowly. The Primer dislikes greedy eyes.",
                WITCH_AMBIENT_COOLDOWN_TICKS);
    }

    private static void handleLeonardGaze(ServerLevel level, ServerPlayer player) {
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

    private static void handleWitchAmbientTick(ServerLevel level, ServerPlayer player) {
        CovenWitchEntity witch = findNearbyCovenWitch(level, player, WITCH_AMBIENT_RANGE);
        if (witch == null) {
            PLAYER_AMBIENT_STATES.remove(player.getUUID());
            return;
        }
        if (player.isInvisible() || player.hasEffect(MobEffects.INVISIBILITY)) {
            dispatchWitchAmbient(level, witch, player,
                    "invisible player lingering near the witch",
                    "Do not bother hiding, little absence. Your breath still fogs the rafters.",
                    WITCH_AMBIENT_COOLDOWN_TICKS);
            return;
        }
        if (isHoldingPrimer(player)) {
            dispatchWitchAmbient(level, witch, player,
                    "holding and reading the Witches' Primer near the witch",
                    "That Primer has teeth, " + player.getName().getString() + ". Read with both eyes open.",
                    WITCH_AMBIENT_COOLDOWN_TICKS);
            return;
        }
        if (player.isShiftKeyDown()) {
            dispatchWitchAmbient(level, witch, player,
                    "sneaking around near the witch",
                    "Sneaking is for thieves and secrets. Which one are you today?",
                    WITCH_AMBIENT_COOLDOWN_TICKS);
            return;
        }

        PlayerAmbientState previous = PLAYER_AMBIENT_STATES.get(player.getUUID());
        Vec3 position = player.position();
        int idleTicks = 20;
        if (previous != null && previous.level() == level && previous.position().distanceToSqr(position) < 0.0125D
                && player.onGround()) {
            idleTicks = previous.idleTicks() + 20;
        }
        PLAYER_AMBIENT_STATES.put(player.getUUID(), new PlayerAmbientState(level, position, idleTicks));
        if (idleTicks >= WITCH_IDLE_TRIGGER_TICKS) {
            boolean sheltered = !level.canSeeSky(player.blockPosition());
            dispatchWitchAmbient(level, witch, player,
                    sheltered ? "idling quietly in the witch's hut" : "loitering close to the witch",
                    sheltered ? "If you're haunting my hut, haunt it usefully." : "Roots grow from standing still, not answers.",
                    WITCH_IDLE_COOLDOWN_TICKS);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        NpcBrainService.INSTANCE.prewarm();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LEONARD_GAZE_COOLDOWNS.clear();
        WITCH_AMBIENT_COOLDOWNS.clear();
        PLAYER_AMBIENT_STATES.clear();
        NpcBrainService.INSTANCE.shutdown();
    }

    private static CovenWitchEntity findNearbyCovenWitch(ServerLevel level, ServerPlayer player, double range) {
        List<CovenWitchEntity> witches = level.getEntitiesOfClass(
                CovenWitchEntity.class, player.getBoundingBox().inflate(range, 4.0, range));
        if (witches.isEmpty()) {
            return null;
        }
        witches.sort(Comparator.comparingDouble(player::distanceToSqr));
        return witches.getFirst();
    }

    private static ChatTarget findNearestChatTarget(ServerLevel level, ServerPlayer player, String message) {
        List<ChatTarget> candidates = new ArrayList<>();
        addCovenChatCandidates(level, player, message, candidates);
        addLeonardChatCandidates(level, player, message, candidates);
        addVillagerChatCandidates(level, player, message, candidates);
        return candidates.stream()
                .min(Comparator.comparingDouble(ChatTarget::distanceSqr))
                .orElse(null);
    }

    private static void addCovenChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                               List<ChatTarget> candidates) {
        List<CovenWitchEntity> witches = level.getEntitiesOfClass(
                CovenWitchEntity.class, player.getBoundingBox().inflate(CHAT_RANGE, 4.0, CHAT_RANGE));
        if (witches.isEmpty()) {
            return;
        }
        witches.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesLeonard(message) || explicitlyNamesVillager(message);
        for (CovenWitchEntity witch : witches) {
            if (isNamedAddress(witch, message)) {
                candidates.add(new ChatTarget(ChatPersona.COVEN_WITCH, witch, player.distanceToSqr(witch)));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        CovenWitchEntity nearest = witches.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= CASUAL_CHAT_RANGE * CASUAL_CHAT_RANGE
                && (isCasualAddress(message) || isShortFollowUp(message))) {
            candidates.add(new ChatTarget(ChatPersona.COVEN_WITCH, nearest, distance));
            return;
        }
        if (distance <= CHAT_RANGE * CHAT_RANGE && isLookingAt(player, nearest) && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.COVEN_WITCH, nearest, distance));
        }
    }

    private static void addLeonardChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                 List<ChatTarget> candidates) {
        List<LeonardEntity> shades = level.getEntitiesOfClass(
                LeonardEntity.class, player.getBoundingBox().inflate(LEONARD_CHAT_RANGE, 6.0, LEONARD_CHAT_RANGE));
        if (shades.isEmpty()) {
            return;
        }
        shades.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesVillager(message);
        for (LeonardEntity shade : shades) {
            if (isNamedLeonardAddress(shade, message)) {
                candidates.add(new ChatTarget(ChatPersona.LEONARD, shade, player.distanceToSqr(shade)));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        LeonardEntity nearest = shades.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= LEONARD_CASUAL_CHAT_RANGE * LEONARD_CASUAL_CHAT_RANGE
                && isLookingAt(player, nearest)
                && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.LEONARD, nearest, distance));
        }
    }

    private static void addVillagerChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                  List<ChatTarget> candidates) {
        List<Villager> villagers = level.getEntitiesOfClass(
                Villager.class, player.getBoundingBox().inflate(VILLAGER_CHAT_RANGE, 4.0, VILLAGER_CHAT_RANGE));
        if (villagers.isEmpty()) {
            return;
        }
        villagers.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesLeonard(message);
        for (Villager villager : villagers) {
            if (isNamedVillagerAddress(villager, message)) {
                candidates.add(new ChatTarget(ChatPersona.VILLAGER, villager, player.distanceToSqr(villager)));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        Villager nearest = villagers.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= VILLAGER_CASUAL_CHAT_RANGE * VILLAGER_CASUAL_CHAT_RANGE
                && (isCasualAddress(message) || isShortFollowUp(message))) {
            candidates.add(new ChatTarget(ChatPersona.VILLAGER, nearest, distance));
            return;
        }
        if (distance <= VILLAGER_CHAT_RANGE * VILLAGER_CHAT_RANGE && isLookingAt(player, nearest)
                && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.VILLAGER, nearest, distance));
        }
    }

    private static boolean requestTargetChat(ChatTarget target, ServerPlayer player, String message) {
        return switch (target.kind()) {
            case COVEN_WITCH -> target.entity() instanceof CovenWitchEntity witch
                    && NpcBrainService.INSTANCE.requestCovenChat(witch, player, message);
            case LEONARD -> target.entity() instanceof LeonardEntity leonard
                    && NpcBrainService.INSTANCE.requestLeonardChat(leonard, player, message);
            case VILLAGER -> target.entity() instanceof Villager villager
                    && NpcBrainService.INSTANCE.requestVillagerChat(villager, player, message);
        };
    }

    private static void nudgeTarget(ChatTarget target, ServerPlayer player) {
        switch (target.kind()) {
            case COVEN_WITCH -> {
                if (target.entity() instanceof CovenWitchEntity witch) {
                    witch.nudgePlayerAttention(player);
                }
            }
            case LEONARD -> {
                if (target.entity() instanceof LeonardEntity leonard) {
                    leonard.nudgePlayerAttention(player);
                }
            }
            case VILLAGER -> {
                if (target.entity() instanceof Villager villager) {
                    villager.getNavigation().stop();
                    villager.getLookControl().setLookAt(player, 30.0F, 30.0F);
                }
            }
        }
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

    private static boolean isNamedVillagerAddress(Villager villager, String message) {
        String normalized = normalize(message);
        if (normalized.contains("villager")) {
            return true;
        }
        String name = villager.hasCustomName() && villager.getCustomName() != null
                ? villager.getCustomName().getString()
                : "";
        if (!name.isBlank()) {
            String normalizedName = normalize(name);
            if (normalized.contains(normalizedName)) {
                return true;
            }
            String first = normalizedName.split("\\s+")[0];
            if (first.length() >= 3 && normalized.contains(first)) {
                return true;
            }
        }
        String profession = com.emoniph.witchery.ai.NpcPromptBuilder.villagerProfessionName(villager);
        return !profession.isBlank() && normalized.contains(normalize(profession));
    }

    private static boolean explicitlyNamesWitch(String message) {
        String normalized = normalize(message);
        return normalized.contains("witch") || normalized.contains("coven");
    }

    private static boolean explicitlyNamesLeonard(String message) {
        String normalized = normalize(message);
        return normalized.contains("leonard") || normalized.contains("shade of leonard")
                || normalized.contains("the shade");
    }

    private static boolean explicitlyNamesVillager(String message) {
        String normalized = normalize(message);
        return normalized.contains("villager")
                || normalized.contains("apothecary")
                || normalized.contains("farmer")
                || normalized.contains("cleric")
                || normalized.contains("librarian")
                || normalized.contains("cartographer")
                || normalized.contains("armorer")
                || normalized.contains("armourer")
                || normalized.contains("weaponsmith")
                || normalized.contains("toolsmith")
                || normalized.contains("mason")
                || normalized.contains("fletcher")
                || normalized.contains("fisherman")
                || normalized.contains("shepherd")
                || normalized.contains("butcher")
                || normalized.contains("leatherworker")
                || normalized.contains("nitwit");
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

    private static boolean isChestLike(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof LeechChestBlock;
    }

    private static boolean isHoldingPrimer(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        ItemStack offhand = player.getOffhandItem();
        return main.is(WitcheryItems.WITCHES_PRIMER.get()) || offhand.is(WitcheryItems.WITCHES_PRIMER.get());
    }

    private static void dispatchWitchAmbient(ServerLevel level, CovenWitchEntity witch, ServerPlayer player,
                                             String cue, String fallbackSpeech, long cooldownTicks) {
        long now = level.getGameTime();
        String topic = normalize(cue);
        String key = witch.getUUID() + ":" + player.getUUID() + ":" + topic;
        Long blockedUntil = WITCH_AMBIENT_COOLDOWNS.get(key);
        if (blockedUntil != null && blockedUntil > now) {
            return;
        }
        WITCH_AMBIENT_COOLDOWNS.put(key, now + cooldownTicks);
        Component fallback = speechComponent(witch, fallbackSpeech);
        if (!NpcBrainService.INSTANCE.requestCovenAmbient(witch, player, cue, fallback)) {
            witch.nudgePlayerAttention(player);
            player.displayClientMessage(fallback, false);
        }
    }

    private static Component speechComponent(CovenWitchEntity witch, String speech) {
        String witchName = witch.hasCustomName() ? witch.getCustomName().getString() : witch.getDisplayName().getString();
        return Component.literal("\u00a79<" + witchName + "> \u00a7r").append(Component.literal(speech));
    }

    private static void applyWitchDarknessCurse(ServerLevel level, ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, WITCH_DARKNESS_CURSE_TICKS, 0, true, true));
        level.playSound(null, player.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 0.75F, 0.65F);
        level.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0D, player.getZ(),
                14, 0.45D, 0.5D, 0.45D, 0.03D);
    }

    private record PlayerAmbientState(ServerLevel level, Vec3 position, int idleTicks) {
    }

    private enum ChatPersona {
        COVEN_WITCH,
        LEONARD,
        VILLAGER
    }

    private record ChatTarget(ChatPersona kind, LivingEntity entity, double distanceSqr) {
    }
}
