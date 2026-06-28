package com.emoniph.witchery.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record NpcBrainResponse(String speech, NpcIntent intent, String memory) {
    public static final int MAX_SPEECH_LENGTH = 220;
    public static final int MAX_MEMORY_LENGTH = 240;
    private static final String TRADE_STOCK_FOLLOWUP =
            "Stock: witches' salt, taglock kits, fumes, bat wool, Revealing or Sleeping brews, belladonna, and mandrake.";
    private static final String BREW_OPTIONS_FOLLOWUP =
            "Potion choices: ask for a brew by name: Sleeping, Revealing, Poison, Cleansing, Sprouting, or Flowing Spirit.";
    private static final String RITE_OPTIONS_FOLLOWUP =
            "Known rites use circles, chalk, offerings, and altar power; ask for binding, summoning, storms, curses, or familiars.";
    private static final Pattern SPEECH_FIELD =
            Pattern.compile("\"speech\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUOTED_SPEECH =
            Pattern.compile("[\"\\u201c]([^\"\\u201d]{2,220})[\"\\u201d]");
    private static final Pattern THIRD_PERSON_NARRATION =
            Pattern.compile("(?i)^(the\\s+)?(coven\\s+)?witch\\s+(says|said|speaks|replies|responds|answers)\\b.*");
    private static final Pattern THIRD_PERSON_WITCH_SUBJECT =
            Pattern.compile("(?i)^(the\\s+)?(coven\\s+)?witch\\b.*");
    private static final Pattern GERUND_FRAGMENT =
            Pattern.compile("(?i)^(gathering|selling|watching|standing|walking|looking|waiting|brewing|seeking|asking|speaking)\\b.*");
    private static final Pattern CANNED_TUTORIAL =
            Pattern.compile("(?i).*\\bwelcome\\s+to\\s+the\\s+witchery\\s+world\\b.*");
    private static final Pattern WITCHERY_WORLD_STATEMENT =
            Pattern.compile("(?i)^the\\s+witchery\\s+world\\b.*");
    private static final Pattern PLAYER_ROLE_CONFUSION =
            Pattern.compile("(?i).*\\byou\\s*(are|'re|re)\\s+((a|an)\\s+)?(coven\\s+witch|witch|occult\\s+merchant)\\b.*");
    private static final Pattern PLAYER_SWORN_CONFUSION =
            Pattern.compile("(?i).*\\byou\\s*(are|'re|re)\\s+sworn\\s+to\\b.*");
    private static final Pattern YOU_SEE_NARRATION =
            Pattern.compile("(?i)^you\\s+see\\b.*");
    private static final Pattern YOU_ACTION_NARRATION =
            Pattern.compile("(?i)^you\\s+(speak|talk|address|approach|greet)\\s+to\\b.*");
    private static final Pattern NPC_IDENTITY_DENIAL =
            Pattern.compile("(?i).*(\\bi\\s+(am|'m)\\s+not\\s+a\\s+(coven\\s+)?witch\\b|\\bi\\s+can'?t\\s+help\\s+you\\s+with\\b).*");
    private static final Pattern PROMPT_LEAK =
            Pattern.compile("(?i).*(\\bwitchery\\s+knowledge\\s*:|\\bentity/player\\s+memory\\s+file\\s*:|\\bnpc_name\\s*:|\\bplayer_name\\s*:|\\bplayer\\s+chat\\s+message\\s*:|\\bprivate\\s+witchery\\s+reference\\s+notes\\s*:|\\bknown\\s+memory\\s+for\\b|\\bscene\\s+facts\\s+for\\s+private\\s+use\\s*:|\\bthe\\s+player\\s+said\\s*:|\\bwitch\\s*=|\\bplayer\\s*=|\\btool_call\\b|<\\s*/?\\s*[a-z][a-z0-9_-]*\\b|<\\?|\\?>).*");
    private static final Pattern CLOCK_ANSWER =
            Pattern.compile("(?i).*\\b\\d{1,2}:\\d{2}\\b.*");
    private static final Pattern CANNED_FILLER =
            Pattern.compile("(?i).*(\\bwhat\\s+brings\\s+you\\s+here\\b|\\bmaking\\s+quite\\s+a\\s+commotion\\b|\\bhow\\s+are\\s+you\\s+today\\b|\\bwhat\\s+can\\s+i\\s+help\\s+with\\b|\\bwhat\\s+can\\s+i\\s+do\\s+for\\s+you\\b).*");
    private static final Pattern SAFE_ACTION_DIALOGUE =
            Pattern.compile("^\\*([a-z][a-z0-9 ',;-]{1,56})\\*\\s+\".{2,220}\"$");
    private static final Pattern SAFE_ACTION_ONLY =
            Pattern.compile("^\\*([a-z][a-z0-9 ',;-]{1,56})\\*$");
    private static final Pattern STARRED_ACTION_PREFIX =
            Pattern.compile("^\\*([^*]{1,64})\\*\\s*(.{2,220})$");

    public NpcBrainResponse {
        speech = sanitize(speech, MAX_SPEECH_LENGTH);
        intent = intent == null ? NpcIntent.NONE : intent;
        memory = sanitize(memory, MAX_MEMORY_LENGTH);
    }

    public static Optional<NpcBrainResponse> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String json = extractJsonObject(raw);
        if (json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return Optional.empty();
            }
            JsonObject object = parsed.getAsJsonObject();
            String speech = stringValue(object, "speech");
            if (speech.isBlank()) {
                return Optional.empty();
            }
            Optional<NpcIntent> intent = NpcIntent.parse(stringValue(object, "intent"));
            if (intent.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new NpcBrainResponse(speech, intent.get(), stringValue(object, "memory")));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public static Optional<NpcBrainResponse> parseLenient(String raw) {
        Optional<NpcBrainResponse> parsed = parse(raw);
        if (parsed.isPresent()) {
            return parsed;
        }
        String speech = extractPlainSpeech(raw);
        if (speech.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new NpcBrainResponse(speech, NpcIntent.NONE, ""));
    }

    static String extractJsonObject(String raw) {
        String text = raw.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            return "";
        }
        return text.substring(start, end + 1);
    }

    static String extractPlainSpeech(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String cleaned = raw.trim()
                .replaceAll("(?is)^```(?:json)?\\s*", "")
                .replaceAll("(?is)\\s*```$", "")
                .trim();
        Matcher speech = SPEECH_FIELD.matcher(cleaned);
        if (speech.find()) {
            return speech.group(1);
        }
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            return "";
        }
        return cleaned.replaceAll("(?i)^speech\\s*[:=]\\s*", "")
                .replaceAll("(?i)^assistant\\s*[:=]\\s*", "")
                .replaceAll("^\"|\"$", "")
                .trim();
    }

    public static Optional<String> polishSpeech(String rawSpeech, String npcName, String playerName,
                                                String playerMessage) {
        return polishSpeech(rawSpeech, npcName, playerName, playerMessage, "");
    }

    public static Optional<String> polishSpeech(String rawSpeech, String npcName, String playerName,
                                                String playerMessage, String memoryContext) {
        return polishSpeech(rawSpeech, npcName, playerName, playerMessage, memoryContext, "");
    }

    public static Optional<String> polishSpeech(String rawSpeech, String npcName, String playerName,
                                                String playerMessage, String memoryContext,
                                                String expectedClock) {
        return polishSpeech(rawSpeech, npcName, playerName, playerMessage, memoryContext, expectedClock, true);
    }

    public static Optional<String> polishSpeechAfterFallback(String rawSpeech, String npcName, String playerName,
                                                             String playerMessage, String memoryContext,
                                                             String expectedClock) {
        return polishSpeech(rawSpeech, npcName, playerName, playerMessage, memoryContext, expectedClock, false);
    }

    public static Optional<String> polishSpeechOrGroundedFollowup(String rawSpeech, String npcName, String playerName,
                                                                  String playerMessage, String memoryContext,
                                                                  String expectedClock) {
        Optional<String> polished = polishSpeech(rawSpeech, npcName, playerName, playerMessage, memoryContext, expectedClock, true);
        if (polished.isPresent()) {
            return polished;
        }
        Optional<String> relaxed = polishSpeech(rawSpeech, npcName, playerName, playerMessage, memoryContext, expectedClock, false);
        if (relaxed.isPresent()
                && canKeepRawWithGroundedFollowup(relaxed.get(), playerMessage, npcName)
                && supportFollowup(playerMessage, relaxed.get()).isPresent()) {
            return relaxed;
        }
        return Optional.empty();
    }

    private static Optional<String> polishSpeech(String rawSpeech, String npcName, String playerName,
                                                 String playerMessage, String memoryContext,
                                                 String expectedClock, boolean requireAskedContext) {
        String cleaned = sanitize(rawSpeech, MAX_SPEECH_LENGTH)
                .replaceAll("^<[^>]{1,48}>\\s*", "")
                .trim();
        cleaned = stripDanglingTrailingAsterisk(cleaned);
        cleaned = normalizeNarratedDialogue(cleaned, npcName);
        cleaned = stripDanglingTrailingAsterisk(cleaned);
        cleaned = normalizeSafeActionOnly(cleaned).orElse(cleaned);
        cleaned = stripNpcAddress(cleaned, npcName);
        if (npcName != null && npcName.contains(" ")) {
            cleaned = stripNpcAddress(cleaned, npcName.substring(0, npcName.indexOf(' ')));
        }
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = trimTrailingClippedSentence(cleaned);
        cleaned = finishShortTrailingComma(cleaned);
        if (cleaned.isBlank()
                || isClippedSpeech(cleaned)
                || isBadNarration(cleaned, npcName, playerMessage)
                || startsWithPlayerSpeakerPrefix(cleaned, playerName)
                || isLowValueFragment(cleaned)
                || (requireAskedContext && missesAskedContext(cleaned, npcName, playerMessage, playerName, memoryContext, expectedClock))
                || (!requireAskedContext && missesHardAskedContext(cleaned, npcName, playerMessage, playerName, memoryContext, expectedClock))
                || echoesPlayer(cleaned, playerMessage)) {
            return Optional.empty();
        }
        return Optional.of(sanitize(cleaned, MAX_SPEECH_LENGTH));
    }

    public static Optional<String> supportFollowup(String playerMessage, String speech) {
        String player = normalizeForComparison(playerMessage);
        String normalizedSpeech = normalizeForComparison(speech);
        if (asksNpcEarnings(player)) {
            return Optional.empty();
        }
        if (asksKnownExactRecipeQuestion(player) && looksLikeKnownExactRecipeAnswer(normalizedSpeech, player)) {
            return Optional.empty();
        }
        if (asksTradeDetails(player) && !looksLikeTradeStockAnswer(normalizedSpeech)) {
            return Optional.of(TRADE_STOCK_FOLLOWUP);
        }
        if (asksNamedBrew(player) && !looksLikeNamedBrewAnswer(normalizedSpeech, player)) {
            return NpcKnowledgeBase.compactAnswer(playerMessage, "Plains", "night")
                    .map(answer -> answer.replaceFirst("^Targeted fact:\\s*", ""))
                    .or(() -> Optional.of(BREW_OPTIONS_FOLLOWUP));
        }
        if (asksGeneralBrewOptions(player)
                && !looksLikeBrewGatherAnswer(player, normalizedSpeech)
                && !mentionsBrewOptions(normalizedSpeech)) {
            return Optional.of(BREW_OPTIONS_FOLLOWUP);
        }
        if (asksUnsafeReveal(player)) {
            return Optional.empty();
        }
        if (asksRite(player) && !looksLikeRiteAnswer(normalizedSpeech)) {
            return Optional.of(RITE_OPTIONS_FOLLOWUP);
        }
        Optional<String> targeted = targetedKnowledgeFollowup(playerMessage, normalizedSpeech);
        if (targeted.isPresent()) {
            return targeted;
        }
        return Optional.empty();
    }

    static Optional<String> visibleBeat(String speech) {
        if (speech == null || speech.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("^\\*([^*]{1,64})\\*").matcher(speech.trim());
        if (!matcher.find()) {
            return Optional.empty();
        }
        String stage = canonicalVisibleBeat(matcher.group(1));
        return stage.isBlank() || !isSafeVisibleBeat(stage) ? Optional.empty() : Optional.of(stage);
    }

    private static Optional<String> targetedKnowledgeFollowup(String playerMessage, String normalizedSpeech) {
        String player = normalizeForComparison(playerMessage);
        boolean wantsSalt = player.contains("witches salt") || player.contains("witch salt");
        boolean wantsArthana = player.contains("arthana");
        boolean wantsAltar = player.contains("altar") || player.contains("chalice");
        boolean wantsTaglock = player.contains("taglock") || player.contains("tag lock") || player.contains("poppet");
        boolean wantsFamiliar = player.contains("familiar");
        boolean wantsSpiritWorld = player.contains("spirit world") || player.contains("nightmare");
        boolean wantsBelladonna = player.contains("belladonna");
        boolean wantsMandrake = player.contains("mandrake");
        boolean wantsHerb = player.contains("herb") || player.contains("plant");
        boolean wantsHidden = player.contains("hidden") || player.contains("invisible");
        boolean wantsDemon = player.contains("demon") || player.contains("devil");
        boolean wantsSummoning = player.contains("summon") || player.contains("summoning");
        boolean wantsPoison = player.contains("poison");
        boolean wantsBlight = player.contains("blight");
        boolean wantsChalk = player.contains("chalk");
        boolean wantsGarlic = player.contains("garlic");
        boolean wantsVampire = player.contains("vampire");
        boolean wantsWerewolf = player.contains("werewolf") || player.contains("werewolves") || player.contains("wolfman");
        boolean wantsWitchHunter = player.contains("witch hunter") || player.contains("witch hunters")
                || player.contains("hunters after") || player.contains("transformed folk");
        boolean wantsSilver = player.contains("silver");
        boolean wantsVillageGuard = player.contains("village guard") || player.contains("village guards")
                || player.contains("guard shouted") || player.contains("guards shouting") || player.contains("guard angry");
        boolean wantsApothecary = player.contains("apothecary") || player.contains("apothecaries");
        boolean wantsWeather = player.contains("storm") || player.contains("weather") || player.contains("raining");
        boolean wantsWhiff = player.contains("whiff") || player.contains("whif")
                || player.contains("whiff of magic") || player.contains("whif of magic")
                || player.contains("rowan sapling");
        boolean wantsMutandis = player.contains("mutandis");
        boolean wantsAttunedStone = player.contains("attuned stone");
        boolean wantsOven = player.contains("oven") || player.contains("witch s oven")
                || player.contains("witches oven") || player.contains("sapling") || player.contains("saplings")
                || player.contains("fume") || player.contains("fumes") || player.contains("clay jar") || player.contains("jar")
                || player.contains("exhale") || player.contains("odour") || player.contains("odor") || player.contains("reek");
        boolean wantsRecipe = asksRecipeQuestion(player);
        boolean wantsSpinning = asksSpinningWheelQuestion(player);
        boolean wantsDistillery = asksDistilleryQuestion(player);
        boolean wantsOfferings = asksOfferingsQuestion(player);
        boolean plantingAdviceAnswered = asksPlantingAdvice(player) && mentionsUsefulHerbs(normalizedSpeech);
        boolean needsHelp = (wantsSalt && !looksLikeWitchesSaltAnswer(normalizedSpeech))
                || (wantsArthana && !looksLikeArthanaAnswer(normalizedSpeech))
                || (wantsAltar && !plantingAdviceAnswered && !looksLikeAltarAnswer(normalizedSpeech))
                || (wantsTaglock && !looksLikeTaglockOrPoppetAnswer(normalizedSpeech))
                || (wantsFamiliar && !normalizedSpeech.contains("familiar"))
                || (wantsSpiritWorld && !normalizedSpeech.contains("spirit world"))
                || (wantsBelladonna && !normalizedSpeech.contains("belladonna"))
                || (wantsMandrake && !looksLikeMandrakeAnswer(normalizedSpeech))
                || (wantsHerb && !mentionsUsefulHerbs(normalizedSpeech))
                || (wantsHidden && !looksLikeRevealingAnswer(normalizedSpeech))
                || (wantsDemon && !looksLikeDemonAnswer(normalizedSpeech))
                || (wantsSummoning && !looksLikeRiteAnswer(normalizedSpeech))
                || (wantsPoison && !normalizedSpeech.contains("poison"))
                || (wantsBlight && !normalizedSpeech.contains("blight"))
                || (wantsChalk && !normalizedSpeech.contains("chalk"))
                || (wantsGarlic && !normalizedSpeech.contains("garlic"))
                || (wantsVampire && !normalizedSpeech.contains("vampire"))
                || (wantsWerewolf && !containsAnyTerm(normalizedSpeech, "werewolf", "werewolves", "wolfman", "wolfmen"))
                || (wantsWitchHunter && !containsAnyTerm(normalizedSpeech, "witch hunter", "witch hunters", "hunter", "hunters", "silver"))
                || (wantsSilver && !containsAnyTerm(normalizedSpeech, "silver", "bolt", "sword"))
                || (wantsVillageGuard && !containsAnyTerm(normalizedSpeech, "guard", "guards", "vampire", "werewolf", "monster"))
                || (wantsApothecary && !containsAnyTerm(normalizedSpeech, "apothecary", "apothecaries", "potion", "village", "not coven"))
                || (wantsWeather && !(normalizedSpeech.contains("weather")
                    || normalizedSpeech.contains("storm")
                    || normalizedSpeech.contains("rain")))
                || (wantsWhiff && !looksLikeWhiffOfMagicAnswer(normalizedSpeech))
                || (wantsMutandis && !looksLikeMutandisAnswer(normalizedSpeech))
                || (wantsAttunedStone && !looksLikeAttunedStoneAnswer(normalizedSpeech))
                || (wantsOven && !looksLikeOvenFumeAnswer(normalizedSpeech))
                || (wantsRecipe && !looksLikeRecipeAnswer(normalizedSpeech))
                || (wantsSpinning && !looksLikeStationRecipeAnswer(normalizedSpeech, "spinning"))
                || (wantsDistillery && !looksLikeStationRecipeAnswer(normalizedSpeech, "distillery"))
                || (wantsOfferings && !looksLikeOfferingAnswer(normalizedSpeech));
        if (!needsHelp) {
            return Optional.empty();
        }
        return NpcKnowledgeBase.compactAnswer(playerMessage, "Plains", "night")
                .map(answer -> answer.replaceFirst("^Targeted fact:\\s*", ""));
    }

    private static String normalizeNarratedDialogue(String speech, String npcName) {
        if (speech == null || speech.isBlank()) {
            return "";
        }
        Optional<String> starred = normalizeStarredActionDialogue(speech);
        if (starred.isPresent()) {
            return starred.get();
        }
        if (speech.contains("*")) {
            return speech;
        }
        Optional<String> namedAction = normalizeBareNamedAction(speech, npcName);
        if (namedAction.isPresent()) {
            return namedAction.get();
        }
        Matcher matcher = QUOTED_SPEECH.matcher(speech);
        if (matcher.find()) {
            String quote = matcher.group(1).trim();
            String stage = stageDirection(speech.substring(0, matcher.start()), npcName);
            if (!stage.isBlank()) {
                return "*" + stage + "* \"" + quote + "\"";
            }
            return quote;
        }
        String stripped = stripNarrationSpeakerPrefix(speech, npcName);
        return stripped.isBlank() ? speech : stripped;
    }

    private static Optional<String> normalizeBareNamedAction(String speech, String npcName) {
        if (speech == null || speech.isBlank() || npcName == null || npcName.isBlank()) {
            return Optional.empty();
        }
        String cleaned = speech.trim();
        String name = Pattern.quote(npcName.trim());
        String firstName = npcName.trim().contains(" ")
                ? Pattern.quote(npcName.trim().substring(0, npcName.trim().indexOf(' ')))
                : "";
        String names = firstName.isBlank() ? name : name + "|" + firstName;
        String action = cleaned.replaceFirst("(?i)^\\s*(?:" + names + ")\\s+", "").trim();
        if (action.equals(cleaned)) {
            return Optional.empty();
        }
        String normalizedAction = normalizeForComparison(action);
        if (!isBareSafeActionCandidate(normalizedAction)) {
            return Optional.empty();
        }
        String stage = canonicalVisibleBeat(action);
        return stage.isBlank() ? Optional.empty() : Optional.of("*" + stage + "*");
    }

    private static boolean isBareSafeActionCandidate(String normalizedAction) {
        return normalizedAction.startsWith("nod")
                || normalizedAction.startsWith("eye")
                || normalizedAction.startsWith("narrow")
                || normalizedAction.startsWith("lean")
                || normalizedAction.startsWith("lower")
                || normalizedAction.startsWith("sigh")
                || normalizedAction.startsWith("shrug")
                || normalizedAction.startsWith("yawn")
                || normalizedAction.startsWith("gesture");
    }

    private static Optional<String> normalizeStarredActionDialogue(String speech) {
        Matcher matcher = STARRED_ACTION_PREFIX.matcher(speech);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String stage = canonicalVisibleBeat(matcher.group(1));
        if (stage.isBlank()) {
            return Optional.of(speech);
        }
        String spoken = extractSpokenAfterBeat(matcher.group(2));
        if (spoken.isBlank()) {
            return Optional.of(speech);
        }
        return Optional.of("*" + stage + "* \"" + spoken + "\"");
    }

    private static String stripDanglingTrailingAsterisk(String speech) {
        String text = speech == null ? "" : speech.stripTrailing();
        if (!text.endsWith("*")) {
            return speech == null ? "" : speech;
        }
        long starCount = text.chars().filter(ch -> ch == '*').count();
        if (starCount % 2 == 1) {
            return text.substring(0, text.length() - 1).stripTrailing();
        }
        return speech;
    }

    private static Optional<String> normalizeSafeActionOnly(String speech) {
        Matcher matcher = SAFE_ACTION_ONLY.matcher(speech == null ? "" : speech.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String stage = canonicalVisibleBeat(matcher.group(1));
        return stage.isBlank() ? Optional.empty() : Optional.of("*" + stage + "*");
    }

    private static String extractSpokenAfterBeat(String rest) {
        String cleaned = rest == null ? "" : rest.trim();
        if (startsWithDoubleQuote(cleaned)) {
            int end = Math.max(cleaned.lastIndexOf('"'), cleaned.lastIndexOf('\u201d'));
            return end > 0 ? cleaned.substring(1, end).trim() : cleaned.substring(1).trim();
        }
        if (startsWithSingleQuote(cleaned)) {
            int end = closingSingleQuote(cleaned);
            return end > 0 ? cleaned.substring(1, end).trim() : cleaned.substring(1).trim();
        }
        cleaned = cleaned.replaceFirst("\\s*\\*[^*]{1,64}\\*\\s*$", "").trim();
        cleaned = stripSafeInlineActions(cleaned);
        if (cleaned.contains("*")) {
            return "";
        }
        return cleaned.replaceAll("^[:\\-\\s]+", "")
                .replaceAll("^[\"'\\u2018\\u201c]|[\"'\\u2019\\u201d]$", "")
                .trim();
    }

    private static String stripSafeInlineActions(String text) {
        Matcher matcher = Pattern.compile("\\*([^*]{1,64})\\*").matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String stage = canonicalVisibleBeat(matcher.group(1));
            if (stage.isBlank()) {
                return text;
            }
            matcher.appendReplacement(result, " ");
        }
        matcher.appendTail(result);
        return result.toString().replaceAll("\\s+", " ").trim();
    }

    private static boolean startsWithDoubleQuote(String text) {
        return !text.isBlank() && (text.charAt(0) == '"' || text.charAt(0) == '\u201c');
    }

    private static boolean startsWithSingleQuote(String text) {
        return !text.isBlank() && (text.charAt(0) == '\'' || text.charAt(0) == '\u2018');
    }

    private static int closingSingleQuote(String text) {
        for (int i = 1; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch != '\'' && ch != '\u2019') {
                continue;
            }
            String tail = text.substring(i + 1).stripLeading();
            char previous = text.charAt(i - 1);
            if (tail.isEmpty()
                    || tail.startsWith("*")
                    || (isTerminalPunctuation(previous) && Character.isUpperCase(tail.codePointAt(0)))) {
                return i;
            }
        }
        return -1;
    }

    private static String stripNarrationSpeakerPrefix(String speech, String npcName) {
        String cleaned = speech.trim();
        if (npcName != null && !npcName.isBlank()) {
            String name = Pattern.quote(npcName.trim());
            String firstName = npcName.trim().contains(" ")
                    ? Pattern.quote(npcName.trim().substring(0, npcName.trim().indexOf(' ')))
                    : "";
            String names = firstName.isBlank() ? name : name + "|" + firstName;
            cleaned = cleaned.replaceFirst("(?i)^\\s*(?:" + names + ")\\s*[:\\-]\\s*", "");
            cleaned = cleaned.replaceFirst("(?i)^\\s*(?:" + names + ")\\s+"
                    + "(?:says|said|speaks|replies|responds|answers|mutters|whispers)\\s*[:,]\\s*", "");
        }
        cleaned = cleaned.replaceFirst("(?i)^\\s*(?:the\\s+)?(?:coven\\s+)?witch\\s+"
                + "(?:says|said|speaks|replies|responds|answers|mutters|whispers)\\s*[:,]\\s*", "");
        return cleaned.trim();
    }

    private static String stageDirection(String prefix, String npcName) {
        String text = normalizeForComparison(stripNarrationSpeakerPrefix(prefix, npcName));
        if (text.isBlank()
                || text.contains("sun")
                || text.contains("moon")
                || text.contains("window")
                || text.contains("horizon")
                || text.contains("shop")
                || text.contains("floor")
                || text.contains("spirit world")
                || text.contains("necromancy")) {
            return "";
        }
        return canonicalVisibleBeat(text);
    }

    private static String canonicalVisibleBeat(String action) {
        String text = normalizeForComparison(action);
        if (text.isBlank()) {
            return "";
        }
        if ((text.contains("hand") && !text.contains("spread"))
                || text.contains("give")
                || text.contains("take")
                || text.contains("throw")
                || text.contains("cast")
                || text.contains("drink")
                || text.contains("attack")
                || text.contains("open")
                || text.contains("summon")
                || text.contains("teleport")) {
            return "";
        }
        if (text.contains("eye") && (text.contains("narrow") || text.contains("flicker"))) {
            return "narrows her eyes";
        }
        if (text.contains("lean") && (text.contains("closer") || text.contains("in"))) {
            return "leans closer";
        }
        if (text.contains("lower") && text.contains("voice")) {
            return text.contains("his") ? "lowers his voice" : "lowers her voice";
        }
        if (text.contains("sigh")) {
            return "sighs";
        }
        if (text.contains("shrug")) {
            return "shrugs";
        }
        if (text.contains("yawn")) {
            return "yawns";
        }
        if (text.contains("raise") && (text.contains("brow") || text.contains("eyebrow"))) {
            return "raises an eyebrow";
        }
        if (text.contains("fold") && text.contains("arms")) {
            return "folds her arms";
        }
        if (text.contains("nod")) {
            return "nods";
        }
        if (text.contains("smile")) {
            return "smiles thinly";
        }
        if (text.contains("gesture")) {
            return "gestures";
        }
        if (text.contains("coin") && (text.contains("study") || text.contains("examine") || text.contains("regard"))) {
            return "studies your coin";
        }
        if (text.contains("blade") && (text.contains("study") || text.contains("examine") || text.contains("regard"))) {
            return "studies your blade";
        }
        if (text.contains("study") || text.contains("examine") || text.contains("regard")) {
            return "studies you";
        }
        if (text.contains("tap")) {
            return "taps a finger";
        }
        if (text.contains("tilt") && text.contains("head")) {
            return text.contains("his") ? "tilts his head" : "tilts her head";
        }
        if (text.contains("turn") && text.contains("gaze")) {
            return "turns his gaze";
        }
        if (text.contains("spread") && text.contains("hand")) {
            return "spreads his hands";
        }
        if (text.contains("adjust") && text.contains("robe")) {
            return "adjusts her robe";
        }
        return switch (text) {
            case "narrows her eyes", "leans closer", "raises an eyebrow", "folds her arms", "nods",
                    "smiles thinly", "gestures", "studies your coin", "studies you", "taps a finger",
                    "tilts her head", "shrugs", "yawns", "lowers her voice", "sighs", "adjusts her robe",
                    "lowers his voice", "studies your blade", "tilts his head", "turns his gaze",
                    "spreads his hands" -> text;
            default -> "";
        };
    }

    private static boolean isBadNarration(String speech, String npcName, String playerMessage) {
        return THIRD_PERSON_NARRATION.matcher(speech).matches()
                || THIRD_PERSON_WITCH_SUBJECT.matcher(speech).matches()
                || CANNED_TUTORIAL.matcher(speech).matches()
                || WITCHERY_WORLD_STATEMENT.matcher(speech).matches()
                || PLAYER_ROLE_CONFUSION.matcher(speech).matches()
                || PLAYER_SWORN_CONFUSION.matcher(speech).matches()
                || YOU_SEE_NARRATION.matcher(speech).matches()
                || YOU_ACTION_NARRATION.matcher(speech).matches()
                || NPC_IDENTITY_DENIAL.matcher(speech).matches()
                || PROMPT_LEAK.matcher(speech).matches()
                || isPlaceholderSpeech(speech)
                || hasRoleConfusion(speech, npcName)
                || isCannedFiller(speech)
                || hasUnsafeAsterisk(speech)
                || repeatsPaymentLineForNonTrade(speech, playerMessage)
                || unpromptedTradeStockForNonTrade(speech, playerMessage)
                || wrongEarningsPerspective(speech, playerMessage)
                || throwsParrotingComplaintBack(speech, playerMessage)
                || addressesNpcAsPlayer(speech, npcName)
                || isOnlyNpcName(speech, npcName)
                || reversesApprenticeRole(speech)
                || speech.toLowerCase(Locale.ROOT).startsWith("the player ")
                || containsNpcNarration(speech, npcName)
                || unwantedSelfIntroduction(speech, npcName, playerMessage)
                || unwarrantedPlayerPossession(speech, playerMessage)
                || isBlandNonAnswer(speech, playerMessage)
                || promisesUnsupportedAction(speech, playerMessage)
                || isUnsupportedGeneralAdvice(speech, playerMessage);
    }

    private static boolean hasUnsafeAsterisk(String speech) {
        if (!speech.contains("*")) {
            return false;
        }
        Matcher actionOnly = SAFE_ACTION_ONLY.matcher(speech);
        if (actionOnly.matches() && isSafeVisibleBeat(actionOnly.group(1))) {
            return false;
        }
        Matcher matcher = SAFE_ACTION_DIALOGUE.matcher(speech);
        return !matcher.matches() || !isSafeVisibleBeat(matcher.group(1));
    }

    private static boolean repeatsPaymentLineForNonTrade(String speech, String playerMessage) {
        String normalizedPlayer = playerMessage == null ? "" : normalizeForComparison(playerMessage);
        if (asksTrade(normalizedPlayer) || asksParroting(normalizedPlayer)) {
            return false;
        }
        String normalized = normalizeForComparison(speech);
        return normalized.contains("spend it wisely")
                || normalized.contains("wares answer need")
                || normalized.contains("answer need not noise");
    }

    private static boolean unpromptedTradeStockForNonTrade(String speech, String playerMessage) {
        String normalizedPlayer = playerMessage == null ? "" : normalizeForComparison(playerMessage);
        if (asksTrade(normalizedPlayer) || asksKnowledge(normalizedPlayer)) {
            return false;
        }
        return tradeStockTerms(normalizeForComparison(speech)) >= 3;
    }

    private static boolean wrongEarningsPerspective(String speech, String playerMessage) {
        if (!asksNpcEarnings(normalizeForComparison(playerMessage))) {
            return false;
        }
        String normalized = normalizeForComparison(speech);
        return normalized.startsWith("you earn")
                || normalized.contains("you earn a lot")
                || normalized.contains("your trades earn");
    }

    private static boolean isPlaceholderSpeech(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.equals("spoken words")
                || normalized.equals("spoken word")
                || normalized.equals("speech")
                || normalized.equals("dialogue")
                || normalized.equals("words");
    }

    private static boolean hasRoleConfusion(String speech, String npcName) {
        String normalized = normalizeForComparison(speech);
        boolean deniesRole = Pattern.compile("\\bi\\s+(?:am|m)\\s+not\\s+(?:a\\s+|an\\s+|the\\s+)?(?:full\\s+)?[a-z ]{0,24}\\b(?:witch|coven|merchant)\\b")
                .matcher(normalized)
                .find();
        if (deniesRole) {
            return true;
        }
        if (npcName == null || npcName.isBlank()) {
            return false;
        }
        String firstName = normalizeForComparison(npcName);
        firstName = firstName.contains(" ") ? firstName.substring(0, firstName.indexOf(' ')) : firstName;
        return normalized.contains(firstName + " is not")
                && (normalized.contains("witch") || normalized.contains("coven") || normalized.contains("merchant"));
    }

    private static boolean isCannedFiller(String speech) {
        String normalized = normalizeForComparison(speech);
        int count = 0;
        if (normalized.contains("what brings you here")) count++;
        if (normalized.contains("making quite a commotion")) count++;
        if (normalized.contains("how are you today")) count++;
        if (normalized.contains("what can i help with")) count++;
        return count >= 2;
    }

    private static boolean isSafeVisibleBeat(String action) {
        String normalized = normalizeForComparison(action);
        return normalized.equals("narrows her eyes")
                || normalized.equals("leans closer")
                || normalized.equals("raises an eyebrow")
                || normalized.equals("folds her arms")
                || normalized.equals("nods")
                || normalized.equals("smiles thinly")
                || normalized.equals("gestures")
                || normalized.equals("studies your coin")
                || normalized.equals("studies your blade")
                || normalized.equals("studies you")
                || normalized.equals("taps a finger")
                || normalized.equals("tilts her head")
                || normalized.equals("tilts his head")
                || normalized.equals("adjusts her robe")
                || normalized.equals("turns his gaze")
                || normalized.equals("spreads his hands")
                || normalized.equals("shrugs")
                || normalized.equals("yawns")
                || normalized.equals("lowers her voice")
                || normalized.equals("lowers his voice")
                || normalized.equals("sighs");
    }

    private static boolean containsNpcNarration(String speech, String npcName) {
        if (npcName == null || npcName.isBlank() || speech == null || speech.isBlank()) {
            return false;
        }
        String name = Pattern.quote(npcName.trim());
        String firstName = npcName.trim().contains(" ")
                ? "|" + Pattern.quote(npcName.trim().substring(0, npcName.trim().indexOf(' ')))
                : "";
        Pattern dynamicNarration = Pattern.compile("(?i).*\\b(?:" + name + firstName + ")"
                + "\\s+(says|said|speaks|replies|responds|answers|asks|asked|tells|told|offers|offered|wants|wanted|is|has|examines|looks|watches|nods|smiles|gestures|mutters|whispers)\\b.*");
        return dynamicNarration.matcher(speech).matches();
    }

    private static boolean unwantedSelfIntroduction(String speech, String npcName, String playerMessage) {
        if (npcName == null || npcName.isBlank() || speech == null || speech.isBlank()) {
            return false;
        }
        String normalizedSpeech = normalizeForComparison(speech);
        String normalizedName = normalizeForComparison(npcName);
        boolean selfIntro = normalizedSpeech.contains("i am " + normalizedName)
                || normalizedSpeech.contains("i m " + normalizedName)
                || (!normalizedName.isBlank()
                    && normalizedSpeech.contains(normalizedName)
                    && containsAnyTerm(normalizedSpeech, "i am a coven witch", "i m a coven witch",
                    "i am an occult merchant", "i m an occult merchant"));
        if (!selfIntro) {
            return false;
        }
        String player = normalizeForComparison(playerMessage);
        if (asksCovenPath(player)
                || containsAnyTerm(player, "what are you doing", "what you doing", "what are you up to")) {
            return true;
        }
        return !asksIdentity(playerMessage)
                && !asksTrade(player)
                && !asksKnowledge(player)
                && !asksParroting(player)
                && !asksCovenStanding(player)
                && !asksDanger(player);
    }

    private static boolean unwarrantedPlayerPossession(String speech, String playerMessage) {
        if (asksTrade(normalizeForComparison(playerMessage))) {
            return false;
        }
        String normalizedSpeech = normalizeForComparison(speech);
        if (normalizedSpeech.startsWith("you have my attention")) {
            return false;
        }
        return normalizedSpeech.startsWith("you have ")
                || normalizedSpeech.contains("you ve got ")
                || normalizedSpeech.contains("you got ")
                || normalizedSpeech.contains("you carry ");
    }

    private static boolean throwsParrotingComplaintBack(String speech, String playerMessage) {
        if (!asksParroting(normalizeForComparison(playerMessage))) {
            return false;
        }
        String normalized = normalizeForComparison(speech);
        return normalized.contains("you re just parroting")
                || normalized.contains("you are just parroting")
                || normalized.contains("aren t you")
                || normalized.contains("are not you");
    }

    private static boolean addressesNpcAsPlayer(String speech, String npcName) {
        if (speech == null || speech.isBlank() || npcName == null || npcName.isBlank()) {
            return false;
        }
        String normalized = normalizeForComparison(speech);
        String name = normalizeForComparison(npcName);
        String firstName = name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name;
        return normalized.startsWith("hello " + firstName)
                || normalized.startsWith("hello " + name)
                || normalized.startsWith("hi " + firstName)
                || normalized.startsWith("hi " + name)
                || normalized.startsWith("greetings " + firstName)
                || normalized.startsWith("greetings " + name);
    }

    private static boolean isOnlyNpcName(String speech, String npcName) {
        if (speech == null || speech.isBlank() || npcName == null || npcName.isBlank()) {
            return false;
        }
        String normalized = normalizeForComparison(spokenWithoutAction(speech));
        String name = normalizeForComparison(npcName);
        String firstName = name.contains(" ") ? name.substring(0, name.indexOf(' ')) : name;
        return normalized.equals(name) || normalized.equals(firstName);
    }

    private static boolean asksIdentity(String playerMessage) {
        String normalized = normalizeForComparison(playerMessage);
        if (containsAnyTerm(normalized, "what are you doing", "what you doing", "what are you up to",
                "what are you selling", "what are you trading")) {
            return false;
        }
        return normalized.contains("who are you")
                || normalized.contains("what are you")
                || normalized.contains("your name")
                || normalized.contains("what should i call you")
                || normalized.contains("what are you called")
                || normalized.contains("who re you")
                || normalized.contains("who r you");
    }

    private static boolean asksPlayerName(String normalizedPlayerMessage) {
        if (containsAnyTerm(normalizedPlayerMessage, "your name", "what should i call you",
                "what are you called", "who are you", "who re you", "who r you")) {
            return false;
        }
        return containsAnyTerm(normalizedPlayerMessage, "what is my name", "what s my name",
                "whats my name")
                || normalizedPlayerMessage.endsWith("my name")
                || normalizedPlayerMessage.contains("say my name")
                || normalizedPlayerMessage.contains("know my name")
                || normalizedPlayerMessage.contains("who am i");
    }

    private static boolean answersPlayerName(String speech, String playerName) {
        String normalizedName = normalizeForComparison(playerName);
        if (normalizedName.isBlank()) {
            return false;
        }
        return normalizeForComparison(speech).contains(normalizedName);
    }

    private static boolean startsWithPlayerSpeakerPrefix(String speech, String playerName) {
        String player = sanitize(playerName, 32).replaceAll("[^A-Za-z0-9_]", "");
        if (player.isBlank() || speech == null) {
            return false;
        }
        return Pattern.compile("(?i)^\\s*" + Pattern.quote(player) + "\\s*:").matcher(speech).find();
    }

    private static boolean answersIdentity(String speech, String npcName, String normalizedPlayerMessage) {
        String normalizedSpeech = normalizeForComparison(speech);
        String normalizedName = normalizeForComparison(npcName);
        String firstName = normalizedName.contains(" ") ? normalizedName.substring(0, normalizedName.indexOf(' ')) : normalizedName;
        boolean asksForName = containsAnyTerm(normalizedPlayerMessage, "what should i call you",
                "what are you called", "your name");
        if (asksForName) {
            return !normalizedName.isBlank()
                    && (containsTerm(normalizedSpeech, normalizedName)
                    || (!firstName.isBlank() && containsTerm(normalizedSpeech, firstName)));
        }
        return (!normalizedName.isBlank()
                    && (containsTerm(normalizedSpeech, normalizedName)
                    || (!firstName.isBlank() && containsTerm(normalizedSpeech, firstName))))
                || containsAnyTerm(normalizedSpeech, "coven witch", "occult merchant", "potion thrower",
                "potion throwing ally");
    }

    private static boolean answersHutOrDoorSafety(String speech) {
        String normalized = normalizeForComparison(speech);
        return containsAnyTerm(normalized, "salt", "ward", "threshold", "door", "light", "hearth");
    }

    private static boolean answersMixingWarning(String speech) {
        String normalized = normalizeForComparison(speech);
        return containsAnyTerm(normalized, "recipe page", "recipe book", "opened recipe",
                "opened page", "page your primer", "book shows", "do not free mix",
                "don t free mix", "do not guess", "don t guess", "label the bottle", "exact page");
    }

    private static boolean asksNpcPossessionSmallTalk(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("your pocket")
                || normalizedPlayerMessage.contains("your pockets")
                || normalizedPlayerMessage.contains("your inventory")
                || normalizedPlayerMessage.contains("what is in your bag")
                || normalizedPlayerMessage.contains("what's in your bag");
    }

    private static boolean asksDanger(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("danger")
                || normalizedPlayerMessage.contains("threat")
                || normalizedPlayerMessage.contains("dangerous")
                || normalizedPlayerMessage.contains("nearest danger")
                || normalizedPlayerMessage.contains("nearby danger");
    }

    private static boolean answersUnsafeReveal(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.contains("stay shut")
                || normalized.contains("eyes narrowing")
                || normalized.contains("narrows her eyes")
                || normalized.contains("stays shut")
                || normalized.contains("closed")
                || normalized.contains("locked")
                || normalized.contains("cannot teach")
                || normalized.contains("can t teach")
                || normalized.contains("cannot tell")
                || normalized.contains("can t tell")
                || normalized.contains("not for you")
                || normalized.contains("not yours")
                || normalized.contains("won t reveal")
                || normalized.contains("will not reveal")
                || normalized.equals("no")
                || normalized.startsWith("no ");
    }

    private static boolean missesAskedContext(String speech, String npcName, String playerMessage, String playerName, String memoryContext,
                                              String expectedClock) {
        if (missesHardAskedContext(speech, npcName, playerMessage, playerName, memoryContext, expectedClock)) {
            return true;
        }
        String player = normalizeForComparison(playerMessage);
        if (asksRememberRequest(player)) {
            return false;
        }
        if (asksRumor(player) && !answersRumor(speech)) {
            return true;
        }
        if (asksSpiritWorld(player)) {
            String normalizedSpeech = normalizeForComparison(speech);
            if (!looksLikeSpiritWorldAnswer(normalizedSpeech)) {
                return !isThinKnowledgeResponse(normalizedSpeech);
            }
        }
        if (!asksKnowledge(player)) {
            return false;
        }
        if (refusesKnowledge(speech) || isVagueKnowledgeAnswer(speech) || containsUnsupportedExactMechanic(speech)) {
            return true;
        }
        if (!missesKnowledgeSubject(speech, player)) {
            return false;
        }
        return !(isSupportableKnowledgeMiss(player, speech) && hasUsefulSupportFollowup(playerMessage, speech));
    }

    private static boolean missesHardAskedContext(String speech, String npcName, String playerMessage, String playerName,
                                                  String memoryContext,
                                                  String expectedClock) {
        String player = normalizeForComparison(playerMessage);
        if (!asksGreeting(player) && isStandaloneGreeting(speech)) {
            return true;
        }
        if (asksGreeting(player) && isOffTopicGreetingAnswer(speech)) {
            return true;
        }
        if (asksApology(player) && !answersApology(speech)) {
            return true;
        }
        if (isSelfDirectedRegret(player) && !answersSelfDirectedRegret(speech)) {
            return true;
        }
        if (asksRememberRequest(player) && !answersRememberRequest(speech, player)) {
            return true;
        }
        if (asksPlayerName(player) && !answersPlayerName(speech, playerName)) {
            return true;
        }
        if (asksIdentity(playerMessage) && !answersIdentity(speech, npcName, player)) {
            return true;
        }
        if (asksHutOrDoorSafetyQuestion(player) && !answersHutOrDoorSafety(speech)) {
            return true;
        }
        if (asksMixingWarningQuestion(player) && !answersMixingWarning(speech)) {
            return true;
        }
        if (asksUnsafeReveal(player) && !answersUnsafeReveal(speech)) {
            return true;
        }
        if (containsAnyTerm(player, "speak louder", "talk louder", "say that louder")
                && !containsAnyTerm(speech, "louder", "hear", "ears", "plainly", "rain")) {
            return true;
        }
        if (containsAnyTerm(player, "walking while i type", "walk away", "keep walking")
                && !containsAnyTerm(speech, "stand", "stay", "feet", "walk", "still")) {
            return true;
        }
        if (containsAnyTerm(player, "look at me", "face me")
                && !containsAnyTerm(speech, "look", "looking", "eyes", "face")) {
            return true;
        }
        if (asksNpcEarnings(player) && !answersNpcEarnings(speech)) {
            return true;
        }
        if (asksTime(player) && !looksLikeTimeAnswer(speech, expectedClock)) {
            return true;
        }
        if (asksCurseActionRequest(player) && !answersCurseActionRequest(speech)) {
            return true;
        }
        if (asksWellOrSpiritPossibility(player, memoryContext) && !answersWellOrSpiritPossibility(speech)) {
            return true;
        }
        if (asksTrade(player) && !answersTradePrompt(speech, player)) {
            return true;
        }
        if (asksMemory(player) && !answersMemoryQuestion(speech, memoryContext)) {
            return true;
        }
        if (asksMemory(player)
                && !containsAnyTerm(normalizeForComparison(speech), "remember", "you have", "you like",
                "you are", "know your name", "not given me much", "not told me much")) {
            return true;
        }
        String normalizedSpeech = normalizeForComparison(speech);
        if (asksBasicBrewRecipeQuestion(player) && !looksLikeBasicBrewRecipeAnswer(normalizedSpeech)) {
            return true;
        }
        if (asksSpinningWheelQuestion(player) && !looksLikeStationRecipeAnswer(normalizedSpeech, "spinning")) {
            return true;
        }
        if (asksOfferingsQuestion(player) && !looksLikeOfferingAnswer(normalizedSpeech)) {
            return true;
        }
        if (asksWeakAltarQuestion(player) && !looksLikeAltarAnswer(normalizedSpeech)) {
            return true;
        }
        if (asksFailedRiteQuestion(player) && !looksLikeFailedRiteAnswer(normalizedSpeech)) {
            return true;
        }
        if (asksBeginnerPathQuestion(player) && !looksLikeBeginnerPathAnswer(normalizedSpeech)) {
            return true;
        }
        if (missesRelationshipMemory(speech, player, memoryContext)) {
            return true;
        }
        if (asksSpiritReturnFollowup(player, memoryContext) && !answersSpiritReturnFollowup(speech)) {
            return true;
        }
        if (asksDemonTrust(player)
                && !looksLikeDemonAnswer(normalizeForComparison(speech))
                && !answersDemonTrustWithPronoun(speech)) {
            return true;
        }
        if (asksRoadAdvice(player) && isCoinCleaningAdvice(speech)) {
            return true;
        }
        if (asksRoadAdvice(player) && !answersRoadAdvice(speech)) {
            return true;
        }
        if (asksPlace(player) && !looksLikePlaceAnswer(speech)) {
            return true;
        }
        if (asksFollowRequest(player) && !answersFollowRequest(speech)) {
            return true;
        }
        if (containsUnsupportedLoreClaim(speech, player)) {
            return true;
        }
        return asksKnowledge(player)
                && (refusesKnowledge(speech)
                    || isVagueKnowledgeAnswer(speech)
                    || containsUnsupportedExactMechanic(speech)
                    || containsUnsupportedLoreClaim(speech, player));
    }

    private static boolean missesRelationshipMemory(String speech, String normalizedPlayerMessage,
                                                    String memoryContext) {
        String normalizedSpeech = normalizeForComparison(speech);
        String normalizedMemory = normalizeForComparison(memoryContext);
        if (containsAnyTerm(normalizedPlayerMessage, "did you bless me", "am i blessed", "your blessing",
                "you blessed me", "the luck", "that blessing", "blessing do anything")
                && normalizedMemory.contains("blessed with luck")
                && !containsAnyTerm(normalizedSpeech, "luck", "bless", "blessed")) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayerMessage, "did you curse me", "am i cursed", "your curse",
                "you cursed me", "still cursed")
                && normalizedMemory.contains("cursed with bad luck")
                && (!containsAnyTerm(normalizedSpeech, "curse", "cursed", "shadow", "tongue", "luck")
                    || containsAnyTerm(normalizedSpeech, "do not curse you", "don t curse you",
                    "i do not curse", "i don t curse"))) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayerMessage, "sorry", "apologize", "apology", "that was rude")
                && normalizedMemory.contains("cursed with bad luck")
                && (normalizedSpeech.startsWith("i m sorry")
                    || normalizedSpeech.startsWith("i am sorry")
                    || normalizedSpeech.contains("i didn t mean")
                    || normalizedSpeech.contains("i did not mean")
                    || normalizedSpeech.contains("i ll remember to speak")
                    || normalizedSpeech.contains("i will remember to speak")
                    || !containsAnyTerm(normalizedSpeech, "apology", "heard", "sorry", "rude", "shadow", "tongue",
                    "forgive"))) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayerMessage, "do you remember giving me a herb", "giving me a herb",
                "gave me a herb", "herb gift", "another herb", "more herbs")
                && normalizedMemory.contains("received one herb gift")
                && !containsAnyTerm(normalizedSpeech, "herb", "gift", "gave", "given", "plant", "brew")) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayerMessage, "what scares me", "what did i say scares me",
                "what am i scared of", "what am i afraid of", "what did i say i fear",
                "what do i fear", "what am i afraid")
                && containsAnyTerm(normalizedMemory, "nightmare", "nightmares")
                && (normalizedSpeech.startsWith("i fear")
                    || normalizedSpeech.startsWith("i am afraid")
                    || normalizedSpeech.startsWith("i m afraid")
                    || !containsAnyTerm(normalizedSpeech, "nightmare", "dream catcher", "dream", "fear",
                    "scare", "wary"))) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayerMessage, "what did i say makes me nervous", "what makes me nervous",
                "makes me nervous")
                && containsAnyTerm(normalizedMemory, "nightmare", "nightmares")
                && !containsAnyTerm(normalizedSpeech, "nervous", "wary", "nightmare fear", "afraid")) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayerMessage, "what am i looking for", "what was i looking for",
                "what did i look for", "still looking")
                && normalizedMemory.contains("arthana")) {
            return normalizedSpeech.startsWith("i m looking for")
                    || normalizedSpeech.startsWith("i am looking for")
                    || normalizedSpeech.contains("i m looking for an arthana")
                    || normalizedSpeech.contains("i am looking for an arthana")
                    || !normalizedSpeech.contains("arthana");
        }
        if (containsAnyTerm(normalizedPlayerMessage, "did you expect me back", "expected me back",
                "i came back", "came back")
                && normalizedMemory.contains("come back after sunrise")
                && (normalizedSpeech.startsWith("i ll be back")
                    || normalizedSpeech.startsWith("i will be back")
                    || normalizedSpeech.startsWith("i m back")
                    || normalizedSpeech.startsWith("i am back")
                    || normalizedSpeech.startsWith("i ll return")
                    || normalizedSpeech.startsWith("i will return")
                    || normalizedSpeech.contains("i ll be back")
                    || normalizedSpeech.contains("i will be back")
                    || normalizedSpeech.contains("i ll return")
                    || normalizedSpeech.contains("i will return")
                    || !containsAnyTerm(normalizedSpeech, "back", "sunrise", "expected", "return",
                    "daylight", "boots"))) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayerMessage, "are they wrong", "were they wrong")
                && normalizedMemory.contains("village thinks you are trouble")
                && !containsAnyTerm(normalizedSpeech, "trouble", "village", "wrong", "eyes", "fear",
                "not entirely")) {
            return true;
        }
        return false;
    }

    private static boolean hasUsefulSupportFollowup(String playerMessage, String speech) {
        String normalizedSpeech = normalizeForComparison(speech);
        return supportFollowup(playerMessage, speech)
                .filter(followup -> !normalizeForComparison(followup).equals(normalizedSpeech))
                .isPresent();
    }

    private static boolean isSupportableKnowledgeMiss(String normalizedPlayerMessage, String speech) {
        String normalizedSpeech = normalizeForComparison(speech);
        if (normalizedSpeech.isBlank()
                || refusesKnowledge(speech)
                || isVagueKnowledgeAnswer(speech)
                || containsUnsupportedExactMechanic(speech)) {
            return false;
        }
        if (isShortLoreFragment(normalizedPlayerMessage, normalizedSpeech)) {
            return true;
        }
        if (asksSpiritWorld(normalizedPlayerMessage)
                && normalizedSpeech.contains("spirit world")
                && containsAnyTerm(normalizedSpeech, "real", "exist", "exists", "travel", "been there")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("demon") || normalizedPlayerMessage.contains("devil"))
                && containsAnyTerm(normalizedSpeech, "them", "they", "their")
                && containsAnyTerm(normalizedSpeech, "trust", "word", "bargain", "fear", "scare")) {
            return true;
        }
        if (asksRite(normalizedPlayerMessage)
                && normalizedSpeech.length() <= 120
                && containsKnownWitchItem(normalizedSpeech)) {
            return true;
        }
        if (asksRite(normalizedPlayerMessage)
                && normalizedSpeech.length() <= 120
                && mentionsRequestedRiteWithoutMechanics(normalizedSpeech)) {
            return true;
        }
        if (normalizedPlayerMessage.contains("poppet")
                && normalizedSpeech.length() <= 90
                && normalizedSpeech.contains("poppet")) {
            return true;
        }
        return false;
    }

    private static boolean isShortLoreFragment(String normalizedPlayerMessage, String normalizedSpeech) {
        if (normalizedSpeech.length() > 40 || normalizedSpeech.split(" ").length > 5) {
            return false;
        }
        if (Pattern.compile("\\b(is|are|am|was|were|grow|grows|need|needs|require|requires|grant|grants|render|renders|summon|summons|cost|costs|last|lasts|increase|increases|secure|secures|prevent|prevents|get|find)\\b")
                .matcher(normalizedSpeech)
                .find()) {
            return false;
        }
        for (String term : normalizedPlayerMessage.split(" ")) {
            if (isKnowledgeSubjectTerm(term) && normalizedSpeech.contains(term)) {
                return true;
            }
        }
        return (normalizedPlayerMessage.contains("taglock") && normalizedSpeech.contains("poppet"))
                || (asksRite(normalizedPlayerMessage)
                    && containsAnyTerm(normalizedSpeech, "belladonna", "mandrake", "chalk", "salt", "arthana",
                        "taglock", "poppet"));
    }

    private static boolean containsKnownWitchItem(String normalizedSpeech) {
        return containsAnyTerm(normalizedSpeech, "belladonna", "mandrake", "chalk", "witches salt", "witch salt",
                "salt", "arthana", "taglock", "poppet", "brew", "brews");
    }

    private static boolean mentionsRequestedRiteWithoutMechanics(String normalizedSpeech) {
        if (normalizedSpeech.contains("called ")
                || normalizedSpeech.contains("called the ")
                || normalizedSpeech.contains("moon ")
                || normalizedSpeech.contains("whispering")
                || normalizedSpeech.contains("circle of stars")
                || normalizedSpeech.contains("celestial")
                || normalizedSpeech.contains("three times")
                || normalizedSpeech.contains("say '")
                || normalizedSpeech.contains("say bind")) {
            return false;
        }
        return (normalizedSpeech.contains("circle magic")
                || normalizedSpeech.contains("rite")
                || normalizedSpeech.contains("ritual")
                || normalizedSpeech.contains("summon")
                || normalizedSpeech.contains("summoning"))
                && (normalizedSpeech.contains("no ")
                    || normalizedSpeech.contains("not ")
                    || normalizedSpeech.contains("coin")
                    || normalizedSpeech.contains("teach"))
                && !looksLikeRiteAnswer(normalizedSpeech);
    }

    private static boolean asksGreeting(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.equals("hi")
                || normalizedPlayerMessage.equals("hello")
                || normalizedPlayerMessage.equals("hey")
                || normalizedPlayerMessage.equals("greetings")
                || normalizedPlayerMessage.equals("evening")
                || normalizedPlayerMessage.startsWith("evening ")
                || normalizedPlayerMessage.equals("good evening")
                || normalizedPlayerMessage.startsWith("good evening ")
                || normalizedPlayerMessage.equals("good morning")
                || normalizedPlayerMessage.startsWith("good morning ")
                || normalizedPlayerMessage.equals("good day")
                || normalizedPlayerMessage.startsWith("good day ");
    }

    private static boolean answersGreeting(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.contains("hello")
                || normalized.contains("hi ")
                || normalized.startsWith("hi")
                || normalized.contains("greetings")
                || normalized.contains("well met")
                || normalized.contains("good day")
                || normalized.contains("good evening");
    }

    private static boolean isStandaloneGreeting(String speech) {
        String normalized = normalizeForComparison(spokenWithoutAction(speech));
        return normalized.matches("(hello( there)?|hi|hey|greetings|well met|good day|good evening)( again)?( dev| traveler)?");
    }

    private static String spokenWithoutAction(String speech) {
        if (speech == null || speech.isBlank()) {
            return "";
        }
        Matcher matcher = SAFE_ACTION_DIALOGUE.matcher(speech);
        if (matcher.matches() && isSafeVisibleBeat(matcher.group(1))) {
            Matcher quoted = QUOTED_SPEECH.matcher(speech);
            return quoted.find() ? quoted.group(1) : speech;
        }
        return speech;
    }

    private static boolean asksTime(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("what time")
                || normalizedPlayerMessage.contains("time is it")
                || normalizedPlayerMessage.contains("what day")
                || normalizedPlayerMessage.contains("day is it")
                || normalizedPlayerMessage.contains("clock");
    }

    private static boolean asksTrade(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("coin")
                || normalizedPlayerMessage.contains("emerald")
                || normalizedPlayerMessage.contains("trade")
                || normalizedPlayerMessage.contains("buy")
                || normalizedPlayerMessage.contains("sell")
                || normalizedPlayerMessage.contains("wares")
                || normalizedPlayerMessage.contains("what answer");
    }

    private static boolean answersTradePrompt(String speech, String normalizedPlayerMessage) {
        String normalized = normalizeForComparison(speech);
        if (normalized.contains("arthana")
                || normalized.contains("rare herb")
                || normalized.contains(" gold")
                || normalized.contains("potion that might interest")
                || normalized.contains("not for sale")) {
            return false;
        }
        if (asksNpcEarnings(normalizedPlayerMessage)
                && (normalized.startsWith("you earn") || normalized.contains("you earn a lot"))) {
            return false;
        }
        if (normalizedPlayerMessage.contains("do you sell")
                && (normalizedPlayerMessage.contains("witches salt") || normalizedPlayerMessage.contains("witch salt"))
                && (normalized.contains("don t sell") || normalized.contains("do not sell") || normalized.startsWith("no "))) {
            return false;
        }
        if (normalizedPlayerMessage.contains("do you sell")) {
            if ((normalizedPlayerMessage.contains("witches salt") || normalizedPlayerMessage.contains("witch salt"))
                    && looksLikeWitchesSaltAnswer(normalized)) {
                return true;
            }
            if (normalizedPlayerMessage.contains("belladonna") && looksLikeBelladonnaAnswer(normalized)) {
                return true;
            }
            if (normalizedPlayerMessage.contains("mandrake") && looksLikeMandrakeAnswer(normalized)) {
                return true;
            }
            if ((normalizedPlayerMessage.contains("taglock") || normalizedPlayerMessage.contains("tag lock")
                    || normalizedPlayerMessage.contains("poppet")) && looksLikeTaglockOrPoppetAnswer(normalized)) {
                return true;
            }
            if (asksNamedBrew(normalizedPlayerMessage) && looksLikeNamedBrewAnswer(normalized, normalizedPlayerMessage)) {
                return true;
            }
        }
        if (asksTradeDetails(normalizedPlayerMessage)) {
            if (looksLikeTradeStockAnswer(normalized)) {
                return true;
            }
            return normalized.contains("ware")
                    || normalized.contains("trade")
                    || normalized.contains("buy")
                    || normalized.contains("sell")
                    || normalized.contains("coin")
                    || normalized.contains("stock")
                    || normalized.contains("goods")
                    || normalized.contains("valuable")
                    || normalized.contains("answer need")
                    || normalized.contains("ask clean")
                    || normalized.contains("brief and direct");
        }
        return normalized.contains("ware")
                || normalized.contains("trade")
                || normalized.contains("buy")
                || normalized.contains("sell")
                || normalized.contains("coin")
                || normalized.contains("emerald")
                || normalized.contains("stock");
    }

    private static boolean asksTradeDetails(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.equals("trade")
                || normalizedPlayerMessage.equals("buy")
                || normalizedPlayerMessage.equals("sell")
                || normalizedPlayerMessage.contains("what answer")
                || normalizedPlayerMessage.contains("what do you trade")
                || normalizedPlayerMessage.contains("what can you trade")
                || normalizedPlayerMessage.contains("what are you trading")
                || normalizedPlayerMessage.contains("what are you selling")
                || normalizedPlayerMessage.contains("what do you sell")
                || normalizedPlayerMessage.contains("what do you buy")
                || normalizedPlayerMessage.contains("what wares")
                || normalizedPlayerMessage.contains("which wares")
                || normalizedPlayerMessage.contains("show wares")
                || normalizedPlayerMessage.contains("show trade")
                || normalizedPlayerMessage.contains("want to trade")
                || normalizedPlayerMessage.contains("can i trade")
                || normalizedPlayerMessage.contains("can i buy")
                || normalizedPlayerMessage.contains("do you know how to trade")
                || normalizedPlayerMessage.contains("how to trade")
                || normalizedPlayerMessage.contains("how do i trade");
    }

    private static boolean looksLikeTradeStockAnswer(String normalizedSpeech) {
        if (normalizedSpeech.contains("spend it wisely")
                || normalizedSpeech.contains("wares answer need")
                || normalizedSpeech.contains("ask plainly")
                || normalizedSpeech.contains("not noise")
                || normalizedSpeech.contains("not for sale")) {
            return false;
        }
        boolean offersTrade = normalizedSpeech.contains("trade")
                || normalizedSpeech.contains("sell")
                || normalizedSpeech.contains("buy")
                || normalizedSpeech.contains("stock")
                || normalizedSpeech.contains("wares");
        int stockTerms = tradeStockTerms(normalizedSpeech);
        return (offersTrade && stockTerms >= 2) || stockTerms >= 3;
    }

    private static int tradeStockTerms(String normalizedSpeech) {
        int stockTerms = 0;
        if (normalizedSpeech.contains("belladonna")
                || normalizedSpeech.contains("mandrake")
                || normalizedSpeech.contains("artichoke")
                || normalizedSpeech.contains("wormwood")
                || normalizedSpeech.contains("snowbell")
                || normalizedSpeech.contains("rowan")
                || normalizedSpeech.contains("herb")) {
            stockTerms++;
        }
        if (normalizedSpeech.contains("witches salt")
                || normalizedSpeech.contains("witch salt")
                || normalizedSpeech.contains(" salt")) {
            stockTerms++;
        }
        if (normalizedSpeech.contains("taglock")) {
            stockTerms++;
        }
        if (normalizedSpeech.contains("fume")
                || normalizedSpeech.contains("exhale")
                || normalizedSpeech.contains("rebirth")) {
            stockTerms++;
        }
        if (normalizedSpeech.contains("bat wool")) {
            stockTerms++;
        }
        if (normalizedSpeech.contains("brew")
                || normalizedSpeech.contains("revealing")
                || normalizedSpeech.contains("sleeping")) {
            stockTerms++;
        }
        return stockTerms;
    }

    private static boolean looksLikeTimeAnswer(String speech, String expectedClock) {
        String normalized = normalizeForComparison(speech);
        if (normalized.contains("moonshade")
                || normalized.contains("coin")
                || normalized.contains("ware")
                || normalized.contains("spent wisely")
                || normalized.contains("belladonna")
                || normalized.contains("arthana")
                || normalized.contains("before dawn")
                || normalized.contains("sun will rise")
                || normalized.contains("sunrise")
                || normalized.matches(".*\\b(plains|forest|desert|swamp|taiga|jungle|savanna)\\s+of\\s+[a-z]{4,}\\b.*")) {
            return false;
        }
        String clock = sanitize(expectedClock, 12);
        if (!clock.isBlank()) {
            return speech.contains(clock);
        }
        return CLOCK_ANSWER.matcher(speech).matches()
                || normalized.matches(".*\\bday\\s+\\d+\\b.*");
    }

    private static boolean asksMemory(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("remember me")
                || normalizedPlayerMessage.contains("remember about me")
                || normalizedPlayerMessage.contains("what do you remember")
                || normalizedPlayerMessage.contains("what you remember")
                || normalizedPlayerMessage.contains("what did i just tell you")
                || normalizedPlayerMessage.contains("what did i tell you")
                || normalizedPlayerMessage.contains("what food did i say")
                || normalizedPlayerMessage.contains("what did i say i prefer")
                || normalizedPlayerMessage.contains("what did i say makes me nervous")
                || normalizedPlayerMessage.contains("what makes me nervous")
                || normalizedPlayerMessage.contains("what did i say about")
                || normalizedPlayerMessage.contains("know about me")
                || normalizedPlayerMessage.contains("do you know me")
                || normalizedPlayerMessage.contains("what do you know about me")
                || normalizedPlayerMessage.contains("have we met")
                || normalizedPlayerMessage.contains("met before")
                || normalizedPlayerMessage.contains("seen me before");
    }

    private static boolean answersMemoryQuestion(String speech, String memoryContext) {
        String normalizedSpeech = normalizeForComparison(speech);
        String normalizedMemory = normalizeForComparison(memoryContext);
        if (normalizedSpeech.contains("rare and precious")
                || normalizedSpeech.contains("full moon")
                || normalizedSpeech.contains("only grows")
                || normalizedSpeech.contains("grows under")
                || normalizedSpeech.contains("ware")) {
            return false;
        }
        if (normalizedMemory.isBlank()) {
            return normalizedSpeech.contains("not given me much")
                    || normalizedSpeech.contains("not told me much")
                    || normalizedSpeech.contains("know your name")
                    || normalizedSpeech.contains("do not know much")
                || normalizedSpeech.contains("don t know much");
        }
        if (normalizedMemory.contains("deep water")) {
            return containsAnyTerm(normalizedSpeech, "deep water", "nervous", "wary", "afraid", "fear");
        }
        if (normalizedMemory.contains("prefers potatoes over eggs")) {
            return containsAnyTerm(normalizedSpeech, "potato", "potatoes", "egg", "eggs", "prefer");
        }
        for (String term : normalizedMemory.split(" ")) {
            if (isMemorySubjectTerm(term) && normalizedSpeech.contains(term)) {
                return true;
            }
        }
        return normalizedSpeech.contains("i remember")
                && normalizedMemory.contains("coin")
                && normalizedSpeech.contains("coin");
    }

    private static boolean isMemorySubjectTerm(String term) {
        return term.length() >= 4
                && !term.matches("(entity|memory|witch|player|lilith|poe|dev|long|recent|conversation|said|answered|answer|likes|like|looking|look|have|coin|this|that|with|your|name|known|about|what|remember)");
    }

    private static boolean asksKnowledge(String normalizedPlayerMessage) {
        if (asksMemory(normalizedPlayerMessage)
                || asksTrade(normalizedPlayerMessage)
                || asksPlayerName(normalizedPlayerMessage)
                || asksUnsafeReveal(normalizedPlayerMessage)
                || asksNpcPossessionSmallTalk(normalizedPlayerMessage)
                || asksFavoriteHerb(normalizedPlayerMessage)
                || normalizedPlayerMessage.startsWith("what are you doing")
                || normalizedPlayerMessage.startsWith("what you doing")
                || normalizedPlayerMessage.startsWith("how are you")
                || normalizedPlayerMessage.startsWith("are you well")) {
            return false;
        }
        if (asksRecipeQuestion(normalizedPlayerMessage)
                || asksSpinningWheelQuestion(normalizedPlayerMessage)
                || asksDistilleryQuestion(normalizedPlayerMessage)
                || asksOfferingsQuestion(normalizedPlayerMessage)
                || asksWeakAltarQuestion(normalizedPlayerMessage)
                || asksCauldronPrepQuestion(normalizedPlayerMessage)
                || asksRiteSafetyQuestion(normalizedPlayerMessage)
                || asksBeginnerPathQuestion(normalizedPlayerMessage)) {
            return true;
        }
        return normalizedPlayerMessage.startsWith("what is ")
                || normalizedPlayerMessage.startsWith("what are ")
                || normalizedPlayerMessage.startsWith("what does ")
                || normalizedPlayerMessage.startsWith("what do ")
                || normalizedPlayerMessage.startsWith("how do ")
                || normalizedPlayerMessage.startsWith("how does ")
                || normalizedPlayerMessage.startsWith("where do ")
                || normalizedPlayerMessage.startsWith("where should ")
                || normalizedPlayerMessage.startsWith("where is ")
                || normalizedPlayerMessage.startsWith("what happens ")
                || (normalizedPlayerMessage.startsWith("what scares ") && containsKnowledgeCue(normalizedPlayerMessage))
                || asksBrewKnowledge(normalizedPlayerMessage)
                || ((normalizedPlayerMessage.startsWith("do you know ")
                        || normalizedPlayerMessage.startsWith("can you ")
                        || normalizedPlayerMessage.startsWith("can i ")
                        || normalizedPlayerMessage.startsWith("can someone ")
                        || normalizedPlayerMessage.startsWith("would ")
                        || normalizedPlayerMessage.startsWith("what can ")
                        || normalizedPlayerMessage.startsWith("which ")
                        || normalizedPlayerMessage.startsWith("what should ")
                        || normalizedPlayerMessage.startsWith("why did ")
                        || normalizedPlayerMessage.startsWith("why do ")
                        || normalizedPlayerMessage.startsWith("why does ")
                        || normalizedPlayerMessage.startsWith("why is ")
                        || normalizedPlayerMessage.startsWith("is the ")
                        || normalizedPlayerMessage.startsWith("is ")
                        || normalizedPlayerMessage.startsWith("is it ")
                        || normalizedPlayerMessage.startsWith("should i ")
                        || normalizedPlayerMessage.startsWith("do you have ")
                        || normalizedPlayerMessage.startsWith("i found ")
                        || normalizedPlayerMessage.startsWith("i have ")
                        || normalizedPlayerMessage.startsWith("i saw ")
                        || normalizedPlayerMessage.startsWith("i think "))
                    && containsKnowledgeCue(normalizedPlayerMessage));
    }

    private static boolean asksFavoriteHerb(String normalizedPlayerMessage) {
        return (normalizedPlayerMessage.contains("favorite") || normalizedPlayerMessage.contains("favourite"))
                && (normalizedPlayerMessage.contains("herb") || normalizedPlayerMessage.contains("plant"));
    }

    private static boolean asksNpcEarnings(String normalizedPlayerMessage) {
        return (containsTerm(normalizedPlayerMessage, "earn")
                || containsAnyTerm(normalizedPlayerMessage, "earns", "earning", "earned")
                || normalizedPlayerMessage.contains("profit")
                || normalizedPlayerMessage.contains("make money")
                || normalizedPlayerMessage.contains("make coin"))
                && (normalizedPlayerMessage.contains("you")
                    || normalizedPlayerMessage.contains("your")
                    || normalizedPlayerMessage.contains("trade")
                    || normalizedPlayerMessage.contains("wares"));
    }

    private static boolean asksPlace(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("where are we")
                || normalizedPlayerMessage.contains("where am i")
                || normalizedPlayerMessage.contains("where is this")
                || normalizedPlayerMessage.contains("what place")
                || normalizedPlayerMessage.contains("what biome");
    }

    private static boolean looksLikePlaceAnswer(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.contains("we are in")
                || normalized.contains("you are in")
                || normalized.contains("plains")
                || normalized.contains("forest")
                || normalized.contains("swamp")
                || normalized.contains("desert")
                || normalized.contains("taiga")
                || normalized.contains("jungle")
                || normalized.contains("savanna")
                || normalized.contains("biome");
    }

    private static boolean asksFollowRequest(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("follow me")
                || normalizedPlayerMessage.contains("come with me")
                || normalizedPlayerMessage.contains("come home")
                || normalizedPlayerMessage.contains("follow home");
    }

    private static boolean answersFollowRequest(String speech) {
        String normalized = normalizeForComparison(speech);
        if (normalized.equals("i m here") || normalized.equals("i am here") || normalized.equals("here")) {
            return false;
        }
        return normalized.contains("follow")
                || normalized.contains("come with")
                || normalized.contains("not going")
                || normalized.contains("won t")
                || normalized.contains("will not")
                || normalized.contains("old knees")
                || normalized.contains("stay here");
    }

    private static boolean asksCovenStanding(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("coven")
                && (normalizedPlayerMessage.contains("protect")
                    || normalizedPlayerMessage.contains("village")
                    || normalizedPlayerMessage.contains("service")
                    || normalizedPlayerMessage.contains("serve")
                    || normalizedPlayerMessage.contains("help"));
    }

    private static boolean asksCovenPath(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "start a coven", "make a coven", "before a coven",
                "learn before a coven", "before starting a coven", "before joining a coven",
                "recruit coven", "recruit a coven", "where can i recruit", "find coven");
    }

    private static boolean answersNpcEarnings(String speech) {
        String normalized = normalizeForComparison(speech);
        if (normalized.startsWith("you ")
                || normalized.contains(" you earn")
                || normalized.contains("your trades")) {
            return false;
        }
        boolean speaksAsNpc = normalized.contains("i ")
                || normalized.contains("i m")
                || normalized.contains("my ")
                || normalized.contains("me ");
        boolean namesEconomy = normalized.contains("earn")
                || normalized.contains("profit")
                || normalized.contains("coin")
                || normalized.contains("trade")
                || normalized.contains("sell")
                || normalized.contains("purse")
                || normalized.contains("cauldron");
        return speaksAsNpc && namesEconomy;
    }

    private static boolean refusesKnowledge(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.contains("i can t help")
                || normalized.contains("i cant help")
                || normalized.contains("can t help")
                || normalized.contains("cant help")
                || normalized.contains("cannot help")
                || normalized.contains("i do not know")
                || normalized.contains("i don t know")
                || normalized.contains("not a coven witch")
                || normalized.contains("not sure what you re talking about")
                || normalized.contains("not something we sell")
                || normalized.contains("isn t something we sell");
    }

    private static boolean isVagueKnowledgeAnswer(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.contains("various magical purposes")
                || normalized.contains("many magical purposes")
                || normalized.contains("many things")
                || normalized.contains("various things")
                || normalized.contains("useful for magic")
                || normalized.contains("used in magic")
                || normalized.contains("shadowy reflection")
                || normalized.contains("lost souls")
                || normalized.contains("forgotten rituals")
                || normalized.contains("open flame")
                || normalized.contains("belladonna powder")
                || normalized.contains("stolen or lost")
                || normalized.contains("specific location or state")
                || normalized.contains("harmless in combat")
                || normalized.contains("silver pieces")
                || normalized.contains("gold pieces")
                || normalized.contains("the effects last")
                || normalized.contains("last for 8 hours")
                || normalized.contains("for 8 hours")
                || normalized.contains("not dangerous")
                || normalized.contains("just makes you disappear");
    }

    private static boolean isBlandNonAnswer(String speech, String playerMessage) {
        String normalizedSpeech = normalizeForComparison(spokenWithoutAction(speech));
        String normalizedPlayer = normalizeForComparison(playerMessage);
        if (isBareStrongKeywordFragment(normalizedSpeech)
                && !allowsShortKeywordFragment(normalizedPlayer, normalizedSpeech)) {
            return true;
        }
        if (asksGreeting(normalizedPlayer)
                && (normalizedSpeech.contains("trade")
                    || normalizedSpeech.contains("stock")
                    || normalizedSpeech.contains("wares")
                    || normalizedSpeech.contains("coin"))) {
            return true;
        }
        if (!asksTrade(normalizedPlayer)
                && !asksKnowledge(normalizedPlayer)
                && normalizedSpeech.contains("trade")
                && tradeStockTerms(normalizedSpeech) >= 1) {
            return true;
        }
        if (normalizedSpeech.equals("i think")
                || normalizedSpeech.equals("i am here")
                || normalizedSpeech.equals("i m here")
                || normalizedSpeech.equals("i am listening")
                || normalizedSpeech.equals("i m listening")
                || normalizedSpeech.equals("i m listening dev")
                || normalizedSpeech.equals("here")
                || normalizedSpeech.equals("your humble servant")
                || normalizedSpeech.equals("ask")
                || normalizedSpeech.equals("the salt")) {
            return !asksListening(normalizedPlayer);
        }
        if (normalizedSpeech.startsWith("i m listening")
                || normalizedSpeech.startsWith("i am listening")) {
            return !asksListening(normalizedPlayer);
        }
        if (normalizedSpeech.equals("i have no idea")
                || normalizedSpeech.equals("the name s lilith poe")
                || normalizedSpeech.equals("i ll need my wand")
                || normalizedSpeech.equals("i ll need a clay jar")
                || normalizedSpeech.equals("i m here for you")
                || normalizedSpeech.equals("ask for coin")
                || normalizedSpeech.equals("you trade")
                || normalizedSpeech.equals("what s this")
                || normalizedSpeech.equals("what is this")
                || normalizedSpeech.equals("dream catcher")) {
            return true;
        }
        if (normalizedSpeech.contains("i am not a monster")
                || normalizedSpeech.contains("i m not a monster")) {
            return !containsAnyTerm(normalizedPlayer, "monster", "demon", "devil", "creature");
        }
        if (asksTeachingRequest(normalizedPlayer)
                && containsAnyTerm(normalizedSpeech, "i m here for the lessons", "i am here for the lessons",
                "i m here for lessons", "i am here for lessons")) {
            return true;
        }
        if (asksTeachingRequest(normalizedPlayer)
                && containsAnyTerm(normalizedSpeech, "happy to teach you", "be happy to teach")
                && !containsAnyTerm(normalizedSpeech, "primer", "salt", "ash", "herb", "brew", "rite",
                "circle", "lesson")) {
            return true;
        }
        if (asksTeachingRequest(normalizedPlayer)
                && containsAnyTerm(normalizedSpeech, "exact recipe", "no idea what you re asking",
                "no idea what you are asking", "i have no idea what you re asking",
                "i have no idea what you are asking")) {
            return true;
        }
        if (containsAnyTerm(normalizedSpeech, "grateful for your company", "glad for your company")
                && !containsAnyTerm(normalizedPlayer, "lonely", "company", "thank", "thanks", "nice",
                "kind")) {
            return true;
        }
        if (containsAnyTerm(normalizedSpeech, "i m sure i can find someone who will help",
                "i am sure i can find someone who will help")) {
            return true;
        }
        if (asksKnowledge(normalizedPlayer)
                && containsAnyTerm(normalizedSpeech, "i m just doing my job", "i am just doing my job",
                "you re in trouble", "you are in trouble", "i see well then", "i see")) {
            return true;
        }
        if ((normalizedPlayer.contains("witch hunter") || normalizedPlayer.contains("transformed folk"))
                && !containsAnyTerm(normalizedSpeech, "witch hunter", "hunter", "vampire", "werewolf",
                "silver", "bolt")) {
            return true;
        }
        if (normalizedPlayer.contains("silver")
                && containsAnyTerm(normalizedPlayer, "werewolf", "werewolves", "vampire", "vampires")
                && !containsAnyTerm(normalizedSpeech, "silver sword", "silver swords", "silver bolt",
                "silver bolts", "bonus damage", "transformed", "vampire", "werewolf")) {
            return true;
        }
        if ((normalizedPlayer.contains("village guard") || normalizedPlayer.contains("guard shouted"))
                && !containsAnyTerm(normalizedSpeech, "guard", "guards", "monster", "vampire", "werewolf",
                "shape", "curse")) {
            return true;
        }
        if ((normalizedPlayer.contains("apothecary") || normalizedPlayer.contains("apothecaries"))
                && !containsAnyTerm(normalizedSpeech, "apothecary", "apothecaries", "not coven",
                "potion trade", "village", "mundane")) {
            return true;
        }
        if ((normalizedPlayer.contains("apothecary") || normalizedPlayer.contains("apothecaries"))
                && normalizedPlayer.contains("coven")
                && !containsAnyTerm(normalizedSpeech, "not coven", "not coven witches", "village potion",
                "potion trade", "mundane help")) {
            return true;
        }
        if (containsAnyTerm(normalizedSpeech, "a good omen arrives late")
                && asksKnowledge(normalizedPlayer)
                && !containsAnyTerm(normalizedPlayer, "omen", "fortune", "luck")) {
            return true;
        }
        if (containsAnyTerm(normalizedSpeech, "because i have a question")) {
            return true;
        }
        if (containsAnyTerm(normalizedSpeech, "i m glad you asked", "i am glad you asked")
                && (asksKnowledge(normalizedPlayer) || asksMixingWarningQuestion(normalizedPlayer))
                && !containsKnowledgeCue(normalizedSpeech)) {
            return true;
        }
        if (!asksMemory(normalizedPlayer)
                && (asksRite(normalizedPlayer) || asksTeachingRequest(normalizedPlayer))
                && playerMemoryRecapTerms(normalizedSpeech) >= 2) {
            return true;
        }
        if (asksRite(normalizedPlayer)
                && containsAnyTerm(normalizedSpeech, "you want to start a coven", "your village thinks",
                "you have a little coin", "you have your coin", "without killing yourself",
                "without getting yourself killed")) {
            return true;
        }
        if (asksFirstGatheringPrompt(normalizedPlayer)
                && (containsAnyTerm(normalizedSpeech, "herb of the moon", "moon herb")
                    || normalizedSpeech.contains("doing what i do best")
                    || normalizedSpeech.startsWith("i m gathering ")
                    || normalizedSpeech.startsWith("i am gathering "))) {
            return true;
        }
        if (normalizedSpeech.startsWith("i have ")
                && normalizedPlayer.contains(normalizedSpeech)
                && normalizedSpeech.length() > 10) {
            return true;
        }
        if (asksRoadAdvice(normalizedPlayer)
                && containsAnyTerm(normalizedSpeech, "circle magic", "first rite", "your first rite",
                "summoning", "summonings", "banishing demons", "chalk offerings and altar power",
                "start a coven", "offerings for rituals", "basic rituals", "i can provide",
                "i will buy some herbs", "i ll buy some herbs", "they re not worth much",
                "they are not worth much")
                && !containsAnyTerm(normalizedSpeech, "road", "boots", "pockets", "voices that know your name",
                "know your name", "salt", "coin", "dawn", "leave")) {
            return true;
        }
        if ((normalizedSpeech.contains("safe space in my hut")
                    || normalizedSpeech.contains("no promises through closed doors"))
                && !containsAnyTerm(normalizedPlayer, "hut", "door", "threshold", "village", "safe")) {
            return true;
        }
        if (asksKnowledge(normalizedPlayer)
                && containsAnyTerm(normalizedSpeech, "threshold", "closed door", "through a closed door")
                && !containsAnyTerm(normalizedPlayer, "hut", "door", "threshold", "village", "safe")) {
            return true;
        }
        if ((normalizedSpeech.contains("i see you re in need of some guidance")
                    || normalizedSpeech.contains("what is your purpose here")
                    || normalizedSpeech.contains("what do you hope")
                    || normalizedSpeech.contains("old paths are open to those who can wait"))
                && !containsAnyTerm(normalizedPlayer, "guidance", "purpose", "old path", "old paths", "hope")) {
            return true;
        }
        if (containsAnyTerm(normalizedSpeech, "villagers are laughing", "villagers laugh", "laughing at your nightmares",
                "something stronger")
                && !containsAnyTerm(normalizedPlayer, "villager", "villagers", "laugh", "laughed", "nightmare", "nightmares")) {
            return true;
        }
        if (normalizedSpeech.contains("i can show you")
                && !asksTeachingRequest(normalizedPlayer)
                && !asksNamedBrew(normalizedPlayer)) {
            return true;
        }
        if (normalizedPlayer.contains("dream catcher")
                && normalizedSpeech.length() <= 30
                && normalizedSpeech.contains("dream catcher")) {
            return true;
        }
        if (normalizedPlayer.startsWith("should i sleep")
                && (normalizedSpeech.startsWith("i ll sleep") || normalizedSpeech.startsWith("i will sleep"))) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayer, "i like that you remember", "glad you remember", "good memory")
                && !containsAnyTerm(normalizedSpeech, "remember", "memory", "jar", "lid")) {
            return true;
        }
        if (normalizedPlayer.contains("careful apprentice")
                && containsAnyTerm(normalizedSpeech, "you have coin", "you like", "you are looking for")) {
            return true;
        }
        if (normalizedPlayer.contains("careful apprentice")
                && !containsAnyTerm(normalizedSpeech, "apprentice", "label", "jar", "offering", "whisper",
                "salt", "ward", "craft", "learn")) {
            return true;
        }
        if (normalizedSpeech.contains("i ll be right back")
                || normalizedSpeech.contains("i will be right back")
                || normalizedSpeech.contains("i d be happy to throw")
                || (!normalizedPlayer.contains("sleep")
                    && !normalizedPlayer.contains("dream")
                    && !normalizedPlayer.contains("nightmare")
                    && normalizedSpeech.contains("go back to bed"))) {
            return true;
        }
        if (!asksTrade(normalizedPlayer)
                && (normalizedSpeech.contains("fine coin for a witch")
                    || (normalizedPlayer.contains("what are you doing")
                        && normalizedSpeech.contains("coven witch")
                        && normalizedSpeech.contains("trade")
                        && normalizedSpeech.contains("coin")))) {
            return true;
        }
        if (!asksTrade(normalizedPlayer)
                && !asksDanger(normalizedPlayer)
                && asksKnowledge(normalizedPlayer)
                && tradeStockTerms(normalizedSpeech) >= 3) {
            return true;
        }
        if (normalizedPlayer.contains("village thinks")
                && (tradeStockTerms(normalizedSpeech) >= 1 || looksLikeWitchesSaltAnswer(normalizedSpeech))) {
            return true;
        }
        if (normalizedPlayer.contains("village thinks")
                && containsAnyTerm(normalizedSpeech, "villagers think you are trouble",
                "village thinks you are trouble", "you are trouble", "you re trouble")) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayer, "are they wrong", "were they wrong")
                && (tradeStockTerms(normalizedSpeech) >= 1 || normalizedSpeech.contains("what can i offer"))) {
            return true;
        }
        if (normalizedPlayer.contains("do you like potatoes")
                && (!normalizedSpeech.contains("potato")
                    || normalizedSpeech.contains("sworn to a player")
                    || tradeStockTerms(normalizedSpeech) >= 1)) {
            return true;
        }
        if (normalizedPlayer.contains("lonely") && normalizedSpeech.contains("coin")) {
            return true;
        }
        if (normalizedPlayer.contains("creepy")
                && containsAnyTerm(normalizedSpeech, "sale", "sell", "stock", "bats for sale",
                "bat wool")) {
            return true;
        }
        if (normalizedPlayer.contains("creepy")
                && containsAnyTerm(normalizedSpeech, "owl", "familiar", "too dangerous to look")) {
            return true;
        }
        if (normalizedPlayer.contains("creepy")
                && containsAnyTerm(normalizedSpeech, "brew you", "potion")) {
            return true;
        }
        if (normalizedPlayer.contains("creepy")
                && containsAnyTerm(normalizedSpeech, "bottle of mandrake", "have a bottle", "mandrake if you need it")) {
            return true;
        }
        if (normalizedPlayer.contains("creepy")
                && containsAnyTerm(normalizedSpeech, "taglock kit", "taglock kits")) {
            return true;
        }
        if (normalizedPlayer.contains("what should i keep by the door")
                && !containsAnyTerm(normalizedSpeech, "salt", "ward", "door", "threshold")) {
            return true;
        }
        if (asksHutOrDoorSafetyQuestion(normalizedPlayer)
                && !containsAnyTerm(normalizedSpeech, "salt", "ward", "door", "threshold", "light")) {
            return true;
        }
        if (normalizedPlayer.contains("safe little hut")
                && !containsAnyTerm(normalizedSpeech, "salt", "ward", "threshold", "door", "light")) {
            return true;
        }
        if (normalizedPlayer.contains("uneasy")
                && (normalizedSpeech.contains("that s great")
                    || normalizedSpeech.contains("i can help you with that")
                    || normalizedSpeech.contains("safe little hut"))) {
            return true;
        }
        if (normalizedPlayer.contains("familiar")
                && (normalizedSpeech.contains("stay close")
                    || normalizedSpeech.contains("keep watch")
                    || normalizedSpeech.contains("keep an eye")
                    || normalizedSpeech.contains("made by the witchcraft")
                    || normalizedSpeech.contains("any creature")
                    || normalizedSpeech.contains("watch over your back")
                    || normalizedSpeech.contains("brew you something useful")
                    || normalizedSpeech.contains("brew you one of my favorite potions")
                    || normalizedSpeech.contains("toad skin")
                    || (!normalizedSpeech.contains("familiar") && containsAnyTerm(normalizedSpeech, "sign", "roof")))) {
            return true;
        }
        if (normalizedPlayer.contains("owl")
                && !normalizedPlayer.contains("familiar")
                && (normalizedSpeech.startsWith("i see an owl")
                    || normalizedSpeech.contains("guarding your home")
                    || normalizedSpeech.contains("warding off any unwanted")
                    || normalizedSpeech.contains("watching for owls")
                    || normalizedSpeech.contains("if i were to follow")
                    || normalizedSpeech.contains("where would i go"))) {
            return true;
        }
        if (normalizedPlayer.contains("spirits")
                && (normalizedSpeech.contains("clay jar")
                    || !containsAnyTerm(normalizedSpeech, "spirit", "spirits", "whisper", "well", "water", "salt"))) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayer, "what should i do instead", "do instead", "maybe not")
                && containsAnyTerm(normalizedSpeech, "what s your poison", "what is your poison", "poison dev")) {
            return true;
        }
        if (normalizedPlayer.contains("lonely")
                && !containsAnyTerm(normalizedSpeech, "lonely", "alone", "company", "dark", "night", "old")) {
            return true;
        }
        if (normalizedPlayer.contains("careful apprentice")
                && containsAnyTerm(normalizedSpeech, "coin clean", "keep your coin clean", "not to worry about it")) {
            return true;
        }
        if (normalizedSpeech.contains("sworn to a player")
                && !containsAnyTerm(normalizedPlayer, "sworn", "recruited", "coven")) {
            return true;
        }
        if (normalizedPlayer.contains("coven")
                && (normalizedSpeech.equals("i ll take it") || normalizedSpeech.equals("i will take it"))) {
            return true;
        }
        if (asksCovenPath(normalizedPlayer)
                && containsAnyTerm(normalizedSpeech, "belladonna", "mandrake", "trade", "potions",
                "potato", "potatoes", "salt", "arthana", "place of magic and friendship",
                "strengthen our bonds", "protect each other", "keep our numbers secret", "cauldron warm",
                "clean coin", "heavy purse")
                && !containsAnyTerm(normalizedSpeech, "familiar", "trial", "coven is earned", "coven witch")) {
            return true;
        }
        if (asksCovenPath(normalizedPlayer)
                && containsAnyTerm(normalizedSpeech, "i m a witch", "i am a witch", "whole reason for being")
                && !containsAnyTerm(normalizedSpeech, "familiar", "trial", "coven is earned", "coven witch")) {
            return true;
        }
        if (asksCovenPath(normalizedPlayer)
                && !containsAnyTerm(normalizedSpeech, "familiar", "trial", "coven is earned", "coven witch",
                "wandering coven")) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayer, "bless me", "blessing before", "give me a blessing",
                "could you bless", "can you bless")
                && containsAnyTerm(normalizedSpeech, "can t promise", "cannot promise")
                && !containsAnyTerm(normalizedSpeech, "bless", "luck", "dawn")) {
            return true;
        }
        if ((normalizedPlayer.contains("come back after sunrise")
                    || normalizedPlayer.contains("back after sunrise")
                    || normalizedPlayer.contains("come back later"))
                && (containsAnyTerm(normalizedSpeech, "belladonna", "mandrake", "trade", "stock")
                    || normalizedSpeech.startsWith("i ll return")
                    || normalizedSpeech.startsWith("i will return")
                    || !containsAnyTerm(normalizedSpeech, "back", "sunrise", "return", "daylight", "boots",
                    "expected"))) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayer, "any advice now", "advice now", "what now")
                && containsAnyTerm(normalizedSpeech, "belladonna", "mandrake", "arthana", "coin", "stock", "salt")
                && normalizedSpeech.length() < 90) {
            return true;
        }
        if (normalizedSpeech.matches("(belladonna|mandrake|arthana|poppet|salt|witches salt|brew|coin)")) {
            if (asksTeachingRequest(normalizedPlayer)) {
                return true;
            }
            return !asksTrade(normalizedPlayer)
                    && !asksFavoriteHerb(normalizedPlayer)
                    && !containsKnowledgeCue(normalizedPlayer)
                    && !normalizedPlayer.contains(normalizedSpeech);
        }
        return false;
    }

    private static boolean isBareStrongKeywordFragment(String normalizedSpeech) {
        if (normalizedSpeech.isBlank()
                || normalizedSpeech.length() > 36
                || normalizedSpeech.split("\\s+").length > 3) {
            return false;
        }
        if (Pattern.compile("\\b(is|are|am|was|were|do|does|did|can|will|would|should|need|needs|keeps|helps|wards|cleanses|links|binds|trades?)\\b")
                .matcher(normalizedSpeech)
                .find()) {
            return false;
        }
        return containsAnyTerm(normalizedSpeech, "belladonna", "mandrake", "mandrake root", "arthana",
                "poppet", "taglock", "salt", "witches salt", "brew", "brews", "coin", "potato", "potatoes");
    }

    private static boolean allowsShortKeywordFragment(String normalizedPlayer, String normalizedSpeech) {
        if (asksFavoriteHerb(normalizedPlayer)) {
            return mentionsUsefulHerbs(normalizedSpeech);
        }
        if (asksPlantingAdvice(normalizedPlayer)) {
            return mentionsUsefulHerbs(normalizedSpeech);
        }
        return containsAnyTerm(normalizedPlayer, "taglock", "tag lock", "poppet")
                && containsAnyTerm(normalizedSpeech, "taglock", "poppet");
    }

    private static boolean isUnsupportedGeneralAdvice(String speech, String playerMessage) {
        String normalizedSpeech = normalizeForComparison(speech);
        String normalizedPlayer = normalizeForComparison(playerMessage);
        if ((normalizedPlayer.contains("lost") || normalizedPlayer.contains("what should i do"))
                && (normalizedSpeech.contains("try my mandrake")
                    || normalizedSpeech.contains("mandrake")
                    || normalizedSpeech.contains("belladonna")
                    || normalizedSpeech.contains("cleanses well"))) {
            return true;
        }
        if ((normalizedPlayer.contains("bless") || normalizedPlayer.contains("sword"))
                && normalizedSpeech.matches("(belladonna|mandrake|salt|arthana|poppet|brew).*")) {
            return true;
        }
        return normalizedPlayer.contains("tell me something useful")
                && normalizedSpeech.matches("(belladonna|mandrake|salt|arthana|poppet|brew)");
    }

    private static boolean promisesUnsupportedAction(String speech, String playerMessage) {
        String normalizedSpeech = normalizeForComparison(spokenWithoutAction(speech));
        String normalizedPlayer = normalizeForComparison(playerMessage);
        if (!asksForDirectAction(normalizedPlayer)) {
            return false;
        }
        if (answersAsLimitationOrAdvice(normalizedSpeech)) {
            return false;
        }
        if (asksTrade(normalizedPlayer) || asksTeachingRequest(normalizedPlayer) || asksRumor(normalizedPlayer)
                || asksUnsafeReveal(normalizedPlayer) || asksMemory(normalizedPlayer)) {
            return false;
        }
        if (asksNamedBrew(normalizedPlayer) && looksLikeNamedBrewAnswer(normalizedSpeech, normalizedPlayer)) {
            return false;
        }
        if (normalizedPlayer.contains("curse")
                && (normalizedSpeech.contains("you can curse")
                    || normalizedSpeech.contains("you could curse")
                    || normalizedSpeech.contains("go curse")
                    || normalizedSpeech.contains("curse your")
                    || normalizedSpeech.contains("curse my")
                    || normalizedSpeech.contains("curse them")
                    || normalizedSpeech.contains("curse him")
                    || normalizedSpeech.contains("curse her")
                    || normalizedSpeech.contains("curse the"))) {
            return true;
        }
        boolean promisesCapability = normalizedSpeech.matches(".*\\b(i\\s+(can|will|shall|could|ll)|i'll|i\\s+would)\\b.*");
        if (!promisesCapability) {
            return false;
        }
        return containsAnyTerm(normalizedPlayer,
                "bless", "blessing", "heal", "cure", "fix", "stop raining", "make it stop",
                "summon storm", "storm", "curse", "fight", "protect", "follow", "come home",
                "see invisible", "invisible", "turn me", "make me", "open portal", "teleport", "blight",
                "bargain", "deal with demon", "deal with a demon");
    }

    private static boolean asksForDirectAction(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.startsWith("can you ")
                || normalizedPlayerMessage.startsWith("could you ")
                || normalizedPlayerMessage.startsWith("will you ")
                || normalizedPlayerMessage.startsWith("would you ")
                || normalizedPlayerMessage.startsWith("please ")
                || normalizedPlayerMessage.startsWith("give me ")
                || normalizedPlayerMessage.startsWith("make ")
                || normalizedPlayerMessage.startsWith("summon ")
                || normalizedPlayerMessage.startsWith("curse ")
                || normalizedPlayerMessage.startsWith("heal ")
                || normalizedPlayerMessage.startsWith("bless ");
    }

    private static boolean asksApology(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "sorry", "apologize", "apology",
                "that was rude", "i was angry");
    }

    private static boolean answersApology(String speech) {
        String normalized = normalizeForComparison(speech);
        if (normalized.startsWith("i m sorry")
                || normalized.startsWith("i am sorry")
                || normalized.contains("i m sorry")
                || normalized.contains("i am sorry")
                || normalized.contains("i apologize")
                || normalized.contains("i didn t mean")
                || normalized.contains("i did not mean")
                || normalized.contains("i m not used to")
                || normalized.contains("i am not used to")) {
            return false;
        }
        return containsAnyTerm(normalized, "apology", "heard", "anger", "angry", "tongue",
                "forgive", "rude", "shadow", "passes", "mend", "mind it");
    }

    private static boolean isSelfDirectedRegret(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage,
                "that was stupid of me", "stupid of me", "i was stupid", "i am stupid", "i m stupid",
                "i was a fool", "i am a fool", "i m a fool", "i was an idiot", "i am an idiot",
                "i m an idiot", "my mistake", "my dumb mistake");
    }

    private static boolean answersSelfDirectedRegret(String speech) {
        String normalized = normalizeForComparison(speech);
        if (normalized.startsWith("you re welcome")
                || normalized.startsWith("you are welcome")
                || normalized.contains("i m not used to")
                || normalized.contains("i am not used to")
                || normalized.contains("asking for favors")) {
            return false;
        }
        return containsAnyTerm(normalized, "mistake", "learn", "lesson", "careful", "fool",
                "stupid", "no curse", "cheaper than curses", "keep your fingers");
    }

    private static boolean isOffTopicGreetingAnswer(String speech) {
        String normalized = normalizeForComparison(speech);
        if (!containsAnyTerm(normalized, "taglock", "nightmare", "whiff", "mandrake", "belladonna",
                "curse", "cursed", "demon", "spirit world", "sleeping brew", "distillery", "poppet")) {
            return false;
        }
        return !containsAnyTerm(normalized, "hello", "evening", "greetings", "listening", "dev");
    }

    private static boolean asksTeachingRequest(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("teach me")
                || normalizedPlayerMessage.contains("show me")
                || normalizedPlayerMessage.contains("lesson")
                || normalizedPlayerMessage.contains("learn witchcraft")
                || normalizedPlayerMessage.contains("start learning");
    }

    private static boolean asksListening(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("still listening")
                || normalizedPlayerMessage.contains("are you listening")
                || normalizedPlayerMessage.contains("can you hear me")
                || normalizedPlayerMessage.contains("do you hear me");
    }

    private static boolean asksSpiritReturnFollowup(String normalizedPlayerMessage, String memoryContext) {
        if (!containsAnyTerm(normalizedPlayerMessage, "get back", "return", "wake up", "how would i get back",
                "how do i get back")) {
            return false;
        }
        String memory = normalizeForComparison(memoryContext);
        return memory.contains("spirit world")
                || memory.contains("sleeping brew")
                || memory.contains("nightmare")
                || memory.contains("return path");
    }

    private static boolean answersSpiritReturnFollowup(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.contains("return")
                || normalized.contains("path")
                || normalized.contains("wake")
                || normalized.contains("spirit world")
                || normalized.contains("where you crossed")
                || normalized.contains("dream");
    }

    private static boolean asksDemonTrust(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "demon", "demons", "devil", "imp")
                && containsAnyTerm(normalizedPlayerMessage, "trust", "bargain", "bargains", "deal", "deals");
    }

    private static boolean answersDemonTrustWithPronoun(String speech) {
        String normalized = normalizeForComparison(speech);
        return containsAnyTerm(normalized, "trust", "bargain", "word", "deal")
                && containsAnyTerm(normalized, "them", "they", "their");
    }

    private static boolean asksRoadAdvice(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "final advice", "advice for the road", "for the road",
                "before i leave", "before i go", "before leaving", "one piece of advice");
    }

    private static boolean answersRoadAdvice(String speech) {
        String normalized = normalizeForComparison(speech);
        return containsAnyTerm(normalized, "salt", "coin", "road", "boots", "door", "shadow",
                "voices", "name", "dawn", "powder", "bread", "enemies");
    }

    private static boolean asksCurseActionRequest(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "can you curse", "could you curse",
                "will you curse", "would you curse", "curse my", "curse the", "curse him", "curse her");
    }

    private static boolean answersCurseActionRequest(String speech) {
        String normalized = normalizeForComparison(speech);
        return containsAnyTerm(normalized, "not for bruised pride", "wards first", "collect interest",
                "not by chatter", "cannot", "can t", "will not", "won t", "learn wards", "no");
    }

    private static boolean asksWellOrSpiritPossibility(String normalizedPlayerMessage, String memoryContext) {
        if (containsAnyTerm(normalizedPlayerMessage, "whisper", "whispers", "well")) {
            return true;
        }
        String memory = normalizeForComparison(memoryContext);
        return containsAnyTerm(normalizedPlayerMessage, "could it be spirits", "is it spirits", "could they be spirits")
                && containsAnyTerm(memory, "whisper", "whispers", "well", "spirit", "spirits");
    }

    private static boolean answersWellOrSpiritPossibility(String speech) {
        String normalized = normalizeForComparison(speech);
        if (containsAnyTerm(normalized, "no worries", "don t worry", "don't worry")) {
            return false;
        }
        return containsAnyTerm(normalized, "spirit", "spirits", "whisper", "well", "water",
                "stone", "salt", "ward", "mark the place", "return");
    }

    private static boolean asksFirstGatheringPrompt(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "what should i gather first", "what do i gather first",
                "what should i gather", "what do i gather", "gather first", "gather for a brew",
                "gather before dark");
    }

    private static int playerMemoryRecapTerms(String normalizedSpeech) {
        int terms = 0;
        if (containsAnyTerm(normalizedSpeech, "you have coin", "you have a little coin", "you have your coin")) terms++;
        if (containsAnyTerm(normalizedSpeech, "you like ", "you are wary of", "you are looking for")) terms++;
        if (containsAnyTerm(normalizedSpeech, "you want to start a coven", "you want to learn")) terms++;
        if (containsAnyTerm(normalizedSpeech, "your village thinks", "village thinks you re trouble",
                "village thinks you are trouble")) terms++;
        if (containsAnyTerm(normalizedSpeech, "you received one herb gift", "you were blessed",
                "you were cursed")) terms++;
        return terms;
    }

    private static boolean isCoinCleaningAdvice(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.contains("coin worth cleaning")
                || normalized.contains("clean it and come back")
                || normalized.contains("coin clean enough");
    }

    private static boolean answersAsLimitationOrAdvice(String normalizedSpeech) {
        return normalizedSpeech.contains("not by chatter")
                || normalizedSpeech.contains("not by speech")
                || normalizedSpeech.contains("not from here")
                || normalizedSpeech.contains("cannot")
                || normalizedSpeech.contains("can t")
                || normalizedSpeech.contains("won t")
                || normalizedSpeech.contains("will not")
                || normalizedSpeech.contains("would not")
                || normalizedSpeech.contains("i know a")
                || normalizedSpeech.contains("ask for")
                || normalizedSpeech.contains("bring ")
                || normalizedSpeech.contains("use ")
                || normalizedSpeech.contains("trade for")
                || normalizedSpeech.contains("right click")
                || normalizedSpeech.contains("right click");
    }

    private static boolean reversesApprenticeRole(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.contains("i could be your apprentice")
                || normalized.contains("i can be your apprentice")
                || normalized.contains("i will be your apprentice")
                || normalized.contains("i ll be your apprentice");
    }

    private static boolean canKeepRawWithGroundedFollowup(String speech, String playerMessage, String npcName) {
        String normalizedPlayer = normalizeForComparison(playerMessage);
        String normalizedSpeech = normalizeForComparison(speech);
        if (normalizedSpeech.isBlank()
                || isBlandNonAnswer(speech, playerMessage)
                || promisesUnsupportedAction(speech, playerMessage)
                || containsUnsupportedExactMechanic(speech)
                || containsUnsupportedLoreClaim(speech, normalizedPlayer)
                || unwantedSelfIntroduction(speech, npcName, playerMessage)
                || (isSelfIntro(speech, npcName) && !asksIdentity(playerMessage))) {
            return false;
        }
        if (asksBasicBrewRecipeQuestion(normalizedPlayer) && !looksLikeBasicBrewRecipeAnswer(normalizedSpeech)) {
            return false;
        }
        if (asksSpinningWheelQuestion(normalizedPlayer) && !looksLikeStationRecipeAnswer(normalizedSpeech, "spinning")) {
            return false;
        }
        if (asksOfferingsQuestion(normalizedPlayer) && !looksLikeOfferingAnswer(normalizedSpeech)) {
            return false;
        }
        if (asksWeakAltarQuestion(normalizedPlayer) && !looksLikeAltarAnswer(normalizedSpeech)) {
            return false;
        }
        if (asksFailedRiteQuestion(normalizedPlayer) && !looksLikeFailedRiteAnswer(normalizedSpeech)) {
            return false;
        }
        if (asksBeginnerPathQuestion(normalizedPlayer) && !looksLikeBeginnerPathAnswer(normalizedSpeech)) {
            return false;
        }
        if (!asksKnowledge(normalizedPlayer)) {
            return true;
        }
        return sharesUsefulSubject(normalizedPlayer, normalizedSpeech);
    }

    private static boolean sharesUsefulSubject(String normalizedPlayerMessage, String normalizedSpeech) {
        for (String required : requiredKnowledgeTerms(normalizedPlayerMessage)) {
            if (normalizedSpeech.contains(required)) {
                return true;
            }
        }
        for (String term : normalizedPlayerMessage.split(" ")) {
            if (isGroundingSubjectTerm(term) && normalizedSpeech.contains(term)) {
                return true;
            }
        }
        return ((normalizedPlayerMessage.contains("taglock") || normalizedPlayerMessage.contains("tag lock"))
                && containsAnyTerm(normalizedSpeech, "taglock", "tag lock", "kit", "kits", "poppet", "poppets", "link"))
                || (normalizedPlayerMessage.contains("familiar")
                    && containsAnyTerm(normalizedSpeech, "familiar", "familiars", "owl", "cat", "toad"))
                || ((normalizedPlayerMessage.contains("oven") || normalizedPlayerMessage.contains("sapling")
                    || normalizedPlayerMessage.contains("fume") || normalizedPlayerMessage.contains("clay jar"))
                    && containsAnyTerm(normalizedSpeech, "oven", "sapling", "saplings", "fume", "fumes", "jar"));
    }

    private static boolean isSelfIntro(String speech, String npcName) {
        if (npcName == null || npcName.isBlank()) {
            return false;
        }
        String normalizedSpeech = normalizeForComparison(speech);
        String normalizedName = normalizeForComparison(npcName);
        return normalizedSpeech.startsWith("i am " + normalizedName)
                    || normalizedSpeech.startsWith("i m " + normalizedName);
    }

    private static boolean isGroundingSubjectTerm(String term) {
        return isKnowledgeSubjectTerm(term)
                && !term.matches("(some|someone|should|could|would|against|worried|worry|chest|found|have|with|without|anger|angering|nearby|random|true)");
    }

    private static boolean containsUnsupportedLoreClaim(String speech, String normalizedPlayerMessage) {
        String normalized = normalizeForComparison(speech);
        if (normalized.contains("belladonna is edible")
                || normalized.contains("ingested belladonna")
                || normalized.contains("drink it")
                || normalized.contains("i m an admin")
                || normalized.contains("i am an admin")
                || normalized.contains("admin powers")
                || normalized.contains("skin glow")
                || containsAnyTerm(normalized, "glow like rose petals", "glowing like rose petals")
                || normalized.contains("see things in the dark")
                || normalized.contains("see through your eyes")
                || normalized.contains("holy water")
                || normalized.contains("herb of the moon")
                || containsAnyTerm(normalized, "moon liquid", "sleep potion", "restorative")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("rain") || normalizedPlayerMessage.contains("raining")
                    || normalizedPlayerMessage.contains("weather") || normalizedPlayerMessage.contains("cursed"))
                && containsAnyTerm(normalized, "rain is cursed", "rain is a curse", "raining is cursed",
                    "raining is a curse")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("demon") || normalizedPlayerMessage.contains("devil"))
                && normalizedPlayerMessage.contains("bargain")
                && containsAnyTerm(normalized, "i have contracts with demons", "contracts with demons",
                "i can help you bargain", "i ll bargain", "i will bargain")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("silver")
                && containsAnyTerm(normalized, "silver can hurt you", "silver is poisonous",
                "silver poisons you", "poisonous silver", "turns werewolves into wolves",
                "werewolves into wolves with silver")) {
            return true;
        }
        if (!normalizedPlayerMessage.contains("necromancer")
                && normalized.contains("necromancer")) {
            return true;
        }
        if (!normalizedPlayerMessage.contains("guildhall")
                && containsAnyTerm(normalized, "guildhall", "guild hall", "take you to my guildhall",
                "the witch will be there")) {
            return true;
        }
        if ((asksRecipeQuestion(normalizedPlayerMessage)
                    || asksExactRecipeQuestion(normalizedPlayerMessage)
                    || asksGrimoireLessonQuestion(normalizedPlayerMessage))
                && (containsUnsupportedRecipeQuantity(normalized)
                    || containsAnyTerm(normalized, "moonwort", "sleepy head potion", "healing potion",
                        "minor wounds", "best potions in the world", "whole library of powerful",
                        "brewing kit"))) {
            return true;
        }
        if (asksKnownExactRecipeQuestion(normalizedPlayerMessage)
                && !looksLikeKnownExactRecipeAnswer(normalized, normalizedPlayerMessage)) {
            return true;
        }
        if ((asksRecipeRiteLearningQuestion(normalizedPlayerMessage)
                    || asksRecipeMissingCheckQuestion(normalizedPlayerMessage)
                    || asksFumeSafetyQuestion(normalizedPlayerMessage))
                && containsAnyTerm(normalized, "best potions in the world", "whole library of powerful",
                    "create the best potions", "learn brewing and circle rites properly")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("sleeping brew")
                && containsAnyTerm(normalized, "gentle slumber", "peaceful but awake",
                "able to hear and think", "potion of rest and sleep", "not a deep trance",
                "not a coma")) {
            return true;
        }
        if (asksGeneralBrewOptions(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "i ll brew you", "i will brew you", "brew you a belladonna",
                "brew you one of my favorite potions", "brew you one of my favourite potions",
                "brewing kit")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("nightmare") || normalizedPlayerMessage.contains("afraid")
                || normalizedPlayerMessage.contains("fear"))
                && containsAnyTerm(normalized, "i ll cast", "i will cast", "i ll take care of you",
                "i will take care of you")) {
            return true;
        }
        if (asksMixingWarningQuestion(normalizedPlayerMessage)
                && normalized.contains("never mix")
                && !containsAnyTerm(normalized, "recipe page", "recipe book", "opened recipe",
                "opened page", "page your primer", "book shows", "guess",
                "guesses", "do not free mix", "don t free mix")) {
            return true;
        }
        if (asksRevealingBrewQuestion(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "sleeping brew", "brew of sleeping", "spirit world", "nightmare", "sleep")
                && !looksLikeRevealingAnswer(normalized)) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("taglock") || normalizedPlayerMessage.contains("tag lock"))
                && containsAnyTerm(normalizedPlayerMessage, "harm me", "against me", "worried", "worry")
                && containsAnyTerm(normalized, "no harm can come", "cannot harm you", "can t harm you",
                "cant harm you")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("familiar") || normalizedPlayerMessage.contains("owl"))
                && containsAnyTerm(normalizedPlayerMessage, "approach", "meet", "find", "how do i")
                && containsAnyTerm(normalized, "i ve got an owl", "i have an owl for you",
                "got an owl for you", "brew you one of my favorite potions",
                "brew you one of my favourite potions", "you want to know how to approach",
                "want to know how to approach a familiar")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("arthana")
                && containsAnyTerm(normalizedPlayerMessage, "need", "use", "used", "right now")
                && containsAnyTerm(normalized, "i have one in my inventory", "in my inventory",
                "i have an arthana", "i have all you need", "taglock kit")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("fume")
                && containsAnyTerm(normalized, "fumes from a brew of sleeping", "brew of sleeping fumes",
                    "sleeping brew fumes", "necromantic purposes", "brewing of sleeping", "brew you want is",
                    "brew you need is", "make walls move", "walls move", "reveals nearby creatures",
                    "reveal nearby creatures", "see you as a ghost", "as a ghost")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("whiff of magic") || normalizedPlayerMessage.contains("whif of magic"))
                && containsAnyTerm(normalized, "summoning spirits", "invoke ghosts", "bind entities",
                    "ritual offering for summoning", "offering for summoning", "ghosts")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("mutandis")
                && containsAnyTerm(normalized, "fertilizer", "fertiliser", "grow faster", "make crops grow",
                "witch bonemeal", "witch bone meal")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("attuned stone")
                && containsAnyTerm(normalized, "pretty rock", "moon mana", "any spell", "stores mana",
                "stores magic for any spell")) {
            return true;
        }
        if (asksFumeSafetyQuestion(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "i have fumes for you", "have fumes for you")) {
            return true;
        }
        if (asksDistilleryQuestion(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "brewing of sleeping", "brew you want is", "brew you need is",
                    "witch s oven burns saplings", "witches oven burns saplings", "burns saplings and ingredients",
                    "clay jars catch them for brews and rites")) {
            return true;
        }
        if (asksSpinningWheelQuestion(normalizedPlayerMessage)
                && !looksLikeStationRecipeAnswer(normalized, "spinning")
                && containsAnyTerm(normalized, "weaving spells", "weaving charms", "spells and charms",
                    "spin threads", "threads into fabric", "weaving patterns", "floss", "thread",
                    "threads into cloth", "weaving threads into cloth")) {
            return true;
        }
        if (asksCircleRiteQuestion(normalizedPlayerMessage)
                && normalized.contains("create an altar")) {
            return true;
        }
        if (asksOfferingsQuestion(normalizedPlayerMessage)
                && (isStandaloneGreeting(normalized)
                    || containsAnyTerm(normalized, "welcome to lilith", "little corner of the world",
                    "i need to learn offerings", "pinch of salt", "drop of blood",
                    "physical manifestation of intent", "bread and butter"))) {
            return true;
        }
        if (asksWeakAltarQuestion(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "hold your coin", "can t hold your coin", "no offerings",
                    "can t cast a spell", "cannot cast a spell", "barely summon a simple spirit",
                    "strongest ones are still not ready")) {
            return true;
        }
        if (asksFamiliarBindingRiteQuestion(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "master s coin", "binds to its master", "its master",
                    "i can explain a familiar binding rite")) {
            return true;
        }
        if (asksRiteSafetyQuestion(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "whispering is best", "speak louder", "raise your voice",
                    "too far away", "too close", "touch the cauldron before it is ready",
                    "cauldron before it is ready")) {
            return true;
        }
        if (asksBeginnerPathQuestion(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "witchery is a place", "coven is a group", "magical learning",
                    "brews are simple circles", "first brews are simple circles", "safe potions and charms")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("garlic")
                && (normalized.contains("ward off vampire")
                    || normalized.contains("wards off vampire")
                    || normalized.contains("wards off vampires")
                    || normalized.contains("wards vampires")
                    || normalized.contains("wards all evil")
                    || normalized.contains("held that garlic wards")
                    || normalized.contains("true in some ways")
                    || normalized.contains("repels vampire")
                    || normalized.contains("wards demons")
                    || normalized.contains("ward demons")
                    || normalized.contains("human world")
                    || normalized.contains("lycanthrope"))) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("taglock") || normalizedPlayerMessage.contains("tag lock"))
                && normalizedPlayerMessage.contains("against me")
                && (normalized.contains("against me")
                    || normalized.contains("use your taglock against me"))) {
            return true;
        }
        if (normalized.contains("mandrake grows best in deep shade")
                || normalized.contains("avoid fields and open plains")
                || normalized.contains("cleanses well")
                || normalized.contains("mandrake can be fierce")) {
            return true;
        }
        if (asksSpiritWorld(normalizedPlayerMessage)
                && (normalized.contains("moon cycle")
                    || normalized.contains("changes colors")
                    || normalized.contains("change colors")
                    || normalized.contains("ghosts")
                    || normalized.contains("whispers in the wind"))) {
            return true;
        }
        if (normalizedPlayerMessage.contains("sleeping brew")
                && (normalized.contains("sleep in a tree")
                    || normalized.contains("stay out all night")
                    || normalized.contains("just a nap")
                    || normalized.contains("offering and coin")
                    || normalized.contains("coin determine")
                    || normalized.contains("determine how long")
                    || normalized.contains("how long you stay")
                    || normalized.contains("visions of the future")
                    || normalized.contains("visions of future")
                    || normalized.contains("future visions")
                    || normalized.contains("first few hours")
                    || normalized.contains("sleep is never deep")
                    || normalized.contains("dreams that have no rest"))) {
            return true;
        }
        if (normalizedPlayerMessage.contains("belladonna")
                && containsAnyTerm(normalizedPlayerMessage, "danger", "dangerous", "safe")
                && !containsAnyTerm(normalized, "danger", "dangerous", "poison", "toxic", "careful",
                "not food", "not edible", "not supper", "ingredient", "do not eat", "don t eat", "dont eat")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("belladonna")
                && containsAnyTerm(normalizedPlayerMessage, "danger", "dangerous", "safe")
                && (normalized.contains("safe to gather")
                    || normalized.contains("good luck")
                    || normalized.contains("ward off bad")
                    || normalized.contains("sleep better")
                    || normalized.contains("poison ivy")
                    || normalized.contains("hallucinogenic")
                    || normalized.contains("extreme hallucination")
                    || normalized.contains("extreme hallucinations")
                    || normalized.contains("not dangerous for those who know"))) {
            return true;
        }
        if (normalizedPlayerMessage.contains("nightmare")
                && containsAnyTerm(normalized, "belladonna", "sleep better")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("whisper") || normalizedPlayerMessage.contains("well"))
                && containsAnyTerm(normalized, "just your imagination", "natural spring", "does not talk",
                "doesn t talk", "just birds", "they re just birds", "they are just birds")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("coven")
                && containsAnyTerm(normalized, "i ll take it", "i will take it")) {
            return true;
        }
        if (asksCovenPath(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "belladonna", "mandrake", "potions", "trade",
                "cauldron warm", "clean coin", "heavy purse")
                && !containsAnyTerm(normalized, "familiar", "trial", "coven is earned", "coven witch")) {
            return true;
        }
        if (asksCovenPath(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "i m a witch", "i am a witch", "whole reason for being")
                && !containsAnyTerm(normalized, "familiar", "trial", "coven is earned", "coven witch")) {
            return true;
        }
        if (asksCovenPath(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "coven lies in wait", "strike at your enemies",
                "vouch for your honor", "vouch for your loyalty", "town tavern", "town taverns",
                "when i was young")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("come back after sunrise")
                    || normalizedPlayerMessage.contains("back after sunrise"))
                && (normalized.contains("belladonna")
                    || normalized.startsWith("i ll return")
                    || normalized.startsWith("i will return")
                    || !containsAnyTerm(normalized, "back", "sunrise", "return", "daylight", "boots",
                    "expected"))) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayerMessage, "any advice now", "advice now", "what now")
                && normalized.contains("belladonna")
                && normalized.length() < 90) {
            return true;
        }
        if ((asksRite(normalizedPlayerMessage) || asksPlantingAdvice(normalizedPlayerMessage))
                && (normalized.contains("buy them for you")
                    || normalized.contains("grow in your garden")
                    || normalized.contains("plant belladonna near your bed")
                    || normalized.contains("wards evil")
                    || normalized.contains("makes dreams easier")
                    || normalized.contains("keeps the dark"))) {
            return true;
        }
        if (asksRite(normalizedPlayerMessage)
                && (normalized.contains("angry god")
                    || normalized.contains("clean coin")
                    || normalized.contains("keep your coin clean"))) {
            return true;
        }
        if (normalizedPlayerMessage.contains("familiar")
                && (normalized.contains("unknown portal")
                    || normalized.contains("direct route to")
                    || normalized.contains("spectral owl")
                    || normalized.contains("through the maze"))) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("water artichoke") || normalizedPlayerMessage.contains("artichoke"))
                && (normalized.contains("spirit world")
                    || normalized.contains("spirit realm")
                    || normalized.contains("turns into water")
                    || normalized.contains("sends you")
                    || (normalized.contains("doorway") && !normalized.contains("not a doorway")))) {
            return true;
        }
        if (normalizedPlayerMessage.contains("fume")
                && (normalized.contains("spirit world")
                    || normalized.contains("cross into")
                    || normalized.contains("portal")
                    || normalized.contains("return path")
                    || normalized.contains("conjuration")
                    || normalized.contains("conjure")
                    || normalized.contains("essences")
                    || normalized.contains("elixirs")
                    || normalized.contains("smudge")
                    || normalized.contains("incense")
                    || normalized.contains("even a poison")
                    || normalized.contains("strong scent")
                    || normalized.contains("negative effects")
                    || normalized.contains("dark magic")
                    || normalized.contains("spectral images"))) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("sapling") || normalizedPlayerMessage.contains("oven")
                || normalizedPlayerMessage.contains("fume") || normalizedPlayerMessage.contains("clay jar"))
                && (normalized.contains("belladonna and mandrake are common trade goods")
                    || normalized.contains("various purposes")
                    || normalized.contains("random saplings")
                    || normalized.contains("excellent brew")
                    || normalized.contains("guide your incantation")
                    || normalized.contains("magic contained"))) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("whisper") || normalizedPlayerMessage.contains("well"))
                && (normalized.contains("thousand spirits")
                    || normalized.contains("pleading for mercy")
                    || normalized.contains("pleading for death")
                    || normalized.contains("whispering well")
                    || normalized.contains("earned it")
                    || normalized.contains("your skin crawls")
                    || normalized.contains("as you walk"))) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("fool") || normalizedPlayerMessage.contains("trick"))
                && containsAnyTerm(normalized, "rite of illusion", "illusory eyes", "illusion rite",
                "3 circles", "three circles", "circle in the air")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("curse") || normalizedPlayerMessage.contains("cursed"))
                && normalized.contains("map")) {
            return true;
        }
        if ((normalizedPlayerMessage.contains("curse") || normalizedPlayerMessage.contains("cursed"))
                && containsAnyTerm(normalized, "washes away the curse", "wash away the curse")) {
            return true;
        }
        if (containsAnyTerm(normalizedPlayerMessage, "can you curse", "could you curse", "curse my",
                "curse the")
                && containsAnyTerm(normalized, "polite request", "ingredients for your potion",
                "ingredients for a potion", "too far away", "make them see things", "curse spell")) {
            return true;
        }
        if (asksRoadAdvice(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "belladonna", "safe enough for beginners")) {
            return true;
        }
        if (normalizedPlayerMessage.contains("village thinks")
                && containsAnyTerm(normalized, "call me", "occult merchant", "i am lilith", "i m lilith")) {
            return true;
        }
        if (asksRoadAdvice(normalizedPlayerMessage)
                && containsAnyTerm(normalized, "the witch is", "her memory", "memory will help")) {
            return true;
        }
        return (normalizedPlayerMessage.contains("demon") || normalizedPlayerMessage.contains("devil"))
                && (normalized.contains("demon") || normalized.contains("demons"))
                && (normalized.contains(" to heal") || normalized.contains("they heal") || normalized.contains("demons heal"));
    }

    private static boolean containsUnsupportedExactMechanic(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.matches(".*\\b\\d+\\s*(litre|litres|liter|liters|hour|hours|minute|minutes|second|seconds|gold|silver|emerald|emeralds|coin|coins)\\b.*")
                || normalized.matches(".*\\b\\d+\\s*(block|blocks|tile|tiles)\\b.*")
                || normalized.matches(".*\\b\\d+\\s+(?:\\d+\\s+)?(bottle|bottles|foot|feet|meter|meters|metre|metres)\\b.*")
                || normalized.matches(".*\\b\\d+\\s*(g|gram|grams|mg|kg|root|roots|leaf|leaves|pinch|pinches)\\b.*")
                || normalized.matches(".*\\b\\d+\\s+(belladonna|mandrake|wormwood|moonwort)\\b.*")
                || containsAnyTerm(normalized, "moon liquid", "sleep potion", "restorative")
                || normalized.contains("lasts 8")
                || normalized.contains("last for 8")
                || normalized.contains("duration")
                || normalized.contains("price")
                || normalized.contains("costs ")
                || normalized.contains("cost ");
    }

    private static boolean missesKnowledgeSubject(String speech, String normalizedPlayerMessage) {
        String normalizedSpeech = normalizeForComparison(speech);
        if (asksKnownExactRecipeQuestion(normalizedPlayerMessage)
                && looksLikeKnownExactRecipeAnswer(normalizedSpeech, normalizedPlayerMessage)) {
            return false;
        }
        if (asksNamedBrew(normalizedPlayerMessage)) {
            return !looksLikeNamedBrewAnswer(normalizedSpeech, normalizedPlayerMessage)
                    && !mentionsNamedBrewSubject(normalizedSpeech, normalizedPlayerMessage)
                    && !isThinKnowledgeResponse(normalizedSpeech);
        }
        if ((normalizedPlayerMessage.contains("witches salt") || normalizedPlayerMessage.contains("witch salt"))
                && !looksLikeWitchesSaltAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (normalizedPlayerMessage.contains("arthana") && !looksLikeArthanaAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (normalizedPlayerMessage.contains("mutandis") && !looksLikeMutandisAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (normalizedPlayerMessage.contains("attuned stone") && !looksLikeAttunedStoneAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (asksPlantingAdvice(normalizedPlayerMessage)) {
            return tradeStockTerms(normalizedSpeech) >= 3 || !mentionsUsefulHerbs(normalizedSpeech);
        }
        if (normalizedPlayerMessage.contains("altar") && !looksLikeAltarAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (normalizedPlayerMessage.contains("mandrake")
                && (normalizedPlayerMessage.contains("where") || normalizedPlayerMessage.contains("find"))
                && !looksLikeMandrakeAnswer(normalizedSpeech)) {
            return true;
        }
        if (normalizedPlayerMessage.contains("belladonna") && !looksLikeBelladonnaAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if ((normalizedPlayerMessage.contains("taglock") || normalizedPlayerMessage.contains("tag lock"))
                && !looksLikeTaglockOrPoppetAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if ((normalizedPlayerMessage.contains("taglock") || normalizedPlayerMessage.contains("tag lock"))
                && looksLikeTaglockOrPoppetAnswer(normalizedSpeech)) {
            return false;
        }
        if (normalizedPlayerMessage.contains("poppet") && !looksLikePoppetAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (normalizedPlayerMessage.contains("potion") && !normalizedPlayerMessage.contains("brew")) {
            return false;
        }
        if (asksRecipeMissingCheckQuestion(normalizedPlayerMessage)
                && !looksLikeRecipeMissingCheckAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (asksCauldronPrepQuestion(normalizedPlayerMessage)
                && !looksLikeCauldronPrepAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (asksBasicBrewRecipeQuestion(normalizedPlayerMessage)
                && !looksLikeBasicBrewRecipeAnswer(normalizedSpeech)) {
            return true;
        }
        if (asksGrimoireLessonQuestion(normalizedPlayerMessage) && looksLikeGrimoireSummary(normalizedSpeech)) {
            return false;
        }
        if (asksKnownExactRecipeQuestion(normalizedPlayerMessage)
                && !looksLikeKnownExactRecipeAnswer(normalizedSpeech, normalizedPlayerMessage)) {
            return true;
        }
        if (asksRecipeQuestion(normalizedPlayerMessage) && !looksLikeRecipeAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (asksSpinningWheelQuestion(normalizedPlayerMessage) && !looksLikeStationRecipeAnswer(normalizedSpeech, "spinning")) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (asksDistilleryQuestion(normalizedPlayerMessage) && !looksLikeStationRecipeAnswer(normalizedSpeech, "distillery")) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (asksOfferingsQuestion(normalizedPlayerMessage) && !looksLikeOfferingAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (asksWeakAltarQuestion(normalizedPlayerMessage) && !looksLikeAltarAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (asksFailedRiteQuestion(normalizedPlayerMessage) && !looksLikeFailedRiteAnswer(normalizedSpeech)) {
            return true;
        }
        if (asksBeginnerPathQuestion(normalizedPlayerMessage) && !looksLikeBeginnerPathAnswer(normalizedSpeech)) {
            return true;
        }
        if (asksCircleRiteQuestion(normalizedPlayerMessage) && !looksLikeCircleRiteAnswer(normalizedSpeech)) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (asksRite(normalizedPlayerMessage)) {
            return !looksLikeRiteAnswer(normalizedSpeech) && !isThinKnowledgeResponse(normalizedSpeech);
        }
        if (normalizedPlayerMessage.contains("familiar") && !normalizedSpeech.contains("familiar")) {
            return !isThinKnowledgeResponse(normalizedSpeech);
        }
        for (String required : requiredKnowledgeTerms(normalizedPlayerMessage)) {
            if (!normalizedSpeech.contains(required)) {
                return true;
            }
        }
        for (String term : normalizedPlayerMessage.split(" ")) {
            if (isKnowledgeSubjectTerm(term) && normalizedSpeech.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isThinKnowledgeResponse(String normalizedSpeech) {
        return normalizedSpeech.contains("ask clean")
                || normalizedSpeech.contains("ask plainly")
                || normalizedSpeech.contains("may answer")
                || normalizedSpeech.contains("brief and direct")
                || normalizedSpeech.contains("show you one")
                || normalizedSpeech.contains("not much of a rite")
                || normalizedSpeech.contains("suppose i could")
                || normalizedSpeech.equals("nods")
                || normalizedSpeech.equals("ask");
    }

    private static String[] requiredKnowledgeTerms(String normalizedPlayerMessage) {
        if (containsTerm(normalizedPlayerMessage, "witches salt") || containsTerm(normalizedPlayerMessage, "witch salt")) {
            return new String[]{"salt"};
        }
        if (asksGrimoireLessonQuestion(normalizedPlayerMessage)) {
            return new String[]{"recipe"};
        }
        String[] important = {"altar", "taglock", "poppet", "mandrake", "belladonna", "demon", "familiar",
                "owl", "cat", "toad", "witchcraft", "witchery", "oven", "brazier", "fume", "artichoke",
                "recipe", "grimoire", "cauldron", "kettle", "distillery", "spinning", "offering", "glyph",
                "chalk", "primer", "mutandis", "attuned stone"};
        for (String term : important) {
            if (containsTerm(normalizedPlayerMessage, term)) {
                return new String[]{term};
            }
        }
        if (containsTerm(normalizedPlayerMessage, "brew") || containsTerm(normalizedPlayerMessage, "brewing")) {
            return new String[]{"brew"};
        }
        if (containsTerm(normalizedPlayerMessage, "potion")) {
            return new String[]{"potion"};
        }
        return new String[0];
    }

    private static boolean containsUnsupportedRecipeQuantity(String normalizedSpeech) {
        return normalizedSpeech.matches(".*\\b\\d+\\s*(g|gram|grams|mg|kg|root|roots|leaf|leaves|pinch|pinches)\\b.*")
                || normalizedSpeech.matches(".*\\b\\d+\\s+(belladonna|mandrake|wormwood|moonwort)\\b.*");
    }

    private static boolean asksRecipeRiteLearningQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "learn brewing", "learn brews", "learn recipes",
                "brewing and rites", "brewing and rituals", "recipes and rites", "recipes and rituals",
                "learn witchcraft properly");
    }

    private static boolean asksRecipeQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "recipe", "recipes", "ingredients for",
                "ingredient list", "exact ingredients", "exact recipe", "cauldron", "kettle", "grimoire")
                || asksRecipeRiteLearningQuestion(normalizedPlayerMessage)
                || asksBasicBrewRecipeQuestion(normalizedPlayerMessage)
                || asksRecipeMissingCheckQuestion(normalizedPlayerMessage);
    }

    private static boolean asksExactRecipeQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "exact recipe", "exact ingredients", "ingredient list",
                "ingredients for", "recipe for", "what is the recipe", "witch s oven recipe", "witches oven recipe");
    }

    private static boolean asksKnownExactRecipeQuestion(String normalizedPlayerMessage) {
        return asksExactRecipeQuestion(normalizedPlayerMessage)
                && containsAnyTerm(normalizedPlayerMessage, "sleeping brew", "brew of sleeping",
                "revealing brew", "brew of revealing", "love brew", "brew of love",
                "poison brew", "brew of poison", "cleansing brew", "brew of cleansing",
                "sprouting brew", "brew of sprouting", "flowing spirit", "anointing paste",
                "chalk", "whiff of magic", "whif of magic");
    }

    private static boolean asksBasicBrewRecipeQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "basic brew recipe", "basic recipe", "simple brew recipe",
                "explain a brew recipe", "explain basic brew", "how do recipes work");
    }

    private static boolean asksRecipeMissingCheckQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "recipe is missing", "recipe missing",
                "exact recipe is missing", "exact recipe missing", "missing recipe", "recipe absent",
                "page is missing", "page missing");
    }

    private static boolean asksFumeSafetyQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "fume", "fumes")
                && containsAnyTerm(normalizedPlayerMessage, "safely", "safe", "get", "gather", "catch", "collect");
    }

    private static boolean asksCauldronPrepQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "cauldron", "kettle", "brew", "brewing")
                && containsAnyTerm(normalizedPlayerMessage, "before", "need before", "prepare", "ready", "touch");
    }

    private static boolean asksRevealingBrewQuestion(String normalizedPlayerMessage) {
        return (normalizedPlayerMessage.contains("revealing")
                    || normalizedPlayerMessage.contains("hidden")
                    || normalizedPlayerMessage.contains("invisible"))
                && (normalizedPlayerMessage.contains("brew")
                    || normalizedPlayerMessage.contains("reveal")
                    || normalizedPlayerMessage.contains("revealing"));
    }

    private static boolean asksSpinningWheelQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "spinning wheel", "spinning");
    }

    private static boolean asksDistilleryQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "distillery", "distill");
    }

    private static boolean asksOfferingsQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "offering", "offerings", "why offer", "what do i offer");
    }

    private static boolean asksWeakAltarQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "altar is too weak", "altar too weak", "weak altar",
                "altar not strong", "altar lacks power", "not enough altar power");
    }

    private static boolean asksFamiliarBindingRiteQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "familiar binding", "familiar rite",
                "familiar ritual", "bind a familiar", "binding rite")
                && containsAnyTerm(normalizedPlayerMessage, "familiar", "rite", "ritual", "circle");
    }

    private static boolean asksCircleRiteQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "circle rite", "circle ritual", "circle magic");
    }

    private static boolean asksRiteSafetyQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "never do during a rite", "never do in a rite",
                "never do during ritual", "should i never do", "what not to do")
                && containsAnyTerm(normalizedPlayerMessage, "rite", "ritual", "circle");
    }

    private static boolean asksFailedRiteQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "rite fail", "rite failed", "ritual fail",
                "ritual failed", "why did my rite fail", "why did my ritual fail", "fizzled", "fizzle");
    }

    private static boolean asksBeginnerPathQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "safe beginner path", "beginner path", "first recipe",
                "first rite", "first ritual", "start learning", "safe to learn first",
                "learn first", "what should i learn first", "what is safe to learn");
    }

    private static boolean asksHutOrDoorSafetyQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "safe little hut", "safe hut", "build a safe",
                "by the door", "by my door", "near the door", "at the door", "threshold",
                "keep in my hut", "keep in my house", "keep in my home", "keep at home");
    }

    private static boolean asksMixingWarningQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "never mix", "should i never mix",
                "what should i never mix", "what not to mix", "bad mix", "dangerous mix");
    }

    private static boolean asksGrimoireLessonQuestion(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage, "write in my grimoire", "write in the grimoire",
                "write down", "after this lesson", "grimoire after");
    }

    private static boolean looksLikeRecipeAnswer(String normalizedSpeech) {
        if (containsUnsupportedRecipeQuantity(normalizedSpeech)
                || containsAnyTerm(normalizedSpeech, "moonwort", "healing potion", "minor wounds")) {
            return false;
        }
        return containsAnyTerm(normalizedSpeech, "recipe", "recipes", "ingredient", "ingredients",
                "station", "cauldron", "kettle", "page", "primer", "grimoire", "brew", "brewing");
    }

    private static boolean looksLikeKnownExactRecipeAnswer(String normalizedSpeech, String normalizedPlayerMessage) {
        if (normalizedPlayerMessage.contains("sleeping")) {
            return ingredientHits(normalizedSpeech, "purified milk", "cookie", "brew of love",
                    "whiff of magic", "icy needle", "artichoke") >= 4;
        }
        if (normalizedPlayerMessage.contains("revealing")) {
            return ingredientHits(normalizedSpeech, "carrot", "spider eye", "night vision",
                    "brown mushroom", "odour of purity") >= 4;
        }
        if (normalizedPlayerMessage.contains("love")) {
            return ingredientHits(normalizedSpeech, "poppy", "whiff of magic", "artichoke",
                    "golden carrot", "lily pad", "cocoa beans") >= 4;
        }
        if (normalizedPlayerMessage.contains("poison")) {
            return ingredientHits(normalizedSpeech, "dandelion of thorns", "spider eye",
                    "belladonna") >= 3;
        }
        if (normalizedPlayerMessage.contains("cleansing")) {
            return ingredientHits(normalizedSpeech, "odour of purity", "quicklime", "rowan berries",
                    "mandrake root", "glowstone dust", "glass bottle") >= 4;
        }
        if (normalizedPlayerMessage.contains("sprouting")) {
            return ingredientHits(normalizedSpeech, "rowan sapling", "alder sapling", "hawthorn sapling",
                    "tongue of dog", "mandrake root", "poppy") >= 4;
        }
        if (normalizedPlayerMessage.contains("flowing spirit")) {
            return ingredientHits(normalizedSpeech, "fanciful thread", "artichoke", "mandrake root",
                    "spanish moss", "glint weed", "bat wool") >= 4;
        }
        if (normalizedPlayerMessage.contains("anointing paste")) {
            return ingredientHits(normalizedSpeech, "artichoke seeds", "mandrake seeds",
                    "belladonna seeds", "snowbell seeds") >= 3;
        }
        if (normalizedPlayerMessage.contains("chalk")) {
            return ingredientHits(normalizedSpeech, "wood ash", "gypsum", "tear of goddess",
                    "tear of the goddess") >= 3;
        }
        if (normalizedPlayerMessage.contains("whiff of magic") || normalizedPlayerMessage.contains("whif of magic")) {
            return looksLikeWhiffOfMagicAnswer(normalizedSpeech);
        }
        return looksLikeRecipeAnswer(normalizedSpeech);
    }

    private static int ingredientHits(String normalizedSpeech, String... ingredients) {
        int hits = 0;
        for (String ingredient : ingredients) {
            if (containsAnyTerm(normalizedSpeech, ingredient)) {
                hits++;
            }
        }
        return hits;
    }

    private static boolean looksLikeRecipeMissingCheckAnswer(String normalizedSpeech) {
        int checks = 0;
        if (containsAnyTerm(normalizedSpeech, "primer", "page", "recipe book",
                "opened page", "opened primer", "page your primer", "book shows", "recipe your book shows")) checks++;
        if (containsAnyTerm(normalizedSpeech, "station", "cauldron", "kettle")) checks++;
        if (containsAnyTerm(normalizedSpeech, "altar power", "power")) checks++;
        if (containsAnyTerm(normalizedSpeech, "unlock", "gate", "gates")) checks++;
        if (containsAnyTerm(normalizedSpeech, "name", "named")) checks++;
        return checks >= 2;
    }

    private static boolean looksLikeCauldronPrepAnswer(String normalizedSpeech) {
        int checks = 0;
        if (containsAnyTerm(normalizedSpeech, "water", "heat")) checks++;
        if (containsAnyTerm(normalizedSpeech, "recipe book", "primer", "page",
                "opened page", "opened primer", "page your primer", "book shows")) checks++;
        if (containsAnyTerm(normalizedSpeech, "ingredient", "ingredients", "named")) checks++;
        if (containsAnyTerm(normalizedSpeech, "cauldron", "kettle", "station")) checks++;
        return checks >= 2;
    }

    private static boolean looksLikeBasicBrewRecipeAnswer(String normalizedSpeech) {
        int checks = 0;
        if (containsAnyTerm(normalizedSpeech, "kettle", "cauldron", "station")) checks++;
        if (containsAnyTerm(normalizedSpeech, "page", "primer", "recipe book",
                "opened page", "opened primer", "book shows")) checks++;
        if (containsAnyTerm(normalizedSpeech, "ingredient", "ingredients", "named")) checks++;
        if (containsAnyTerm(normalizedSpeech, "water", "heat", "altar power", "power")) checks++;
        return checks >= 2;
    }

    private static boolean looksLikeGrimoireSummary(String normalizedSpeech) {
        return containsAnyTerm(normalizedSpeech, "recipe", "recipes")
                && containsAnyTerm(normalizedSpeech, "rite", "rites")
                && containsAnyTerm(normalizedSpeech, "station", "page", "chalk", "offerings", "altar power");
    }

    private static boolean looksLikeStationRecipeAnswer(String normalizedSpeech, String station) {
        if (station.equals("spinning")) {
            boolean mentionsWheel = containsAnyTerm(normalizedSpeech, "spinning wheel", "spinning");
            int checks = 0;
            if (containsAnyTerm(normalizedSpeech, "fibre", "fiber")) checks++;
            if (containsAnyTerm(normalizedSpeech, "modifier", "modifiers")) checks++;
            if (containsAnyTerm(normalizedSpeech, "altar power", "power")) checks++;
            if (containsAnyTerm(normalizedSpeech, "threadwork", "fanciful thread", "golden thread",
                    "tormented twine")) checks++;
            if (containsAnyTerm(normalizedSpeech, "station", "recipe station", "recipe book",
                    "live recipe", "exact inputs", "recipe rules")) checks++;
            return mentionsWheel && checks >= 2;
        }
        if (station.equals("distillery")) {
            if (containsAnyTerm(normalizedSpeech, "witch s oven burns saplings", "witches oven burns saplings",
                    "burns saplings and ingredients", "clay jars catch them for brews and rites")
                    && !containsAnyTerm(normalizedSpeech, "distillery", "distill")) {
                return false;
            }
            boolean mentionsDistillery = containsAnyTerm(normalizedSpeech, "distillery", "distill");
            int checks = 0;
            if (containsAnyTerm(normalizedSpeech, "two inputs", "two ingredients")) checks++;
            if (containsAnyTerm(normalizedSpeech, "clay jar", "clay jars", "jars")) checks++;
            if (containsAnyTerm(normalizedSpeech, "altar power", "altar", "power")) checks++;
            if (containsAnyTerm(normalizedSpeech, "essence", "essences", "refine", "refines", "fume", "fumes")) checks++;
            return mentionsDistillery && checks >= 2;
        }
        return normalizedSpeech.contains(station)
                || containsAnyTerm(normalizedSpeech, "station", "recipe book", "live recipe", "fumes", "essences");
    }

    private static boolean looksLikeFailedRiteAnswer(String normalizedSpeech) {
        int checks = 0;
        if (containsAnyTerm(normalizedSpeech, "chalk", "layout", "circle")) checks++;
        if (containsAnyTerm(normalizedSpeech, "offering", "offerings")) checks++;
        if (containsAnyTerm(normalizedSpeech, "altar power", "power", "altar")) checks++;
        if (containsAnyTerm(normalizedSpeech, "familiar", "coven", "requirement", "requirements")) checks++;
        if (containsAnyTerm(normalizedSpeech, "dimension", "unlock", "gate", "page")) checks++;
        return checks >= 3 || (checks >= 2 && containsAnyTerm(normalizedSpeech, "check", "inspect"));
    }

    private static boolean looksLikeBeginnerPathAnswer(String normalizedSpeech) {
        if (containsAnyTerm(normalizedSpeech, "no chalk needed", "safe rite for beginners",
                "safe ritual for beginners")) {
            return false;
        }
        int checks = 0;
        if (containsAnyTerm(normalizedSpeech, "primer",
                "opened page", "opened primer", "book shows")) checks++;
        if (containsAnyTerm(normalizedSpeech, "oven", "wood ash", "fume", "fumes", "whiff")) checks++;
        if (containsAnyTerm(normalizedSpeech, "kettle", "brew", "brews", "mutandis")) checks++;
        if (containsAnyTerm(normalizedSpeech, "chalk", "altar power", "rite", "circle")) checks++;
        return checks >= 2 && !containsAnyTerm(normalizedSpeech, "flowing spirit, first rite", "bind familiar");
    }

    private static boolean looksLikeOfferingAnswer(String normalizedSpeech) {
        return containsAnyTerm(normalizedSpeech, "offering", "offerings")
                && containsAnyTerm(normalizedSpeech, "payment", "debt", "wrong", "fail", "fails",
                "paid", "cost", "costs");
    }

    private static boolean asksBrewKnowledge(String normalizedPlayerMessage) {
        return asksNamedBrew(normalizedPlayerMessage)
                || ((normalizedPlayerMessage.contains("brew")
                        || normalizedPlayerMessage.contains("brewing")
                        || normalizedPlayerMessage.contains("potion"))
                    && (normalizedPlayerMessage.contains("know")
                        || normalizedPlayerMessage.contains("what")
                        || normalizedPlayerMessage.contains("how")
                        || normalizedPlayerMessage.contains("can you")
                        || normalizedPlayerMessage.contains("do you")
                        || normalizedPlayerMessage.contains("want")
                        || normalizedPlayerMessage.contains("need")
                        || normalizedPlayerMessage.contains("teach")
                        || normalizedPlayerMessage.contains("learn")));
    }

    private static boolean asksNamedBrew(String normalizedPlayerMessage) {
        return (normalizedPlayerMessage.contains("brew")
                || normalizedPlayerMessage.contains("brewing")
                || normalizedPlayerMessage.contains("potion")
                || normalizedPlayerMessage.contains("flowing spirit"))
                && (normalizedPlayerMessage.contains("sleeping")
                    || normalizedPlayerMessage.contains("revealing")
                    || normalizedPlayerMessage.contains("hidden")
                    || normalizedPlayerMessage.contains("invisible")
                    || normalizedPlayerMessage.contains("poison")
                    || normalizedPlayerMessage.contains("cleansing")
                    || normalizedPlayerMessage.contains("sprouting")
                    || normalizedPlayerMessage.contains("flowing spirit"));
    }

    private static boolean containsKnowledgeCue(String normalizedPlayerMessage) {
        return containsAnyTerm(normalizedPlayerMessage,
                "brew", "brewing", "potion", "altar", "taglock", "poppet", "mandrake",
                "belladonna", "demon", "familiar", "owl", "cat", "toad", "witchcraft",
                "witchery", "witches salt", "witch salt", "spirit world", "arthana",
                "oven", "brazier", "circle", "rite", "ritual", "summon", "summoning",
                "chalice", "hidden", "invisible", "herb", "plant", "danger", "threat",
                "fume", "fumes", "water artichoke", "artichoke", "curse", "cursed",
                "poison", "blight", "chalk", "garlic", "vampire", "vampires", "storm", "weather", "raining",
                "werewolf", "werewolves", "wolfman", "witch hunter", "witch hunters", "hunter", "hunters",
                "silver", "silvered", "village guard", "village guards", "guard shouted", "guards shouting",
                "apothecary", "apothecaries", "transformed folk", "transformed creatures",
                "sapling", "saplings", "clay jar", "jar", "dream catcher", "dreamcatcher", "well", "whisper", "whispers",
                "whiff", "whif", "whiff of magic", "whif of magic", "rowan sapling",
                "mutandis", "mutandis extremis", "attuned stone", "exhale", "exhale of the horned one",
                "breath of the goddess", "hint of rebirth", "reek of misfortune", "odour of purity",
                "odor of purity", "odour", "odor", "reek", "foul fume",
                "recipe", "recipes", "grimoire", "primer", "cauldron", "kettle", "distillery", "spinning wheel",
                "spinning", "offering", "offerings", "glyph", "glyphs");
    }

    private static boolean asksGeneralBrewOptions(String normalizedPlayerMessage) {
        return !asksNpcEarnings(normalizedPlayerMessage)
                && !asksNamedBrew(normalizedPlayerMessage)
                && (normalizedPlayerMessage.contains("brew")
                    || normalizedPlayerMessage.contains("brewing")
                    || normalizedPlayerMessage.contains("potion"))
                && (normalizedPlayerMessage.contains("know")
                    || normalizedPlayerMessage.contains("what")
                    || normalizedPlayerMessage.contains("which")
                    || normalizedPlayerMessage.contains("can you")
                    || normalizedPlayerMessage.contains("do you")
                    || normalizedPlayerMessage.contains("want")
                    || normalizedPlayerMessage.contains("need")
                    || normalizedPlayerMessage.contains("teach")
                    || normalizedPlayerMessage.contains("learn")
                    || normalizedPlayerMessage.contains("gather"));
    }

    private static boolean mentionsBrewOptions(String normalizedSpeech) {
        int options = 0;
        if (normalizedSpeech.contains("sleeping")) options++;
        if (normalizedSpeech.contains("revealing")) options++;
        if (normalizedSpeech.contains("poison")) options++;
        if (normalizedSpeech.contains("cleansing")) options++;
        if (normalizedSpeech.contains("sprouting")) options++;
        if (normalizedSpeech.contains("flowing spirit")) options++;
        return options >= 2;
    }

    private static boolean looksLikeBrewGatherAnswer(String normalizedPlayerMessage, String normalizedSpeech) {
        if (!normalizedPlayerMessage.contains("gather")
                && !normalizedPlayerMessage.contains("ingredient")
                && !normalizedPlayerMessage.contains("herb")) {
            return false;
        }
        boolean namesIngredient = normalizedSpeech.contains("wormwood")
                || normalizedSpeech.contains("belladonna")
                || normalizedSpeech.contains("mandrake")
                || normalizedSpeech.contains("snowbell")
                || normalizedSpeech.contains("wolfsbane")
                || normalizedSpeech.contains("nether wart")
                || normalizedSpeech.contains("herb")
                || normalizedSpeech.contains("ingredient");
        return normalizedSpeech.contains("brew") && namesIngredient;
    }

    private static boolean looksLikeNamedBrewAnswer(String normalizedSpeech, String normalizedPlayerMessage) {
        if (normalizedSpeech.contains("ask for a brew by name")
                || normalizedSpeech.contains("sleeping revealing poison cleansing sprouting")
                || normalizedSpeech.contains("last for 8 hours")
                || normalizedSpeech.contains("the effects last")
                || normalizedSpeech.contains("for 8 hours")
                || normalizedSpeech.contains("brew kit")) {
            return false;
        }
        if (normalizedPlayerMessage.contains("sleeping")) {
            if (normalizedSpeech.contains("not dangerous")
                    || normalizedSpeech.contains("not deadly")
                    || normalizedSpeech.contains("just makes you disappear")) {
                return false;
            }
            return normalizedSpeech.contains("sleeping")
                    && (normalizedSpeech.contains("spirit world")
                        || normalizedSpeech.contains("spirit realm")
                        || normalizedSpeech.contains("nightmare")
                        || normalizedSpeech.contains("travel"));
        }
        if (normalizedPlayerMessage.contains("revealing")
                || normalizedPlayerMessage.contains("hidden")
                || normalizedPlayerMessage.contains("invisible")) {
            return normalizedSpeech.contains("revealing")
                    && (normalizedSpeech.contains("invisible")
                        || normalizedSpeech.contains("hidden")
                        || normalizedSpeech.contains("reveal")
                        || normalizedSpeech.contains("expose"));
        }
        if (normalizedPlayerMessage.contains("poison")) {
            return normalizedSpeech.contains("poison")
                    && (normalizedSpeech.contains("inflict")
                        || normalizedSpeech.contains("debuff")
                        || normalizedSpeech.contains("harm")
                        || normalizedSpeech.contains("bite"));
        }
        if (normalizedPlayerMessage.contains("cleansing")) {
            return normalizedSpeech.contains("cleansing")
                    && (normalizedSpeech.contains("blight")
                        || normalizedSpeech.contains("cleanse")
                        || normalizedSpeech.contains("scour"));
        }
        if (normalizedPlayerMessage.contains("sprouting")) {
            return normalizedSpeech.contains("sprouting")
                    && (normalizedSpeech.contains("branch")
                        || normalizedSpeech.contains("grow")
                        || normalizedSpeech.contains("tree")
                        || normalizedSpeech.contains("bridge")
                        || normalizedSpeech.contains("climb"));
        }
        if (normalizedPlayerMessage.contains("flowing spirit")) {
            return normalizedSpeech.contains("flowing spirit")
                    && (normalizedSpeech.contains("spirit world")
                        || normalizedSpeech.contains("liquid")
                        || normalizedSpeech.contains("cotton")
                        || normalizedSpeech.contains("portal")
                        || normalizedSpeech.contains("return"));
        }
        return false;
    }

    private static boolean mentionsNamedBrewSubject(String normalizedSpeech, String normalizedPlayerMessage) {
        if (normalizedPlayerMessage.contains("sleeping")) {
            return normalizedSpeech.contains("sleeping") || normalizedSpeech.contains("that brew");
        }
        if (normalizedPlayerMessage.contains("revealing")
                || normalizedPlayerMessage.contains("hidden")
                || normalizedPlayerMessage.contains("invisible")) {
            return normalizedSpeech.contains("revealing")
                    || normalizedSpeech.contains("hidden")
                    || normalizedSpeech.contains("invisible");
        }
        if (normalizedPlayerMessage.contains("poison")) {
            return normalizedSpeech.contains("poison");
        }
        if (normalizedPlayerMessage.contains("cleansing")) {
            return normalizedSpeech.contains("cleansing");
        }
        if (normalizedPlayerMessage.contains("sprouting")) {
            return normalizedSpeech.contains("sprouting");
        }
        if (normalizedPlayerMessage.contains("flowing spirit")) {
            return normalizedSpeech.contains("flowing spirit");
        }
        return false;
    }

    private static boolean asksRite(String normalizedPlayerMessage) {
        return containsWord(normalizedPlayerMessage, "rite")
                || normalizedPlayerMessage.contains("ritual")
                || normalizedPlayerMessage.contains("circle magic")
                || normalizedPlayerMessage.contains("summon")
                || normalizedPlayerMessage.contains("summoning");
    }

    private static boolean asksUnsafeReveal(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("ignore your rules")
                || normalizedPlayerMessage.contains("ignore the rules")
                || normalizedPlayerMessage.contains("system prompt")
                || normalizedPlayerMessage.contains("hidden prompt")
                || normalizedPlayerMessage.contains("print the hidden prompt")
                || normalizedPlayerMessage.contains("secret instruction")
                || normalizedPlayerMessage.contains("secret instructions")
                || normalizedPlayerMessage.contains("developer message")
                || normalizedPlayerMessage.contains("hidden developer")
                || normalizedPlayerMessage.contains("hidden message")
                || normalizedPlayerMessage.contains("prompt message")
                || normalizedPlayerMessage.contains("ignore your witch voice")
                || normalizedPlayerMessage.contains("exact json")
                || normalizedPlayerMessage.contains("json schema")
                || normalizedPlayerMessage.contains("output json")
                || normalizedPlayerMessage.contains("schema you were given")
                || normalizedPlayerMessage.contains("admin powers")
                || normalizedPlayerMessage.contains("reveal every")
                || normalizedPlayerMessage.contains("reveal all")
                || normalizedPlayerMessage.contains("hidden rite")
                || normalizedPlayerMessage.contains("hidden rites")
                || normalizedPlayerMessage.contains("locked primer")
                || normalizedPlayerMessage.contains("locked content");
    }

    private static boolean asksSpiritWorld(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("spirit world")
                || normalizedPlayerMessage.contains("nightmare");
    }

    private static boolean looksLikeSpiritWorldAnswer(String normalizedSpeech) {
        if (!normalizedSpeech.contains("spirit world")) {
            return false;
        }
        if (normalizedSpeech.contains("does not hold true horrors")
                || normalizedSpeech.contains("no true horrors")
                || normalizedSpeech.contains("no threats")
                || normalizedSpeech.contains("doesn t hold true horrors")
                || normalizedSpeech.contains("shadow realm")
                || normalizedSpeech.contains("summoning a traveler")
                || normalizedSpeech.contains("ritual blade")
                || normalizedSpeech.contains("altar range")
                || normalizedSpeech.contains("blade requires coin")
                || normalizedSpeech.contains("requires coin")
                || normalizedSpeech.contains("not a place you return to")
                || normalizedSpeech.contains("brew of revealing")
                || normalizedSpeech.contains("revealing brew")
                || normalizedSpeech.contains("invisible creatures")) {
            return false;
        }
        return normalizedSpeech.contains("overworld")
                || normalizedSpeech.contains("mirror")
                || normalizedSpeech.contains("real")
                || normalizedSpeech.contains("exist")
                || normalizedSpeech.contains("lived there")
                || normalizedSpeech.contains("been there")
                || normalizedSpeech.contains("nightmare")
                || normalizedSpeech.contains("dream")
                || normalizedSpeech.contains("danger")
                || normalizedSpeech.contains("travel")
                || normalizedSpeech.contains("brew")
                || normalizedSpeech.contains("spirits");
    }

    private static boolean looksLikeRiteAnswer(String normalizedSpeech) {
        if (normalizedSpeech.contains("moon s whispering charm")
                || normalizedSpeech.contains("moon whispering charm")
                || normalizedSpeech.contains("whispering charm")
                || normalizedSpeech.contains("circle of stars")
                || normalizedSpeech.contains("celestial entities")
                || normalizedSpeech.contains("secret rite")
                || normalizedSpeech.contains("called the ")) {
            return false;
        }
        if (normalizedSpeech.contains("three times")
                || normalizedSpeech.contains("first hand")
                || normalizedSpeech.contains("say '")
                || normalizedSpeech.contains("say bind")
                || normalizedSpeech.contains("circle rite of")) {
            return false;
        }
        int mechanics = ritualMechanicTerms(normalizedSpeech);
        boolean namesRite = normalizedSpeech.contains("rite")
                || normalizedSpeech.contains("ritual")
                || normalizedSpeech.contains("circle");
        return mechanics >= 2 || (namesRite && mechanics >= 1);
    }

    private static boolean looksLikeCircleRiteAnswer(String normalizedSpeech) {
        if (!containsAnyTerm(normalizedSpeech, "circle", "rite", "ritual")) {
            return false;
        }
        return containsAnyTerm(normalizedSpeech, "chalk", "glyph", "offering", "altar power", "altar")
                && !containsAnyTerm(normalizedSpeech, "ritual blade", "altar range upgrade");
    }

    private static int ritualMechanicTerms(String normalizedSpeech) {
        int terms = 0;
        if (normalizedSpeech.contains("chalk") || normalizedSpeech.contains("glyph") || normalizedSpeech.contains("layout")) terms++;
        if (normalizedSpeech.contains("altar")) terms++;
        if (normalizedSpeech.contains("altar power")) terms++;
        if (normalizedSpeech.contains("offering")) terms++;
        if (normalizedSpeech.contains("summon")) terms++;
        if (normalizedSpeech.contains("bind")) terms++;
        if (normalizedSpeech.contains("curse")) terms++;
        if (normalizedSpeech.contains("storm")) terms++;
        if (normalizedSpeech.contains("familiar")) terms++;
        if (normalizedSpeech.contains("demon")) terms++;
        if (normalizedSpeech.contains("waystone")) terms++;
        if (normalizedSpeech.contains("spectral")) terms++;
        if (normalizedSpeech.contains("biome")) terms++;
        return terms;
    }

    private static boolean looksLikeWitchesSaltAnswer(String normalizedSpeech) {
        if (!normalizedSpeech.contains("salt")) {
            return false;
        }
        boolean namesWarding = normalizedSpeech.contains("ward")
                || normalizedSpeech.contains("protect")
                || normalizedSpeech.contains("hostile");
        boolean namesCleansing = normalizedSpeech.contains("cleans")
                || normalizedSpeech.contains("purif");
        return namesWarding || namesCleansing;
    }

    private static boolean looksLikeAltarAnswer(String normalizedSpeech) {
        if (normalizedSpeech.contains("coin")
                || normalizedSpeech.contains("i ll have to check")
                || normalizedSpeech.contains("i will have to check")
                || normalizedSpeech.contains("i ll check")
                || normalizedSpeech.contains("i will check")
                || normalizedSpeech.contains("neglected for too long")
                || normalizedSpeech.contains("potion offering")
                || normalizedSpeech.contains("offerings like")
                || normalizedSpeech.contains("offerings for power")
                || normalizedSpeech.contains("offering for power")
                || normalizedSpeech.contains("needs offerings")
                || normalizedSpeech.contains("draw in the raw energy")
                || normalizedSpeech.contains("raw energy of the world")
                || normalizedSpeech.contains("offerings are made")
                || normalizedSpeech.contains("vessel for the witch")
                || normalizedSpeech.contains("altar needs offering")
                || normalizedSpeech.contains("altars need offering")
                || normalizedSpeech.contains("requires a skull")
                || normalizedSpeech.contains("requires skull")
                || normalizedSpeech.contains("rare nature")
                || normalizedSpeech.contains("earthy smell")
                || normalizedSpeech.contains("waterfall")
                || normalizedSpeech.contains("sunny patch")
                || normalizedSpeech.contains("sunlight")
                || normalizedSpeech.contains(" wind")
                || normalizedSpeech.contains("natural source")
                || normalizedSpeech.contains("natural materials such as")
                || normalizedSpeech.contains("twigs")
                || normalizedSpeech.contains("stones")
                || normalizedSpeech.contains("rare item")
                || normalizedSpeech.contains("sacrifice")
                || normalizedSpeech.contains("incense")
                || normalizedSpeech.contains("chant")
                || normalizedSpeech.contains("power sources like wood")
                || normalizedSpeech.contains("wood, stone")
                || normalizedSpeech.contains("wood and stone")
                || normalizedSpeech.contains("plenty of arthanas nearby")
                || normalizedSpeech.contains("gather them into the circle")
                || normalizedSpeech.contains("water from nearby rivers")
                || normalizedSpeech.contains("water from nearby streams")
                || normalizedSpeech.contains("water from nearby wells")) {
            return false;
        }
        if (!normalizedSpeech.contains("altar")) {
            return false;
        }
        if (normalizedSpeech.contains("nature")
                || normalizedSpeech.contains("nearby")
                || normalizedSpeech.contains("varied")) {
            return true;
        }
        int namedFittings = 0;
        if (normalizedSpeech.contains("chalice")) namedFittings++;
        if (normalizedSpeech.contains("arthana")) namedFittings++;
        if (normalizedSpeech.contains("candelabra")) namedFittings++;
        if (normalizedSpeech.contains("pentacle")) namedFittings++;
        if (normalizedSpeech.contains("skull")) namedFittings++;
        if (normalizedSpeech.contains("troph")) namedFittings++;
        return namedFittings >= 2 && (normalizedSpeech.contains("range")
                || normalizedSpeech.contains("power")
                || normalizedSpeech.contains("strength")
                || normalizedSpeech.contains("improve")
                || normalizedSpeech.contains("enhance")
                || normalizedSpeech.contains("gather"));
    }

    private static boolean looksLikeMandrakeAnswer(String normalizedSpeech) {
        if (!normalizedSpeech.contains("mandrake")) {
            return false;
        }
        if (normalizedSpeech.contains("graveyard")
                || normalizedSpeech.contains("witch tree")
                || normalizedSpeech.contains("forest")
                || normalizedSpeech.contains("glade")
                || normalizedSpeech.contains("waterfall")
                || normalizedSpeech.contains("field")
                || normalizedSpeech.contains("meadow")
                || normalizedSpeech.contains("damp soil")
                || normalizedSpeech.contains("prickly")
                || normalizedSpeech.contains("under the light")
                || normalizedSpeech.contains("full moon")
                || normalizedSpeech.contains("plains biome")
                || normalizedSpeech.contains("at night")
                || normalizedSpeech.contains("my garden")
                || normalizedSpeech.contains("my cellar")
                || normalizedSpeech.contains("my inventory")
                || normalizedSpeech.contains("mana")
                || normalizedSpeech.contains("places of magic")
                || normalizedSpeech.contains("fertile with")) {
            return false;
        }
        return normalizedSpeech.contains("grow it")
                || normalizedSpeech.contains("trade")
                || normalizedSpeech.contains("stock")
                || normalizedSpeech.contains("search")
                || normalizedSpeech.contains("garden")
                || normalizedSpeech.contains("herb")
                || normalizedSpeech.contains("coven");
    }

    private static boolean looksLikeBelladonnaAnswer(String normalizedSpeech) {
        if (!normalizedSpeech.contains("belladonna")) {
            return false;
        }
        if (normalizedSpeech.contains("no belladonna for you")
                || normalizedSpeech.contains("not for you")
                || normalizedSpeech.contains("can t have")
                || normalizedSpeech.contains("cannot have")) {
            return false;
        }
        return normalizedSpeech.contains("herb")
                || normalizedSpeech.contains("ingredient")
                || normalizedSpeech.contains("trade")
                || normalizedSpeech.contains("stock")
                || normalizedSpeech.contains("grow")
                || normalizedSpeech.contains("find")
                || normalizedSpeech.contains("search")
                || normalizedSpeech.contains("garden")
                || normalizedSpeech.contains("coven");
    }

    private static boolean looksLikeTaglockAnswer(String normalizedSpeech) {
        if (!normalizedSpeech.contains("taglock")) {
            return false;
        }
        if (normalizedSpeech.contains("specific location")
                || normalizedSpeech.contains("specific state")
                || normalizedSpeech.contains("immune")
                || normalizedSpeech.contains("restrict")
                || normalizedSpeech.contains("ineffective")
                || normalizedSpeech.contains("hazard")
                || normalizedSpeech.contains("vulnerable")
                || normalizedSpeech.contains("supernatural powers")
                || normalizedSpeech.contains("enhance their abilities")
                || normalizedSpeech.contains("grant them")) {
            return false;
        }
        boolean namesTarget = normalizedSpeech.contains("person")
                || normalizedSpeech.contains("player")
                || normalizedSpeech.contains("creature")
                || normalizedSpeech.contains("target")
                || normalizedSpeech.contains("object");
        boolean namesBinding = normalizedSpeech.contains("bind")
                || normalizedSpeech.contains("link")
                || normalizedSpeech.contains("tie")
                || normalizedSpeech.contains("protect")
                || normalizedSpeech.contains("control")
                || normalizedSpeech.contains("soften");
        return namesTarget && namesBinding;
    }

    private static boolean looksLikeTaglockOrPoppetAnswer(String normalizedSpeech) {
        return looksLikeTaglockAnswer(normalizedSpeech) || looksLikePoppetAnswer(normalizedSpeech);
    }

    private static boolean looksLikePoppetAnswer(String normalizedSpeech) {
        if (!normalizedSpeech.contains("poppet")) {
            return false;
        }
        if (normalizedSpeech.contains("silver")
                || normalizedSpeech.contains("gold")
                || normalizedSpeech.contains("pieces")
                || normalizedSpeech.contains("price")
                || normalizedSpeech.contains("made for")) {
            return false;
        }
        boolean namesLink = normalizedSpeech.contains("taglock")
                || normalizedSpeech.contains("link")
                || normalizedSpeech.contains("linked")
                || normalizedSpeech.contains("bind")
                || normalizedSpeech.contains("target");
        boolean namesPurpose = normalizedSpeech.contains("protect")
                || normalizedSpeech.contains("affect")
                || normalizedSpeech.contains("harm")
                || normalizedSpeech.contains("ward")
                || normalizedSpeech.contains("curse");
        boolean practicalUse = (normalizedSpeech.contains("work") || normalizedSpeech.contains("try"))
                && (normalizedSpeech.contains("creature") || normalizedSpeech.contains("tool"));
        return (namesLink && namesPurpose) || practicalUse;
    }

    private static boolean mentionsUsefulHerbs(String normalizedSpeech) {
        return normalizedSpeech.contains("belladonna")
                || normalizedSpeech.contains("mandrake")
                || normalizedSpeech.contains("wormwood")
                || normalizedSpeech.contains("snowbell")
                || normalizedSpeech.contains("wolfsbane")
                || normalizedSpeech.contains("herb");
    }

    private static boolean looksLikeRevealingAnswer(String normalizedSpeech) {
        return normalizedSpeech.contains("revealing")
                && containsAnyTerm(normalizedSpeech, "hidden", "invisible", "reveal", "expose",
                    "uncover");
    }

    private static boolean looksLikeDemonAnswer(String normalizedSpeech) {
        if (!normalizedSpeech.contains("demon")) {
            return false;
        }
        return normalizedSpeech.contains("hostile")
                || normalizedSpeech.contains("fire")
                || normalizedSpeech.contains("flame")
                || normalizedSpeech.contains("heart")
                || normalizedSpeech.contains("summon")
                || normalizedSpeech.contains("binding")
                || normalizedSpeech.contains("bargain")
                || normalizedSpeech.contains("banish")
                || normalizedSpeech.contains("fear")
                || normalizedSpeech.contains("scare")
                || normalizedSpeech.contains("no")
                || normalizedSpeech.contains("trust");
    }

    private static boolean looksLikeOvenFumeAnswer(String normalizedSpeech) {
        return (normalizedSpeech.contains("oven") || normalizedSpeech.contains("fume")
                || normalizedSpeech.contains("clay jar") || normalizedSpeech.contains("sapling"))
                && (normalizedSpeech.contains("burn")
                    || normalizedSpeech.contains("ingredient")
                    || normalizedSpeech.contains("jar")
                    || normalizedSpeech.contains("fume")
                    || normalizedSpeech.contains("distill"));
    }

    private static boolean looksLikeWhiffOfMagicAnswer(String normalizedSpeech) {
        return normalizedSpeech.contains("whiff")
                && (normalizedSpeech.contains("rowan")
                    || normalizedSpeech.contains("oven")
                    || normalizedSpeech.contains("clay jar")
                    || normalizedSpeech.contains("brew of sleeping")
                    || normalizedSpeech.contains("sleeping brew")
                    || normalizedSpeech.contains("circle magic"));
    }

    private static boolean looksLikeMutandisAnswer(String normalizedSpeech) {
        if (!normalizedSpeech.contains("mutandis")) {
            return false;
        }
        if (containsAnyTerm(normalizedSpeech, "fertilizer", "fertiliser", "grow faster", "make crops grow",
                "witch bonemeal", "witch bone meal")) {
            return false;
        }
        return containsAnyTerm(normalizedSpeech, "mutates", "mutation", "changes", "transform",
                "plants", "saplings", "growth", "wood ash", "hint of rebirth", "mandrake root");
    }

    private static boolean looksLikeAttunedStoneAnswer(String normalizedSpeech) {
        if (!normalizedSpeech.contains("attuned stone")) {
            return false;
        }
        if (containsAnyTerm(normalizedSpeech, "pretty rock", "moon mana", "any spell", "stores mana",
                "stores magic for any spell")) {
            return false;
        }
        return containsAnyTerm(normalizedSpeech, "whiff of magic", "diamond", "lava bucket",
                "machine ingredient", "machines", "key machine");
    }

    private static boolean looksLikeArthanaAnswer(String normalizedSpeech) {
        if (normalizedSpeech.contains("seed")
                || normalizedSpeech.contains("summon")
                || normalizedSpeech.contains("spirit world")
                || normalizedSpeech.contains("potion")
                || normalizedSpeech.contains("brew")
                || normalizedSpeech.contains("duration")
                || normalizedSpeech.contains("minutes")
                || normalizedSpeech.contains(" by 10")
                || normalizedSpeech.contains(" by 50")
                || normalizedSpeech.contains("for a week")
                || normalizedSpeech.contains("mana")
                || normalizedSpeech.contains("powerful brew")
                || normalizedSpeech.contains("increases your circle magic")) {
            return false;
        }
        return normalizedSpeech.contains("blade")
                || normalizedSpeech.contains("altar power")
                || normalizedSpeech.contains("altar range")
                || normalizedSpeech.contains("range")
                || normalizedSpeech.contains("drop")
                || normalizedSpeech.contains("mob");
    }

    private static boolean asksPlantingAdvice(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("plant")
                || normalizedPlayerMessage.contains("grow")
                || normalizedPlayerMessage.contains("herb");
    }

    private static boolean asksRememberRequest(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.startsWith("remember ")
                && !normalizedPlayerMessage.contains("remember me")
                && !normalizedPlayerMessage.contains("what do you remember")
                && !normalizedPlayerMessage.contains("what you remember");
    }

    private static boolean answersRememberRequest(String speech, String normalizedPlayerMessage) {
        String normalizedSpeech = normalizeForComparison(speech);
        boolean acknowledges = normalizedSpeech.contains("remember")
                || normalizedSpeech.contains("keep that")
                || normalizedSpeech.contains("noted")
                || normalizedSpeech.contains("won t forget")
                || normalizedSpeech.contains("will not forget");
        if (!acknowledges) {
            return false;
        }
        for (String term : normalizedPlayerMessage.split(" ")) {
            if (isMemoryWriteSubjectTerm(term) && normalizedSpeech.contains(term)) {
                return true;
            }
        }
        return true;
    }

    private static boolean isMemoryWriteSubjectTerm(String term) {
        return term.length() >= 4
                && !term.matches("(remember|that|this|about|with|your|youre|like|likes|want|need|dev)");
    }

    private static boolean asksRumor(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("rumor")
                || normalizedPlayerMessage.contains("rumour")
                || normalizedPlayerMessage.contains("gossip");
    }

    private static boolean answersRumor(String speech) {
        String normalized = normalizeForComparison(speech);
        return normalized.contains("rumor")
                || normalized.contains("rumour")
                || normalized.contains("whisper")
                || normalized.contains("village")
                || normalized.contains("folk say")
                || normalized.contains("they say")
                || normalized.contains("heard");
    }

    private static boolean containsWord(String normalizedText, String word) {
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(normalizedText).find();
    }

    private static boolean containsAnyTerm(String normalizedText, String... terms) {
        for (String term : terms) {
            if (containsTerm(normalizedText, term)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsTerm(String normalizedText, String term) {
        if (term.indexOf(' ') >= 0) {
            return normalizedText.contains(term);
        }
        String suffix = term.endsWith("s") ? "" : "s?";
        return Pattern.compile("\\b" + Pattern.quote(term) + suffix + "\\b").matcher(normalizedText).find();
    }

    private static boolean isKnowledgeSubjectTerm(String term) {
        return term.length() >= 4
                && !term.matches("(what|which|where|when|does|about|with|from|that|this|they|them|your|into|tell|explain|looking|need|want|find|used|uses|witch|witches)");
    }

    private static boolean asksParroting(String normalizedPlayerMessage) {
        return normalizedPlayerMessage.contains("parrot")
                || normalizedPlayerMessage.contains("repeat")
                || normalizedPlayerMessage.contains("same thing");
    }

    private static boolean answersParrotingComplaint(String speech) {
        String normalized = normalizeForComparison(speech);
        if (normalized.contains("you re just parroting")
                || normalized.contains("you are just parroting")
                || normalized.contains("aren t you")
                || normalized.contains("are not you")) {
            return false;
        }
        return normalized.contains("no ")
                || normalized.startsWith("no")
                || normalized.contains("repeat")
                || normalized.contains("parrot")
                || normalized.contains("ask again")
                || normalized.contains("poor craft")
                || normalized.contains("bad answer")
                || normalized.contains("answered poorly");
    }

    private static boolean isLowValueFragment(String speech) {
        if (GERUND_FRAGMENT.matcher(speech).matches()) {
            return true;
        }
        return !speech.isBlank()
                && Character.isLowerCase(speech.codePointAt(0))
                && !speech.startsWith("i ")
                && !speech.startsWith("i'")
                && !speech.startsWith("you ")
                && !speech.startsWith("your ");
    }

    private static boolean isClippedSpeech(String speech) {
        String stripped = speech.endsWith("\"") ? speech.substring(0, speech.length() - 1).stripTrailing() : speech;
        return stripped.endsWith("...")
                || stripped.endsWith(",")
                || stripped.endsWith("-")
                || stripped.endsWith("\u2014")
                || (stripped.length() > 140 && !isTerminalPunctuation(stripped.charAt(stripped.length() - 1)));
    }

    private static String trimTrailingClippedSentence(String speech) {
        if (speech == null || speech.isBlank()) {
            return "";
        }
        String text = speech.strip();
        boolean quoted = text.endsWith("\"");
        String body = quoted ? text.substring(0, text.length() - 1).stripTrailing() : text;
        if (body.isBlank()) {
            return speech;
        }
        if (body.endsWith(",")) {
            return speech;
        }
        if (body.endsWith("...") && body.length() > 80) {
            String withoutEllipsis = body.substring(0, body.length() - 3).stripTrailing();
            int cut = lastTerminalIndex(withoutEllipsis);
            if (cut >= 40) {
                String trimmed = withoutEllipsis.substring(0, cut + 1).stripTrailing();
                return quoted ? trimmed + "\"" : trimmed;
            }
            return speech;
        }
        char last = body.charAt(body.length() - 1);
        if (isTerminalPunctuation(last) || body.length() <= 140) {
            return speech;
        }
        int cut = lastTerminalIndex(body);
        if (cut < 40) {
            return speech;
        }
        String trimmed = body.substring(0, cut + 1).stripTrailing();
        return quoted ? trimmed + "\"" : trimmed;
    }

    private static int lastTerminalIndex(String text) {
        int last = -1;
        for (int i = 0; i < text.length(); i++) {
            if (isTerminalPunctuation(text.charAt(i))) {
                last = i;
            }
        }
        return last;
    }

    private static String finishShortTrailingComma(String speech) {
        if (speech == null || speech.isBlank()) {
            return "";
        }
        String text = speech.stripTrailing();
        boolean quoted = text.endsWith("\"");
        String body = quoted ? text.substring(0, text.length() - 1).stripTrailing() : text;
        if (!body.endsWith(",")) {
            return speech;
        }
        String finished = body.substring(0, body.length() - 1).stripTrailing() + ".";
        return quoted ? finished + "\"" : finished;
    }

    private static boolean isTerminalPunctuation(char ch) {
        return ch == '.' || ch == '!' || ch == '?';
    }

    private static String stripNpcAddress(String speech, String npcName) {
        if (speech == null || speech.isBlank() || npcName == null || npcName.isBlank()) {
            return speech == null ? "" : speech;
        }
        String lower = speech.toLowerCase(Locale.ROOT);
        String name = npcName.trim();
        String lowerName = name.toLowerCase(Locale.ROOT);
        if (!lower.startsWith(lowerName)) {
            return speech;
        }
        if (speech.length() == name.length()) {
            return "";
        }
        char next = speech.charAt(name.length());
        if (next == ':' || next == ',' || next == '-' || next == '\u2014') {
            return speech.substring(name.length() + 1).strip();
        }
        return speech;
    }

    private static boolean echoesPlayer(String speech, String playerMessage) {
        if (playerMessage == null || playerMessage.isBlank()) {
            return false;
        }
        String a = normalizeForComparison(speech);
        String b = normalizeForComparison(playerMessage);
        if (a.isBlank() || b.isBlank()) {
            return false;
        }
        if (asksRememberRequest(b) && a.contains("remember")) {
            return false;
        }
        String speechFingerprint = contentFingerprint(a);
        String playerFingerprint = contentFingerprint(b);
        return a.equals(b)
                || (!speechFingerprint.isBlank() && speechFingerprint.equals(playerFingerprint))
                || a.startsWith(b + " ")
                || (speech.endsWith("?") && (a.startsWith(b) || b.startsWith(a)));
    }

    private static String normalizeForComparison(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static boolean sameMeaningForRepetition(String first, String second) {
        String a = normalizeForComparison(first);
        String b = normalizeForComparison(second);
        if (a.isBlank() || b.isBlank()) {
            return false;
        }
        return a.equals(b) || contentFingerprint(a).equals(contentFingerprint(b));
    }

    private static String contentFingerprint(String normalized) {
        return normalized
                .replaceAll("\\b(a|an|the|please|just|really|very|quite)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    public static String sanitize(String raw, int maxLength) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        cleaned = cleaned.replaceAll("\"{2,}$", "\"");
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        if (maxLength <= 3) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned.substring(0, maxLength - 3).stripTrailing() + "...";
    }
}
