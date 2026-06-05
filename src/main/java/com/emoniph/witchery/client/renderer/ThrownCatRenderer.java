package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.CatFamiliarModel;
import com.emoniph.witchery.entity.ThrownCatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class ThrownCatRenderer extends MobRenderer<ThrownCatEntity, CatFamiliarModel.CatFamiliarRenderState, CatFamiliarModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/blackcat.png");

    public ThrownCatRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CatFamiliarModel(ctx.bakeLayer(CatFamiliarModel.LAYER_LOCATION)), 0.25F);
    }

    @Override
    public CatFamiliarModel.CatFamiliarRenderState createRenderState() {
        return new CatFamiliarModel.CatFamiliarRenderState();
    }

    @Override
    public Identifier getTextureLocation(CatFamiliarModel.CatFamiliarRenderState state) {
        return TEXTURE;
    }

    @Override
    protected void scale(CatFamiliarModel.CatFamiliarRenderState state, PoseStack poseStack) {
        poseStack.scale(0.65F, 0.65F, 0.65F);
    }
}
