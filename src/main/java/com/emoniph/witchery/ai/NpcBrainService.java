package com.emoniph.witchery.ai;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.entity.CovenWitchEntity;
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
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        CovenWitchPersona persona = NpcPersonas.COVEN_WITCH;
        if (tryHandleSocialInteraction(persona, witch, player, playerMessage)) {
            return true;
        }
        return requestPersonaResponse(persona, witch, player,
                persona.chatPrompt(witch, player, contextFor(persona, witch, player), playerMessage),
                "chat", playerMessage, null, false);
    }

    private <E extends LivingEntity> boolean requestPersonaResponse(NpcPersona<E> persona, E entity,
                                                                    ServerPlayer player, NpcBrainPrompt prompt,
                                                                    String trigger, String playerMessage,
                                                                    Component fallback, boolean showFallback) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        if (!entity.isAlive() || entity.level() != player.level()) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        String cooldownKey = entity.getUUID() + ":" + player.getUUID() + ":" + trigger;
        Long blockedUntil = cooldownUntil.get(cooldownKey);
        if (blockedUntil != null && blockedUntil > now) {
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
                            playerMessage, fallback, showFallback, response, error));
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

    private <E extends LivingEntity> void handlePersonaResponse(ServerLevel level, NpcPersona<E> persona, UUID entityId,
                                                                ServerPlayer player, E entity, String playerMessage,
                                                                Component fallback, boolean showFallback,
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
                tryDisplayPersonaFallback(level, persona, entityId, player, entity, playerMessage);
            }
            return;
        }
        if (!persona.allowsIntent(response.intent(), entity, player)) {
            rejected.incrementAndGet();
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayPersonaFallback(level, persona, entityId, player, entity, playerMessage);
            }
            return;
        }
        NpcMemoryData memoryData = NpcMemoryData.get(level);
        String memoryContext = memoryData.getContext(entityId, player.getUUID(), persona.roleName(),
                persona.displayName(entity), player.getName().getString(), persona::relationshipNotes);
        var polishedSpeech = persona.polishRaw(response.speech(), entity, player, playerMessage, memoryContext,
                NpcPromptBuilder.clockTime(level.getDayTime()));
        if (polishedSpeech.isEmpty()) {
            Optional<String> relaxedSpeech = rawAfterRepeatedFallback(level, persona, entityId, player, entity,
                    playerMessage, response.speech(), memoryContext);
            if (relaxedSpeech.isPresent()) {
                displayPersonaSpeech(level, persona, entity, player, playerMessage, relaxedSpeech.get());
                if (!response.memory().isBlank()) {
                    NpcMemoryData.get(level).remember(entityId, player.getUUID(), response.memory(), level.getGameTime());
                }
                return;
            }
            rejected.incrementAndGet();
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayPersonaFallback(level, persona, entityId, player, entity, playerMessage);
            }
            return;
        }
        String lastNpcSpeech = memoryData.getLastNpcSpeech(entityId, player.getUUID());
        if (NpcBrainResponse.sameMeaningForRepetition(polishedSpeech.get(), lastNpcSpeech)
                || memoryData.repeatsRecentPlayerSpeech(entityId, player.getUUID(), polishedSpeech.get())) {
            rejected.incrementAndGet();
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayPersonaFallback(level, persona, entityId, player, entity, playerMessage);
            }
            return;
        }
        Optional<String> supportFollowup = persona.supportFollowup(playerMessage, polishedSpeech.get());
        displayPersonaSpeech(level, persona, entity, player, playerMessage, polishedSpeech.get(), supportFollowup);
        if (!response.memory().isBlank()) {
            NpcMemoryData.get(level).remember(entityId, player.getUUID(), response.memory(), level.getGameTime());
        }
    }

    private <E extends LivingEntity> boolean tryDisplayPersonaFallback(ServerLevel level, NpcPersona<E> persona,
                                                                       UUID entityId, ServerPlayer player, E entity,
                                                                       String playerMessage) {
        if (playerMessage == null || playerMessage.isBlank()) {
            return false;
        }
        NpcMemoryData memoryData = NpcMemoryData.get(level);
        String memoryContext = memoryData.getContext(entityId, player.getUUID(), persona.roleName(),
                persona.displayName(entity), player.getName().getString(), persona::relationshipNotes);
        var fallback = persona.fallback(level, entity, player, playerMessage, memoryContext)
                .flatMap(speech -> persona.polishFallback(speech, entity, player, playerMessage, memoryContext,
                        NpcPromptBuilder.clockTime(level.getDayTime())));
        String lastNpcSpeech = memoryData.getLastNpcSpeech(entityId, player.getUUID());
        String fallbackSpeech = fallback.orElseGet(() ->
                persona.lastResortFallback(entity, player, playerMessage, lastNpcSpeech));
        if (NpcBrainResponse.sameMeaningForRepetition(fallbackSpeech, lastNpcSpeech)
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
        displayPersonaSpeech(level, persona, entity, player, playerMessage, speech,
                persona.supportFollowup(playerMessage, speech));
    }

    private <E extends LivingEntity> void displayPersonaSpeech(ServerLevel level, NpcPersona<E> persona, E entity,
                                                               ServerPlayer player, String playerMessage,
                                                               String speech, Optional<String> supportFollowup) {
        String npcName = persona.displayName(entity);
        persona.nudgeAttention(entity, player);
        playVisibleBeat(level, entity, speech);
        successes.incrementAndGet();
        player.displayClientMessage(Component.literal("\u00a79<" + npcName + "> \u00a7r" + speech), false);
        supportFollowup.ifPresent(followup ->
                player.displayClientMessage(Component.literal("\u00a79<" + npcName + "> \u00a7r" + followup), false));
        String rememberedSpeech = supportFollowup.map(followup -> speech + " " + followup).orElse(speech);
        rememberExchange(persona, entity, player, playerMessage, rememberedSpeech);
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
        SoundEvent sound = soundForBeat(cue);
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

    private static SoundEvent soundForBeat(String beat) {
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
