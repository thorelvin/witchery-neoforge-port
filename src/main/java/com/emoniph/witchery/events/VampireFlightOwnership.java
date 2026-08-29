package com.emoniph.witchery.events;

final class VampireFlightOwnership {
    private VampireFlightOwnership() {
    }

    record Decision(boolean mayfly, boolean flying, boolean ownsGrant,
                    boolean mayflyBefore, boolean flyingBefore) {
    }

    static Decision reconcile(boolean batForm, boolean creativeOrSpectator,
                              boolean mayfly, boolean flying, boolean ownsGrant,
                              boolean mayflyBefore, boolean flyingBefore) {
        if (creativeOrSpectator) {
            return new Decision(mayfly, flying, false, false, false);
        }
        if (batForm) {
            if (!ownsGrant) {
                return new Decision(true, true, true, mayfly, flying);
            }
            return new Decision(true, true, true, mayflyBefore, flyingBefore);
        }
        if (!ownsGrant) {
            return new Decision(mayfly, flying, false, false, false);
        }
        return new Decision(mayflyBefore, flyingBefore && mayflyBefore, false, false, false);
    }
}
