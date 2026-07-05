package com.emoniph.witchery.ai;

import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class CovenWitchPersona implements NpcPersona<CovenWitchEntity> {
    public static final CovenWitchPersona INSTANCE = new CovenWitchPersona();
    private static final long SOCIAL_EFFECT_TICKS = 20L * 60L * 5L;
    private static final long SOCIAL_COOLDOWN_TICKS = 20L * 60L * 5L;
    private static final String FLAG_HERB_GIFT = "coven_witch_herb_gift";
    private static final String FLAG_BLESSED = "coven_witch_blessed";
    private static final String FLAG_CURSED = "coven_witch_cursed";

    private CovenWitchPersona() {
    }

    @Override
    public String id() {
        return "coven_witch";
    }

    @Override
    public String roleName() {
        return "witch";
    }

    @Override
    public String defaultName() {
        return "Witch";
    }

    @Override
    public Class<CovenWitchEntity> entityClass() {
        return CovenWitchEntity.class;
    }

    @Override
    public int chatColorRgb() {
        return 0xFF55FF;
    }

    @Override
    public void beginConversation(CovenWitchEntity entity, ServerPlayer player) {
        entity.beginPlayerConversation(player);
    }

    @Override
    public void completeConversation(CovenWitchEntity entity, ServerPlayer player) {
        entity.completePlayerConversation(player);
    }

    @Override
    public void abortConversation(CovenWitchEntity entity, ServerPlayer player) {
        entity.abortPlayerConversation();
    }

    @Override
    public void nudgeAttention(CovenWitchEntity entity, ServerPlayer player) {
        entity.nudgePlayerAttention(player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(CovenWitchEntity entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.covenWitchGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(CovenWitchEntity entity, ServerPlayer player, String memory,
                                    String playerMessage) {
        return NpcPromptBuilder.covenWitchChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, CovenWitchEntity entity, ServerPlayer player) {
        return allowsIntent(intent, entity.isRecruitedBy(player));
    }

    static boolean allowsIntent(NpcIntent intent, boolean recruitedByPlayer) {
        return switch (intent) {
            case NONE, OPEN_TRADE, QUEST_HINT, WARN_THREAT, COMBAT_TAUNT -> true;
            case FOLLOW_OWNER -> recruitedByPlayer;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, CovenWitchEntity entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.covenChat(playerMessage, displayName(entity), player.getName().getString(),
                NpcPromptBuilder.biomeName(level, entity.blockPosition()),
                NpcPromptBuilder.timeName(level.getDayTime()),
                NpcPromptBuilder.clockTime(level.getDayTime()),
                level.getDayTime() / 24000L,
                memoryContext);
    }

    @Override
    public Optional<NpcSocialAction> socialAction(String playerMessage) {
        String normalized = normalizeSocial(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        if (NpcDialogueFallback.asksHerbGift(normalized)) {
            return Optional.of(NpcSocialAction.HERB_GIFT);
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
        String normalized = normalizeSocial(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        if (NpcDialogueFallback.isAmbiguousInsultCandidate(normalized)) {
            return Optional.of(NpcSocialAction.INSULT);
        }
        return Optional.empty();
    }

    @Override
    public boolean handleSocialAction(CovenWitchEntity entity, ServerPlayer player, String playerMessage,
                                      NpcSocialAction action, NpcSocialContext<CovenWitchEntity> context) {
        switch (action) {
            case HERB_GIFT -> handleHerbGift(entity, player, context);
            case INSULT -> handleInsult(entity, player, context);
            case PRAISE -> handlePraise(entity, player, context);
        }
        return true;
    }

    @Override
    public Optional<String> supportFollowup(String playerMessage, String speech) {
        return NpcBrainResponse.supportFollowup(playerMessage, speech);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, CovenWitchEntity entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(rawSpeech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
        if (polished.isEmpty() || rejectsCovenRaw(polished.get(), playerMessage)) {
            return Optional.empty();
        }
        return polished;
    }

    @Override
    public Optional<String> polishFallback(String speech, CovenWitchEntity entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        return NpcBrainResponse.polishSpeechAfterFallback(speech, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock);
    }

    @Override
    public String relationshipNotes(String playerName, List<String> flags) {
        if (flags == null || flags.isEmpty()) {
            return "";
        }
        String name = playerName == null || playerName.isBlank() ? "The player" : playerName;
        List<String> notes = new ArrayList<>();
        for (String flag : flags) {
            switch (normalizeFlag(flag)) {
                case "coven_witch_herb_gift" ->
                        notes.add(name + " received one herb gift from this witch.");
                case "coven_witch_blessed" ->
                        notes.add(name + " was blessed with luck after praising this witch.");
                case "coven_witch_cursed" ->
                        notes.add(name + " was cursed with bad luck after insulting this witch.");
                default -> {
                }
            }
        }
        return NpcBrainResponse.sanitize(String.join(" ", notes), 220);
    }

    @Override
    public String lastResortFallback(CovenWitchEntity entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.lastResortAside(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(CovenWitchEntity entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.alternateForRepeatedFallback(playerMessage, player.getName().getString(),
                avoidSpeech);
    }

    static boolean rejectsCovenRaw(String speech, String playerMessage) {
        String normalized = normalizeSocial(speech);
        if (normalized.isBlank()) {
            return true;
        }
        if (NpcPersonaSpeechGuards.rejectsNarrationOnly(speech)
                || NpcPersonaSpeechGuards.rejectsOutOfWorldMeta(speech)
                || NpcPersonaSpeechGuards.rejectsSoftCompanion(speech)
                || NpcPersonaSpeechGuards.rejectsUnsupportedProtection(speech, playerMessage)
                || NpcPersonaSpeechGuards.rejectsPlayerSpeakerInversion(speech)) {
            return true;
        }
        if (containsAny(normalized, "steps around dev", "circling slowly", "we ve got protection",
                "small personal nature", "if your intent were", "what is it",
                "protection of those", "teach and protect", "my protection yet",
                "protecting those who seek", "you have not earned my protection",
                "i see perhaps we can discuss this matter", "lilithpoet",
                "school for learning", "court declares", "what is a shiny gift",
                "what is a demon", "what do i fear", "you shall refer to me as witch",
                "guardian of this place", "guide you to your destiny", "called by my title",
                "prefer to be called by my title", "demon hearts are rare valuable gems",
                "power and prestige", "not common enough", "witch s knowledge is as precious",
                "lilithpoe", "coven of shadows", "do what is asked of me")) {
            return true;
        }
        return normalized.equals("i see") || normalized.equals("it s not easy");
    }

    private static void handlePraise(CovenWitchEntity witch, ServerPlayer player,
                                     NpcSocialContext<CovenWitchEntity> context) {
        if (context.markCooldown("bless", SOCIAL_COOLDOWN_TICKS)) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, (int) SOCIAL_EFFECT_TICKS, 0));
            context.memoryData().setFlag(witch.getUUID(), player.getUUID(), FLAG_BLESSED,
                    context.level().getGameTime());
            context.playFeedback(true);
            context.say("Kind words have roots, " + player.getName().getString()
                    + ". Take a little luck before it spoils.");
            return;
        }
        context.say("Flattery twice in one sitting curdles. Keep the luck you have.");
    }

    private static void handleInsult(CovenWitchEntity witch, ServerPlayer player,
                                     NpcSocialContext<CovenWitchEntity> context) {
        if (context.markCooldown("curse", SOCIAL_COOLDOWN_TICKS)) {
            player.addEffect(new MobEffectInstance(MobEffects.UNLUCK, (int) SOCIAL_EFFECT_TICKS, 0));
            context.memoryData().setFlag(witch.getUUID(), player.getUUID(), FLAG_CURSED,
                    context.level().getGameTime());
            context.playFeedback(false);
            context.say("Tongues have shadows. Wear yours for five minutes, "
                    + player.getName().getString() + ".");
            return;
        }
        context.say("Again? Hush now, before I find a fresher curse.");
    }

    private static void handleHerbGift(CovenWitchEntity witch, ServerPlayer player,
                                       NpcSocialContext<CovenWitchEntity> context) {
        NpcMemoryData memoryData = context.memoryData();
        if (memoryData.hasFlag(witch.getUUID(), player.getUUID(), FLAG_HERB_GIFT)) {
            context.say("Once, " + player.getName().getString() + ". A gift plucked twice becomes a debt.");
            return;
        }
        ItemStack gift = context.level().random.nextBoolean()
                ? new ItemStack(WitcheryItems.BELLADONNA.get())
                : new ItemStack(WitcheryItems.MANDRAKE_ROOT.get());
        String giftName = gift.getHoverName().getString();
        if (!player.addItem(gift.copy())) {
            player.drop(gift.copy(), false);
        }
        memoryData.setFlag(witch.getUUID(), player.getUUID(), FLAG_HERB_GIFT, context.level().getGameTime());
        context.playFeedback(true);
        context.say("One herb, freely given: " + giftName + ". Spend kindness better than coin.");
    }

    private static String normalizeFlag(String flag) {
        return NpcBrainResponse.sanitize(flag, 48)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.:-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
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

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
