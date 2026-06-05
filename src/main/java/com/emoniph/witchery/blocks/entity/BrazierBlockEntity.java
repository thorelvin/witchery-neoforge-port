package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final int   EFFECT_RADIUS   = 8;
    private static final int   PULSE_INTERVAL  = 60; // ticks between pulses for pulsing recipes

    private int cookTime   = 0;
    private int powerLevel = 0;
    private boolean previousRedstoneState = false;

    // ── Recipe definition ─────────────────────────────────────────────────────

    /**
     * @param ingredients  1-3 required items (order-insensitive)
     * @param burnTicks    how long to burn before completion
     * @param needsAltar   whether an altar must be nearby
     * @param effects      mob effects to apply (on-complete for one-shot; every PULSE_INTERVAL for pulsing)
     * @param pulsing      if true, effects applied every PULSE_INTERVAL while burning; otherwise applied once at completion
     * @param summonEntity entity to spawn on completion (null for none); a Poltergeist also has a 5% bonus spawn
     */
    public record BrazierRecipe(
        Item[] ingredients,
        int burnTicks,
        boolean needsAltar,
        List<MobEffectInstance> effects,
        boolean pulsing,
        @Nullable EntityType<?> summonEntity,
        boolean smokeParticles
    ) {
        boolean matches(ItemStack s0, ItemStack s1, ItemStack s2) {
            var have = new ArrayList<Item>();
            if (!s0.isEmpty()) have.add(s0.getItem());
            if (!s1.isEmpty()) have.add(s1.getItem());
            if (!s2.isEmpty()) have.add(s2.getItem());
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
            600, true, List.of(), false, WitcheryEntities.SPECTRE.get(), false
        ),
        // Banshee: Wormwood + Condensed Fear + Graveyard Dust → spawn Banshee
        new BrazierRecipe(
            new Item[]{ WitcheryItems.WORMWOOD.get(), WitcheryItems.CONDENSED_FEAR.get(), WitcheryItems.GRAVEYARD_DUST.get() },
            600, true, List.of(), false, WitcheryEntities.BANSHEE.get(), false
        ),
        // Poltergeist: Wormwood + Refined Evil + Focused Will → spawn Poltergeist (no altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.WORMWOOD.get(), WitcheryItems.REFINED_EVIL.get(), WitcheryItems.FOCUSED_WILL.get() },
            900, false, List.of(), false, WitcheryEntities.POLTERGEIST.get(), false
        ),

        // ── Pulsing aura (effects applied every 60 ticks while burning) ───────

        // Anguish of the Dead: Tear of Goddess + Blaze Rod + Golden Apple → Strength II aura
        new BrazierRecipe(
            new Item[]{ WitcheryItems.TEAR_OF_GODDESS.get(), Items.BLAZE_ROD, Items.GOLDEN_APPLE },
            6000, true,
            List.of(new MobEffectInstance(MobEffects.STRENGTH, 120, 1)),
            true, null, false
        ),
        // Fortification of the Corpse: Tear of Goddess + Diamond + Golden Apple → Resistance II aura
        new BrazierRecipe(
            new Item[]{ WitcheryItems.TEAR_OF_GODDESS.get(), Items.DIAMOND, Items.GOLDEN_APPLE },
            6000, true,
            List.of(new MobEffectInstance(MobEffects.RESISTANCE, 120, 1)),
            true, null, false
        ),
        // Deathly Veil: Fermented Spider Eye + Ender Pearl + Diamond → Invisibility aura
        new BrazierRecipe(
            new Item[]{ Items.FERMENTED_SPIDER_EYE, Items.ENDER_PEARL, Items.DIAMOND },
            12000, true,
            List.of(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0)),
            true, null, false
        ),
        // Drain Growth: Condensed Fear + Wormy Apple + Graveyard Dust → Weakness aura (no altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.CONDENSED_FEAR.get(), WitcheryItems.WORMY_APPLE.get(), WitcheryItems.GRAVEYARD_DUST.get() },
            1200, false,
            List.of(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1)),
            true, null, false
        ),
        // Graveyard Mist: Nether Wart + Quicklime + Blaze Powder → smoke cloud (visual only)
        new BrazierRecipe(
            new Item[]{ Items.NETHER_WART, WitcheryItems.QUICKLIME.get(), Items.BLAZE_POWDER },
            6000, true, List.of(), true, null, true
        ),

        // ── One-shot completion effects ────────────────────────────────────────

        // Infernal Animus → Fire Resistance + Strength 5 min (altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.INFERNAL_ANIMUS.get() },
            200, true,
            List.of(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0),
                    new MobEffectInstance(MobEffects.STRENGTH,     6000, 0)),
            false, null, false
        ),
        // Exhale of Horned One → Damage Resistance 3 min (altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.EXHALE_OF_HORNED_ONE.get() },
            300, true,
            List.of(new MobEffectInstance(MobEffects.RESISTANCE, 3600, 0)),
            false, null, false
        ),
        // Rowan Berries → Regeneration 1 min
        new BrazierRecipe(
            new Item[]{ WitcheryItems.ROWAN_BERRIES.get() },
            160, false,
            List.of(new MobEffectInstance(MobEffects.REGENERATION, 1200, 0)),
            false, null, false
        ),
        // Fanciful Thread → Luck 2 min
        new BrazierRecipe(
            new Item[]{ WitcheryItems.FANCIFUL_THREAD.get() },
            200, false,
            List.of(new MobEffectInstance(MobEffects.LUCK, 2400, 0)),
            false, null, false
        ),
        // Diamond Vapour + Fanciful Thread → Night Vision + Speed 2 min (altar)
        new BrazierRecipe(
            new Item[]{ WitcheryItems.DIAMOND_VAPOUR.get(), WitcheryItems.FANCIFUL_THREAD.get() },
            400, true,
            List.of(new MobEffectInstance(MobEffects.NIGHT_VISION,   2400, 0),
                    new MobEffectInstance(MobEffects.SPEED, 2400, 0)),
            false, null, false
        ),
        // Gunpowder → Strength + Speed burst 30 sec
        new BrazierRecipe(
            new Item[]{ Items.GUNPOWDER },
            60, false,
            List.of(new MobEffectInstance(MobEffects.STRENGTH,   600, 1),
                    new MobEffectInstance(MobEffects.SPEED, 600, 1)),
            false, null, false
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
            slots.set(SLOT_BURNING_MARKER, new ItemStack(Items.CHARCOAL));
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
            if (level.getGameTime() % 40 == 0)
                be.powerLevel = findAltar(level, pos) != null ? 1 : 0;
            return;
        }

        BrazierRecipe recipe = findRecipe(be);
        if (recipe == null) {
            be.reset();
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.0f);
            return;
        }

        if (recipe.needsAltar()) {
            AltarBlockEntity altar = findAltar(level, pos);
            be.powerLevel = altar != null ? 1 : 0;
            if (altar == null || !altar.consumePower((int) POWER_PER_TICK)) return;
        }

        be.cookTime++;

        // Pulsing aura: apply effects every PULSE_INTERVAL ticks
        if (recipe.pulsing() && be.cookTime % PULSE_INTERVAL == 0) {
            applyEffectsToPlayers(level, pos, recipe);
            if (recipe.smokeParticles() && level instanceof ServerLevel slSmoke) {
                slSmoke.sendParticles(ParticleTypes.EXPLOSION,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        3, 0.5, 0.2, 0.5, 0.1);
            }
        }

        if (be.cookTime >= recipe.burnTicks()) {
            // One-shot completion effects
            if (!recipe.pulsing() && !recipe.effects().isEmpty()) {
                applyEffectsToPlayers(level, pos, recipe);
            }
            // Entity summoning
            if (recipe.summonEntity() != null && level instanceof ServerLevel sl) {
                spawnSummonedEntity(sl, pos, recipe);
            }
            be.reset();
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                        20, 0.5, 0.5, 0.5, 0.1);
            }
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

    /** Apply effects only to players. Wilting recipe applies Weakness to all living; others players only. */
    private static void applyEffectsToPlayers(Level level, BlockPos pos, BrazierRecipe recipe) {
        AABB area = new AABB(pos).inflate(EFFECT_RADIUS);
        // Wilting: Weakness applied to all living entities (including undead for thematic healing feel)
        boolean all = recipe.summonEntity() == null && recipe.ingredients().length >= 3
                && recipe.ingredients()[0] == WitcheryItems.CONDENSED_FEAR.get();
        List<? extends LivingEntity> targets = all
                ? level.getEntitiesOfClass(LivingEntity.class, area)
                : level.getEntitiesOfClass(Player.class, area);
        for (LivingEntity e : targets) {
            for (MobEffectInstance eff : recipe.effects()) {
                e.addEffect(new MobEffectInstance(eff.getEffect(), eff.getDuration(), eff.getAmplifier()));
            }
        }
    }

    private static void spawnSummonedEntity(ServerLevel level, BlockPos pos, BrazierRecipe recipe) {
        var entity = recipe.summonEntity().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (entity != null) {
            entity.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            level.addFreshEntity(entity);
        }
        // 5% bonus Poltergeist spawn on all summoning recipes
        if (level.random.nextDouble() < 0.05) {
            var poltergeist = WitcheryEntities.POLTERGEIST.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (poltergeist != null) {
                poltergeist.setPos(pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 6, pos.getY() + 1, pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 6);
                level.addFreshEntity(poltergeist);
            }
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, slots);
        tag.putInt("CookTime", cookTime);
        tag.putInt("PowerLevel", powerLevel);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        ContainerHelper.loadAllItems(tag, slots);
        cookTime   = tag.getIntOr("CookTime", 0);
        powerLevel = tag.getIntOr("PowerLevel", 0);
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
        setChanged();
        return ContainerHelper.removeItem(slots, slot, amount);
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
