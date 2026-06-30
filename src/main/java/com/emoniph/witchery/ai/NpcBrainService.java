package com.emoniph.witchery.ai;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.init.WitcherySoundEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class NpcBrainService {
    public static final NpcBrainService INSTANCE = new NpcBrainService();
    private static final long CHAT_COOLDOWN_TICKS = 20L;
    private static final int WARMUP_MAX_TOKENS = 24;
    private static final int SOCIAL_INTENT_MAX_TOKENS = 16;
    private static final int SOCIAL_INTENT_TIMEOUT_MILLIS = 2500;
    private static final int MESSAGE_ANALYSIS_MAX_TOKENS = 96;
    private static final int MESSAGE_ANALYSIS_TIMEOUT_MILLIS = 2200;

    private final Object executorLock = new Object();
    private final Object warmupExecutorLock = new Object();
    private final Object warmupLock = new Object();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong warmups = new AtomicLong();
    private final ConcurrentHashMap<String, Long> cooldownUntil = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> socialCooldownUntil = new ConcurrentHashMap<>();
    private volatile ExecutorService executor;
    private volatile ExecutorService warmupExecutor;
    private volatile String warmedConfigKey = "";
    private volatile boolean warmupInFlight;
    private volatile long lastWarmupMillis = -1L;
    private volatile String lastWarmupError = "";
    private volatile String lastError = "";

    private NpcBrainService() {
    }

    public boolean prewarm() {
        if (!WitcheryConfig.LLM_ENABLED.get() || !WitcheryConfig.LLM_PREWARM_ENABLED.get()) {
            return false;
        }
        String key = warmupConfigKey();
        synchronized (warmupLock) {
            if (warmupInFlight || key.equals(warmedConfigKey)) {
                return false;
            }
            warmupInFlight = true;
            lastWarmupError = "";
        }

        LlmProvider provider;
        try {
            provider = providerFromConfig(
                    Math.max(WitcheryConfig.LLM_TIMEOUT_MILLIS.get(), WitcheryConfig.LLM_PREWARM_TIMEOUT_MILLIS.get()),
                    WARMUP_MAX_TOKENS,
                    warmupExecutor());
        } catch (IllegalArgumentException ex) {
            completeWarmup(key, -1L, ex);
            return false;
        }

        long started = System.nanoTime();
        provider.generate(NpcPromptBuilder.warmup())
                .orTimeout(WitcheryConfig.LLM_PREWARM_TIMEOUT_MILLIS.get() + 500L, TimeUnit.MILLISECONDS)
                .whenComplete((response, error) ->
                        completeWarmup(key, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), error));
        return true;
    }

    public boolean requestCovenGreeting(CovenWitchEntity witch, ServerPlayer player, Component fallback) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        CovenWitchPersona persona = NpcPersonas.COVEN_WITCH;
        return requestPersonaResponse(persona, witch, player,
                persona.greetingPrompt(witch, player, contextFor(persona, witch, player)),
                "greet", "", fallback, true);
    }

    public boolean requestCovenChat(CovenWitchEntity witch, ServerPlayer player, String playerMessage) {
        return requestChat(NpcPersonas.COVEN_WITCH, witch, player, playerMessage);
    }

    public boolean requestCovenAmbient(CovenWitchEntity witch, ServerPlayer player, String ambientCue,
                                       Component fallback) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        CovenWitchPersona persona = NpcPersonas.COVEN_WITCH;
        String cue = NpcBrainResponse.sanitize(ambientCue == null ? "" : ambientCue, 180);
        return requestPersonaResponse(persona, witch, player,
                NpcPromptBuilder.covenWitchAmbient(witch, player, contextFor(persona, witch, player), cue),
                "ambient", "[ambient] " + cue, fallback, true, false);
    }

    public boolean requestLeonardGreeting(LeonardEntity leonard, ServerPlayer player, Component fallback) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        LeonardPersona persona = NpcPersonas.LEONARD;
        return requestPersonaResponse(persona, leonard, player,
                persona.greetingPrompt(leonard, player, contextFor(persona, leonard, player)),
                "greet", "", fallback, true);
    }

    public boolean requestLeonardGaze(LeonardEntity leonard, ServerPlayer player, Component fallback) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        LeonardPersona persona = NpcPersonas.LEONARD;
        return requestPersonaResponse(persona, leonard, player,
                NpcPromptBuilder.leonardGaze(leonard, player, contextFor(persona, leonard, player)),
                "gaze", "", fallback, true);
    }

    public boolean requestLeonardChat(LeonardEntity leonard, ServerPlayer player, String playerMessage) {
        return requestChat(NpcPersonas.LEONARD, leonard, player, playerMessage);
    }

    public boolean requestVillagerChat(Villager villager, ServerPlayer player, String playerMessage) {
        return requestChat(NpcPersonas.VILLAGER, villager, player, playerMessage);
    }

    public <E extends LivingEntity> boolean requestChat(NpcPersona<E> persona, E entity, ServerPlayer player,
                                                        String playerMessage) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        if (persona == null || entity == null || player == null) {
            return false;
        }
        if (tryHandleSocialInteraction(persona, entity, player, playerMessage)) {
            return true;
        }
        Optional<NpcSocialAction> ambiguousAction = persona.ambiguousSocialAction(playerMessage);
        if (ambiguousAction.isPresent()
                && requestSocialIntentClassification(persona, entity, player, playerMessage, ambiguousAction.get())) {
            return true;
        }
        return requestPersonaChatWithAnalysis(persona, entity, player, playerMessage);
    }

    private <E extends LivingEntity> boolean requestSocialIntentClassification(NpcPersona<E> persona, E entity,
                                                                               ServerPlayer player,
                                                                               String playerMessage,
                                                                               NpcSocialAction action) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        if (!entity.isAlive() || entity.level() != player.level()) {
            return false;
        }
        int current = inFlight.incrementAndGet();
        if (current > WitcheryConfig.LLM_MAX_CONCURRENT_REQUESTS.get()) {
            inFlight.decrementAndGet();
            rejected.incrementAndGet();
            return false;
        }

        ServerLevel level = (ServerLevel) player.level();
        LlmProvider provider;
        int timeoutMillis = Math.max(250, Math.min(WitcheryConfig.LLM_TIMEOUT_MILLIS.get(),
                SOCIAL_INTENT_TIMEOUT_MILLIS));
        try {
            provider = providerFromConfig(timeoutMillis, SOCIAL_INTENT_MAX_TOKENS, executor());
        } catch (IllegalArgumentException ex) {
            inFlight.decrementAndGet();
            failure(ex);
            return false;
        }

        persona.beginConversation(entity, player);
        NpcBrainPrompt prompt = NpcPromptBuilder.socialIntentCheck(persona.roleName(), action, playerMessage);
        provider.generate(prompt)
                .orTimeout(timeoutMillis + 500L, TimeUnit.MILLISECONDS)
                .whenComplete((response, error) -> {
                    inFlight.decrementAndGet();
                    MinecraftServer server = level.getServer();
                    server.execute(() -> handleSocialIntentClassification(level, persona, player, entity,
                            playerMessage, action, response, error));
                });
        return true;
    }

    private <E extends LivingEntity> boolean requestPersonaChatWithAnalysis(NpcPersona<E> persona, E entity,
                                                                           ServerPlayer player,
                                                                           String playerMessage) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        if (!entity.isAlive() || entity.level() != player.level()) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (isCoolingDown(entity.getUUID(), player.getUUID(), "chat", level.getGameTime())) {
            return false;
        }
        int current = inFlight.incrementAndGet();
        if (current > WitcheryConfig.LLM_MAX_CONCURRENT_REQUESTS.get()) {
            inFlight.decrementAndGet();
            rejected.incrementAndGet();
            return false;
        }

        LlmProvider provider;
        int timeoutMillis = Math.max(250, Math.min(WitcheryConfig.LLM_TIMEOUT_MILLIS.get(),
                MESSAGE_ANALYSIS_TIMEOUT_MILLIS));
        try {
            provider = providerFromConfig(timeoutMillis, MESSAGE_ANALYSIS_MAX_TOKENS, executor());
        } catch (IllegalArgumentException ex) {
            inFlight.decrementAndGet();
            failure(ex);
            return false;
        }

        persona.beginConversation(entity, player);
        NpcBrainPrompt prompt = NpcPromptBuilder.messageAnalysis(persona.roleName(),
                player.getName().getString(), playerMessage);
        provider.generate(prompt)
                .orTimeout(timeoutMillis + 500L, TimeUnit.MILLISECONDS)
                .whenComplete((response, error) -> {
                    inFlight.decrementAndGet();
                    MinecraftServer server = level.getServer();
                    server.execute(() -> handleMessageAnalysis(level, persona, player, entity,
                            playerMessage, response, error));
                });
        return true;
    }

    private <E extends LivingEntity> boolean requestPersonaResponse(NpcPersona<E> persona, E entity,
                                                                    ServerPlayer player, NpcBrainPrompt prompt,
                                                                    String trigger, String playerMessage,
                                                                    Component fallback, boolean showFallback) {
        return requestPersonaResponse(persona, entity, player, prompt, trigger, playerMessage, fallback,
                showFallback, true);
    }

    private <E extends LivingEntity> boolean requestPersonaResponse(NpcPersona<E> persona, E entity,
                                                                    ServerPlayer player, NpcBrainPrompt prompt,
                                                                    String trigger, String playerMessage,
                                                                    Component fallback, boolean showFallback,
                                                                    boolean rememberExchange) {
        return requestPersonaResponse(persona, entity, player, prompt, trigger, playerMessage, fallback,
                showFallback, rememberExchange, "");
    }

    private <E extends LivingEntity> boolean requestPersonaResponse(NpcPersona<E> persona, E entity,
                                                                    ServerPlayer player, NpcBrainPrompt prompt,
                                                                    String trigger, String playerMessage,
                                                                    Component fallback, boolean showFallback,
                                                                    boolean rememberExchange,
                                                                    String memoryContextOverride) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        if (!entity.isAlive() || entity.level() != player.level()) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        String cooldownKey = entity.getUUID() + ":" + player.getUUID() + ":" + trigger;
        if (isCoolingDown(cooldownKey, now)) {
            return false;
        }
        int current = inFlight.incrementAndGet();
        if (current > WitcheryConfig.LLM_MAX_CONCURRENT_REQUESTS.get()) {
            inFlight.decrementAndGet();
            rejected.incrementAndGet();
            return false;
        }

        cooldownUntil.put(cooldownKey, now + cooldownTicks(trigger));
        LlmProvider provider;
        try {
            provider = providerFromConfig();
        } catch (IllegalArgumentException ex) {
            inFlight.decrementAndGet();
            failure(ex);
            return false;
        }

        persona.beginConversation(entity, player);
        provider.generate(prompt)
                .orTimeout(WitcheryConfig.LLM_TIMEOUT_MILLIS.get() + 500L, TimeUnit.MILLISECONDS)
                .whenComplete((response, error) -> {
                    inFlight.decrementAndGet();
                    MinecraftServer server = level.getServer();
                    server.execute(() -> handlePersonaResponse(level, persona, entity.getUUID(), player, entity,
                            playerMessage, fallback, showFallback, rememberExchange, memoryContextOverride,
                            response, error));
                });
        return true;
    }

    public int probe(CommandSourceStack source, String promptText) {
        LlmProvider provider;
        try {
            provider = providerFromConfig();
        } catch (IllegalArgumentException ex) {
            failure(ex);
            source.sendFailure(Component.literal("[AI] " + ex.getMessage()));
            return 0;
        }
        int current = inFlight.incrementAndGet();
        if (current > WitcheryConfig.LLM_MAX_CONCURRENT_REQUESTS.get()) {
            inFlight.decrementAndGet();
            rejected.incrementAndGet();
            source.sendFailure(Component.literal("[AI] Busy; try again in a moment."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("[AI] Probe queued for " + WitcheryConfig.LLM_MODEL.get() + "."), false);
        MinecraftServer server = source.getServer();
        provider.generate(NpcPromptBuilder.probe(promptText))
                .orTimeout(WitcheryConfig.LLM_TIMEOUT_MILLIS.get() + 500L, TimeUnit.MILLISECONDS)
                .whenComplete((response, error) -> {
                    inFlight.decrementAndGet();
                    server.execute(() -> {
                        if (error != null || response == null) {
                            failure(error);
                            source.sendFailure(Component.literal("[AI] Probe failed: " + shortError(error)));
                            return;
                        }
                        successes.incrementAndGet();
                        source.sendSuccess(() -> Component.literal("[AI] " + response.speech()
                                + " (intent=" + response.intent().id() + ")"), false);
                    });
                });
        return 1;
    }

    public String statusLine() {
        String enabled = WitcheryConfig.LLM_ENABLED.get() ? "enabled" : "disabled";
        LlmEndpointValidator.Validation validation = LlmEndpointValidator.validate(
                WitcheryConfig.LLM_ENDPOINT.get(), WitcheryConfig.LLM_ALLOW_LAN_ENDPOINT.get());
        return "[AI] " + enabled
                + ", provider=" + WitcheryConfig.LLM_PROVIDER.get()
                + ", model=" + WitcheryConfig.LLM_MODEL.get()
                + ", endpoint=" + WitcheryConfig.LLM_ENDPOINT.get()
                + ", endpointValid=" + validation.allowed()
                + ", inFlight=" + inFlight.get()
                + ", warmup=" + warmupStatus()
                + ", warmups=" + warmups.get()
                + ", ok=" + successes.get()
                + ", failed=" + failures.get()
                + ", rejected=" + rejected.get()
                + (lastError.isBlank() ? "" : ", lastError=" + lastError);
    }

    public void reload() {
        cooldownUntil.clear();
        socialCooldownUntil.clear();
        resetWarmup();
        lastError = "";
    }

    public void shutdown() {
        synchronized (executorLock) {
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }
        synchronized (warmupExecutorLock) {
            if (warmupExecutor != null) {
                warmupExecutor.shutdownNow();
                warmupExecutor = null;
            }
        }
        inFlight.set(0);
        resetWarmup();
    }

    private void resetWarmup() {
        synchronized (warmupLock) {
            warmedConfigKey = "";
            warmupInFlight = false;
            lastWarmupMillis = -1L;
            lastWarmupError = "";
        }
    }

    private String warmupStatus() {
        if (!WitcheryConfig.LLM_ENABLED.get() || !WitcheryConfig.LLM_PREWARM_ENABLED.get()) {
            return "off";
        }
        String currentKey = warmupConfigKey();
        synchronized (warmupLock) {
            if (warmupInFlight) {
                return "warming";
            }
            if (currentKey.equals(warmedConfigKey)) {
                return lastWarmupMillis >= 0L ? "ready(" + lastWarmupMillis + "ms)" : "ready";
            }
            if (!lastWarmupError.isBlank()) {
                return "failed(" + lastWarmupError + ")";
            }
            return "pending";
        }
    }

    public void rememberCovenExchange(CovenWitchEntity witch, ServerPlayer player, String playerSpeech, String npcSpeech) {
        rememberExchange(NpcPersonas.COVEN_WITCH, witch, player, playerSpeech, npcSpeech);
    }

    public <E extends LivingEntity> void rememberExchange(NpcPersona<E> persona, E entity, ServerPlayer player,
                                                          String playerSpeech, String npcSpeech) {
        if (persona == null || entity == null || player == null || player.level() != entity.level()) {
            return;
        }
        NpcMemoryData.get((ServerLevel) player.level()).rememberExchange(entity.getUUID(), player.getUUID(),
                persona.displayName(entity), player.getName().getString(), playerSpeech, npcSpeech,
                ((ServerLevel) player.level()).getGameTime());
    }

    private <E extends LivingEntity> String contextFor(NpcPersona<E> persona, E entity, ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        return NpcMemoryData.get(level).getContext(entity.getUUID(), player.getUUID(), persona.roleName(),
                persona.displayName(entity), player.getName().getString(), persona::relationshipNotes);
    }

    private static long cooldownTicks(String trigger) {
        long configured = WitcheryConfig.LLM_COOLDOWN_TICKS.get();
        return "chat".equals(trigger) ? Math.min(configured, CHAT_COOLDOWN_TICKS) : configured;
    }

    private boolean isCoolingDown(UUID entityId, UUID playerId, String trigger, long now) {
        return isCoolingDown(entityId + ":" + playerId + ":" + trigger, now);
    }

    private boolean isCoolingDown(String cooldownKey, long now) {
        Long blockedUntil = cooldownUntil.get(cooldownKey);
        return blockedUntil != null && blockedUntil > now;
    }

    private <E extends LivingEntity> void handlePersonaResponse(ServerLevel level, NpcPersona<E> persona, UUID entityId,
                                                                ServerPlayer player, E entity, String playerMessage,
                                                                Component fallback, boolean showFallback,
                                                                boolean rememberExchange,
                                                                String memoryContextOverride,
                                                                NpcBrainResponse response, Throwable error) {
        if (!player.isAlive() || player.level() != level) {
            return;
        }
        if (!entity.isAlive() || entity.level() != level) {
            return;
        }
        persona.completeConversation(entity, player);
        if (error != null || response == null) {
            failure(error);
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayPersonaFallback(level, persona, entityId, player, entity, playerMessage,
                        memoryContextOverride);
            }
            return;
        }
        if (!persona.allowsIntent(response.intent(), entity, player)) {
            rejected.incrementAndGet();
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayPersonaFallback(level, persona, entityId, player, entity, playerMessage,
                        memoryContextOverride);
            }
            return;
        }
        NpcMemoryData memoryData = NpcMemoryData.get(level);
        String memoryContext = memoryContextFor(memoryData, entityId, player, persona, entity, memoryContextOverride);
        var polishedSpeech = persona.polishRaw(response.speech(), entity, player, playerMessage, memoryContext,
                NpcPromptBuilder.clockTime(level.getDayTime()));
        if (polishedSpeech.isEmpty()) {
            Optional<String> relaxedSpeech = rawAfterRepeatedFallback(level, persona, entityId, player, entity,
                    playerMessage, response.speech(), memoryContext);
            if (relaxedSpeech.isPresent()) {
                displayPersonaSpeech(level, persona, entity, player, playerMessage, relaxedSpeech.get(),
                        rememberExchange);
                if (!response.memory().isBlank()) {
                    NpcMemoryData.get(level).remember(entityId, player.getUUID(), response.memory(), level.getGameTime());
                }
                return;
            }
            rejected.incrementAndGet();
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayPersonaFallback(level, persona, entityId, player, entity, playerMessage,
                        memoryContextOverride);
            }
            return;
        }
        String lastNpcSpeech = memoryData.getLastNpcSpeech(entityId, player.getUUID());
        if (NpcBrainResponse.sameMeaningForRepetition(polishedSpeech.get(), lastNpcSpeech)
                || memoryData.repeatsRecentNpcSpeech(entityId, player.getUUID(), polishedSpeech.get())
                || memoryData.repeatsRecentPlayerSpeech(entityId, player.getUUID(), polishedSpeech.get())) {
            rejected.incrementAndGet();
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayPersonaFallback(level, persona, entityId, player, entity, playerMessage,
                        memoryContextOverride);
            }
            return;
        }
        Optional<String> supportFollowup = persona.supportFollowup(playerMessage, polishedSpeech.get());
        displayPersonaSpeech(level, persona, entity, player, playerMessage, polishedSpeech.get(), supportFollowup,
                rememberExchange);
        if (!response.memory().isBlank()) {
            NpcMemoryData.get(level).remember(entityId, player.getUUID(), response.memory(), level.getGameTime());
        }
    }

    private <E extends LivingEntity> void handleSocialIntentClassification(ServerLevel level, NpcPersona<E> persona,
                                                                           ServerPlayer player, E entity,
                                                                           String playerMessage,
                                                                           NpcSocialAction action,
                                                                           NpcBrainResponse response,
                                                                           Throwable error) {
        if (!player.isAlive() || player.level() != level) {
            return;
        }
        if (!entity.isAlive() || entity.level() != level) {
            return;
        }
        if (error != null || response == null) {
            failure(error);
            requestNormalChatAfterSocialIntent(level, persona, entity, player, playerMessage);
            return;
        }
        if (socialIntentAccepted(response)) {
            if (persona.handleSocialAction(entity, player, playerMessage, action,
                    socialContext(level, persona, entity, player, playerMessage))) {
                persona.completeConversation(entity, player);
                return;
            }
        }
        requestNormalChatAfterSocialIntent(level, persona, entity, player, playerMessage);
    }

    private <E extends LivingEntity> void handleMessageAnalysis(ServerLevel level, NpcPersona<E> persona,
                                                               ServerPlayer player, E entity, String playerMessage,
                                                               NpcBrainResponse response, Throwable error) {
        if (!player.isAlive() || player.level() != level) {
            return;
        }
        if (!entity.isAlive() || entity.level() != level) {
            return;
        }
        NpcMessageAnalysis analysis = error == null && response != null
                ? NpcMessageAnalysis.fromResponse(response, player.getName().getString(), playerMessage)
                : NpcMessageAnalysis.deterministic(player.getName().getString(), playerMessage);
        if (analysis.hasMemory()) {
            NpcMemoryData.get(level).remember(entity.getUUID(), player.getUUID(),
                    analysis.memory(), level.getGameTime());
        }
        boolean queued = requestPersonaChatWithAnalysisContext(persona, entity, player, playerMessage, analysis);
        if (!queued) {
            persona.completeConversation(entity, player);
            tryDisplayPersonaFallback(level, persona, entity.getUUID(), player, entity, playerMessage,
                    NpcMessageAnalysis.appendContext(contextFor(persona, entity, player), analysis));
        }
    }

    private <E extends LivingEntity> void requestNormalChatAfterSocialIntent(ServerLevel level, NpcPersona<E> persona,
                                                                            E entity, ServerPlayer player,
                                                                            String playerMessage) {
        boolean queued = requestPersonaChatWithAnalysis(persona, entity, player, playerMessage);
        if (!queued) {
            persona.completeConversation(entity, player);
            tryDisplayPersonaFallback(level, persona, entity.getUUID(), player, entity, playerMessage, "");
        }
    }

    private <E extends LivingEntity> boolean requestPersonaChatWithAnalysisContext(NpcPersona<E> persona, E entity,
                                                                                  ServerPlayer player,
                                                                                  String playerMessage,
                                                                                  NpcMessageAnalysis analysis) {
        String context = NpcMessageAnalysis.appendContext(contextFor(persona, entity, player), analysis);
        return requestPersonaResponse(persona, entity, player,
                persona.chatPrompt(entity, player, context, playerMessage),
                "chat", playerMessage, null, false, true, context);
    }

    static boolean socialIntentAccepted(NpcBrainResponse response) {
        if (response == null) {
            return false;
        }
        String speech = response.speech() == null ? "" : response.speech().trim().toLowerCase(Locale.ROOT);
        return speech.equals("yes") || speech.equals("\"yes\"") || speech.equals("true");
    }

    private <E extends LivingEntity> boolean tryDisplayPersonaFallback(ServerLevel level, NpcPersona<E> persona,
                                                                       UUID entityId, ServerPlayer player, E entity,
                                                                       String playerMessage,
                                                                       String memoryContextOverride) {
        if (playerMessage == null || playerMessage.isBlank()) {
            return false;
        }
        NpcMemoryData memoryData = NpcMemoryData.get(level);
        String memoryContext = memoryContextFor(memoryData, entityId, player, persona, entity, memoryContextOverride);
        var fallback = persona.fallback(level, entity, player, playerMessage, memoryContext)
                .flatMap(speech -> persona.polishFallback(speech, entity, player, playerMessage, memoryContext,
                        NpcPromptBuilder.clockTime(level.getDayTime())));
        String lastNpcSpeech = memoryData.getLastNpcSpeech(entityId, player.getUUID());
        String fallbackSpeech = fallback.orElseGet(() ->
                persona.lastResortFallback(entity, player, playerMessage, lastNpcSpeech));
        if (NpcBrainResponse.sameMeaningForRepetition(fallbackSpeech, lastNpcSpeech)
                || memoryData.repeatsRecentNpcSpeech(entityId, player.getUUID(), fallbackSpeech)
                || memoryData.repeatsRecentPlayerSpeech(entityId, player.getUUID(), fallbackSpeech)) {
            Optional<String> alternate = persona.alternateForRepeatedFallback(entity, player, playerMessage,
                    lastNpcSpeech);
            if (alternate.isPresent()) {
                fallbackSpeech = alternate.get();
            }
        }
        displayPersonaSpeech(level, persona, entity, player, playerMessage, fallbackSpeech, Optional.empty());
        return true;
    }

    private <E extends LivingEntity> String memoryContextFor(NpcMemoryData memoryData, UUID entityId,
                                                             ServerPlayer player, NpcPersona<E> persona, E entity,
                                                             String memoryContextOverride) {
        if (memoryContextOverride != null && !memoryContextOverride.isBlank()) {
            return memoryContextOverride;
        }
        return memoryData.getContext(entityId, player.getUUID(), persona.roleName(),
                persona.displayName(entity), player.getName().getString(), persona::relationshipNotes);
    }

    private <E extends LivingEntity> Optional<String> rawAfterRepeatedFallback(ServerLevel level,
                                                                               NpcPersona<E> persona, UUID entityId,
                                                                               ServerPlayer player, E entity,
                                                                               String playerMessage,
                                                                               String rawSpeech,
                                                                               String memoryContext) {
        if (playerMessage == null || playerMessage.isBlank()) {
            return Optional.empty();
        }
        Optional<String> fallback = personaFallback(level, persona, entity, player, playerMessage, memoryContext);
        if (fallback.isEmpty()) {
            return Optional.empty();
        }
        NpcMemoryData memoryData = NpcMemoryData.get(level);
        if (!memoryData.hasRecentNpcAnswerForPlayerSpeech(entityId, player.getUUID(), playerMessage, fallback.get())) {
            return Optional.empty();
        }
        Optional<String> relaxed = persona.polishFallback(rawSpeech, entity, player, playerMessage, memoryContext,
                NpcPromptBuilder.clockTime(level.getDayTime()));
        if (relaxed.isEmpty()) {
            return Optional.empty();
        }
        String lastNpcSpeech = memoryData.getLastNpcSpeech(entityId, player.getUUID());
        if (NpcBrainResponse.sameMeaningForRepetition(relaxed.get(), lastNpcSpeech)
                || memoryData.repeatsRecentNpcSpeech(entityId, player.getUUID(), relaxed.get())
                || memoryData.repeatsRecentPlayerSpeech(entityId, player.getUUID(), relaxed.get())) {
            return Optional.empty();
        }
        return relaxed;
    }

    private <E extends LivingEntity> Optional<String> personaFallback(ServerLevel level, NpcPersona<E> persona,
                                                                      E entity, ServerPlayer player,
                                                                      String playerMessage, String memoryContext) {
        if (player == null) {
            return Optional.empty();
        }
        return persona.fallback(level, entity, player, playerMessage, memoryContext)
                .flatMap(speech -> persona.polishFallback(speech, entity, player, playerMessage, memoryContext,
                        NpcPromptBuilder.clockTime(level.getDayTime())));
    }

    private <E extends LivingEntity> void displayPersonaSpeech(ServerLevel level, NpcPersona<E> persona, E entity,
                                                               ServerPlayer player, String playerMessage,
                                                               String speech) {
        displayPersonaSpeech(level, persona, entity, player, playerMessage, speech, true);
    }

    private <E extends LivingEntity> void displayPersonaSpeech(ServerLevel level, NpcPersona<E> persona, E entity,
                                                               ServerPlayer player, String playerMessage,
                                                               String speech, boolean rememberExchange) {
        displayPersonaSpeech(level, persona, entity, player, playerMessage, speech,
                persona.supportFollowup(playerMessage, speech), rememberExchange);
    }

    private <E extends LivingEntity> void displayPersonaSpeech(ServerLevel level, NpcPersona<E> persona, E entity,
                                                               ServerPlayer player, String playerMessage,
                                                               String speech, Optional<String> supportFollowup) {
        displayPersonaSpeech(level, persona, entity, player, playerMessage, speech, supportFollowup, true);
    }

    private <E extends LivingEntity> void displayPersonaSpeech(ServerLevel level, NpcPersona<E> persona, E entity,
                                                               ServerPlayer player, String playerMessage,
                                                               String speech, Optional<String> supportFollowup,
                                                               boolean rememberExchange) {
        String npcName = persona.displayName(entity);
        persona.nudgeAttention(entity, player);
        playVisibleBeat(level, entity, speech);
        successes.incrementAndGet();
        player.displayClientMessage(Component.literal("\u00a79<" + npcName + "> \u00a7r" + speech), false);
        supportFollowup.ifPresent(followup ->
                player.displayClientMessage(Component.literal("\u00a79<" + npcName + "> \u00a7r" + followup), false));
        if (rememberExchange) {
            String rememberedSpeech = supportFollowup.map(followup -> speech + " " + followup).orElse(speech);
            rememberExchange(persona, entity, player, playerMessage, rememberedSpeech);
        }
    }

    private static void playVisibleBeat(ServerLevel level, LivingEntity entity, String speech) {
        Optional<String> beat = NpcBrainResponse.visibleBeat(speech);
        if (beat.isEmpty()) {
            return;
        }
        String cue = beat.get();
        if (swingsForBeat(cue)) {
            entity.swing(InteractionHand.MAIN_HAND, true);
        }
        SoundEvent sound = soundForBeat(entity, cue);
        float volume = volumeForBeat(cue);
        float pitch = pitchForBeat(cue);
        level.playSound(null, entity.blockPosition(), sound, SoundSource.NEUTRAL, volume, pitch);
    }

    private static boolean swingsForBeat(String beat) {
        return switch (beat) {
            case "gestures", "studies your coin", "taps a finger", "shrugs", "folds her arms",
                    "adjusts her robe" -> true;
            default -> false;
        };
    }

    private static SoundEvent soundForBeat(LivingEntity entity, String beat) {
        if (entity instanceof LeonardEntity) {
            return WitcherySoundEvents.ENTITY_LEONARD_SAY.get();
        }
        if (entity instanceof Villager) {
            return switch (beat) {
                case "nods", "smiles thinly" -> SoundEvents.VILLAGER_YES;
                case "narrows her eyes", "folds her arms" -> SoundEvents.VILLAGER_NO;
                default -> SoundEvents.VILLAGER_AMBIENT;
            };
        }
        return switch (beat) {
            case "nods", "smiles thinly" -> SoundEvents.VILLAGER_YES;
            case "narrows her eyes", "folds her arms" -> SoundEvents.VILLAGER_NO;
            case "sighs", "shrugs", "yawns" -> SoundEvents.WITCH_AMBIENT;
            default -> SoundEvents.WITCH_AMBIENT;
        };
    }

    private static float volumeForBeat(String beat) {
        return switch (beat) {
            case "lowers her voice", "leans closer", "studies you", "studies your coin" -> 0.28F;
            case "yawns", "sighs", "shrugs" -> 0.35F;
            default -> 0.42F;
        };
    }

    private static float pitchForBeat(String beat) {
        return switch (beat) {
            case "yawns" -> 0.62F;
            case "sighs", "shrugs", "narrows her eyes", "folds her arms" -> 0.78F;
            case "nods", "smiles thinly" -> 0.9F;
            default -> 0.82F;
        };
    }

    private <E extends LivingEntity> boolean tryHandleSocialInteraction(NpcPersona<E> persona, E entity,
                                                                        ServerPlayer player, String playerMessage) {
        Optional<NpcSocialAction> action = persona.socialAction(playerMessage);
        if (action.isEmpty()) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        return persona.handleSocialAction(entity, player, playerMessage, action.get(),
                socialContext(level, persona, entity, player, playerMessage));
    }

    private <E extends LivingEntity> NpcSocialContext<E> socialContext(ServerLevel level, NpcPersona<E> persona,
                                                                       E entity, ServerPlayer player,
                                                                       String playerMessage) {
        return new NpcSocialContext<>() {
            @Override
            public ServerLevel level() {
                return level;
            }

            @Override
            public NpcMemoryData memoryData() {
                return NpcMemoryData.get(level);
            }

            @Override
            public boolean markCooldown(String action, long ticks) {
                return markSocialCooldown(level, entity, player, action, ticks);
            }

            @Override
            public void playFeedback(boolean pleasant) {
                socialFx(level, player, pleasant);
            }

            @Override
            public void say(String speech) {
                displayPersonaSpeech(level, persona, entity, player, playerMessage, speech, Optional.empty());
            }
        };
    }

    private boolean markSocialCooldown(ServerLevel level, LivingEntity entity, ServerPlayer player,
                                       String action, long ticks) {
        String key = entity.getUUID() + ":" + player.getUUID() + ":" + action;
        long now = level.getGameTime();
        Long blockedUntil = socialCooldownUntil.get(key);
        if (blockedUntil != null && blockedUntil > now) {
            return false;
        }
        socialCooldownUntil.put(key, now + ticks);
        return true;
    }

    private static void socialFx(ServerLevel level, ServerPlayer player, boolean pleasant) {
        level.playSound(null, player.blockPosition(),
                pleasant ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.WITCH_CELEBRATE,
                SoundSource.NEUTRAL, 0.7F, pleasant ? 1.35F : 0.75F);
        level.sendParticles(pleasant ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.WITCH,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                12, 0.45D, 0.45D, 0.45D, 0.02D);
    }

    private LlmProvider providerFromConfig() {
        return providerFromConfig(WitcheryConfig.LLM_TIMEOUT_MILLIS.get(), WitcheryConfig.LLM_MAX_TOKENS.get(),
                executor());
    }

    private LlmProvider providerFromConfig(int timeoutMillis, int maxTokens, ExecutorService requestExecutor) {
        LlmProviderConfig config = new LlmProviderConfig(
                WitcheryConfig.LLM_ENDPOINT.get(),
                WitcheryConfig.LLM_MODEL.get(),
                timeoutMillis,
                maxTokens,
                WitcheryConfig.LLM_ALLOW_LAN_ENDPOINT.get(),
                WitcheryConfig.LLM_KEEP_ALIVE_MINUTES.get());
        LlmEndpointValidator.Validation validation = LlmEndpointValidator.validate(
                config.endpoint(), config.allowLanEndpoint());
        if (!validation.allowed()) {
            throw new IllegalArgumentException(validation.message());
        }
        String provider = WitcheryConfig.LLM_PROVIDER.get().trim().toLowerCase(Locale.ROOT);
        if (provider.equals("openai_compatible") || provider.equals("openai-compatible")) {
            return new OpenAiCompatibleLlmProvider(config, requestExecutor);
        }
        if (!provider.equals("ollama")) {
            throw new IllegalArgumentException("Unsupported LLM provider: " + WitcheryConfig.LLM_PROVIDER.get());
        }
        return new OllamaLlmProvider(config, requestExecutor);
    }

    private ExecutorService executor() {
        synchronized (executorLock) {
            if (executor == null || executor.isShutdown()) {
                executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, "Witchery NPC LLM");
                        thread.setDaemon(true);
                        return thread;
                    }
                });
            }
            return executor;
        }
    }

    private ExecutorService warmupExecutor() {
        synchronized (warmupExecutorLock) {
            if (warmupExecutor == null || warmupExecutor.isShutdown()) {
                warmupExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, "Witchery NPC LLM Warmup");
                        thread.setDaemon(true);
                        return thread;
                    }
                });
            }
            return warmupExecutor;
        }
    }

    private static String warmupConfigKey() {
        return WitcheryConfig.LLM_PROVIDER.get().trim().toLowerCase(Locale.ROOT)
                + "|" + WitcheryConfig.LLM_ENDPOINT.get().trim()
                + "|" + WitcheryConfig.LLM_MODEL.get().trim()
                + "|" + WitcheryConfig.LLM_KEEP_ALIVE_MINUTES.get();
    }

    private void completeWarmup(String key, long elapsedMillis, Throwable error) {
        synchronized (warmupLock) {
            warmupInFlight = false;
            lastWarmupMillis = elapsedMillis;
            if (error == null) {
                warmedConfigKey = key;
                lastWarmupError = "";
                warmups.incrementAndGet();
            } else {
                lastWarmupError = shortError(error);
            }
        }
    }

    private void failure(Throwable error) {
        failures.incrementAndGet();
        lastError = shortError(error);
    }

    private static String shortError(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return cause.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
