package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.DemonicContractItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;

public class FlameImpEntity extends Monster {

    // ── Synced ────────────────────────────────────────────────────────────────
    private static final EntityDataAccessor<Boolean> DATA_POWERED =
            SynchedEntityData.defineId(FlameImpEntity.class, EntityDataSerializers.BOOLEAN);

    // ── Server-side state ────────────────────────────────────────────────────
    @Nullable private UUID ownerUuid;
    private int  affection     = 0;
    private int  secretsShared = 0;
    private long lastGiftTime  = 0L;
    private long powerUpExpiry = 0L;
    private BlockPos home      = BlockPos.ZERO;

    private static final int  MAX_AFFECTION   = 20;
    private static final int  GIFT_DELAY_TICKS = 20 * 60 * 3; // 3 min
    private static final long POWER_DURATION   = 20L * 60 * 60; // 60 min in ticks, matching source

    // Shinies: item → affection points
    private static final Map<Item, Integer> SHINIES;
    static {
        Map<Item, Integer> m = new HashMap<>();
        m.put(Items.DIAMOND,            16);
        m.put(Items.EMERALD,            24);
        m.put(Items.GOLD_INGOT,          8);
        m.put(Items.GOLD_NUGGET,         2);
        m.put(Items.AMETHYST_SHARD,      4);
        m.put(Items.LAPIS_LAZULI,        3);
        m.put(Items.QUARTZ,              3);
        m.put(Items.PRISMARINE_CRYSTALS, 2);
        m.put(Items.DIAMOND_BLOCK,      72);
        m.put(Items.EMERALD_BLOCK,      27);
        m.put(Items.GOLD_BLOCK,          9);
        SHINIES = Collections.unmodifiableMap(m);
    }

    private static final String[] DEMON_NAMES = {
        "Ppaironael","Aethon","Tyrnak","Beelzebuth","Botis","Moloch","Taet",
        "Epnanaet","Unonom","Hexpemsazon","Thayax","Ethahoat","Pruslas",
        "Ahtuxies","Laripael","Elxar","Tarihimal","Sapanolr","Sahaminapiel",
        "Honed","Oghmus","Zedeson","Halmaneop","Nopoz","Ekarnahox",
        "Sacuhatakael","Ticos","Arametheus","Azmodaeus","Larhepeis",
        "Topriraiz","Rarahaimzah","Tedrahamael","Osaselael","Phlegon",
        "Nelokhiel","Haristum","Zul","Aamon","Tramater","Ehhbes",
        "Quarax","Hotesiatrem","Surgat","Litedabh","Bolenoz","Hilopael",
        "Uhn","Hiepacth","Pemcapso","Ankou","Pundohien","Koit",
        "Montobulus","Amsaset","Aropet","Isnal","Solael","Exroh",
        "Sidragrosam","Pnecamob","Malashim","Ehohit","Izatap","Olon",
        "Assoaz","Agalierept","Krakus","Umlaboor","Aknrar","Damaz",
        "Rhysus","Ba`al","Rasuniolpas","Anhoor","Nyarlathotep","Erdok",
        "Ezon","Glassyalabolas","Ehnnat","Terxor","Asramel","Tadal",
        "Arpzih","Henbolaron"
    };

