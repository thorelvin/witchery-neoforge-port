package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class EntModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourceSixteenPartCubeGraphGeometryAndMirrorTimingArePreserved()
            throws ReflectiveOperationException {
        ModelPart root = EntModel.createBodyLayer().bakeRoot();
        assertEquals(17, root.getAllParts().size());
        assertEquals(16, cubes(root).size());
        assertPose(root.getChild("arm_left"), 8, -4, 0, 0, 0, 0);
        assertPose(root.getChild("body"), 0, 20, 0, 0, 0, 0);
        assertPose(root.getChild("leg8"), 4, 20, -1, 0, 0.5759587F, 0.1919862F);
        assertPose(root.getChild("leg1"), -4, 20, 2, 0, 0.5759587F, -0.1919862F);
        assertPose(root.getChild("leaves_base"), -30, -42, -30, 0, 0, 0);
        assertPose(root.getChild("leaves_top_inner"), -14, -57, -14, 0, 0, 0);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(16, mirrored);
    }

    @Test
    void innerLeavesUseTheirDistinctSourceTextureCoordinateSpace()
            throws ReflectiveOperationException {
        ModelPart actualRoot = EntModel.createBodyLayer().bakeRoot();
        ModelPart.Cube actualBase = cubes(actualRoot.getChild("leaves_base_inner")).getFirst();
        ModelPart.Cube actualTop = cubes(actualRoot.getChild("leaves_top_inner")).getFirst();

        MeshDefinition expectedMesh = new MeshDefinition();
        expectedMesh.getRoot().addOrReplaceChild("base",
                CubeListBuilder.create().mirror().texOffs(24, 59).addBox(0, 0, 0, 56, 14, 56),
                PartPose.ZERO);
        expectedMesh.getRoot().addOrReplaceChild("top",
                CubeListBuilder.create().mirror().texOffs(108, 14).addBox(0, 0, 0, 28, 14, 28),
                PartPose.ZERO);
        ModelPart expectedRoot = LayerDefinition.create(expectedMesh, 64, 32).bakeRoot();
        assertArrayEquals(uvBounds(cubes(expectedRoot.getChild("base")).getFirst()), uvBounds(actualBase), EPSILON);
        assertArrayEquals(uvBounds(cubes(expectedRoot.getChild("top")).getFirst()), uvBounds(actualTop), EPSILON);
    }

    @Test
    void attackIdleLegAndScreamingStatesMatchSource() {
        ModelPart root = EntModel.createBodyLayer().bakeRoot();
        EntModel model = new EntModel(root);
        EntModel.EntRenderState state = new EntModel.EntRenderState();
        state.walkAnimationPos = 3.2F;
        state.walkAnimationSpeed = 0.7F;
        model.setupAnim(state);
        assertFalse(root.getChild("face").visible);
        float triangle = (Math.abs(3.2F % 13.0F - 6.5F) - 3.25F) / 3.25F;
        assertEquals((-0.2F + 0.1F * triangle) * 0.7F - 0.1F,
                root.getChild("arm_right").zRot, EPSILON);
        assertEquals(-Math.PI / 4.0F
                        + Math.abs(Math.sin(3.2F * 0.6662F) * 0.4F) * 0.7F,
                root.getChild("leg1").zRot, EPSILON);

        state.screaming = true;
        state.attackTimer = 8.5F;
        model.setupAnim(state);
        assertTrue(root.getChild("face").visible);
        float attackTriangle = (Math.abs(8.5F % 10.0F - 5.0F) - 2.5F) / 2.5F;
        assertEquals(3.0F - 1.3F * attackTriangle, root.getChild("arm_right").xRot, EPSILON);
        assertEquals(2.5F - 1.2F * attackTriangle, root.getChild("arm_left").xRot, EPSILON);
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

    private static Vector3f westFaceNormal(ModelPart.Cube cube) throws ReflectiveOperationException {
        for (Field field : polygons(cube)[2].getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(polygons(cube)[2]);
            if (value instanceof Vector3f normal) {
                return normal;
            }
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }

    private static float[] uvBounds(ModelPart.Cube cube) throws ReflectiveOperationException {
        float minU = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (Object polygon : polygons(cube)) {
            for (Field field : polygon.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(polygon);
                if (!(value instanceof Object[] vertices)) {
                    continue;
                }
                for (Object vertex : vertices) {
                    Field u = vertex.getClass().getDeclaredField("u");
                    Field v = vertex.getClass().getDeclaredField("v");
                    u.setAccessible(true);
                    v.setAccessible(true);
                    float vertexU = u.getFloat(vertex);
                    float vertexV = v.getFloat(vertex);
                    minU = Math.min(minU, vertexU);
                    maxU = Math.max(maxU, vertexU);
                    minV = Math.min(minV, vertexV);
                    maxV = Math.max(maxV, vertexV);
                }
            }
        }
        return new float[]{minU, maxU, minV, maxV};
    }

    private static Object[] polygons(ModelPart.Cube cube) throws ReflectiveOperationException {
        Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
        polygonsField.setAccessible(true);
        return (Object[]) polygonsField.get(cube);
    }
}
