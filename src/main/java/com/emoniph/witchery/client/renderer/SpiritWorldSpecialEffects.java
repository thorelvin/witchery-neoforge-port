package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.spiritworld.SpiritWorldAtmosphere;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.CustomCloudsRenderer;
import org.joml.Matrix4f;

public final class SpiritWorldSpecialEffects {
    public static final CustomCloudsRenderer NO_CLOUDS = new CustomCloudsRenderer() {
        @Override
        public boolean renderClouds(LevelRenderState levelRenderState, Vec3 camPos, CloudStatus cloudStatus,
                                    int cloudColor, float cloudHeight, Matrix4f modelViewMatrix) {
            return true;
        }
    };

    private SpiritWorldSpecialEffects() {
    }

    public static float currentSunAngleRadians() {
        SpiritState state = currentSpiritState();
        return SpiritWorldAtmosphere.forState(state.nightmare(), state.demonic()).sunAngleRadians();
    }

    public static SpiritColor currentSpiritColor() {
        SpiritState state = currentSpiritState();
        SpiritWorldAtmosphere.Color color =
                SpiritWorldAtmosphere.forState(state.nightmare(), state.demonic()).color();
        return new SpiritColor(color.red(), color.green(), color.blue());
    }

    /**
     * True when the local client is currently seeing a nightmare (ordinary or demonic). Used to swap
     * Wispy Cotton to its disturbed appearance, matching the source rule that nightmare cotton yields
     * Disturbed Cotton.
     */
    public static boolean isNightmareLike() {
        return currentSpiritState().nightmareLike();
    }

    private static SpiritState currentSpiritState() {
        Player player = Minecraft.getInstance().player;
        boolean nightmare = false;
        boolean demonic = false;
        if (player != null) {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            nightmare = data.isNightmare();
            demonic = data.isDemonicNightmare();
        }
        return new SpiritState(nightmare, demonic);
    }

    public record SpiritColor(float red, float green, float blue) {}

    private record SpiritState(boolean nightmare, boolean demonic) {
        private boolean nightmareLike() {
            return nightmare || demonic;
        }
    }
}
