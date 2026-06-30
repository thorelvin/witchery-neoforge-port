package com.emoniph.witchery.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Locale;
import java.util.Optional;

public final class VillagerPersona implements NpcPersona<Villager> {
    public static final VillagerPersona INSTANCE = new VillagerPersona();

    private VillagerPersona() {
    }

    @Override
    public String id() {
        return "villager";
    }

    @Override
    public String roleName() {
        return "villager";
    }

    @Override
    public String defaultName() {
        return "Villager";
    }

    @Override
    public Class<Villager> entityClass() {
        return Villager.class;
    }

    @Override
    public int chatColorRgb() {
        return 0xFFAA00;
    }

    @Override
    public void beginConversation(Villager entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void completeConversation(Villager entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public void nudgeAttention(Villager entity, ServerPlayer player) {
        holdAttention(entity, player);
    }

    @Override
    public NpcBrainPrompt greetingPrompt(Villager entity, ServerPlayer player, String memory) {
        return NpcPromptBuilder.villagerGreeting(entity, player, memory);
    }

    @Override
    public NpcBrainPrompt chatPrompt(Villager entity, ServerPlayer player, String memory,
                                     String playerMessage) {
        return NpcPromptBuilder.villagerChat(entity, player, memory, playerMessage);
    }

    @Override
    public boolean allowsIntent(NpcIntent intent, Villager entity, ServerPlayer player) {
        return switch (intent) {
            case NONE, OPEN_TRADE, QUEST_HINT, WARN_THREAT -> true;
            case FOLLOW_OWNER, COMBAT_TAUNT -> false;
        };
    }

    @Override
    public Optional<String> fallback(ServerLevel level, Villager entity, ServerPlayer player,
                                     String playerMessage, String memoryContext) {
        return NpcDialogueFallback.villagerChat(
                playerMessage,
                displayName(entity),
                player.getName().getString(),
                NpcPromptBuilder.villagerProfessionName(entity),
                NpcPromptBuilder.villagerVillageInfo(level, entity),
                memoryContext);
    }

    @Override
    public Optional<String> polishRaw(String rawSpeech, Villager entity, ServerPlayer player,
                                      String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(rawSpeech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        cleaned = NpcBrainResponse.polishSpeechAfterFallback(cleaned, displayName(entity),
                player.getName().getString(), playerMessage, memoryContext, expectedClock).orElse(cleaned);
        if (cleaned.isBlank() || rejectsVillagerRaw(cleaned) || rejectsVillagerRawForMessage(cleaned, playerMessage)) {
            return Optional.empty();
        }
        return Optional.of(cleaned);
    }

    @Override
    public Optional<String> polishFallback(String speech, Villager entity, ServerPlayer player,
                                           String playerMessage, String memoryContext, String expectedClock) {
        String cleaned = NpcBrainResponse.sanitize(speech, NpcBrainResponse.MAX_SPEECH_LENGTH);
        return cleaned.isBlank() ? Optional.empty() : Optional.of(cleaned);
    }

    @Override
    public String lastResortFallback(Villager entity, ServerPlayer player, String playerMessage,
                                     String avoidSpeech) {
        return NpcDialogueFallback.villagerLastResort(playerMessage, player.getName().getString(), avoidSpeech);
    }

    @Override
    public Optional<String> alternateForRepeatedFallback(Villager entity, ServerPlayer player,
                                                         String playerMessage, String avoidSpeech) {
        return NpcDialogueFallback.villagerAlternate(playerMessage, player.getName().getString(), avoidSpeech);
    }

    static boolean rejectsVillagerRaw(String speech) {
        String normalized = speech == null ? "" : speech.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized.isBlank()
                || normalized.contains("prompt")
                || normalized.contains("json")
                || normalized.contains("schema")
                || normalized.contains("as an ai")
                || normalized.contains("language model")
                || normalized.contains(" card ")
                || normalized.endsWith(" card")
                || normalized.contains("i am a witch")
                || normalized.contains("i m a witch")
                || normalized.contains("i m a good witch")
                || normalized.contains("i am a good witch")
                || normalized.contains("i am the shade")
                || normalized.contains("shade of leonard")
                || normalized.contains("coven witch")
                || normalized.contains("i brew")
                || normalized.contains("i can brew")
                || normalized.contains("i brewed")
                || normalized.contains("i sell fumes")
                || normalized.contains("my fumes")
                || normalized.contains("i make mutandis")
                || normalized.contains("i can make mutandis")
                || normalized.contains("i make whiff")
                || normalized.contains("i can make whiff")
                || normalized.contains("player coven")
                || normalized.contains("player s coven")
                || normalized.contains("players coven")
                || normalized.contains("targeted fact")
                || normalized.contains("server state says")
                || normalized.contains("my taglocks say")
                || normalized.contains("i ll give you my whiff")
                || normalized.contains("i will give you my whiff")
                || normalized.contains("my whiff of magic recipe")
                || normalized.contains("i m the best at it")
                || normalized.contains("i am the best at it")
                || normalized.contains("happy to show you what i know about witchcraft")
                || normalized.contains("good witch voice")
                || normalized.contains("i ll ring the bell")
                || normalized.contains("i will ring the bell")
                || normalized.contains("you can hide behind me")
                || normalized.contains("hide behind me");
    }

    static boolean rejectsVillagerRawForMessage(String speech, String playerMessage) {
        String answer = normalizeForCheck(speech);
        String message = normalizeForCheck(playerMessage);
        if (answer.isBlank()) {
            return true;
        }
        if (containsAny(answer, "i m a good witch", "i am a good witch", "i am a witch", "i m a witch",
                "i brew", "i can brew", "i brewed", "i sell fumes", "my fumes",
                "i make mutandis", "i can make mutandis", "i make whiff", "i can make whiff")) {
            return true;
        }
        if (containsAny(answer, "apothecary card", "targeted fact", "player s coven", "players coven",
                "calls out from her window")) {
            return true;
        }
        if (containsAny(answer, "i ll ring the bell", "i will ring the bell", "hide behind me",
                "you can hide behind me", "i ll give you my whiff", "i will give you my whiff",
                "my whiff of magic recipe", "my taglock", "my taglocks", "100 gold",
                "good witch voice", "hercules", "câ", "c�")
                || answer.equals("apothecary")
                || answer.equals("apothecary marta")
                || answer.startsWith("apothecary ")) {
            return true;
        }
        if (containsAny(message, "how big", "village size", "how many villagers", "population", "beds", "doors")
                && (!containsAny(answer, "villager", "villagers", "bed", "beds", "door", "doors", "eight", "ten", "seven")
                    || containsAny(answer, "apothecary", "sell what keeps", "herbs and potions"))) {
            return true;
        }
        if (containsAny(message, "guard", "guards")
                && !containsAny(answer, "guard", "guards", "bell")) {
            return true;
        }
        if (containsAny(message, "raid", "raider", "raiders", "pillager", "pillagers")
                && (!containsAny(answer, "raid", "no raid", "raider", "raiders", "pillager", "pillagers")
                    || containsAny(answer, "yes there is a raid", "there is a raid", "vampire raid"))) {
            return true;
        }
        if (containsAny(message, "vampire", "vampires")
                && (answer.contains("not sure what you mean")
                    || answer.contains("few of them")
                    || answer.contains("not yet ready to drink blood")
                    || answer.contains("golem is nearby")
                    || !containsAny(answer, "vampire", "vampires", "garlic", "sunset"))) {
            return true;
        }
        if (containsAny(message, "werewolf", "werewolves", "wolfman", "wolfmen")
                && (!containsAny(answer, "werewolf", "werewolves", "moon", "silver")
                    || containsAny(answer, "get my hands on some of that blood", "need blood to tell you"))) {
            return true;
        }
        if (containsAny(message, "witch", "witches", "witchcraft")
                && containsAny(answer, "i don t know any", "i dont know any", "no specific witches",
                "happy to show you", "best at it", "i could teach you", "i can teach you")) {
            return true;
        }
        if (containsAny(message, "witch", "witches", "coven", "curse", "hair", "taglock", "poppet")
                && containsAny(answer, "i am an apothecary", "i am the apothecary",
                "jars for fumes", "herbs for coughs", "buy herbs and sell", "garlic jar",
                "bat wool", "sleepy brew", "mutandis to keep your hair")) {
            return true;
        }
        if (containsAny(message, "primer", "read a primer")
                && !containsAny(answer, "primer", "witch", "witches", "apothecary", "leave it", "read")) {
            return true;
        }
        if (containsAny(message, "whiff", "whif", "mutandis")
                && containsAny(answer, "i ll give you", "i will give you", "my recipe", "my whiff",
                "buying a bottle", "buying from you", "from you")) {
            return true;
        }
        if (containsAny(message, "what did i say", "what do you remember", "what did i say i fear",
                "what do i fear", "what am i afraid of")
                && (!containsAny(answer, "remember", "told me", "said", "fear", "afraid", "vampire",
                "vampires", "nervous")
                    || containsAny(answer, "i am afraid", "it frightens me to my core",
                    "if you need a remedy", "looking for magic"))) {
            return true;
        }
        if (containsAny(message, "hair", "brush", "taglock", "my name")
                && containsAny(answer, "give it to the witch", "she will use it wisely",
                "your hair is your shield", "i ll take it", "i will take it", "tastes fresh",
                "coven knows your hair", "happy to cut", "cut off what", "potions and charms")) {
            return true;
        }
        if (containsAny(message, "taglock", "tag lock")
                && !containsAny(answer, "taglock", "tag lock", "hair", "name", "bottle", "close", "guard")) {
            return true;
        }
        if (containsAny(message, "demon", "demons")
                && (containsAny(answer, "gold", "price", "sell you", "do you need anything else",
                "herbs and potions", "fumes that might help", "demon worship", "trade of herbs")
                    || !containsAny(answer, "door", "bell", "guard", "golem", "run", "shut", "demon",
                    "witch", "witches", "bargain", "debt"))) {
            return true;
        }
        if (containsAny(message, "weather", "rain", "storm", "sky", "clear sky", "crows", "milk turns sour",
                "sour milk")
                && containsAny(answer, "villagers wear clothes", "don t mean anything", "just birds",
                "i know what folk like to hear", "moon hangs high", "stars are still",
                "same as when we last spoke", "i don t know what you re on")) {
            return true;
        }
        if (containsAny(message, "milk turns sour", "sour milk", "milk sour")
                && (!containsAny(answer, "milk", "sour", "heat", "bad luck", "neighbor", "blame")
                    || containsAny(answer, "crows are weather", "gossip with wings"))) {
            return true;
        }
        if (containsAny(message, "food stores", "hungry", "hunger", "famine")
                && containsAny(answer, "keep the door open", "road knows no fear")) {
            return true;
        }
        if (containsAny(message, "harvest", "crops", "field", "fields", "clear sky", "omens", "bad harvest")
                && (containsAny(answer, "road is a witch s path", "road knows no fear",
                "keep the bell silent", "can t go there yet", "w  ", "lead you where you don t want",
                "moonlight turns them into wolves", "not all wolves turn", "coughs and colds",
                "bodies strong", "good nutrition", "air feels fresh", "birds chirp", "wolf howls",
                "bell jingles", "wind whistles", "rain drops")
                    || !containsAny(answer, "harvest", "crop", "crops", "field", "fields", "bread",
                    "soil", "seed", "seeds", "food", "stores", "hunger", "hungry", "mud", "roots",
                    "crows", "milk", "omen", "omens", "fence", "fences"))) {
            return true;
        }
        if (containsAny(message, "sleep here", "sleep tonight", "stay here tonight", "safe to sleep")
                && !containsAny(answer, "sleep", "bed", "door", "bell", "salt", "threshold")) {
            return true;
        }
        if (containsAny(message, "garlic")
                && containsAny(answer, "witch warm", "fresh in her mind", "real magic comes from")) {
            return true;
        }
        if (containsAny(message, "before dusk", "what should i do before dusk")
                && (containsAny(answer, "witch warm", "fresh in her mind")
                    || !containsAny(answer, "door", "bell", "bread", "dusk", "garlic", "threshold"))) {
            return true;
        }
        if (containsAny(message, "road", "besides a sword", "bring on the road")
                && containsAny(answer, "not too expensive", "apothecary shop", "good place for herbs")) {
            return true;
        }
        if (containsAny(message, "who watches the roads", "watch roads", "road after sunset", "roads after sunset")
                && (containsAny(answer, "roads watch", "keep your bed warm", "never know the truth")
                    || !containsAny(answer, "guard", "guards", "golem", "bell", "door", "lantern", "sunset",
                    "road", "roads", "watch"))) {
            return true;
        }
        if (containsAny(message, "goodbye", "farewell", "bye", "sleep here", "sleep tonight")
                && containsAny(answer, "hide behind me", "i ll look out for you", "i will look out for you")) {
            return true;
        }
        if (containsAny(message, "ring the bell", "if i ring", "when i ring")
                && containsAny(answer, "grab you", "take you away", "to the witch",
                "out of place", "not in danger", "do not know better", "bell jingles",
                "wind whistles", "rain drops", "creature moves nearby")) {
            return true;
        }
        if (containsAny(message, "ring the bell", "if i ring", "when i ring")
                && !containsAny(answer, "villager", "villagers", "doors", "door", "danger", "raid",
                "run", "hide", "bell")) {
            return true;
        }
        if (containsAny(message, "sleep here", "sleep tonight", "stay here tonight", "safe to sleep")
                && containsAny(answer, "yes dev sleep", "i ll keep watch", "i will keep watch",
                "keep watch with", "not afraid of nightfall", "that is not good")) {
            return true;
        }
        if (containsAny(message, "trust the witch", "trust a witch", "witch at the edge")
                && !containsAny(answer, "witch", "trust", "hair", "name", "bottle", "bottles", "taglock")) {
            return true;
        }
        if (containsAny(message, "buy something for travel", "something for travel", "travel")
                && containsAny(answer, "open doors and gates", "100 gold", "make an offer",
                "not too steep", "rare", "perhaps this one")) {
            return true;
        }
        return false;
    }

    private static String normalizeForCheck(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
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

    private static void holdAttention(Villager villager, ServerPlayer player) {
        if (villager == null || player == null || villager.level().isClientSide()
                || villager.level() != player.level() || !villager.isAlive() || !player.isAlive()) {
            return;
        }
        villager.getNavigation().stop();
        villager.getLookControl().setLookAt(player, 30.0F, 30.0F);
    }
}
