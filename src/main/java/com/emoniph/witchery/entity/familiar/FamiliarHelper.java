package com.emoniph.witchery.entity.familiar;

import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared familiar lookup helpers so gameplay systems use the same
 * "active familiar" rules across levels instead of only checking the
 * player's current loaded dimension.
 */
public final class FamiliarHelper {

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
}
