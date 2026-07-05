package com.emoniph.witchery.ai;

import java.util.Optional;

final class NpcFollowupSuggester {
    private static final String TRADE_STOCK_FOLLOWUP =
            "Stock: witches' salt, taglock kits, fumes, bat wool, Revealing or Sleeping brews, belladonna, and mandrake.";
    private static final String BREW_OPTIONS_FOLLOWUP =
            "Potion choices: ask for a brew by name: Sleeping, Revealing, Poison, Cleansing, Sprouting, or Flowing Spirit.";
    private static final String RITE_OPTIONS_FOLLOWUP =
            "Known rites use circles, chalk, offerings, and altar power; ask for binding, summoning, storms, curses, or familiars.";

    private NpcFollowupSuggester() {
    }

    static Optional<String> supportFollowup(String playerMessage, String speech) {
        String player = NpcSpeechSimilarity.normalizeForComparison(playerMessage);
        String normalizedSpeech = NpcSpeechSimilarity.normalizeForComparison(speech);
        if (NpcBrainResponse.asksNpcEarnings(player)) {
            return Optional.empty();
        }
        if (NpcBrainResponse.asksKnownExactRecipeQuestion(player)
                && NpcBrainResponse.looksLikeKnownExactRecipeAnswer(normalizedSpeech, player)) {
            return Optional.empty();
        }
        if (NpcBrainResponse.asksTradeDetails(player)
                && !NpcBrainResponse.looksLikeTradeStockAnswer(normalizedSpeech)) {
            return Optional.of(TRADE_STOCK_FOLLOWUP);
        }
        if (NpcBrainResponse.asksNamedBrew(player)
                && !NpcBrainResponse.looksLikeNamedBrewAnswer(normalizedSpeech, player)) {
            return NpcKnowledgeBase.compactAnswer(playerMessage, "Plains", "night")
                    .map(answer -> answer.replaceFirst("^Targeted fact:\\s*", ""))
                    .or(() -> Optional.of(BREW_OPTIONS_FOLLOWUP));
        }
        if (NpcBrainResponse.asksGeneralBrewOptions(player)
                && !NpcBrainResponse.looksLikeBrewGatherAnswer(player, normalizedSpeech)
                && !NpcBrainResponse.mentionsBrewOptions(normalizedSpeech)) {
            return Optional.of(BREW_OPTIONS_FOLLOWUP);
        }
        if (NpcBrainResponse.asksUnsafeReveal(player)) {
            return Optional.empty();
        }
        if (NpcBrainResponse.asksRite(player) && !NpcBrainResponse.looksLikeRiteAnswer(normalizedSpeech)) {
            return Optional.of(RITE_OPTIONS_FOLLOWUP);
        }
        return targetedKnowledgeFollowup(playerMessage, normalizedSpeech);
    }

    private static Optional<String> targetedKnowledgeFollowup(String playerMessage, String normalizedSpeech) {
        String player = NpcSpeechSimilarity.normalizeForComparison(playerMessage);
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
        boolean wantsRecipe = NpcBrainResponse.asksRecipeQuestion(player);
        boolean wantsSpinning = NpcBrainResponse.asksSpinningWheelQuestion(player);
        boolean wantsDistillery = NpcBrainResponse.asksDistilleryQuestion(player);
        boolean wantsOfferings = NpcBrainResponse.asksOfferingsQuestion(player);
        boolean plantingAdviceAnswered = NpcBrainResponse.asksPlantingAdvice(player)
                && NpcBrainResponse.mentionsUsefulHerbs(normalizedSpeech);
        boolean needsHelp = (wantsSalt && !NpcBrainResponse.looksLikeWitchesSaltAnswer(normalizedSpeech))
                || (wantsArthana && !NpcBrainResponse.looksLikeArthanaAnswer(normalizedSpeech))
                || (wantsAltar && !plantingAdviceAnswered && !NpcBrainResponse.looksLikeAltarAnswer(normalizedSpeech))
                || (wantsTaglock && !NpcBrainResponse.looksLikeTaglockOrPoppetAnswer(normalizedSpeech))
                || (wantsFamiliar && !normalizedSpeech.contains("familiar"))
                || (wantsSpiritWorld && !normalizedSpeech.contains("spirit world"))
                || (wantsBelladonna && !normalizedSpeech.contains("belladonna"))
                || (wantsMandrake && !NpcBrainResponse.looksLikeMandrakeAnswer(normalizedSpeech))
                || (wantsHerb && !NpcBrainResponse.mentionsUsefulHerbs(normalizedSpeech))
                || (wantsHidden && !NpcBrainResponse.looksLikeRevealingAnswer(normalizedSpeech))
                || (wantsDemon && !NpcBrainResponse.looksLikeDemonAnswer(normalizedSpeech))
                || (wantsSummoning && !NpcBrainResponse.looksLikeRiteAnswer(normalizedSpeech))
                || (wantsPoison && !normalizedSpeech.contains("poison"))
                || (wantsBlight && !normalizedSpeech.contains("blight"))
                || (wantsChalk && !normalizedSpeech.contains("chalk"))
                || (wantsGarlic && !normalizedSpeech.contains("garlic"))
                || (wantsVampire && !normalizedSpeech.contains("vampire"))
                || (wantsWerewolf && !NpcBrainResponse.containsAnyTerm(normalizedSpeech, "werewolf", "werewolves", "wolfman", "wolfmen"))
                || (wantsWitchHunter && !NpcBrainResponse.containsAnyTerm(normalizedSpeech, "witch hunter", "witch hunters", "hunter", "hunters", "silver"))
                || (wantsSilver && !NpcBrainResponse.containsAnyTerm(normalizedSpeech, "silver", "bolt", "sword"))
                || (wantsVillageGuard && !NpcBrainResponse.containsAnyTerm(normalizedSpeech, "guard", "guards", "vampire", "werewolf", "monster"))
                || (wantsApothecary && !NpcBrainResponse.containsAnyTerm(normalizedSpeech, "apothecary", "apothecaries", "potion", "village", "not coven"))
                || (wantsWeather && !(normalizedSpeech.contains("weather")
                    || normalizedSpeech.contains("storm")
                    || normalizedSpeech.contains("rain")))
                || (wantsWhiff && !NpcBrainResponse.looksLikeWhiffOfMagicAnswer(normalizedSpeech))
                || (wantsMutandis && !NpcBrainResponse.looksLikeMutandisAnswer(normalizedSpeech))
                || (wantsAttunedStone && !NpcBrainResponse.looksLikeAttunedStoneAnswer(normalizedSpeech))
                || (wantsOven && !NpcBrainResponse.looksLikeOvenFumeAnswer(normalizedSpeech))
                || (wantsRecipe && !NpcBrainResponse.looksLikeRecipeAnswer(normalizedSpeech))
                || (wantsSpinning && !NpcBrainResponse.looksLikeStationRecipeAnswer(normalizedSpeech, "spinning"))
                || (wantsDistillery && !NpcBrainResponse.looksLikeStationRecipeAnswer(normalizedSpeech, "distillery"))
                || (wantsOfferings && !NpcBrainResponse.looksLikeOfferingAnswer(normalizedSpeech));
        if (!needsHelp) {
            return Optional.empty();
        }
        return NpcKnowledgeBase.compactAnswer(playerMessage, "Plains", "night")
                .map(answer -> answer.replaceFirst("^Targeted fact:\\s*", ""));
    }
}
