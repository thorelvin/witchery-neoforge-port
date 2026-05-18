package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.SpiritModel;
import com.emoniph.witchery.entity.SpiritEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;

public class SpiritRenderer extends MobRenderer<SpiritEntity, LivingEntityRenderState, SpiritModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/spirit.png");
    public SpiritRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpiritModel(ctx.bakeLayer(SpiritModel.LAYER_LOCATION)), 0.25f);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }
    @Override
    public void submit(LivingEntityRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        ps.pushPose(); ps.scale(0.5f, 0.5f, 0.5f);
        super.submit(state, ps, collector, camera);
        ps.popPose();
    }
}
