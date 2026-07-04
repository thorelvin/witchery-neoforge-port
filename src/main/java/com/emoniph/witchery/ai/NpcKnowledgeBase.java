package com.emoniph.witchery.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NpcKnowledgeBase {
    private static final String RESOURCE = "/data/witchery/ai/coven_witch_knowledge.md";
    private static final String CARD_INDEX_RESOURCE = "/data/witchery/ai/cards/index.txt";
    private static final String CARD_RESOURCE_DIR = "/data/witchery/ai/cards/";
    private static final String TARGETED_PREFIX = "- [triggers:";
    private static final String TARGETED_MARKER = "Targeted fact:";
    private static final Pattern CARD_METADATA = Pattern.compile("\\[([a-zA-Z0-9_]+):\\s*([^\\]]+)]");
    private static final int MAX_CONTEXT_LENGTH = 1400;
    private static final int MAX_RELEVANT_SECTIONS = 3;
    private static final int MAX_TARGETED_NOTES = 6;
    private static final List<String> CARD_PATHS = cardIndexPaths();
    private static final String MARKDOWN = loadMarkdown();
    private static final List<Section> SECTIONS = loadSections(MARKDOWN);
    private static final List<TargetedNote> TARGETED_NOTES = loadTargetedNotes();
    private static volatile KnowledgeDiagnostics lastDiagnostics =
            new KnowledgeDiagnostics(0, 0, 0, "none", "none");

    private NpcKnowledgeBase() {
    }

    public static String relevantContext(String playerMessage, String biome, String timeOfDay) {
        return relevantContext(playerMessage, biome, timeOfDay, NpcCardRequest.any());
    }

    public static String relevantContext(String playerMessage, String biome, String timeOfDay,
                                         NpcCardRequest cardRequest) {
        NpcCardRequest request = cardRequest == null ? NpcCardRequest.any() : cardRequest;
        String normalized = normalize(playerMessage);
        if (isFavoriteHerbQuestion(normalized)) {
            rememberDiagnostics("skipped:favorite_herb", "none");
            return "";
        }
        List<TargetedMatch> selectedNotes = selectedTargetedMatches(normalized, request);
        boolean personaCue = !NpcCardRequest.ANY_PERSONA.equals(request.personaId()) && !selectedNotes.isEmpty();
        if (!looksLikeKnowledgeQuestion(playerMessage) && !containsKnowledgeCue(normalized) && !personaCue) {
            rememberDiagnostics("skipped:no_knowledge_cue", "none");
            return "";
        }
        Set<String> terms = terms((playerMessage == null ? "" : playerMessage) + " " + biome + " " + timeOfDay);
        List<SectionScore> scored = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        selectedNotes.stream()
                .limit(Math.min(MAX_TARGETED_NOTES, request.maxTopics() * request.maxFactsPerTopic()))
                .forEach(match -> appendText(context, match.note().note()));
        for (Section section : SECTIONS) {
            int score = score(section, terms);
            if (score > 0) {
                scored.add(new SectionScore(section, score));
            }
        }
        List<SectionScore> selectedSections = scored.stream()
                .sorted(Comparator.comparingInt(SectionScore::score).reversed())
                .limit(MAX_RELEVANT_SECTIONS)
                .toList();
        selectedSections.forEach(result -> appendSection(context, result.section()));
        String text = context.toString().trim();
        rememberDiagnostics(knowledgeReason(selectedNotes, selectedSections, text),
                knowledgeSources(selectedNotes, selectedSections));
        return text.isBlank() ? "" : NpcBrainResponse.sanitize(text, MAX_CONTEXT_LENGTH);
    }

    public static Optional<String> compactAnswer(String playerMessage, String biome, String timeOfDay) {
        String normalized = normalize(playerMessage);
        if (isFavoriteHerbQuestion(normalized)) {
            return Optional.empty();
        }
        Set<String> queryTerms = terms(playerMessage == null ? "" : playerMessage);
        if (queryTerms.isEmpty() || (!looksLikeKnowledgeQuestion(playerMessage) && !containsKnowledgeCue(normalized))) {
            return Optional.empty();
        }
        Optional<TargetedMatch> targeted = targetedMatches(normalized).stream().findFirst();
        if (targeted.isPresent()) {
            return Optional.of(NpcBrainResponse.sanitize(targeted.get().note().spokenFact(), 180));
        }
        Set<String> contextTerms = terms(playerMessage + " " + biome + " " + timeOfDay);
        return SECTIONS.stream()
                .map(section -> answerScore(section, contextTerms, queryTerms))
                .filter(result -> result.score() > 0)
                .max(Comparator.comparingInt(AnswerScore::score))
                .map(AnswerScore::sentence)
                .filter(answer -> !answer.isBlank())
                .map(answer -> NpcBrainResponse.sanitize(answer, 180));
    }

    static List<Section> sections() {
        return SECTIONS;
    }

    public static String diagnosticsLine() {
        KnowledgeDiagnostics diagnostics = diagnostics();
        return "sectionsLoaded=" + diagnostics.sectionsLoaded()
                + ", cardsLoaded=" + diagnostics.cardsLoaded()
                + ", targetedNotesLoaded=" + diagnostics.targetedNotesLoaded()
                + ", lastKnowledgeReason=" + diagnostics.lastReason()
                + ", lastCards=" + diagnostics.lastCards();
    }

    static KnowledgeDiagnostics diagnostics() {
        KnowledgeDiagnostics previous = lastDiagnostics;
        return new KnowledgeDiagnostics(SECTIONS.size(), CARD_PATHS.size(), TARGETED_NOTES.size(),
                previous.lastReason(), previous.lastCards());
    }

    private static void rememberDiagnostics(String reason, String cards) {
        lastDiagnostics = new KnowledgeDiagnostics(SECTIONS.size(), CARD_PATHS.size(), TARGETED_NOTES.size(),
                NpcBrainResponse.sanitize(reason, 120),
                NpcBrainResponse.sanitize(cards, 180));
    }

    private static void appendSection(StringBuilder context, Section section) {
        appendText(context, section.title() + ": " + section.body());
    }

    private static void appendText(StringBuilder context, String text) {
        if (!context.isEmpty()) {
            context.append('\n');
        }
        context.append(text);
    }

    private static String knowledgeReason(List<TargetedMatch> selectedNotes, List<SectionScore> selectedSections,
                                          String context) {
        if (!selectedNotes.isEmpty()) {
            return "targeted_match:" + joinedTopics(selectedNotes);
        }
        if (!selectedSections.isEmpty()) {
            return "section_match:" + selectedSections.stream()
                    .map(match -> normalizeTopic(match.section().title()))
                    .distinct()
                    .limit(3)
                    .reduce((left, right) -> left + "," + right)
                    .orElse("unknown");
        }
        return context.isBlank() ? "no_match" : "context";
    }

    private static String knowledgeSources(List<TargetedMatch> selectedNotes, List<SectionScore> selectedSections) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (TargetedMatch match : selectedNotes) {
            String source = match.note().sourceAlias().isBlank() ? match.note().topic() : match.note().sourceAlias();
            if (!source.isBlank()) {
                names.add(normalizeTopic(source));
            }
        }
        for (SectionScore match : selectedSections) {
            names.add(normalizeTopic(match.section().title()));
        }
        if (names.isEmpty()) {
            return "none";
        }
        return String.join(",", names.stream().limit(6).toList());
    }

    private static String joinedTopics(List<TargetedMatch> selectedNotes) {
        return selectedNotes.stream()
                .map(match -> match.note().topic())
                .filter(topic -> !topic.isBlank())
                .distinct()
                .limit(4)
                .reduce((left, right) -> left + "," + right)
                .orElse("unknown");
    }

    private static int score(Section section, Set<String> terms) {
        int score = 0;
        String haystack = section.searchText();
        for (String term : terms) {
            if (term.length() >= 3 && containsTerm(haystack, term)) {
                score += containsTerm(section.titleLower(), term) ? 3 : 1;
            }
        }
        return score;
    }

    private static AnswerScore answerScore(Section section, Set<String> contextTerms, Set<String> queryTerms) {
        int sectionScore = score(section, contextTerms);
        String sentence = bestSentence(section, queryTerms);
        int sentenceScore = sentenceScore(sentence, queryTerms);
        return new AnswerScore(sentence, sectionScore + (sentenceScore * 5));
    }

    private static String bestSentence(Section section, Set<String> queryTerms) {
        String best = "";
        int bestScore = 0;
        for (String sentence : section.body().split("(?<=[.!?])\\s+")) {
            String cleaned = sentence.strip();
            if (cleaned.isBlank()) {
                continue;
            }
            int score = sentenceScore(cleaned, queryTerms);
            if (score > bestScore) {
                best = cleaned;
                bestScore = score;
            }
        }
        if (bestScore > 0) {
            return best;
        }
        return section.body().split("(?<=[.!?])\\s+", 2)[0].strip();
    }

    private static int sentenceScore(String sentence, Set<String> queryTerms) {
        int score = 0;
        String haystack = sentence.toLowerCase(Locale.ROOT);
        for (String term : queryTerms) {
            if (term.length() >= 3 && containsTerm(haystack, term)) {
                score++;
                if (haystack.startsWith(term + ":") || haystack.startsWith(term + " ")) {
                    score += 4;
                }
            }
        }
        return score;
    }

    private static List<TargetedMatch> targetedMatches(String normalized) {
        return TARGETED_NOTES.stream()
                .filter(note -> note.matchesPersona(NpcCardRequest.ANY_PERSONA))
                .map(note -> new TargetedMatch(note, note.matchScore(normalized)))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingInt(TargetedMatch::score).reversed())
                .toList();
    }

    private static List<TargetedMatch> selectedTargetedMatches(String normalized, NpcCardRequest request) {
        List<TargetedMatch> matches = TARGETED_NOTES.stream()
                .filter(note -> note.matchesPersona(request.personaId()))
                .map(note -> new TargetedMatch(note, note.matchScore(normalized)))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingInt(TargetedMatch::score).reversed())
                .toList();
        if (matches.isEmpty()) {
            return List.of();
        }
        Map<String, List<TargetedMatch>> byTopic = new LinkedHashMap<>();
        for (TargetedMatch match : matches) {
            byTopic.computeIfAbsent(match.note().topic(), ignored -> new ArrayList<>()).add(match);
        }
        List<TargetedMatch> selected = new ArrayList<>();
        int topics = 0;
        for (Map.Entry<String, List<TargetedMatch>> entry : byTopic.entrySet()) {
            if (topics >= request.maxTopics()) {
                break;
            }
            List<TargetedMatch> variants = new ArrayList<>(entry.getValue());
            int picks = Math.min(request.maxFactsPerTopic(), variants.size());
            for (int pick = 0; pick < picks; pick++) {
                TargetedMatch chosen = weightedVariant(entry.getKey(), variants, request, pick);
                selected.add(chosen);
                variants.remove(chosen);
            }
            topics++;
        }
        return selected;
    }

    private static TargetedMatch weightedVariant(String topic, List<TargetedMatch> variants,
                                                 NpcCardRequest request, int pickIndex) {
        if (pickIndex == 0 && variants.size() > 1) {
            int bestScore = variants.get(0).score();
            int secondBest = variants.stream().skip(1)
                    .mapToInt(TargetedMatch::score)
                    .max()
                    .orElse(0);
            if (bestScore >= secondBest + 8) {
                return variants.get(0);
            }
        }
        int totalWeight = variants.stream()
                .mapToInt(match -> Math.max(1, match.note().weight()))
                .sum();
        int slot = Math.floorMod(Objects.hash(topic, request.entityId(), request.playerId(),
                request.conversationTurn(), pickIndex), totalWeight);
        int cursor = 0;
        for (TargetedMatch variant : variants) {
            cursor += Math.max(1, variant.note().weight());
            if (slot < cursor) {
                return variant;
            }
        }
        return variants.get(0);
    }

    private static Set<String> terms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        for (String term : text.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+")) {
            if (term.length() >= 3) {
                terms.add(term);
            }
        }
        return terms;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean looksLikeKnowledgeQuestion(String text) {
        String normalized = normalize(text);
        if (normalized.contains("remember")
                || normalized.contains("know about me")
                || isFavoriteHerbQuestion(normalized)
                || normalized.startsWith("what are you doing")
                || normalized.startsWith("what you doing")
                || normalized.startsWith("how are you")
                || normalized.startsWith("are you well")) {
            return false;
        }
        return normalized.startsWith("what is ")
                || normalized.startsWith("what are ")
                || normalized.startsWith("what does ")
                || normalized.startsWith("what do ")
                || normalized.startsWith("how do ")
                || normalized.startsWith("how does ")
                || normalized.startsWith("where do ")
                || normalized.startsWith("where is ")
                || normalized.startsWith("what scares ")
                || normalized.startsWith("tell me about ")
                || normalized.startsWith("explain ")
                || looksLikeBrewQuestion(normalized)
                || ((normalized.startsWith("do you know ")
                        || normalized.startsWith("can you ")
                        || normalized.startsWith("can i ")
                        || normalized.startsWith("what can ")
                        || normalized.startsWith("what should ")
                        || normalized.startsWith("why did ")
                        || normalized.startsWith("why do ")
                        || normalized.startsWith("why does ")
                        || normalized.startsWith("why is ")
                        || normalized.startsWith("is the ")
                        || normalized.startsWith("is it ")
                        || normalized.startsWith("should i ")
                        || normalized.startsWith("do you have "))
                    && containsKnowledgeCue(normalized))
                || normalized.contains(" for")
                || normalized.contains(" use");
    }

    private static boolean looksLikeBrewQuestion(String normalized) {
        return containsAny(normalized, "brew", "brewing", "potion", "flowing spirit")
                && containsAny(normalized, "sleeping", "revealing", "poison", "cleansing", "sprouting",
                "flowing spirit", "what", "how", "know", "can you", "do you", "want", "need", "teach", "learn");
    }

    private static boolean containsKnowledgeCue(String normalized) {
        return containsSocialOrTradeCue(normalized)
                || containsBrewMachineOrRecipeCue(normalized)
                || containsHerbOrReagentCue(normalized)
                || containsRitePoppetOrCurseCue(normalized)
                || containsSpiritCreatureOrWorldCue(normalized)
                || containsToolBookOrStructureCue(normalized)
                || containsTransformationInfusionOrSymbolCue(normalized);
    }

    private static boolean containsSocialOrTradeCue(String normalized) {
        return containsAnyTerm(normalized,
                "coven", "covens", "recruit", "recruiting", "trial", "quest",
                "trade", "trades", "trading", "wares", "stock", "buy", "sell", "coin", "coins",
                "emerald", "emeralds", "gift", "bless", "blessing", "luck", "apology", "insult",
                "thank", "thanks", "praise", "sorry", "forgive", "rude", "stupid", "hag",
                "egg", "eggs", "potato", "potatoes",
                "witchery", "witchcraft", "rumor", "rumour", "gossip", "omen", "omens", "fortune",
                "prophecy", "teach me", "lesson", "next step", "what next", "what now", "build first",
                "where do i start", "start witchery", "start witchcraft");
    }

    private static boolean containsBrewMachineOrRecipeCue(String normalized) {
        return containsAnyTerm(normalized,
                "brew", "brewing", "potion", "altar", "oven", "kettle", "cauldron",
                "witch cauldron", "witch s cauldron", "witches cauldron", "anointing paste",
                "wake cauldron", "heated cauldron", "cauldron working", "cauldron mutandis",
                "cauldron mutandis extremis", "cauldron drop of luck", "distillery",
                "spinning wheel", "brazier", "brazier conjuration", "light brazier",
                "ignite brazier", "clear brazier", "empty brazier", "weak smoke", "brazier smoke",
                "brazier fizz", "brazier fizzing", "fizzing brazier", "fizz", "flint and steel",
                "redstone soup", "flying ointment", "mystic unguent", "drop of luck",
                "brew of bats", "brew of erosion", "brew of webs", "brew of ice",
                "brew of vines", "brew of thorns", "combustion brew", "brew of the depths",
                "brew of depths", "brew of raising", "brew of substitution", "brew modifiers",
                "modifier", "modifiers", "splash brew", "throwable brew", "gas brew", "dispersal",
                "poison", "blight", "fume", "fumes", "clay jar", "jar", "machine", "machines",
                "station", "stations", "recipe page", "recipe book", "opened recipe", "opened page",
                "readable page", "crafting page", "crafting recipe");
    }

    private static boolean containsHerbOrReagentCue(String normalized) {
        return containsAnyTerm(normalized,
                "mandrake", "belladonna", "witches salt", "witch salt", "flowing spirit",
                "herb", "herbs", "plant", "plants", "rowan", "wormwood", "snowbell",
                "wolfsbane", "alder", "hawthorn", "rowan berries", "belladonna seeds",
                "mandrake seeds", "snowbell seeds", "wolfsbane seeds", "water artichoke",
                "artichoke globe", "glint weed", "spanish moss", "ember moss", "blood poppy",
                "blood poppies", "mutating sprig", "mutator", "grassper", "grasspers",
                "wispy cotton", "disturbed cotton", "treefyd seeds", "wood ash", "ent twig",
                "quicklime", "graveyard dust", "refined evil", "condensed fear", "focused will",
                "ender dew", "infernal blood", "diamond vapour", "diamond vapor", "oil of vitriol",
                "tear of the goddess", "tear of goddess", "glowstone dust",
                "netherrack infernal blood", "fanciful thread", "golden thread",
                "tormented twine", "impregnated leather", "fume funnel", "fume filter",
                "filtered fume funnel", "hay bale golden thread", "threadwork", "woven", "cloth",
                "whiff", "whif", "whiff of magic", "whif of magic", "mutandis",
                "mutandis extremis", "attuned stone", "exhale", "exhale of the horned one",
                "breath of the goddess", "hint of rebirth", "reek of misfortune",
                "odour of purity", "odor of purity", "odour", "odor", "reek", "foul fume",
                "witches salt recipe", "witch salt recipe", "sapling", "saplings");
    }

    private static boolean containsRitePoppetOrCurseCue(String normalized) {
        return containsAnyTerm(normalized,
                "taglock", "poppet", "bone needle", "death protection", "fire protection",
                "water protection", "earth protection", "hunger protection", "tool protection",
                "armor protection", "armour protection", "poppet protection", "voodoo poppet",
                "vampiric poppet", "circle", "rite", "ritual", "summon", "summoned",
                "summoning", "chalice", "hidden", "invisible", "golden chalk", "ritual chalk",
                "infernal chalk", "otherwhere chalk", "chalk color", "chalk colour",
                "chalk colors", "chalk colours", "center glyph", "circle size", "circle shape",
                "small circle", "medium circle", "large circle", "circle talisman", "pit dirt",
                "pit grass", "false floor", "curse", "curses", "cursed", "remove curse",
                "ward", "wards", "voodoo", "protection", "curse of misfortune",
                "misfortune curse", "curse of sinking", "sinking curse", "curse of overheating",
                "overheating curse", "curse of insanity", "insanity curse", "curse creature",
                "creature curse", "waking nightmare curse", "curse of waking nightmare",
                "corrupt poppet", "hell on earth", "curse hell on earth", "protective poppet",
                "curse blocked", "curse failed", "witch hunter curse protection", "am i cursed",
                "sanctity", "transposition", "manifestation", "prior incarnation",
                "beastial call", "sky wrath", "sky s wrath", "lightning", "barrier",
                "force field", "banish", "anguish of the dead", "fortification of the corpse",
                "deathly veil", "drain growth", "graveyard mist", "chalk", "poppet shelf");
    }

    private static boolean containsSpiritCreatureOrWorldCue(String normalized) {
        return containsAnyTerm(normalized,
                "demon", "demons", "imp", "flame imp", "familiar", "familiars",
                "spirit", "spirits", "spirit world", "dream", "dreams", "nightmare",
                "spectre", "spectres", "banshee", "banshees", "poltergeist", "poltergeists",
                "cat", "owl", "toad", "bat wool", "wool of bat", "tongue of dog",
                "toe of frog", "owlet wing", "owlet s wing", "creeper heart", "spectral dust",
                "hellhound head", "creature drops", "rare drops", "polynesia charm",
                "animal trade", "animal trades", "devil s tongue charm", "devils tongue charm",
                "demonic contract", "concentrated bat ball", "frozen heart",
                "brew of frogs tongue", "wormy apple", "dream catcher", "dreamcatcher",
                "dream weaver", "dream weavers", "dream weave", "fleet foot", "iron arm",
                "fasting", "nightmares weave", "intensity weave", "hobgoblin", "hobgoblins",
                "koboldite", "ent", "ents", "treefyd", "treefyds", "flying monkey",
                "flying monkeys");
    }

    private static boolean containsToolBookOrStructureCue(String normalized) {
        return containsAnyTerm(normalized,
                "arthana", "well", "whisper", "whispers", "scry", "scrying", "crystal ball",
                "seer stone", "player compass", "compass", "waystone", "waystones",
                "bound waystone", "blooded waystone", "boline", "brew bag", "witches hat",
                "witch hat", "witches robes", "witch robes", "candelabra", "filled chalice",
                "pentacle", "koboldite pentacle", "leech chest", "blood rose", "critter snare",
                "garlic garland", "leonard s urn", "leonards urn", "book", "books",
                "lesson book", "circle magic book", "book of circle magic", "book of poppets",
                "book of spirit world", "book of herbology", "book of the oven",
                "book of distilling", "book of creatures", "book of transformations",
                "book of infusions", "book of wands", "unlock", "unlocked",
                "shelf compass", "poppet shelf compass", "fanciful thinking", "apothecary",
                "village guard", "village guards", "guards shouting", "guards angry",
                "hunter clothes", "hunter clothing", "hunter armor", "hunter armour",
                "hobgoblin trade", "hobgoblin trades", "koboldite trade", "hobgoblin hut",
                "hobgoblin patron", "statue of hobgoblin patron", "patron statue",
                "koboldite pickaxe", "twisting band", "village walls", "walled village",
                "witch hunter", "witch hunters");
    }

    private static boolean containsTransformationInfusionOrSymbolCue(String normalized) {
        return containsAnyTerm(normalized,
                "baba yaga", "death", "binky", "lilith", "leonard", "goat", "hooves", "horns",
                "satyr", "sabbath", "torment", "garlic", "vampire", "vampires", "storm",
                "weather", "raining", "werewolf", "werewolves", "lycanthropy", "wolf curse",
                "full moon", "wolf altar", "transformation", "transformations", "infusion",
                "infusions", "infernal animus", "coffin", "stake", "glass goblet",
                "vampire goblet", "blood stained wool", "woven cruor", "mesmerise",
                "mesmerize", "wolftrap", "wolf trap", "moon charm", "silver sword",
                "silver bolt", "silvered armor", "silvered armour", "vampire book",
                "observations of an immortal", "ghost of the light", "soul of the world",
                "spirit of otherwhere", "lasting infusion", "permanent infusion",
                "rite of infusion", "infusion recharge", "light infusion", "overworld infusion",
                "otherwhere infusion", "infernal infusion", "ghost of light", "soul of world",
                "spirit otherwhere", "choose infusion", "symbology", "mystic branch",
                "air glyph", "branch spell", "symbol spell", "draw symbol", "draw glyph",
                "unknown symbol", "branch fizzles", "infused energy", "infusion energy",
                "demon soul glyph", "learn symbol", "glyph", "glyphs", "ignianima",
                "carnosa diem", "tormentum", "morsmordre", "crucio", "imperio",
                "avada kedavra", "accio", "episkey", "expelliarmus", "flipendo",
                "impedimenta", "incendio", "lumos", "nox", "stupefy", "aguamenti",
                "alohomora", "defodio", "ennervate", "attraho", "cave inimicum",
                "meteolojinx", "protego", "flagrate", "colloportus", "caelum",
                "baratrum", "ortus", "occasus", "witch hand", "witches hand",
                "creature power", "creature powers", "altar kill power", "silver damage",
                "silver hurts werewolves", "silver hurts vampires", "silver against werewolves",
                "silver against vampires", "hunter curse protection");
    }

    private static boolean isFavoriteHerbQuestion(String normalized) {
        return (normalized.contains("favorite") || normalized.contains("favourite"))
                && (normalized.contains("herb") || normalized.contains("plant"));
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyTerm(String normalized, String... terms) {
        for (String term : terms) {
            if (containsTerm(normalized, term)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsTerm(String normalized, String term) {
        if (term.indexOf(' ') >= 0) {
            return normalized.contains(term) || stripArticles(normalized).contains(stripArticles(term));
        }
        String suffix = term.endsWith("s") ? "" : "s?";
        return normalized.matches(".*\\b" + java.util.regex.Pattern.quote(term) + suffix + "\\b.*");
    }

    private static String stripArticles(String normalized) {
        return normalized.replaceAll("\\b(a|an|the)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static List<Section> loadSections(String markdown) {
        List<Section> sections = parseSections(markdown);
        return sections.isEmpty() ? fallbackSections() : List.copyOf(sections);
    }

    private static List<TargetedNote> loadTargetedNotes() {
        List<TargetedNote> notes = new ArrayList<>(parseTargetedNotes(loadResource(RESOURCE), ""));
        for (String cardPath : CARD_PATHS) {
            String resource = cardPath.startsWith("/") ? cardPath : CARD_RESOURCE_DIR + cardPath;
            notes.addAll(parseTargetedNotes(loadResource(resource), sourceAlias(cardPath)));
        }
        return List.copyOf(notes);
    }

    private static String loadMarkdown() {
        StringBuilder markdown = new StringBuilder();
        appendMarkdown(markdown, loadResource(RESOURCE));
        for (String line : CARD_PATHS) {
            String resource = line.startsWith("/") ? line : CARD_RESOURCE_DIR + line;
            appendMarkdown(markdown, loadResource(resource));
        }
        return markdown.toString();
    }

    private static List<String> cardIndexPaths() {
        List<String> paths = new ArrayList<>();
        String index = loadResource(CARD_INDEX_RESOURCE);
        for (String rawLine : index.split("\\R")) {
            String line = rawLine.strip();
            if (!line.isBlank() && !line.startsWith("#")) {
                paths.add(line);
            }
        }
        return paths;
    }

    private static String sourceAlias(String cardPath) {
        String name = cardPath == null ? "" : cardPath;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.replaceFirst("\\.md$", "").replace('_', ' ');
    }

    private static void appendMarkdown(StringBuilder markdown, String text) {
        if (text.isBlank()) {
            return;
        }
        if (!markdown.isEmpty()) {
            markdown.append("\n\n");
        }
        markdown.append(text);
    }

    private static String loadResource(String resource) {
        try (InputStream stream = NpcKnowledgeBase.class.getResourceAsStream(resource)) {
            if (stream == null) {
                return "";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private static List<Section> parseSections(String markdown) {
        List<Section> sections = new ArrayList<>();
        String title = "";
        boolean always = false;
        StringBuilder body = new StringBuilder();
        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine.strip();
            if (line.startsWith("## ")) {
                addSection(sections, title, always, body);
                title = line.substring(3).replace("[always]", "").strip();
                always = line.contains("[always]");
                body.setLength(0);
            } else if (!title.isBlank() && !line.isBlank() && !line.startsWith("#")) {
                if (line.toLowerCase(Locale.ROOT).startsWith(TARGETED_PREFIX)) {
                    continue;
                }
                if (!body.isEmpty()) {
                    body.append(' ');
                }
                body.append(line.replaceFirst("^-\\s*", ""));
            }
        }
        addSection(sections, title, always, body);
        return sections;
    }

    private static List<TargetedNote> parseTargetedNotes(String markdown, String sourceAlias) {
        List<TargetedNote> notes = new ArrayList<>();
        String sectionTitle = "";
        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine.strip();
            if (line.startsWith("## ")) {
                sectionTitle = line.substring(3).strip();
                continue;
            }
            if (!line.toLowerCase(Locale.ROOT).startsWith(TARGETED_PREFIX)) {
                continue;
            }
            int end = line.indexOf(']');
            if (end <= TARGETED_PREFIX.length()) {
                continue;
            }
            String triggers = line.substring(TARGETED_PREFIX.length(), end).strip();
            int factStart = line.indexOf(TARGETED_MARKER, end + 1);
            if (factStart < 0) {
                continue;
            }
            String metadata = line.substring(end + 1, factStart).strip();
            String note = line.substring(factStart).strip();
            if (!triggers.isBlank() && !note.isBlank()) {
                notes.add(new TargetedNote(
                        triggers,
                        note,
                        cardMetadata(metadata, "topic").map(NpcKnowledgeBase::normalizeTopic)
                                .orElse(defaultTopic(triggers)),
                        cardMetadata(metadata, "persona").map(NpcCardRequest::normalizePersona)
                                .orElse(NpcCardRequest.ANY_PERSONA),
                        cardMetadata(metadata, "weight").map(NpcKnowledgeBase::parseWeight).orElse(1),
                        cardMetadata(metadata, "tags").orElse(""),
                        sectionTitle,
                        sourceAlias));
            }
        }
        return notes;
    }

    private static Optional<String> cardMetadata(String metadata, String key) {
        Matcher matcher = CARD_METADATA.matcher(metadata);
        while (matcher.find()) {
            if (matcher.group(1).equalsIgnoreCase(key)) {
                String value = matcher.group(2).strip();
                return value.isBlank() ? Optional.empty() : Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private static int parseWeight(String value) {
        try {
            return Math.max(1, Math.min(20, Integer.parseInt(value.strip())));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static String defaultTopic(String triggers) {
        for (String trigger : triggers.split("\\|")) {
            String normalized = normalize(trigger);
            if (!normalized.isBlank()) {
                return normalizeTopic(normalized);
            }
        }
        return "general";
    }

    private static String normalizeTopic(String topic) {
        String normalized = topic.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return normalized.isBlank() ? "general" : normalized;
    }

    private static void addSection(List<Section> sections, String title, boolean always, StringBuilder body) {
        String cleaned = body.toString().strip();
        if (!title.isBlank() && !cleaned.isBlank()) {
            sections.add(new Section(title, cleaned, always));
        }
    }

    private static List<Section> fallbackSections() {
        return List.of(
                new Section("Witchery basics", "Witchery is practical magic: brews, circles, poppets, spirits, covens, curses, and bargains.", true),
                new Section("Coven witches", "Coven witches are neutral potion merchants who may join a player's coven after a small quest.", true)
        );
    }

    record Section(String title, String body, boolean always) {
        String titleLower() {
            return title.toLowerCase(Locale.ROOT);
        }

        String searchText() {
            return (title + " " + body).toLowerCase(Locale.ROOT);
        }
    }

    record KnowledgeDiagnostics(int sectionsLoaded, int cardsLoaded, int targetedNotesLoaded,
                                String lastReason, String lastCards) {
    }

    private record SectionScore(Section section, int score) {
    }

    private record AnswerScore(String sentence, int score) {
    }

    private record TargetedMatch(TargetedNote note, int score) {
    }

    private record TargetedNote(String triggers, String note, String topic, String personaId, int weight,
                                String tags, String sectionTitle, String sourceAlias) {
        boolean matches(String normalized) {
            return matchScore(normalized) > 0;
        }

        boolean matchesPersona(String requestedPersona) {
            String requested = NpcCardRequest.normalizePersona(requestedPersona);
            if (NpcCardRequest.ANY_PERSONA.equals(requested)) {
                return NpcCardRequest.ANY_PERSONA.equals(personaId);
            }
            return NpcCardRequest.ANY_PERSONA.equals(personaId) || personaId.equals(requested);
        }

        int matchScore(String normalized) {
            int best = 0;
            for (String trigger : triggers.split("\\|")) {
                String cleaned = normalize(trigger.strip());
                if (!cleaned.isBlank() && containsTerm(normalized, cleaned)) {
                    int words = terms(cleaned).size();
                    int score = Math.max(1, words * 10 + cleaned.length() / 8);
                    if (normalized.equals(cleaned)) {
                        score += 100;
                    } else if (normalized.contains(cleaned)) {
                        score += 5;
                    }
                    best = Math.max(best, score);
                }
            }
            best = Math.max(best, aliasMatchScore(normalized, topic, 8));
            best = Math.max(best, aliasMatchScore(normalized, tags, 6));
            best = Math.max(best, aliasMatchScore(normalized, sectionTitle, 4));
            best = Math.max(best, aliasMatchScore(normalized, sourceAlias, 3));
            return best;
        }

        private static int aliasMatchScore(String normalized, String aliases, int baseScore) {
            int best = 0;
            for (String alias : aliases.split("[,|]")) {
                String cleaned = normalize(alias.strip());
                if (cleaned.length() < 5 || isWeakAlias(cleaned)) {
                    continue;
                }
                if (containsTerm(normalized, cleaned)) {
                    best = Math.max(best, baseScore + terms(cleaned).size() * 3);
                }
            }
            return best;
        }

        private static boolean isWeakAlias(String cleaned) {
            return cleaned.equals("item")
                    || cleaned.equals("tool")
                    || cleaned.equals("boss")
                    || cleaned.equals("brew")
                    || cleaned.equals("recipe")
                    || cleaned.equals("structure")
                    || cleaned.equals("worldgen");
        }

        String spokenFact() {
            return note.replaceFirst("^Targeted fact:\\s*", "");
        }
    }
}
