package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Hellhound — a fire-immune infernal hound. Port of EntityHellhound (1.7.10-0.24.1).
 *
 *  - 20 HP, 0.3 speed, 4.0 attack, +2 armour; fire-immune and sets its target ablaze.
 *  - Hunts sheep and nearby players; non-player, non-projectile attackers are resisted
 *    (incoming damage halved), mirroring the source's tough infernal hide.
 *  - Trails flame particles each client tick.
 *  - Can be "cured" into a vanilla Wolf: weaken it (Potion of Weakness) then feed it a
 *    plain Golden Apple — after a delay it converts, faster near iron bars / beds.
 *  - Drops Tongue of Dog, and rarely its own mounted Hellhound Head.
 *
 * Natural worldgen spawns are intentionally NOT registered (infernal lane): the Hellhound
 * is summon-/egg-spawnable, matching how the port surfaces other infernal mobs.
 */
public class HellhoundEntity extends Monster {

    /** &ge;0 while converting to a Wolf; counts down each server tick. */
    private int conversionTime = -1;

    public HellhoundEntity(EntityType<? extends HellhoundEntity> type, Level level) {
        super(type, level);
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH,     20.0)
                .add(Attributes.MOVEMENT_SPEED,  0.3)
                .add(Attributes.ATTACK_DAMAGE,   4.0)
                .add(Attributes.ARMOR,           2.0)
                .add(Attributes.FOLLOW_RANGE,   16.0);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Sheep.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true; // a hound of hell does not drown
    }

    // ── Tick — conversion countdown + flame trail ────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (conversionTime >= 0) {
                conversionTime -= conversionBoost();
                if (conversionTime <= 0) {
                    convertToWolf();
                }
            }
        } else if (tickCount % 2 == 0) {
            double w = getBbWidth();
            level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                    getX() - w * 0.35 + random.nextDouble() * w * 0.7,
                    getY() + 0.5 + random.nextDouble() * (getBbHeight() - 0.1),
                    getZ() - w * 0.35 + random.nextDouble() * w * 0.7,
                    0.0, 0.0, 0.0);
        }
    }

    // ── Damage — tough hide vs non-players; ignite on bite ────────────────────

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker != null && !(attacker instanceof Player) && !source.is(DamageTypeTags.IS_PROJECTILE)) {
            amount = (amount + 1.0F) / 2.0F;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean flag = super.doHurtTarget(level, target);
        if (flag && target instanceof LivingEntity) {
            int diff = level.getDifficulty().getId();
            if (diff > 0 && random.nextFloat() < diff * 0.1F) {
                target.igniteForSeconds(2 * diff);
            }
        }
        return flag;
    }

    // ── Curing into a Wolf (Golden Apple while weakened) ──────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.GOLDEN_APPLE) && hasEffect(MobEffects.WEAKNESS) && conversionTime < 0) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!level().isClientSide()) {
                startConversion(random.nextInt(1000) + 3600);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void startConversion(int time) {
        conversionTime = time;
        removeEffect(MobEffects.WEAKNESS);
        addEffect(new MobEffectInstance(MobEffects.STRENGTH, time,
                Math.max(0, level().getDifficulty().getId() - 1)));
    }

    /** Cure goes faster surrounded by iron bars or beds, mirroring the zombie-cure boost. */
    private int conversionBoost() {
        int boost = 1;
        if (random.nextFloat() < 0.01F) {
            BlockPos base = blockPosition();
            int counted = 0;
            for (BlockPos p : BlockPos.betweenClosed(base.offset(-4, -4, -4), base.offset(3, 3, 3))) {
                BlockState s = level().getBlockState(p);
                if (s.is(Blocks.IRON_BARS) || s.is(BlockTags.BEDS)) {
                    if (random.nextFloat() < 0.3F) {
                        boost++;
                    }
                    if (++counted >= 14) {
                        break;
                    }
                }
            }
        }
        return boost;
    }

    private void convertToWolf() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Wolf wolf = EntityType.WOLF.create(sl, EntitySpawnReason.CONVERSION);
        if (wolf == null) {
            discard();
            return;
        }
        wolf.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
        wolf.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), EntitySpawnReason.CONVERSION, null);
        wolf.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
        sl.levelEvent(null, 1017, blockPosition(), 0); // zombie-villager-cure finish effect
        discard();
    }

    public boolean isConverting() {
        return conversionTime >= 0;
    }

    // ── Drops ─────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        spawnAtLocation(level, new ItemStack(WitcheryItems.TONGUE_OF_DOG.get()));
        if (random.nextInt(12) == 0) {
            spawnAtLocation(level, new ItemStack(WitcheryItems.HELLHOUND_HEAD.get()));
        }
    }

    // ── Sounds (wolf voice, infernal and low) ────────────────────────────────

    // Nether-beast voice (no static wolf growl exists in 1.21.11 — it is variant-driven),
    // with canine footfalls.
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.HOGLIN_ANGRY; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.HOGLIN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.HOGLIN_DEATH; }
    @Override protected float getSoundVolume() { return 0.5F; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOLF_STEP, 0.15F, 1.0F);
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("ConversionTime", conversionTime);
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        conversionTime = in.getIntOr("ConversionTime", -1);
    }
}
