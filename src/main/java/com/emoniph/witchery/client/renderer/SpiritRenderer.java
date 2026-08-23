package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.SpiritModel;
import com.emoniph.witchery.entity.SpiritEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class SpiritRenderer extends MobRenderer<SpiritEntity, LivingEntityRenderState, SpiritModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.0F;
    static final float SOURCE_PRE_BASE_TRANSLATE_Y = -0.1073125F;
    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/spirit.png");
    static final int SOURCE_ALPHA_WHITE = 0x99FFFFFF;

    public SpiritRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpiritModel(ctx.bakeLayer(SpiritModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }
    @Override public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }
    @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state) { return TEXTURE; }

    @Override
    protected RenderType getRenderType(LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
        return bodyVisible ? RenderTypes.entityTranslucent(TEXTURE) : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(LivingEntityRenderState state) {
        return SOURCE_ALPHA_WHITE;
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack ps) {
        ps.translate(0.0F, SOURCE_PRE_BASE_TRANSLATE_Y, 0.0F);
        ps.scale(0.5F, 0.5F, 0.5F);
    }
}
