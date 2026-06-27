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
    private static final String FLAG_HERB_GIFT = "coven_witch_herb_gift";

    private final Object executorLock = new Object();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final ConcurrentHashMap<String, Long> cooldownUntil = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> socialCooldownUntil = new ConcurrentHashMap<>();
    private volatile ExecutorService executor;
    private volatile String lastError = "";

    private NpcBrainService() {
    }

    public boolean requestCovenGreeting(CovenWitchEntity witch, ServerPlayer player, Component fallback) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        return requestCovenResponse(witch, player,
                NpcPromptBuilder.covenWitchGreeting(witch, player, contextFor(witch, player)),
                "greet", "", fallback, true);
    }

    public boolean requestCovenChat(CovenWitchEntity witch, ServerPlayer player, String playerMessage) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        if (tryHandleCovenSocialInteraction(witch, player, playerMessage)) {
            return true;
        }
        return requestCovenResponse(witch, player,
                NpcPromptBuilder.covenWitchChat(witch, player, contextFor(witch, player), playerMessage),
                "chat", playerMessage, null, false);
    }

    private boolean requestCovenResponse(CovenWitchEntity witch, ServerPlayer player, NpcBrainPrompt prompt,
                                         String trigger, String playerMessage, Component fallback, boolean showFallback) {
        if (!WitcheryConfig.LLM_ENABLED.get()) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        String cooldownKey = witch.getUUID() + ":" + player.getUUID() + ":" + trigger;
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

        witch.beginPlayerConversation(player);
        provider.generate(prompt)
                .orTimeout(WitcheryConfig.LLM_TIMEOUT_MILLIS.get() + 500L, TimeUnit.MILLISECONDS)
                .whenComplete((response, error) -> {
                    inFlight.decrementAndGet();
                    MinecraftServer server = level.getServer();
                    server.execute(() -> handleCovenResponse(level, witch.getUUID(), player, witch,
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
                + ", ok=" + successes.get()
                + ", failed=" + failures.get()
                + ", rejected=" + rejected.get()
                + (lastError.isBlank() ? "" : ", lastError=" + lastError);
    }

    public void reload() {
        cooldownUntil.clear();
        socialCooldownUntil.clear();
        lastError = "";
    }

    public void shutdown() {
        synchronized (executorLock) {
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }
        inFlight.set(0);
    }

    public void rememberCovenExchange(CovenWitchEntity witch, ServerPlayer player, String playerSpeech, String npcSpeech) {
        if (witch == null || player == null || player.level() != witch.level()) {
            return;
        }
        String witchName = witch.hasCustomName() ? witch.getCustomName().getString() : "Witch";
        NpcMemoryData.get((ServerLevel) player.level()).rememberExchange(witch.getUUID(), player.getUUID(),
                witchName, player.getName().getString(), playerSpeech, npcSpeech,
                ((ServerLevel) player.level()).getGameTime());
    }

    private String contextFor(CovenWitchEntity witch, ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        return NpcMemoryData.get(level).getContext(witch.getUUID(), player.getUUID());
    }

    private static long cooldownTicks(String trigger) {
        long configured = WitcheryConfig.LLM_COOLDOWN_TICKS.get();
        return "chat".equals(trigger) ? Math.min(configured, CHAT_COOLDOWN_TICKS) : configured;
    }

    private void handleCovenResponse(ServerLevel level, UUID witchId, ServerPlayer player, CovenWitchEntity witch,
                                     String playerMessage, Component fallback, boolean showFallback,
                                     NpcBrainResponse response, Throwable error) {
        if (!player.isAlive() || player.level() != level) {
            return;
        }
        if (witch.isAlive() && witch.level() == level) {
            witch.completePlayerConversation(player);
        }
        if (error != null || response == null) {
            failure(error);
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayCovenFallback(level, witchId, player, witch, playerMessage);
            }
            return;
        }
        if (!response.intent().allowedForCovenWitch(witch.isRecruitedBy(player))) {
            rejected.incrementAndGet();
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayCovenFallback(level, witchId, player, witch, playerMessage);
            }
            return;
        }
        String witchName = witch.hasCustomName() ? witch.getCustomName().getString() : "Witch";
        String playerName = player.getName().getString();
        NpcMemoryData memoryData = NpcMemoryData.get(level);
        String memoryContext = memoryData.getContext(witchId, player.getUUID());
        var polishedSpeech = NpcBrainResponse.polishSpeech(response.speech(), witchName, playerName,
                playerMessage, memoryContext, NpcPromptBuilder.clockTime(level.getDayTime()));
        if (polishedSpeech.isEmpty()) {
            Optional<String> relaxedSpeech = rawAfterRepeatedFallback(level, witchId, player, witch,
                    playerMessage, response.speech(), witchName, playerName, memoryContext);
            if (relaxedSpeech.isPresent()) {
                displayCovenSpeech(level, witch, player, playerMessage, relaxedSpeech.get());
                if (!response.memory().isBlank()) {
                    NpcMemoryData.get(level).remember(witchId, player.getUUID(), response.memory(), level.getGameTime());
                }
                return;
            }
            rejected.incrementAndGet();
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayCovenFallback(level, witchId, player, witch, playerMessage);
            }
            return;
        }
        String lastNpcSpeech = memoryData.getLastNpcSpeech(witchId, player.getUUID());
        if (NpcBrainResponse.sameMeaningForRepetition(polishedSpeech.get(), lastNpcSpeech)
                || memoryData.repeatsRecentPlayerSpeech(witchId, player.getUUID(), polishedSpeech.get())) {
            rejected.incrementAndGet();
            if (showFallback && fallback != null) {
                player.displayClientMessage(fallback, false);
            } else if (!showFallback) {
                tryDisplayCovenFallback(level, witchId, player, witch, playerMessage);
            }
            return;
        }
        Optional<String> supportFollowup = NpcBrainResponse.supportFollowup(playerMessage, polishedSpeech.get());
        displayCovenSpeech(level, witch, player, playerMessage, polishedSpeech.get(), supportFollowup);
        if (!response.memory().isBlank()) {
            NpcMemoryData.get(level).remember(witchId, player.getUUID(), response.memory(), level.getGameTime());
        }
    }

    private boolean tryDisplayCovenFallback(ServerLevel level, UUID witchId, ServerPlayer player,
                                            CovenWitchEntity witch, String playerMessage) {
        if (playerMessage == null || playerMessage.isBlank()) {
            return false;
        }
        String witchName = witch.hasCustomName() ? witch.getCustomName().getString() : "Witch";
        String playerName = player.getName().getString();
        NpcMemoryData memoryData = NpcMemoryData.get(level);
        String memoryContext = memoryData.getContext(witchId, player.getUUID());
        var fallback = NpcDialogueFallback.covenChat(playerMessage, witchName, playerName,
                        NpcPromptBuilder.biomeName(level, witch.blockPosition()),
                        NpcPromptBuilder.timeName(level.getDayTime()),
                        NpcPromptBuilder.clockTime(level.getDayTime()),
                        level.getDayTime() / 24000L,
                        memoryContext)
                .flatMap(speech -> NpcBrainResponse.polishSpeech(speech, witchName, playerName,
                        playerMessage, memoryContext, NpcPromptBuilder.clockTime(level.getDayTime())));
        String fallbackSpeech = fallback.orElseGet(() -> NpcDialogueFallback.lastResortAside(playerMessage, playerName));
        String lastNpcSpeech = memoryData.getLastNpcSpeech(witchId, player.getUUID());
        if (NpcBrainResponse.sameMeaningForRepetition(fallbackSpeech, lastNpcSpeech)
                || memoryData.repeatsRecentPlayerSpeech(witchId, player.getUUID(), fallbackSpeech)) {
            Optional<String> alternate = NpcDialogueFallback.alternateForRepeatedFallback(
                    playerMessage, playerName, lastNpcSpeech);
            if (alternate.isPresent()) {
                fallbackSpeech = alternate.get();
            }
        }
        successes.incrementAndGet();
        player.displayClientMessage(Component.literal("\u00a79<" + witchName + "> \u00a7r" + fallbackSpeech), false);
        rememberCovenExchange(witch, player, playerMessage, fallbackSpeech);
        return true;
    }

    private Optional<String> rawAfterRepeatedFallback(ServerLevel level, UUID witchId, ServerPlayer player,
                                                      CovenWitchEntity witch, String playerMessage,
                                                      String rawSpeech, String witchName, String playerName,
                                                      String memoryContext) {
        if (playerMessage == null || playerMessage.isBlank()) {
            return Optional.empty();
        }
        Optional<String> fallback = covenFallback(level, witch, playerMessage, witchName, playerName, memoryContext);
        if (fallback.isEmpty()) {
            return Optional.empty();
        }
        NpcMemoryData memoryData = NpcMemoryData.get(level);
        if (!memoryData.hasRecentNpcAnswerForPlayerSpeech(witchId, player.getUUID(), playerMessage, fallback.get())) {
            return Optional.empty();
        }
        Optional<String> relaxed = NpcBrainResponse.polishSpeechAfterFallback(rawSpeech, witchName, playerName,
                playerMessage, memoryContext, NpcPromptBuilder.clockTime(level.getDayTime()));
        if (relaxed.isEmpty()) {
            return Optional.empty();
        }
        String lastNpcSpeech = memoryData.getLastNpcSpeech(witchId, player.getUUID());
        if (NpcBrainResponse.sameMeaningForRepetition(relaxed.get(), lastNpcSpeech)
                || memoryData.repeatsRecentPlayerSpeech(witchId, player.getUUID(), relaxed.get())) {
            return Optional.empty();
        }
        return relaxed;
    }

    private Optional<String> covenFallback(ServerLevel level, CovenWitchEntity witch, String playerMessage,
                                           String witchName, String playerName, String memoryContext) {
        return NpcDialogueFallback.covenChat(playerMessage, witchName, playerName,
                        NpcPromptBuilder.biomeName(level, witch.blockPosition()),
                        NpcPromptBuilder.timeName(level.getDayTime()),
                        NpcPromptBuilder.clockTime(level.getDayTime()),
                        level.getDayTime() / 24000L,
                        memoryContext)
                .flatMap(speech -> NpcBrainResponse.polishSpeech(speech, witchName, playerName,
                        playerMessage, memoryContext, NpcPromptBuilder.clockTime(level.getDayTime())));
    }

    private void displayCovenSpeech(ServerLevel level, CovenWitchEntity witch, ServerPlayer player,
                                    String playerMessage, String speech) {
        displayCovenSpeech(level, witch, player, playerMessage, speech,
                NpcBrainResponse.supportFollowup(playerMessage, speech));
    }

    private void displayCovenSpeech(ServerLevel level, CovenWitchEntity witch, ServerPlayer player,
                                    String playerMessage, String speech, Optional<String> supportFollowup) {
        String witchName = witch.hasCustomName() ? witch.getCustomName().getString() : "Witch";
        successes.incrementAndGet();
        player.displayClientMessage(Component.literal("\u00a79<" + witchName + "> \u00a7r" + speech), false);
        supportFollowup.ifPresent(followup ->
                player.displayClientMessage(Component.literal("\u00a79<" + witchName + "> \u00a7r" + followup), false));
        String rememberedSpeech = supportFollowup.map(followup -> speech + " " + followup).orElse(speech);
        rememberCovenExchange(witch, player, playerMessage, rememberedSpeech);
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
        LlmProviderConfig config = new LlmProviderConfig(
                WitcheryConfig.LLM_ENDPOINT.get(),
                WitcheryConfig.LLM_MODEL.get(),
                WitcheryConfig.LLM_TIMEOUT_MILLIS.get(),
                WitcheryConfig.LLM_MAX_TOKENS.get(),
                WitcheryConfig.LLM_ALLOW_LAN_ENDPOINT.get());
        LlmEndpointValidator.Validation validation = LlmEndpointValidator.validate(
                config.endpoint(), config.allowLanEndpoint());
        if (!validation.allowed()) {
            throw new IllegalArgumentException(validation.message());
        }
        String provider = WitcheryConfig.LLM_PROVIDER.get().trim().toLowerCase(Locale.ROOT);
        if (provider.equals("openai_compatible") || provider.equals("openai-compatible")) {
            return new OpenAiCompatibleLlmProvider(config, executor());
        }
        if (!provider.equals("ollama")) {
            throw new IllegalArgumentException("Unsupported LLM provider: " + WitcheryConfig.LLM_PROVIDER.get());
        }
        return new OllamaLlmProvider(config, executor());
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
