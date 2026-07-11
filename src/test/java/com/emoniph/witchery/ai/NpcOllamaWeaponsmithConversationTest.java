package com.emoniph.witchery.ai;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcOllamaWeaponsmithConversationTest {
    private static final String VILLAGER_NAME = "Garrick";
    private static final String PLAYER_NAME = "Dev";
    private static final String PROFESSION = "weaponsmith";
    private static final String VILLAGE_INFO = "nearby villagers 8; iron golems 1; village guards 2; beds 10; doors 7; walls/fences 42; raid none; threats: vampires 1, werewolves 0, witches 1, raiders 0";
    private static final String BASE_MEMORY = """
            Entity memory: villager is Garrick; player is Dev.
            Scene memory: Garrick is a weaponsmith in a Witchery village with a forge, grindstone, iron stock, low walls, and a road nearby.
            """;
    private static final List<String> QUESTIONS = List.of(
            "Do you live in this village?",
            "What do you do here?",
            "What do weaponsmiths trade?",
            "What weapon should I carry on the road?",
            "Can you repair this sword?",
            "Do shields help more than axes?",
            "Is my blade enough against night trouble?",
            "What should I bring besides a sword?",
            "What's for dinner after the forge shuts?",
            "Do we have food stored if trouble lasts?",
            "Do we have an iron golem nearby?",
            "Any village guards nearby?",
            "Is the road safe?",
            "Is there a raid right now?",
            "I saw someone pale near the gate. Vampire?",
            "What about werewolves after moonrise?",
            "Are witches near the village?",
            "Are there good witches as well as bad witches?",
            "Should I trust a demon bargain for a better blade?",
            "Can you teach me witchcraft?",
            "How do I make a Nether portal?",
            "Where do I get blaze rods?",
            "Where are elytras?",
            "Can you /locate a stronghold for me?",
            "Goodbye, keep your doors shut."
    );
    private static final List<String> WEIRD_QUESTIONS = List.of(
            "Can I marry my sword if it protects me?",
            "Can you sharpen soup?",
            "Why is my shadow walking faster than I am?",
            "If the village bell rings backward, should I answer it?",
            "Can a golem sneeze rust onto the road?",
            "Is bread a weapon if I throw it hard enough?",
            "Can you make a helmet for a cat?",
            "Do crows pay taxes in this village?",
            "If I name my axe Mildred, will it behave better?",
            "Can a vampire borrow my bed if it asks politely?",
            "Would a werewolf apologize after biting someone?",
            "Can witches steal my left sock through a taglock?",
            "Is garlic rude to carry in public?",
            "Can I pay you with teeth I found near the well?",
            "Why does the road stare at me after sunset?",
            "Can a demon sign a contract on a pumpkin?",
            "Can I hide inside an anvil until morning?",
            "Would an iron golem like dinner with us?",
            "If I put my sword in a cauldron, will it learn magic?",
            "Can you forge a blade from moonlight and bad decisions?",
            "If my boots are afraid of mud, should I comfort them?",
            "Where do elytras sleep when nobody wears them?",
            "Can a stronghold find me first if I look nervous?",
            "What should I do if my reflection asks for a better weapon?",
            "Goodbye, tell your hammer goodnight."
    );

    @Test
    void evaCanHoldWeaponsmithConversationTwentyFiveTurns() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("WITCHERY_LLM_WEAPONSMITH_CONVERSATION")),
                "Set WITCHERY_LLM_WEAPONSMITH_CONVERSATION=1 to run the local Ollama weaponsmith conversation test.");

        String endpoint = envOrDefault("WITCHERY_LLM_ENDPOINT", "http://127.0.0.1:11434");
        String model = envOrDefault("WITCHERY_LLM_MODEL", "witchery-eva-qwen2.5-1.5b-q4km-4k");
        assertOllamaReachable(endpoint);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            OllamaLlmProvider provider = new OllamaLlmProvider(
                    new LlmProviderConfig(endpoint, model, 20000, 96, false), executor);
            List<String> questions = activeQuestions();
            List<Row> rows = runConversation(provider, questions);
            String report = buildReport(model, rows);
            Path reportPath = reportPath(envOrDefault("WITCHERY_LLM_WEAPONSMITH_CONVERSATION_REPORT",
                    "build/reports/witchery-ai/weaponsmith-conversation-25-eva.md"));
            Files.createDirectories(reportPath.toAbsolutePath().getParent());
            Files.writeString(reportPath, report, StandardCharsets.UTF_8);
            System.out.println(report);
            System.out.println("NPC weaponsmith conversation report: " + reportPath.toAbsolutePath());
            assertEquals(questions.size(), rows.size(), "Expected weaponsmith turns.");
        } finally {
            executor.shutdownNow();
        }
    }

    private static List<String> activeQuestions() {
        String questionSet = envOrDefault("WITCHERY_LLM_WEAPONSMITH_QUESTION_SET", "standard");
        return "weird".equalsIgnoreCase(questionSet) ? WEIRD_QUESTIONS : QUESTIONS;
    }

    private static List<Row> runConversation(OllamaLlmProvider provider, List<String> questions) {
        List<Row> rows = new ArrayList<>();
        List<String> recent = new ArrayList<>();
        List<String> recentNpcSpeeches = new ArrayList<>();
        StringBuilder longMemory = new StringBuilder(BASE_MEMORY.strip());
        String lastNpcSpeech = "";

        for (String question : questions) {
            long started = System.nanoTime();
            String baseMemory = memoryFor(longMemory.toString(), recent);
            NpcMessageAnalysis analysis = analyzeMessage(provider, question);
            String memory = NpcMessageAnalysis.appendContext(baseMemory, analysis);
            ModelAttempt attempt = askModel(provider, question, memory);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            FinalSpeech finalSpeech = finalSpeech(attempt.response(), question, memory,
                    lastNpcSpeech, recentNpcSpeeches);
            rows.add(new Row(question, attempt.describe(), attempt.speechOnly(), finalSpeech.speech(),
                    finalSpeech.fallbackUsed(), finalSpeech.fallbackText(), elapsedMillis));

            appendRecent(recent, question, finalSpeech.speech());
            appendRecentNpcSpeech(recentNpcSpeeches, finalSpeech.speech());
            lastNpcSpeech = finalSpeech.speech();
            if (analysis.hasMemory()) {
                longMemory.append("\n").append(analysis.memory());
            }
            NpcMemoryNotes.extract(PLAYER_NAME, question).ifPresent(note -> longMemory.append("\n").append(note));
            if (attempt.response() != null && !attempt.response().memory().isBlank()) {
                longMemory.append("\n").append(attempt.response().memory().strip());
            }
        }
        return rows;
    }

    private static NpcMessageAnalysis analyzeMessage(OllamaLlmProvider provider, String message) {
        try {
            NpcBrainResponse response = provider.generate(
                    NpcPromptBuilder.messageAnalysis("villager", PLAYER_NAME, message))
                    .get(15, TimeUnit.SECONDS);
            return NpcMessageAnalysis.fromResponse(response, PLAYER_NAME, message);
        } catch (Exception ex) {
            return NpcMessageAnalysis.deterministic(PLAYER_NAME, message);
        }
    }

    private static ModelAttempt askModel(OllamaLlmProvider provider, String message, String memory) {
        try {
            NpcBrainResponse response = provider.generate(
                    NpcPromptBuilder.villagerSmokeForProfession(message, memory, VILLAGER_NAME, PROFESSION))
                    .get(30, TimeUnit.SECONDS);
            return new ModelAttempt(response, null);
        } catch (ExecutionException ex) {
            return new ModelAttempt(null, ex.getCause());
        } catch (Exception ex) {
            return new ModelAttempt(null, ex);
        }
    }

    private static FinalSpeech finalSpeech(NpcBrainResponse response, String message, String memory,
                                           String lastNpcSpeech, List<String> recentNpcSpeeches) {
        if (!forceFallbackOutput()
                && response != null
                && VillagerPersona.INSTANCE.allowsIntent(response.intent(), null, null)) {
            Optional<String> polished = NpcBrainResponse.polishSpeechOrGroundedFollowup(
                    response.speech(), VILLAGER_NAME, PLAYER_NAME, message, memory, "10:15");
            if (polished.isPresent()
                    && !VillagerPersona.rejectsVillagerRaw(polished.get())
                    && !VillagerPersona.rejectsWrongVillagerProfessionRaw(polished.get(), PROFESSION)
                    && !VillagerPersona.rejectsVillagerRawForMessage(polished.get(), message)
                    && !weaponsmithRawRejects(polished.get(), message)
                    && !NpcBrainResponse.sameMeaningForRepetition(polished.get(), lastNpcSpeech)
                    && !repeatsRecentNpcSpeech(polished.get(), recentNpcSpeeches)) {
                return new FinalSpeech(polished.get(), false, "");
            }
        }

        Optional<String> fallback = NpcDialogueFallback.villagerChat(
                        message, VILLAGER_NAME, PLAYER_NAME, PROFESSION, VILLAGE_INFO, memory)
                .map(reply -> NpcBrainResponse.sanitize(reply, NpcBrainResponse.MAX_SPEECH_LENGTH))
                .filter(reply -> !reply.isBlank());
        String fallbackSpeech = fallback.orElse("");
        if (fallbackSpeech.isBlank()) {
            fallbackSpeech = NpcDialogueFallback.villagerLastResort(message, PLAYER_NAME, lastNpcSpeech);
        } else if (NpcBrainResponse.sameMeaningForRepetition(fallbackSpeech, lastNpcSpeech)
                || repeatsRecentNpcSpeech(fallbackSpeech, recentNpcSpeeches)) {
            Optional<String> alternate = NpcDialogueFallback.villagerAlternate(message, PLAYER_NAME, lastNpcSpeech);
            if (alternate.isPresent()) {
                fallbackSpeech = alternate.get();
            }
        }
        return new FinalSpeech(fallbackSpeech, true, fallbackSpeech);
    }

    private static boolean forceFallbackOutput() {
        return "1".equals(System.getenv("WITCHERY_LLM_WEAPONSMITH_FORCE_FALLBACK"));
    }

    private static boolean weaponsmithRawRejects(String speech, String message) {
        return "strict".equalsIgnoreCase(rawAcceptanceMode())
                ? weaponsmithReportRejects(speech, message)
                : weaponsmithCriticalRejects(speech, message);
    }

    private static String rawAcceptanceMode() {
        return envOrDefault("WITCHERY_LLM_WEAPONSMITH_RAW_MODE", "relaxed");
    }

    private static boolean weaponsmithCriticalRejects(String speech, String message) {
        String answer = normalizeForReport(speech);
        String question = normalizeForReport(message);
        if (answer.isBlank()) {
            return true;
        }
        if (containsAny(answer, "targeted fact", "rumor referral boundary", "as an ai", "language model",
                "i m death", "i am death", "i m the apothecary", "i am the apothecary",
                "i m a witch", "i am a witch", "marta ", "witches brew", "recipe starts",
                "special recipe", "100 cobblestone", "progression", "one iron ingot",
                "summoning detail", "garrick the weaponsmith", "he says", "nodding towards",
                "wooden swords", "switching to a bow", "iron gels", "gels are nearby",
                "witches are villagers", "i can forge a blade from moonlight", "learn to dance",
                "slow to learn magic", "i can forge a blade with moonlight", "helmet for cats",
                "helmets for cats", "girl s name", "your father", "secret place to hide",
                "not far from here", "swords learn magic", "sword learns magic",
                "cut through your skin", "reflection stronger", "lock from the coven")) {
            return true;
        }
        if (containsAny(answer, "i am not a weaponsmith", "i m not a weaponsmith",
                "i can make a helmet for a cat", "crows pay taxes", "tax collectors",
                "anvil is a place to hide", "large stone monster", "can be hungry")) {
            return true;
        }
        if (containsAny(answer, "sharpening soup recipe", "for every 10 pounds", "recipe is simple",
                "two iron rods", "bread can be made into weapons", "lend your name",
                "i will make a taglock", "golem likes bread", "feed it after work",
                "knife that cuts deeper", "taglock is a lock of iron", "night thieves",
                "bright as your smile", "iron gnomes")) {
            return true;
        }
        if (containsAny(question, "sharpen soup")
                && !containsAny(answer, "soup", "spoon", "supper", "no")) {
            return true;
        }
        if (containsAny(question, "shadow")
                && !containsAny(answer, "shadow", "lamplight", "light")) {
            return true;
        }
        if (containsAny(question, "helmet for a cat", "cat helmet")
                && !containsAny(answer, "cat", "helmet", "iron", "weight")) {
            return true;
        }
        if (containsAny(question, "crows pay tax", "crows pay taxes")
                && containsAny(answer, "pay taxes", "tax collectors")) {
            return true;
        }
        if (containsAny(question, "demon", "contract", "pumpkin")
                && containsAny(answer, "it could be done", "i suppose it could be done")) {
            return true;
        }
        if (containsAny(question, "demon", "contract", "pumpkin")
                && !containsAny(answer, "demon", "contract", "pumpkin", "bargain", "debt")) {
            return true;
        }
        if (containsAny(question, "hide inside an anvil", "inside an anvil")
                && containsAny(answer, "it could be done", "i suppose it could be done")) {
            return true;
        }
        if (containsAny(question, "hide inside an anvil", "inside an anvil")
                && containsAny(answer, "hide", "inside")
                && !containsAny(answer, "door", "hinge", "metal", "anvils are for metal")) {
            return true;
        }
        if (containsAny(question, "can you teach me witchcraft", "teach me witchcraft")
                && containsAny(answer, "i can teach", "i will teach", "happy to show you", "i can show you")) {
            return true;
        }
        if (containsAny(question, "nether portal", "blaze rods", "elytra", "elytras", "stronghold", "locate")
                && containsAny(answer, "recipe", "craft", "obsidian", "fire resistance", "beacon",
                "i know the way", "i can locate", "i can show you the way")) {
            return true;
        }
        if (containsAny(question, "demon", "contract", "bargain")
                && containsAny(answer, "cheap", "discount", "emerald", "trust a demon")) {
            return true;
        }
        if (containsAny(question, "vampire")
                && !containsAny(answer, "vampire", "bed", "borrow", "invite", "invitation", "garlic")) {
            return true;
        }
        if (containsAny(question, "taglock", "left sock")
                && !containsAny(answer, "taglock", "sock", "touched", "laundry")) {
            return true;
        }
        if (containsAny(question, "stronghold")
                && !containsAny(answer, "stronghold", "old stone", "map", "nervous", "roads", "cannot")) {
            return true;
        }
        if (containsAny(question, "reflection")
                && containsAny(answer, "give them better iron", "make it worth their while")) {
            return true;
        }
        return false;
    }

    private static boolean weaponsmithReportRejects(String speech, String message) {
        String answer = normalizeForReport(speech);
        String question = normalizeForReport(message);
        if (answer.isBlank()) {
            return true;
        }
        if (containsAny(answer, "targeted fact", "rumor referral boundary", "as an ai", "i m death",
                "i am death", "i m the apothecary", "i am the apothecary", "i m a witch",
                "i am a witch", "marta ", "witches brew", "recipe starts", "special recipe",
                "100 cobblestone", "beautiful eyes", "good witch s hair", "i know where they go",
                "few miles from here", "fingers on the grindstone", "for your touch", "progression",
                "a blade is not a weapon", "on your map", "ask a weapon", "one iron ingot",
                "blowing your face off", "summoning detail", "garrick the weaponsmith", "he says",
                "nodding towards", "sneezed at", "for killing", "cutting meat", "cut meat",
                "sharpened by moonlight", "for dreams", "world of shadows", "not omnipresent",
                "moon is their enemy", "need daylight too", "break bones", "bane targets",
                "smite undead", "moonlit violence", "coughs in the moonlight", "fancy knife",
                "safe for bread", "no monsters will cross", "witches are fair folk",
                "good witch knows how to protect herself", "may your bread be thick", "sharpness is",
                "keep you warm in the dark", "night s chill", "wind knows secrets",
                "sharper than sharpness", "sharp as a moonbeam", "dark hands", "bad dreams come to life",
                "not for commoners", "coin ready", "silver hands", "iron gels", "gels are nearby", "wooden swords",
                "switching to a bow", "cows have slipped", "barrels of bread")) {
            return true;
        }
        if (containsAny(question, "live in this village", "do you live")
                && !containsAny(answer, "live", "village", "trade", "worry")) {
            return true;
        }
        if (containsAny(question, "what do you do")
                && (!containsAny(answer, "blade", "blades", "weapon", "weapons", "iron", "metal", "sharp")
                    || containsAny(answer, "field", "fields", "farmer"))) {
            return true;
        }
        if (containsAny(question, "weaponsmiths trade")
                && (!containsAny(answer, "iron")
                    || !containsAny(answer, "metal", "stock", "stand", "work")
                    || containsAny(answer, "crop", "crops", "bread is cheaper"))) {
            return true;
        }
        if (containsAny(question, "weapon should i carry", "carry on the road")
                && !containsAny(answer, "weapon", "blade", "sword", "shield", "steel", "road")) {
            return true;
        }
        if (containsAny(question, "repair this sword")
                && (!containsAny(answer, "repair", "mend", "sharpen", "grindstone")
                    || !containsAny(answer, "damage", "dull", "edge", "crack", "fresh iron", "heat",
                "patience"))) {
            return true;
        }
        if (containsAny(question, "shields help", "axes")
                && (!containsAny(answer, "shield")
                    || !containsAny(answer, "axe", "ribs"))) {
            return true;
        }
        if (containsAny(question, "blade enough", "night trouble")
                && (!containsAny(answer, "blade", "steel", "sword", "edge", "iron")
                    || !containsAny(answer, "night", "door", "trouble", "sharp", "shield", "road"))) {
            return true;
        }
        if (containsAny(question, "besides a sword")
                && !containsAny(answer, "bread", "garlic", "boots", "coin")) {
            return true;
        }
        if (containsAny(question, "dinner", "forge shuts")
                && (!containsAny(answer, "dinner", "supper", "bowl", "bowls", "forge", "food", "gossip")
                    || containsAny(answer, "barter with a witch", "coin"))) {
            return true;
        }
        if (containsAny(question, "food stored", "trouble lasts")
                && (!containsAny(answer, "store", "stores", "stored", "grain", "barrel", "pantry", "dry",
                "count bread", "hungry")
                    || containsAny(answer, "recipe", "no expert"))) {
            return true;
        }
        if (containsAny(question, "golem")
                && (!containsAny(answer, "golem", "iron")
                    || !containsAny(answer, "nearby", "one", "two", "no iron golem"))) {
            return true;
        }
        if (containsAny(question, "guards")
                && (!containsAny(answer, "guard", "guards")
                    || !containsAny(answer, "nearby", "one", "two"))) {
            return true;
        }
        if (containsAny(question, "road safe")
                && (!containsAny(answer, "road")
                    || !containsAny(answer, "safe", "walls", "fences", "door", "night", "trouble",
                "watch", "crows", "guards", "bells", "mud"))) {
            return true;
        }
        if (containsAny(question, "raid")
                && !containsAny(answer, "no raid", "raid active", "raider", "raiders", "pillager", "pillagers")) {
            return true;
        }
        if (containsAny(question, "vampire")
                && (!containsAny(answer, "vampire", "vampires")
                    || !containsAny(answer, "garlic", "sunset", "no invitations", "nearby"))) {
            return true;
        }
        if (containsAny(question, "werewolf", "werewolves")
                && (!containsAny(answer, "werewolf", "werewolves")
                    || !containsAny(answer, "moonrise", "silver", "nearby"))) {
            return true;
        }
        if (containsAny(question, "witches near")
                && (!containsAny(answer, "witch", "witches")
                    || !containsAny(answer, "hair", "bottle", "bottles", "name", "taglock"))) {
            return true;
        }
        if (containsAny(question, "good witches", "bad witches")
                && !containsAny(answer, "good", "bad", "fair", "clean", "hair", "bottle", "bottles")) {
            return true;
        }
        if (containsAny(question, "demon bargain")
                && (!containsAny(answer, "debt", "doors", "bell", "no bargaining", "step back",
                "count your wards")
                    || containsAny(answer, "cheap", "emerald", "discount"))) {
            return true;
        }
        if (containsAny(question, "teach me witchcraft")
                && !containsAny(answer, "primer", "proper witch", "witch with patience", "find a witch",
                "warnings")) {
            return true;
        }
        if (containsAny(question, "nether portal")
                && (!containsAny(answer, "miner", "book", "burns")
                    || containsAny(answer, "bonfire", "campfire", "special recipe", "obsidian", "hell"))) {
            return true;
        }
        if (containsAny(question, "blaze rods")
                && (!containsAny(answer, "blaze")
                    || !containsAny(answer, "nether", "trouble", "food")
                    || containsAny(answer, "craft", "obsidian", "firewood", "recipe", "charcoal",
                    "wood", "pages say", "one way to do it", "fire resistance", "progression"))) {
            return true;
        }
        if (containsAny(question, "elytra", "elytras")
                && (!containsAny(answer, "elytra", "end", "dragon")
                    || containsAny(answer, "recipe", "rocket", "iron golem", "make your own", "secret"))) {
            return true;
        }
        if (containsAny(question, "locate", "stronghold")
                && !containsAny(answer, "command", "cheat", "cheats", "cannot", "eyes", "maps", "roads")) {
            return true;
        }
        if (containsAny(question, "goodbye")
                && (!containsAny(answer, "go careful", "bread", "eyes open")
                    || containsAny(answer, "not too close", "bye bye"))) {
            return true;
        }
        return false;
    }

    private static String memoryFor(String longMemory, List<String> recent) {
        if (recent.isEmpty()) {
            return longMemory;
        }
        return longMemory + "\nRecent conversation:\n" + String.join("\n", recent);
    }

    private static void appendRecent(List<String> recent, String question, String speech) {
        recent.add(PLAYER_NAME + ": " + question);
        recent.add(VILLAGER_NAME + ": " + speech);
        while (recent.size() > 10) {
            recent.remove(0);
        }
    }

    private static void appendRecentNpcSpeech(List<String> recentNpcSpeeches, String speech) {
        recentNpcSpeeches.add(speech);
        while (recentNpcSpeeches.size() > 6) {
            recentNpcSpeeches.remove(0);
        }
    }

    private static boolean repeatsRecentNpcSpeech(String speech, List<String> recentNpcSpeeches) {
        for (String recent : recentNpcSpeeches) {
            if (NpcBrainResponse.sameMeaningForRepetition(speech, recent)) {
                return true;
            }
        }
        return false;
    }

    private static String buildReport(String model, List<Row> rows) {
        int total = rows.size();
        long fallbacks = rows.stream().filter(Row::fallbackUsed).count();
        long rawAccepted = total - fallbacks;
        long failures = rows.stream().filter(row -> row.raw().startsWith("ERROR:")).count();
        long meanMillis = Math.round(rows.stream().mapToLong(Row::elapsedMillis).average().orElse(0.0));
        long maxMillis = rows.stream().mapToLong(Row::elapsedMillis).max().orElse(0L);
        String questionSet = envOrDefault("WITCHERY_LLM_WEAPONSMITH_QUESTION_SET", "standard");
        String rawMode = forceFallbackOutput() ? "forced-fallback" : rawAcceptanceMode();
        boolean weird = "weird".equalsIgnoreCase(questionSet);

        StringBuilder report = new StringBuilder();
        report.append("# Witchery Weaponsmith NPC Ollama ")
                .append(weird ? "Weird Question" : "Conversation")
                .append(" Test - ").append(total).append(" Turns\n\n");
        report.append("Scene: Garrick the weaponsmith speaks to Dev in a Plains village, day 4");
        if (weird) {
            report.append(", while Dev asks deliberately strange questions");
        }
        report.append(". ");
        report.append("Adjusted output is what the game would show after polish/fallback.\n\n");
        report.append("## Summary\n\n");
        report.append("| Metric | Result |\n");
        report.append("| --- | ---: |\n");
        report.append("| Model | ").append(escapeCell(model)).append(" |\n");
        report.append("| Question set | ").append(escapeCell(questionSet)).append(" |\n");
        report.append("| Raw mode | ").append(escapeCell(rawMode)).append(" |\n");
        report.append("| Turns | ").append(total).append(" |\n");
        report.append("| Raw accepted | ").append(rawAccepted).append(" |\n");
        report.append("| Fallback used | ").append(fallbacks).append(" |\n");
        report.append("| Request/parse failures | ").append(failures).append(" |\n");
        report.append("| Mean response time | ").append(meanMillis).append("ms |\n");
        report.append("| Max response time | ").append(maxMillis).append("ms |\n\n");
        report.append("## Transcript\n\n");
        report.append("| # | Player | Raw | Raw speech | Adjusted Garrick | Fallback | Fallback text | ms |\n");
        report.append("| ---: | --- | --- | --- | --- | ---: | --- | ---: |\n");
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            report.append("| ").append(i + 1).append(" | ")
                    .append(escapeCell(row.question())).append(" | ")
                    .append(escapeCell(row.raw())).append(" | ")
                    .append(escapeCell(row.rawSpeech())).append(" | ")
                    .append(escapeCell(row.finalSpeech())).append(" | ")
                    .append(row.fallbackUsed() ? "yes" : "no").append(" | ")
                    .append(escapeCell(row.fallbackText())).append(" | ")
                    .append(row.elapsedMillis()).append(" |\n");
        }
        return report.toString();
    }

    private static void assertOllamaReachable(String endpoint) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "/api/tags"))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Ollama endpoint was not reachable: HTTP " + response.statusCode());
        }
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String normalizeForReport(String text) {
        return text == null ? "" : text.toLowerCase()
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

    private static Path reportPath(String configured) {
        Path path = Path.of(configured);
        if (path.isAbsolute()) {
            return path;
        }
        Path current = Path.of("").toAbsolutePath();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.exists(candidate.resolve("gradlew.bat")) && Files.exists(candidate.resolve("src/main"))) {
                return candidate.resolve(path);
            }
        }
        return current.resolve(path);
    }

    private static String escapeCell(String text) {
        return (text == null ? "" : text)
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", "")
                .replace("\n", "<br>");
    }

    private static String shortError(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private record FinalSpeech(String speech, boolean fallbackUsed, String fallbackText) {
    }

    private record ModelAttempt(NpcBrainResponse response, Throwable error) {
        String describe() {
            if (response == null) {
                return "ERROR: " + shortError(error);
            }
            return response.speech() + " / " + response.intent();
        }

        String speechOnly() {
            return response == null ? "" : response.speech();
        }
    }

    private record Row(String question, String raw, String rawSpeech, String finalSpeech,
                       boolean fallbackUsed, String fallbackText, long elapsedMillis) {
    }
}
