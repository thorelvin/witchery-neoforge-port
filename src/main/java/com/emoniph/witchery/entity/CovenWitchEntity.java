package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Coven Witch — a friendly wandering merchant that sells witchery ingredients.
 * Skin is randomly assigned on spawn (0-4 → covenwitch1-5.png).
 */
public class CovenWitchEntity extends AbstractVillager {

    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(CovenWitchEntity.class, EntityDataSerializers.INT);

    public CovenWitchEntity(EntityType<? extends CovenWitchEntity> type, Level level) {
        super(type, level);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
    }

    public int getSkin() { return this.entityData.get(SKIN); }

    // ── Attributes + goals ────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(3, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // ── Trades ────────────────────────────────────────────────────────────────

    @Override
    protected void updateTrades(net.minecraft.server.level.ServerLevel _level) {
        var offers = this.getOffers();
        // Buy witchery plants for emeralds
        // Common herbs — cheap
        addTrade(offers, Items.EMERALD, 1,  WitcheryItems.BELLADONNA.get(),    2,  8, 5);
        addTrade(offers, Items.EMERALD, 1,  WitcheryItems.MANDRAKE_ROOT.get(), 1,  8, 5);
        addTrade(offers, Items.EMERALD, 1,  WitcheryItems.ARTICHOKE.get(),     2, 12, 3);
        addTrade(offers, Items.EMERALD, 1,  WitcheryItems.WORMWOOD.get(),      2, 12, 3);
        addTrade(offers, Items.EMERALD, 1,  WitcheryItems.SNOWBELL.get(),      2, 10, 3);
        addTrade(offers, Items.EMERALD, 2,  WitcheryItems.ROWAN_BERRIES.get(), 4, 10, 4);
        addTrade(offers, Items.EMERALD, 3,  WitcheryItems.WOOD_ASH.get(),      4, 12, 4);
        // Uncommon ingredients
        addTrade(offers, Items.EMERALD, 4,  WitcheryItems.TAGLOCK_KIT.get(),   1,  6, 6);
        addTrade(offers, Items.EMERALD, 5,  WitcheryItems.EXHALE_OF_HORNED_ONE.get(), 1, 6, 8);
        addTrade(offers, Items.EMERALD, 5,  WitcheryItems.HINT_OF_REBIRTH.get(),      1, 6, 8);
        addTrade(offers, Items.EMERALD, 8,  WitcheryItems.BAT_WOOL.get(),             2, 5, 10);
        // Pre-brewed brews — expensive
        addTrade(offers, Items.EMERALD, 6,  WitcheryItems.BREW_OF_REVEALING.get(),  2, 5, 8);
        addTrade(offers, Items.EMERALD, 8,  WitcheryItems.BREW_OF_SLEEPING.get(),   2, 5, 10);
        // Sell witchery plants to coven for emeralds
        addTrade(offers, WitcheryItems.BELLADONNA.get(),    3, Items.EMERALD, 1, 16, 2);
        addTrade(offers, WitcheryItems.MANDRAKE_ROOT.get(), 2, Items.EMERALD, 1, 16, 2);
        addTrade(offers, WitcheryItems.WORMWOOD.get(),      3, Items.EMERALD, 1, 16, 2);
    }

    private static void addTrade(net.minecraft.world.item.trading.MerchantOffers offers,
                                  net.minecraft.world.item.Item costItem, int costCount,
                                  net.minecraft.world.item.Item resultItem, int resultCount,
                                  int maxUses, int xp) {
        offers.add(new MerchantOffer(
                new ItemCost(costItem, costCount),
                new ItemStack(resultItem, resultCount),
                maxUses, xp, 0.05f));
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    private static final String[] GREET_KEYS = {
        "witchery.witch.say.greet1",
        "witchery.witch.say.greet2",
        "witchery.witch.say.greet3",
        "witchery.witch.say.greet4",
        "witchery.witch.say.greet5",
    };

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && hand == InteractionHand.MAIN_HAND && !isTrading()) {
            String witchName = hasCustomName() ? getCustomName().getString() : "Witch";
            // Send translatable component — client handles localisation
            player.displayClientMessage(
                    Component.literal("§9<" + witchName + "> §r")
                             .append(Component.translatable(GREET_KEYS[random.nextInt(GREET_KEYS.length)])), false);
        }
        return super.mobInteract(player, hand); // opens the trade GUI
    }

    // Intentional: no villager-level progress bar; trades available immediately, no xp rewards
    @Override public boolean showProgressBar() { return false; }
    @Override public int getVillagerXp()       { return 0; }
    @Override public boolean canRestock()      { return true; }

    // ── Misc ──────────────────────────────────────────────────────────────────

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) { return null; }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) { /* no xp reward */ }

    @Override
    public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.WITCH_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WITCH_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WITCH_DEATH; }

    // ── Spawn init ────────────────────────────────────────────────────────────

    private static final String[] FIRST_NAMES = {
        "Abigail","Agatha","Alcina","Amanita","Ambrosia","Anastasia","Andromeda","Aradia",
        "Artemis","Astrid","Aurora","Beatrix","Belladonna","Callypso","Carmilla","Cassandra",
        "Celeste","Circe","Cordelia","Drusilla","Elphaba","Elvira","Esmerelda","Evangeline",
        "Hecate","Helena","Iris","Isabeau","Isis","Isolde","Jinx","Katrina","Lavinia","Lenore",
        "Lilith","Luna","Lydia","Magdalena","Medea","Melantha","Miranda","Moira","Morgana",
        "Morticia","Nyx","Ophelia","Pandora","Persephone","Prudence","Raven","Rowena","Sabrina",
        "Selene","Seraphina","Sybil","Tabitha","Tempest","Willow","Zinnia"
    };
    private static final String[] SURNAMES = {
        "Ashwood","Bathory","Blackthorn","Blackwood","Bloodstone","Coldwell","Crowley","Darke",
        "Deadwood","DeLioncourt","Drago","Eldritch","Faust","Grimm","Grimsby","Harker",
        "Hemlock","Hexwood","Hollow","Lavinia","LeFay","Mandrake","Montague","Moone","Nocton",
        "Poe","Raven","Ravencraft","Reaper","Rookwood","Shelley","Spectre","Spellman","Stoker",
        "Stormwind","Strange","Thornwood","Valentine","Van Helsing","Vesper","Widdowes","Wilde",
        "Winterrose","Witchwood","Wolftooth"
    };

    public static String generateWitchName() {
        java.util.Random ra = new java.util.Random();
        return FIRST_NAMES[ra.nextInt(FIRST_NAMES.length)] + " " + SURNAMES[ra.nextInt(SURNAMES.length)];
    }

    @Override
    public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                                        net.minecraft.world.DifficultyInstance difficulty,
                                        net.minecraft.world.entity.EntitySpawnReason spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        this.entityData.set(SKIN, this.random.nextInt(5));
        if (!hasCustomName()) {
            setCustomName(Component.literal(generateWitchName()));
        }
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }
}
