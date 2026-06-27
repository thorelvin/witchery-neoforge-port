package com.emoniph.witchery.ai;

import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

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
    private static final long SOCIAL_EFFECT_TICKS = 20L * 60L * 5L;
    private static final long SOCIAL_COOLDOWN_TICKS = 20L * 60L * 5L;
    private static final int WARMUP_MAX_TOKENS = 24;
    private static final String FLAG_HERB_GIFT = "coven_witch_herb_gift";
    private static final String FLAG_BLESSED = "coven_witch_blessed";
    private static final String FLAG_CURSED = "coven_witch_cursed";

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
        if (tryHandleCovenSocialInteraction(witch, player, playerMessage)) {
            return true;
        }
        CovenWitchPersona persona = NpcPersonas.COVEN_WITCH;
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
                persona.displayName(entity), player.getName().getString());
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
        if (entity.isAlive() && entity.level() == level) {
            persona.completeConversation(entity, player);
        }
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
                persona.displayName(entity), player.getName().getString());
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
                persona.displayName(entity), player.getName().getString());
        var fallback = persona.fallback(level, entity, player, playerMessage, memoryContext)
                .flatMap(speech -> persona.polishFallback(speech, entity, player, playerMessage, memoryContext,
                        NpcPromptBuilder.clockTime(level.getDayTime())));
        String playerName = player.getName().getString();
        String fallbackSpeech = fallback.orElseGet(() -> NpcDialogueFallback.lastResortAside(playerMessage, playerName));
        String lastNpcSpeech = memoryData.getLastNpcSpeech(entityId, player.getUUID());
        if (NpcBrainResponse.sameMeaningForRepetition(fallbackSpeech, lastNpcSpeech)
                || memoryData.repeatsRecentPlayerSpeech(entityId, player.getUUID(), fallbackSpeech)) {
            Optional<String> alternate = NpcDialogueFallback.alternateForRepeatedFallback(
                    playerMessage, playerName, lastNpcSpeech);
            if (alternate.isPresent()) {
                fallbackSpeech = alternate.get();
            }
        }
        successes.incrementAndGet();
        player.displayClientMessage(Component.literal("\u00a79<" + persona.displayName(entity) + "> \u00a7r" + fallbackSpeech), false);
        rememberExchange(persona, entity, player, playerMessage, fallbackSpeech);
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

    private void displayCovenSpeech(ServerLevel level, CovenWitchEntity witch, ServerPlayer player,
                                    String playerMessage, String speech) {
        displayPersonaSpeech(level, NpcPersonas.COVEN_WITCH, witch, player, playerMessage, speech);
    }

    private void displayCovenSpeech(ServerLevel level, CovenWitchEntity witch, ServerPlayer player,
                                    String playerMessage, String speech, Optional<String> supportFollowup) {
        displayPersonaSpeech(level, NpcPersonas.COVEN_WITCH, witch, player, playerMessage, speech, supportFollowup);
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
        successes.incrementAndGet();
        player.displayClientMessage(Component.literal("\u00a79<" + npcName + "> \u00a7r" + speech), false);
        supportFollowup.ifPresent(followup ->
                player.displayClientMessage(Component.literal("\u00a79<" + npcName + "> \u00a7r" + followup), false));
        String rememberedSpeech = supportFollowup.map(followup -> speech + " " + followup).orElse(speech);
        rememberExchange(persona, entity, player, playerMessage, rememberedSpeech);
    }

    private boolean tryHandleCovenSocialInteraction(CovenWitchEntity witch, ServerPlayer player, String playerMessage) {
        String normalized = normalizeSocial(playerMessage);
        if (normalized.isBlank()) {
            return false;
        }
        if (NpcDialogueFallback.asksHerbGift(normalized)) {
            handleHerbGift(witch, player, playerMessage);
            return true;
        }
        if (NpcDialogueFallback.isInsult(normalized)) {
            handleInsult(witch, player, playerMessage);
            return true;
        }
        if (NpcDialogueFallback.isPraise(normalized)) {
            handlePraise(witch, player, playerMessage);
            return true;
        }
        return false;
    }

    private void handlePraise(CovenWitchEntity witch, ServerPlayer player, String playerMessage) {
        ServerLevel level = (ServerLevel) player.level();
        if (markSocialCooldown(level, witch, player, "bless", SOCIAL_COOLDOWN_TICKS)) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, (int) SOCIAL_EFFECT_TICKS, 0));
            NpcMemoryData.get(level).setFlag(witch.getUUID(), player.getUUID(), FLAG_BLESSED, level.getGameTime());
            socialFx(level, player, true);
            displayCovenSpeech(level, witch, player, playerMessage,
                    "Kind words have roots, " + player.getName().getString() + ". Take a little luck before it spoils.",
                    Optional.empty());
            return;
        }
        displayCovenSpeech(level, witch, player, playerMessage,
                "Flattery twice in one sitting curdles. Keep the luck you have.",
                Optional.empty());
    }

    private void handleInsult(CovenWitchEntity witch, ServerPlayer player, String playerMessage) {
        ServerLevel level = (ServerLevel) player.level();
        if (markSocialCooldown(level, witch, player, "curse", SOCIAL_COOLDOWN_TICKS)) {
            player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, (int) SOCIAL_EFFECT_TICKS, 0));
            NpcMemoryData.get(level).setFlag(witch.getUUID(), player.getUUID(), FLAG_CURSED, level.getGameTime());
            socialFx(level, player, false);
            displayCovenSpeech(level, witch, player, playerMessage,
                    "Tongues have shadows. Wear yours for five minutes, " + player.getName().getString() + ".",
                    Optional.empty());
            return;
        }
        displayCovenSpeech(level, witch, player, playerMessage,
                "Again? Hush now, before I find a fresher curse.",
                Optional.empty());
    }

    private void handleHerbGift(CovenWitchEntity witch, ServerPlayer player, String playerMessage) {
        ServerLevel level = (ServerLevel) player.level();
        NpcMemoryData memoryData = NpcMemoryData.get(level);
        if (memoryData.hasFlag(witch.getUUID(), player.getUUID(), FLAG_HERB_GIFT)) {
            displayCovenSpeech(level, witch, player, playerMessage,
                    "Once, " + player.getName().getString() + ". A gift plucked twice becomes a debt.",
                    Optional.empty());
            return;
        }
        ItemStack gift = level.random.nextBoolean()
                ? new ItemStack(WitcheryItems.BELLADONNA.get())
                : new ItemStack(WitcheryItems.MANDRAKE_ROOT.get());
        String giftName = gift.getHoverName().getString();
        if (!player.addItem(gift.copy())) {
            player.drop(gift.copy(), false);
        }
        memoryData.setFlag(witch.getUUID(), player.getUUID(), FLAG_HERB_GIFT, level.getGameTime());
        socialFx(level, player, true);
        displayCovenSpeech(level, witch, player, playerMessage,
                "One herb, freely given: " + giftName + ". Spend kindness better than coin.",
                Optional.empty());
    }

    private boolean markSocialCooldown(ServerLevel level, CovenWitchEntity witch, ServerPlayer player,
                                       String action, long ticks) {
        String key = witch.getUUID() + ":" + player.getUUID() + ":" + action;
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

    private static String normalizeSocial(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9' ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
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
