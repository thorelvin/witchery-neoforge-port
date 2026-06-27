package com.emoniph.witchery.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class WoodenScannerClientEnumParams {
    public static final EnumProxy<HumanoidModel.ArmPose> WOODEN_SCANNER_HOLD = new EnumProxy<>(
            HumanoidModel.ArmPose.class,
            true,
            true,
            (IArmPoseTransformer) WoodenScannerClientEnumParams::poseScannerArms
    );

    private WoodenScannerClientEnumParams() {
    }

    private static void poseScannerArms(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        float focus = Mth.clamp(state.ticksUsingItem / 20.0F, 0.0F, 1.0F);
        focus = (focus * focus + focus * 2.0F) / 3.0F;
        float headPitch = state.xRot * ((float) Math.PI / 180.0F);
        float armPitch = -1.10F - focus * 0.12F + headPitch * 0.30F;
        poseArm(model.rightArm, armPitch, -0.52F, 0.56F);
        poseArm(model.leftArm, armPitch, 0.52F, -0.56F);
    }

    private static void poseArm(ModelPart arm, float xRot, float yRot, float zRot) {
        arm.xRot = xRot;
        arm.yRot = yRot;
        arm.zRot = zRot;
    }
}
