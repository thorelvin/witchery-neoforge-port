package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LeonardModel;
import com.emoniph.witchery.entity.LeonardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class LeonardRenderer extends MobRenderer<LeonardEntity, LivingEntityRenderState, LeonardModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/leonard.png");

    public LeonardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LeonardModel(ctx.bakeLayer(LeonardModel.LAYER_LOCATION)), 0.6F);
    }

    @Override
    public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) { return TEXTURE; }

    @Override
    protected net.minecraft.client.renderer.rendertype.RenderType getRenderType(
            LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
        return bodyVisible ? RenderTypes.entityTranslucent(TEXTURE)
                           : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(LivingEntityRenderState state) {
        return 0xBB808080;
    }

    /** Gentle whole-body hover bob so Leonard feels like a shade even while walking. */
    @Override
    protected void scale(LivingEntityRenderState state, PoseStack ps) {
        float hover = Mth.sin(state.ageInTicks * 0.05F) * 0.06F;
        ps.translate(0.0F, hover, 0.0F);
    }
}
