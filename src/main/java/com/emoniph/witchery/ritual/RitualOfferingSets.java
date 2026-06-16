package com.emoniph.witchery.ritual;

import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * The sacrifice-set data records for ritual dispatch — each bundles the specific offerings a rite
 * needs once they have all been found on the circle. The matching logic ({@code find*Offerings}) and
 * the dispatch that consumes these still live in {@code RitualExecutor}; this file just holds the
 * pure-data carriers (plus the {@link StormRiteVariant} selector and the {@link
 * OptionalWaystoneRelocation} relocation result) so the executor is not cluttered with ~50 DTOs.
 *
 * <p>Extracted verbatim from {@code RitualExecutor} as the third slice of the dispatcher split;
 * behaviour is unchanged.
 */
final class RitualOfferingSets {
    private RitualOfferingSets() {}
}

record ChargeStoneOfferings(RitualOffering glowstone, RitualOffering redstone,
                            RitualOffering woodAsh, RitualOffering quicklime) {}

record ForestationOfferings(ItemEntity wickerBundle, ItemEntity brewOfSprouting, ItemEntity entTwig) {}

record BanishDemonOfferings(RitualOffering blazePowder, RitualOffering waystone) {}

record CookFoodOfferings(ItemEntity woodAsh, ItemEntity coal) {}

record ChargedCookFoodOfferings(ItemEntity chargedStone, ItemEntity woodAsh, ItemEntity blazePowder) {}

record NecromanticStoneOfferings(ItemEntity attunedStone, ItemEntity bone, ItemEntity rottenFlesh,
                                 ItemEntity woodAsh, ItemEntity gunpowder, ItemEntity spectralDust) {}

record SpectralStoneOfferings(ItemEntity necroStone, ItemEntity congealedSpirit, ItemEntity condensedFear,
                              ItemEntity spectralDust, ItemEntity boline) {}

record BindSpectralOfferings(ItemEntity spectralDust, ItemEntity boline) {}

record BindFetishOfferings(ItemEntity attunedStone, ItemEntity necroStone, ItemEntity boline) {}

record BindBloodedWaystoneOfferings(ItemEntity enderDew, ItemEntity slimeBall, ItemEntity snowball,
                                    ItemEntity icyNeedle, ItemEntity chargedStone, boolean needsPower) {}

record BindWaystonePortableOfferings(ItemEntity enderDew, ItemEntity woodAsh, ItemEntity chargedStone) {}

record BindWaystoneCopyPortableOfferings(ItemEntity blankWaystone, ItemEntity enderDew,
                                         ItemEntity quicklime, ItemEntity chargedStone) {}

record CurseCreatureOfferings(ItemEntity taglockKit, ItemEntity exhaleOfHornedOne,
                              ItemEntity spiderEye, ItemEntity gunpowder,
                              ItemEntity brewGrotesque) {}

record CurseSinkingOfferings(ItemEntity taglockKit, ItemEntity exhaleOfHornedOne, ItemEntity inkSac,
                             ItemEntity netherWart, ItemEntity brewGrotesque) {}

record CurseOverheatingOfferings(ItemEntity taglockKit, ItemEntity exhaleOfHornedOne, ItemEntity demonicBlood,
                                 ItemEntity blazeRod, ItemEntity brewGrotesque) {}

record CurseInsanityOfferings(ItemEntity taglockKit, ItemEntity exhaleOfHornedOne, ItemEntity poisonousPotato,
                              ItemEntity sugar, ItemEntity brewGrotesque) {}

record CurseWakingNightmareOfferings(ItemEntity taglockKit, ItemEntity exhaleOfHornedOne, ItemEntity mellifluousHunger,
                                     ItemEntity tormentedTwine, ItemEntity emerald,
                                     ItemEntity brewGrotesque) {}

record CureInsanityOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity potato,
                             ItemEntity sugar, ItemEntity brewOfLove) {}

record CureSinkingOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity boneMeal,
                            ItemEntity netherWart, ItemEntity brewOfTheDepths) {}

record CureOverheatingOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity icyNeedle,
                                ItemEntity blazeRod, ItemEntity brewOfTheDepths) {}

record CureWakingNightmareOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity goldenCarrot,
                                    ItemEntity tormentedTwine, ItemEntity brewOfLove) {}

record CureMisfortuneOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity goldNugget,
                               ItemEntity bread, ItemEntity brewOfLove) {}

record CureCreatureOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity spiderEye,
                             ItemEntity gunpowder, ItemEntity brewOfLove) {}

record CureVampirismOfferings(ItemEntity taglockKit, ItemEntity breathOfTheGoddess, ItemEntity silverSword,
                              ItemEntity garlic, ItemEntity diamond, ItemEntity brewOfLove) {}

record CorruptPoppetOfferings(ItemEntity exhaleOfHornedOne, ItemEntity blazePowder,
                              ItemEntity spectralDust) {}

record SummonFamiliarOfferings(ItemEntity egg, ItemEntity goldIngot, ItemEntity arthana) {}

record BindFamiliarOfferings(ItemEntity tearOfTheGoddess, ItemEntity odourOfPurity,
                             ItemEntity whiffOfMagic, ItemEntity diamond, ItemEntity demonicBlood) {}

