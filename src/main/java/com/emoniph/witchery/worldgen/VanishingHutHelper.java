package com.emoniph.witchery.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Pure decision helpers for the Vanishing Witch Hut. Kept free of world/entity state so the
 * relocation gate, the "is a player looking at the hut" cone test, and the cross-dimension roll
 * can be unit-tested headlessly (see {@code VanishingHutHelperTest}).
 *
 * <p>The observation math mirrors {@code CreakingFamiliarHelper.isWatchedByNonOwner}: a player is
 * "looking at" a point when the dot product of their normalized look vector and the normalized
 * vector from their eye to the point exceeds {@link #LOOK_DOT_THRESHOLD} (a ~60 degree cone).
 */
public final class VanishingHutHelper {

    /** cos(~60 deg): look-cone half-angle for "is watching the hut". Matches the Creaking familiar. */
    public static final double LOOK_DOT_THRESHOLD = 0.5D;

    /** Range within which a player counts as a potential observer of the hut. */
    public static final double WATCH_RANGE = 32.0D;
    public static final double WATCH_RANGE_SQ = WATCH_RANGE * WATCH_RANGE;

    /** Default chance (0-1) that a relocation crosses dimensions instead of hopping in-world. */
    public static final double CROSS_DIMENSION_CHANCE = 0.25D;

    /** Minimum ticks between relocations so the hut can't strobe. */
    public static final long DEFAULT_MOVE_COOLDOWN_TICKS = 1200L; // 1 minute

    private VanishingHutHelper() {}

    /**
     * The Weeping-Angel trigger. The hut relocates only when nobody is inside, at least one player
     * who has been inside is now outside, no player is watching it, it is not held in place, and the
     * move cooldown has elapsed.
     */
    public static boolean shouldRelocate(boolean anyInside, boolean anyPrimedOutside, boolean watched,
                                         boolean heldInPlace, long sinceLastMoveTicks, long cooldownTicks) {
        if (anyInside) return false;
        if (!anyPrimedOutside) return false;
        if (watched) return false;
        if (heldInPlace) return false;
        return sinceLastMoveTicks >= cooldownTicks;
    }

    /**
     * True if a viewer at {@code eye} looking along {@code look} has {@code target} inside their
     * look cone. Degenerate case (target coincides with the eye) counts as looking.
     */
    public static boolean isLookingAt(Vec3 eye, Vec3 look, Vec3 target) {
        Vec3 toTarget = target.subtract(eye);
        double len = toTarget.length();
        if (len < 1.0E-4D) {
            return true;
        }
        Vec3 lookNorm = look.normalize();
        Vec3 toNorm = toTarget.scale(1.0D / len);
        return lookNorm.dot(toNorm) > LOOK_DOT_THRESHOLD;
    }

    /** Roll whether this relocation should cross between the Overworld and the Spirit World. */
    public static boolean pickCrossDimension(RandomSource random, double chance) {
        if (chance <= 0.0D) return false;
        if (chance >= 1.0D) return true;
        return random.nextDouble() < chance;
    }
}
