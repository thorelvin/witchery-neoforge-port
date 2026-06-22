package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.CustomCloudsRenderer;
import org.joml.Matrix4f;

public final class SpiritWorldSpecialEffects {
    private static final float DREAM_ANGLE = 1.0F;
    private static final float NIGHTMARE_ANGLE = 0.5F;
    public static final CustomCloudsRenderer NO_CLOUDS = new CustomCloudsRenderer() {
        @Override
        public boolean renderClouds(LevelRenderState levelRenderState, Vec3 camPos, CloudStatus cloudStatus,
                                    int cloudColor, float cloudHeight, Matrix4f modelViewMatrix) {
            return true;
        }
    };

    private SpiritWorldSpecialEffects() {
    }

    public static int currentSpiritRgb() {
        SpiritColor color = currentSpiritColor();
        return rgb(color.red(), color.green(), color.blue());
    }

    public static float currentSunAngleRadians() {
        return (currentSpiritState().nightmareLike() ? NIGHTMARE_ANGLE : DREAM_ANGLE) * Mth.TWO_PI;
    }

    public static SpiritColor currentSpiritColor() {
        SpiritState state = currentSpiritState();

        float red;
        float green;
        float blue;
        if (state.demonic()) {
            red = 1.0F;
            green = 0.0F;
            blue = 0.0F;
        } else if (state.nightmare()) {
            red = 0.0F;
            green = 1.0F;
            blue = 0.0F;
        } else {
            red = 0.8F;
            green = 0.2F;
            blue = 0.6F;
        }

        float angle = sourceColorFactor(state.nightmareLike() ? NIGHTMARE_ANGLE : DREAM_ANGLE);
        return new SpiritColor(
                red * (angle * 0.94F + 0.06F),
                green * (angle * 0.94F + 0.06F),
                blue * (angle * 0.91F + 0.09F));
    }

    public static SpiritFog currentSpiritFog() {
        SpiritState state = currentSpiritState();
        if (state.demonic()) {
            return new SpiritFog(1.5F, 32.0F);
        }
        if (state.nightmare()) {
            return new SpiritFog(2.5F, 40.0F);
        }
        return new SpiritFog(3.0F, 52.0F);
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

    private static float sourceColorFactor(float celestialAngle) {
        return Mth.clamp(Mth.cos(celestialAngle * Mth.TWO_PI) * 2.0F + 0.5F, 0.0F, 1.0F);
    }

    private static int rgb(float red, float green, float blue) {
        int r = Mth.clamp((int)(red * 255.0F), 0, 255);
        int g = Mth.clamp((int)(green * 255.0F), 0, 255);
        int b = Mth.clamp((int)(blue * 255.0F), 0, 255);
        return r << 16 | g << 8 | b;
    }

    public record SpiritColor(float red, float green, float blue) {}

    public record SpiritFog(float nearPlane, float farPlane) {}

    private record SpiritState(boolean nightmare, boolean demonic) {
        private boolean nightmareLike() {
            return nightmare || demonic;
        }
    }
}
