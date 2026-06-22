package com.emoniph.witchery.blocks.entity;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record LeechChestDrainResult(
        @Nullable UUID playerUuid,
        String playerName,
        List<String> missingPlayers,
        boolean onlyOwnTaglock) {
    public static LeechChestDrainResult success(UUID playerUuid, String playerName) {
        return new LeechChestDrainResult(playerUuid, playerName, List.of(), false);
    }

    public static LeechChestDrainResult missingPlayers(List<String> missingPlayers) {
        return new LeechChestDrainResult(null, "", List.copyOf(missingPlayers), false);
    }

    public static LeechChestDrainResult onlyOwn() {
        return new LeechChestDrainResult(null, "", List.of(), true);
    }

    public static LeechChestDrainResult none() {
        return new LeechChestDrainResult(null, "", List.of(), false);
    }

    public boolean hasPlayer() {
        return playerUuid != null && !playerName.isBlank();
    }
}
