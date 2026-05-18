package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.FlyingMonkeyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class FlyingMonkeyRenderer extends HumanoidMobRenderer<FlyingMonkeyEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/flying_monkey.png");

    public FlyingMonkeyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.4F);
    }

    @Override public HumanoidRenderState createRenderState() { return new HumanoidRenderState(); }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.HumanoidRenderState state) { return TEXTURE; }

    @Override
    public void submit(HumanoidRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose(); ps.scale(0.65f, 0.65f, 0.65f);
        super.submit(state, ps, collector, camera);
        ps.popPose();
    }
}
