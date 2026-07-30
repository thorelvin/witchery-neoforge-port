package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.item.DemonicContractItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

public class FlameImpEntity extends TamableAnimal {

    // ── Synced ────────────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Boolean> DATA_POWERED =
            SynchedEntityData.defineId(FlameImpEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_AFFECTION =
            SynchedEntityData.defineId(FlameImpEntity.class, EntityDataSerializers.INT);

    // ── Server-side state ────────────────────────────────────────────────────
    private int  secretsShared = 0;
    private long lastGiftTime  = 0L;
    private long powerUpExpiry = 0L;
    private BlockPos home      = BlockPos.ZERO;

    static final int REWARD_AFFECTION_LEVEL = 20;
    static final int GIFT_DELAY_TICKS = 20 * 60 * 3;
    static final int AFFECTION_DECAY_INTERVAL_TICKS = 20 * 60 * 5;
    static final int CONTRACT_ABANDON_AGE_TICKS = 20 * 60 * 60;
    static final int PASSIVE_HEAL_INTERVAL_TICKS = 400;
    static final int POWERED_HEAL_INTERVAL_TICKS = 20;
    static final long POWER_DURATION_TICKS = 20L * 60 * 60;
    static final double HOME_RANGE = 16.0D;
    static final float NORMAL_DAMAGE_CAP = 15.0F;
    static final float POWERED_DAMAGE_CAP = 5.0F;
    static final float NORMAL_ATTACK_DAMAGE = 4.0F;
    static final float POWERED_ATTACK_DAMAGE = 8.0F;

    // Shinies: item → affection points
    private static final Map<Item, Integer> SHINIES;
    static {
        Map<Item, Integer> m = new HashMap<>();
        m.put(Items.DIAMOND, 8);
        m.put(Items.DIAMOND_AXE, 24);
        m.put(Items.DIAMOND_HOE, 16);
        m.put(Items.DIAMOND_SWORD, 16);
        m.put(Items.DIAMOND_SHOVEL, 8);
        m.put(Items.DIAMOND_PICKAXE, 24);
        m.put(Items.EMERALD, 3);
        m.put(Items.GOLD_INGOT, 1);
        m.put(Items.NETHER_STAR, 16);
        m.put(Items.BLAZE_ROD, 1);
        m.put(Items.GHAST_TEAR, 4);
        m.put(Items.GOLDEN_AXE, 3);
        m.put(Items.GOLDEN_SWORD, 2);
        m.put(Items.GOLDEN_HOE, 2);
        m.put(Items.GOLDEN_SHOVEL, 1);
        m.put(Items.GOLDEN_PICKAXE, 3);
        m.put(Items.GOLD_BLOCK, 9);
        m.put(Items.EMERALD_BLOCK, 27);
        m.put(Items.DIAMOND_BLOCK, 72);
        m.put(Items.LAPIS_BLOCK, 7);
        m.put(Items.REDSTONE_BLOCK, 5);
        SHINIES = Collections.unmodifiableMap(m);
    }

    private static final String[] DEMON_NAMES = {
        "Ppaironael", "Aethon", "Tyrnak", "Beelzebuth", "Botis", "Moloch", "Taet",
        "Epnanaet", "Unonom", "Hexpemsazon", "Thayax", "Ethahoat", "Pruslas",
        "Ahtuxies", "Laripael", "Elxar", "Tarihimal", "Sapanolr", "Sahaminapiel",
        "Honed", "Oghmus", "Zedeson", "Halmaneop", "Nopoz", "Ekarnahox",
        "Sacuhatakael", "Ticos", "Arametheus", "Azmodaeus", "Larhepeis",
        "Topriraiz", "Rarahaimzah", "Tedrahamael", "Osaselael", "Phlegon",
        "Nelokhiel", "Haristum", "Zul", "Larhepeis", "Aamon", "Tramater", "Ehhbes",
        "Kra`an", "Quarax", "Hotesiatrem", "Surgat", "Nu`uhn", "Litedabh",
        "Unonom", "Bolenoz", "Hilopael", "Haristum", "Uhn", "Hiepacth", "Pemcapso",
        "Ankou", "Pundohien", "Koit", "Montobulus", "Amsaset", "Aropet", "Isnal",
        "Solael", "Exroh", "Sidragrosam", "Pnecamob", "Malashim", "Beelzebuth",
        "Ehohit", "Izatap", "Olon", "Assoaz", "Agalierept", "Krakus", "Umlaboor",
        "Aknrar", "Damaz", "Rhysus", "Pundohien", "Ba`al", "Rasuniolpas", "Anhoor",
        "Nyarlathotep", "Krakus", "Larhepeis", "Itakup", "Erdok", "Umlaboor",
        "Ezon", "Krakus", "Glassyalabolas", "Kra`an", "Ehnnat", "Terxor", "Asramel",
        "Tadal", "Arpzih", "Azmodaeus", "Henbolaron", "Rhysus"
    };

    public FlameImpEntity(EntityType<? extends FlameImpEntity> type, Level level) {
        super(type, level);
        setTame(false, false);
        setPathfindingMalus(PathType.WATER, 8.0F);
        if (getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanFloat(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,   50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE,  4.0)
                .add(Attributes.FOLLOW_RANGE,  16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_POWERED, false);
        builder.define(DATA_AFFECTION, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new HomeRestrictedStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(3, new HostileToPlayersGoal(this));
    }

    /** Only target players when untamed. */
    private static class HostileToPlayersGoal extends NearestAttackableTargetGoal<Player> {
        private final FlameImpEntity imp;
        HostileToPlayersGoal(FlameImpEntity imp) {
            super(imp, Player.class, 0, false, true, null);
            this.imp = imp;
        }
        @Override public boolean canUse() { return !imp.isTame() && super.canUse(); }
    }

    // ── Ownership ─────────────────────────────────────────────────────────────

    public boolean isOwned() {
        return isTame() && getOwnerReference() != null;
    }

    @Nullable
    public UUID getOwnerUuid() {
        EntityReference<LivingEntity> owner = getOwnerReference();
        return owner == null ? null : owner.getUUID();
    }

    private void acceptOwnershipContract(Player player) {
        tame(player);
        setOrderedToSit(false);
        setInSittingPose(false);
        home = new BlockPos((int) getX(), (int) getY(), (int) getZ());
        setTarget(null);
        getNavigation().stop();
        setPersistenceRequired();
        String demonName = randomDemonName();
        setCustomName(Component.literal(demonName).withStyle(s -> s.withColor(0xAA0000)));
        player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.contract.deal", demonName)
                .withStyle(s -> s.withColor(0xAA0000)), false);
    }

    // ── Powered state ─────────────────────────────────────────────────────────

    public boolean isPowered() { return entityData.get(DATA_POWERED); }

    private void setPowered(boolean powered) {
        if (entityData.get(DATA_POWERED) != powered) {
            entityData.set(DATA_POWERED, powered);
            refreshDimensions();
        }
    }

    private void powerUpForSourceDuration() {
        powerUpExpiry = currentServerTick() + POWER_DURATION_TICKS;
        setPowered(true);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_POWERED.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions dimensions = super.getDefaultDimensions(pose);
        return isPowered() ? dimensions.scale(1.5F, 1.0F) : dimensions;
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            if (isPowered()) {
                double width = getBbWidth();
                level().addParticle(ParticleTypes.FLAME,
                        getX() - width * 0.5D + random.nextDouble() * width,
                        getY() + 0.1D + random.nextDouble() * 2.0D,
                        getZ() - width * 0.5D + random.nextDouble() * width,
                        0.0D, 0.0D, 0.0D);
            }
            return;
        }

        if (powerUpExpiry > 0L && currentServerTick() >= powerUpExpiry) {
            setPowered(false);
            powerUpExpiry = 0L;
        }

        if (isPowered() && tickCount % POWERED_HEAL_INTERVAL_TICKS == 0) {
            heal(1.0F);
        }
        if (tickCount % PASSIVE_HEAL_INTERVAL_TICKS == 0) {
            heal(1.0F);
        }

        if (isOwned() && tickCount % AFFECTION_DECAY_INTERVAL_TICKS == 0
                && getOwner() instanceof ServerPlayer owner) {
            setAffection(Math.max(0, getAffection() - 1));
            if (getAffection() == 0 && tickCount > CONTRACT_ABANDON_AGE_TICKS
                    && random.nextDouble() < 0.01D) {
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.7D, getZ(),
                            16, 0.35D, 0.45D, 0.35D, 0.02D);
                }
                playSound(WitcherySoundEvents.MOB_IMP_LAUGH.get(), 1.0F, getVoicePitch());
                owner.displayClientMessage(Component.translatable(
                                "entity.witchery.flame_imp.goodbye", getDisplayName())
                        .withStyle(s -> s.withColor(0xAA0000)), false);
                discard();
            }
        }
    }

    // ── Interaction (right-click) ─────────────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return InteractionResult.SUCCESS;
        }

        // ── Taming with Demonic Contract ──
        if (!isTame() && DemonicContractItem.isOwnershipContract(stack)) {
            Optional<UUID> contractOwner = DemonicContractItem.getBoundUUID(stack);
            if (contractOwner.isEmpty()) {
                playImpLaugh();
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.contract.unsigned", getDisplayName())
                        .withStyle(s -> s.withColor(0xAA0000)), false);
                return InteractionResult.CONSUME;
            }
            if (!contractOwner.get().equals(player.getUUID())) {
                playImpLaugh();
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.contract.notowners", getDisplayName())
                        .withStyle(s -> s.withColor(0xAA0000)), false);
                return InteractionResult.CONSUME;
            }
            if (player.experienceLevel < 25 && !player.isCreative()) {
                playImpLaugh();
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.contract.noxp", getDisplayName())
                        .withStyle(s -> s.withColor(0xAA0000)), false);
                return InteractionResult.CONSUME;
            }
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            player.giveExperienceLevels(-25);
            acceptOwnershipContract(player);
            playImpLaugh();
            return InteractionResult.SUCCESS;
        }

        if (!isTame()) return super.mobInteract(player, hand);

        // ── Tamed interactions ──

        if (DemonicContractItem.isSpellContract(stack) && DemonicContractItem.isBound(stack)
                && player instanceof ServerPlayer serverPlayer) {
            return castSpellContract(serverPlayer, stack);
        }

        // Demon Heart → power up
        if (stack.is(WitcheryItems.DEMON_HEART.get())) {
            if (!player.isCreative()) stack.shrink(1);
            powerUpForSourceDuration();
            playImpLaugh();
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.power",
                    getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.SUCCESS;
        }

        // Icy Needle → cancel power
        if (stack.is(WitcheryItems.ICY_NEEDLE.get())) {
            if (!player.isCreative()) stack.shrink(1);
            powerUpExpiry = 0L;
            setPowered(false);
            playImpLaugh();
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.powerloss",
                    getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.SUCCESS;
        }

        // Shiny item → raise affection
        Integer boost = SHINIES.get(stack.getItem());
        if (boost != null && !stack.isDamaged()) {
            if (!player.isCreative()) stack.shrink(1);
            int affection = getAffection() + boost;
            playImpLaugh();

            long now = currentServerTick();
            if (affection >= REWARD_AFFECTION_LEVEL
                    && (now > lastGiftTime + GIFT_DELAY_TICKS || player.isCreative())
                    && random.nextInt(sourceGiftRollBound(affection)) == 0) {
                giveGift(player, now);
                affection = 0;
            } else if (now < lastGiftTime + GIFT_DELAY_TICKS) {
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.toomany",
                        getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
            } else {
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.like",
                        getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
            }
            setAffection(affection);
            return InteractionResult.SUCCESS;
        }

        playImpLaugh();
        player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.hate",
                getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult castSpellContract(ServerPlayer player, ItemStack stack) {
        playSound(WitcherySoundEvents.MOB_IMP_LAUGH.get(), 0.5F, getVoicePitch());
        if (isPowered()) {
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.spell.toomuchpower", getDisplayName())
                    .withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.CONSUME;
        }
        if (getAffection() < REWARD_AFFECTION_LEVEL) {
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.spell.notliked", getDisplayName())
                    .withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.CONSUME;
        }

        long now = currentServerTick();
        if (now <= lastGiftTime + GIFT_DELAY_TICKS && !player.isCreative()) {
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.spell.toooften", getDisplayName())
                    .withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.CONSUME;
        }

        Optional<LivingEntity> target = DemonicContractItem.findBoundTarget(((ServerLevel) player.level()).getServer(), stack);
        if (target.isEmpty()) {
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.spell.cannotfind",
                    getDisplayName(), DemonicContractItem.getBoundName(stack)).withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.CONSUME;
        }

        LivingEntity targetEntity = target.get();
        if (DemonicContractItem.activate(stack, targetEntity)) {
            lastGiftTime = now;
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.7D, getZ(),
                        12, 0.35D, 0.35D, 0.35D, 0.03D);
            }
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.spell.feelthefire",
                    getDisplayName(), targetEntity.getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
        } else {
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.spell.failed",
                    getDisplayName(), targetEntity.getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
        }
        return InteractionResult.SUCCESS;
    }

    private void giveGift(Player player, long now) {
        lastGiftTime = now;
        ItemStack gift = buildGift();
        spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), gift);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, 0xFFFFFF, 1.0F),
                    getX(), getY() + 0.5D * getBbHeight(), getZ(),
                    16, 0.5D * getBbWidth(), 0.5D * getBbHeight(),
                    0.5D * getBbWidth(), 0.0D);
        }
        playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.reciprocate",
                getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
    }

    private ItemStack buildGift() {
        return switch (secretsShared++) {
            case 0  -> new ItemStack(WitcheryItems.HUNGER_DEMON_SOUL.get());
            case 1  -> new ItemStack(WitcheryItems.FEAR_DEMON_SOUL.get());
            case 2  -> new ItemStack(WitcheryItems.ANGUISH_DEMON_SOUL.get());
            case 3  -> new ItemStack(WitcheryItems.TORMENT_SCROLL.get());
            default -> randomExtraDrop();
        };
    }

    private ItemStack randomExtraDrop() {
        return switch (random.nextInt(7)) {
            case 0 -> new ItemStack(WitcheryItems.BAT_WOOL.get(), 5);
            case 1 -> new ItemStack(WitcheryItems.TONGUE_OF_DOG.get(), 5);
            case 2 -> new ItemStack(WitcheryItems.TOE_OF_FROG.get(), 2);
            case 3 -> new ItemStack(WitcheryItems.OWLETS_WING.get(), 2);
            case 4 -> new ItemStack(WitcheryItems.ENT_TWIG.get());
            case 5 -> new ItemStack(WitcheryItems.INFERNAL_BLOOD.get(), 2);
            default -> new ItemStack(WitcheryItems.CREEPER_HEART.get(), 2);
        };
    }

    // ── Combat ────────────────────────────────────────────────────────────────

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource src, float amount) {
        return super.hurtServer(level, src,
                Math.min(amount, isPowered() ? POWERED_DAMAGE_CAP : NORMAL_DAMAGE_CAP));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel _sl, net.minecraft.world.entity.Entity target) {
        float damage = isPowered() ? POWERED_ATTACK_DAMAGE : NORMAL_ATTACK_DAMAGE;
        target.hurt(damageSources().mobAttack(this), damage);
        return true;
    }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    public boolean removeWhenFarAway(double dist) { return true; }

    @Override
    protected SoundEvent getAmbientSound() {
        return WitcherySoundEvents.MOB_IMP_LAUGH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return WitcherySoundEvents.MOB_IMP_HIT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WitcherySoundEvents.MOB_IMP_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return isPowered()
                ? (random.nextFloat() - random.nextFloat()) * 0.2F + 0.7F
                : (random.nextFloat() - random.nextFloat()) * 0.2F + 1.1F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 20 * 40;
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Affection", getAffection());
        tag.putInt("SecretsShared", secretsShared);
        tag.putLong("LastGiftTime", lastGiftTime);
        tag.putLong("PowerUpUntil2", powerUpExpiry);
        tag.putInt("HomeLocX", home.getX());
        tag.putInt("HomeLocY", home.getY());
        tag.putInt("HomeLocZ", home.getZ());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        if (getOwnerReference() == null) {
            String legacyOwner = tag.getStringOr("OwnerUUID", "");
            try {
                if (!legacyOwner.isEmpty()) {
                    setOwnerReference(EntityReference.of(UUID.fromString(legacyOwner)));
                    setTame(true, false);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        setAffection(tag.getIntOr("Affection", 0));
        secretsShared = tag.getIntOr("SecretsShared", 0);
        lastGiftTime  = tag.getLongOr("LastGiftTime", 0L);

        long now = currentServerTick();
        if (tag.getLong("PowerUpUntil2").isPresent()) {
            powerUpExpiry = tag.getLongOr("PowerUpUntil2", 0L);
        } else if (tag.getLong("PowerUpExpiry").isPresent()) {
            long remaining = tag.getLongOr("PowerUpExpiry", 0L) - level().getGameTime();
            powerUpExpiry = remaining > 0L ? now + Math.min(remaining, POWER_DURATION_TICKS) : 0L;
        } else if (tag.getLongOr("PowerUpUntil", 0L) > 0L) {
            powerUpExpiry = now + POWER_DURATION_TICKS;
        }

        if (tag.getInt("HomeLocX").isPresent()) {
            home = new BlockPos(
                    tag.getIntOr("HomeLocX", 0),
                    tag.getIntOr("HomeLocY", 0),
                    tag.getIntOr("HomeLocZ", 0));
        } else {
            home = new BlockPos(
                    tag.getIntOr("HomeX", 0),
                    tag.getIntOr("HomeY", 0),
                    tag.getIntOr("HomeZ", 0));
        }

        if (isOwned()) {
            setPersistenceRequired();
        }
        setPowered(powerUpExpiry > now);
        if (!isPowered()) {
            powerUpExpiry = 0L;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String randomDemonName() {
        if (random.nextInt(5) == 0) {
            return DEMON_NAMES[random.nextInt(DEMON_NAMES.length)];
        }
        return DEMON_NAMES[random.nextInt(DEMON_NAMES.length)]
                + " " + DEMON_NAMES[random.nextInt(DEMON_NAMES.length)];
    }

    private void playImpLaugh() {
        playSound(WitcherySoundEvents.MOB_IMP_LAUGH.get(), 0.5F, getVoicePitch());
    }

    private void setAffection(int affection) {
        entityData.set(DATA_AFFECTION, Math.max(0, affection));
    }

    public int getAffection() {
        return entityData.get(DATA_AFFECTION);
    }

    static int sourceShinyValue(Item item) {
        return SHINIES.getOrDefault(item, 0);
    }

    static int sourceShinyCount() {
        return SHINIES.size();
    }

    static int sourceDemonNameCount() {
        return DEMON_NAMES.length;
    }

    static int sourceGiftRollBound(int affection) {
        return Math.max(1, 10 - Math.max(affection - REWARD_AFFECTION_LEVEL, 0));
    }

    static long currentServerTick() {
        return System.currentTimeMillis() / 50L;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal partner) {
        return false;
    }

    private static final class HomeRestrictedStrollGoal extends RandomStrollGoal {
        private final FlameImpEntity imp;

        private HomeRestrictedStrollGoal(FlameImpEntity imp, double speedModifier) {
            super(imp, speedModifier);
            this.imp = imp;
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            Vec3 candidate = super.getPosition();
            if (candidate == null) {
                return null;
            }
            return candidate.distanceToSqr(
                    imp.home.getX(), imp.home.getY(), imp.home.getZ()) <= HOME_RANGE * HOME_RANGE
                    ? candidate
                    : null;
        }
    }
}