record BindCreakingFamiliarOfferings(ItemEntity resinClump, ItemEntity paleOakSapling,
                                     ItemEntity whiffOfMagic, ItemEntity tearOfTheGoddess,
                                     ItemEntity mutandisExtremis) {}

record CallFamiliarOfferings(ItemEntity breathOfTheGoddess, ItemEntity hintOfRebirth,
                             ItemEntity whiffOfMagic) {}

record SummonImpOfferings(ItemEntity refinedEvil, ItemEntity infernalBlood, ItemEntity enderPearl,
                          ItemEntity attunedStone) {}

record SummonWitchOfferings(ItemEntity diamondVapour, ItemEntity exhaleOfHornedOne, ItemEntity magmaCream,
                            ItemEntity arthana, ItemEntity fermentedSpiderEye) {}

record SummonWitherOfferings(ItemEntity diamondVapour, ItemEntity enderPearl, Villager villager) {}

record ExpensiveSummonWitherOfferings(ItemEntity diamond, ItemEntity enderPearl, ItemEntity attunedStone,
                                      ItemEntity chargedStone) {}

record SummonDemonOfferings(ItemEntity refinedEvil, ItemEntity blazePowder, ItemEntity enderPearl,
                            Villager villager) {}

record ExpensiveSummonDemonOfferings(ItemEntity refinedEvil, ItemEntity blazeRod, ItemEntity enderPearl,
                                     ItemEntity attunedStone, ItemEntity chargedStone) {}

record HellOnEarthOfferings(ItemEntity redstoneSoup, ItemEntity demonHeart, ItemEntity waystone,
                            ItemEntity netherStar, Villager villager, ItemEntity boundWaystone,
                            BlockPos effectCenter, boolean tooFar) {}

record FertilityOfferings(ItemEntity hintOfRebirth, ItemEntity diamondVapour, ItemEntity quicklime,
                          ItemEntity gypsum, ItemEntity mutandis) {}

record PortableFertilityOfferings(ItemEntity hintOfRebirth, ItemEntity diamondVapour, ItemEntity quicklime,
                                  ItemEntity gypsum, ItemEntity mutandisExtremis, ItemEntity chargedStone) {}

record BlindnessOfferings(ItemEntity chargedStone, ItemEntity redstoneSoup, ItemEntity reekOfMisfortune,
                          ItemEntity exhaleOfHornedOne, ItemEntity brewOfInk, ItemEntity poisonousPotato,
                          ItemEntity spiderEye, ItemEntity diamond) {}

record BlightOfferings(ItemEntity chargedStone, ItemEntity redstoneSoup, ItemEntity reekOfMisfortune,
                       ItemEntity fermentedSpiderEye, ItemEntity poisonousPotato, ItemEntity rottenFlesh,
                       ItemEntity diamond) {}

record RainOfToadsOfferings(ItemEntity chargedStone, ItemEntity redstoneSoup, ItemEntity reekOfMisfortune,
                            ItemEntity toeOfFrog, ItemEntity waterBucket, ItemEntity belladonna) {}

record NaturesPowerOfferings(ItemEntity rowanSapling, ItemEntity alderSapling, ItemEntity hawthornSapling,
                             ItemEntity oakSapling, ItemEntity spruceSapling, ItemEntity birchSapling,
                             ItemEntity chargedStone) {}

record TransposeOresOfferings(ItemEntity enderPearl, ItemEntity ironIngot, ItemEntity blazePowder,
                              ItemEntity diamondVapour, ItemEntity chargedStone) {}

record RaiseColumnOfferings(ItemEntity brewOfSprouting, ItemEntity cactus, ItemEntity focus) {}

record OptionalWaystoneRelocation(ItemEntity boundWaystone, BlockPos effectCenter, boolean tooFar) {}

enum StormRiteVariant {
    BASE,
    LARGE,
    PORTABLE
}

record StormOfferings(ItemEntity weatherFocus, ItemEntity woodAsh, ItemEntity chargedStone,
                      ItemEntity boundWaystone, BlockPos effectCenter, boolean tooFar) {}

record MysticBranchOfferings(ItemEntity entTwig, ItemEntity mysticUnguent) {}

record CrystalBallOfferings(ItemEntity quartzSphere, ItemEntity goldIngot, ItemEntity happenstanceOil) {}

record SeerStoneOfferings(ItemEntity quartzSphere, ItemEntity obsidian, ItemEntity happenstanceOil) {}

record PriorIncarnationOfferings(ItemEntity necroStone, ItemEntity dogTongue,
                                 ItemEntity bone, ItemEntity spectralDust,
                                 ItemEntity chargedStone) {}

record ManifestationOfferings(ItemEntity spectralDust, ItemEntity mellifluousHunger, ItemEntity necroStone,
                              ItemEntity goldenPickaxe, ItemEntity arthana,
                              ItemEntity gunpowder) {}

record TotalEclipseOfferings(ItemEntity quicklime, ItemEntity chargedStone) {}

record EntityTranspositionOfferings(ItemEntity waystone, ItemEntity enderDew, ItemEntity ironAxe) {}

record InfuseBrewSoaringOfferings(ItemEntity infusedBrewBase, ItemEntity broom, ItemEntity feather,
                                  ItemEntity arthana, OwlFamiliarEntity owl) {}

record InfuseBrewGraveOfferings(ItemEntity infusedBrewBase, ItemEntity bone, ItemEntity denseWeb,
                                ItemEntity necroStone, Zombie zombie) {}