    public FlameImpEntity(EntityType<? extends FlameImpEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,   50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE,  4.0)
                .add(Attributes.FOLLOW_RANGE,  24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_POWERED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new HostileToPlayersGoal(this));
    }

    /** Only target players when untamed. */
    private static class HostileToPlayersGoal extends NearestAttackableTargetGoal<Player> {
        private final FlameImpEntity imp;
        HostileToPlayersGoal(FlameImpEntity imp) { super(imp, Player.class, true); this.imp = imp; }
        @Override public boolean canUse() { return !imp.isOwned() && super.canUse(); }
    }

    // ── Ownership ─────────────────────────────────────────────────────────────

    public boolean isOwned() { return ownerUuid != null; }

    @Nullable public UUID getOwnerUuid() { return ownerUuid; }

    private void tame(Player player) {
        ownerUuid = player.getUUID();
        home      = blockPosition();
        String demonName = randomDemonName();
        setCustomName(Component.literal(demonName).withStyle(s -> s.withColor(0xAA0000)));
        setCustomNameVisible(true);
        player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.contract.deal", demonName)
                .withStyle(s -> s.withColor(0xAA0000)), false);
    }

    // ── Powered state ─────────────────────────────────────────────────────────

    public boolean isPowered() { return entityData.get(DATA_POWERED); }

    private void setPowered(boolean v) {
        entityData.set(DATA_POWERED, v);
        if (!level().isClientSide()) {
            powerUpExpiry = v ? level().getGameTime() + POWER_DURATION : 0L;
        }
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        // Power-up expiry
        if (powerUpExpiry > 0 && level().getGameTime() >= powerUpExpiry) {
            setPowered(false);
        }

        // Regenerate HP every 20 ticks when tamed
        if (isOwned() && tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
            heal(1.0f);
        }
    }

    // ── Interaction (right-click) ─────────────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = player.getItemInHand(hand);

        // ── Taming with Demonic Contract ──
        if (!isOwned() && DemonicContractItem.isOwnershipContract(stack)) {
            Optional<UUID> contractOwner = DemonicContractItem.getBoundUUID(stack);
            if (contractOwner.isEmpty()) {
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.contract.unsigned", getDisplayName())
                        .withStyle(s -> s.withColor(0xAA0000)), false);
                return InteractionResult.CONSUME;
            }
            if (!contractOwner.get().equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.contract.notowners", getDisplayName())
                        .withStyle(s -> s.withColor(0xAA0000)), false);
                return InteractionResult.CONSUME;
            }
            if (player.experienceLevel < 25 && !player.isCreative()) {
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.contract.noxp", getDisplayName())
                        .withStyle(s -> s.withColor(0xAA0000)), false);
                return InteractionResult.CONSUME;
            }
            if (!player.isCreative()) {
                player.giveExperienceLevels(-25);
                stack.shrink(1);
            }
            tame(player);
            playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        if (!isOwned()) return super.mobInteract(player, hand);

        // ── Tamed interactions ──

        if (DemonicContractItem.isSpellContract(stack) && player instanceof ServerPlayer serverPlayer) {
            return castSpellContract(serverPlayer, stack);
        }

        // Demon Heart → power up
        if (stack.is(Items.HEART_OF_THE_SEA) || stack.is(WitcheryItems.DEMON_HEART.get())) {
            if (isPowered()) {
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.alreadypowered",
                        getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
                return InteractionResult.CONSUME;
            }
            if (!player.isCreative()) stack.shrink(1);
            setPowered(true);
            playSound(SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f);
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.power",
                    getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.SUCCESS;
        }

        // Icy Needle → cancel power
        if (stack.is(WitcheryItems.ICY_NEEDLE.get())) {
            if (!isPowered()) return InteractionResult.CONSUME;
            if (!player.isCreative()) stack.shrink(1);
            setPowered(false);
            playSound(SoundEvents.PLAYER_HURT_FREEZE, 1.0f, 1.5f);
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.powerloss",
                    getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.SUCCESS;
        }

        // Shiny item → raise affection
        Integer boost = SHINIES.get(stack.getItem());
        if (boost != null && !stack.isDamaged()) {
            if (!player.isCreative()) stack.shrink(1);
            affection = Math.min(MAX_AFFECTION, affection + boost);
            playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.5f, 1.2f);

            long now = level().getGameTime();
            if (affection >= MAX_AFFECTION
                    && (now > lastGiftTime + GIFT_DELAY_TICKS || player.isCreative())
                    && random.nextInt(Math.max(1, 10 - Math.max(affection - MAX_AFFECTION, 0))) == 0) {
                giveGift(player, now);
            } else {
                player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.gift.like",
                        getDisplayName()).withStyle(s -> s.withColor(0xAA0000)), false);
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private InteractionResult castSpellContract(ServerPlayer player, ItemStack stack) {
        playSound(SoundEvents.BLAZE_AMBIENT, 0.5F, getVoicePitch());
        if (!DemonicContractItem.isBound(stack)) {
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.contract.unsigned", getDisplayName())
                    .withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.CONSUME;
        }
        if (isPowered()) {
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.spell.toomuchpower", getDisplayName())
                    .withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.CONSUME;
        }
        if (affection < MAX_AFFECTION) {
            player.displayClientMessage(Component.translatable("entity.witchery.flame_imp.spell.notliked", getDisplayName())
                    .withStyle(s -> s.withColor(0xAA0000)), false);
            return InteractionResult.CONSUME;
        }

        long now = level().getGameTime();
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
        affection = 0;
        spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), gift);
        playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.8f, 1.5f);
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
        return super.hurtServer(level, src, Math.min(amount, isPowered() ? 5.0f : 15.0f));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel _sl, net.minecraft.world.entity.Entity target) {
        float dmg = isPowered() ? 8.0f : 4.0f;
        target.hurt(damageSources().mobAttack(this), dmg);
        return true;
    }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    public boolean removeWhenFarAway(double dist) { return !isOwned(); }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) tag.putString("OwnerUUID", ownerUuid.toString());
        tag.putInt("Affection",     affection);
        tag.putInt("SecretsShared", secretsShared);
        tag.putLong("LastGiftTime", lastGiftTime);
        tag.putLong("PowerUpExpiry", powerUpExpiry);
        tag.putInt("HomeX", home.getX());
        tag.putInt("HomeY", home.getY());
        tag.putInt("HomeZ", home.getZ());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String ownerStr = tag.getStringOr("OwnerUUID", "");
        try {
            ownerUuid = ownerStr.isEmpty() ? null : java.util.UUID.fromString(ownerStr);
        } catch (IllegalArgumentException ignored) {
            ownerUuid = null;
        }
        affection     = tag.getIntOr("Affection", 0);
        secretsShared = tag.getIntOr("SecretsShared", 0);
        lastGiftTime  = tag.getLongOr("LastGiftTime", 0L);
        powerUpExpiry = tag.getLongOr("PowerUpExpiry", 0L);
        home = new BlockPos(tag.getIntOr("HomeX", 0), tag.getIntOr("HomeY", 0), tag.getIntOr("HomeZ", 0));
        if (powerUpExpiry > 0 && level().getGameTime() < powerUpExpiry) {
            entityData.set(DATA_POWERED, true);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // Small chance to drop Torment Scroll on death if not yet given
        if (secretsShared == 0 && random.nextInt(5) == 0) {
            spawnAtLocation((net.minecraft.server.level.ServerLevel) level(), new ItemStack(WitcheryItems.TORMENT_SCROLL.get()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String randomDemonName() {
        String a = DEMON_NAMES[random.nextInt(DEMON_NAMES.length)];
        String b = DEMON_NAMES[random.nextInt(DEMON_NAMES.length)];
        return random.nextInt(5) == 0 ? a : (a + " " + b);
    }

    public int getAffection() { return affection; }
}
