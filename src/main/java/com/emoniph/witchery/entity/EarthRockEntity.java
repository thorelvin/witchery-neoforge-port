package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The earth-rock the Overworld infusion hurls (source {@code EntityWitchProjectile} firing the rock
 * item). A plain thrown-item projectile rendered as a cobblestone, dealing impact damage.
 */
public class EarthRockEntity extends ThrowableProjectile implements ItemSupplier {

    private static final float DAMAGE = 5.0F;

    public EarthRockEntity(EntityType<? extends EarthRockEntity> type, Level level) {
        super(type, level);
    }

    /** Spawn a rock at {@code (x,y,z)} flying along {@code look}, owned by the caster. */
    public static EarthRockEntity hurl(Level level, LivingEntity owner, double x, double y, double z, Vec3 look) {
        EarthRockEntity rock = new EarthRockEntity(WitcheryEntities.EARTH_ROCK.get(), level);
        rock.setOwner(owner);
        rock.setPos(x, y, z);
        rock.shoot(look.x, look.y, look.z, 1.3F, 1.0F);
        return rock;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.COBBLESTONE);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel level && result.getEntity() instanceof LivingEntity living) {
            Entity owner = getOwner();
            DamageSource source = owner instanceof LivingEntity attacker
                    ? level.damageSources().mobProjectile(this, attacker)
                    : level.damageSources().thrown(this, owner);
            living.hurtServer(level, source, DAMAGE);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.05);
            level.playSound(null, blockPosition(), SoundType.STONE.getBreakSound(), SoundSource.PLAYERS, 0.8F, 0.9F);
            discard();
        }
    }
}
