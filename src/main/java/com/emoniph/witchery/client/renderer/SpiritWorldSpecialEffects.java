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

    /** Keeps the Spirit provider's source celestial angle local to the client viewing that dimension. */
    public static void pinClientDayTime() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || !minecraft.level.dimension().equals(com.emoniph.witchery.init.WitcheryDimensions.SPIRIT_WORLD)) {
            return;
        }
        SpiritState state = currentSpiritState();
        long dayTime = SpiritWorldAtmosphere.forState(state.nightmare(), state.demonic()).clientDayTime();
        minecraft.level.getLevelData().setDayTime(dayTime);
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
        Minecraft minecraft = Minecraft.getInstance();
        boolean nightmare = false;
        if (minecraft.level == null) {
            return new SpiritState(false, false);
        }
        for (Player player : minecraft.level.players()) {
            WitcheryPlayerData data = WitcheryAttachments.get(player);
            if (data.isDemonicNightmare()) {
                return new SpiritState(true, true);
            }
            nightmare |= data.isNightmare();
        }
        return new SpiritState(nightmare, false);
    }

    public record SpiritColor(float red, float green, float blue) {}

    private record SpiritState(boolean nightmare, boolean demonic) {
        private boolean nightmareLike() {
            return nightmare || demonic;
        }
    }
}
