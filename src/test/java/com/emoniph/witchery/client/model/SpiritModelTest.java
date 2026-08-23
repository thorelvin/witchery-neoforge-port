package com.emoniph.witchery.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.junit.jupiter.api.Test;

class SpiritModelTest {
    private static final float EPSILON = 0.0001F;

    @Test
    void spiritAndLostSoulKeepTheFixedSourceFiveCubeBody() {
        assertSourceBody(SpiritModel.createBodyLayer().bakeRoot(), false);
        assertSourceBody(LostSoulModel.createBodyLayer().bakeRoot(), true);
    }

    private static void assertSourceBody(ModelPart root, boolean lostSoul) {
        assertEquals(7, root.getAllParts().size());
        assertEquals(5, cubes(root).size());
        ModelPart body = root.getChild("body");
        assertEquals(21.0F, body.y, EPSILON);
        LivingEntityRenderState state = new LivingEntityRenderState();
        state.ageInTicks = 50.0F;
        if (lostSoul) new LostSoulModel(root).setupAnim(state);
        else new SpiritModel(root).setupAnim(state);
        assertEquals(21.0F, body.y, EPSILON);
    }

    private static List<ModelPart.Cube> cubes(ModelPart part) {
        List<ModelPart.Cube> cubes = new ArrayList<>();
        part.visit(new PoseStack(), (pose, path, index, cube) -> cubes.add(cube));
        return cubes;
    }
}
