package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.HobgoblinEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class HobgoblinRenderer extends HumanoidMobRenderer<HobgoblinEntity, HobgoblinRenderer.HobgoblinRenderState, HumanoidModel<HobgoblinRenderer.HobgoblinRenderState>> {

    public static class HobgoblinRenderState extends HumanoidRenderState {
        public int entityId = 0;
    }

    private static final Identifier[] TEXTURES = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin3.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/hobgoblin4.png"),
    };

    public HobgoblinRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override public HobgoblinRenderState createRenderState() { return new HobgoblinRenderState(); }

    @Override
    public void extractRenderState(HobgoblinEntity entity, HobgoblinRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
    }

    @Override
    public Identifier getTextureLocation(HobgoblinRenderState state) {
        int idx = (int)(Math.abs(state.entityId) % TEXTURES.length);
        return TEXTURES[idx];
    }

    @Override
    public void submit(HobgoblinRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose(); ps.scale(0.75f, 0.75f, 0.75f);
        super.submit(state, ps, collector, camera);
        ps.popPose();
    }
}
