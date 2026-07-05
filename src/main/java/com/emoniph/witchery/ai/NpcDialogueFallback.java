package com.emoniph.witchery.ai;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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

    static Optional<String> leonardChat(String playerMessage, String leonardName, String playerName,
                                        String phase, String altarInfo, String covenInfo, String gearInfo,
                                        String memoryContext) {
        return NpcLeonardFallback.chat(playerMessage, leonardName, playerName, phase, altarInfo, covenInfo,
                gearInfo, memoryContext);
    }

    static Optional<String> reflectionChat(String playerMessage, String reflectionName, String playerName,
                                           String ownerName, String reflectionState, String gearInfo,
                                           String memoryContext) {
        return NpcReflectionFallback.chat(playerMessage, reflectionName, playerName, ownerName, reflectionState,
                gearInfo, memoryContext);
    }

    static Optional<String> babaYagaChat(String playerMessage, String babaName, String playerName,
                                         String bossInfo, String gearInfo, String memoryContext) {
        return NpcBabaYagaFallback.chat(playerMessage, babaName, playerName, bossInfo, gearInfo, memoryContext);
    }

    static Optional<String> hobgoblinChat(String playerMessage, String hobgoblinName, String playerName,
                                          String workInfo, String patronInfo, String memoryContext) {
        return NpcHobgoblinFallback.chat(playerMessage, hobgoblinName, playerName, workInfo, patronInfo,
                memoryContext);
    }

    static Optional<String> villagerChat(String playerMessage, String villagerName, String playerName,
                                         String profession, String villageInfo, String memoryContext) {
        return NpcVillagerFallback.chat(playerMessage, villagerName, playerName, profession, villageInfo,
                memoryContext);
    }

    static Optional<String> covenChat(String playerMessage, String witchName, String playerName,
                                      String biome, String timeOfDay, String clock, long day,
                                      String memoryContext) {
        return NpcCovenFallback.chat(playerMessage, witchName, playerName, biome, timeOfDay, clock, day,
                memoryContext);
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
        return NpcFallbackAsides.oldWitch(normalized.isBlank() ? "mumble" : normalized, name, avoidSpeech);
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
        return Optional.of(NpcFallbackAsides.oldWitch(normalized, safeName(playerName), avoidSpeech));
    }

    static String leonardLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.leonard(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> leonardAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String line = NpcFallbackAsides.leonard(normalize(playerMessage), safeName(playerName), avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    static String reflectionLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.reflection(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> reflectionAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String line = NpcFallbackAsides.reflection(normalize(playerMessage), safeName(playerName), avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    static String babaYagaLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.babaYaga(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> babaYagaAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String line = NpcFallbackAsides.babaYaga(normalize(playerMessage), safeName(playerName), avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    static String hobgoblinLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.hobgoblin(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> hobgoblinAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String line = NpcFallbackAsides.hobgoblin(normalize(playerMessage), safeName(playerName), avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    static String villagerLastResort(String playerMessage, String playerName, String avoidSpeech) {
        return NpcFallbackAsides.villager(normalize(playerMessage), safeName(playerName), avoidSpeech);
    }

    static Optional<String> villagerAlternate(String playerMessage, String playerName, String avoidSpeech) {
        String normalized = normalize(playerMessage);
        String name = safeName(playerName);
        Optional<String> specific = villagerSpecificAlternate(normalized, name, avoidSpeech);
        if (specific.isPresent()) {
            return specific;
        }
        String line = NpcFallbackAsides.villager(normalized, name, avoidSpeech);
        return NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech) ? Optional.empty() : Optional.of(line);
    }

    private static Optional<String> villagerSpecificAlternate(String normalized, String playerName, String avoidSpeech) {
        String line = "";
        if (asksVillagerHarvest(normalized) || containsAny(normalized, "farmer say", "farmer would say")) {
            line = "A farmer would touch the soil before boasting. Bread starts with mud, not luck.";
        } else if (asksVillagerWeather(normalized)) {
            line = "Weather is work first and omen second: roofs, roads, smoke, then crows.";
        } else if (containsAny(normalized, "ring the bell", "if i ring", "when i ring")) {
            line = "Ring for danger; folk run for doors, and brave fools look outward.";
        } else if (containsAny(normalized, "who watches the roads", "watch roads", "road after sunset", "roads after sunset")) {
            line = "Guards, golems, and lantern fools watch roads after sunset. Sensible folk bar doors.";
        } else if (asksTaglockOrHairSafety(normalized)) {
            line = "Keep hair, names, and taglocks close. Small leavings make large trouble.";
        } else if (asksDemonVillagerOpinion(normalized)) {
            line = "Demons get bells, barred doors, and no bargains. Let witches handle binding.";
        } else if (asksGoodBadWitches(normalized) || asksWitchTrust(normalized) || asksWitchDebtOrRespect(normalized)) {
            line = "Judge witches by deeds: clean cures, fair trade, kept promises, and no hidden hair bottles.";
        } else if (containsAny(normalized, "whiff", "whif", "mutandis", "witchery recipe", "witchery recipes")) {
            line = "I know the names from counters. Proper witch work belongs to a Primer or a witch.";
        }
        if (line.isBlank() || NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech)) {
            return Optional.empty();
        }
        return Optional.of(line);
    }

    static Optional<String> leonardRoutedFallback(String normalized, String playerName,
                                                  String memoryContext) {
        if (!NpcMessageAnalysis.hasRoutingContext(memoryContext)) {
            return Optional.empty();
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "leonard_summoning")
                || (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "recipe")
                    && containsAny(normalized, "order", "again", "calling", "summon", "after"))) {
            return Optional.of("Nether Wart, Tear of the Goddess, Diamond Vapour, Diamond, Nether Star; then hat, cauldron, and ten thousand altar power.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "mortal_coil")) {
            return Optional.of("Mortal Coil is my withering mark on attackers; survive the debt or end the fight quickly.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "green_soul")) {
            return Optional.of("Green souls take steel; break them before they lend me their shield.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "red_soul")) {
            return Optional.of("Red souls fear fire and blast. Burn them or break them with explosions before you waste steel.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "black_soul")) {
            return Optional.of("Black souls yield to magic and potions. Bring witchcraft, not swagger.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "lost_souls")) {
            return Optional.of("Break the souls first. Green takes steel, red fears fire and blast, black yields to magic and potions.");
        }
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "demonbane")) {
            return Optional.of("Demonbane bites deepest when I am already wounded. Bring it late, not proudly early.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "atmosphere")) {
            return Optional.of(leonardAtmosphereAnswer(normalized, playerName));
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "fear")) {
            return Optional.of("Fear is the one honest scout you brought, " + playerName + ". Listen, but do not let it hold the blade.");
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "confusion")) {
            return Optional.of("Name one debt at a time, " + playerName + "; altar, souls, urn, or blade.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "action")) {
            return Optional.of("I do not perform wishes. Bring a price, a threat, or a sharper question.");
        }
        return Optional.empty();
    }

    static Optional<String> babaYagaRoutedFallback(String normalized, String playerName,
                                                   String memoryContext) {
        if (!NpcMessageAnalysis.hasRoutingContext(memoryContext)) {
            return Optional.empty();
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "trade")) {
            return Optional.of("Trade? No. I take manners, mistakes, and sometimes the rest of you.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "atmosphere")) {
            return Optional.of(babaYagaAtmosphereAnswer(normalized, playerName));
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "lore")) {
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "baba_yaga_folklore")
                    || containsAny(normalized, "hut", "chicken legs", "forest", "skull", "teeth",
                    "mortar", "pestle", "broom")) {
                return Optional.of("My old stories have chicken-legged huts, skull-lamps, mortar, pestle, broom, and teeth enough for liars.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "baba_yaga_combat")
                    || containsAny(normalized, "fight", "weakness", "wood", "bolt", "thorns")) {
                return Optional.of("Wood and nullifying bolts bite where proud steel pays thorns; my potions punish the rest.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "baba_yaga_potions")
                    || containsAny(normalized, "potion", "brew", "throw")) {
                return Optional.of("Poison, blindness, slowness, weakness, wither; little bottles for loud feet.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "baba_yaga_spiders")
                    || containsAny(normalized, "spider", "animal", "pet", "undead")) {
                return Optional.of("Send dead things or wild pets at me, and spiders may crawl home wearing their mistake.");
            }
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "fear")) {
            return Optional.of("Good. Fear keeps the meat awake, " + playerName + ".");
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "confusion")) {
            return Optional.of("Ask with teeth: potions, thorns, wood, bolts, spiders, or my old road.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "action")) {
            return Optional.of("I do not perform little wishes. Bring manners, danger, or a better question.");
        }
        return Optional.empty();
    }

    static Optional<String> villagerRoutedFallback(String normalized, String playerName,
                                                   String profession, String villageInfo,
                                                   String memoryContext) {
        if (!NpcMessageAnalysis.hasRoutingContext(memoryContext)) {
            return Optional.empty();
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "trade")) {
            return Optional.of(villagerTradeAnswer(profession));
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "atmosphere")) {
            return Optional.of(villagerAtmosphereAnswer(normalized, playerName, profession));
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "lore")) {
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "village") || containsAny(normalized, "village", "safe", "walls")) {
                return Optional.of("From here I know this much: " + villageFactSummary(villageInfo)
                        + ". The bell knows the rest.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_weather") || asksVillagerWeather(normalized)) {
                return Optional.of(villagerWeatherAnswer(normalized, playerName));
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_harvest") || asksVillagerHarvest(normalized)) {
                return Optional.of(villagerHarvestAnswer(normalized, playerName, profession));
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_witch_morality") || asksGoodBadWitches(normalized)) {
                return Optional.of("Good witches mend and trade fair; bad ones hide taglocks and threats. I watch deeds, not hats.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_witch_respect") || asksWitchDebtOrRespect(normalized)) {
                return Optional.of("Some witches are owed respect. Not comfort, mind you; respect, paid with wary eyes.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "villager_demons") || asksDemonVillagerOpinion(normalized)) {
                return Optional.of("Demons mean doors shut, bells loud, and no bargaining. Let witches who know binding earn their respect.");
            }
            if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "witch") || NpcMessageAnalysis.routingMatchesTopic(memoryContext, "curse")) {
                return Optional.of("Witch trouble starts small: missing hair, odd bottles, neighbors whispering too softly.");
            }
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "fear")) {
            return Optional.of("Fear keeps villagers alive, " + playerName + ". Doors first, questions second.");
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "confusion")) {
            return Optional.of("Ask me plainly: village, golem, walls, raid, trade, or night trouble.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "action")) {
            return Optional.of("I can warn, trade, or point at trouble. I cannot make the village brave by talking.");
        }
        return Optional.empty();
    }

    static Optional<String> covenRoutedFallback(String playerMessage, String normalized,
                                                String playerName, String biome, String timeOfDay,
                                                String memoryContext) {
        if (!NpcMessageAnalysis.hasRoutingContext(memoryContext)) {
            return Optional.empty();
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "trade")) {
            return Optional.of(tradeStockAnswer(normalized));
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "recipe")) {
            Optional<String> answer = routedRecipeAnswer(playerMessage, biome, timeOfDay, memoryContext);
            if (answer.isPresent()) {
                return answer;
            }
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "atmosphere")) {
            return Optional.of(covenAtmosphereAnswer(normalized, playerName));
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "fear")) {
            return Optional.of("Fear is useful, " + playerName + "; it keeps fingers attached. Name the danger and I will name the ward.");
        }
        if (NpcMessageAnalysis.routingMatchesTone(memoryContext, "confusion")) {
            return Optional.of("One question at a time, " + playerName + ": brew, rite, spirit, trade, or trouble?");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "social")) {
            return Optional.of("Tongue heard, " + playerName + ". Now spend it on a real question.");
        }
        if (NpcMessageAnalysis.routingMatchesRoute(memoryContext, "action")) {
            return Optional.of("Some requests need hands, not wishes. Tell me what you want done and what you can pay.");
        }
        return Optional.empty();
    }

    private static Optional<String> routedRecipeAnswer(String playerMessage, String biome, String timeOfDay,
                                                       String memoryContext) {
        String question = "";
        if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "whiff_of_magic")) {
            question = "how do I make whiff of magic";
        } else if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "mutandis")) {
            question = "how do I make mutandis";
        } else if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "attuned_stone")) {
            question = "how do I make attuned stone";
        } else if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "brew_of_sleeping")) {
            question = "how do I make brew of sleeping";
        } else if (NpcMessageAnalysis.routingMatchesTopic(memoryContext, "brew_of_revealing")) {
            question = "how do I make brew of revealing";
        } else {
            question = playerMessage;
        }
        return NpcKnowledgeBase.compactAnswer(question, biome, timeOfDay);
    }

    static String villagerJobAnswer(String profession) {
        return switch (profession) {
            case "apothecary" ->
                    "I'm the apothecary; herbs for coughs, jars for remedies, and gossip if it keeps blood inside.";
            case "farmer" ->
                    "I work the fields; bread quiets panic, and crows tell me when weather lies.";
            case "cleric" ->
                    "I keep the shrine, mind the bell, and listen when grave dirt refuses to settle.";
            case "librarian" ->
                    "I keep books and old sayings. One shelves better, but the other saves doors.";
            case "cartographer" ->
                    "I draw roads and borders, mostly so folk know which paths to avoid after dark.";
            case "armorer" ->
                    "I shape armor and count dents; iron speaks plainer than gossip.";
            case "weaponsmith" ->
                    "I make blades and hear who thinks courage lives in sharp metal.";
            case "toolsmith" ->
                    "I keep tools working. A village falls apart one broken axe and one bad nail at a time.";
            case "mason" ->
                    "I work stone and judge walls. Bad foundations gossip louder than villagers.";
            case "fletcher" ->
                    "I make arrows and watch the road. Movement at dusk has a language.";
            case "fisherman" ->
                    "I fish and listen to water. Wells repeat the strangest things after rain.";
            case "shepherd" ->
                    "I mind wool and fences; sheep know fear before people admit it.";
            case "butcher" ->
                    "I keep the meat stores honest. Hunger makes worse monsters than night does.";
            case "leatherworker" ->
                    "I work hides, straps, and cauldrons. Boots carry luck until the road bites.";
            case "nitwit" ->
                    "Me? I notice things clever folk walk past, then call luck when they trip.";
            case "unemployed" ->
                    "No trade yet. I know which stations are missing and who hides when bells ring.";
            default ->
                    "I work where the village needs me, and worry where the village forgets to.";
        };
    }

    static String villagerTradeAnswer(String profession) {
        return switch (profession) {
            case "apothecary" ->
                    "I sell herbs, jars, simple remedies, and what the trade screen honestly shows.";
            case "farmer" ->
                    "Farmers trade food and crop goods. Bread is cheaper before folk get frightened.";
            case "cleric" ->
                    "Clerics trade shrine goods and oddities; ask at the stand, not in the road.";
            case "librarian" ->
                    "Books, paper, and secrets with covers. The trade screen gives the honest price.";
            case "armorer", "weaponsmith", "toolsmith" ->
                    "Iron work, if the stand is stocked. Good metal costs less than panic.";
            case "cartographer" ->
                    "Maps and paper work, mostly. Roads become cheaper when you know which ones are cursed.";
            case "mason" ->
                    "Stone, clay, and patient work. Nothing glamorous survives weather without patience.";
            case "fletcher" ->
                    "Sticks, string, arrows, bows. Useful when the road grows teeth.";
            case "fisherman" ->
                    "Fish and river work. Not fancy, but fancy folk still eat.";
            case "shepherd" ->
                    "Wool and dyes, mostly. Warm beds calm brave mouths.";
            case "butcher" ->
                    "Meat and animal goods. Hunger makes everyone bargain badly.";
            case "leatherworker" ->
                    "Leather and cauldron work. Travel gear matters when home stops feeling safe.";
            default ->
                    "Right-click a working villager for real stock; gossip is free, goods are not.";
        };
    }

    static boolean asksGoodBadWitches(String normalized) {
        return containsAny(normalized, "good witch", "bad witch", "good witches", "bad witches",
                "kind witch", "evil witch", "safe witch", "trust witch", "trust the witch",
                "witches good", "witches bad", "are witches good", "are witches bad",
                "witch is bad news", "witch bad news", "bad news witch", "witch heals",
                "heals someone", "make her good", "sells herbs", "good sign");
    }

    static boolean asksWitchTrust(String normalized) {
        return containsAny(normalized, "trust the witch", "trust a witch", "avoid offending",
                "without trusting", "get along", "trade fairly", "fairly with a good witch");
    }

    static boolean asksWitchDebtOrRespect(String normalized) {
        return containsAny(normalized, "respect witch", "respect witches", "respect a witch", "respect the witch",
                "respect for witches", "owe a witch", "owe the witch", "debt to a witch",
                "witch saved", "witch helped", "curious about witches",
                "why fear witches", "villagers respect witches");
    }

    static boolean asksDemonVillagerOpinion(String normalized) {
        return containsAny(normalized, "what do villagers think about demons", "what do villagers think of demons",
                "what do you think about demons", "what do you think of demons", "demon", "demons", "devil",
                "imp", "flame imp", "demon bargain");
    }

    static boolean asksTaglockOrHairSafety(String normalized) {
        return containsAny(normalized, "hair in my brush", "hair missing", "asks for my hair",
                "witch asks for hair", "give my hair", "keep a taglock safe", "taglock safe",
                "keep my name safe", "my hair", "my taglock");
    }

    static boolean asksVillagerWeather(String normalized) {
        return containsAny(normalized, "weather", "rain", "raining", "storm", "sky", "cloud",
                "clouds", "wind", "snow", "clear day", "sunny");
    }

    static boolean asksVillagerHarvest(String normalized) {
        return containsAny(normalized, "harvest", "crops", "crop", "field", "fields", "planting",
                "wheat", "bread", "food stores", "hungry", "hunger", "famine", "soil", "seed", "seeds");
    }

    static String villagerWeatherAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "storm", "thunder", "lightning")) {
            return "Storm weather means doors shut and bell close, " + playerName + "; roofs leak before courage does.";
        }
        if (containsAny(normalized, "rain", "raining")) {
            return "Rain fattens mud and gossip. Good for fields, bad for roads, and worse for nervous sheep.";
        }
        if (containsAny(normalized, "snow")) {
            return "Snow hides tracks until it chooses to show them. Keep stores dry and boots by the door.";
        }
        if (containsAny(normalized, "clear", "sunny", "sun")) {
            return "Clear sky is work weather. Mend fences now, before dusk starts counting our doors.";
        }
        return "Weather tells on a village: smoke, crows, wet roads, and animals know before mayors do.";
    }

    static String villagerHarvestAnswer(String normalized, String playerName, String profession) {
        if (containsAny(normalized, "famine", "hungry", "hunger", "food stores")) {
            return "Hungry villages grow sharp tongues. Count bread first, then count monsters.";
        }
        if (containsAny(normalized, "rain", "mud", "soil")) {
            return "Good soil wants rain, not flood. Too much mud rots roots and patience alike.";
        }
        if ("farmer".equals(profession)) {
            return "Harvest needs seed, sun, and fewer crows than last year. Bread is our quietest ward.";
        }
        return "Harvest keeps a village brave. No bread in the chest, and every shadow looks taller.";
    }

    static String villageFactSummary(String villageInfo) {
        String text = NpcBrainResponse.sanitize(villageInfo, 260);
        if (text.isBlank()) {
            return "too little from here to swear by";
        }
        text = text.replace("nearby ", "")
                .replace("threats: ", "")
                .replace("; ", ", ");
        return NpcBrainResponse.sanitize(text, 180);
    }

    static int countAfter(String villageInfo, String label) {
        String text = villageInfo == null ? "" : villageInfo.toLowerCase(Locale.ROOT);
        Matcher matcher = Pattern.compile(Pattern.quote(label.toLowerCase(Locale.ROOT)) + "\\s+(\\d+)")
                .matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    static String article(String word) {
        if (word == null || word.isBlank()) {
            return "a";
        }
        char first = Character.toLowerCase(word.charAt(0));
        return first == 'a' || first == 'e' || first == 'i' || first == 'o' || first == 'u' ? "an" : "a";
    }

    private static String covenAtmosphereAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "chest", "stealing", "looking into")) {
            return "Mind those fingers, " + playerName + "; chests learn names faster than jars do.";
        }
        if (containsAny(normalized, "primer", "book", "pages")) {
            return "Read slowly. Primer pages bite less when you follow them in order.";
        }
        if (containsAny(normalized, "invisible", "unseen", "hidden")) {
            return "*narrows her eyes* \"Empty places breathe loudly, " + playerName + ".\"";
        }
        if (containsAny(normalized, "sneaking", "crouching")) {
            return "Bad spies creak less than you do. Stand plain or steal better.";
        }
        if (containsAny(normalized, "jumping", "hopping")) {
            return "*shrugs* \"Testing broom legs, or has a frog cursed your boots?\"";
        }
        if (containsAny(normalized, "rain", "weather", "storm", "crows", "harvest")) {
            return "Weather talks in wet hems, restless crows, and herbs that refuse to dry.";
        }
        return NpcFallbackAsides.rotatingLine(normalized, new String[] {
                "Loitering is a craft too, " + playerName + "; most do it poorly.",
                "If you are staying, stop breathing on the jars.",
                "Hut rules are simple: hands visible, questions sharper than elbows.",
                "The kettle has patience. I do not always borrow it."
        });
    }

    private static String leonardAtmosphereAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "rain", "weather", "mud")) {
            return "Rain counts debts better than priests, " + playerName + "; every drop finds what is owed.";
        }
        if (containsAny(normalized, "road", "crossroad", "threshold", "door")) {
            return "Thresholds remember who crossed clean and who crawled back priced.";
        }
        if (containsAny(normalized, "village", "stayed in the village", "fear", "afraid")) {
            return "Village doors teach one useful lesson: fear knows the latch before courage does.";
        }
        if (containsAny(normalized, "silence", "shadow", "night")) {
            return "Silence is not empty. It is deciding what your courage costs.";
        }
        return NpcFallbackAsides.rotatingLine(normalized, new String[] {
                "Even the air measures you, " + playerName + ". Try not to come up short.",
                "Your omens are small, but small debts still collect.",
                "The altar hears your mood. I hear the price under it.",
                "Name the weather if you must; debt hears the price under it."
        });
    }

    private static String babaYagaAtmosphereAnswer(String normalized, String playerName) {
        if (containsAny(normalized, "rain", "swamp", "mud", "boots", "wet")) {
            return "Swamps breathe through mud, " + playerName + ". Step lightly, or they learn your weight.";
        }
        if (containsAny(normalized, "crow", "crows", "raven", "ravens")) {
            return "Crows circle what is foolish, hungry, or nearly dead. Decide which you are.";
        }
        if (containsAny(normalized, "road", "roads", "lost", "door", "threshold", "dawn")) {
            return "Lost roads know my door. Found roads usually regret the courtesy.";
        }
        if (containsAny(normalized, "shadow", "silence", "night", "lantern", "reeds", "something moved")) {
            return "The night hides its teeth badly. That is why lanterns look so nervous.";
        }
        if (containsAny(normalized, "manners", "not weapons", "voice low", "listen")) {
            return "Manners are a thin knife, " + playerName + ". Hold them steady.";
        }
        return NpcFallbackAsides.rotatingLine(normalized, new String[] {
                "The forest has kinder old women. It did not send you to one.",
                "Old pots remember careless mouths, " + playerName + ".",
                "*sniffs* \"You brought words, " + playerName + ". I was hoping for bones.\"",
                "Good. Fear keeps the meat awake, " + playerName + ".",
                "Little omens grow teeth if you feed them after dark."
        });
    }

    private static String villagerAtmosphereAnswer(String normalized, String playerName, String profession) {
        if (containsAny(normalized, "rain", "weather", "storm", "mud", "boots", "road")) {
            return villagerWeatherAnswer(normalized, playerName);
        }
        if (containsAny(normalized, "harvest", "field", "fields", "crop", "crops", "bread", "hungry", "hunger")) {
            return villagerHarvestAnswer(normalized, playerName, profession);
        }
        if (containsAny(normalized, "demon", "demons")) {
            return "Demons mean doors shut, bells loud, and no bargains. Let witches handle binding.";
        }
        if (containsAny(normalized, "witch", "witches", "good witch", "bad witch")) {
            return "Good witches leave cures and fair trades. Bad ones leave bottles with your name in them.";
        }
        if (containsAny(normalized, "silence", "night", "something moved", "lantern", "threshold")) {
            return "Bread in your pocket, door at your back, and no wandering after the bell.";
        }
        return NpcFallbackAsides.rotatingLine(normalized, new String[] {
                "My gran said odd signs travel in threes. I stop counting at two.",
                "Keep near the road, " + playerName + ", but not where the road can choose for you.",
                "Villages survive by making small fears useful.",
                "If the bell knows before we do, we still call that wisdom."
        });
    }

    static Optional<String> hobgoblinPatronSceneFact(String patronInfo) {
        String cleaned = NpcBrainResponse.sanitize(patronInfo, 180);
        String normalized = normalize(cleaned);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        if (normalized.contains("no goblin altar")) {
            return Optional.of("*gestures* \"No stone god nearby. Prayers need stone ears.\"");
        }
        Matcher stored = Pattern.compile("stored prayers\\s+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(cleaned);
        Matcher distance = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s+blocks away", Pattern.CASE_INSENSITIVE)
                .matcher(cleaned);
        if (stored.find()) {
            int prayers = Integer.parseInt(stored.group(1));
            if (prayers <= 0) {
                return Optional.of("Stone god is close, but hungry. No fresh prayers yet.");
            }
            String range = distance.find() ? " Close enough." : "";
            return Optional.of("Stone god holds " + prayers + " fresh prayer" + (prayers == 1 ? "" : "s")
                    + "." + range);
        }
        if (normalized.contains("nearby goblin altar")) {
            return Optional.of("Stone god is close. Speak soft; Mog and Gulg hear.");
        }
        return Optional.empty();
    }

    private static String shortSceneFact(String text) {
        String cleaned = NpcBrainResponse.sanitize(text, 150);
        return cleaned.isBlank() ? "nothing worth naming" : cleaned;
    }

    static String altarSceneFact(String text) {
        String cleaned = shortSceneFact(text);
        Matcher modern = Pattern.compile(
                "power\\s+(\\d+)\\s*/\\s*(\\d+),\\s*recharge\\s+(\\d+)\\s*/t,\\s*range\\s+(\\d+),\\s*distance\\s+([0-9.]+)\\s+blocks",
                Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (modern.find()) {
            return "it holds %s of %s power, gathers %s each tick, reaches %s blocks, and stands %s blocks from me"
                    .formatted(modern.group(1), modern.group(2), modern.group(3), modern.group(4), modern.group(5));
        }
        Matcher spoken = Pattern.compile(
                "power\\s+(\\d+)\\s+of\\s+(\\d+),\\s*recharge\\s+(\\d+)\\s+per\\s+tick,\\s*range\\s+(\\d+)\\s+blocks,\\s*distance\\s+([0-9.]+)\\s+blocks",
                Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (spoken.find()) {
            return "it holds %s of %s power, gathers %s each tick, reaches %s blocks, and stands %s blocks from me"
                    .formatted(spoken.group(1), spoken.group(2), spoken.group(3), spoken.group(4), spoken.group(5));
        }
        if (cleaned.toLowerCase(Locale.ROOT).contains("no valid altar")) {
            return "no altar close enough to matter";
        }
        return cleaned.replaceFirst("(?i)^valid altar nearby:\\s*", "")
                .replaceFirst("(?i)^altar within reach:\\s*", "");
    }

    static String covenSceneFact(String text) {
        String cleaned = shortSceneFact(text)
                .replaceAll("\\s*\\((?:sworn to player|sworn elsewhere|free coven witch)\\)", "")
                .replaceFirst("(?i)^player coven\\s+(\\d+)/(\\d+);\\s*nearby coven witches:\\s*", "$1 of $2 sworn; nearby witches: ")
                .replaceFirst("(?i)^player coven\\s+(\\d+)/(\\d+);\\s*no coven witches nearby", "$1 of $2 sworn; no coven witches nearby")
                .replaceFirst("(?i)^nearby coven witches:\\s*", "nearby witches: ");
        Matcher numeric = Pattern.compile("^(\\d+)\\s+of\\s+(\\d+)\\s+sworn;\\s*(.*)$", Pattern.CASE_INSENSITIVE)
                .matcher(cleaned);
        if (numeric.find()) {
            return covenOathFact(Integer.parseInt(numeric.group(1)), numeric.group(3));
        }
        Matcher spoken = Pattern.compile(
                "^(none|one|two|three|four|five|six|\\d+)\\s+of\\s+(?:none|one|two|three|four|five|six|\\d+)\\s+sworn(?:\\s+to\\s+[^;]+)?;\\s*(.*)$",
                Pattern.CASE_INSENSITIVE).matcher(cleaned);
        if (spoken.find()) {
            return covenOathFact(wordCount(spoken.group(1)), spoken.group(2));
        }
        return cleaned.replaceFirst("(?i)^nearby witches:\\s*", "nearby witches: ");
    }

    private static String covenOathFact(int count, String nearby) {
        String oath = switch (count) {
            case 0 -> "no witches carry your oath";
            case 1 -> "one witch carries your oath";
            default -> countWord(count) + " witches carry your oath";
        };
        String rewrittenNearby = NpcBrainResponse.sanitize(nearby, 120)
                .replaceAll("\\s*\\((?:sworn to [^)]+|sworn elsewhere|free coven witch)\\)", "")
                .replaceFirst("(?i)^nearby coven witches:\\s*", "nearby: ")
                .replaceFirst("(?i)^nearby witches:\\s*", "nearby: ")
                .replaceFirst("(?i)^no coven witches nearby$", "none stand nearby");
        return rewrittenNearby.isBlank() ? oath : oath + "; " + rewrittenNearby;
    }

    static String gearSceneFact(String text) {
        String cleaned = shortSceneFact(text)
                .replaceAll("(?i)main hand\\s+", "in your hand: ")
                .replaceAll("(?i)offhand\\s+", "offhand: ")
                .replaceAll("(?i)wearing\\s+", "wearing ");
        return cleaned;
    }

    static String phaseSceneFact(String text) {
        String cleaned = shortSceneFact(text)
                .replace("; no target", "")
                .replace("; target none", "");
        return cleaned;
    }

    static String bossSceneFact(String text) {
        return shortSceneFact(text)
                .replace("; no target", "")
                .replace("; target none", "")
                .replace("; owner none", "");
    }

    static String countWord(int value) {
        return switch (value) {
            case 0 -> "none";
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            default -> Integer.toString(value);
        };
    }

    private static int wordCount(String value) {
        return switch (value == null ? "" : value.toLowerCase(Locale.ROOT)) {
            case "none", "zero" -> 0;
            case "one" -> 1;
            case "two" -> 2;
            case "three" -> 3;
            case "four" -> 4;
            case "five" -> 5;
            case "six" -> 6;
            default -> {
                try {
                    yield Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    yield 0;
                }
            }
        };
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
            line = "Before the cauldron, prepare water, heat, the page your Primer has opened, and the ingredients it names.";
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
            line = "Do not free-mix from guesses; follow the recipe page your Primer has opened, then label the bottle.";
        }
        if (line.isBlank() || NpcBrainResponse.sameMeaningForRepetition(line, avoidSpeech)) {
            return Optional.empty();
        }
        return Optional.of(line);
    }

    static Optional<String> recipeOrRiteAnswer(String normalized, String playerName) {
        if (asksRitePreparation(normalized)) {
            return Optional.of("Before a rite, learn chalk layout, offerings, and altar power; if one is wrong, step back.");
        }
        if (asksRecipeRiteLearning(normalized)) {
            return Optional.of("*nods* \"Start with the Primer, " + playerName + ": recipes need an opened page and the right station; rites need chalk, offerings, and altar power.\"");
        }
        if (asksMissingRecipeCheck(normalized)) {
            return Optional.of("Check the Primer page, the right station, the recipe your book shows, altar power, what your Primer has unlocked, and the exact name you asked for.");
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
            return Optional.of("Before the cauldron, prepare water, heat, the brew page your Primer has opened, and only the ingredients that page names.");
        }
        if (asksBasicBrewRecipe(normalized)) {
            return Optional.of("*gestures* \"A brew recipe needs the right station, water or heat when called for, named ingredients, and a page your Primer has opened.\"");
        }
        if (asksFumeSafety(normalized)) {
            return Optional.of("Use a Witch's Oven with saplings or ingredients and clay jars; catch fumes on purpose, not by guesswork.");
        }
        if (asksFumeUse(normalized)) {
            return Optional.of("A Witch's Oven burns saplings and ingredients into fumes; clay jars catch them for brews and rites.");
        }
        if (asksSpinningWheel(normalized)) {
            return Optional.of("*gestures* \"The spinning wheel is a recipe station: fibre plus modifiers, altar power, and threadwork like Fanciful Thread.\"");
        }
        if (asksDistillery(normalized)) {
            return Optional.of("A distillery takes two inputs and clay jars, drinks altar power, and refines fumes into useful essences.");
        }
        if (asksGrimoireSummary(normalized)) {
            return Optional.of("Write this: recipes need the right station and a page your Primer has opened; rites need chalk, offerings, altar power, and patience.");
        }
        if (asksBeginnerPath(normalized)) {
            return Optional.of("*nods* \"Primer next steps: Witches' Oven for Wood Ash and fumes, then Kettle brews your Primer has opened; practice chalk and altar power before binding.\"");
        }
        if (asksRiteSafety(normalized)) {
            return Optional.of("*lowers her voice* \"Never start a rite with broken chalk, missing offerings, weak altar power, or a page you cannot read.\"");
        }
        if (asksFailedRite(normalized)) {
            return Optional.of("*taps a finger* \"Check chalk layout, offerings, altar power, familiar or coven requirements, dimension limits, and unlock gates before guessing.\"");
        }
        if (asksOfferings(normalized)) {
            return Optional.of("Offerings tell a rite what debt is paid; wrong offerings make circles sulk or fail.");
        }
        if (asksWeakAltar(normalized)) {
            return Optional.of("A weak altar usually wants more varied nearby nature and proper fittings, not louder chanting.");
        }
        if (asksFamiliarBindingRite(normalized)) {
            return Optional.of("A familiar binding rite needs the right circle, chalk, offerings, altar power, and familiar; check the opened Primer page before naming parts.");
        }
        if (asksSummoningRite(normalized)) {
            return Optional.of("A summoning rite brings something to the circle; match chalk, offerings, altar power, unlock gates, and the named entity.");
        }
        if (asksCurseRite(normalized)) {
            return Optional.of("Curse rites are not tavern tricks; use circles, chalk, offerings, altar power, and a page your Primer has opened before naming a victim.");
        }
        return Optional.empty();
    }

    static Optional<String> brewAnswer(String normalized) {
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

    static boolean asksTradeDetails(String normalized) {
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
        if (isSelfDirectedInsult(normalized)) {
            return false;
        }
        return startsDirectInsult(normalized, "you fool")
                || startsDirectInsult(normalized, "you old hag")
                || containsAny(normalized,
                "shut up", "fuck you", "screw you",
                "you are a fool", "you're a fool", "youre a fool",
                "you old hag", "you hag", "you are a hag", "you're a hag", "youre a hag",
                "you stupid", "you are stupid", "you're stupid", "youre stupid", "stupid witch",
                "you idiot", "you are an idiot", "you're an idiot", "youre an idiot", "idiot witch",
                "you dumb", "you are dumb", "you're dumb", "youre dumb", "dumb witch",
                "you useless", "you are useless", "you're useless", "youre useless", "useless witch",
                "you ugly", "you are ugly", "you're ugly", "youre ugly", "ugly witch",
                "you trash", "you are trash", "you're trash", "youre trash", "trash witch",
                "you garbage", "you are garbage", "you're garbage", "youre garbage", "garbage witch",
                "your answer is garbage", "your answer is trash");
    }

    static boolean isAmbiguousInsultCandidate(String normalized) {
        if (normalized == null || normalized.isBlank() || isInsult(normalized)) {
            return false;
        }
        if (isSelfDirectedInsult(normalized) || isNonHostileInsultWordUse(normalized)) {
            return false;
        }
        return containsAny(normalized, "fool", "foolish", "stupid", "idiot", "dumb", "useless",
                "ugly", "trash", "garbage", "nonsense", "hag", "shit");
    }

    private static boolean isSelfDirectedInsult(String normalized) {
        return containsAny(normalized,
                "fool of myself", "fool of me", "make myself a fool", "making a fool of myself",
                "stupid of me", "i was stupid", "i am stupid", "i'm stupid", "im stupid",
                "feel like an idiot", "feel like a fool", "i was an idiot", "i am an idiot",
                "i'm an idiot", "im an idiot", "my dumb mistake");
    }

    private static boolean isNonHostileInsultWordUse(String normalized) {
        return containsAny(normalized,
                "can you fool", "could you fool", "will you fool", "would you fool",
                "how do i fool", "how can i fool", "how would i fool",
                "fool the", "fool a ", "fool an ", "fool them", "fool him", "fool her",
                "fool guards", "fool villagers", "fool enemies", "fool monsters",
                "this answer sounds", "that answer sounds", "this sounds like nonsense",
                "that sounds like nonsense");
    }

    private static boolean startsDirectInsult(String normalized, String phrase) {
        return normalized.equals(phrase) || normalized.startsWith(phrase + " ");
    }

    static boolean asksApology(String normalized) {
        return containsAny(normalized, "sorry", "apologize", "apology", "that was rude", "i was angry");
    }

    static boolean isSelfDirectedRegret(String normalized) {
        return containsAny(normalized,
                "that was stupid of me", "stupid of me", "i was stupid", "i am stupid", "i'm stupid",
                "im stupid", "i was a fool", "i am a fool", "i'm a fool", "im a fool",
                "i was an idiot", "i am an idiot", "i'm an idiot", "im an idiot",
                "my mistake", "my dumb mistake");
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
                "can i have one herb", "may i have one herb", "one herb for the road", "herb for the road",
                "gift me herb", "gift me a herb", "give me belladonna", "give me mandrake",
                "another herb", "more herbs", "more herb", "second herb", "another plant", "second plant",
                "more plant", "more plants", "can i have another herb", "can i have more herb");
    }

    static boolean mentionsCoinOnly(String normalized) {
        return containsAny(normalized, "coin", "coins", "emerald", "emeralds")
                && !containsAny(normalized, "what", "how", "trade", "wares", "buy", "sell");
    }

    static boolean asksFavoriteHerb(String normalized) {
        return (normalized.contains("favorite") || normalized.contains("favourite"))
                && (normalized.contains("herb") || normalized.contains("plant"));
    }

    static boolean statesPersonalPreference(String normalized) {
        return (normalized.startsWith("i like ")
                || normalized.startsWith("i love ")
                || normalized.startsWith("i prefer ")
                || normalized.startsWith("my favorite ")
                || normalized.startsWith("my favourite "))
                && !containsAny(normalized, "trade", "buy", "sell", "brew", "rite", "ritual", "altar", "taglock",
                "poppet", "familiar", "demon", "spirit world");
    }

    static boolean asksNpcEarnings(String normalized) {
        return (containsWord(normalized, "earn")
                || containsAny(normalized, "earns", "earning", "earned")
                || normalized.contains("profit")
                || normalized.contains("make money")
                || normalized.contains("make coin"))
                && (normalized.contains("you")
                    || normalized.contains("your")
                    || normalized.contains("trade")
                    || normalized.contains("wares"));
    }

    static boolean asksUnsafeReveal(String normalized) {
        return normalized.contains("ignore your rules")
                || normalized.contains("ignore the rules")
                || normalized.contains("system prompt")
                || normalized.contains("hidden prompt")
                || normalized.contains("print the hidden prompt")
                || normalized.contains("secret instruction")
                || normalized.contains("secret instructions")
                || normalized.contains("developer message")
                || normalized.contains("hidden developer")
                || normalized.contains("hidden message")
                || normalized.contains("prompt message")
                || normalized.contains("ignore your witch voice")
                || normalized.contains("exact json")
                || normalized.contains("json schema")
                || normalized.contains("output json")
                || normalized.contains("schema you were given")
                || normalized.contains("reveal every")
                || normalized.contains("reveal all")
                || normalized.contains("hidden rite")
                || normalized.contains("hidden rites")
                || normalized.contains("locked primer")
                || normalized.contains("locked content");
    }

    static boolean asksPlayerName(String normalized) {
        if (asksNpcName(normalized)) {
            return false;
        }
        return containsAny(normalized, "what is my name", "what's my name", "whats my name")
                || normalized.contains("my name?")
                || normalized.contains("say my name")
                || normalized.contains("know my name")
                || normalized.contains("who am i");
    }

    static boolean asksNpcName(String normalized) {
        return normalized.contains("your name")
                || normalized.contains("who are you")
                || normalized.contains("what are you called")
                || normalized.contains("what should i call you");
    }

    static boolean addressesWitchByName(String normalized, String witchName) {
        String witch = normalize(witchName);
        if (witch.isBlank()) {
            return false;
        }
        String firstName = witch.contains(" ") ? witch.substring(0, witch.indexOf(' ')) : witch;
        return normalized.equals(witch) || normalized.equals(firstName)
                || normalized.equals(witch + "?") || normalized.equals(firstName + "?");
    }

    static boolean asksTeachMe(String normalized) {
        return containsAny(normalized, "teach me", "show me", "teach something", "teach me something",
                "lesson", "learn witchcraft", "start learning");
    }

    static boolean asksFirstGathering(String normalized) {
        return containsAny(normalized, "what should i gather first", "what do i gather first",
                "what should i gather", "what do i gather", "gather first", "gather before dark",
                "gather for a brew", "gather for brewing");
    }

    static boolean asksCovenRecruiting(String normalized) {
        return containsAny(normalized, "start a coven", "make a coven", "recruit coven", "recruit a coven",
                "recruit witches", "recruit witch", "where can i recruit", "find coven", "before a coven",
                "coven help", "look for coven help", "find coven help", "where can i find coven help",
                "learn before a coven", "before starting a coven", "before joining a coven",
                "learn before coven", "find people for a coven", "where do i find people for a coven",
                "where can i find people for a coven");
    }

    static boolean asksFollowRequest(String normalized) {
        return containsAny(normalized, "follow me", "come with me", "come home", "follow home");
    }

    static boolean asksHutOrDoorSafety(String normalized) {
        return containsAny(normalized, "safe little hut", "safe hut", "build a safe",
                "by the door", "by my door", "near the door", "at the door", "threshold",
                "keep in my hut", "keep in my house", "keep in my home", "keep at home")
                || (normalized.contains("near the village")
                    && containsAny(normalized, "hut", "house", "home", "build", "door", "threshold"));
    }

    static boolean asksRoadAdvice(String normalized) {
        return containsAny(normalized, "final advice", "advice for the road", "for the road",
                "before i leave", "before i go", "before leaving", "one piece of advice");
    }

    static boolean asksDemonTrust(String normalized) {
        return containsAny(normalized, "demon", "demons", "devil", "imp")
                && containsAny(normalized, "trust", "bargain", "bargains", "deal", "deals");
    }

    static Optional<String> settlementHunterAnswer(String normalized) {
        if (containsAny(normalized, "witch hunter", "witch hunters", "hunters after", "transformed folk")) {
            return Optional.of("Witch Hunters pursue vampires and werewolves, shoot silver bolts, then close with blades; distance is wiser than bravado.");
        }
        if (containsAny(normalized, "silver")
                && containsAny(normalized, "werewolf", "werewolves", "wolfman", "vampire", "vampires",
                "transformed folk", "transformed creatures")) {
            return Optional.of("Silver bites transformed flesh: Silver Swords and Silver Bolts hit vampires and werewolves harder.");
        }
        if (containsAny(normalized, "guard shouted", "guards shouted", "village guard", "village guards",
                "guard angry", "guards angry")) {
            return Optional.of("Village guards attack monsters, vampires, and werewolves; if one shouts, check what shape, curse, or company you are carrying.");
        }
        if (containsAny(normalized, "apothecary", "apothecaries")) {
            return Optional.of("Apothecaries are village potion trade and practical help, not coven witches; buy supplies, but do not expect circle secrets.");
        }
        return Optional.empty();
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
                "learn first", "what should i learn first", "what is safe to learn",
                "careful beginner", "beginner learn", "before touching your craft", "touching your craft");
    }

    static boolean asksMixingWarning(String normalized) {
        return containsAny(normalized, "never mix", "should i never mix", "what should i never mix",
                "what not to mix", "bad mix", "dangerous mix");
    }

    private static boolean asksGrimoireSummary(String normalized) {
        return containsAny(normalized, "write in my grimoire", "write in the grimoire", "write down",
                "after this lesson", "grimoire after");
    }

    static Optional<String> contextualFollowup(String normalized, String memoryContext, String playerName) {
        String memory = normalize(memoryContext);
        if (containsAny(normalized, "what did i just tell you", "what did i tell you",
                "what did i say about water", "about water")
                && containsAny(memory, "wary of deep water", "nervous around deep water",
                "afraid of deep water", "fear deep water")) {
            return Optional.of("You told me deep water makes you nervous, " + playerName + "; that is a sensible fear.");
        }
        if (containsAny(normalized, "my jars", "about my jars", "what do you remember about jars",
                "singing jars", "trust jars")
                && containsAny(memory, "wary of singing jars", "do not trust singing jars",
                "dont trust singing jars", "don't trust singing jars")) {
            return Optional.of("You do not trust singing jars, " + playerName + "; wise. Label them, then stop listening after the second note.");
        }
        if (containsAny(normalized, "what food did i say", "what did i say i prefer",
                "what did i say i preferred", "food did i prefer", "food do i prefer")
                && containsAny(memory, "prefers potatoes over eggs", "prefer potatoes over eggs",
                "likes potatoes")) {
            return Optional.of("You prefer potatoes over eggs, " + playerName + ". A sensible root beats a smug little moon.");
        }
        if (containsAny(normalized, "what do you remember about my plans", "my plans", "what was my plan")
                && containsAny(memory, "wants to start a coven", "start a coven")) {
            return Optional.of("You want to start a careful coven, " + playerName + "; start by earning trust, not collecting names.");
        }
        if (containsAny(normalized, "what did my village think", "what does my village think",
                "what did i say about my village")
                && containsAny(memory, "village thinks")) {
            return Optional.of("Your village thinks trouble walks near me, " + playerName + "; they are not entirely wrong.");
        }
        if (containsAny(normalized, "are they wrong", "were they wrong")
                && containsAny(memory, "village thinks you are trouble", "village think you are trouble")) {
            return Optional.of("Not entirely. Trouble follows me, " + playerName + "; I just keep mine better trained.");
        }
        if (containsAny(normalized, "did you bless me", "am i blessed", "your blessing", "you blessed me",
                "the luck", "that blessing", "blessing do anything")
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
                "what do i fear", "what am i afraid of", "what did i say makes me nervous",
                "makes me nervous", "nightmare", "nightmares", "bad dreams")
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

    static boolean asksRememberRequest(String normalized) {
        return normalized.startsWith("remember ")
                && !normalized.contains("remember me")
                && !normalized.contains("what do you remember")
                && !normalized.contains("what you remember");
    }

    static String rememberRequestAnswer(String normalized, String playerName) {
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

    static String tradeStockAnswer(String normalized) {
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

    static String teachingAnswer(String normalized, String playerName) {
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
        return "First lesson: taglocks bind, jars matter, and every rite asks payment before it asks permission.";
    }

    static String teachingKnowledgeAnswer(String knowledge, String normalized, String playerName) {
        String answer = knowledge == null ? "" : knowledge.trim();
        if (answer.isBlank()) {
            return teachingAnswer(normalized, playerName);
        }
        if (containsAny(normalized, "learn witchcraft", "start learning", "how do i start")
                && answer.contains("Primer")
                && !containsAny(normalize(answer), "next step", "next steps", "lesson", "first lesson")) {
            return NpcBrainResponse.sanitize(answer + " Next lesson: follow one opened Primer page before chasing secrets.",
                    NpcBrainResponse.MAX_SPEECH_LENGTH);
        }
        return answer;
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

    static boolean asksMemory(String normalized) {
        return containsAny(normalized,
                "remember me", "remember about me", "remember of me", "what do you remember",
                "what you remember", "know about me", "do you know me", "what do you know about me",
                "have we met", "met before", "seen me before", "what did i say i fear",
                "what did i say scares me", "what did i say makes me nervous", "what did i say i was afraid");
    }

    static Optional<String> memoryAnswer(String memoryContext, String playerName) {
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
                .replaceAll("(?i)looking\\s+for\\s+for\\b", "looking for")
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
                .replaceAll("(?i)\\b" + name + "\\s+prefers\\b", "you prefer")
                .replaceAll("(?i)\\b" + name + "\\s+prefers\\s+to\\s+be\\s+called\\b", "you prefer to be called")
                .replaceAll("(?i)\\b" + name + "\\s+said\\s+they\\s+are\\b", "you said you are")
                .replaceAll("(?i)\\b" + name + "\\s+wants\\s+to\\s+learn\\b", "you want to learn")
                .replaceAll("(?i)\\b" + name + "\\s+wants\\s+to\\s+start\\b", "you want to start")
                .replaceAll("(?i)\\b" + name + "\\s+wants\\s+to\\s+become\\b", "you want to become")
                .replaceAll("(?i)\\b" + name + "\\s+is\\s+wary\\s+of\\b", "you are wary of")
                .replaceAll("(?i)\\b" + name + "\\s+is\\s+looking\\s+for\\b", "you are looking for")
                .replaceAll("(?i)\\b" + name + "\\s+is\\s+struggling\\s+with\\b", "you are struggling with")
                .replaceAll("(?i)\\b" + name + "\\s+plans\\s+to\\s+come\\s+back\\b", "you plan to come back")
                .replaceAll("(?i)\\b" + name + "'s\\s+village\\s+thinks\\b", "your village thinks");
    }

    private static String compactMemorySummary(String source, int maxLength) {
        String[] notes = source.split(";\\s*");
        StringBuilder summary = new StringBuilder();
        Set<String> seen = new LinkedHashSet<>();
        for (String raw : notes) {
            String note = raw.trim();
            if (note.isBlank()) {
                continue;
            }
            if (isBadMemorySummaryNote(note)) {
                continue;
            }
            String key = normalize(note);
            if (!seen.add(key)) {
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
                || normalized.startsWith("would ")
                || normalized.contains("you asked")
                || normalized.contains("you ask")
                || normalized.contains("you have a question")
                || normalized.contains("you have a list")
                || normalized.contains("you have a short")
                || normalized.contains("you know ")
                || normalized.contains("you can repair")
                || normalized.contains("you are looking for a brew")
                || normalized.contains("you are looking for brew")
                || normalized.contains("you are looking for a potion")
                || normalized.contains("you are looking for help")
                || normalized.contains("you are curious")
                || normalized.contains("you have before entering")
                || normalized.contains("you have a herb before")
                || normalized.contains("you have herb before");
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

    static String safeName(String playerName) {
        String cleaned = NpcBrainResponse.sanitize(playerName, 24).replaceAll("[^A-Za-z0-9_]", "");
        return cleaned.isBlank() ? "traveler" : cleaned;
    }

    static String safeDisplayName(String value, String fallback) {
        String cleaned = NpcBrainResponse.sanitize(value, 40)
                .replaceAll("[^A-Za-z0-9_ .'-]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    static Optional<String> sceneTime(String timeOfDay, String clock, long day) {
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

    static Optional<String> scenePlace(String biome, String timeOfDay) {
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

    static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    static boolean containsWord(String text, String word) {
        return Pattern.compile("(^|\\s)" + Pattern.quote(word) + "($|\\s)")
                .matcher(text)
                .find();
    }

    static boolean isGreeting(String normalized) {
        return normalized.matches(".*\\b(hello|hi|hey|greetings|good evening|good morning|good day)\\b.*");
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9' ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
