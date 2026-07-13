package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.util.Enslaved;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;
import java.util.UUID;

/**
 * Shared source behavior from old {@code EntitySummonedUndead}.
 *
 * <p>The port keeps the concrete registry IDs ({@code spectre}, {@code banshee},
 * {@code poltergeist}) but restores their common source state: summoner,
 * obscured flag, optional suicide timer, undead type, and per-hit damage cap.</p>
 */
public abstract class SummonedUndeadEntity extends Monster {
    public static final String TAG_SUMMONER = "Summoner";
    public static final String TAG_OBSCURED = "Obscured";
    public static final String TAG_SUICIDE_IN = "SuicideIn";
    public static final String LEGACY_OWNER_UUID = "OwnerUUID";
    public static final int SOURCE_MAX_DAMAGE = 15;
    public static final float SOURCE_GRAVEYARD_DUST_HEALTH_GAIN = 2.0F;
    public static final float SOURCE_GRAVEYARD_DUST_MAX_HEALTH = 50.0F;

    private static final EntityDataAccessor<String> DATA_SUMMONER =
            SynchedEntityData.defineId(SummonedUndeadEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_OBSCURED =
            SynchedEntityData.defineId(SummonedUndeadEntity.class, EntityDataSerializers.BOOLEAN);

    private int timeToLive = -1;
    private UUID summonerUuid;

    protected SummonedUndeadEntity(EntityType<? extends SummonedUndeadEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SUMMONER, "");
        builder.define(DATA_OBSCURED, false);
    }

    public void setSummoner(Player player) {
        if (player == null) {
            setSummonerName("");
            summonerUuid = null;
            return;
        }
        setSummonerName(player.getName().getString());
        summonerUuid = player.getUUID();
        Enslaved.enslave(this, player);
    }

    public void setSummonerName(String summonerName) {
        String value = summonerName == null ? "" : summonerName;
        if (!value.isEmpty()) {
            setPersistenceRequired();
        }
        entityData.set(DATA_SUMMONER, value);
    }

    public String getSummonerName() {
        return entityData.get(DATA_SUMMONER);
    }

    public Optional<UUID> getSummonerUuid() {
        return Optional.ofNullable(summonerUuid);
    }

    public Player getSummoner() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (summonerUuid != null) {
            Player player = serverLevel.getServer().getPlayerList().getPlayer(summonerUuid);
            if (player != null) {
                return player;
            }
        }
        String name = getSummonerName();
        return name.isEmpty() ? null : serverLevel.getServer().getPlayerList().getPlayerByName(name);
    }

    public boolean isSummoner(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (summonerUuid != null && summonerUuid.equals(player.getUUID())) {
            return true;
        }
        return Enslaved.isEnslavedBy(this, player)
                || player.getName().getString().equals(getSummonerName());
    }

    public void setTimeToLive(int ticks) {
        timeToLive = ticks < 0 ? -1 : ticks;
    }

    public boolean isTemp() {
        return timeToLive != -1;
    }

    public boolean isObscured() {
        return entityData.get(DATA_OBSCURED);
    }

    public void setObscured(boolean obscured) {
        entityData.set(DATA_OBSCURED, obscured);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isSummoner(target) && super.canAttack(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (getTarget() != null && isSummoner(getTarget())) {
            setTarget(null);
        }
        if (timeToLive != -1 && (--timeToLive <= 0 || getTarget() == null || !getTarget().isAlive())) {
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        getX(), getY() + 0.5D, getZ(), 8, 0.35D, 0.35D, 0.35D, 0.0D);
            }
            discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return super.hurtServer(level, source, sourceDamageCap(amount));
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(TAG_SUMMONER, getSummonerName());
        tag.putBoolean(TAG_OBSCURED, isObscured());
        tag.putInt(TAG_SUICIDE_IN, timeToLive);
        if (summonerUuid != null) {
            tag.putString(LEGACY_OWNER_UUID, summonerUuid.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setSummonerName(tag.getStringOr(TAG_SUMMONER, ""));
        setObscured(tag.getBooleanOr(TAG_OBSCURED, false));
        timeToLive = tag.getIntOr(TAG_SUICIDE_IN, -1);
        String uuid = tag.getStringOr(LEGACY_OWNER_UUID, "");
        if (uuid.isEmpty()) {
            uuid = tag.getStringOr(Enslaved.PORT_ENSLAVER_UUID_TAG, "");
        }
        try {
            summonerUuid = uuid.isEmpty() ? null : UUID.fromString(uuid);
        } catch (IllegalArgumentException ignored) {
            summonerUuid = null;
        }
        if (summonerUuid != null) {
            setPersistenceRequired();
        }
    }

    public boolean applyGraveyardDust(Player player, ItemStack stack) {
        if (player == null || stack.isEmpty() || !stack.is(WitcheryItems.GRAVEYARD_DUST.get()) || !isSummoner(player)) {
            return false;
        }
        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return false;
        }
        float currentMax = (float) maxHealth.getBaseValue();
        float healedMax = sourceGraveyardDustHealthAfter(currentMax);
        if (healedMax <= currentMax) {
            return false;
        }
        maxHealth.setBaseValue(healedMax);
        setHealth(healedMax);
        setPersistenceRequired();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, 0xFFFFFF, 1.0F),
                    getX(), getY() + 0.7D, getZ(), 18, 0.35D, 0.45D, 0.35D, 0.02D);
            serverLevel.playSound(null, blockPosition(), SoundEvents.SILVERFISH_DEATH,
                    SoundSource.PLAYERS, 0.7F, 1.0F);
        }
        return true;
    }

    public static float sourceDamageCap(float amount) {
        return Math.min(amount, SOURCE_MAX_DAMAGE);
    }

    public static float sourceGraveyardDustHealthAfter(float currentMaxHealth) {
        float healed = currentMaxHealth + SOURCE_GRAVEYARD_DUST_HEALTH_GAIN;
        return healed <= SOURCE_GRAVEYARD_DUST_MAX_HEALTH ? healed : currentMaxHealth;
    }
}
