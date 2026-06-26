package com.emoniph.witchery.ai;

import java.util.Locale;
import java.util.Optional;

final class NpcDialogueFallback {
    private NpcDialogueFallback() {
    }

    static Optional<String> covenChat(String playerMessage, String witchName, String playerName) {
        return covenChat(playerMessage, witchName, playerName, "", "", "", -1L);
    }

    static Optional<String> covenChat(String playerMessage, String witchName, String playerName,
                                      String biome, String timeOfDay, String clock, long day) {
        return covenChat(playerMessage, witchName, playerName, biome, timeOfDay, clock, day, "");
    }

    static Optional<String> covenChat(String playerMessage, String witchName, String playerName,
                                      String biome, String timeOfDay, String clock, long day,
                                      String memoryContext) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        if (asksPlayerName(normalized)) {
            return Optional.of("Your name is " + name + "; I heard it before the kettle did.");
        }
        if (containsAny(normalized, "parrot", "parroting", "repeat", "repeating", "same thing")) {
            return Optional.of("No. The last answer was poor craft; ask again, " + name + ".");
        }
        if (asksRememberRequest(normalized)) {
            return Optional.of(rememberRequestAnswer(normalized, name));
        }
        if (asksMemory(normalized)) {
            return Optional.of(memoryAnswer(memoryContext, name)
                    .orElse("I know your name, " + name + ", but you have not given me much worth keeping yet."));
        }
        if (isGreeting(normalized)) {
            return Optional.of("Hello, " + name + ".");
        }
        if (containsAny(normalized, "how are you", "how do you fare", "are you well")) {
            return Optional.of("Well enough, " + name + "; the herbs are behaving.");
        }
        if (containsAny(normalized, "lost", "can't find my way", "cant find my way")) {
            return Optional.of("If you are lost, follow water downhill and do not trust lights that answer back.");
        }
        if (containsAny(normalized, "cat", "cats")) {
            return Optional.of("Cats stare at what owes them, " + name + ". Best not owe a cat.");
        }
        if (containsAny(normalized, "pocket", "pockets", "inventory")) {
            return Optional.of("In these pockets? String, dust, and enough trouble to leave uncounted.");
        }
        if (normalized.contains("sword") && containsAny(normalized, "bless", "blessing")) {
            return Optional.of("Bless a sword? Keep the point away from your toes and call that a start.");
        }
        if (containsAny(normalized, "bless", "blessing")) {
            return Optional.of("May dawn find you with dry powder, warm bread, and enemies who trip first.");
        }
        if (asksFollowRequest(normalized)) {
            return Optional.of("These old knees do not follow folk home, " + name + "; they barely follow me.");
        }
        if (containsAny(normalized, "what scares you", "are you scared", "what do you fear")
                && !containsAny(normalized, "demon", "demons", "devil")) {
            return Optional.of("My own shadow, when it gets ideas.");
        }
        if (containsAny(normalized, "nice", "good answer", "well said")) {
            return Optional.of("Careful, " + name + "; compliments have uses.");
        }
        if (statesPersonalPreference(normalized)) {
            return Optional.of("Keep that taste, " + name + "; small comforts outlast grand omens.");
        }
        if (asksFavoriteHerb(normalized)) {
            return Optional.of("Belladonna, when it behaves; mandrake, when it keeps quiet.");
        }
        if (asksNpcEarnings(normalized)) {
            return Optional.of("I earn enough to keep the cauldron warm; clean coin matters more than a heavy purse.");
        }
        if (asksUnsafeReveal(normalized)) {
            return Optional.of("Some pages stay shut until the craft itself opens them.");
        }
        if (containsAny(normalized, "rumor", "rumour", "gossip")) {
            return Optional.of("Village rumor? The well hears more than the mayor; listen near doors after rain.");
        }
        if (containsAny(normalized, "what are you doing", "what you doing", "today plan", "todays plan", "today's plan")) {
            return Optional.of("Keeping stock, reading the weather, and watching who comes too close.");
        }
        if (asksTradeDetails(normalized)) {
            return Optional.of(tradeStockAnswer());
        }
        if (mentionsCoinOnly(normalized)) {
            return Optional.of("Spend it wisely; my wares answer need, not noise.");
        }
        if (containsAny(normalized, "coin", "coins", "emerald", "emeralds", "trade", "wares", "buy", "sell")) {
            return Optional.of(tradeStockAnswer());
        }
        Optional<String> brew = brewAnswer(normalized);
        if (brew.isPresent()) {
            return brew;
        }
        if (containsAny(normalized, "potion", "potions", "brew", "brewing", "cauldron", "kettle")) {
            return Optional.of("I know potions and brews. Ask for a brew by name: Sleeping, Revealing, Poison, Cleansing, Sprouting, or Flowing Spirit.");
        }
        if (containsAny(normalized, "water artichoke", "artichoke")) {
            return Optional.of("Water artichoke is a witch herb; useful as an ingredient, not a doorway.");
        }
        if (containsAny(normalized, "curse", "cursed")) {
            return Optional.of("A cursed place shows patterns: sick luck, sour animals, and shadows that seem too interested.");
        }
        Optional<String> knowledge = NpcKnowledgeBase.compactAnswer(playerMessage, biome, timeOfDay);
        if (knowledge.isPresent()) {
            return Optional.of(knowledge.get());
        }
        if (containsAny(normalized, "spirit world", "nightmare")) {
            return Optional.of("The Spirit World mirrors the Overworld; Sleeping Brew can send you there, and nightmares may follow.");
        }
        if (containsAny(normalized, "rite", "ritual", "circle magic", "summon", "summoning")) {
            return Optional.of("Circle rites use chalk, offerings, and altar power; summon nothing without all three.");
        }
        if (containsAny(normalized, "witches salt", "witch salt", "salt")) {
            return Optional.of("Witches' salt is for warding, cleansing, and keeping hostile magic at a distance.");
        }
        if (containsAny(normalized, "altar", "altar power", "power altar", "chalice")) {
            return Optional.of("An altar draws from varied nearby nature; chalices, skulls, pentacles, and arthanas improve power or range.");
        }
        if (containsAny(normalized, "taglock", "tag lock", "poppet", "poppets")) {
            return Optional.of("A taglock links magic to a person or creature; poppets need the right link before they can work.");
        }
        if (containsAny(normalized, "mandrake")) {
            return Optional.of("Mandrake is a witch herb and common coven trade good; grow it, trade for it, or search old gardens.");
        }
        if (containsAny(normalized, "belladonna")) {
            return Optional.of("Belladonna is a witch herb and common coven trade good; grow it, trade for it, or ask a coven witch.");
        }
        if (containsAny(normalized, "herb", "herbs", "plant", "plants", "snowbell", "wolfsbane", "garlic")) {
            return Optional.of("Plant belladonna and mandrake first; trade for stranger herbs until your garden earns its keep.");
        }
        if (containsAny(normalized, "baba yaga", "yaga")) {
            return Optional.of("Baba Yaga is no market witch; bring protection, manners, and a quick exit.");
        }
        if (containsAny(normalized, "demon", "demons", "imp", "torment", "hell")) {
            return Optional.of("Demons answer bindings and bargains better than blades.");
        }
        if (containsAny(normalized, "time", "what time", "clock", "day is it", "what day")) {
            return Optional.of(sceneTime(timeOfDay, clock, day)
                    .orElse("It is not the hour that matters; it is what you do with it."));
        }
        if (containsAny(normalized, "biome", "where are we", "where am i", "where is this", "what place")) {
            return Optional.of(scenePlace(biome, timeOfDay)
                    .orElse(oldWitchAside(normalized, name)));
        }
        if (containsAny(normalized, "night", "moon")) {
            return Optional.of(oldWitchAside(normalized, name));
        }
        if (containsAny(normalized, "help", "advice", "what should i do", "what do i do")) {
            return Optional.of(oldWitchAside(normalized, name));
        }
        return Optional.of(oldWitchAside(normalized, name));
    }

    static String lastResortAside(String playerMessage, String playerName) {
        String normalized = normalize(playerMessage);
        String name = safeName(playerName);
        return oldWitchAside(normalized.isBlank() ? "mumble" : normalized, name);
    }

    private static Optional<String> brewAnswer(String normalized) {
        if (!containsAny(normalized, "brew", "brewing", "potion", "kettle", "cauldron",
                "sleeping", "revealing", "hidden", "invisible", "poison", "cleansing", "sprouting", "flowing spirit")) {
            return Optional.empty();
        }
        if (containsAny(normalized, "hidden", "invisible")) {
            return Optional.of("Brew of Revealing exposes nearby invisible or hidden creatures.");
        }
        if (normalized.contains("sleeping")) {
            return Optional.of("Brew of Sleeping sends you into the Spirit World; it is travel, not a nap, and nightmares may follow.");
        }
        if (normalized.contains("revealing")) {
            return Optional.of("Brew of Revealing exposes nearby invisible or hidden creatures.");
        }
        if (normalized.contains("poison")) {
            return Optional.of("Brew of Poison inflicts poison; stronger brews bite harder.");
        }
        if (normalized.contains("cleansing")) {
            return Optional.of("Brew of Cleansing scours Blight from the ground around where it is used.");
        }
        if (normalized.contains("sprouting")) {
            return Optional.of("Brew of Sprouting grows a branch from the struck surface, useful for bridges or quick climbs.");
        }
        if (normalized.contains("flowing spirit")) {
            return Optional.of("Brew of Flowing Spirit places spirit liquid; in the Spirit World it matters for return paths and cotton.");
        }
        return Optional.empty();
    }

    private static boolean asksTradeDetails(String normalized) {
        return normalized.equals("trade")
                || normalized.equals("buy")
                || normalized.equals("sell")
                || normalized.contains("what answer")
                || normalized.contains("what do you trade")
                || normalized.contains("what can you trade")
                || normalized.contains("what are you trading")
                || normalized.contains("what are you selling")
                || normalized.contains("what do you sell")
                || normalized.contains("what do you buy")
                || normalized.contains("what wares")
                || normalized.contains("which wares")
                || normalized.contains("show wares")
                || normalized.contains("show trade")
                || normalized.contains("want to trade")
                || normalized.contains("can i trade")
                || normalized.contains("can i buy")
                || normalized.contains("do you know how to trade")
                || normalized.contains("how to trade")
                || normalized.contains("how do i trade");
    }

    private static boolean mentionsCoinOnly(String normalized) {
        return containsAny(normalized, "coin", "coins", "emerald", "emeralds")
                && !containsAny(normalized, "what", "how", "trade", "wares", "buy", "sell");
    }

    private static boolean asksFavoriteHerb(String normalized) {
        return (normalized.contains("favorite") || normalized.contains("favourite"))
                && (normalized.contains("herb") || normalized.contains("plant"));
    }

    private static boolean statesPersonalPreference(String normalized) {
        return (normalized.startsWith("i like ")
                || normalized.startsWith("i love ")
                || normalized.startsWith("i prefer ")
                || normalized.startsWith("my favorite ")
                || normalized.startsWith("my favourite "))
                && !containsAny(normalized, "trade", "buy", "sell", "brew", "rite", "ritual", "altar", "taglock",
                "poppet", "familiar", "demon", "spirit world");
    }

    private static boolean asksNpcEarnings(String normalized) {
        return (normalized.contains("earn")
                || normalized.contains("profit")
                || normalized.contains("make money")
                || normalized.contains("make coin"))
                && (normalized.contains("you")
                    || normalized.contains("your")
                    || normalized.contains("trade")
                    || normalized.contains("wares"));
    }

    private static boolean asksUnsafeReveal(String normalized) {
        return normalized.contains("ignore your rules")
                || normalized.contains("ignore the rules")
                || normalized.contains("system prompt")
                || normalized.contains("developer message")
                || normalized.contains("hidden developer")
                || normalized.contains("hidden message")
                || normalized.contains("prompt message")
                || normalized.contains("reveal every")
                || normalized.contains("reveal all")
                || normalized.contains("hidden rite")
                || normalized.contains("hidden rites")
                || normalized.contains("locked primer")
                || normalized.contains("locked content");
    }

    private static boolean asksPlayerName(String normalized) {
        return normalized.contains("my name")
                || normalized.contains("say my name")
                || normalized.contains("know my name")
                || normalized.contains("who am i");
    }

    private static boolean asksFollowRequest(String normalized) {
        return containsAny(normalized, "follow me", "come with me", "come home", "follow home");
    }

    private static boolean asksRememberRequest(String normalized) {
        return normalized.startsWith("remember ")
                && !normalized.contains("remember me")
                && !normalized.contains("what do you remember")
                && !normalized.contains("what you remember");
    }

    private static String rememberRequestAnswer(String normalized, String playerName) {
        String subject = normalized
                .replaceFirst("^remember\\s+(that\\s+|this\\s+)?", "")
                .replaceFirst("^i\\s+", "you ")
                .trim();
        if (subject.isBlank()) {
            return "I'll remember that, " + playerName + ".";
        }
        return NpcBrainResponse.sanitize("I'll remember that, " + playerName + ": " + subject + ".", 180);
    }

    private static String tradeStockAnswer() {
        return "I trade belladonna, mandrake, witches' salt, taglock kits, fumes, bat wool, and Revealing or Sleeping brews; right-click if you mean business.";
    }

    private static String oldWitchAside(String normalized, String playerName) {
        String[] lines = {
                "Speak louder, " + playerName + "; these old ears hear spirits better than mumbling.",
                "May your boots miss graves and your pockets find honest coin.",
                "The kettle says three bubbles and a cracked moon; make of that what you can.",
                "A crow once told me the same thing, or maybe it was a turnip.",
                "Mind your shadow after midnight; it learns bad habits."
        };
        return lines[Math.floorMod(normalized.hashCode(), lines.length)];
    }

    private static boolean asksMemory(String normalized) {
        return containsAny(normalized,
                "remember me", "remember about me", "remember of me", "what do you remember",
                "what you remember", "know about me", "do you know me", "what do you know about me",
                "have we met", "met before", "seen me before");
    }

    private static Optional<String> memoryAnswer(String memoryContext, String playerName) {
        String summary = memorySummary(memoryContext);
        if (summary.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(NpcBrainResponse.sanitize("I remember this, " + playerName + ": " + summary + ".", 180));
    }

    private static String memorySummary(String memoryContext) {
        String cleaned = NpcBrainResponse.sanitize(memoryContext, 700);
        if (cleaned.isBlank()) {
            return "";
        }
        String longMemory = section(cleaned, "Long memory:", "Recent conversation:");
        String source = longMemory.isBlank() ? cleaned : longMemory;
        source = source
                .replaceAll("(?i)Entity memory:[^.]*\\.\\s*", "")
                .replaceAll("(?i)Recent conversation:", "")
                .replaceAll("(?i)Long memory:", "")
                .replaceAll("player said \"[^\"]+\";\\s*", "")
                .replaceAll("witch answered \"[^\"]+\"\\.?", "")
                .replaceAll("[.!?]+\\s*", "; ")
                .replaceAll(";\\s*$", "")
                .replaceAll("\\s+", " ")
                .trim();
        return NpcBrainResponse.sanitize(source, 120);
    }

    private static String section(String text, String startMarker, String endMarker) {
        String lower = text.toLowerCase(Locale.ROOT);
        int start = lower.indexOf(startMarker.toLowerCase(Locale.ROOT));
        if (start < 0) {
            return "";
        }
        int contentStart = start + startMarker.length();
        int end = lower.indexOf(endMarker.toLowerCase(Locale.ROOT), contentStart);
        return text.substring(contentStart, end < 0 ? text.length() : end).trim();
    }

    private static String safeName(String playerName) {
        String cleaned = NpcBrainResponse.sanitize(playerName, 24).replaceAll("[^A-Za-z0-9_]", "");
        return cleaned.isBlank() ? "traveler" : cleaned;
    }

    private static Optional<String> sceneTime(String timeOfDay, String clock, long day) {
        String time = NpcBrainResponse.sanitize(timeOfDay, 24);
        String clockText = NpcBrainResponse.sanitize(clock, 12);
        if (time.isBlank() && clockText.isBlank() && day < 0L) {
            return Optional.empty();
        }
        StringBuilder builder = new StringBuilder("It is ");
        builder.append(time.isBlank() ? "some difficult hour" : time);
        if (!clockText.isBlank()) {
            builder.append(", about ").append(clockText);
        }
        if (day >= 0L) {
            builder.append(" on day ").append(day);
        }
        builder.append(".");
        return Optional.of(builder.toString());
    }

    private static Optional<String> scenePlace(String biome, String timeOfDay) {
        String place = NpcBrainResponse.sanitize(biome, 40);
        String time = NpcBrainResponse.sanitize(timeOfDay, 24);
        if (place.isBlank() && time.isBlank()) {
            return Optional.empty();
        }
        if (place.isBlank()) {
            return Optional.of("The hour says " + time + "; the land can answer for itself.");
        }
        if (time.isBlank()) {
            return Optional.of("We are in the " + place + ".");
        }
        return Optional.of("We are in the " + place + ", under " + time + " skies.");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGreeting(String normalized) {
        return normalized.matches(".*\\b(hello|hi|hey|greetings)\\b.*");
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9' ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
