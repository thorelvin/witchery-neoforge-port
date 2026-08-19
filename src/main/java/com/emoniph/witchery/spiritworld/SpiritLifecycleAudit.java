package com.emoniph.witchery.spiritworld;

import java.util.ArrayList;
import java.util.List;

/** Pure consistency audit for the three distinct ways a player can touch the Spirit World. */
public final class SpiritLifecycleAudit {
    private SpiritLifecycleAudit() {
    }

    public record Snapshot(
            boolean inSpiritDimension,
            boolean dreaming,
            boolean hearthTraveller,
            boolean manifestedGhost,
            boolean hasReturnPosition,
            boolean hasReturnDimension,
            boolean hasMortalInventorySnapshot,
            int manifestationSeconds
    ) {
    }

    public static List<String> violations(Snapshot state) {
        List<String> violations = new ArrayList<>();

        if (state.dreaming() && state.hearthTraveller()) {
            violations.add("Dream travel and physical Hearth travel are both active.");
        }
        if (state.hearthTraveller() && !state.inSpiritDimension()) {
            violations.add("Physical Hearth traveller flag is set outside the Spirit World.");
        }
        if (state.dreaming() && !state.hasReturnPosition()) {
            violations.add("Dream travel has no stored waking position.");
        }
        if (state.dreaming() && !state.hasReturnDimension()) {
            violations.add("Dream travel has no stored waking dimension.");
        }
        if (state.dreaming() && !state.hasMortalInventorySnapshot()) {
            violations.add("Dream travel has no stored mortal inventory.");
        }
        if (state.manifestedGhost()) {
            if (!state.dreaming()) {
                violations.add("Manifestation is active without an underlying Spirit dream.");
            }
            if (state.inSpiritDimension()) {
                violations.add("Manifested ghost is still physically in the Spirit World.");
            }
            if (state.manifestationSeconds() <= 0) {
                violations.add("Manifested ghost has no remaining manifestation time.");
            }
        } else if (state.dreaming() && !state.inSpiritDimension()) {
            violations.add("Dream state is active outside the Spirit World without manifestation.");
        }
        if (state.inSpiritDimension() && !state.dreaming() && !state.hearthTraveller()) {
            violations.add("Player is in the Spirit World without dream or physical-travel state.");
        }

        return List.copyOf(violations);
    }
}
