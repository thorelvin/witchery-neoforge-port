package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.entity.*;
import com.emoniph.witchery.entity.ElleEntity;
import com.emoniph.witchery.entity.LilithEntity;
import com.emoniph.witchery.entity.EgassyEntity;
import com.emoniph.witchery.entity.DeathEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.SoulfireProjectile;
import com.emoniph.witchery.entity.NightmareEntity;
import com.emoniph.witchery.entity.SpiritEntity;
import com.emoniph.witchery.entity.BroomEntity;
import com.emoniph.witchery.entity.familiar.CatFamiliarEntity;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.emoniph.witchery.entity.familiar.SpectralFamiliarEntity;
import com.emoniph.witchery.entity.familiar.ToadFamiliarEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public class WitcheryEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, WitcheryMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<OwlFamiliarEntity>> OWL =
            ENTITY_TYPES.register("owl", () ->
                    EntityType.Builder.of(OwlFamiliarEntity::new, MobCategory.CREATURE)
                            .sized(0.5F, 0.8F).clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "owl"))));

    public static final DeferredHolder<EntityType<?>, EntityType<RavenEntity>> RAVEN =
            ENTITY_TYPES.register("raven", () ->
                    EntityType.Builder.of(RavenEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.6F).clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "raven"))));

    public static final DeferredHolder<EntityType<?>, EntityType<AttackBatEntity>> ATTACK_BAT =
            ENTITY_TYPES.register("attack_bat", () ->
                    EntityType.Builder.of(AttackBatEntity::new, MobCategory.MONSTER)
                            .sized(0.5F, 0.9F).clientTrackingRange(8).updateInterval(2)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "attack_bat"))));

    public static final DeferredHolder<EntityType<?>, EntityType<CatFamiliarEntity>> FAMILIAR_CAT =
            ENTITY_TYPES.register("familiar_cat", () ->
                    EntityType.Builder.of(CatFamiliarEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.7F).clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "familiar_cat"))));

    public static final DeferredHolder<EntityType<?>, EntityType<SpectralFamiliarEntity>> SPECTRAL_FAMILIAR =
            ENTITY_TYPES.register("spectral_familiar", () ->
                    EntityType.Builder.of(SpectralFamiliarEntity::new, MobCategory.CREATURE)
                            .sized(0.8F, 0.8F).clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "spectral_familiar"))));

    public static final DeferredHolder<EntityType<?>, EntityType<ToadFamiliarEntity>> TOAD =
            ENTITY_TYPES.register("toad", () ->
                    EntityType.Builder.of(ToadFamiliarEntity::new, MobCategory.CREATURE)
                            .sized(0.7F, 0.5F).clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "toad"))));

    public static final DeferredHolder<EntityType<?>, EntityType<ParasyticLouseEntity>> PARASYTIC_LOUSE =
            ENTITY_TYPES.register("parasytic_louse", () ->
                    EntityType.Builder.of(ParasyticLouseEntity::new, MobCategory.MONSTER)
                            .sized(0.3F, 0.7F).clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "parasytic_louse"))));

    public static final DeferredHolder<EntityType<?>, EntityType<CovenWitchEntity>> COVEN_WITCH =
            ENTITY_TYPES.register("coven_witch", () ->
                    EntityType.Builder.of(CovenWitchEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "coven_witch"))));

    public static final DeferredHolder<EntityType<?>, EntityType<SpectralWitchEntity>> SPECTRAL_WITCH =
            ENTITY_TYPES.register("spectral_witch", () ->
                    EntityType.Builder.of(SpectralWitchEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(2).fireImmune().noSave().noSummon().build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "spectral_witch"))));

    public static final DeferredHolder<EntityType<?>, EntityType<ScryingEyeEntity>> SCRYING_EYE =
            ENTITY_TYPES.register("scrying_eye", () ->
                    EntityType.Builder.<ScryingEyeEntity>of(ScryingEyeEntity::new, MobCategory.MISC)
                            .sized(0.01F, 0.01F).clientTrackingRange(10).updateInterval(2).noSave().noSummon()
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "scrying_eye"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.EarthRockEntity>> EARTH_ROCK =
            ENTITY_TYPES.register("earth_rock", () ->
                    EntityType.Builder.<com.emoniph.witchery.entity.EarthRockEntity>of(com.emoniph.witchery.entity.EarthRockEntity::new, MobCategory.MISC)
                            .sized(0.4F, 0.4F).clientTrackingRange(4).updateInterval(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "earth_rock"))));

    public static final DeferredHolder<EntityType<?>, EntityType<CrazyCatLadyEntity>> CRAZY_CAT_LADY =
            ENTITY_TYPES.register("crazy_cat_lady", () ->
                    EntityType.Builder.of(CrazyCatLadyEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "crazy_cat_lady"))));

    public static final DeferredHolder<EntityType<?>, EntityType<WitchHunterEntity>> WITCH_HUNTER =
            ENTITY_TYPES.register("witch_hunter", () ->
                    EntityType.Builder.of(WitchHunterEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "witch_hunter"))));

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownCatEntity>> THROWN_CAT =
            ENTITY_TYPES.register("thrown_cat", () ->
                    EntityType.Builder.<ThrownCatEntity>of(ThrownCatEntity::new, MobCategory.MISC)
                            .sized(0.6F, 0.7F).clientTrackingRange(8).updateInterval(2).noSave().noSummon()
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "thrown_cat"))));

    // ── Broom ─────────────────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<BroomEntity>> BROOM =
            ENTITY_TYPES.register("broom", () ->
                    EntityType.Builder.<BroomEntity>of(BroomEntity::new, MobCategory.MISC)
                            .sized(1.2F, 0.5F).clientTrackingRange(10).noSummon().build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "broom"))));

    // ── M27 — Horror + Summoned Entities ─────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<BansheeEntity>> BANSHEE =
            ENTITY_TYPES.register("banshee", () ->
                    EntityType.Builder.of(BansheeEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "banshee"))));

    public static final DeferredHolder<EntityType<?>, EntityType<SpectreEntity>> SPECTRE =
            ENTITY_TYPES.register("spectre", () ->
                    EntityType.Builder.of(SpectreEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "spectre"))));

    public static final DeferredHolder<EntityType<?>, EntityType<PoltergeistEntity>> POLTERGEIST =
            ENTITY_TYPES.register("poltergeist", () ->
                    EntityType.Builder.of(PoltergeistEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.5F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "poltergeist"))));

    public static final DeferredHolder<EntityType<?>, EntityType<TreefydEntity>> TREEFYD =
            ENTITY_TYPES.register("treefyd", () ->
                    EntityType.Builder.of(TreefydEntity::new, MobCategory.MONSTER)
                            .sized(0.4F, 1.8F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "treefyd"))));

    public static final DeferredHolder<EntityType<?>, EntityType<EntEntity>> ENT =
            ENTITY_TYPES.register("ent", () ->
                    EntityType.Builder.of(EntEntity::new, MobCategory.CREATURE)
                            .sized(1.2F, 3.0F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "ent"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.HellhoundEntity>> HELLHOUND =
            ENTITY_TYPES.register("hellhound", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.HellhoundEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 0.9F).fireImmune().clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "hellhound"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.MindrakeEntity>> MINDRAKE =
            ENTITY_TYPES.register("mindrake", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.MindrakeEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.8F).clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "mindrake"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.DarkMarkEntity>> DARK_MARK =
            ENTITY_TYPES.register("dark_mark", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.DarkMarkEntity::new, MobCategory.MISC)
                            .sized(2.0F, 2.0F).fireImmune().noSummon().clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "dark_mark"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.IllusionEntity.Creeper>> ILLUSION_CREEPER =
            ENTITY_TYPES.register("illusion_creeper", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.IllusionEntity.Creeper::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.7F).clientTrackingRange(10).updateInterval(2).noSave().noSummon()
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "illusion_creeper"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.IllusionEntity.Spider>> ILLUSION_SPIDER =
            ENTITY_TYPES.register("illusion_spider", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.IllusionEntity.Spider::new, MobCategory.MONSTER)
                            .sized(1.4F, 0.9F).clientTrackingRange(10).updateInterval(2).noSave().noSummon()
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "illusion_spider"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.IllusionEntity.Zombie>> ILLUSION_ZOMBIE =
            ENTITY_TYPES.register("illusion_zombie", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.IllusionEntity.Zombie::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).clientTrackingRange(10).updateInterval(2).noSave().noSummon()
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "illusion_zombie"))));

    public static final DeferredHolder<EntityType<?>, EntityType<MandrakeEntity>> MANDRAKE_MOB =
            ENTITY_TYPES.register("mandrake_mob", () ->
                    EntityType.Builder.of(MandrakeEntity::new, MobCategory.MONSTER)
                            .sized(MandrakeEntity.SOURCE_WIDTH, MandrakeEntity.SOURCE_HEIGHT)
                            .clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "mandrake_mob"))));

    public static final DeferredHolder<EntityType<?>, EntityType<DemonEntity>> DEMON =
            ENTITY_TYPES.register("demon", () ->
                    EntityType.Builder.of(DemonEntity::new, MobCategory.MONSTER)
                            .sized(1.0F, 2.9F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "demon"))));

    // ── Binky — Death's horse ─────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.BinkyEntity>> BINKY =
            ENTITY_TYPES.register("binky", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.BinkyEntity::new, MobCategory.CREATURE)
                            .sized(1.4F, 1.6F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "binky"))));

    // ── Death boss ────────────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<DeathEntity>> DEATH =
            ENTITY_TYPES.register("death", () ->
                    EntityType.Builder.of(DeathEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).clientTrackingRange(16).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "death"))));

    // ── Flame Imp ─────────────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.FlameImpEntity>> FLAME_IMP =
            ENTITY_TYPES.register("flame_imp", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.FlameImpEntity::new, MobCategory.MONSTER)
                            .sized(0.4F, 1.3F).fireImmune().clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "flame_imp"))));

    // ── Lord of Torment boss ──────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<LordOfTormentEntity>> LORD_OF_TORMENT =
            ENTITY_TYPES.register("lord_of_torment", () ->
                    EntityType.Builder.of(LordOfTormentEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.9F).clientTrackingRange(16).fireImmune().build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "lord_of_torment"))));

    public static final DeferredHolder<EntityType<?>, EntityType<HornedHuntsmanEntity>> HORNED_HUNTSMAN =
            ENTITY_TYPES.register("horned_huntsman", () ->
                    EntityType.Builder.of(HornedHuntsmanEntity::new, MobCategory.MONSTER)
                            .sized(1.4F, 3.2F).clientTrackingRange(16).fireImmune().build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "horned_huntsman"))));

    public static final DeferredHolder<EntityType<?>, EntityType<SoulfireProjectile>> SOULFIRE =
            ENTITY_TYPES.register("soulfire", () ->
                    EntityType.Builder.<SoulfireProjectile>of(SoulfireProjectile::new, MobCategory.MISC)
                            .sized(0.3125F, 0.3125F).clientTrackingRange(4).updateInterval(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "soulfire"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.ThrownBrewEntity>> THROWN_BREW =
            ENTITY_TYPES.register("thrown_brew", () ->
                    EntityType.Builder.<com.emoniph.witchery.entity.ThrownBrewEntity>of(com.emoniph.witchery.entity.ThrownBrewEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "thrown_brew"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.SunGrenadeEntity>> SUN_GRENADE =
            ENTITY_TYPES.register("sun_grenade", () ->
                    EntityType.Builder.<com.emoniph.witchery.entity.SunGrenadeEntity>of(com.emoniph.witchery.entity.SunGrenadeEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "sun_grenade"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.SpellEffectEntity>> SPELL_EFFECT =
            ENTITY_TYPES.register("spell_effect", () ->
                    EntityType.Builder.<com.emoniph.witchery.entity.SpellEffectEntity>of(com.emoniph.witchery.entity.SpellEffectEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "spell_effect"))));

    // ── Flying Monkey (M10) ───────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.FlyingMonkeyEntity>> FLYING_MONKEY =
            ENTITY_TYPES.register("flying_monkey", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.FlyingMonkeyEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 0.8F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "flying_monkey"))));

    // ── Egassy ───────────────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<EgassyEntity>> EGASSY =
            ENTITY_TYPES.register("egassy", () ->
                    EntityType.Builder.of(EgassyEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "egassy"))));

    // ── Hobgoblin (M9) ────────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.HobgoblinEntity>> HOBGOBLIN =
            ENTITY_TYPES.register("hobgoblin", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.HobgoblinEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "hobgoblin"))));

    // ── M25b — Spirit World entities ──────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<NightmareEntity>> NIGHTMARE =
            ENTITY_TYPES.register("nightmare", () ->
                    EntityType.Builder.of(NightmareEntity::new, MobCategory.MONSTER)
                            .fireImmune().sized(0.6F, 1.8F).clientTrackingRange(10).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "nightmare"))));

    public static final DeferredHolder<EntityType<?>, EntityType<SpiritEntity>> SPIRIT =
            ENTITY_TYPES.register("spirit", () ->
                    EntityType.Builder.of(SpiritEntity::new, MobCategory.AMBIENT)
                            .sized(0.5F, 0.5F).clientTrackingRange(8).build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "spirit"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.CorpseEntity>> CORPSE =
            ENTITY_TYPES.register("corpse", () ->
                    EntityType.Builder.<com.emoniph.witchery.entity.CorpseEntity>of(com.emoniph.witchery.entity.CorpseEntity::new, MobCategory.MISC)
                            .sized(0.6F, 0.2F).clientTrackingRange(10).noSummon()
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "corpse"))));

    // ── Village Guard ─────────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.VillageGuardEntity>> VILLAGE_GUARD =
            ENTITY_TYPES.register("village_guard", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.VillageGuardEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F).clientTrackingRange(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "village_guard"))));

    // ── Bolt — witch-hunter crossbow projectile ───────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.BoltEntity>> BOLT =
            ENTITY_TYPES.register("bolt", () ->
                    EntityType.Builder.<com.emoniph.witchery.entity.BoltEntity>of(com.emoniph.witchery.entity.BoltEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20).noSummon()
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "bolt"))));

    // ── Goblin bosses — Gulg (melee) and Mog (ranged) ────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.GoblinGulgEntity>> GOBLIN_GULG =
            ENTITY_TYPES.register("goblin_gulg", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.GoblinGulgEntity::new, MobCategory.MONSTER)
                            .sized(0.8F, 1.8F).clientTrackingRange(16)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "goblin_gulg"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.GoblinMogEntity>> GOBLIN_MOG =
            ENTITY_TYPES.register("goblin_mog", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.GoblinMogEntity::new, MobCategory.MONSTER)
                            .sized(0.8F, 1.8F).clientTrackingRange(16)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "goblin_mog"))));

    // ── Vampire mob ───────────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.VampireEntity>> VAMPIRE =
            ENTITY_TYPES.register("vampire", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.VampireEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).clientTrackingRange(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "vampire"))));

    // ── Baba Yaga — boss witch ────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.BabaYagaEntity>> BABA_YAGA =
            ENTITY_TYPES.register("baba_yaga", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.BabaYagaEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).clientTrackingRange(16)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "baba_yaga"))));

    // ── Wolfman + Were-Villager — werewolf mob chain ──────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.WolfmanEntity>> WOLFMAN =
            ENTITY_TYPES.register("wolfman", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.WolfmanEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.8F).clientTrackingRange(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "wolfman"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.WereVillagerEntity>> WERE_VILLAGER =
            ENTITY_TYPES.register("were_villager", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.WereVillagerEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F).clientTrackingRange(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "were_villager"))));

    // ── Shade of Leonard + Lost Souls — Leonard boss chain ───────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.LeonardEntity>> LEONARD =
            ENTITY_TYPES.register("leonard", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.LeonardEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).fireImmune().clientTrackingRange(16)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "leonard"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.LostSoulEntity>> LOST_SOUL =
            ENTITY_TYPES.register("lost_soul", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.LostSoulEntity::new, MobCategory.MONSTER)
                            .sized(0.5F, 0.5F).clientTrackingRange(8)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "lost_soul"))));

    // ── Elle + Lilith — vampire patron chain ─────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<ElleEntity>> ELLE =
            ENTITY_TYPES.register("elle", () ->
                    EntityType.Builder.of(ElleEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F).clientTrackingRange(10)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "elle"))));

    public static final DeferredHolder<EntityType<?>, EntityType<LilithEntity>> LILITH =
            ENTITY_TYPES.register("lilith", () ->
                    EntityType.Builder.of(LilithEntity::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.5F).clientTrackingRange(16).fireImmune()
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "lilith"))));

    // ── Mirror World ──────────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.ReflectionEntity>> REFLECTION =
            ENTITY_TYPES.register("reflection", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.ReflectionEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F).clientTrackingRange(12)
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "reflection"))));

    public static final DeferredHolder<EntityType<?>, EntityType<com.emoniph.witchery.entity.MirrorFaceEntity>> MIRROR_FACE =
            ENTITY_TYPES.register("mirror_face", () ->
                    EntityType.Builder.of(com.emoniph.witchery.entity.MirrorFaceEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(2).fireImmune()
                            .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("witchery", "mirror_face"))));

    @SubscribeEvent
    public static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(BANSHEE.get(),      SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(SPECTRE.get(),      SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(POLTERGEIST.get(),  SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(MANDRAKE_MOB.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(DEMON.get(),        SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(NIGHTMARE.get(),    SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(GOBLIN_GULG.get(),  SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(GOBLIN_MOG.get(),   SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(VAMPIRE.get(),      SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(BABA_YAGA.get(),    SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(WOLFMAN.get(),      SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(WERE_VILLAGER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON),
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(VILLAGE_GUARD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON),
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(LEONARD.get(),      SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(LOST_SOUL.get(),    SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(HOBGOBLIN.get(),     SpawnPlacementTypes.ON_GROUND,        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, rand) -> level.getBlockState(pos.below())
                        .is(net.minecraft.tags.BlockTags.ANIMALS_SPAWNABLE_ON),
                RegisterSpawnPlacementsEvent.Operation.OR);

        // Spirit World — spirit orb floats freely
        event.register(SPIRIT.get(),        SpawnPlacementTypes.NO_RESTRICTIONS,  Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.Mob::checkMobSpawnRules,    RegisterSpawnPlacementsEvent.Operation.OR);

        // Ground mobs — humanoids and ground-walkers
        event.register(COVEN_WITCH.get(),   SpawnPlacementTypes.ON_GROUND,        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(CRAZY_CAT_LADY.get(), SpawnPlacementTypes.ON_GROUND,        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(WITCH_HUNTER.get(), SpawnPlacementTypes.ON_GROUND,         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(TREEFYD.get(),       SpawnPlacementTypes.ON_GROUND,        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(ENT.get(),           SpawnPlacementTypes.ON_GROUND,        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(HELLHOUND.get(),     SpawnPlacementTypes.ON_GROUND,        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);

        // Familiars — creature-category animals
        event.register(OWL.get(),           SpawnPlacementTypes.ON_GROUND,        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,   RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(RAVEN.get(),         SpawnPlacementTypes.ON_GROUND,        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,   RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(FAMILIAR_CAT.get(), SpawnPlacementTypes.ON_GROUND,         Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,   RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(SPECTRAL_FAMILIAR.get(), SpawnPlacementTypes.ON_GROUND,    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,   RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(TOAD.get(),          SpawnPlacementTypes.ON_GROUND,        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,   RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(PARASYTIC_LOUSE.get(), SpawnPlacementTypes.ON_GROUND,      Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(OWL.get(),           OwlFamiliarEntity.createAttributes().build());
        event.put(RAVEN.get(),         RavenEntity.createAttributes().build());
        event.put(ATTACK_BAT.get(),    net.minecraft.world.entity.ambient.Bat.createAttributes().build());
        event.put(FAMILIAR_CAT.get(),  CatFamiliarEntity.createAttributes().build());
        event.put(SPECTRAL_FAMILIAR.get(), SpectralFamiliarEntity.createAttributes().build());
        event.put(TOAD.get(),          ToadFamiliarEntity.createAttributes().build());
        event.put(PARASYTIC_LOUSE.get(), ParasyticLouseEntity.createAttributes().build());
        event.put(COVEN_WITCH.get(),   CovenWitchEntity.createAttributes().build());
        event.put(SPECTRAL_WITCH.get(), SpectralWitchEntity.createAttributes().build());
        event.put(SCRYING_EYE.get(),    ScryingEyeEntity.createAttributes().build());
        event.put(CRAZY_CAT_LADY.get(), CrazyCatLadyEntity.createAttributes().build());
        event.put(THROWN_CAT.get(),     ThrownCatEntity.createAttributes().build());
        event.put(WITCH_HUNTER.get(),  WitchHunterEntity.createAttributes().build());
        event.put(BANSHEE.get(),       BansheeEntity.createAttributes().build());
        event.put(SPECTRE.get(),       SpectreEntity.createAttributes().build());
        event.put(POLTERGEIST.get(),   PoltergeistEntity.createAttributes().build());
        event.put(TREEFYD.get(),       TreefydEntity.createAttributes().build());
        event.put(ENT.get(),           EntEntity.createAttributes().build());
        event.put(HELLHOUND.get(),     com.emoniph.witchery.entity.HellhoundEntity.createAttributes().build());
        event.put(MINDRAKE.get(),      com.emoniph.witchery.entity.MindrakeEntity.createAttributes().build());
        event.put(DARK_MARK.get(),     com.emoniph.witchery.entity.DarkMarkEntity.createAttributes().build());
        event.put(ILLUSION_CREEPER.get(), com.emoniph.witchery.entity.IllusionEntity.createAttributes().build());
        event.put(ILLUSION_SPIDER.get(),  com.emoniph.witchery.entity.IllusionEntity.createAttributes().build());
        event.put(ILLUSION_ZOMBIE.get(),  com.emoniph.witchery.entity.IllusionEntity.createAttributes().build());
        event.put(MANDRAKE_MOB.get(),  MandrakeEntity.createAttributes().build());
        event.put(DEMON.get(),         DemonEntity.createAttributes().build());
        event.put(NIGHTMARE.get(),        NightmareEntity.createAttributes().build());
        event.put(SPIRIT.get(),           SpiritEntity.createAttributes().build());
        event.put(BROOM.get(),            BroomEntity.createAttributes().build());
        event.put(LORD_OF_TORMENT.get(),  LordOfTormentEntity.createAttributes().build());
        event.put(HORNED_HUNTSMAN.get(),   HornedHuntsmanEntity.createAttributes().build());
        event.put(FLAME_IMP.get(),        com.emoniph.witchery.entity.FlameImpEntity.createAttributes().build());
        event.put(BINKY.get(),            com.emoniph.witchery.entity.BinkyEntity.createAttributes().build());
        event.put(DEATH.get(),            DeathEntity.createAttributes().build());
        event.put(EGASSY.get(),           EgassyEntity.createAttributes().build());
        event.put(HOBGOBLIN.get(),        com.emoniph.witchery.entity.HobgoblinEntity.createAttributes().build());
        event.put(FLYING_MONKEY.get(),    com.emoniph.witchery.entity.FlyingMonkeyEntity.createAttributes().build());
        event.put(CORPSE.get(),           com.emoniph.witchery.entity.CorpseEntity.createAttributes().build());
        event.put(REFLECTION.get(),       com.emoniph.witchery.entity.ReflectionEntity.createAttributes().build());
        event.put(MIRROR_FACE.get(),      com.emoniph.witchery.entity.MirrorFaceEntity.createAttributes().build());
        event.put(ELLE.get(),             ElleEntity.createAttributes().build());
        event.put(LILITH.get(),           LilithEntity.createAttributes().build());
        event.put(VILLAGE_GUARD.get(),     com.emoniph.witchery.entity.VillageGuardEntity.createAttributes().build());
        event.put(GOBLIN_GULG.get(),       com.emoniph.witchery.entity.GoblinGulgEntity.createAttributes().build());
        event.put(GOBLIN_MOG.get(),        com.emoniph.witchery.entity.GoblinMogEntity.createAttributes().build());
        event.put(VAMPIRE.get(),           com.emoniph.witchery.entity.VampireEntity.createAttributes().build());
        event.put(BABA_YAGA.get(),         com.emoniph.witchery.entity.BabaYagaEntity.createAttributes().build());
        event.put(WOLFMAN.get(),           com.emoniph.witchery.entity.WolfmanEntity.createAttributes().build());
        event.put(WERE_VILLAGER.get(),     com.emoniph.witchery.entity.WereVillagerEntity.createAttributes().build());
        event.put(LEONARD.get(),          com.emoniph.witchery.entity.LeonardEntity.createAttributes().build());
        event.put(LOST_SOUL.get(),        com.emoniph.witchery.entity.LostSoulEntity.createAttributes().build());
    }
}
