package com.emoniph.witchery.util;

import com.emoniph.witchery.entity.BabaYagaEntity;
import com.emoniph.witchery.entity.DeathEntity;
import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.EntEntity;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import com.emoniph.witchery.entity.GoblinMogEntity;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.entity.LeonardEntity;
import com.emoniph.witchery.entity.LilithEntity;
import com.emoniph.witchery.entity.LordOfTormentEntity;
import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.init.WitcheryMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;

/**
 * Port of {@code PotionEnslaved}'s static helpers (source
 * {@code com.emoniph.witchery.brewing.potions.PotionEnslaved}). Enslavement is recorded on the mob's
 * persistent data and the source-shaped {@code enslaved} effect: {@link #SOURCE_ENSLAVER_TAG}
 * (player name, source-faithful) plus {@link #PORT_ENSLAVER_UUID_TAG} (UUID, added by the port so a
 * renamed player keeps their thralls). The AI behaviours are driven by {@code EnslavedMobEvents}
 * instead of the source's {@code EntityAIEnslaverHurtByTarget} goal.
 */
public final class Enslaved {

    /** Source tag holding the enslaver's display name. Shared with the brews for interoperability. */
    public static final String SOURCE_ENSLAVER_TAG = "WITCEnslaverName";
    /** Port-only tag holding the enslaver's UUID, so a name change does not orphan the thrall. */
    public static final String PORT_ENSLAVER_UUID_TAG = "witchery_enslaver_uuid";
    /**
     * Marks a mob killed by a Witch-Hand creature-power sacrifice. The sacrifice already grants the
     * power, so the port's altar-kill acquisition (WitcheryPlayerEvents.onMobDeath) must skip it to
     * avoid double-granting charges and draining altar power.
     */
    public static final String SACRIFICED_TAG = "witchery_sacrificed";

    private Enslaved() {}

    /**
     * Bind {@code mob} to {@code player}. Mirrors source {@code setEnslaverForMob}: a no-op (returns
     * {@code false}) if the mob is already enslaved by this same player, otherwise stamps the tags,
     * applies the permanent but curable source effect, and clears its current target. Persistence is
     * deliberately left to the creation route: raised undead require it, while ordinary infernal
     * binding did not make arbitrary mobs persistent in 1.7.10.
     */
    public static boolean enslave(Mob mob, Player player) {
        if (mob == null || player == null || !canBeEnslaved(mob)) {
            return false;
        }
        if (isEnslavedBy(mob, player)) {
            return false;
        }
        mob.getPersistentData().putString(SOURCE_ENSLAVER_TAG, player.getName().getString());
        mob.getPersistentData().putString(PORT_ENSLAVER_UUID_TAG, player.getUUID().toString());
        mob.addEffect(new MobEffectInstance(WitcheryMobEffects.ENSLAVED, Integer.MAX_VALUE));
        mob.setTarget(null);
        return true;
    }

    /** True when {@code mob} is enslaved by {@code player} (by UUID, falling back to name). */
    public static boolean isEnslavedBy(Mob mob, Player player) {
        if (mob == null || player == null) {
            return false;
        }
        String uuid = mob.getPersistentData().getStringOr(PORT_ENSLAVER_UUID_TAG, "");
        if (!uuid.isEmpty()) {
            return uuid.equals(player.getUUID().toString());
        }
        return player.getName().getString().equals(mob.getPersistentData().getStringOr(SOURCE_ENSLAVER_TAG, ""));
    }

    /** True when {@code mob} is enslaved by anyone. */
    public static boolean isEnslaved(Mob mob) {
        return mob != null && !enslaverName(mob).isEmpty();
    }

    /** Source revenge AI runs only while the curable Enslaved effect remains active. */
    public static boolean hasActiveBond(Mob mob) {
        return isEnslaved(mob) && mob.hasEffect(WitcheryMobEffects.ENSLAVED);
    }

    /** The recorded enslaver display name, or empty. */
    public static String enslaverName(Mob mob) {
        return mob == null ? "" : mob.getPersistentData().getStringOr(SOURCE_ENSLAVER_TAG, "");
    }

    /** The recorded enslaver UUID string, or empty. */
    public static String enslaverUuid(Mob mob) {
        return mob == null ? "" : mob.getPersistentData().getStringOr(PORT_ENSLAVER_UUID_TAG, "");
    }

    /**
     * Mirrors source {@code canCreatureBeEnslaved}: any {@link Mob} that is not a boss, golem, witch,
     * or one of the mod's own infernal natives (demon, imp, ent). Witchery bosses are excluded too.
     */
    public static boolean canBeEnslaved(LivingEntity entity) {
        if (!(entity instanceof Mob)) {
            return false;
        }
        return !(entity instanceof EnderDragon)
                && !(entity instanceof WitherBoss)
                && !(entity instanceof AbstractGolem)
                && !(entity instanceof Witch)
                && !(entity instanceof DemonEntity)
                && !(entity instanceof FlameImpEntity)
                && !(entity instanceof EntEntity)
                && !isWitcheryBoss(entity);
    }

    /** Witchery's own bosses — source {@code IBossDisplayData} implementers, which cannot be enslaved. */
    private static boolean isWitcheryBoss(LivingEntity entity) {
        return entity instanceof BabaYagaEntity
                || entity instanceof DeathEntity
                || entity instanceof GoblinGulgEntity
                || entity instanceof GoblinMogEntity
                || entity instanceof HornedHuntsmanEntity
                || entity instanceof LeonardEntity
                || entity instanceof LilithEntity
                || entity instanceof LordOfTormentEntity
                || entity instanceof ReflectionEntity;
    }
}
