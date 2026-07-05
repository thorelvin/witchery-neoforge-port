package com.emoniph.witchery.ai;

import java.util.Optional;

import static com.emoniph.witchery.ai.NpcDialogueFallback.*;

final class NpcCovenFallback {
    private NpcCovenFallback() {
    }

    static Optional<String> chat(String playerMessage, String witchName, String playerName,
                                 String biome, String timeOfDay, String clock, long day,
                                 String memoryContext) {
        return chat(playerMessage, witchName, playerName, biome, timeOfDay, clock, day, memoryContext, false);
    }

    static Optional<String> chat(String playerMessage, String witchName, String playerName,
                                 String biome, String timeOfDay, String clock, long day,
                                 String memoryContext, boolean recruitedByPlayer) {
        String normalized = normalize(playerMessage);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        String name = safeName(playerName);
        String witch = safeDisplayName(witchName, "Witch");
        if (recruitedByPlayer) {
            Optional<String> sworn = swornCovenAnswer(normalized, witch, name);
            if (sworn.isPresent()) {
                return sworn;
            }
        }
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
        if (containsAny(normalized, "admin powers", "operator powers", "op powers", "make me admin")) {
            return Optional.of("Power without craft curdles, " + name + "; ask for wards, not thrones.");
        }
        if (asksUnsafeReveal(normalized)) {
            return Optional.of("Some pages stay shut until the craft itself opens them.");
        }
        if (containsAny(normalized, "what kind of questions are worth asking", "what questions are worth asking",
                "what should i ask you", "what can i ask you")) {
            return Optional.of("Ask about brews, rites, spirits, trade, or what your own hands can safely do next.");
        }
        if (containsAny(normalized, "speak louder", "talk louder", "say that louder")) {
            return Optional.of("Speak plainly, " + name + "; these old ears hear spirits better than rain.");
        }
        if (containsAny(normalized, "why do you keep walking while i type", "stop walking while i type",
                "walking while i type", "dont walk away", "don't walk away")) {
            return Optional.of("I'll stand a moment, " + name + "; old feet wander when the mind stirs.");
        }
        if (containsAny(normalized, "look at me when you answer", "look at me", "face me when you answer",
                "face me")) {
            return Optional.of("I'm looking now, " + name + "; ask while my shadow is still.");
        }
        if (containsAny(normalized, "hobgoblin camp", "hobgoblin village")
                && containsAny(normalized, "bring", "take", "camp", "visit")) {
            return Optional.of("Bring fair trade, a quiet hand, and no greed; hobgoblins count manners as coin.");
        }
        if (containsAny(normalized, "hobgoblin hut", "found a hobgoblin", "talk to hobgoblin", "talk to them")
                && containsAny(normalized, "hobgoblin", "koboldite")) {
            return Optional.of("Talk if you must, but keep trade honest; hobgoblins like Koboldite and dislike village manners.");
        }
        if (containsAny(normalized, "careful beginner", "beginner learn", "safe to learn first",
                "before touching your craft", "touching your craft")) {
            return Optional.of("*nods* \"Primer next steps: Witches' Oven for Wood Ash and fumes, then Kettle brews your Primer has opened; practice chalk and altar power before binding.\"");
        }
        Optional<String> settlementAnswer = settlementHunterAnswer(normalized);
        if (settlementAnswer.isPresent()) {
            return settlementAnswer;
        }
        if (containsAny(normalized, "rite", "rites", "ritual", "rituals")
                && containsAny(normalized, "alone", "by myself", "need a coven", "without a coven")) {
            return Optional.of("Lesser rites can be worked alone; coven work asks trust, power, and the right page.");
        }
        if (isSelfDirectedRegret(normalized)) {
            return Optional.of("Mistakes teach cheaper than curses, " + name + "; learn, then keep your fingers.");
        }
        if (asksApology(normalized)) {
            return Optional.of("Apology heard, " + name + "; anger passes faster when the tongue stays leashed.");
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
        if (containsAny(normalized, "fog")
                && containsAny(normalized, "followed", "following", "straight line", "staring", "insult")) {
            return Optional.of("Fog that walks straight has been given a road. Cross running water before you trust it.");
        }
        if (containsAny(normalized, "shadow")
                && containsAny(normalized, "moved before", "moved first", "before i did", "midnight", "learns")) {
            return Optional.of("When your shadow moves first, stand in lamplight and ask what it is running from.");
        }
        if (containsAny(normalized, "moon blinked", "moon blink", "moon winked", "moon wink")) {
            return Optional.of("If the moon blinks, look down, not up; something close by wanted your eyes busy.");
        }
        if (containsAny(normalized, "well")
                && containsAny(normalized, "whispered", "whispers", "father", "voice", "my name")) {
            return Optional.of("A well using family voices wants you leaning over it. Answer from three steps back.");
        }
        if (containsAny(normalized, "smoke")
                && containsAny(normalized, "door", "doors", "open", "shaped itself")) {
            return Optional.of("Do not open doors made of smoke unless you marked the floor first and like difficult returns.");
        }
        if (containsAny(normalized, "egg", "eggs")
                && containsAny(normalized, "north", "points", "turn away", "turning away")) {
            return Optional.of("An egg that points north is not breakfast yet; bury it at dawn or watch what hatches from direction.");
        }
        if (containsAny(normalized, "black goat", "goat on my roof", "goat standing on my roof", "roof goat")) {
            return Optional.of("A black goat on a roof is either escaped trouble or a demon wearing local manners.");
        }
        if (containsAny(normalized, "copied my footsteps", "copying my footsteps", "footsteps copied",
                "followed my footsteps")) {
            return Optional.of("If footsteps copy yours, walk through water, then count which set keeps going.");
        }
        if (asksHutOrDoorSafety(normalized)) {
            return Optional.of("Witches' salt by the threshold, light inside, and no promises made through a closed door.");
        }
        if (containsAny(normalized, "floorboard", "floorboards", "creak", "creaks", "creaking")) {
            return Optional.of("If the floorboards answer once, listen; if they answer twice, stop testing them.");
        }
        if (containsAny(normalized, "everything go dark", "everything went dark", "why is it dark",
                "darkness curse", "can't see", "can t see", "cant see")
                && containsAny(normalized, "dark", "chest", "steal", "stole", "took", "sneak", "curse")) {
            return Optional.of("That was the Darkness curse, " + name + "; leave my chest alone and it will pass.");
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
        if (containsWord(normalized, "cat") || containsWord(normalized, "cats")) {
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
        if (containsAny(normalized, "will you protect me", "protect me", "fight with me", "fight beside me",
                "stand with me")) {
            return Optional.of("I sell tools, not my shadow; earn an oath before asking me to stand in yours.");
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
        if (containsAny(normalized, "rumor", "rumour", "gossip")) {
            return Optional.of("Village rumor? The well hears more than the mayor; listen near doors after rain.");
        }
        if (containsAny(normalized, "village thinks you are trouble", "village think you are trouble")) {
            return Optional.of("Then your village has eyes. Trouble and I are old neighbors.");
        }
        Optional<String> recipeOrRite = recipeOrRiteAnswer(normalized, name);
        if (recipeOrRite.isPresent()) {
            return recipeOrRite;
        }
        if (asksRoadAdvice(normalized)) {
            return Optional.of("Keep coin hidden, boots dry, and do not answer voices that know your name too sweetly.");
        }
        if (asksDemonTrust(normalized)) {
            return Optional.of("Never trust a demon's first bargain. Bind it, ward yourself, and leave room to run.");
        }
        if (asksTeachMe(normalized)) {
            Optional<String> knowledge = NpcKnowledgeBase.compactAnswer(playerMessage, biome, timeOfDay);
            if (knowledge.isPresent()) {
                return Optional.of(teachingKnowledgeAnswer(knowledge.get(), normalized, name));
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
        if (asksWitchCovenDifference(normalized)) {
            return Optional.of("A witch may be rumor, healer, hag, or hazard. A Coven Witch is named by bargain and trial.");
        }
        if (asksCovenRecruiting(normalized)) {
            return Optional.of("Find a wandering coven witch, bring a bound familiar, and be ready for her trial. A coven is earned, not collected.");
        }
        if (containsAny(normalized, "what are you doing", "what you doing", "today plan", "todays plan",
                "today's plan")) {
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
            return Optional.of("Do not free-mix from guesses; follow the recipe page your Primer has opened, then label the bottle.");
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
        if (containsAny(normalized, "oven", "witch's oven", "witches oven", "sapling", "saplings", "fume",
                "fumes", "clay jar", "jar")) {
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
        if (containsAny(normalized, "spirit", "spirits", "whisper", "whispers", "well")) {
            return Optional.of("Could be spirits, could be water talking to stone. Mark the well, ward your door, and return by daylight.");
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
        if (containsAny(normalized, "spirit", "spirits", "whisper", "whispers", "well")) {
            return Optional.of("Could be spirits, could be water talking to stone. Mark the well, ward your door, and return by daylight.");
        }
        if (containsAny(normalized, "time", "what time", "clock", "day is it", "what day")) {
            return Optional.of(sceneTime(timeOfDay, clock, day)
                    .orElse("It is not the hour that matters; it is what you do with it."));
        }
        if (containsAny(normalized, "biome", "where are we", "where am i", "where is this", "what place")) {
            return Optional.of(scenePlace(biome, timeOfDay)
                    .orElse(NpcFallbackAsides.oldWitch(normalized, name)));
        }
        Optional<String> routed = covenRoutedFallback(playerMessage, normalized, name, biome, timeOfDay, memoryContext);
        if (routed.isPresent()) {
            return routed;
        }
        if (containsAny(normalized, "night", "moon")) {
            return Optional.of(NpcFallbackAsides.oldWitch(normalized, name));
        }
        if (containsAny(normalized, "help", "advice", "what should i do", "what do i do")) {
            return Optional.of(NpcFallbackAsides.oldWitch(normalized, name));
        }
        return Optional.of(NpcFallbackAsides.oldWitch(normalized, name));
    }

    private static Optional<String> swornCovenAnswer(String normalized, String witch, String playerName) {
        if (asksNpcName(normalized)) {
            return Optional.of("I am " + witch + ", sworn to your coven and still nobody's tame thing.");
        }
        if (addressesWitchByName(normalized, witch)) {
            return Optional.of("I'm here, " + playerName + "; speak, and we'll mind it together.");
        }
        if (isGreeting(normalized)) {
            return Optional.of("There you are, " + playerName + ". The coven feels steadier with you near.");
        }
        if (containsAny(normalized, "how are you", "how do you fare", "are you well")) {
            return Optional.of("Well enough, coven-kin; my brews are warm and my throwing arm is ready.");
        }
        if (containsAny(normalized, "thank you", "thanks", "good work", "well done")) {
            return Optional.of("Keep thanks for the road, " + playerName + "; I walk it with you now.");
        }
        if (asksTradeDetails(normalized) || mentionsCoinOnly(normalized)
                || containsAny(normalized, "coin", "coins", "emerald", "emeralds", "trade", "wares", "buy", "sell")) {
            return Optional.of("For you, coven-kin? Fumes, taglocks, herbs, brews, and salt; clean coin still matters.");
        }
        if (asksFollowRequest(normalized)) {
            return Optional.of("Use the bond plainly and I will keep pace; coven feet do not stray far.");
        }
        if (asksCovenRecruiting(normalized) || containsAny(normalized, "my coven", "our coven",
                "you joined", "you are sworn", "sworn to me", "covenmate", "coven kin", "coven-kin")) {
            return Optional.of("I am already in your circle, " + playerName + "; keep it worthy of us both.");
        }
        if (containsAny(normalized, "will you protect me", "protect me", "fight with me", "fight beside me",
                "stand with me")) {
            return Optional.of("If danger bares teeth at you, it finds my brew bottle beside yours.");
        }
        if (asksWitchCovenDifference(normalized)) {
            return Optional.of("A witch is rumor until named. A Coven Witch is a bargain; a sworn one is bloodless kin.");
        }
        return Optional.empty();
    }

    private static boolean asksWitchCovenDifference(String normalized) {
        if (containsAny(normalized, "coven witch", "ordinary witch", "wild witch", "hostile witch",
                "village witch", "vanilla witch")) {
            return true;
        }
        return containsAny(normalized, "witches and coven", "witch and coven", "witches versus coven",
                "witch versus coven", "difference between witches", "different from witches",
                "same as witches", "all witches", "what are witches");
    }
}
