package com.emoniph.witchery.ai;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String witch = safeDisplayName(witchName, "Witch");
        if (asksNpcName(normalized)) {
            return Optional.of("I am " + witch + ".");
        }
        if (asksPlayerName(normalized)) {
            return Optional.of("Your name is " + name + "; I heard it before the kettle did.");
        }
        if (addressesWitchByName(normalized, witchName)) {
            return Optional.of("I'm listening, " + name + "; ask plain.");
        }
        Optional<String> contextual = contextualFollowup(normalized, memoryContext, name);
        if (contextual.isPresent()) {
            return contextual;
        }
        if (asksHerbGift(normalized)) {
            return Optional.of("Ask with an empty hand and honest need; herbs remember greedy fingers.");
        }
        if (isInsult(normalized)) {
            return Optional.of("Mind your tongue, " + name + "; old women know where curses sleep.");
        }
        if (isPraise(normalized)) {
            return Optional.of("Careful, " + name + "; compliments have roots.");
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
        if (asksHutOrDoorSafety(normalized)) {
            return Optional.of("Witches' salt by the threshold, light inside, and no promises made through a closed door.");
        }
        if (containsAny(normalized, "dream catcher", "dreamcatcher")) {
            return Optional.of("Keep a Dream Catcher near the bed if you must sleep close to strange magic.");
        }
        if (containsAny(normalized, "owl", "owls") && containsAny(normalized, "suit me", "good familiar", "familiar")) {
            return Optional.of("An owl suits night work and watchful witches; earn its trust before asking it to guard yours.");
        }
        if (containsAny(normalized, "owl", "owls") && !containsAny(normalized, "familiar", "familiars")) {
            return Optional.of("An owl on the roof is a warning, not a command. Watch it, then ward your door.");
        }
        if (containsAny(normalized, "cat", "cats")) {
            return Optional.of("Cats stare at what owes them, " + name + ". Best not owe a cat.");
        }
        if (containsAny(normalized, "egg", "eggs")) {
            return Optional.of("Eggs are honest little moons; crack one and it tells you what it was hiding.");
        }
        if (containsAny(normalized, "potato", "potatoes")
                && containsAny(normalized, "do you", "you like", "too", "favorite", "favourite")) {
            return Optional.of("Potatoes? Sensible roots. Better in stew than in omens.");
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
        if (containsAny(normalized, "i like that you remember", "glad you remember", "good memory")) {
            return Optional.of("Memory is a jar, " + name + "; useful only if the lid stays on.");
        }
        if (statesPersonalPreference(normalized)) {
            return Optional.of("Keep that taste, " + name + "; small comforts outlast grand omens.");
        }
        if (asksFavoriteHerb(normalized)) {
            return Optional.of("Mandrake, when it keeps quiet; snowbell, when the cold bites.");
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
        if (containsAny(normalized, "village thinks you are trouble", "village think you are trouble")) {
            return Optional.of("Then your village has eyes. Trouble and I are old neighbors.");
        }
        if (asksRoadAdvice(normalized)) {
            return Optional.of("Keep salt dry, coin hidden, and do not answer voices that know your name too sweetly.");
        }
        if (asksDemonTrust(normalized)) {
            return Optional.of("Never trust a demon's first bargain. Bind it, ward yourself, and leave room to run.");
        }
        Optional<String> recipeOrRite = recipeOrRiteAnswer(normalized, name);
        if (recipeOrRite.isPresent()) {
            return recipeOrRite;
        }
        if (asksTeachMe(normalized)) {
            Optional<String> knowledge = NpcKnowledgeBase.compactAnswer(playerMessage, biome, timeOfDay);
            if (knowledge.isPresent()) {
                return Optional.of(knowledge.get());
            }
            return Optional.of(teachingAnswer(normalized, name));
        }
        if (containsAny(normalized, "what should i do instead", "do instead", "maybe not")) {
            return Optional.of("Let fools laugh. Ward your door, learn your craft, and spend curses only when they have teeth.");
        }
        if (containsAny(normalized, "lonely", "alone")) {
            return Optional.of("Lonely? No. The dark talks too much, and I have learned which parts to ignore.");
        }
        if (containsAny(normalized, "careful apprentice", "apprentice")) {
            return Optional.of("A careful apprentice labels jars, counts offerings twice, and never mistakes a whisper for permission.");
        }
        if (asksCovenRecruiting(normalized)) {
            return Optional.of("Find a wandering coven witch, bring a bound familiar, and be ready for her trial. A coven is earned, not collected.");
        }
        if (containsAny(normalized, "what are you doing", "what you doing", "today plan", "todays plan", "today's plan")) {
            return Optional.of("Keeping stock, reading the weather, and watching who comes too close.");
        }
        if (asksFirstGathering(normalized)) {
            return Optional.of("Start with salt, clay jars, ash, and the herb your page names; guessing feeds bad brews.");
        }
        if (asksTradeDetails(normalized)) {
            return Optional.of(tradeStockAnswer(normalized));
        }
        if (mentionsCoinOnly(normalized)) {
            return Optional.of("Spend it wisely; my wares answer need, not noise.");
        }
        if (containsAny(normalized, "coin", "coins", "emerald", "emeralds", "trade", "wares", "buy", "sell")) {
            return Optional.of(tradeStockAnswer(normalized));
        }
        recipeOrRite = recipeOrRiteAnswer(normalized, name);
        if (recipeOrRite.isPresent()) {
            return recipeOrRite;
        }
        Optional<String> brew = brewAnswer(normalized);
        if (brew.isPresent()) {
            return brew;
        }
        if (asksMixingWarning(normalized)) {
            return Optional.of("Do not free-mix from guesses; follow the visible recipe page, then label the bottle.");
        }
        if (containsAny(normalized, "potion", "potions", "brew", "brewing", "cauldron", "kettle")) {
            return Optional.of("I know potions and brews. Ask for a brew by name: Sleeping, Revealing, Poison, Cleansing, Sprouting, or Flowing Spirit.");
        }
        if (containsAny(normalized, "mandrake") && containsAny(normalized, "clay jar", "jar")) {
            return Optional.of("Mandrake is an ingredient; clay jars belong with oven work. Learn which fumes the recipe asks for first.");
        }
        if (containsAny(normalized, "fume", "fumes")) {
            return Optional.of("Witch's Oven fumes serve brews and rites; catch them in jars before trusting the mystery.");
        }
        if (containsAny(normalized, "oven", "witch's oven", "witches oven", "sapling", "saplings", "fume", "fumes",
                "clay jar", "jar")) {
            return Optional.of("A Witch's Oven burns saplings and ingredients into fumes; clay jars matter, so do not treat it like a furnace.");
        }
        if (containsAny(normalized, "water artichoke", "artichoke")) {
            return Optional.of("Water artichoke is a witch herb; useful as an ingredient, not a doorway.");
        }
        if (containsAny(normalized, "taglock", "tag lock", "poppet", "poppets")
                && containsAny(normalized, "against me", "use my", "my taglock", "worried")) {
            return Optional.of("A taglock links magic to a person; guard yours like a tooth with your name carved in it.");
        }
        if (containsAny(normalized, "curse", "cursed")) {
            if (containsAny(normalized, "can you curse", "could you curse", "curse the", "curse him", "curse her")) {
                return Optional.of("Not for bruised pride. Learn wards first; curses collect interest.");
            }
            return Optional.of("A cursed place shows patterns: sick luck, sour animals, and shadows that seem too interested.");
        }
        if (containsAny(normalized, "belladonna")
                && containsAny(normalized, "danger", "dangerous", "safe", "eat", "edible", "food")) {
            return Optional.of("Belladonna is useful but dangerous, not food; handle it as an ingredient and keep it from idle mouths.");
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
            if (containsAny(normalized, "against me", "use my", "my taglock", "worried")) {
                return Optional.of("A taglock links magic to a person; guard yours like a tooth with your name carved in it.");
            }
            return Optional.of("A taglock links magic to a person or creature; poppets need the right link before they can work.");
        }
        if (containsAny(normalized, "mandrake")) {
            return Optional.of("Mandrake is a witch herb and common coven trade good; grow it, trade for it, or search old gardens.");
        }
        if (containsAny(normalized, "belladonna")) {
            if (containsAny(normalized, "danger", "dangerous", "safe", "eat", "edible", "food")) {
                return Optional.of("Belladonna is useful but dangerous, not food; handle it as an ingredient and keep it from idle mouths.");
            }
            return Optional.of("Belladonna is a witch herb and common coven trade good; grow it, trade for it, or ask a coven witch.");
        }
        if (containsAny(normalized, "herb", "herbs", "plant", "plants", "snowbell", "wolfsbane", "garlic")) {
            if (containsAny(normalized, "garlic")) {
                return Optional.of("Garlic is a witch herb, not a wall against every evil. Salt your threshold too.");
            }
            return Optional.of("Ask after the herb your work needs; grow the common ones and trade for stranger roots.");
        }
        if (containsAny(normalized, "baba yaga", "yaga")) {
            return Optional.of("Baba Yaga is no market witch; bring protection, manners, and a quick exit.");
        }
        if (containsAny(normalized, "demon", "demons", "imp", "torment", "hell")) {
            return Optional.of("Never trust a demon's first bargain. Bind it, ward yourself, and leave room to run.");
        }
        if (containsAny(normalized, "spirit", "spirits", "whispers", "well")) {
            return Optional.of("Could be spirits, could be water talking to stone. Mark the place and return with salt.");
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
        return lastResortAside(playerMessage, playerName, "");
    }

    static String lastResortAside(String playerMessage, String playerName, String avoidSpeech) {
        String normalized = normalize(playerMessage);
        String name = safeName(playerName);
        Optional<String> specific = specificAlternate(normalized.isBlank() ? "mumble" : normalized, name, avoidSpeech);
        if (specific.isPresent()) {
            return specific.get();
        }
        return oldWitchAside(normalized.isBlank() ? "mumble" : normalized, name, avoidSpeech);
    }

    static Optional<String> alternateForRepeatedFallback(String playerMessage, String playerName, String avoidSpeech) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        Optional<String> specific = specificAlternate(normalized, safeName(playerName), avoidSpeech);
        if (specific.isPresent()) {
            return specific;
        }
        if (isSpecificAnswerPrompt(normalized)) {
            return Optional.empty();
        }
        return Optional.of(oldWitchAside(normalized, safeName(playerName), avoidSpeech));
    }

    private static Optional<String> specificAlternate(String normalized, String playerName, String avoidSpeech) {
        String line = "";
        if (asksHutOrDoorSafety(normalized)) {
            line = "Salt the threshold, keep a light burning, and make no bargains through the door.";
        } else if (containsAny(normalized, "oven", "witch's oven", "witches oven", "sapling", "saplings", "fume", "fumes",
                "clay jar", "jar")) {
            line = "In a Witch's Oven, saplings feed fumes; clay jars are the clever bit, not random smoke.";
        } else if (asksExactRecipe(normalized) || asksMissingRecipeCheck(normalized)) {
            line = "If the exact page is absent, use the Primer or recipe book; I will not invent ingredients.";
        } else if (asksCauldronPrep(normalized)) {
            line = "Before the cauldron, prepare water, heat, a visible page, and the ingredients it names.";
        } else if (asksWeakAltar(normalized)) {
            line = "A weak altar wants varied nearby nature and proper fittings, not louder chanting.";
        } else if (asksOfferings(normalized)) {
            line = "Offerings are the rite's payment; wrong payment makes the circle sulk.";
        } else if (asksRiteSafety(normalized)) {
            line = "Never start a rite with broken chalk, missing offerings, weak power, or a page you cannot read.";
        } else if (containsAny(normalized, "dream catcher", "dreamcatcher", "nightmare", "nightmares")) {
            line = "Keep the Dream Catcher near your bed, " + playerName + "; strange sleep leaves muddy footprints.";
        } else if (containsAny(normalized, "potato", "potatoes")) {
            line = "A potato keeps its secrets underground; respectable vegetable, that.";
        } else if (asksDemonTrust(normalized) || containsAny(normalized, "demon", "demons", "devil", "imp")) {
            line = "If a demon sounds reasonable, " + playerName + ", count your wards and step back twice.";
        } else if (containsAny(normalized, "familiar", "familiars", "owl", "cat", "toad")) {
            line = "A familiar is met with patience and respect; if it watches you first, mind your manners.";
        } else if (containsAny(normalized, "taglock", "tag lock", "poppet", "poppets")) {
            line = "Guard your taglocks, " + playerName + "; sympathetic magic loves a careless pocket.";
        } else if (containsAny(normalized, "mandrake")) {
            line = "Mandrake is a witch herb and common coven trade good; grow it, trade for it, or search old gardens.";
        } else if (containsAny(normalized, "belladonna")) {
            line = "Belladonna is a witch herb and common coven trade good; useful as an ingredient, not supper.";
        } else if (containsAny(normalized, "spirit world", "nightmare")) {
            line = "The Spirit World mirrors this one; mark your return and keep a Dream Catcher close.";
        } else if (asksCovenRecruiting(normalized)) {
            line = "Seek wandering coven witches, earn a familiar's bond, and be ready for a trial.";
        } else if (asksMixingWarning(normalized)) {
            line = "Do not free-mix from guesses; follow the visible recipe page, then label the bottle.";
        }
        if (line.isBlank() || NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech)) {
            return Optional.empty();
        }
        return Optional.of(line);
    }

    private static Optional<String> recipeOrRiteAnswer(String normalized, String playerName) {
        if (asksRitePreparation(normalized)) {
            return Optional.of("Before a rite, learn chalk layout, offerings, and altar power; if one is wrong, step back.");
        }
        if (asksRecipeRiteLearning(normalized)) {
            return Optional.of("Start with the Primer, " + playerName + ": recipes need a visible page and station; rites need chalk, offerings, and altar power.");
        }
        if (asksMissingRecipeCheck(normalized)) {
            return Optional.of("Check the Primer page, the right station, visible recipe book, altar power, unlock gates, and the exact name you asked for.");
        }
        if (asksExactRecipe(normalized)) {
            Optional<String> knownRecipe = NpcKnowledgeBase.compactAnswer(normalized, "Plains", "night");
            if (knownRecipe.isPresent() && !knownRecipe.get().contains("must come from")) {
                return knownRecipe;
            }
            if (normalized.contains("sleeping")) {
                return Optional.of("The exact Sleeping Brew page is absent from my grimoire; check your Primer or recipe book before naming ingredients. The brew sends travelers into the Spirit World.");
            }
            if (containsAny(normalized, "oven", "witch's oven", "witches oven")) {
                return Optional.of("The exact Witch's Oven craft belongs to the recipe book; what matters here is that it burns saplings and ingredients into fumes.");
            }
            return Optional.of("The exact page is absent from my grimoire; check your Primer or recipe book before naming ingredients.");
        }
        if (asksCauldronPrep(normalized)) {
            return Optional.of("Before the cauldron, prepare water, heat, a visible brew page, and only the ingredients that page names.");
        }
        if (asksBasicBrewRecipe(normalized)) {
            return Optional.of("A brew recipe needs the right station, water or heat when called for, named ingredients, and a page you can actually read.");
        }
        if (asksFumeSafety(normalized)) {
            return Optional.of("Use a Witch's Oven with saplings or ingredients and clay jars; catch fumes on purpose, not by guesswork.");
        }
        if (asksFumeUse(normalized)) {
            return Optional.of("A Witch's Oven burns saplings and ingredients into fumes; clay jars catch them for brews and rites.");
        }
        if (asksSpinningWheel(normalized)) {
            return Optional.of("The spinning wheel takes fibre and modifiers, drinks altar power, and spins threadwork like Fanciful Thread.");
        }
        if (asksDistillery(normalized)) {
            return Optional.of("A distillery takes two inputs and clay jars, drinks altar power, and refines fumes into useful essences.");
        }
        if (asksGrimoireSummary(normalized)) {
            return Optional.of("Write this: recipes need the right station and visible page; rites need chalk, offerings, altar power, and patience.");
        }
        if (asksBeginnerPath(normalized)) {
            return Optional.of("Begin with one visible brew from the Primer, then a harmless circle lesson; no hidden recipe from memory.");
        }
        if (asksRiteSafety(normalized)) {
            return Optional.of("Never start a rite with broken chalk, missing offerings, weak altar power, or a page you cannot read.");
        }
        if (asksFailedRite(normalized)) {
            return Optional.of("Check chalk layout, offerings, altar power, familiar or coven requirements, dimension limits, and unlock gates before guessing.");
        }
        if (asksOfferings(normalized)) {
            return Optional.of("Offerings tell a rite what debt is paid; wrong offerings make circles sulk or fail.");
        }
        if (asksWeakAltar(normalized)) {
            return Optional.of("A weak altar usually wants more varied nearby nature and proper fittings, not louder chanting.");
        }
        if (asksFamiliarBindingRite(normalized)) {
            return Optional.of("A familiar binding rite needs the right circle, chalk, offerings, altar power, and familiar; check the visible page before naming parts.");
        }
        if (asksSummoningRite(normalized)) {
            return Optional.of("A summoning rite brings something to the circle; match chalk, offerings, altar power, unlock gates, and the named entity.");
        }
        if (asksCurseRite(normalized)) {
            return Optional.of("Curse rites are not tavern tricks; use circles, chalk, offerings, altar power, and a visible page before naming a victim.");
        }
        return Optional.empty();
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

    static boolean isInsult(String normalized) {
        return containsAny(normalized,
                "stupid", "idiot", "fool", "useless", "shut up", "old hag", "hag",
                "ugly", "dumb", "trash", "garbage", "fuck you", "screw you", "no shit");
    }

    static boolean isPraise(String normalized) {
        return containsAny(normalized,
                "thank you", "thanks", "good witch", "kind witch", "nice witch", "wise witch",
                "you are kind", "youre kind", "you are wise", "youre wise", "you are helpful",
                "youre helpful", "i like you", "you helped me", "well done", "good answer");
    }

    static boolean asksHerbGift(String normalized) {
        return containsAny(normalized, "give me herb", "give me a herb", "spare herb", "spare a herb",
                "can i have herb", "can i have a herb", "may i have herb", "may i have a herb",
                "gift me herb", "gift me a herb", "give me belladonna", "give me mandrake",
                "another herb", "more herbs", "can i have another herb", "can i have more herb");
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
        if (asksNpcName(normalized)) {
            return false;
        }
        return containsAny(normalized, "what is my name", "what's my name", "whats my name")
                || normalized.contains("my name?")
                || normalized.contains("say my name")
                || normalized.contains("know my name")
                || normalized.contains("who am i");
    }

    private static boolean asksNpcName(String normalized) {
        return normalized.contains("your name")
                || normalized.contains("who are you")
                || normalized.contains("what are you called")
                || normalized.contains("what should i call you");
    }

    private static boolean addressesWitchByName(String normalized, String witchName) {
        String witch = normalize(witchName);
        if (witch.isBlank()) {
            return false;
        }
        String firstName = witch.contains(" ") ? witch.substring(0, witch.indexOf(' ')) : witch;
        return normalized.equals(witch) || normalized.equals(firstName)
                || normalized.equals(witch + "?") || normalized.equals(firstName + "?");
    }

    private static boolean asksTeachMe(String normalized) {
        return containsAny(normalized, "teach me", "show me", "teach something", "teach me something",
                "lesson", "learn witchcraft", "start learning");
    }

    private static boolean asksFirstGathering(String normalized) {
        return containsAny(normalized, "what should i gather first", "what do i gather first",
                "what should i gather", "what do i gather", "gather first", "gather before dark",
                "gather for a brew", "gather for brewing");
    }

    private static boolean asksCovenRecruiting(String normalized) {
        return containsAny(normalized, "start a coven", "make a coven", "recruit coven", "recruit a coven",
                "recruit witches", "recruit witch", "where can i recruit", "find coven", "before a coven",
                "learn before a coven", "before starting a coven", "before joining a coven",
                "learn before coven", "find people for a coven", "where do i find people for a coven",
                "where can i find people for a coven");
    }

    private static boolean asksFollowRequest(String normalized) {
        return containsAny(normalized, "follow me", "come with me", "come home", "follow home");
    }

    private static boolean asksHutOrDoorSafety(String normalized) {
        return containsAny(normalized, "safe little hut", "safe hut", "build a safe",
                "by the door", "by my door", "near the door", "at the door", "threshold",
                "keep in my hut", "keep in my house", "keep in my home", "keep at home")
                || (normalized.contains("near the village")
                    && containsAny(normalized, "hut", "house", "home", "build", "door", "threshold"));
    }

    private static boolean asksRoadAdvice(String normalized) {
        return containsAny(normalized, "final advice", "advice for the road", "for the road",
                "before i leave", "before i go", "before leaving");
    }

    private static boolean asksDemonTrust(String normalized) {
        return containsAny(normalized, "demon", "demons", "devil", "imp")
                && containsAny(normalized, "trust", "bargain", "bargains", "deal", "deals");
    }

    private static boolean asksRecipeRiteLearning(String normalized) {
        return containsAny(normalized, "learn brewing", "learn brews", "learn recipes", "learn a recipe",
                "brewing and rites", "brewing and rituals", "recipes and rites", "recipes and rituals",
                "learn brewing and rites", "learn witchcraft properly");
    }

    private static boolean asksRitePreparation(String normalized) {
        return containsAny(normalized, "learn before trying a rite", "learn before trying ritual",
                "what should i learn before trying a rite", "what should i learn before trying ritual",
                "before trying a rite", "before trying ritual", "before a rite", "before a ritual",
                "before doing a rite", "before doing ritual");
    }

    private static boolean asksExactRecipe(String normalized) {
        return containsAny(normalized, "exact recipe", "exact ingredients", "ingredient list", "ingredients for",
                "recipe for", "what is the recipe", "witch's oven recipe", "witches oven recipe");
    }

    private static boolean asksMissingRecipeCheck(String normalized) {
        return containsAny(normalized, "recipe is missing", "recipe missing", "exact recipe is missing",
                "exact recipe missing", "missing recipe", "recipe absent", "page is missing", "page missing");
    }

    private static boolean asksCauldronPrep(String normalized) {
        return containsAny(normalized, "before i touch the cauldron", "before touching the cauldron",
                "before the cauldron", "prepare the cauldron", "ready the cauldron", "need before")
                && containsAny(normalized, "cauldron", "kettle", "brew", "brewing");
    }

    private static boolean asksBasicBrewRecipe(String normalized) {
        return containsAny(normalized, "basic brew recipe", "basic recipe", "simple brew recipe",
                "explain a brew recipe", "explain basic brew", "how do recipes work")
                && containsAny(normalized, "brew", "brewing", "potion", "recipe");
    }

    private static boolean asksFumeUse(String normalized) {
        return containsAny(normalized, "fume", "fumes");
    }

    private static boolean asksFumeSafety(String normalized) {
        return containsAny(normalized, "fume", "fumes")
                && containsAny(normalized, "safely", "safe", "get", "gather", "catch", "collect");
    }

    private static boolean asksSpinningWheel(String normalized) {
        return containsAny(normalized, "spinning wheel", "spinning");
    }

    private static boolean asksDistillery(String normalized) {
        return containsAny(normalized, "distillery", "distill");
    }

    private static boolean asksOfferings(String normalized) {
        return containsAny(normalized, "offering", "offerings", "why offer", "what do i offer");
    }

    private static boolean asksWeakAltar(String normalized) {
        return containsAny(normalized, "altar is too weak", "altar too weak", "weak altar",
                "altar not strong", "altar lacks power", "not enough altar power");
    }

    private static boolean asksFailedRite(String normalized) {
        return containsAny(normalized, "rite fail", "rite failed", "ritual fail", "ritual failed",
                "why did my rite fail", "why did my ritual fail", "fizzled", "fizzle");
    }

    private static boolean asksFamiliarBindingRite(String normalized) {
        return containsAny(normalized, "familiar binding", "familiar rite", "familiar ritual",
                "bind a familiar", "binding rite")
                && containsAny(normalized, "familiar", "rite", "ritual", "circle");
    }

    private static boolean asksSummoningRite(String normalized) {
        return containsAny(normalized, "summoning rite", "summon rite", "summoning ritual", "summon ritual")
                || (containsAny(normalized, "summon", "summoning") && containsAny(normalized, "explain", "rite", "ritual"));
    }

    private static boolean asksCurseRite(String normalized) {
        return containsAny(normalized, "curse rite", "curse ritual", "cursing rite", "cursing ritual")
                || (containsAny(normalized, "curse", "cursed") && containsAny(normalized, "explain", "rite", "ritual"));
    }

    private static boolean asksRiteSafety(String normalized) {
        return containsAny(normalized, "never do during a rite", "never do in a rite", "never do during ritual",
                "should i never do", "what not to do")
                && containsAny(normalized, "rite", "ritual", "circle");
    }

    private static boolean asksBeginnerPath(String normalized) {
        return containsAny(normalized, "safe beginner path", "beginner path", "first recipe",
                "first rite", "first ritual", "start learning", "safe to learn first",
                "learn first", "what should i learn first", "what is safe to learn");
    }

    private static boolean asksMixingWarning(String normalized) {
        return containsAny(normalized, "never mix", "should i never mix", "what should i never mix",
                "what not to mix", "bad mix", "dangerous mix");
    }

    private static boolean asksGrimoireSummary(String normalized) {
        return containsAny(normalized, "write in my grimoire", "write in the grimoire", "write down",
                "after this lesson", "grimoire after");
    }

    private static Optional<String> contextualFollowup(String normalized, String memoryContext, String playerName) {
        String memory = normalize(memoryContext);
        if (containsAny(normalized, "are they wrong", "were they wrong")
                && containsAny(memory, "village thinks you are trouble", "village think you are trouble")) {
            return Optional.of("Not entirely. Trouble follows me, " + playerName + "; I just keep mine better trained.");
        }
        if (containsAny(normalized, "did you bless me", "am i blessed", "your blessing", "you blessed me", "the luck")
                && containsAny(memory, "blessed with luck", "coven_witch_blessed")) {
            return Optional.of("A small luck, " + playerName + "; spend it before pride eats the shine off.");
        }
        if (containsAny(normalized, "did you curse me", "am i cursed", "your curse", "you cursed me", "still cursed")
                && containsAny(memory, "cursed with bad luck", "coven_witch_cursed")) {
            return Optional.of("A five-minute shadow, not a life sentence. Mind your tongue when it lifts.");
        }
        if (containsAny(normalized, "sorry", "apologize", "apology", "that was rude")
                && containsAny(memory, "cursed with bad luck", "insulting this witch")) {
            return Optional.of("Apology heard, " + playerName + "; let the shadow finish its walk, then mind your tongue.");
        }
        if (containsAny(normalized, "that herb", "the herb", "herb gift", "giving me a herb", "gave me a herb",
                "do you remember giving me a herb", "another herb", "more herbs")
                && containsAny(memory, "received one herb gift", "coven_witch_herb_gift")) {
            return Optional.of("I gave one herb freely, " + playerName + "; plant it, brew it, or owe it respect.");
        }
        if (containsAny(normalized, "what scares me", "what did i say scares me", "what did i say i fear",
                "what do i fear", "what am i afraid of", "nightmare", "nightmares", "bad dreams")
                && containsAny(memory, "wary of nightmares", "afraid of nightmares", "wary of nightmares",
                "fear nightmares", "fears nightmares")) {
            return Optional.of("I remember your nightmare fear, " + playerName + "; keep a Dream Catcher close and mark your way back.");
        }
        if (containsAny(normalized, "what did i say i fear", "what do i fear", "what am i afraid of")) {
            return Optional.of("You spoke of nightmares, " + playerName + "; keep a Dream Catcher close and mark your way back.");
        }
        if (containsAny(normalized, "what am i looking for", "what was i looking for", "still looking", "where start")
                && containsAny(memory, "looking for an arthana")) {
            return Optional.of("Still chasing the arthana, " + playerName + "? Ask after ritual blades and altar range, not table knives.");
        }
        if (containsAny(normalized, "that sounds creepy", "sounds creepy", "creepy")
                && containsAny(memory, "taglock", "poppet")) {
            return Optional.of("Creepy keeps fools careful. Taglocks are tools, not toys.");
        }
        if (containsAny(normalized, "how would i get back", "how do i get back", "get back", "return")
                && containsAny(memory, "spirit world", "sleeping brew", "nightmare")) {
            return Optional.of("Mark where you crossed, keep to return paths, and do not treat dreams like roads.");
        }
        if (containsAny(normalized, "come back after sunrise", "back after sunrise", "after sunrise")) {
            return Optional.of("Then come with daylight on your boots, " + playerName + "; night debts keep poor records.");
        }
        if (containsAny(normalized, "did you expect me back", "expected me back", "i came back", "came back")
                && containsAny(memory, "plans to come back after sunrise", "come back after sunrise")) {
            return Optional.of("I expected a shadow at sunrise; you brought boots. Good enough.");
        }
        if (containsAny(normalized, "village thinks you are trouble", "village think you are trouble")) {
            return Optional.of("Then your village has eyes. Trouble and I are old neighbors.");
        }
        if (containsAny(normalized, "any advice now", "advice now", "what now")) {
            if (containsAny(memory, "received one herb gift", "blessed with luck", "cursed with bad luck",
                    "looking for an arthana", "afraid of nightmares", "wary of nightmares")) {
                return Optional.of("Keep the gifted herb, keep a Dream Catcher close, and let the little shadow expire before boasting.");
            }
            return Optional.of("Keep salt dry, coin hidden, and questions sharper than fear.");
        }
        return Optional.empty();
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
                .trim();
        subject = playerMemoryPerspective(subject);
        if (subject.isBlank()) {
            return "I'll remember that, " + playerName + ".";
        }
        return NpcBrainResponse.sanitize("I'll remember that, " + playerName + ": " + subject + ".", 180);
    }

    private static String playerMemoryPerspective(String subject) {
        String text = subject == null ? "" : subject.trim();
        if (text.isBlank()) {
            return "";
        }
        text = text.replaceFirst("^i\\s+owe\\s+you\\b", "you owe me");
        text = text.replaceFirst("^i\\s+gave\\s+you\\b", "you gave me");
        text = text.replaceFirst("^i\\s+paid\\s+you\\b", "you paid me");
        text = text.replaceFirst("^i\\s+am\\s+afraid\\s+of\\b", "you are afraid of");
        text = text.replaceFirst("^i'm\\s+afraid\\s+of\\b", "you are afraid of");
        text = text.replaceFirst("^i\\s+am\\b", "you are");
        text = text.replaceFirst("^i'm\\b", "you are");
        text = text.replaceFirst("^i\\s+", "you ");
        text = text.replaceAll("\\bmy\\b", "your");
        return text.trim();
    }

    private static String tradeStockAnswer(String normalized) {
        String[] stock = {
                "witches' salt",
                "taglock kits",
                "fumes",
                "bat wool",
                "Revealing or Sleeping brews",
                "belladonna",
                "mandrake"
        };
        int start = Math.floorMod(normalized == null ? 0 : normalized.hashCode(), stock.length);
        StringBuilder answer = new StringBuilder("I trade ");
        for (int i = 0; i < stock.length; i++) {
            if (i > 0) {
                answer.append(i == stock.length - 1 ? ", and " : ", ");
            }
            answer.append(stock[(start + i) % stock.length]);
        }
        return answer.append("; right-click if you mean business.").toString();
    }

    private static String oldWitchAside(String normalized, String playerName) {
        return oldWitchAside(normalized, playerName, "");
    }

    private static String oldWitchAside(String normalized, String playerName, String avoidSpeech) {
        String[] lines = {
                "Speak louder, " + playerName + "; these old ears hear spirits better than mumbling.",
                "May your boots miss graves and your pockets find honest coin.",
                "The kettle says three bubbles and a cracked moon; make of that what you can.",
                "A crow once told me the same thing, or maybe it was a turnip.",
                "Mind your shadow after midnight; it learns bad habits.",
                "If the moon answers first, pretend you did not hear it.",
                "Never trust a quiet jar; silence ferments.",
                "Salt at the door, ash at the hearth, and questions where I can see them.",
                "A good omen arrives late and complains about the road.",
                "If you mean trouble, stand downwind so I can smell it coming.",
                "Some questions need rainwater. Some need teeth.",
                "Keep one pocket for coin and one for consequences.",
                "The old paths open for patience, not stomping.",
                "I have seen braver faces on boiled cabbage, but yours may yet improve.",
                "Ask again with fewer elbows in the sentence."
        };
        int start = Math.floorMod(normalized.hashCode(), lines.length);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[(start + i) % lines.length];
            if (avoidSpeech == null || avoidSpeech.isBlank()
                    || !NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech)) {
                return line;
            }
        }
        return lines[start];
    }

    private static String teachingAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "learn witchcraft", "start learning")) {
            return "Open your Witches' Primer, " + playerName + "; its first lesson is ash, herbs, circles, and patience.";
        }
        if (containsAny(normalized, "rite", "ritual", "circle")) {
            return "First rite lesson, " + playerName + ": draw the right chalk, feed the altar, place only named offerings, then stand back.";
        }
        if (containsAny(normalized, "brew", "potion", "cauldron", "kettle")) {
            return "First brew lesson: know the effect you want before the kettle knows your name.";
        }
        if (containsAny(normalized, "coven")) {
            return "First coven lesson: a familiar opens doors, but a witch's trial decides if you may enter.";
        }
        return "First lesson: salt wards, taglocks bind, and every rite asks payment before it asks permission.";
    }

    private static boolean isSpecificAnswerPrompt(String normalized) {
        return asksPlayerName(normalized)
                || asksNpcName(normalized)
                || asksTradeDetails(normalized)
                || asksMemory(normalized)
                || asksCovenRecruiting(normalized)
                || asksTeachMe(normalized)
                || containsAny(normalized, "time", "where are we", "where am i", "witches salt",
                "taglock", "poppet", "belladonna", "mandrake", "arthana", "altar", "rite", "ritual",
                "brew", "potion", "spirit world");
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
        return Optional.of(NpcBrainResponse.sanitize("I remember this, " + playerName + ": " + summary + ".", NpcBrainResponse.MAX_SPEECH_LENGTH));
    }

    private static String memorySummary(String memoryContext) {
        String cleaned = NpcBrainResponse.sanitize(memoryContext, 700);
        if (cleaned.isBlank()) {
            return "";
        }
        String longMemory = section(cleaned, "Long memory:", "Recent conversation:");
        String relationship = section(cleaned, "Relationship notes:", "Recent conversation:");
        String source = longMemory.isBlank() ? cleaned : longMemory;
        if (!relationship.isBlank()) {
            source = (source.isBlank() ? "" : source + " ") + relationship;
        }
        String rememberedPlayerName = playerNameFromMemory(cleaned);
        source = source
                .replaceAll("(?i)Entity memory:[^.]*\\.\\s*", "")
                .replaceAll("(?i)Recent conversation:", "")
                .replaceAll("(?i)Relationship notes:", "")
                .replaceAll("(?i)Long memory:", "")
                .replaceAll("player said \"[^\"]+\";\\s*", "")
                .replaceAll("witch answered \"[^\"]+\"\\.?", "")
                .replaceAll("(?i)\\b[A-Za-z0-9_]{2,24}(?:\\s+[A-Za-z0-9_]{2,24})?:\\s*[^;.!?]{0,140}(?:[;.!?]|$)", "");
        source = rewritePlayerMemoryPerspective(source, rememberedPlayerName)
                .replaceAll("(?i)without\\s+getting\\s+themselves\\s+killed", "without getting yourself killed")
                .replaceAll("(?i)([,;]\\s*)likes\\b", "$1you like")
                .replaceAll("(?i)([,;]\\s*)is\\s+wary\\s+of\\b", "$1you are wary of")
                .replaceAll("(?i)\\band\\s+is\\s+looking\\s+for\\b", "and you are looking for")
                .replaceAll("(?i)([,;]\\s*)is\\s+looking\\s+for\\b", "$1you are looking for")
                .replaceAll("(?i);\\s*(what\\s+now|now\\s+what|d)\\b", "")
                .replaceAll("(?i);\\s*none\\b", "")
                .replaceAll("[.!?]+\\s*", "; ")
                .replaceAll(";\\s*$", "")
                .replaceAll("\\s+", " ")
                .trim();
        return compactMemorySummary(source, 170);
    }

    private static String rewritePlayerMemoryPerspective(String source, String playerName) {
        String player = NpcBrainResponse.sanitize(playerName, 80);
        if (player.isBlank()) {
            return source;
        }
        String name = Pattern.quote(player);
        return source
                .replaceAll("(?i)\\b" + name + "\\s+received\\s+one\\s+herb\\s+gift\\s+from\\s+this\\s+witch\\b", "you received one herb gift from me")
                .replaceAll("(?i)\\b" + name + "\\s+was\\s+blessed\\s+with\\s+luck\\s+after\\s+praising\\s+this\\s+witch\\b", "you were blessed with luck after praising me")
                .replaceAll("(?i)\\b" + name + "\\s+was\\s+cursed\\s+with\\s+bad\\s+luck\\s+after\\s+insulting\\s+this\\s+witch\\b", "you were cursed with bad luck after insulting me")
                .replaceAll("(?i)\\b" + name + "\\s+has\\b", "you have")
                .replaceAll("(?i)\\b" + name + "\\s+likes\\b", "you like")
                .replaceAll("(?i)\\b" + name + "\\s+prefers\\s+to\\s+be\\s+called\\b", "you prefer to be called")
                .replaceAll("(?i)\\b" + name + "\\s+said\\s+they\\s+are\\b", "you said you are")
                .replaceAll("(?i)\\b" + name + "\\s+wants\\s+to\\s+learn\\b", "you want to learn")
                .replaceAll("(?i)\\b" + name + "\\s+is\\s+wary\\s+of\\b", "you are wary of")
                .replaceAll("(?i)\\b" + name + "\\s+is\\s+looking\\s+for\\b", "you are looking for")
                .replaceAll("(?i)\\b" + name + "\\s+is\\s+struggling\\s+with\\b", "you are struggling with")
                .replaceAll("(?i)\\b" + name + "\\s+plans\\s+to\\s+come\\s+back\\b", "you plan to come back")
                .replaceAll("(?i)\\b" + name + "'s\\s+village\\s+thinks\\b", "your village thinks");
    }

    private static String compactMemorySummary(String source, int maxLength) {
        String[] notes = source.split(";\\s*");
        StringBuilder summary = new StringBuilder();
        for (String raw : notes) {
            String note = raw.trim();
            if (note.isBlank()) {
                continue;
            }
            if (isBadMemorySummaryNote(note)) {
                continue;
            }
            if (summary.isEmpty()) {
                if (note.length() <= maxLength) {
                    summary.append(note);
                }
                continue;
            }
            if (summary.length() + 2 + note.length() > maxLength) {
                continue;
            }
            summary.append("; ").append(note);
        }
        return NpcBrainResponse.sanitize(summary.toString(), maxLength);
    }

    private static boolean isBadMemorySummaryNote(String note) {
        String normalized = normalize(note);
        return normalized.length() <= 3
                || (!normalized.startsWith("you ") && !normalized.startsWith("your "))
                || normalized.startsWith("what ")
                || normalized.startsWith("how ")
                || normalized.startsWith("where ")
                || normalized.startsWith("when ")
                || normalized.startsWith("why ")
                || normalized.startsWith("can ")
                || normalized.startsWith("do ")
                || normalized.startsWith("does ")
                || normalized.startsWith("should ")
                || normalized.startsWith("would ");
    }

    private static String playerNameFromMemory(String memoryContext) {
        Matcher matcher = Pattern.compile("(?i)player\\s+is\\s+([A-Za-z0-9_]{2,24})").matcher(memoryContext);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher fact = Pattern.compile("(?i)\\b([A-Za-z0-9_]{2,24})\\s+(?:has|likes|prefers\\s+to\\s+be\\s+called|said\\s+they\\s+are|wants\\s+to\\s+learn|is\\s+wary\\s+of|is\\s+looking\\s+for|is\\s+struggling\\s+with|plans\\s+to\\s+come\\s+back|received\\s+one\\s+herb\\s+gift|was\\s+blessed|was\\s+cursed)\\b")
                .matcher(memoryContext);
        return fact.find() ? fact.group(1) : "";
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

    private static String safeDisplayName(String value, String fallback) {
        String cleaned = NpcBrainResponse.sanitize(value, 40)
                .replaceAll("[^A-Za-z0-9_ .'-]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? fallback : cleaned;
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
        return normalized.matches(".*\\b(hello|hi|hey|greetings|good evening|good morning|good day)\\b.*");
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
