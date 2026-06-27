package com.emoniph.witchery.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class NpcKnowledgeBase {
    private static final String RESOURCE = "/data/witchery/ai/coven_witch_knowledge.md";
    private static final String CARD_INDEX_RESOURCE = "/data/witchery/ai/cards/index.txt";
    private static final String CARD_RESOURCE_DIR = "/data/witchery/ai/cards/";
    private static final String TARGETED_PREFIX = "- [triggers:";
    private static final int MAX_CONTEXT_LENGTH = 1400;
    private static final int MAX_RELEVANT_SECTIONS = 3;
    private static final int MAX_TARGETED_NOTES = 6;
    private static final String MARKDOWN = loadMarkdown();
    private static final List<Section> SECTIONS = loadSections(MARKDOWN);
    private static final List<TargetedNote> TARGETED_NOTES = loadTargetedNotes(MARKDOWN);

    private NpcKnowledgeBase() {
    }

    public static String relevantContext(String playerMessage, String biome, String timeOfDay) {
        String normalized = normalize(playerMessage);
        if (isFavoriteHerbQuestion(normalized)) {
            return "";
        }
        if (!looksLikeKnowledgeQuestion(playerMessage) && !containsKnowledgeCue(normalized)) {
            return "";
        }
        Set<String> terms = terms((playerMessage == null ? "" : playerMessage) + " " + biome + " " + timeOfDay);
        List<SectionScore> scored = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        targetedMatches(normalized).stream()
                .limit(MAX_TARGETED_NOTES)
                .forEach(match -> appendText(context, match.note().note()));
        for (Section section : SECTIONS) {
            int score = score(section, terms);
            if (score > 0) {
                scored.add(new SectionScore(section, score));
            }
        }
        scored.stream()
                .sorted(Comparator.comparingInt(SectionScore::score).reversed())
                .limit(MAX_RELEVANT_SECTIONS)
                .forEach(result -> appendSection(context, result.section()));
        String text = context.toString().trim();
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

    private static void appendSection(StringBuilder context, Section section) {
        appendText(context, section.title() + ": " + section.body());
    }

    private static void appendText(StringBuilder context, String text) {
        if (!context.isEmpty()) {
            context.append('\n');
        }
        context.append(text);
    }

    private static int score(Section section, Set<String> terms) {
        int score = 0;
        String haystack = section.searchText();
        for (String term : terms) {
            if (term.length() >= 3 && haystack.contains(term)) {
                score += section.titleLower().contains(term) ? 3 : 1;
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
            if (term.length() >= 3 && haystack.contains(term)) {
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
                .map(note -> new TargetedMatch(note, note.matchScore(normalized)))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingInt(TargetedMatch::score).reversed())
                .toList();
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
        return containsAnyTerm(normalized,
                "brew", "brewing", "potion", "altar", "taglock", "poppet", "mandrake",
                "belladonna", "demon", "familiar", "witches salt", "witch salt", "flowing spirit",
                "spirit world", "nightmare", "arthana", "oven", "brazier", "circle", "rite", "ritual", "summon",
                "summoning", "chalice", "hidden", "invisible", "herb", "herbs", "plant", "plants",
                "rowan", "wormwood", "snowbell", "wolfsbane", "cat", "owl", "toad", "witchcraft",
                "witchery", "rumor", "rumour", "gossip", "baba yaga", "death", "poison", "blight",
                "chalk", "garlic", "vampire", "vampires", "storm", "weather", "raining",
                "fume", "fumes", "clay jar", "jar", "sapling", "saplings", "dream catcher", "dreamcatcher",
                "well", "whisper", "whispers", "whiff", "whif", "whiff of magic", "whif of magic",
                "mutandis", "mutandis extremis", "attuned stone", "exhale", "exhale of the horned one",
                "breath of the goddess", "hint of rebirth", "reek of misfortune", "odour of purity",
                "odor of purity", "odour", "odor", "reek", "foul fume", "witches salt recipe", "witch salt recipe",
                "machine", "machines", "station", "stations", "recipe page", "recipe book", "opened recipe",
                "opened page", "readable page", "unlock", "unlocked", "crafting page", "crafting recipe",
                "teach me", "lesson", "next step", "what next", "what now", "build first",
                "where do i start", "start witchery", "start witchcraft",
                "diamond vapour", "diamond vapor", "oil of vitriol", "tear of the goddess",
                "fanciful thread", "golden thread", "tormented twine");
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

    private static List<TargetedNote> loadTargetedNotes(String markdown) {
        List<TargetedNote> notes = parseTargetedNotes(markdown);
        return List.copyOf(notes);
    }

    private static String loadMarkdown() {
        StringBuilder markdown = new StringBuilder();
        appendMarkdown(markdown, loadResource(RESOURCE));
        String index = loadResource(CARD_INDEX_RESOURCE);
        for (String rawLine : index.split("\\R")) {
            String line = rawLine.strip();
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String resource = line.startsWith("/") ? line : CARD_RESOURCE_DIR + line;
            appendMarkdown(markdown, loadResource(resource));
        }
        return markdown.toString();
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

    private static List<TargetedNote> parseTargetedNotes(String markdown) {
        List<TargetedNote> notes = new ArrayList<>();
        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine.strip();
            if (!line.toLowerCase(Locale.ROOT).startsWith(TARGETED_PREFIX)) {
                continue;
            }
            int end = line.indexOf(']');
            if (end <= TARGETED_PREFIX.length()) {
                continue;
            }
            String triggers = line.substring(TARGETED_PREFIX.length(), end).strip();
            String note = line.substring(end + 1).strip();
            if (!triggers.isBlank() && !note.isBlank()) {
                notes.add(new TargetedNote(triggers, note));
            }
        }
        return notes;
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

    private record SectionScore(Section section, int score) {
    }

    private record AnswerScore(String sentence, int score) {
    }

    private record TargetedMatch(TargetedNote note, int score) {
    }

    private record TargetedNote(String triggers, String note) {
        boolean matches(String normalized) {
            return matchScore(normalized) > 0;
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
            return best;
        }

        String spokenFact() {
            return note.replaceFirst("^Targeted fact:\\s*", "");
        }
    }
}
