package com.emoniph.witchery.entity.familiar;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared familiar lookup helpers so gameplay systems use the same
 * "active familiar" rules across levels instead of only checking the
 * player's current loaded dimension.
 */
public final class FamiliarHelper {

    public static final double BOUND_FAMILIAR_MAX_HEALTH = 50.0;
    private static final String[] TOAD_NAMES = {
            "Casper", "Wart", "Langston", "Croaker", "Prince Charming", "Frog-n-stien",
            "Randolph", "Evileye", "Churchill", "Santa", "Dillinger", "Spuds"
    };
    private static final String[] CAT_NAMES = {
            "Pyewackett", "Salem", "Gobbolino", "Sabbath", "Norris", "Crookshanks",
            "Binx", "Voodoo", "Raven", "Simpkin", "Fishbone", "Kismet"
    };
    private static final String[] OWL_NAMES = {
            "Archimedes", "Dumbledornithologist", "Al Travis", "Baltimore", "Cornelius", "Hadwig",
            "Hoot", "Merlin", "Owl Capone", "Pigwidgeon", "Athena", "Albertine"
    };

    private FamiliarHelper() {
    }

    @Nullable
    public static TamableAnimal findBoundFamiliar(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Optional<UUID> familiarUuid = WitcheryAttachments.get(player).getFamiliarUUID();
        if (familiarUuid.isEmpty() || serverLevel.getServer() == null) {
            return null;
        }

        for (ServerLevel level : serverLevel.getServer().getAllLevels()) {
            Entity entity = level.getEntity(familiarUuid.get());
            if (entity instanceof TamableAnimal tameable && tameable.isOwnedBy(player)) {
                promoteBoundFamiliar(tameable);
                return tameable;
            }
        }

        return null;
    }

    public static int getActiveFamiliarType(Player player) {
        TamableAnimal familiar = findBoundFamiliar(player);
        if (familiar instanceof CatFamiliarEntity) {
            return 1;
        }
        if (familiar instanceof SpectralFamiliarEntity) {
            return 4;
        }
        if (familiar instanceof ToadFamiliarEntity) {
            return 2;
        }
        if (familiar instanceof OwlFamiliarEntity) {
            return 3;
        }
        return 0;
    }

    public static boolean hasActiveCatFamiliar(ServerPlayer player) {
        return getActiveFamiliarType(player) == 1;
    }

    public static boolean hasActiveOwlFamiliar(Player player) {
        return getActiveFamiliarType(player) == 3;
    }

    public static String describeFamiliar(@Nullable TamableAnimal familiar) {
        if (familiar instanceof CatFamiliarEntity) {
            return "cat";
        }
        if (familiar instanceof SpectralFamiliarEntity) {
            return "spectral familiar";
        }
        if (familiar instanceof ToadFamiliarEntity) {
            return "toad";
        }
        if (familiar instanceof OwlFamiliarEntity) {
            return "owl";
        }
        return "";
    }

    public static void promoteBoundFamiliar(@Nullable TamableAnimal familiar) {
        if (!(familiar instanceof FamiliarEntity)) {
            return;
        }

        AttributeInstance maxHealth = familiar.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        if (maxHealth.getBaseValue() < BOUND_FAMILIAR_MAX_HEALTH) {
            maxHealth.setBaseValue(BOUND_FAMILIAR_MAX_HEALTH);
            familiar.setHealth((float) BOUND_FAMILIAR_MAX_HEALTH);
        }
        familiar.setPersistenceRequired();
    }

    public static void ensureDefaultFamiliarName(@Nullable TamableAnimal familiar, RandomSource random) {
        if (familiar == null || familiar.hasCustomName()) {
            return;
        }

        String[] pool = switch (describeFamiliar(familiar)) {
            case "owl" -> OWL_NAMES;
            case "toad" -> TOAD_NAMES;
            case "cat" -> CAT_NAMES;
            default -> null;
        };
        if (pool == null || pool.length == 0) {
            return;
        }

        familiar.setCustomName(Component.literal(pool[random.nextInt(pool.length)]));
        familiar.setCustomNameVisible(false);
    }

    @Nullable
    public static ServerPlayer findBoundOwner(@Nullable TamableAnimal familiar) {
        if (familiar == null || !(familiar.level() instanceof ServerLevel serverLevel) || serverLevel.getServer() == null) {
            return null;
        }

        UUID familiarUuid = familiar.getUUID();
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (familiar.isOwnedBy(player)
                    && WitcheryAttachments.get(player).getFamiliarUUID().map(familiarUuid::equals).orElse(false)) {
                return player;
            }
        }

        return null;
    }

    public static void markFamiliarDismissed(ServerPlayer player, @Nullable TamableAnimal familiar, boolean removeEntity) {
        var data = WitcheryAttachments.get(player);
        if (familiar != null) {
            String familiarType = describeFamiliar(familiar);
            if (!familiarType.isBlank()) {
                data.setFamiliarType(familiarType);
            }
            if (familiar.hasCustomName()) {
                data.setFamiliarName(familiar.getCustomName().getString());
            }
            data.setFamiliarSitting(familiar.isOrderedToSit());
            data.setFamiliarNameVisible(familiar.isCustomNameVisible());
        }

        data.setFamiliarUUID(null);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));

        if (removeEntity && familiar != null && familiar.isAlive()) {
            familiar.discard();
        }
    }

    @Nullable
    public static TamableAnimal restoreBoundFamiliar(ServerPlayer player, ServerLevel level, double x, double y, double z) {
        String familiarType = WitcheryAttachments.get(player).getFamiliarType();
        if (familiarType == null || familiarType.isBlank()) {
            return null;
        }

        FamiliarEntity familiar = switch (familiarType.toLowerCase(Locale.ROOT)) {
            case "owl" -> WitcheryEntities.OWL.get().create(level, EntitySpawnReason.TRIGGERED);
            case "cat" -> WitcheryEntities.FAMILIAR_CAT.get().create(level, EntitySpawnReason.TRIGGERED);
            case "spectral", "spectral familiar" -> WitcheryEntities.SPECTRAL_FAMILIAR.get().create(level, EntitySpawnReason.TRIGGERED);
            case "toad" -> WitcheryEntities.TOAD.get().create(level, EntitySpawnReason.TRIGGERED);
            default -> null;
        };
        if (familiar == null) {
            return null;
        }

        familiar.tame(player);
        promoteBoundFamiliar(familiar);
        familiar.setOrderedToSit(WitcheryAttachments.get(player).isFamiliarSitting());
        String familiarName = WitcheryAttachments.get(player).getFamiliarName();
        if (!familiarName.isBlank()) {
            familiar.setCustomName(Component.literal(familiarName));
            familiar.setCustomNameVisible(WitcheryAttachments.get(player).isFamiliarNameVisible());
        } else {
            ensureDefaultFamiliarName(familiar, level.getRandom());
        }
        familiar.snapTo(x, y, z, player.getYRot(), 0.0F);
        level.addFreshEntity(familiar);
        WitcheryAttachments.get(player).setFamiliarUUID(familiar.getUUID());
        return familiar;
    }
}
