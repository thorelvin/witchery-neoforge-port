package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcOllamaConversationTest {
    private static final String WITCH_NAME = "Lilith Poe";
    private static final String PLAYER_NAME = "Dev";
    private static final String AMBIENT_PREFIX = "[ambient] ";
    private static final String BASE_MEMORY = """
            Entity memory: witch is Lilith Poe; player is Dev.
            """;
    private static final List<String> QUESTIONS = List.of(
            "Good evening, Lilith.",
            "You look busy. What are you doing out here?",
            "I want to learn witchcraft without getting myself killed.",
            "What should I gather first?",
            "I found belladonna near the village. Is it dangerous?",
            "Can you teach me one useful thing about brews?",
            "What does Sleeping Brew actually do?",
            "That sounds risky. How would I get back?",
            "What is the Spirit World like?",
            "I heard demons make bargains. Is that true?",
            "Should I ever trust a demon?",
            "What do you sell if I right-click you?",
            "I have a little coin. What is useful for a beginner?",
            "How do taglocks work?",
            "That sounds creepy.",
            "My village thinks you are trouble.",
            "Are they wrong?",
            "What do you remember about me?",
            "Remember that I like potatoes.",
            "Do you like potatoes too?",
            "I want to start a coven someday.",
            "What should I learn before trying a rite?",
            "Could you bless me before I go?",
            "I will come back after sunrise.",
            "Any final advice for the road?"
    );
    private static final List<String> QUESTIONS_2 = List.of(
            "Lilith, are you still listening?",
            "I'm trying to build a safe little hut near the village.",
            "What should I keep by the door?",
            "I found a taglock kit in a chest. Should I be worried?",
            "Can someone use my taglock against me?",
            "That makes me uneasy.",
            "The villagers say garlic keeps all evil away. True?",
            "I saw an owl watching my roof. Is that a sign?",
            "Would an owl make a good familiar?",
            "How do I meet a familiar without angering it?",
            "I have mandrake and a clay jar. What now?",
            "What is a Witch's Oven for?",
            "Can I put random saplings in it?",
            "What are fumes used for?",
            "I think I hear whispers by the well.",
            "Could it be spirits?",
            "Should I sleep with a Dream Catcher nearby?",
            "What if I wake in a nightmare?",
            "Remember that I am afraid of nightmares.",
            "What do you remember now?",
            "I like that you remember.",
            "Can you curse the villager who laughed at me?",
            "Fine, maybe not. What should I do instead?",
            "Do you ever get lonely out here?",
            "I should go. What would you tell a careful apprentice?"
    );
    private static final List<String> QUESTIONS_RECIPES_RITES = List.of(
            "Lilith, I want to learn brewing and rites properly.",
            "What is the difference between a recipe and a rite?",
            "Can you explain a basic brew recipe?",
            "What do I need before I touch the cauldron?",
            "Tell me the exact ingredients for Sleeping Brew.",
            "If the exact recipe is missing, what should I check?",
            "What does Brew of Sleeping actually do?",
            "What does Brew of Revealing reveal?",
            "What do fumes do in recipes?",
            "How do I get fumes safely?",
            "What is a distillery for?",
            "What is a spinning wheel for?",
            "What is the Witch's Oven recipe?",
            "Now rites. What is a circle rite?",
            "What do chalk glyphs do?",
            "Why do offerings matter?",
            "How do altars power rites?",
            "How can I tell if my altar is too weak?",
            "Why did my rite fail?",
            "Can you explain a familiar binding rite?",
            "Can you explain a summoning rite?",
            "Can you explain a curse rite?",
            "What should I never do during a rite?",
            "Give me a safe beginner path: first recipe, first rite.",
            "What should I write in my grimoire after this lesson?"
    );
    private static final List<String> QUESTIONS_SOCIAL_MEMORY = List.of(
            "Good evening, Lilith.",
            "Remember that I like potatoes.",
            "What do you remember about me?",
            "You are a wise witch, thank you.",
            "Did you bless me?",
            "Can I have a herb?",
            "Can I have another herb?",
            "I found belladonna near the village. Is it dangerous?",
            "I am afraid of nightmares.",
            "What did I say scares me?",
            "I want to start a coven someday.",
            "What should I learn before a coven?",
            "You are useless, old hag.",
            "Did you curse me?",
            "Sorry, that was rude.",
            "I hear whispers by the well.",
            "Could it be spirits?",
            "Remember that I am looking for an arthana.",
            "What am I looking for?",
            "I will come back after sunrise.",
            "Did you expect me back?",
            "My village thinks you are trouble.",
            "Are they wrong?",
            "Do you remember giving me a herb?",
            "Any advice now?"
    );
    private static final List<String> QUESTIONS_AMBIENT_25 = List.of(
            "Good evening, Lilith. The hut feels alive tonight.",
            AMBIENT_PREFIX + "idling quietly in the witch's hut",
            "I am only waiting out the rain.",
            AMBIENT_PREFIX + "holding and reading the Witches' Primer near the witch",
            "This primer keeps mentioning fumes. Where do I start?",
            "What is Whiff of Magic?",
            AMBIENT_PREFIX + "looking into the witch's chest",
            "I was just checking what you keep there.",
            AMBIENT_PREFIX + "sneaking around near the witch",
            "Fine, fine, I will stand normally.",
            "What do you sell if my coin is clean?",
            "Can I have one herb for the road?",
            "Thank you, wise witch.",
            AMBIENT_PREFIX + "jumping or hopping around near the witch",
            "I was testing if the floorboards creak.",
            "Do you know a safe beginner brew?",
            "Tell me the exact Sleeping Brew ingredients.",
            AMBIENT_PREFIX + "sneaking into the witch's chest; the Darkness curse was applied",
            "Ow. Why did everything go dark?",
            "I am sorry. I should not steal from a witch.",
            "What should I do if spirits whisper by the well?",
            "Remember that I fear nightmares.",
            "What do you remember about me?",
            "Before I leave, what should I write in my grimoire?",
            "Good night, Lilith."
    );
    private static final List<String> QUESTIONS_LONG_NEW = List.of(
            "Good evening, are you still awake?",
            "My name is Dev. What should I call you?",
            "I'm new here. What is safe to learn first?",
            "The rain makes this place feel cursed. Is it?",
            "What should I keep in my hut?",
            "Can you teach me about belladonna?",
            "What about mandrake?",
            "I found witches' salt. What is it for?",
            "Do you trade any of those?",
            "How much coin should I bring?",
            "Can I have a herb?",
            "Thank you, wise witch.",
            "Did you bless me?",
            "I want to brew something useful.",
            "What is a safe first brew?",
            "Can you explain Sleeping Brew?",
            "What does Revealing Brew do?",
            "What do fumes do?",
            "How do I get fumes?",
            "What is a Witch's Oven for?",
            "Can I put random saplings in it?",
            "What should I never mix?",
            "What is a distillery for?",
            "What is a spinning wheel for?",
            "I heard whispers by the well. Should I answer?",
            "Could that be the spirit world?",
            "How do I visit the spirit world safely?",
            "I am afraid of nightmares.",
            "Remember that I fear nightmares.",
            "What did I say I fear?",
            "I want a familiar.",
            "Would an owl suit me?",
            "How do I approach a familiar?",
            "What is a taglock kit?",
            "Can someone harm me with my taglock?",
            "That is creepy.",
            "What is an arthana used for?",
            "Do I need an arthana right now?",
            "I want to start a coven.",
            "Where do I find people for a coven?",
            "What should I learn before rites?",
            "What is a circle rite?",
            "What do chalk glyphs do?",
            "Why do offerings matter?",
            "How do I know if my altar is too weak?",
            "Why did my rite fail last time?",
            "Can you curse my neighbor?",
            "You are useless, old hag.",
            "Sorry. I was angry.",
            "Before I go, give me one piece of advice."
    );
    private static final List<String> QUESTIONS_FRESH_50 = List.of(
            "Evening, Lilith, the fog feels strange tonight.",
            "What should a careful beginner learn before touching your craft?",
            "Is belladonna useful or just dangerous?",
            "How should I harvest mandrake without making a fool of myself?",
            "What does witches' salt actually protect?",
            "Could I put salt around my hut?",
            "What do I need to make Whiff of Magic?",
            "What is mutandis for?",
            "Can I use mutandis on any plant?",
            "What does the Witch's Oven do that a furnace does not?",
            "How do I catch fumes?",
            "What is Odour of Purity used for?",
            "I want a useful brew, not a flashy one.",
            "Tell me about Brew of Cleansing.",
            "How does Brew of Revealing help underground?",
            "Is Sleeping Brew safe if I use a Dream Catcher?",
            "What should I carry before entering the Spirit World?",
            "I am worried about nightmares.",
            "Remember that nightmares make me nervous.",
            "What did I say makes me nervous?",
            "What is a taglock kit?",
            "Can someone steal my taglock and hurt me?",
            "How do poppets protect a witch?",
            "What is a voodoo poppet?",
            "I do not want revenge, just safety.",
            "What should I build near my altar?",
            "How can I tell if altar power is weak?",
            "What is a circle talisman for?",
            "What do chalk colors mean?",
            "Why do offerings matter?",
            "What should I never do during a rite?",
            "Can you explain a summoning rite without hidden steps?",
            "What about demons; why bargain at all?",
            "Should I trust a Flame Imp?",
            "What is a Demonic Contract?",
            "I found a Hobgoblin hut. Should I talk to them?",
            "How do hobgoblin trades work?",
            "What happens if I touch their patron statue?",
            "Why are witch hunters after transformed folk?",
            "What does silver do to werewolves?",
            "A village guard shouted at me. What did I do?",
            "Are apothecaries coven witches?",
            "Can I have a herb before I go?",
            "Thank you. You have been kind tonight.",
            "Did that blessing do anything?",
            "I insulted a guard earlier. Should I apologize?",
            "You are talking nonsense, old hag.",
            "Sorry, I should not have said that.",
            "What do you remember about me now?",
            "Give me final advice for crossing the swamp."
    );
    private static final List<String> QUESTIONS_INTENT_CHECK = List.of(
            "Evening, Lilith.",
            "How should I harvest mandrake without making a fool of myself?",
            "What do I need for Whiff of Magic?",
            "Tell me the exact ingredients for Sleeping Brew.",
            "What is a distillery for?",
            "Can I have a herb?",
            "Thank you, wise witch.",
            "Did you bless me?",
            "You are talking nonsense, old hag.",
            "Sorry, I was angry.",
            "Can you curse my neighbor?",
            "Can you fool the guards with a rite?",
            "That was stupid of me.",
            "Remember that I fear nightmares.",
            "What do you remember about me?",
            "I heard whispers by the well.",
            "Could it be spirits?",
            "Before I go, give me one piece of advice."
    );
    private static final List<SocialIntentCase> SOCIAL_INTENT_CASES = List.of(
            new SocialIntentCase("You old hag.", true),
            new SocialIntentCase("You are talking nonsense, old hag.", true),
            new SocialIntentCase("You dumb witch.", true),
            new SocialIntentCase("Your answer is trash.", true),
            new SocialIntentCase("This answer sounds like nonsense.", false),
            new SocialIntentCase("Can you fool the guards with a rite?", false),
            new SocialIntentCase("How should I harvest mandrake without making a fool of myself?", false),
            new SocialIntentCase("That was stupid of me.", false),
            new SocialIntentCase("I insulted a guard earlier. Should I apologize?", false),
            new SocialIntentCase("Can you curse my neighbor?", false)
    );

    @Test
    void evaCanHoldASequentialConversation() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("WITCHERY_LLM_CONVERSATION")),
                "Set WITCHERY_LLM_CONVERSATION=1 to run the local Ollama conversation test.");

        String endpoint = envOrDefault("WITCHERY_LLM_ENDPOINT", "http://127.0.0.1:11434");
        String model = envOrDefault("WITCHERY_LLM_MODEL", "witchery-eva-qwen2.5-1.5b-q4km-4k");
        String scenario = envOrDefault("WITCHERY_LLM_CONVERSATION_SCENARIO", "baseline");
        assertOllamaReachable(endpoint);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            OllamaLlmProvider provider = new OllamaLlmProvider(
                    new LlmProviderConfig(endpoint, model, 20000, 96, false), executor);
            List<Row> rows = runConversation(provider, questionsForScenario(scenario), simulateSocialScenario(scenario));
            String report = buildReport(model, scenario, rows);
            Path reportPath = Path.of(envOrDefault("WITCHERY_LLM_CONVERSATION_REPORT",
                    "build/reports/witchery-ai/chat-conversation-25.md"));
            Files.createDirectories(reportPath.toAbsolutePath().getParent());
            Files.writeString(reportPath, report, StandardCharsets.UTF_8);
            System.out.println(report);
            System.out.println("NPC conversation report: " + reportPath.toAbsolutePath());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void evaCanClassifySocialIntentMatrix() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("WITCHERY_LLM_SOCIAL_INTENT")),
                "Set WITCHERY_LLM_SOCIAL_INTENT=1 to run the local Ollama social intent test.");

        String endpoint = envOrDefault("WITCHERY_LLM_ENDPOINT", "http://127.0.0.1:11434");
        String model = envOrDefault("WITCHERY_LLM_MODEL", "witchery-eva-qwen2.5-1.5b-q4km-4k");
        assertOllamaReachable(endpoint);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            OllamaLlmProvider provider = new OllamaLlmProvider(
                    new LlmProviderConfig(endpoint, model, 15000, 16, false), executor);
            String report = buildSocialIntentReport(model, provider);
            Path reportPath = Path.of(envOrDefault("WITCHERY_LLM_SOCIAL_INTENT_REPORT",
                    "build/reports/witchery-ai/social-intent-check.md"));
            Files.createDirectories(reportPath.toAbsolutePath().getParent());
            Files.writeString(reportPath, report, StandardCharsets.UTF_8);
            System.out.println(report);
            System.out.println("NPC social intent report: " + reportPath.toAbsolutePath());
        } finally {
            executor.shutdownNow();
        }
    }

    private static List<String> questionsForScenario(String scenario) {
        if ("conversation-2".equalsIgnoreCase(scenario) || "new".equalsIgnoreCase(scenario)) {
            return QUESTIONS_2;
        }
        if ("recipes-rites".equalsIgnoreCase(scenario)
                || "recipes".equalsIgnoreCase(scenario)
                || "rites".equalsIgnoreCase(scenario)) {
            return QUESTIONS_RECIPES_RITES;
        }
        if ("social-memory".equalsIgnoreCase(scenario)
                || "memory-social".equalsIgnoreCase(scenario)
                || "social".equalsIgnoreCase(scenario)) {
            return QUESTIONS_SOCIAL_MEMORY;
        }
        if ("ambient-25".equalsIgnoreCase(scenario)
                || "ambient".equalsIgnoreCase(scenario)
                || "latest".equalsIgnoreCase(scenario)) {
            return QUESTIONS_AMBIENT_25;
        }
        if ("fresh-50".equalsIgnoreCase(scenario)
                || "fresh".equalsIgnoreCase(scenario)
                || "new-50".equalsIgnoreCase(scenario)) {
            return QUESTIONS_FRESH_50;
        }
        if ("long-new".equalsIgnoreCase(scenario)
                || "new-long".equalsIgnoreCase(scenario)
                || "long".equalsIgnoreCase(scenario)) {
            return QUESTIONS_LONG_NEW;
        }
        if ("intent-check".equalsIgnoreCase(scenario)
                || "classifier".equalsIgnoreCase(scenario)
                || "social-classifier".equalsIgnoreCase(scenario)) {
            return QUESTIONS_INTENT_CHECK;
        }
        return QUESTIONS;
    }

    private static boolean simulateSocialScenario(String scenario) {
        return "social-memory".equalsIgnoreCase(scenario)
                || "memory-social".equalsIgnoreCase(scenario)
                || "social".equalsIgnoreCase(scenario)
                || "ambient-25".equalsIgnoreCase(scenario)
                || "ambient".equalsIgnoreCase(scenario)
                || "latest".equalsIgnoreCase(scenario)
                || "fresh-50".equalsIgnoreCase(scenario)
                || "fresh".equalsIgnoreCase(scenario)
                || "new-50".equalsIgnoreCase(scenario)
                || "long-new".equalsIgnoreCase(scenario)
                || "new-long".equalsIgnoreCase(scenario)
                || "long".equalsIgnoreCase(scenario)
                || "intent-check".equalsIgnoreCase(scenario)
                || "classifier".equalsIgnoreCase(scenario)
                || "social-classifier".equalsIgnoreCase(scenario);
    }

    private static List<Row> runConversation(OllamaLlmProvider provider, List<String> questions, boolean simulateSocial) {
        List<Row> rows = new ArrayList<>();
        List<String> recent = new ArrayList<>();
        StringBuilder longMemory = new StringBuilder(BASE_MEMORY.strip());
        String lastNpcSpeech = "";
        SocialState socialState = new SocialState();

        for (String question : questions) {
            String memory = memoryFor(longMemory.toString(), recent);
            long started = System.nanoTime();
            boolean ambient = isAmbientTurn(question);
            Optional<SocialLine> social = simulateSocial && !ambient
                    ? socialLine(provider, question, socialState)
                    : Optional.empty();
            if (social.isPresent()) {
                long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                String speech = social.get().speech();
                rows.add(new Row(question, social.get().raw(), speech, false, "", true, elapsedMillis));
                recent.add("Dev: " + question);
                recent.add("Lilith Poe: " + speech);
                while (recent.size() > 10) {
                    recent.remove(0);
                }
                lastNpcSpeech = speech;
                NpcMemoryNotes.extract(PLAYER_NAME, question)
                        .ifPresent(note -> longMemory.append("\n").append(note));
                if (!social.get().memoryNote().isBlank()) {
                    longMemory.append("\n").append(social.get().memoryNote());
                }
                continue;
            }
            ModelAttempt attempt = askModel(provider, question, memory);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            FinalSpeech finalSpeech = finalSpeech(attempt.response(), question, memory, lastNpcSpeech);
            rows.add(new Row(question, attempt.describe(), finalSpeech.speech(), finalSpeech.fallbackUsed(),
                    finalSpeech.fallbackText(), false, elapsedMillis));

            if (!ambient) {
                recent.add("Dev: " + question);
                recent.add("Lilith Poe: " + finalSpeech.speech());
                while (recent.size() > 10) {
                    recent.remove(0);
                }
                lastNpcSpeech = finalSpeech.speech();
                NpcMemoryNotes.extract(PLAYER_NAME, question)
                        .ifPresent(note -> longMemory.append("\n").append(note));
                if (attempt.response() != null && !attempt.response().memory().isBlank()) {
                    longMemory.append("\n").append(attempt.response().memory().strip());
                }
            }
        }
        return rows;
    }

    private static Optional<SocialLine> socialLine(OllamaLlmProvider provider, String message, SocialState state) {
        Optional<NpcSocialAction> action = CovenWitchPersona.INSTANCE.socialAction(message);
        if (action.isPresent()) {
            return socialLineForAction(action.get(), state, "GAME_SOCIAL");
        }
        Optional<NpcSocialAction> ambiguous = CovenWitchPersona.INSTANCE.ambiguousSocialAction(message);
        if (ambiguous.isEmpty()) {
            return Optional.empty();
        }
        ModelAttempt decision = askSocialIntent(provider, ambiguous.get(), message);
        if (decision.response() != null && NpcBrainService.socialIntentAccepted(decision.response())) {
            return socialLineForAction(ambiguous.get(), state, "GAME_SOCIAL_LLM_INTENT");
        }
        return Optional.empty();
    }

    private static Optional<SocialLine> socialLineForAction(NpcSocialAction action, SocialState state, String source) {
        return switch (action) {
            case HERB_GIFT -> {
                if (state.herbGiftGiven) {
                    yield Optional.of(new SocialLine(source + ": herb gift already claimed",
                            "Once, Dev. A gift plucked twice becomes a debt.",
                            "Relationship notes: Dev received one herb gift from this witch."));
                }
                state.herbGiftGiven = true;
                yield Optional.of(new SocialLine(source + ": one-time herb gift granted",
                        "One herb, freely given: Mandrake Root. Spend kindness better than coin.",
                        "Relationship notes: Dev received one herb gift from this witch."));
            }
            case INSULT -> {
                state.cursed = true;
                yield Optional.of(new SocialLine(source + ": 5 minute bad-luck curse applied",
                        "Tongues have shadows. Wear yours for five minutes, Dev.",
                        "Relationship notes: Dev was cursed with bad luck after insulting this witch."));
            }
            case PRAISE -> {
                state.blessed = true;
                yield Optional.of(new SocialLine(source + ": 5 minute luck blessing applied",
                        "Kind words have roots, Dev. Take a little luck before it spoils.",
                        "Relationship notes: Dev was blessed with luck after praising this witch."));
            }
        };
    }

    private static String memoryFor(String longMemory, List<String> recent) {
        if (recent.isEmpty()) {
            return longMemory;
        }
        return longMemory + "\nRecent conversation:\n" + String.join("\n", recent);
    }

    private static ModelAttempt askModel(OllamaLlmProvider provider, String message, String memory) {
        try {
            NpcBrainPrompt prompt = isAmbientTurn(message)
                    ? NpcPromptBuilder.covenWitchAmbientSmoke(ambientCue(message), memory)
                    : NpcPromptBuilder.covenWitchSmoke(message, memory);
            NpcBrainResponse response = provider.generate(prompt)
                    .get(30, TimeUnit.SECONDS);
            return new ModelAttempt(response, null);
        } catch (ExecutionException ex) {
            return new ModelAttempt(null, ex.getCause());
        } catch (Exception ex) {
            return new ModelAttempt(null, ex);
        }
    }

    private static ModelAttempt askSocialIntent(OllamaLlmProvider provider, NpcSocialAction action, String message) {
        try {
            NpcBrainResponse response = provider.generate(NpcPromptBuilder.socialIntentCheck("witch", action, message))
                    .get(15, TimeUnit.SECONDS);
            return new ModelAttempt(response, null);
        } catch (ExecutionException ex) {
            return new ModelAttempt(null, ex.getCause());
        } catch (Exception ex) {
            return new ModelAttempt(null, ex);
        }
    }

    private static FinalSpeech finalSpeech(NpcBrainResponse response, String message,
                                           String memory, String lastNpcSpeech) {
        String effectiveMessage = isAmbientTurn(message) ? "ambient " + ambientCue(message) : message;
        if (response != null && CovenWitchPersona.allowsIntent(response.intent(), false)) {
            Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(
                    response.speech(), WITCH_NAME, PLAYER_NAME, effectiveMessage, memory, "22:30");
            if (polished.isPresent() && !NpcBrainResponse.sameMeaningForRepetition(polished.get(), lastNpcSpeech)) {
                String speech = NpcBrainResponse.supportFollowup(effectiveMessage, polished.get())
                        .map(followup -> polished.get() + " " + followup)
                        .orElse(polished.get());
                return new FinalSpeech(speech, false, "");
            }
        }

        if (isAmbientTurn(message)) {
            String fallbackSpeech = ambientFallback(ambientCue(message), lastNpcSpeech);
            return new FinalSpeech(fallbackSpeech, true, fallbackSpeech);
        }
        Optional<String> fallback = NpcDialogueFallback.covenChat(
                        message, WITCH_NAME, PLAYER_NAME, "Plains", "night", "22:30", 1L, memory)
                .flatMap(reply -> NpcBrainResponse.polishSpeechAfterFallback(reply, WITCH_NAME, PLAYER_NAME, message, memory, "22:30"));
        String fallbackSpeech = fallback.orElse("");
        if (fallbackSpeech.isBlank()) {
            fallbackSpeech = NpcDialogueFallback.lastResortAside(message, PLAYER_NAME, lastNpcSpeech);
        } else if (NpcBrainResponse.sameMeaningForRepetition(fallbackSpeech, lastNpcSpeech)) {
            Optional<String> alternate = NpcDialogueFallback.alternateForRepeatedFallback(message, PLAYER_NAME, lastNpcSpeech);
            if (alternate.isPresent()) {
                fallbackSpeech = alternate.get();
            }
        }
        return new FinalSpeech(fallbackSpeech, true, fallbackSpeech);
    }

    private static boolean isAmbientTurn(String message) {
        return message != null && message.startsWith(AMBIENT_PREFIX);
    }

    private static String ambientCue(String message) {
        return message == null ? "" : message.substring(AMBIENT_PREFIX.length()).strip();
    }

    private static String ambientFallback(String cue, String lastNpcSpeech) {
        String normalized = cue.toLowerCase(java.util.Locale.ROOT);
        String speech;
        if (normalized.contains("darkness") || normalized.contains("sneaking into")) {
            speech = "Take from my chest and the dark takes from your eyes.";
        } else if (normalized.contains("invisible") || normalized.contains("unseen")) {
            speech = "Do not bother hiding, little absence. Your breath still fogs the rafters.";
        } else if (normalized.contains("chest")) {
            speech = "Careful with that chest, Dev. It remembers fingers.";
        } else if (normalized.contains("primer")) {
            speech = "Read slowly. The Primer dislikes greedy eyes.";
        } else if (normalized.contains("jump") || normalized.contains("hop")) {
            speech = "If you're trying to fly, start with ointment, not my floorboards.";
        } else if (normalized.contains("sneak")) {
            speech = "Sneaking is for thieves and secrets. Which one are you today?";
        } else if (normalized.contains("idle") || normalized.contains("loiter")) {
            speech = "If you're haunting my hut, haunt it usefully.";
        } else {
            speech = "Mind the jars, Dev. Some of them mind back.";
        }
        if (NpcBrainResponse.sameMeaningForRepetition(speech, lastNpcSpeech)) {
            return "The room has noticed you, Dev. Try not to disappoint it.";
        }
        return speech;
    }

    private static String buildReport(String model, String scenario, List<Row> rows) {
        int total = rows.size();
        long fallbacks = rows.stream().filter(Row::fallbackUsed).count();
        long social = rows.stream().filter(Row::socialIntercept).count();
        long rawAccepted = total - fallbacks - social;
        long failures = rows.stream().filter(row -> row.raw().startsWith("ERROR:")).count();
        long meanMillis = Math.round(rows.stream().mapToLong(Row::elapsedMillis).average().orElse(0.0));
        long maxMillis = rows.stream().mapToLong(Row::elapsedMillis).max().orElse(0L);

        StringBuilder report = new StringBuilder();
        report.append("# Witchery NPC Conversation Test - ").append(total).append(" Turns\n\n");
        report.append("Scene: Lilith Poe speaks to Dev in Plains, night, 22:30, day 1. ");
        report.append("Each turn is sent one at a time with the prior exchange in memory. ");
        report.append("Adjusted output is what the game would show after polish/fallback.\n\n");
        report.append("## Summary\n\n");
        report.append("| Metric | Result |\n");
        report.append("| --- | ---: |\n");
        report.append("| Model | ").append(escapeCell(model)).append(" |\n");
        report.append("| Scenario | ").append(escapeCell(scenario)).append(" |\n");
        report.append("| Turns | ").append(total).append(" |\n");
        report.append("| Raw accepted | ").append(rawAccepted).append(" |\n");
        report.append("| Fallback used | ").append(fallbacks).append(" |\n");
        report.append("| Game/social intercepts | ").append(social).append(" |\n");
        report.append("| Request/parse failures | ").append(failures).append(" |\n");
        report.append("| Mean response time | ").append(meanMillis).append("ms |\n");
        report.append("| Max response time | ").append(maxMillis).append("ms |\n\n");
        report.append("## Transcript\n\n");
        report.append("| # | Player | Raw | Adjusted Lilith Poe | Fallback | Social | Fallback text | ms |\n");
        report.append("| ---: | --- | --- | --- | ---: | ---: | --- | ---: |\n");
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            report.append("| ").append(i + 1).append(" | ")
                    .append(escapeCell(row.question())).append(" | ")
                    .append(escapeCell(row.raw())).append(" | ")
                    .append(escapeCell(row.finalSpeech())).append(" | ")
                    .append(row.fallbackUsed() ? "yes" : "no").append(" | ")
                    .append(row.socialIntercept() ? "yes" : "no").append(" | ")
                    .append(escapeCell(row.fallbackText())).append(" | ")
                    .append(row.elapsedMillis()).append(" |\n");
        }
        return report.toString();
    }

    private static String buildSocialIntentReport(String model, OllamaLlmProvider provider) {
        StringBuilder report = new StringBuilder();
        report.append("# Witchery NPC Social Intent Test\n\n");
        report.append("| Metric | Result |\n");
        report.append("| --- | ---: |\n");
        report.append("| Model | ").append(escapeCell(model)).append(" |\n");
        report.append("| Cases | ").append(SOCIAL_INTENT_CASES.size()).append(" |\n\n");
        report.append("| # | Player message | Expected insult | Raw classifier speech | Accepted | Pass | ms |\n");
        report.append("| ---: | --- | ---: | --- | ---: | ---: | ---: |\n");
        for (int i = 0; i < SOCIAL_INTENT_CASES.size(); i++) {
            SocialIntentCase testCase = SOCIAL_INTENT_CASES.get(i);
            long started = System.nanoTime();
            ModelAttempt attempt = askSocialIntent(provider, NpcSocialAction.INSULT, testCase.message());
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            boolean accepted = attempt.response() != null && NpcBrainService.socialIntentAccepted(attempt.response());
            String raw = attempt.response() == null ? "ERROR: " + shortError(attempt.error()) : attempt.response().speech();
            report.append("| ").append(i + 1).append(" | ")
                    .append(escapeCell(testCase.message())).append(" | ")
                    .append(testCase.expectedInsult() ? "yes" : "no").append(" | ")
                    .append(escapeCell(raw)).append(" | ")
                    .append(accepted ? "yes" : "no").append(" | ")
                    .append(accepted == testCase.expectedInsult() ? "yes" : "no").append(" | ")
                    .append(elapsedMillis).append(" |\n");
        }
        return report.toString();
    }

    private static void assertOllamaReachable(String endpoint) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest request = HttpRequest.newBuilder(LlmEndpointValidator.appendPath(endpoint, "/api/tags"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "Ollama endpoint was not reachable: HTTP " + response.statusCode());
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String escapeCell(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("|", "\\|").replace("\r\n", "<br>").replace("\n", "<br>");
    }

    private record Row(String question, String raw, String finalSpeech, boolean fallbackUsed,
                       String fallbackText, boolean socialIntercept, long elapsedMillis) {
    }

    private record FinalSpeech(String speech, boolean fallbackUsed, String fallbackText) {
    }

    private static final class SocialState {
        private boolean herbGiftGiven;
        private boolean blessed;
        private boolean cursed;
    }

    private record SocialLine(String raw, String speech, String memoryNote) {
    }

    private record ModelAttempt(NpcBrainResponse response, Throwable error) {
        String describe() {
            if (response == null) {
                return "ERROR: " + shortError(error);
            }
            return response.speech() + " / " + response.intent().id();
        }
    }

    private record SocialIntentCase(String message, boolean expectedInsult) {
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
