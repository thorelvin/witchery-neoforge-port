package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BrazierBlockEntity extends BlockEntity implements WorldlyContainer {

    // Slots: 0-2 = ingredients, 3 = burning marker
    public static final int SLOT_INGREDIENT_1 = 0;
    public static final int SLOT_INGREDIENT_2 = 1;
    public static final int SLOT_INGREDIENT_3 = 2;
    public static final int SLOT_BURNING_MARKER = 3;
    public static final int SLOT_COUNT = 4;

    private static final int[] AUTOMATION_SLOTS = {SLOT_INGREDIENT_1, SLOT_INGREDIENT_2, SLOT_INGREDIENT_3};

    private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    private static final int   ALTAR_RADIUS    = 16;
    private static final float POWER_PER_TICK  = 1.0f;
    private static final int   SOURCE_AURA_INTERVAL = 60;
    private static final int   SOURCE_SUMMON_INTERVAL = 100;

    private int cookTime   = 0;
    private int powerLevel = 0;
    private long powerStorage = 0L;
    private boolean previousRedstoneState = false;

    // ── Recipe definition ─────────────────────────────────────────────────────

    private enum BrazierMode {
        SUMMON,
        AURA,
        WILTING,
        SMOKE,
        COMPLETION_BUFF
    }

    /**
     * @param ingredients      1-3 required items (order-insensitive)
     * @param burnTicks        how long to burn before completion, before source storage extension
     * @param needsAltar       whether an altar must be nearby
     * @param effects          mob effects to apply for aura/completion recipes
     * @param summonEntity     entity to spawn on completion (null for none)
     * @param mode             source behavior family
     * @param effectRadius     old circular radius for aura/buff recipes
     * @param pulseInterval    burning callback cadence in ticks
     * @param exactIngredients whether extra filled ingredient slots should block the match
     */
    public record BrazierRecipe(
        Item[] ingredients,
        int burnTicks,
        boolean needsAltar,
        List<MobEffectInstance> effects,
        @Nullable EntityType<?> summonEntity,
        BrazierMode mode,
        int effectRadius,
        int pulseInterval,
        boolean exactIngredients
    ) {
        boolean matches(ItemStack s0, ItemStack s1, ItemStack s2) {
            var have = new ArrayList<Item>();
            if (!s0.isEmpty()) have.add(s0.getItem());
            if (!s1.isEmpty()) have.add(s1.getItem());
            if (!s2.isEmpty()) have.add(s2.getItem());
            if (exactIngredients && have.size() != ingredients.length) {
                return false;
            }
            for (Item need : ingredients) {
                if (!have.remove(need)) return false;
            }
            return true;
        }
    }

    // ── Recipe table ──────────────────────────────────────────────────────────

    public static final List<BrazierRecipe> RECIPES = List.of(

        // ── Summoning (on-completion entity spawn) ────────────────────────────

        // Spectre: Wormwood + Bat Wool + Graveyard Dust → spawn Spectre (5% bonus Poltergeist)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.WORMWOOD.get(), WitcheryItems.BAT_WOOL.get(), WitcheryItems.GRAVEYARD_DUST.get() },
            600, true, List.of(), WitcheryEntities.SPECTRE.get(),
            BrazierMode.SUMMON, 0, SOURCE_SUMMON_INTERVAL, true
        ),
        // Banshee: Wormwood + Condensed Fear + Graveyard Dust → spawn Banshee
        new BrazierRecipe(
            new Item[]{ WitcheryItems.WORMWOOD.get(), WitcheryItems.CONDENSED_FEAR.get(), WitcheryItems.GRAVEYARD_DUST.get() },
            600, true, List.of(), WitcheryEntities.BANSHEE.get(),
            BrazierMode.SUMMON, 0, SOURCE_SUMMON_INTERVAL, true
        ),
        // Poltergeist: Wormwood + Refined Evil + Focused Will → spawn Poltergeist (no altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.WORMWOOD.get(), WitcheryItems.REFINED_EVIL.get(), WitcheryItems.FOCUSED_WILL.get() },
            900, false, List.of(), WitcheryEntities.POLTERGEIST.get(),
            BrazierMode.SUMMON, 0, SOURCE_SUMMON_INTERVAL, true
        ),

        // ── Pulsing aura (effects applied every 60 ticks while burning) ───────

        // Anguish of the Dead: Tear of Goddess + Blaze Rod + Golden Apple → Strength II aura
        new BrazierRecipe(
            new Item[]{ WitcheryItems.TEAR_OF_GODDESS.get(), Items.BLAZE_ROD, Items.GOLDEN_APPLE },
            6000, true,
            List.of(new MobEffectInstance(MobEffects.STRENGTH, 120, 0)),
            null, BrazierMode.AURA, 4, SOURCE_AURA_INTERVAL, true
        ),
        // Fortification of the Corpse: Tear of Goddess + Diamond + Golden Apple → Resistance II aura
        new BrazierRecipe(
            new Item[]{ WitcheryItems.TEAR_OF_GODDESS.get(), Items.DIAMOND, Items.GOLDEN_APPLE },
            6000, true,
            List.of(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0)),
            null, BrazierMode.AURA, 4, SOURCE_AURA_INTERVAL, true
        ),
        // Deathly Veil: Fermented Spider Eye + Ender Pearl + Diamond → Invisibility aura
        new BrazierRecipe(
            new Item[]{ Items.FERMENTED_SPIDER_EYE, Items.ENDER_PEARL, Items.DIAMOND },
            12000, true,
            List.of(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0)),
            null, BrazierMode.AURA, 6, SOURCE_AURA_INTERVAL, true
        ),
        // Drain Growth: Condensed Fear + Wormy Apple + Graveyard Dust → Weakness aura (no altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.CONDENSED_FEAR.get(), WitcheryItems.WORMY_APPLE.get(), WitcheryItems.GRAVEYARD_DUST.get() },
            1200, false, List.of(), null, BrazierMode.WILTING, 3, 5, true
        ),
        // Graveyard Mist: Nether Wart + Quicklime + Blaze Powder → smoke cloud (visual only)
        new BrazierRecipe(
            new Item[]{ Items.NETHER_WART, WitcheryItems.QUICKLIME.get(), Items.BLAZE_POWDER },
            6000, true, List.of(), null, BrazierMode.SMOKE, 16, 1, true
        ),

        // ── One-shot completion effects ────────────────────────────────────────

        // Infernal Animus → Fire Resistance + Strength 5 min (altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.INFERNAL_ANIMUS.get() },
            200, true,
            List.of(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0),
                    new MobEffectInstance(MobEffects.STRENGTH,     6000, 0)),
            null, BrazierMode.COMPLETION_BUFF, 8, 0, true
        ),
        // Exhale of Horned One → Damage Resistance 3 min (altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.EXHALE_OF_HORNED_ONE.get() },
            300, true,
            List.of(new MobEffectInstance(MobEffects.RESISTANCE, 3600, 0)),
            null, BrazierMode.COMPLETION_BUFF, 8, 0, true
        ),
        // Rowan Berries → Regeneration 1 min
        new BrazierRecipe(
            new Item[]{ WitcheryItems.ROWAN_BERRIES.get() },
            160, false,
            List.of(new MobEffectInstance(MobEffects.REGENERATION, 1200, 0)),
            null, BrazierMode.COMPLETION_BUFF, 8, 0, true
        ),
        // Fanciful Thread → Luck 2 min
        new BrazierRecipe(
            new Item[]{ WitcheryItems.FANCIFUL_THREAD.get() },
            200, false,
            List.of(new MobEffectInstance(MobEffects.LUCK, 2400, 0)),
            null, BrazierMode.COMPLETION_BUFF, 8, 0, true
        ),
        // Diamond Vapour + Fanciful Thread → Night Vision + Speed 2 min (altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.DIAMOND_VAPOUR.get(), WitcheryItems.FANCIFUL_THREAD.get() },
            400, true,
            List.of(new MobEffectInstance(MobEffects.NIGHT_VISION,   2400, 0),
                    new MobEffectInstance(MobEffects.SPEED, 2400, 0)),
            null, BrazierMode.COMPLETION_BUFF, 8, 0, true
        ),
        // Gunpowder → Strength + Speed burst 30 sec
        new BrazierRecipe(
            new Item[]{ Items.GUNPOWDER },
            60, false,
            List.of(new MobEffectInstance(MobEffects.STRENGTH,   600, 1),
                    new MobEffectInstance(MobEffects.SPEED, 600, 1)),
            null, BrazierMode.COMPLETION_BUFF, 8, 0, true
        )
    );

    public BrazierBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.BRAZIER.get(), pos, state);
    }

    // ── State queries ─────────────────────────────────────────────────────────

    public boolean isBurning() { return !slots.get(SLOT_BURNING_MARKER).isEmpty(); }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SLOT_BURNING_MARKER; i++) if (!slots.get(i).isEmpty()) return false;
        return true;
    }

    public int getIngredientCount() {
        int count = 0;
        for (int i = 0; i < SLOT_BURNING_MARKER; i++) {
            if (!slots.get(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public int getContentsColor() {
        BrazierRecipe recipe = findRecipe(this);
        int rgb = recipe == null ? averageLoadedIngredientColor() : averageIngredientColor(recipe.ingredients());
        float shade = isBurning() ? 1.0F : 0.78F;
        float alpha = isBurning() ? 0.95F : 0.85F;
        return colorWithShade(rgb, shade, alpha);
    }

    public int getCookTime()   { return cookTime; }
    public int getPowerLevel() { return powerLevel; }

    // ── Player interaction helpers ────────────────────────────────────────────

    public boolean addIngredient(ItemStack stack) {
        for (int i = 0; i < SLOT_BURNING_MARKER; i++) {
            if (slots.get(i).isEmpty()) {
                slots.set(i, new ItemStack(stack.getItem(), 1));
                setChanged();
                return true;
            }
        }
        return false;
    }

    public void begin() {
        if (!isEmpty()) {
            cookTime = 0;
            powerStorage = 0L;
            slots.set(SLOT_BURNING_MARKER, new ItemStack(WitcheryItems.WOOD_ASH.get()));
            setChanged();
        }
    }

    public boolean shouldIgniteFromRedstone(boolean powered) {
        boolean risingEdge = previousRedstoneState != powered && powered;
        previousRedstoneState = powered;
        return risingEdge;
    }

    public void reset() {
        clearContent();
        cookTime = 0;
        powerStorage = 0L;
        setChanged();
    }

    public NonNullList<ItemStack> getDrops() {
        NonNullList<ItemStack> drops = NonNullList.create();
        for (int i = 0; i < SLOT_BURNING_MARKER; i++) if (!slots.get(i).isEmpty()) drops.add(slots.get(i));
        return drops;
    }

    // ── Server tick ───────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, BrazierBlockEntity be) {
        if (!be.isBurning()) {
            if (level.getGameTime() % 40 == 0) {
                be.setPowerLevel(findAltar(level, pos) != null ? 1 : 0);
            }
            return;
        }

        BrazierRecipe recipe = findRecipe(be);
        if (recipe == null) {
            be.reset();
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        8, 0.5D, 0.5D, 0.5D, 0.02D);
            }
            return;
        }

        if (recipe.needsAltar()) {
            AltarBlockEntity altar = findAltar(level, pos);
            be.setPowerLevel(altar != null ? 1 : 0);
            if (altar == null || !altar.consumePower((int) POWER_PER_TICK)) return;
        } else {
            be.setPowerLevel(1);
        }

        be.cookTime++;
        long burnTarget = recipe.burnTicks() + be.powerStorage * 400L;
        if (be.cookTime >= burnTarget) {
            runCompletionEffect(level, pos, recipe);
            be.reset();
        } else {
            runBurningEffect(level, pos, recipe, be);
        }
    }

    private static BrazierRecipe findRecipe(BrazierBlockEntity be) {
        ItemStack s0 = be.slots.get(0), s1 = be.slots.get(1), s2 = be.slots.get(2);
        for (BrazierRecipe r : RECIPES) {
            if (r.matches(s0, s1, s2)) return r;
        }
        return null;
    }

    private static AltarBlockEntity findAltar(Level level, BlockPos center) {
        for (BlockPos bp : BlockPos.betweenClosed(
                center.offset(-ALTAR_RADIUS, -ALTAR_RADIUS, -ALTAR_RADIUS),
                center.offset( ALTAR_RADIUS,  ALTAR_RADIUS,  ALTAR_RADIUS))) {
            if (level.getBlockEntity(bp) instanceof AltarBlockEntity altar
                    && altar.getPower() >= (int) POWER_PER_TICK)
                return altar;
        }
        return null;
    }

    private void setPowerLevel(int value) {
        if (powerLevel != value) {
            powerLevel = value;
            setChanged();
        }
    }

    private int averageLoadedIngredientColor() {
        int red = 0;
        int green = 0;
        int blue = 0;
        int count = 0;
        for (int i = SLOT_INGREDIENT_1; i <= SLOT_INGREDIENT_3; i++) {
            ItemStack stack = slots.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int rgb = ingredientContentsColor(stack.getItem());
            red += (rgb >>> 16) & 0xFF;
            green += (rgb >>> 8) & 0xFF;
            blue += rgb & 0xFF;
            count++;
        }
        if (count == 0) {
            return 0x6A5A43;
        }
        return ((red / count) << 16) | ((green / count) << 8) | (blue / count);
    }

    private static int averageIngredientColor(Item[] ingredients) {
        int red = 0;
        int green = 0;
        int blue = 0;
        for (Item item : ingredients) {
            int rgb = ingredientContentsColor(item);
            red += (rgb >>> 16) & 0xFF;
            green += (rgb >>> 8) & 0xFF;
            blue += rgb & 0xFF;
        }
        int count = Math.max(1, ingredients.length);
        return ((red / count) << 16) | ((green / count) << 8) | (blue / count);
    }

    private static int ingredientContentsColor(Item item) {
        if (item == WitcheryItems.WORMWOOD.get()) return 0x5F8A49;
        if (item == WitcheryItems.BAT_WOOL.get()) return 0x65606A;
        if (item == WitcheryItems.GRAVEYARD_DUST.get()) return 0xA7A193;
        if (item == WitcheryItems.CONDENSED_FEAR.get()) return 0x3B2E64;
        if (item == WitcheryItems.REFINED_EVIL.get()) return 0x7D2731;
        if (item == WitcheryItems.FOCUSED_WILL.get()) return 0x6B8CCB;
        if (item == WitcheryItems.TEAR_OF_GODDESS.get()) return 0x8AC8F4;
        if (item == WitcheryItems.WORMY_APPLE.get()) return 0x756B2E;
        if (item == WitcheryItems.QUICKLIME.get()) return 0xDAD4B5;
        if (item == WitcheryItems.INFERNAL_ANIMUS.get()) return 0xD6521F;
        if (item == WitcheryItems.EXHALE_OF_HORNED_ONE.get()) return 0x936B37;
        if (item == WitcheryItems.ROWAN_BERRIES.get()) return 0xB73545;
        if (item == WitcheryItems.FANCIFUL_THREAD.get()) return 0xC684E8;
        if (item == WitcheryItems.DIAMOND_VAPOUR.get()) return 0x77D8F1;
        if (item == Items.BLAZE_ROD) return 0xD98A1F;
        if (item == Items.BLAZE_POWDER) return 0xE8A928;
        if (item == Items.GOLDEN_APPLE) return 0xE0BA38;
        if (item == Items.DIAMOND) return 0x69D6E4;
        if (item == Items.FERMENTED_SPIDER_EYE) return 0x5A314A;
        if (item == Items.ENDER_PEARL) return 0x2C9A83;
        if (item == Items.NETHER_WART) return 0xA42A34;
        if (item == Items.GUNPOWDER) return 0x5F5F5F;
        return 0x6A5A43;
    }

    private static int colorWithShade(int rgb, float shade, float alpha) {
        float red = ((rgb >>> 16) & 0xFF) / 255.0F * shade;
        float green = ((rgb >>> 8) & 0xFF) / 255.0F * shade;
        float blue = (rgb & 0xFF) / 255.0F * shade;
        return ARGB.colorFromFloat(alpha, red, green, blue);
    }

    private static void runBurningEffect(Level level, BlockPos pos, BrazierRecipe recipe, BrazierBlockEntity be) {
        if (recipe.pulseInterval() > 0 && be.cookTime % recipe.pulseInterval() != 0) {
            return;
        }
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        switch (recipe.mode()) {
            case SUMMON -> sendSummonPulse(sl, pos);
            case AURA -> applyEffectsToLiving(level, pos, recipe, recipe.effectRadius());
            case WILTING -> {
                if (tryDrainGrowth(sl, pos)) {
                    be.powerStorage += 2L;
                    if (be.powerStorage == Long.MAX_VALUE) {
                        be.powerStorage = 0L;
                    }
                    be.setChanged();
                }
            }
            case SMOKE -> sendGraveyardMist(sl, pos);
            case COMPLETION_BUFF -> {
            }
        }
    }

    private static void runCompletionEffect(Level level, BlockPos pos, BrazierRecipe recipe) {
        if (recipe.mode() == BrazierMode.SUMMON && level instanceof ServerLevel sl && recipe.summonEntity() != null) {
            spawnSummonedEntity(sl, pos, recipe.summonEntity(), 1.0D, 2.0D, true);
            if (level.random.nextDouble() < 0.05D) {
                spawnSummonedEntity(sl, pos, WitcheryEntities.POLTERGEIST.get(), 6.0D, 10.0D, false);
            }
        } else if (recipe.mode() == BrazierMode.COMPLETION_BUFF) {
            applyEffectsToPlayers(level, pos, recipe, recipe.effectRadius());
            if (level instanceof ServerLevel sl) {
                sendCompletionBuffFx(sl, pos);
            }
        }
    }

    private static void sendSummonPulse(ServerLevel level, BlockPos pos) {
        level.sendParticles(SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, 0xFFFFFF, 1.0F),
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                16, 0.5D, 1.0D, 0.5D, 0.02D);
    }

    private static void sendGraveyardMist(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.EXPLOSION,
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                8, 8.0D, 2.0D, 8.0D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                18, 8.0D, 2.0D, 8.0D, 0.02D);
    }

    private static void sendCompletionBuffFx(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
        level.sendParticles(ParticleTypes.WITCH,
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                24, 0.7D, 0.5D, 0.7D, 0.03D);
    }

    private static void applyEffectsToLiving(Level level, BlockPos pos, BrazierRecipe recipe, int radius) {
        AABB area = new AABB(pos).inflate(radius);
        double radiusSq = radius * radius;
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity.distanceToSqr(x, y, z) > radiusSq) {
                continue;
            }
            for (MobEffectInstance effect : recipe.effects()) {
                entity.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier()));
            }
        }
    }

    private static void applyEffectsToPlayers(Level level, BlockPos pos, BrazierRecipe recipe, int radius) {
        AABB area = new AABB(pos).inflate(radius);
        double radiusSq = radius * radius;
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
            if (player.distanceToSqr(x, y, z) > radiusSq) {
                continue;
            }
            for (MobEffectInstance effect : recipe.effects()) {
                player.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier()));
            }
        }
    }

    private static boolean tryDrainGrowth(ServerLevel level, BlockPos brazierPos) {
        long ticks = level.getGameTime();
        int offsetY = (int) (ticks % 30L) / 5;
        BlockPos target = brazierPos.offset(
                level.random.nextInt(7) - 3,
                -2 + offsetY,
                level.random.nextInt(7) - 3);
        if (!regressAge(level, target)) {
            return false;
        }
        level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF0000FF),
                target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                8, 0.5D, 0.5D, 0.5D, 0.0D);
        healNearbyUndead(level, brazierPos, 3);
        return true;
    }

    private static boolean regressAge(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (var property : state.getProperties()) {
            if (property instanceof IntegerProperty ageProperty && "age".equals(ageProperty.getName())) {
                return regressAgeProperty(level, pos, state, ageProperty);
            }
        }
        return false;
    }

    private static boolean regressAgeProperty(ServerLevel level, BlockPos pos, BlockState state,
                                              IntegerProperty ageProperty) {
        int age = state.getValue(ageProperty);
        int minAge = ageProperty.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(0);
        if (age <= minAge) {
            return false;
        }
        level.setBlock(pos, state.setValue(ageProperty, age - 1), 3);
        return true;
    }

    private static void healNearbyUndead(ServerLevel level, BlockPos pos, int radius) {
        AABB area = new AABB(pos).inflate(radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!entity.getType().is(EntityTypeTags.UNDEAD) || entity.getHealth() >= entity.getMaxHealth()) {
                continue;
            }
            entity.heal(entity.getMaxHealth() * 0.1F);
            level.sendParticles(ParticleTypes.HEART,
                    entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                    3, 0.5D, 0.5D, 0.5D, 0.02D);
        }
    }

    private static void spawnSummonedEntity(ServerLevel level, BlockPos pos, EntityType<?> entityType,
                                            double minRadius, double maxRadius, boolean primary) {
        Entity entity = entityType.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (entity == null) {
            return;
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double radius = minRadius + level.random.nextDouble() * Math.max(0.0D, maxRadius - minRadius);
        double x = pos.getX() + 0.5D + Math.cos(angle) * radius;
        double z = pos.getZ() + 0.5D + Math.sin(angle) * radius;
        double y = pos.getY() + 1.0D;
        entity.setPos(x, y, z);
        entity.setYRot(level.random.nextFloat() * 360.0F);
        entity.setXRot(0.0F);
        level.addFreshEntity(entity);
        level.sendParticles(primary ? ParticleTypes.WITCH : ParticleTypes.SMOKE,
                x, y + 0.5D, z, primary ? 24 : 12, 0.6D, 0.8D, 0.6D, 0.03D);
        if (primary) {
            level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.NOTE_BLOCK_HARP.value(),
                    SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, slots);
        tag.putInt("CookTime", cookTime);
        tag.putInt("PowerLevel", powerLevel);
        tag.putLong("PowerStorage", powerStorage);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, slots);
        cookTime   = tag.getIntOr("CookTime", 0);
        powerLevel = tag.getIntOr("PowerLevel", 0);
        powerStorage = tag.getLongOr("PowerStorage", 0L);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(slots, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(slots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        slots.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        slots.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= SLOT_INGREDIENT_1
                && slot <= SLOT_INGREDIENT_3
                && stack.getCount() == 1
                && slots.get(slot).isEmpty();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return canPlaceItem(index, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }
}
