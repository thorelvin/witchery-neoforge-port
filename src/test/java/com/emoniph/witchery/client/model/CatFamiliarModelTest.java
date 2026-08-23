package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

class CatFamiliarModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceOcelotGeometryUsesExactElevenCubesAndFrontLegPivots() {
        ModelPart root = CatFamiliarModel.createBodyLayer().bakeRoot();
        assertEquals(9, root.getAllParts().size());
        assertEquals(11, cubes(root).size());
        assertPose(root.getChild("head"), 0, 15, -9, 0, 0, 0);
        assertPose(root.getChild("body"), 0, 12, -10, Mth.HALF_PI, 0, 0);
        assertPose(root.getChild("leg_front_left"), 1.2F, 13.8F, -5, 0, 0, 0);
        assertPose(root.getChild("leg_front_right"), -1.2F, 13.8F, -5, 0, 0, 0);
        assertEquals(0.0F, cubes(root.getChild("head")).get(1).minY, EPSILON);
    }

    @Test
    void crouchingSprintingAndSittingMatchTheSourceModes() {
        ModelPart root = CatFamiliarModel.createBodyLayer().bakeRoot();
        CatFamiliarModel model = new CatFamiliarModel(root);
        CatFamiliarModel.CatFamiliarRenderState state = new CatFamiliarModel.CatFamiliarRenderState();
        state.walkAnimationPos = 1.7F;
        state.walkAnimationSpeed = 0.55F;

        state.crouching = true;
        model.setupAnim(state);
        assertEquals(13.0F, root.getChild("body").y, EPSILON);
        assertEquals(17.0F, root.getChild("head").y, EPSILON);
        assertEquals(16.0F, root.getChild("tail1").y, EPSILON);
        assertEquals(16.0F, root.getChild("tail2").y, EPSILON);
        assertEquals(16.0F, root.getChild("tail2").z, EPSILON);
        assertEquals(1.7278761F + 0.47123894F * Mth.cos(1.7F) * 0.55F,
                root.getChild("tail2").xRot, EPSILON);

        state.crouching = false;
        state.sprinting = true;
        model.setupAnim(state);
        assertEquals(root.getChild("tail1").y, root.getChild("tail2").y, EPSILON);
        assertEquals(16.0F, root.getChild("tail2").z, EPSILON);
        assertEquals(Mth.cos(1.7F * 0.6662F + 0.3F) * 0.55F,
                root.getChild("leg_back_right").xRot, EPSILON);
        assertEquals(Mth.cos(1.7F * 0.6662F + Mth.PI + 0.3F) * 0.55F,
                root.getChild("leg_front_left").xRot, EPSILON);
        assertEquals(1.7278761F + Mth.PI / 10.0F * Mth.cos(1.7F) * 0.55F,
                root.getChild("tail2").xRot, EPSILON);

        state.sprinting = false;
        state.sitting = true;
        model.setupAnim(state);
        assertPose(root.getChild("body"), 0, 8, -5, Mth.PI / 4.0F, 0, 0);
        assertEquals(11.7F, root.getChild("head").y, EPSILON);
        assertEquals(-8.0F, root.getChild("head").z, EPSILON);
        assertPose(root.getChild("leg_front_left"), 1.2F, 15.8F, -7,
                -Mth.PI / 20.0F, 0, 0);
        assertPose(root.getChild("leg_back_left"), 1.1F, 21, 1,
                -Mth.HALF_PI, 0, 0);
    }

    @Test
    void babyLayerRetainsSourceSplitHeadAndBodyTransformsDuringAnimation() {
        ModelPart root = CatFamiliarModel.createBabyLayer().bakeRoot();
        CatFamiliarModel model = new CatFamiliarModel(root);
        CatFamiliarModel.CatFamiliarRenderState state = new CatFamiliarModel.CatFamiliarRenderState();
        state.isBaby = true;
        state.sitting = true;
        model.setupAnim(state);

        ModelPart head = root.getChild("head");
        assertEquals(0.75F, head.xScale, EPSILON);
        assertEquals(16.275F, head.y, EPSILON);
        assertEquals(-3.0F, head.z, EPSILON);
        ModelPart body = root.getChild("body");
        assertEquals(0.5F, body.xScale, EPSILON);
        assertEquals(16.0F, body.y, EPSILON);
        assertEquals(-2.5F, body.z, EPSILON);
        assertEquals(19.9F, root.getChild("leg_front_left").y, EPSILON);
        assertEquals(-3.5F, root.getChild("leg_front_left").z, EPSILON);
        assertEquals(22.5F, root.getChild("leg_back_left").y, EPSILON);
        assertEquals(0.5F, root.getChild("leg_back_left").z, EPSILON);
    }

    private static void assertPose(ModelPart part, float x, float y, float z,
                                   float xRot, float yRot, float zRot) {
        assertEquals(x, part.x, EPSILON);
        assertEquals(y, part.y, EPSILON);
        assertEquals(z, part.z, EPSILON);
        assertEquals(xRot, part.xRot, EPSILON);
        assertEquals(yRot, part.yRot, EPSILON);
        assertEquals(zRot, part.zRot, EPSILON);
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }
}
