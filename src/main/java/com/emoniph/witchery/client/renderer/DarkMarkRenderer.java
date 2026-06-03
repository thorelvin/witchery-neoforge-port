package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.DarkMarkModel;
import com.emoniph.witchery.entity.DarkMarkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class DarkMarkRenderer extends MobRenderer<DarkMarkEntity, LivingEntityRenderState, DarkMarkModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/darkmark.png");

    public DarkMarkRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DarkMarkModel(ctx.bakeLayer(DarkMarkModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        // Source renders the skull at 2× scale, lifted to sit within the 2×2 mark.
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.translate(0.0F, -1.0F, 0.0F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
