package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.ai.NpcCommandKind;
import com.emoniph.witchery.ai.NpcPersonas;
import com.emoniph.witchery.blocks.LeechChestBlock;
import com.emoniph.witchery.blocks.MirrorBlock;
import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.protection.WitcheryProtectionService;
import com.emoniph.witchery.world.MirrorHelper;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.properties.BedPart;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class NpcBrainEvents {
    private static final double CHAT_RANGE = 10.0;
    private static final double CASUAL_CHAT_RANGE = 6.0;
    private static final double LEONARD_CHAT_RANGE = 12.0;
    private static final double LEONARD_CASUAL_CHAT_RANGE = 8.0;
    private static final double BABA_YAGA_CHAT_RANGE = 12.0;
    private static final double BABA_YAGA_CASUAL_CHAT_RANGE = 8.0;
    private static final double VILLAGER_CHAT_RANGE = 10.0;
    private static final double VILLAGER_CASUAL_CHAT_RANGE = 5.0;
    private static final double HOBGOBLIN_CHAT_RANGE = 10.0;
    private static final double HOBGOBLIN_CASUAL_CHAT_RANGE = 6.0;
    private static final double REFLECTION_CHAT_RANGE = 12.0;
    private static final double REFLECTION_CASUAL_CHAT_RANGE = 8.0;
    private static final double LEONARD_GAZE_RANGE = 7.0;
    private static final double REFLECTION_GAZE_RANGE = 8.0;
    private static final long LEONARD_GAZE_COOLDOWN_TICKS = 20L * 45L;
    private static final long REFLECTION_GAZE_COOLDOWN_TICKS = 20L * 35L;
    private static final long MIRROR_WORLD_AMBIENT_COOLDOWN_TICKS = 20L * 80L;
    private static final double WITCH_AMBIENT_RANGE = 8.0;
    private static final long WITCH_AMBIENT_COOLDOWN_TICKS = 20L * 55L;
    private static final long WITCH_IDLE_COOLDOWN_TICKS = 20L * 120L;
    private static final int WITCH_IDLE_TRIGGER_TICKS = 20 * 25;
    private static final int WITCH_DARKNESS_CURSE_TICKS = 20 * 45;
    private static final ConcurrentHashMap<String, Long> LEONARD_GAZE_COOLDOWNS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> REFLECTION_GAZE_COOLDOWNS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> MIRROR_WORLD_AMBIENT_COOLDOWNS = new ConcurrentHashMap<>();
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
        if (handleMirrorChant(level, player, message)) {
            return;
        }
        ChatTarget target = findNearestChatTarget(level, player, message);
        if (target == null) {
            handleMirrorTalk(level, player, message);
            return;
        }
        if (handleNpcCommand(level, target, player, message)) {
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
        handleReflectionGaze(level, player);
        handleMirrorWorldAmbient(level, player);
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
        Component fallback = NpcBrainService.speechComponent(NpcPersonas.LEONARD, leonard,
                "You look at the bargain, and the bargain looks back.");
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
        REFLECTION_GAZE_COOLDOWNS.clear();
        MIRROR_WORLD_AMBIENT_COOLDOWNS.clear();
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
        addBabaYagaChatCandidates(level, player, message, candidates);
        addVillagerChatCandidates(level, player, message, candidates);
        addHobgoblinChatCandidates(level, player, message, candidates);
        addReflectionChatCandidates(level, player, message, candidates);
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
        boolean otherPersonaNamed = explicitlyNamesLeonard(message) || explicitlyNamesBabaYaga(message)
                || explicitlyNamesVillager(message) || explicitlyNamesHobgoblin(message)
                || explicitlyNamesReflection(message);
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
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesBabaYaga(message)
                || explicitlyNamesVillager(message) || explicitlyNamesHobgoblin(message)
                || explicitlyNamesReflection(message);
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

    private static void addBabaYagaChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                  List<ChatTarget> candidates) {
        List<BabaYagaEntity> crones = level.getEntitiesOfClass(
                BabaYagaEntity.class, player.getBoundingBox().inflate(BABA_YAGA_CHAT_RANGE, 6.0, BABA_YAGA_CHAT_RANGE));
        if (crones.isEmpty()) {
            return;
        }
        crones.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesLeonard(message)
                || explicitlyNamesVillager(message) || explicitlyNamesHobgoblin(message)
                || explicitlyNamesReflection(message);
        for (BabaYagaEntity crone : crones) {
            if (isNamedBabaYagaAddress(crone, message)) {
                candidates.add(new ChatTarget(ChatPersona.BABA_YAGA, crone, player.distanceToSqr(crone)));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        BabaYagaEntity nearest = crones.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= BABA_YAGA_CASUAL_CHAT_RANGE * BABA_YAGA_CASUAL_CHAT_RANGE
                && isLookingAt(player, nearest)
                && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.BABA_YAGA, nearest, distance));
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
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesLeonard(message)
                || explicitlyNamesBabaYaga(message) || explicitlyNamesHobgoblin(message)
                || explicitlyNamesReflection(message);
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

    private static void addHobgoblinChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                   List<ChatTarget> candidates) {
        List<HobgoblinEntity> hobgoblins = level.getEntitiesOfClass(
                HobgoblinEntity.class, player.getBoundingBox().inflate(HOBGOBLIN_CHAT_RANGE, 4.0, HOBGOBLIN_CHAT_RANGE));
        if (hobgoblins.isEmpty()) {
            return;
        }
        hobgoblins.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesLeonard(message)
                || explicitlyNamesBabaYaga(message) || explicitlyNamesVillager(message)
                || explicitlyNamesReflection(message);
        for (HobgoblinEntity hobgoblin : hobgoblins) {
            if (isNamedHobgoblinAddress(hobgoblin, message)) {
                candidates.add(new ChatTarget(ChatPersona.HOBGOBLIN, hobgoblin, player.distanceToSqr(hobgoblin)));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        HobgoblinEntity nearest = hobgoblins.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= HOBGOBLIN_CASUAL_CHAT_RANGE * HOBGOBLIN_CASUAL_CHAT_RANGE
                && (isCasualAddress(message) || isShortFollowUp(message))) {
            candidates.add(new ChatTarget(ChatPersona.HOBGOBLIN, nearest, distance));
            return;
        }
        if (distance <= HOBGOBLIN_CHAT_RANGE * HOBGOBLIN_CHAT_RANGE && isLookingAt(player, nearest)
                && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.HOBGOBLIN, nearest, distance));
        }
    }

    private static void addReflectionChatCandidates(ServerLevel level, ServerPlayer player, String message,
                                                    List<ChatTarget> candidates) {
        List<ReflectionEntity> reflections = level.getEntitiesOfClass(
                ReflectionEntity.class, player.getBoundingBox().inflate(REFLECTION_CHAT_RANGE, 6.0, REFLECTION_CHAT_RANGE));
        if (reflections.isEmpty()) {
            return;
        }
        reflections.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean otherPersonaNamed = explicitlyNamesWitch(message) || explicitlyNamesLeonard(message)
                || explicitlyNamesBabaYaga(message) || explicitlyNamesVillager(message)
                || explicitlyNamesHobgoblin(message);
        for (ReflectionEntity reflection : reflections) {
            if (isNamedReflectionAddress(reflection, message)) {
                candidates.add(new ChatTarget(ChatPersona.REFLECTION, reflection, player.distanceToSqr(reflection)));
            }
        }
        if (otherPersonaNamed) {
            return;
        }
        ReflectionEntity nearest = reflections.getFirst();
        double distance = player.distanceToSqr(nearest);
        if (distance <= REFLECTION_CASUAL_CHAT_RANGE * REFLECTION_CASUAL_CHAT_RANGE
                && isLookingAt(player, nearest)
                && (isCasualAddress(message) || isShortFollowUp(message) || WitcheryDimensions.isMirrorWorld(level))) {
            candidates.add(new ChatTarget(ChatPersona.REFLECTION, nearest, distance));
            return;
        }
        if (distance <= REFLECTION_CHAT_RANGE * REFLECTION_CHAT_RANGE && isLookingAt(player, nearest)
                && isConversational(message)) {
            candidates.add(new ChatTarget(ChatPersona.REFLECTION, nearest, distance));
        }
    }

    private static boolean requestTargetChat(ChatTarget target, ServerPlayer player, String message) {
        return switch (target.kind()) {
            case COVEN_WITCH -> target.entity() instanceof CovenWitchEntity witch
                    && NpcBrainService.INSTANCE.requestCovenChat(witch, player, message);
            case LEONARD -> target.entity() instanceof LeonardEntity leonard
                    && NpcBrainService.INSTANCE.requestLeonardChat(leonard, player, message);
            case BABA_YAGA -> target.entity() instanceof BabaYagaEntity baba
                    && NpcBrainService.INSTANCE.requestBabaYagaChat(baba, player, message);
            case VILLAGER -> target.entity() instanceof Villager villager
                    && NpcBrainService.INSTANCE.requestVillagerChat(villager, player, message);
            case HOBGOBLIN -> target.entity() instanceof HobgoblinEntity hobgoblin
                    && NpcBrainService.INSTANCE.requestHobgoblinChat(hobgoblin, player, message);
            case REFLECTION -> target.entity() instanceof ReflectionEntity reflection
                    && NpcBrainService.INSTANCE.requestReflectionChat(reflection, player, message);
        };
    }

    private static boolean handleNpcCommand(ServerLevel level, ChatTarget target, ServerPlayer player, String message) {
        Optional<NpcCommandKind> command = NpcCommandKind.candidate(message);
        if (command.isEmpty()) {
            return false;
        }
        if (NpcCommandKind.isClearlyQuestion(message)) {
            return false;
        }
        NpcCommandKind kind = command.get();
        if (!supportsCommand(target, kind)) {
            return false;
        }
        if (!WitcheryProtectionService.canCommandNpc(player, target.entity(), kind)) {
            denyNpcCommand(level, target, player, message);
            return true;
        }
        if (kind.isPlainDirect(message)) {
            executeNpcCommand(level, target, player, message, kind);
            return true;
        }
        return requestNpcCommandIntent(level, target, player, message, kind);
    }

    private static boolean supportsCommand(ChatTarget target, NpcCommandKind kind) {
        return switch (target.kind()) {
            case COVEN_WITCH -> kind == NpcCommandKind.STAY || kind == NpcCommandKind.STOP;
            case HOBGOBLIN -> kind == NpcCommandKind.MINE
                    || kind == NpcCommandKind.WORSHIP
                    || kind == NpcCommandKind.STAY
                    || kind == NpcCommandKind.STOP
                    || kind == NpcCommandKind.COME_HERE
                    || kind == NpcCommandKind.GO_AWAY
                    || kind == NpcCommandKind.FOLLOW
                    || kind == NpcCommandKind.GUARD;
            case LEONARD, BABA_YAGA, VILLAGER, REFLECTION -> false;
        };
    }

    private static void denyNpcCommand(ServerLevel level, ChatTarget target, ServerPlayer player, String message) {
        switch (target.kind()) {
            case COVEN_WITCH -> {
                if (target.entity() instanceof CovenWitchEntity witch) {
                    String speech = witch.isRecruited()
                            ? "My word is already sworn, " + player.getName().getString() + "."
                            : "Words are wind unless I choose to heed them.";
                    player.displayClientMessage(NpcBrainService.speechComponent(NpcPersonas.COVEN_WITCH, witch, speech),
                            false);
                    NpcBrainService.INSTANCE.rememberCovenExchange(witch, player, message, speech);
                }
            }
            case HOBGOBLIN -> {
                if (target.entity() instanceof HobgoblinEntity hobgoblin) {
                    String owner = hobgoblin.commandOwnerName();
                    String speech = owner.isBlank()
                            ? "*clutches the rope* \"Not your leash. Not your work.\""
                            : "*squints* \"Rope has a name. " + owner + ". Not you.\"";
                    sayHobgoblin(level, hobgoblin, player, message, speech);
                }
            }
            case LEONARD, BABA_YAGA, VILLAGER, REFLECTION -> nudgeTarget(target, player);
        }
    }

    private static boolean requestNpcCommandIntent(ServerLevel level, ChatTarget target, ServerPlayer player,
                                                   String message, NpcCommandKind kind) {
        return switch (target.kind()) {
            case COVEN_WITCH -> target.entity() instanceof CovenWitchEntity witch
                    && NpcBrainService.INSTANCE.requestCommandIntent(
                    NpcPersonas.COVEN_WITCH,
                    witch,
                    player,
                    message,
                    kind,
                    () -> executeNpcCommand(level, target, player, message, kind),
                    () -> requestChatAfterRejectedCommand(level, target, player, message));
            case HOBGOBLIN -> target.entity() instanceof HobgoblinEntity hobgoblin
                    && NpcBrainService.INSTANCE.requestCommandIntent(
                    NpcPersonas.HOBGOBLIN,
                    hobgoblin,
                    player,
                    message,
                    kind,
                    () -> executeNpcCommand(level, target, player, message, kind),
                    () -> requestChatAfterRejectedCommand(level, target, player, message));
            case LEONARD, BABA_YAGA, VILLAGER, REFLECTION -> false;
        };
    }

    private static void requestChatAfterRejectedCommand(ServerLevel level, ChatTarget target,
                                                        ServerPlayer player, String message) {
        if (requestTargetChat(target, player, message)) {
            return;
        }
        if (target.kind() == ChatPersona.HOBGOBLIN && target.entity() instanceof HobgoblinEntity hobgoblin) {
            sayHobgoblin(level, hobgoblin, player, message,
                    "*shrugs* \"Speak plain. Work-order, or just words?\"");
            return;
        }
        nudgeTarget(target, player);
    }

    private static void executeNpcCommand(ServerLevel level, ChatTarget target, ServerPlayer player,
                                          String message, NpcCommandKind kind) {
        switch (target.kind()) {
            case COVEN_WITCH -> {
                if (target.entity() instanceof CovenWitchEntity witch) {
                    executeCovenWitchCommand(witch, player, message, kind);
                }
            }
            case HOBGOBLIN -> {
                if (target.entity() instanceof HobgoblinEntity hobgoblin) {
                    executeHobgoblinCommand(level, hobgoblin, player, message, kind);
                }
            }
            case LEONARD, BABA_YAGA, VILLAGER, REFLECTION -> {
            }
        }
    }

    private static void executeCovenWitchCommand(CovenWitchEntity witch, ServerPlayer player,
                                                 String message, NpcCommandKind kind) {
        if (kind != NpcCommandKind.STAY && kind != NpcCommandKind.STOP) {
            return;
        }
        witch.beginPlayerConversation(player);
        String speech = kind == NpcCommandKind.STOP
                ? "I'll stop a moment, " + player.getName().getString() + "."
                : "I'll stay a moment, " + player.getName().getString() + ".";
        player.displayClientMessage(NpcBrainService.speechComponent(NpcPersonas.COVEN_WITCH, witch, speech),
                false);
        NpcBrainService.INSTANCE.rememberCovenExchange(witch, player, message, speech);
    }

    private static void executeHobgoblinCommand(ServerLevel level, HobgoblinEntity hobgoblin,
                                                ServerPlayer player, String message, NpcCommandKind kind) {
        String speech = switch (kind) {
            case MINE -> executeHobgoblinMineCommand(hobgoblin, player);
            case WORSHIP -> executeHobgoblinWorshipCommand(hobgoblin, player);
            case STAY -> {
                hobgoblin.claimCommandOwner(player);
                hobgoblin.beginPlayerConversation(player);
                yield "*squints* \"Feet stay. For now.\"";
            }
            case STOP -> {
                hobgoblin.commandStopWork(player);
                hobgoblin.beginPlayerConversation(player);
                yield "*grumbles* \"Stopping. Work stops. Prayer stops. Happy?\"";
            }
            case COME_HERE -> {
                hobgoblin.getLookControl().setLookAt(player, 30.0F, 30.0F);
                boolean moving = hobgoblin.getNavigation().moveTo(player, 0.85D);
                if (moving) {
                    hobgoblin.claimCommandOwner(player);
                }
                yield moving
                        ? "Coming, coming. Big feet wait."
                        : "*sniffs* \"Path says no. Move closer.\"";
            }
            case GO_AWAY -> {
                hobgoblin.getLookControl().setLookAt(player, 30.0F, 30.0F);
                boolean moving = moveHobgoblinAwayFromPlayer(hobgoblin, player);
                if (moving) {
                    hobgoblin.claimCommandOwner(player);
                }
                yield moving
                        ? "*backs off* \"Away, away. Good?\""
                        : "*shrugs* \"No path away. World too cramped.\"";
            }
            case FOLLOW -> hobgoblin.commandFollow(player)
                    ? "*huffs* \"Following. Rope or no rope, feet listen for now.\""
                    : "*sniffs* \"Can't follow what isn't here.\"";
            case GUARD -> hobgoblin.commandGuard(player)
                    ? "*bares teeth* \"Guarding. Bad things get bit first.\""
                    : "*sniffs* \"Can't guard smoke. Stand close.\"";
        };
        if (!speech.isBlank()) {
            sayHobgoblin(level, hobgoblin, player, message, speech);
        }
    }

    private static String executeHobgoblinMineCommand(HobgoblinEntity hobgoblin, ServerPlayer player) {
        if (!hobgoblin.isLeashed()) {
            return "*shrugs* \"No rope, no work. Hobgoblin feet choose elsewhere.\"";
        }
        if (!hobgoblin.hasMiningPickaxe()) {
            return "*gestures* \"Give pickaxe first. Then work, work.\"";
        }
        if (hobgoblin.commandMine(player)) {
            return "Work, work... rope bossy, shiny maybe.";
        }
        return "*sniffs* \"No good stone here. Move feet, move rope.\"";
    }

    private static String executeHobgoblinWorshipCommand(HobgoblinEntity hobgoblin, ServerPlayer player) {
        if (hobgoblin.commandWorship(player)) {
            return "*nods* \"Patron sees. Small prayers go up.\"";
        }
        return "*gestures* \"No goblin altar nearby. Put stone god close.\"";
    }

    private static boolean moveHobgoblinAwayFromPlayer(HobgoblinEntity hobgoblin, ServerPlayer player) {
        Vec3 away = hobgoblin.position().subtract(player.position());
        if (away.lengthSqr() < 1.0E-4D) {
            away = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(-1.0D);
        }
        Vec3 target = hobgoblin.position().add(away.normalize().scale(6.0D));
        return hobgoblin.getNavigation().moveTo(target.x, target.y, target.z, 0.85D);
    }

    private static void sayHobgoblin(ServerLevel level, HobgoblinEntity hobgoblin, ServerPlayer player,
                                     String playerMessage, String speech) {
        player.displayClientMessage(NpcBrainService.speechComponent(NpcPersonas.HOBGOBLIN, hobgoblin, speech),
                false);
        level.playSound(null, hobgoblin.blockPosition(), SoundEvents.VILLAGER_AMBIENT,
                SoundSource.NEUTRAL, 0.35F, 0.82F);
        NpcBrainService.INSTANCE.rememberExchange(NpcPersonas.HOBGOBLIN, hobgoblin, player, playerMessage, speech);
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
            case BABA_YAGA -> {
                if (target.entity() instanceof BabaYagaEntity baba) {
                    baba.getNavigation().stop();
                    baba.getLookControl().setLookAt(player, 30.0F, 30.0F);
                }
            }
            case VILLAGER -> {
                if (target.entity() instanceof Villager villager) {
                    villager.getNavigation().stop();
                    villager.getLookControl().setLookAt(player, 30.0F, 30.0F);
                }
            }
            case HOBGOBLIN -> {
                if (target.entity() instanceof HobgoblinEntity hobgoblin) {
                    hobgoblin.nudgePlayerAttention(player);
                }
            }
            case REFLECTION -> {
                if (target.entity() instanceof ReflectionEntity reflection) {
                    reflection.getLookControl().setLookAt(player, 30.0F, 30.0F);
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

    private static void handleReflectionGaze(ServerLevel level, ServerPlayer player) {
        if (!WitcheryDimensions.isMirrorWorld(level)) {
            return;
        }
        ReflectionEntity reflection = findGazedReflection(level, player);
        if (reflection == null) {
            return;
        }
        long now = level.getGameTime();
        String key = reflection.getUUID() + ":" + player.getUUID() + ":gaze";
        Long blockedUntil = REFLECTION_GAZE_COOLDOWNS.get(key);
        if (blockedUntil != null && blockedUntil > now) {
            return;
        }
        REFLECTION_GAZE_COOLDOWNS.put(key, now + REFLECTION_GAZE_COOLDOWN_TICKS);
        Component fallback = NpcBrainService.speechComponent(NpcPersonas.REFLECTION, reflection,
                "Your echo arrived early, " + player.getName().getString() + ".");
        NpcBrainService.INSTANCE.requestReflectionGaze(reflection, player, fallback);
    }

    private static ReflectionEntity findGazedReflection(ServerLevel level, ServerPlayer player) {
        List<ReflectionEntity> reflections = level.getEntitiesOfClass(
                ReflectionEntity.class, player.getBoundingBox().inflate(REFLECTION_GAZE_RANGE, 5.0, REFLECTION_GAZE_RANGE));
        if (reflections.isEmpty()) {
            return null;
        }
        reflections.sort(Comparator.comparingDouble(player::distanceToSqr));
        for (ReflectionEntity reflection : reflections) {
            if (player.hasLineOfSight(reflection) && isLookingAt(player, reflection)) {
                return reflection;
            }
        }
        return null;
    }

    private static void handleMirrorWorldAmbient(ServerLevel level, ServerPlayer player) {
        if (!WitcheryDimensions.isMirrorWorld(level)) {
            return;
        }
        long now = level.getGameTime();
        String key = player.getUUID() + ":mirror_world";
        Long blockedUntil = MIRROR_WORLD_AMBIENT_COOLDOWNS.get(key);
        if (blockedUntil != null && blockedUntil > now) {
            return;
        }
        if (level.random.nextFloat() > 0.08F) {
            return;
        }
        MIRROR_WORLD_AMBIENT_COOLDOWNS.put(key, now + MIRROR_WORLD_AMBIENT_COOLDOWN_TICKS);
        String speech = mirrorAmbientLine(player);
        mirrorSpeech(level, player, speech);
    }

    private static String mirrorAmbientLine(ServerPlayer player) {
        String name = player.getName().getString();
        String[] lines = {
                "Your footstep arrives a breath late, " + name + ".",
                "The silver remembers every face that tried to leave.",
                "Somewhere nearby, a mirror exhales.",
                "The room is not empty. It is only copying silence.",
                "Your shadow looks toward the way home before you do."
        };
        return lines[Math.floorMod(player.getUUID().hashCode() + player.tickCount / 20, lines.length)];
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

    private static boolean isNamedBabaYagaAddress(BabaYagaEntity baba, String message) {
        String normalized = normalize(message);
        if (normalized.contains("baba yaga") || normalized.contains("baba") || normalized.contains("yaga")) {
            return true;
        }
        String name = baba.hasCustomName() && baba.getCustomName() != null
                ? baba.getCustomName().getString()
                : "";
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

    private static boolean isNamedHobgoblinAddress(HobgoblinEntity hobgoblin, String message) {
        String normalized = normalize(message);
        if (normalized.contains("hobgoblin")
                || normalized.contains("koboldite")
                || normalized.contains("patron")
                || normalized.contains("goblin altar")
                || normalized.contains("mog")
                || normalized.contains("gulg")
                || NpcCommandKind.MINE.matchesCandidate(normalized)
                || NpcCommandKind.WORSHIP.matchesCandidate(normalized)) {
            return true;
        }
        String name = hobgoblin.hasCustomName() && hobgoblin.getCustomName() != null
                ? hobgoblin.getCustomName().getString()
                : "";
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

    private static boolean isNamedReflectionAddress(ReflectionEntity reflection, String message) {
        String normalized = normalize(message);
        if (explicitlyNamesReflection(message)) {
            return true;
        }
        String ownerName = reflection.getOwnerName();
        if (ownerName != null && !ownerName.isBlank()) {
            String owner = normalize(ownerName);
            if (normalized.contains(owner + " reflection")
                    || normalized.contains(owner + "s reflection")
                    || normalized.contains("my reflection")) {
                return true;
            }
        }
        String name = reflection.hasCustomName() && reflection.getCustomName() != null
                ? reflection.getCustomName().getString()
                : "";
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

    private static boolean explicitlyNamesWitch(String message) {
        String normalized = normalize(message);
        return normalized.contains("witch") || normalized.contains("coven");
    }

    private static boolean explicitlyNamesLeonard(String message) {
        String normalized = normalize(message);
        return normalized.contains("leonard") || normalized.contains("shade of leonard")
                || normalized.contains("the shade");
    }

    private static boolean explicitlyNamesBabaYaga(String message) {
        String normalized = normalize(message);
        return normalized.contains("baba yaga") || normalized.contains("baba") || normalized.contains("yaga");
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

    private static boolean explicitlyNamesHobgoblin(String message) {
        String normalized = normalize(message);
        return normalized.contains("hobgoblin")
                || normalized.contains("koboldite")
                || normalized.contains("patron")
                || normalized.contains("goblin altar")
                || normalized.contains("mog")
                || normalized.contains("gulg")
                || NpcCommandKind.MINE.matchesCandidate(normalized)
                || NpcCommandKind.WORSHIP.matchesCandidate(normalized);
    }

    private static boolean explicitlyNamesReflection(String message) {
        String normalized = normalize(message);
        return normalized.contains("reflection")
                || normalized.contains("mirror guardian")
                || normalized.contains("mirror world")
                || normalized.contains("my echo")
                || normalized.contains("mirror mirror");
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

    private static boolean handleMirrorChant(ServerLevel level, ServerPlayer player, String message) {
        String normalized = normalize(message);
        if (!normalized.startsWith("mirror mirror")) {
            return false;
        }
        BlockPos mirrorPos = findNearestMirror(level, player, 12.0, false);
        if (mirrorPos == null) {
            return false;
        }
        if (WitcheryDimensions.isMirrorWorld(level)
                && containsAny(normalized, "send me home", "go home", "take me home", "let me out")) {
            BlockPos exitMirror = findNearestMirror(level, player, 16.0, true);
            if (exitMirror != null) {
                mirrorSpeech(level, player, "Then step through, " + player.getName().getString()
                        + ". The way home still has your shape.");
                MirrorHelper.exitMirrorWorld(player, level, exitMirror);
                playMirrorCue(level, exitMirror);
            } else {
                mirrorSpeech(level, player, "No home-facing glass answers you.");
                playMirrorCue(level, mirrorPos);
            }
            return true;
        }
        if (containsAny(normalized, "fairest", "on the wall", "who is fair", "who is the fair")) {
            mirrorSpeech(level, player, "You are fairest here, " + player.getName().getString()
                    + ", because no wiser face stayed to compete.");
            playMirrorCue(level, mirrorPos);
            return true;
        }
        mirrorSpeech(level, player, "The mirror listens, but only some names open doors.");
        playMirrorCue(level, mirrorPos);
        return true;
    }

    private static boolean handleMirrorTalk(ServerLevel level, ServerPlayer player, String message) {
        String normalized = normalize(message);
        if (!containsAny(normalized, "mirror", "fairest", "reflection", "glass")) {
            return false;
        }
        BlockPos mirrorPos = findNearestMirror(level, player, 7.0, false);
        if (mirrorPos == null) {
            return false;
        }
        String speech;
        if (containsAny(normalized, "who are you", "what are you")) {
            speech = "I am only glass until you give me a face.";
        } else if (containsAny(normalized, "how do i leave", "get out", "go home", "send me home")) {
            speech = WitcheryDimensions.isMirrorWorld(level)
                    ? "The linked mirror is the kindest exit this room remembers."
                    : "Step through only if you are ready to meet what steps back.";
        } else if (containsAny(normalized, "fairest")) {
            speech = "Fairest is a dangerous word. It attracts hungry reflections.";
        } else {
            speech = "The silver clouds, then clears with a face almost like yours.";
        }
        mirrorSpeech(level, player, speech);
        playMirrorCue(level, mirrorPos);
        return true;
    }

    private static BlockPos findNearestMirror(ServerLevel level, ServerPlayer player, double range,
                                              boolean requireUnbreakable) {
        int radius = (int) Math.ceil(range);
        BlockPos center = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos check : BlockPos.betweenClosed(center.offset(-radius, -4, -radius),
                center.offset(radius, 4, radius))) {
            BlockState state = level.getBlockState(check);
            if (!state.is(WitcheryBlocks.MIRROR.get())) {
                continue;
            }
            BlockPos foot = mirrorFootPos(state, check);
            BlockState footState = level.getBlockState(foot);
            if (!footState.is(WitcheryBlocks.MIRROR.get())) {
                continue;
            }
            if (requireUnbreakable && (!footState.hasProperty(MirrorBlock.UNBREAKABLE)
                    || !footState.getValue(MirrorBlock.UNBREAKABLE))) {
                continue;
            }
            double distance = player.distanceToSqr(Vec3.atCenterOf(foot));
            if (distance < bestDistance && distance <= range * range) {
                best = foot.immutable();
                bestDistance = distance;
            }
        }
        return best;
    }

    private static BlockPos mirrorFootPos(BlockState state, BlockPos pos) {
        return state.hasProperty(MirrorBlock.PART) && state.getValue(MirrorBlock.PART) == BedPart.HEAD
                ? pos.below()
                : pos;
    }

    private static void mirrorSpeech(ServerLevel level, ServerPlayer player, String speech) {
        player.displayClientMessage(NpcBrainService.speechComponent("Mirror", 0x55FFFF, speech), false);
        level.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                6, 0.35D, 0.25D, 0.35D, 0.01D);
    }

    private static void playMirrorCue(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.BLOCKS, 0.45F, 0.7F);
        level.sendParticles(ParticleTypes.WITCH,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                8, 0.25D, 0.35D, 0.25D, 0.02D);
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
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
        return NpcBrainService.speechComponent(NpcPersonas.COVEN_WITCH, witch, speech);
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
        BABA_YAGA,
        VILLAGER,
        HOBGOBLIN,
        REFLECTION
    }

    private record ChatTarget(ChatPersona kind, LivingEntity entity, double distanceSqr) {
    }
}
