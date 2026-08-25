package com.emoniph.witchery.menu;

/** Source 1.7.10 coordinates shared by brew-storage menus, screens, and parity tests. */
public final class BrewStorageLayout {
    private BrewStorageLayout() {
    }

    public static final class Bag {
        public static final int WIDTH = 176;
        public static final int HEIGHT = 132;
        public static final int STORAGE_Y = 18;
        public static final int PLAYER_INVENTORY_Y = 49;
        public static final int HOTBAR_Y = 107;

        private Bag() {
        }

        public static int storageX(int slot) {
            return 16 + slot * 18;
        }
    }

    public static final class Urn {
        public static final int WIDTH = 176;
        public static final int HEIGHT = 184;
        public static final int PLAYER_INVENTORY_Y = 103;
        public static final int HOTBAR_Y = 161;
        private static final int[] SLOT_X = {80, 80, 103, 56};
        private static final int[] SLOT_Y = {22, 70, 46, 46};

        private Urn() {
        }

        public static int storageX(int slot) {
            return SLOT_X[slot];
        }

        public static int storageY(int slot) {
            return SLOT_Y[slot];
        }
    }
}
