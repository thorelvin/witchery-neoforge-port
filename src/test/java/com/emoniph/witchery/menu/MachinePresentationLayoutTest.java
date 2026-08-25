package com.emoniph.witchery.menu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class MachinePresentationLayoutTest {
    @Test
    void witchesOvenUsesSourceSlotsAndProgressAtlas() {
        assertArrayEquals(new int[]{56, 17, 56, 53, 118, 21, 118, 53, 83, 53}, new int[]{
                MachinePresentationLayout.WitchesOven.INPUT_X,
                MachinePresentationLayout.WitchesOven.INPUT_Y,
                MachinePresentationLayout.WitchesOven.FUEL_X,
                MachinePresentationLayout.WitchesOven.FUEL_Y,
                MachinePresentationLayout.WitchesOven.OUTPUT_X,
                MachinePresentationLayout.WitchesOven.OUTPUT_Y,
                MachinePresentationLayout.WitchesOven.BYPRODUCT_X,
                MachinePresentationLayout.WitchesOven.BYPRODUCT_Y,
                MachinePresentationLayout.WitchesOven.JARS_X,
                MachinePresentationLayout.WitchesOven.JARS_Y
        });
        assertArrayEquals(new int[]{79, 20, 176, 14, 24, 16}, new int[]{
                MachinePresentationLayout.WitchesOven.PROGRESS_X,
                MachinePresentationLayout.WitchesOven.PROGRESS_Y,
                MachinePresentationLayout.WitchesOven.PROGRESS_U,
                MachinePresentationLayout.WitchesOven.PROGRESS_V,
                MachinePresentationLayout.WitchesOven.PROGRESS_WIDTH,
                MachinePresentationLayout.WitchesOven.PROGRESS_HEIGHT
        });
    }

    @Test
    void distilleryUsesSourceSlotsAndAnimatedAtlas() {
        assertArrayEquals(new int[]{48, 16, 48, 34, 48, 54, 110, 16, 128, 16, 110, 34, 128, 34},
                new int[]{
                        MachinePresentationLayout.Distillery.INPUT_X,
                        MachinePresentationLayout.Distillery.INPUT_1_Y,
                        MachinePresentationLayout.Distillery.INPUT_X,
                        MachinePresentationLayout.Distillery.INPUT_2_Y,
                        MachinePresentationLayout.Distillery.JARS_X,
                        MachinePresentationLayout.Distillery.JARS_Y,
                        MachinePresentationLayout.Distillery.OUTPUT_1_X,
                        MachinePresentationLayout.Distillery.OUTPUT_1_Y,
                        MachinePresentationLayout.Distillery.OUTPUT_2_X,
                        MachinePresentationLayout.Distillery.OUTPUT_1_Y,
                        MachinePresentationLayout.Distillery.OUTPUT_1_X,
                        MachinePresentationLayout.Distillery.OUTPUT_2_Y,
                        MachinePresentationLayout.Distillery.OUTPUT_2_X,
                        MachinePresentationLayout.Distillery.OUTPUT_2_Y
                });
        assertArrayEquals(new int[]{35, 58, 197, 0, 9, 9}, new int[]{
                MachinePresentationLayout.Distillery.NO_POWER_X,
                MachinePresentationLayout.Distillery.NO_POWER_Y,
                MachinePresentationLayout.Distillery.NO_POWER_U,
                MachinePresentationLayout.Distillery.NO_POWER_V,
                MachinePresentationLayout.Distillery.NO_POWER_WIDTH,
                MachinePresentationLayout.Distillery.NO_POWER_HEIGHT
        });
        assertArrayEquals(new int[]{68, 14, 176, 29, 38, 35}, new int[]{
                MachinePresentationLayout.Distillery.PROGRESS_X,
                MachinePresentationLayout.Distillery.PROGRESS_Y,
                MachinePresentationLayout.Distillery.PROGRESS_U,
                MachinePresentationLayout.Distillery.PROGRESS_V,
                MachinePresentationLayout.Distillery.PROGRESS_WIDTH,
                MachinePresentationLayout.Distillery.PROGRESS_HEIGHT
        });
        assertArrayEquals(new int[]{33, 20, 185, 29, 12, 29}, new int[]{
                MachinePresentationLayout.Distillery.BUBBLES_X,
                MachinePresentationLayout.Distillery.BUBBLES_Y,
                MachinePresentationLayout.Distillery.BUBBLES_U,
                MachinePresentationLayout.Distillery.BUBBLES_V,
                MachinePresentationLayout.Distillery.BUBBLES_WIDTH,
                MachinePresentationLayout.Distillery.BUBBLES_HEIGHT
        });
    }

    @Test
    void spinningWheelUsesSourceSlotsAndProgressAtlas() {
        assertArrayEquals(new int[]{56, 20, 56, 53, 118, 21, 74, 53, 92, 53}, new int[]{
                MachinePresentationLayout.SpinningWheel.FIBRE_X,
                MachinePresentationLayout.SpinningWheel.FIBRE_Y,
                MachinePresentationLayout.SpinningWheel.MODIFIER_1_X,
                MachinePresentationLayout.SpinningWheel.MODIFIER_1_Y,
                MachinePresentationLayout.SpinningWheel.OUTPUT_X,
                MachinePresentationLayout.SpinningWheel.OUTPUT_Y,
                MachinePresentationLayout.SpinningWheel.MODIFIER_2_X,
                MachinePresentationLayout.SpinningWheel.MODIFIER_2_Y,
                MachinePresentationLayout.SpinningWheel.MODIFIER_3_X,
                MachinePresentationLayout.SpinningWheel.MODIFIER_3_Y
        });
        assertArrayEquals(new int[]{35, 58, 197, 0, 9, 9}, new int[]{
                MachinePresentationLayout.SpinningWheel.NO_POWER_X,
                MachinePresentationLayout.SpinningWheel.NO_POWER_Y,
                MachinePresentationLayout.SpinningWheel.NO_POWER_U,
                MachinePresentationLayout.SpinningWheel.NO_POWER_V,
                MachinePresentationLayout.SpinningWheel.NO_POWER_WIDTH,
                MachinePresentationLayout.SpinningWheel.NO_POWER_HEIGHT
        });
        assertArrayEquals(new int[]{79, 20, 176, 14, 24, 16}, new int[]{
                MachinePresentationLayout.SpinningWheel.PROGRESS_X,
                MachinePresentationLayout.SpinningWheel.PROGRESS_Y,
                MachinePresentationLayout.SpinningWheel.PROGRESS_U,
                MachinePresentationLayout.SpinningWheel.PROGRESS_V,
                MachinePresentationLayout.SpinningWheel.PROGRESS_WIDTH,
                MachinePresentationLayout.SpinningWheel.PROGRESS_HEIGHT
        });
    }
}
