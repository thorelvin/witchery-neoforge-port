package com.emoniph.witchery.entity;

import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.BrewCurse;
import com.emoniph.witchery.brewing.BrewWarningCurse;
import com.emoniph.witchery.brewing.BrewWarningText;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryDataComponents;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.item.BrewType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ThrownBrewEntity extends ThrowableProjectile implements ItemSupplier {
    public static final float SOURCE_THROW_PITCH_OFFSET = -20.0F;
    public static final float SOURCE_THROW_VELOCITY = 0.75F;
    public static final float SOURCE_THROW_INACCURACY = 1.0F;
    public static final double SOURCE_GRAVITY = 0.05D;
    private static final double SOURCE_THROW_ORIGIN_BACKSET = 0.16D;

    private static final EntityDataAccessor<Integer> BREW_TYPE =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_GAS =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> POWER =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.INT);
    /** Synced bottle tint so the flying bottle renders in the brew's colour client-side. */
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(ThrownBrewEntity.class, EntityDataSerializers.INT);

    private static final double SOURCE_CURSE_RADIUS = 2.0;
    private static final int SOURCE_CURSE_CHARGES = 1;
    private static final int DEFAULT_BREW_COLOR = 0x8A2BE2;

    /** Non-empty when carrying a composed custom brew (overrides the fixed BrewType path). */
    private BrewContents composed = BrewContents.EMPTY;
    private Brews.Dispersal composedDispersal = Brews.Dispersal.SPLASH;
    private BrewWarningText warningText;

    public ThrownBrewEntity(EntityType<? extends ThrownBrewEntity> type, Level level) {
        super(type, level);
    }

    public ThrownBrewEntity(Level level, LivingEntity owner, BrewType brewType, boolean gas, int power) {
        super(WitcheryEntities.THROWN_BREW.get(), sourceThrowX(owner), owner.getEyeY() - 0.1D,
                sourceThrowZ(owner), level);
        setOwner(owner);
        setBrewType(brewType.ordinal());
        setGas(gas);
        setPower(power);
        setColor(0xFFFFFF);
    }

    public ThrownBrewEntity(Level level, LivingEntity owner, BrewContents contents, boolean gas) {
        this(level, owner, contents, gas ? Brews.Dispersal.GAS : Brews.Dispersal.SPLASH);
    }

    public ThrownBrewEntity(Level level, LivingEntity owner, BrewContents contents, Brews.Dispersal dispersal) {
        this(level, owner, contents, dispersal, null);
    }

    public ThrownBrewEntity(Level level, LivingEntity owner, BrewContents contents, Brews.Dispersal dispersal,
                            BrewWarningText warningText) {
        super(WitcheryEntities.THROWN_BREW.get(), sourceThrowX(owner), owner.getEyeY() - 0.1D,
                sourceThrowZ(owner), level);
        setOwner(owner);
        this.composed = contents;
        this.composedDispersal = sanitizeComposedDispersal(dispersal);
        this.warningText = warningText == null || warningText.isEmpty() ? null : warningText;
        setBrewType(0);
        setGas(this.composedDispersal == Brews.Dispersal.GAS);
        setPower(contents.power());
        setColor(contents.color());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BREW_TYPE, 0);
        builder.define(IS_GAS, false);
        builder.define(POWER, 1);
        builder.define(COLOR, 0xFFFFFF);
    }

    public int getBrewTypeId() { return entityData.get(BREW_TYPE); }
    public void setBrewType(int id) { entityData.set(BREW_TYPE, id); }
    public boolean isGas() { return entityData.get(IS_GAS); }
    public void setGas(boolean gas) { entityData.set(IS_GAS, gas); }
    public int getPower() { return entityData.get(POWER); }
    public void setPower(int power) { entityData.set(POWER, Math.max(1, Math.min(5, power))); }
    public int getColor() { return entityData.get(COLOR); }
    public void setColor(int color) { entityData.set(COLOR, color); }

    public void shootFromSourceThrower(LivingEntity thrower) {
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), SOURCE_THROW_PITCH_OFFSET,
                SOURCE_THROW_VELOCITY, SOURCE_THROW_INACCURACY);
    }

    /** The bottle item the {@link net.minecraft.client.renderer.entity.ThrownItemRenderer} draws in flight. */
    @Override
    public ItemStack getItem() {
        Item item = !composed.isEmpty() && composedDispersal == Brews.Dispersal.LIQUID
                ? WitcheryItems.LIQUID_BREW_BOTTLE.get()
                : isGas() ? WitcheryItems.LINGERING_BREW_BOTTLE.get() : WitcheryItems.SPLASH_BREW_BOTTLE.get();
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(), List.of(getColor())));
        return stack;
    }

    public BrewType getBrewType() {
        int id = getBrewTypeId();
        BrewType[] vals = BrewType.values();
        return (id >= 0 && id < vals.length) ? vals[id] : BrewType.FOULNESS;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            int color = brewColor();
            level().addParticle(new DustParticleOptions(color, isGas() ? 1.0F : 0.75F),
                    getX(), getY() + 0.1, getZ(),
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1
            );
            if (isGas() && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.WITCH, getX(), getY() + 0.1, getZ(),
                        0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel serverLevel) {
            Vec3 pos = position();
            playBottleBreak(serverLevel, pos);
            if (!composed.isEmpty()) {
                // The true dispersal comes from the contents (e.g. a Zombie Head makes it TRIGGERED),
                // not the throwing bottle, so any composed brew disperses correctly however it was bottled.
                Brews.Dispersal effective = Brews.bottledDispersal(composed.items());
                if (effective == Brews.Dispersal.TRIGGERED) {
                    curseNearbyItems(serverLevel, pos);
                    if (result instanceof BlockHitResult blockHit) {
                        if (warningText != null) {
                            com.emoniph.witchery.blocks.CursedBlocks.tryWarn(serverLevel, blockHit, warningText);
                        }
                        if (composed.hasEffect()) {
                            com.emoniph.witchery.blocks.CursedBlocks.tryCurse(serverLevel, blockHit, composed, getOwner());
                        }
                    }
                } else if (effective == Brews.Dispersal.GAS || effective == Brews.Dispersal.LIQUID) {
                    Brews.placeMovingBrewFluid(serverLevel, Brews.sourceMovingBrewFluidPos(result),
                            composed.items(), effective, getOwner());
                } else {
                    LivingEntity directTarget = result instanceof EntityHitResult entityHit
                            && entityHit.getEntity() instanceof LivingEntity living ? living : null;
                    Brews.applyToArea(serverLevel, pos, composed.items(), Brews.Dispersal.SPLASH,
                            directTarget, getOwner(), result instanceof BlockHitResult blockHit ? blockHit : null);
                }
            } else {
                BrewType type = getBrewType();
                int tier = getPower();
                if (isGas()) {
                    type.applyGas(serverLevel, pos, tier, getOwner());
                } else {
                    type.applySplash(serverLevel, pos, tier, getOwner());
                }
            }
            spawnImpactParticles(serverLevel, pos, brewColor(), isGas());
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (!composed.isEmpty()) {
            tag.store("Composed", BrewContents.CODEC, composed);
            tag.putString("ComposedDispersal", composedDispersal.name());
        }
        if (warningText != null && !warningText.isEmpty()) {
            tag.store("BrewWarning", BrewWarningText.CODEC, warningText);
        }
        tag.putInt("BrewType", getBrewTypeId());
        tag.putBoolean("Gas", isGas());
        tag.putInt("Power", getPower());
        tag.putInt("Color", getColor());
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setBrewType(tag.getIntOr("BrewType", getBrewTypeId()));
        setGas(tag.getBooleanOr("Gas", isGas()));
        setPower(tag.getIntOr("Power", getPower()));
        setColor(tag.getIntOr("Color", getColor()));
        composed = tag.read("Composed", BrewContents.CODEC).orElse(BrewContents.EMPTY);
        warningText = tag.read("BrewWarning", BrewWarningText.CODEC).orElse(null);
        composedDispersal = readComposedDispersal(tag.getStringOr("ComposedDispersal",
                isGas() ? Brews.Dispersal.GAS.name() : Brews.Dispersal.SPLASH.name()));
        setGas(composedDispersal == Brews.Dispersal.GAS);
    }

    /** Source triggered dispersal: splash the brew over dropped items to "curse" them for a delayed release. */
    private void curseNearbyItems(ServerLevel level, Vec3 pos) {
        AABB area = new AABB(pos, pos).inflate(SOURCE_CURSE_RADIUS);
        boolean cursedAny = false;
        boolean applyBrewCurse = composed.hasEffect();
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = itemEntity.getItem();
            if (!itemEntity.isAlive() || stack.isEmpty()) {
                continue;
            }
            boolean needsBrewCurse = applyBrewCurse && !stack.has(WitcheryDataComponents.BREW_CURSE.get());
            boolean needsWarningCurse = warningText != null && !stack.has(WitcheryDataComponents.BREW_WARNING_CURSE.get());
            if (!needsBrewCurse && !needsWarningCurse) {
                continue;
            }
            ItemStack cursed = stack.split(1); // source curses a single item split off the stack
            if (needsBrewCurse) {
                cursed.set(WitcheryDataComponents.BREW_CURSE.get(),
                        new BrewCurse(composed, SOURCE_CURSE_CHARGES, 0L));
            }
            if (needsWarningCurse) {
                cursed.set(WitcheryDataComponents.BREW_WARNING_CURSE.get(),
                        new BrewWarningCurse(warningText, SOURCE_CURSE_CHARGES, 0L));
            }
            if (stack.isEmpty()) {
                itemEntity.setItem(cursed);
            } else {
                itemEntity.setItem(stack);
                ItemEntity cursedEntity = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(),
                        itemEntity.getZ(), cursed);
                cursedEntity.setDeltaMovement(itemEntity.getDeltaMovement());
                level.addFreshEntity(cursedEntity);
            }
            cursedAny = true;
        }
        if (cursedAny) {
            level.sendParticles(new DustParticleOptions(brewColor(), 1.0F),
                    pos.x, pos.y + 0.2, pos.z, 20, 0.6, 0.3, 0.6, 0.02);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                    pos.x, pos.y + 0.2, pos.z, 20, 0.6, 0.3, 0.6, 0.02);
        }
    }

    private int brewColor() {
        int color = !composed.isEmpty() ? composed.color() : getColor();
        return color == 0 ? DEFAULT_BREW_COLOR : color & 0xFFFFFF;
    }

    private void playBottleBreak(ServerLevel level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL,
                0.8F, 0.9F + random.nextFloat() * 0.2F);
    }

    @Override
    protected double getDefaultGravity() {
        return SOURCE_GRAVITY;
    }

    private static void spawnImpactParticles(ServerLevel level, Vec3 pos, int color, boolean gas) {
        int count = gas ? 18 : 28;
        level.sendParticles(new DustParticleOptions(color, gas ? 1.2F : 0.9F),
                pos.x, pos.y + 0.15D, pos.z, count, 0.7D, 0.35D, 0.7D, 0.02D);
        level.sendParticles(gas ? ParticleTypes.WITCH : ParticleTypes.SPLASH,
                pos.x, pos.y + 0.15D, pos.z, gas ? 12 : 18, 0.6D, 0.25D, 0.6D, 0.04D);
    }

    private static Brews.Dispersal readComposedDispersal(String name) {
        try {
            return sanitizeComposedDispersal(Brews.Dispersal.valueOf(name));
        } catch (IllegalArgumentException ex) {
            return Brews.Dispersal.SPLASH;
        }
    }

    private static Brews.Dispersal sanitizeComposedDispersal(Brews.Dispersal dispersal) {
        return dispersal == Brews.Dispersal.DRINK ? Brews.Dispersal.SPLASH : dispersal;
    }

    private static double sourceThrowX(LivingEntity owner) {
        return owner.getX() - Mth.cos(owner.getYRot() * Mth.DEG_TO_RAD) * SOURCE_THROW_ORIGIN_BACKSET;
    }

    private static double sourceThrowZ(LivingEntity owner) {
        return owner.getZ() - Mth.sin(owner.getYRot() * Mth.DEG_TO_RAD) * SOURCE_THROW_ORIGIN_BACKSET;
    }
}
