package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class DeathEntityModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void sourcePartGraphZeroDepthBladeAndRobeScaleArePreserved() throws ReflectiveOperationException {
        ModelPart root = DeathEntityModel.createBodyLayer().bakeRoot();
        assertEquals(9, root.getAllParts().size());
        assertEquals(9, cubes(root).size());
        ModelPart scythe = root.getChild("right_arm").getChild("scythe");
        assertEquals(2, cubes(scythe).size());
        assertEquals(0.0F, cubes(scythe).get(1).maxZ - cubes(scythe).get(1).minZ, EPSILON);
        new DeathEntityModel(root);
        assertEquals(1.05F, root.getChild("robe").xScale, EPSILON);
        assertEquals(1.05F, root.getChild("robe").zScale, EPSILON);

        long mirrored = cubes(root).stream().filter(cube -> {
            try {
                return westFaceNormal(cube).x() > 0.0F;
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
            }
        }).count();
        assertEquals(2, mirrored);
    }

    @Test
    void sourceAttackScytheLegAndIdleArmPoseAreRestored() {
        ModelPart root = DeathEntityModel.createBodyLayer().bakeRoot();
        DeathEntityModel model = new DeathEntityModel(root);
        DeathEntityModel.DeathRenderState state = new DeathEntityModel.DeathRenderState();
        state.walkAnimationPos = 1.25F;
        state.walkAnimationSpeed = 0.4F;
        state.ageInTicks = 17.0F;
        state.yRot = 30.0F;
        state.xRot = -12.0F;
        state.attackProgress = 0.5F;
        model.setupAnim(state);

        ModelPart head = root.getChild("head");
        assertEquals(30.0F * Mth.DEG_TO_RAD, head.yRot, EPSILON);
        assertEquals(-12.0F * Mth.DEG_TO_RAD, head.xRot, EPSILON);
        ModelPart scythe = root.getChild("right_arm").getChild("scythe");
        assertEquals(-0.8F, scythe.x, EPSILON);
        assertEquals(8.1F, scythe.y, EPSILON);
        assertEquals(Mth.HALF_PI, scythe.xRot, EPSILON);
        assertEquals(0.1F, root.getChild("right_leg").z, EPSILON);
        assertEquals(0.1F, root.getChild("left_leg").z, EPSILON);
        assertEquals(12.0F, root.getChild("right_leg").y, EPSILON);
        assertEquals(0.0F, root.getChild("body").xRot, EPSILON);

        float bodyYaw = Mth.sin(Mth.sqrt(0.5F) * Mth.TWO_PI) * 0.2F;
        assertEquals(bodyYaw, root.getChild("body").yRot, EPSILON);
        assertEquals(-Mth.cos(bodyYaw) * 5.0F, root.getChild("right_arm").x, EPSILON);
        assertEquals(Mth.sin(bodyYaw) * 5.0F, root.getChild("right_arm").z, EPSILON);
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }

    private static Vector3f westFaceNormal(ModelPart.Cube cube) throws ReflectiveOperationException {
        Field polygonsField = ModelPart.Cube.class.getDeclaredField("polygons");
        polygonsField.setAccessible(true);
        Object[] polygons = (Object[]) polygonsField.get(cube);
        for (Field field : polygons[2].getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(polygons[2]);
            if (value instanceof Vector3f normal) return normal;
        }
        throw new NoSuchFieldException("Baked polygon has no Vector3f normal");
    }
}
