package com.emoniph.witchery.client.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.emoniph.witchery.client.model.CaneSwordModel;
import com.emoniph.witchery.client.model.HandBowModel;
import com.emoniph.witchery.item.HandBowItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class LegacyHeldItemRendererTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void sharedFirstPersonAdapterReproducesTheLegacyForgePipeline() {
        PoseStack actual = new PoseStack();
        LegacyHeldItemTransforms.beginFirstPerson(actual);
        LegacyHeldItemTransforms.finishFirstPerson(actual);

        PoseStack expected = new PoseStack();
        expected.translate(0.94F, 0.13F, 0.0F);
        expected.mulPose(Axis.YP.rotationDegrees(45.0F));
        expected.scale(0.4F, 0.4F, 0.4F);
        applyLegacyEquippedItemPose(expected);
        expected.scale(1.7F, 1.7F, 1.7F);

        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void sharedThirdPersonAdapterReplacesTheModernTailForBothPhysicalHands() {
        for (boolean rightHand : new boolean[]{true, false}) {
            float side = rightHand ? 1.0F : -1.0F;
            PoseStack actual = new PoseStack();
            applyModernThirdPersonTail(actual, rightHand);
            LegacyHeldItemTransforms.beginThirdPerson(actual, rightHand);

            PoseStack expected = new PoseStack();
            expected.translate(side * -0.93F, -0.64684546F, 0.01979959F);
            applyLegacyThirdPersonTail(expected, rightHand);
            applyLegacyEquippedItemPose(expected);

            assertMatrixEquals(expected.last().pose(), actual.last().pose());
        }
    }

    @Test
    void brewBagSourcePosesMatchFirstAndThirdPersonBranches() {
        assertPose(
                pose -> WitcherySpecialItemRenderers.BrewBagHeldSpecialRenderer.applySourcePose(pose, true),
                pose -> {
                    pose.mulPose(Axis.XP.rotationDegrees(100.0F));
                    pose.mulPose(Axis.YP.rotationDegrees(-41.0F));
                    pose.mulPose(Axis.ZP.rotationDegrees(-81.0F));
                    pose.translate(0.075F, 0.32F, -0.75F);
                    pose.mulPose(Axis.YP.rotationDegrees(10.0F));
                    pose.translate(0.3F, 0.05F, 0.0F);
                    pose.scale(1.5F, 1.5F, 1.5F);
                });
        assertPose(
                pose -> WitcherySpecialItemRenderers.BrewBagHeldSpecialRenderer.applySourcePose(pose, false),
                pose -> {
                    pose.mulPose(Axis.XP.rotationDegrees(100.0F));
                    pose.mulPose(Axis.YP.rotationDegrees(-41.0F));
                    pose.mulPose(Axis.ZP.rotationDegrees(-81.0F));
                    pose.translate(0.075F, 0.32F, -0.75F);
                    pose.scale(1.3F, 1.3F, 1.3F);
                });
    }

    @Test
    void caneSwordSourcePosesCoverEveryPlayerViewAndDeploymentBranch() {
        for (boolean firstPerson : new boolean[]{true, false}) {
            for (boolean deployed : new boolean[]{true, false}) {
                assertPose(
                        pose -> WitcherySpecialItemRenderers.CaneSwordHeldSpecialRenderer
                                .applySourcePose(pose, firstPerson, deployed),
                        pose -> applyExpectedCanePose(pose, firstPerson, deployed));
            }
        }
    }

    @Test
    void caneSwordPerPartScalingOccursAroundTheSourcePivot() {
        ModelPart blade3 = new CaneSwordModel(CaneSwordModel.createLayer().bakeRoot()).blade3();
        PoseStack actual = new PoseStack();
        WitcherySpecialItemRenderers.CaneSwordHeldSpecialRenderer.applyPartScale(
                actual, blade3, 0.0F, 3.15F, 0.0F, 0.2F, 4.3F, 0.2F);

        PoseStack expected = new PoseStack();
        expected.translate(-5.8F / 16.0F, 11.0F / 16.0F, -1.6F / 16.0F);
        expected.translate(0.0F, 3.15F, 0.0F);
        expected.scale(0.2F, 4.3F, 0.2F);
        expected.translate(5.8F / 16.0F, -11.0F / 16.0F, 1.6F / 16.0F);

        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void handBowSourcePoseAndBoltMappingMatchEverySourceBranch() {
        assertPose(
                pose -> WitcherySpecialItemRenderers.HandBowHeldSpecialRenderer.applySourcePose(pose, true, false),
                pose -> applyExpectedHandBowPose(pose, true, false));
        assertPose(
                pose -> WitcherySpecialItemRenderers.HandBowHeldSpecialRenderer.applySourcePose(pose, true, true),
                pose -> applyExpectedHandBowPose(pose, true, true));
        assertPose(
                pose -> WitcherySpecialItemRenderers.HandBowHeldSpecialRenderer.applySourcePose(pose, false, true),
                pose -> applyExpectedHandBowPose(pose, false, true));

        assertEquals(HandBowModel.Bolt.NONE,
                WitcherySpecialItemRenderers.HandBowHeldSpecialRenderer.modelBolt(null));
        for (HandBowItem.LoadedBolt bolt : HandBowItem.LoadedBolt.values()) {
            assertEquals(HandBowModel.Bolt.valueOf(bolt.name()),
                    WitcherySpecialItemRenderers.HandBowHeldSpecialRenderer.modelBolt(bolt));
        }
    }

    @Test
    void huntsmanSpearUsesPhysicalHandednessAndEverySourceSwingBranch() {
        assertEquals(true, WitcherySpecialItemRenderers.HuntsmanSpearHeldSpecialRenderer.displayUsesMainHand(
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, HumanoidArm.RIGHT));
        assertEquals(false, WitcherySpecialItemRenderers.HuntsmanSpearHeldSpecialRenderer.displayUsesMainHand(
                ItemDisplayContext.FIRST_PERSON_LEFT_HAND, HumanoidArm.RIGHT));
        assertEquals(false, WitcherySpecialItemRenderers.HuntsmanSpearHeldSpecialRenderer.displayUsesMainHand(
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.LEFT));
        assertEquals(true, WitcherySpecialItemRenderers.HuntsmanSpearHeldSpecialRenderer.displayUsesMainHand(
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT));

        assertSpearPose(true, 0.0F);
        assertSpearPose(true, 0.25F);
        assertSpearPose(true, 0.75F);
        assertSpearPose(false, 0.2F);
        assertSpearPose(false, 0.4F);
    }

    @Test
    void mysticBranchSourcePoseMatchesTheOriginalRenderer() {
        assertPose(WitcherySpecialItemRenderers.MysticBranchHeldSpecialRenderer::applySourcePose, pose -> {
            pose.mulPose(Axis.XP.rotationDegrees(100.0F));
            pose.mulPose(Axis.YP.rotationDegrees(-51.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-81.0F));
            pose.translate(0.045F, 0.37F, -0.75F);
        });
    }

    private static void assertSpearPose(boolean firstPerson, float swingProgress) {
        assertPose(
                pose -> WitcherySpecialItemRenderers.HuntsmanSpearHeldSpecialRenderer
                        .applySourcePose(pose, firstPerson, swingProgress),
                pose -> {
                    pose.mulPose(Axis.XP.rotationDegrees(100.0F));
                    pose.mulPose(Axis.YP.rotationDegrees(-51.0F));
                    pose.mulPose(Axis.ZP.rotationDegrees(-81.0F));
                    pose.translate(0.1F, 0.12F, -0.72F);
                    if (swingProgress <= 0.0F) return;
                    if (firstPerson) {
                        pose.translate(0.0F, 0.15F, 0.0F);
                        float curve = swingProgress <= 0.5F ? 1.0F - swingProgress : swingProgress;
                        pose.mulPose(Axis.XP.rotationDegrees(50.0F * curve));
                    } else {
                        if (swingProgress > 0.3F) pose.mulPose(Axis.XP.rotationDegrees(50.0F));
                        pose.mulPose(Axis.YP.rotationDegrees(-20.0F));
                    }
                });
    }

    private static void applyExpectedCanePose(PoseStack pose, boolean firstPerson, boolean deployed) {
        pose.mulPose(Axis.XP.rotationDegrees(100.0F));
        pose.mulPose(Axis.YP.rotationDegrees(19.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-86.0F));
        pose.translate(0.475F, 0.12F, 0.0F);
        if (firstPerson) {
            if (deployed) {
                pose.translate(0.4F, 0.3F, -0.2F);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(1.0F));
                pose.mulPose(Axis.YP.rotationDegrees(20.0F));
            } else {
                pose.mulPose(Axis.XP.rotationDegrees(70.0F));
                pose.translate(0.0F, -0.5F, -0.9F);
            }
        } else if (deployed) {
            pose.mulPose(Axis.XP.rotationDegrees(80.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(10.0F));
            pose.translate(0.0F, -0.3F, -0.5F);
        }
    }

    private static void applyExpectedHandBowPose(PoseStack pose, boolean firstPerson, boolean pulling) {
        pose.mulPose(Axis.XP.rotationDegrees(100.0F));
        pose.mulPose(Axis.YP.rotationDegrees(-51.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(-81.0F));
        pose.translate(0.095F, -0.01F, -0.72F);
        if (!firstPerson) return;
        if (pulling) {
            pose.mulPose(Axis.YP.rotationDegrees(-25.0F));
            pose.translate(0.0F, 0.1F, 0.0F);
        }
        pose.translate(0.2F, 0.1F, 0.0F);
    }

    private static void applyModernThirdPersonTail(PoseStack poseStack, boolean rightHand) {
        float side = rightHand ? 1.0F : -1.0F;
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(side / 16.0F, 0.125F, -0.625F);
    }

    private static void applyLegacyThirdPersonTail(PoseStack poseStack, boolean rightHand) {
        float side = rightHand ? 1.0F : -1.0F;
        poseStack.translate(-side / 16.0F, 0.625F, 0.0625F);
        poseStack.scale(0.625F, -0.625F, 0.625F);
        poseStack.mulPose(Axis.XN.rotationDegrees(100.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
    }

    private static void applyLegacyEquippedItemPose(PoseStack poseStack) {
        poseStack.translate(0.0F, -0.3F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
        poseStack.translate(-0.9375F, -0.0625F, 0.0F);
    }

    private static void assertPose(Consumer<PoseStack> actualTransform, Consumer<PoseStack> expectedTransform) {
        PoseStack actual = new PoseStack();
        PoseStack expected = new PoseStack();
        actualTransform.accept(actual);
        expectedTransform.accept(expected);
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                assertEquals(expected.get(column, row), actual.get(column, row), EPSILON,
                        "matrix[" + column + "][" + row + "]");
            }
        }
    }
}
