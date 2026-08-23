package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

class LegacyModelTransformsTest {
    private static final float EPSILON = 0.00001F;

    @Test
    void ordinaryRenderKeepsLegacyPivotUnitsAndZyxRotationOrder() {
        float x = 3.25F;
        float y = -7.5F;
        float z = 11.75F;
        float xRot = 0.43F;
        float yRot = -0.72F;
        float zRot = 1.11F;

        ModelPart part = emptyPart();
        part.loadPose(LegacyModelTransforms.render(x, y, z, xRot, yRot, zRot));

        PoseStack actual = new PoseStack();
        part.translateAndRotate(actual);

        PoseStack expected = new PoseStack();
        expected.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        expected.mulPose(new Quaternionf().rotateZ(zRot).rotateY(yRot).rotateX(xRot));
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void renderWithRotationConvertsLegacyYxzOrderToModernEulerAngles() {
        float x = -5.25F;
        float y = 4.5F;
        float z = -9.75F;
        float xRot = -1.16F;
        float yRot = 0.67F;
        float zRot = 0.91F;

        ModelPart part = emptyPart();
        part.loadPose(LegacyModelTransforms.renderWithRotation(x, y, z, xRot, yRot, zRot));

        PoseStack actual = new PoseStack();
        part.translateAndRotate(actual);

        PoseStack expected = new PoseStack();
        expected.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        expected.mulPose(new Quaternionf().rotateY(yRot).rotateX(xRot).rotateZ(zRot));
        assertMatrixEquals(expected.last().pose(), actual.last().pose());
    }

    @Test
    void dynamicRenderWithRotationUsesTheSameConversion() {
        ModelPart part = emptyPart();
        LegacyModelTransforms.setRenderWithRotation(part, 0.52F, -1.27F, 0.88F);

        Quaternionf expected = new Quaternionf().rotateY(-1.27F).rotateX(0.52F).rotateZ(0.88F);
        Quaternionf actual = new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
        assertEquals(1.0F, Math.abs(expected.dot(actual)), EPSILON);
    }

    private static ModelPart emptyPart() {
        return new ModelPart(List.of(), Map.of());
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
