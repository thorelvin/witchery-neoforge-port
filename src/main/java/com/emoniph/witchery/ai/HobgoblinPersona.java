package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.HobgoblinEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class HobgoblinPersona implements NpcPersona<HobgoblinEntity> {
    public static final HobgoblinPersona INSTANCE = new HobgoblinPersona();
    private static final long SOCIAL_COOLDOWN_TICKS = 3L * 60L * 20L;
    private static final int PRANK_TICKS = 30 * 20;
    private static final String FLAG_PRANKED = "hobgoblin_pranked";

    private HobgoblinPersona() {
    }

    @Override
    public String id() {
        return "hobgoblin";
    }

    @Override
    public String roleName() {
        return "hobgoblin";
    }

    @Override
    public String defaultName() {
        return "Hobgoblin";
    }

    @Override
    public Class<HobgoblinEntity> entityClass() {
        return HobgoblinEntity.class;
    }

    @Override
    public int chatColorRgb() {
        return 0x88AA44;
    }

    @Override
    public void beginConversation(HobgoblinEntity entity, ServerPlayer player) {
        entity.beginPlayerConversation(player);
    }

    @Override
    public void completeConversation(HobgoblinEntity entity, ServerPlayer player) {
        entity.completePlayerConversation(player);
    }

    @Override
    public void abortConversation(HobgoblinEntity entity, ServerPlayer player) {
        entity.abortPlayerConversation();
    }

    @Override
    public void nudgeAttention(HobgoblinEntity entity, ServerPlayer player) {
        entity.nudgePlayerAttention(player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(HobgoblinEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.hobgoblinGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(HobgoblinEntity entity, ServerPlayer player, String memory,
                                     String playerMessage) {
        return NpcPromptBuilder.hobgoblinChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, HobgoblinEntity entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, OPEN_TRADE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case FOLLOW_OWNER -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, HobgoblinEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.hobgoblinChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                NpcPromptBuilder.hobgoblinWorkInfo(entity),
                NpcPromptBuilder.hobgoblinPatronInfo(entity),
                memoryContext);
    }

    @Override
    public Optional<NpcSocialAction> socialAction(String playerMessage) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        if (NpcDialogueFallback.isInsult(normalized)) {
            return Optional.of(NpcSocialAction.INSULT);
        }
        if (NpcDialogueFallback.isPraise(normalized)) {
            return Optional.of(NpcSocialAction.PRAISE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<NpcSocialAction> ambiguousSocialAction(String playerMessage) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        if (NpcDialogueFallback.isAmbiguousInsultCandidate(normalized)) {
            return Optional.of(NpcSocialAction.INSULT);
        }
        return Optional.empty();
    }

    @Override
    public boolean handleSocialAction(HobgoblinEntity entity, ServerPlayer player, String playerMessage,
                                      NpcSocialAction action, NpcSocialContext<HobgoblinEntity> context) {
        switch (action) {
            case INSULT -> handleInsult(entity, player, context);
            case PRAISE -> handlePraise(player, context);
            case HERB_GIFT -> context.say("*shrugs* \"No herbs. Hobgoblin has pockets, not gardens.\"");
        }
        return true;
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, HobgoblinEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        Optional<String> polished = NpcBrainResponse.polishSpeechAfterFallback(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
        if (polished.isEmpty() || rejectsHobgoblinRaw(polished.get(), playerMessage)) {
            return Optional.empty();
        }
        return polished;
    }

    @Override
    public Optional<String> polishFallback(String speech, HobgoblinEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(speech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        return cleaned.isBlank() ? Optional.empty() : Optional.of(cleaned);
    }

    @Override
    public String lastResortFallback(HobgoblinEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.hobgoblinLastResort(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(HobgoblinEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.hobgoblinAlternate(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public String relationshipNotes(String playerName, List<String> flags) {
        if (flags == null || flags.isEmpty()) {
            return "";
        }
        String name = playerName == null || playerName.isBlank() ? "The player" : playerName;
        List<String> notes = new ArrayList<>();
        for (String flag : flags) {
            if (FLAG_PRANKED.equals(normalizeFlag(flag))) {
                notes.add(name + " was pranked by this hobgoblin after an insult.");
            }
        }
        return NpcBrainResponse.sanitize(String.join(" ", notes), 180);
    }

    static boolean rejectsHobgoblinRaw(String speech, String playerMessage) {
        String normalized = normalize(speech);
        String player = normalize(playerMessage);
        if (normalized.isBlank()) {
            return true;
        }
        if (containsAny(normalized, "prompt", "json", "schema", "as an ai", "language model",
                "targeted fact", "npc dialogue brain", "server state")) {
            return true;
        }
        if (containsAny(normalized, "i am a witch", "i m a witch", "coven witch",
                "shade of leonard", "i am leonard", "baba yaga", "apothecary",
                "villager here", "i am a villager", "good witch")) {
            return true;
        }
        if (containsAny(normalized, "my coven", "my cauldron", "i brew", "i can brew",
                "i sell fumes", "witches salt from me", "teach circle", "teach rite")) {
            return true;
        }
        if (containsAny(normalized, "my dear", "dear friend", "traveler friend",
                "how are you today", "what brings you here")) {
            return true;
        }
        if (normalized.split("\\s+").length > 34) {
            return true;
        }
        if (containsAny(player, "koboldite", "dust", "nugget", "ingot")
                && !containsAny(normalized, "koboldite", "dust", "nugget", "ingot", "shiny", "mine", "dig")) {
            return true;
        }
        if (containsAny(player, "mog", "gulg", "patron", "statue", "worship", "god", "gods")
                && !containsAny(normalized, "mog", "gulg", "patron", "statue", "pray", "god", "angry")) {
            return true;
        }
        if (containsAny(player, "trade", "sell", "buy", "stock", "wares", "deal")
                && !containsAny(normalized, "trade", "deal", "coin", "coal", "gold", "koboldite", "shiny")) {
            return true;
        }
        return false;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void handleInsult(HobgoblinEntity hobgoblin, ServerPlayer player,
                                     NpcSocialContext<HobgoblinEntity> context) {
        if (context.markCooldown("prank", SOCIAL_COOLDOWN_TICKS)) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PRANK_TICKS, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 10 * 20, 0));
            context.memoryData().setFlag(hobgoblin.getUUID(), player.getUUID(), FLAG_PRANKED,
                    context.level().getGameTime());
            context.playFeedback(false);
            context.say("*taps a finger* \"Bad mouth gets tied bootlaces.\"");
            return;
        }
        context.say("*shrugs* \"Already pranked. Hobgoblin saves tricks.\"");
    }

    private static void handlePraise(ServerPlayer player, NpcSocialContext<HobgoblinEntity> context) {
        context.playFeedback(true);
        context.say("*studies your coin* \"Pretty words, " + player.getName().getString()
                + ". Coin prettier.\"");
    }

    private static String normalizeFlag(String flag) {
        return NpcBrainResponse.sanitize(flag, 48)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.:-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }
}
