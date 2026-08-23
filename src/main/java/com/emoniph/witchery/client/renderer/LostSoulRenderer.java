package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LostSoulModel;
import com.emoniph.witchery.entity.LostSoulEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class LostSoulRenderer extends MobRenderer<LostSoulEntity, LostSoulRenderer.LostSoulRenderState, LostSoulModel> {

    static final float SOURCE_SHADOW_RADIUS = 0.0F;
    static final float SOURCE_PRE_BASE_TRANSLATE_Y = -0.1073125F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/spirit.png");

    private static final int TINT_RED = 0x99FF0000;
    private static final int TINT_GREEN = 0x9900FF00;
    private static final int TINT_BLUE = 0x990000FF;

    public static class LostSoulRenderState extends LivingEntityRenderState {
        public int soulType = LostSoulEntity.GREEN;
    }

    public LostSoulRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LostSoulModel(ctx.bakeLayer(LostSoulModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }

    @Override
    public LostSoulRenderState createRenderState() { return new LostSoulRenderState(); }

    @Override
    public void extractRenderState(LostSoulEntity entity, LostSoulRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.soulType = entity.getSoulType();
    }

    @Override
    public Identifier getTextureLocation(LostSoulRenderState state) { return TEXTURE; }

    @Override
    protected net.minecraft.client.renderer.rendertype.RenderType getRenderType(
            LostSoulRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
        return bodyVisible ? RenderTypes.entityTranslucent(TEXTURE)
                           : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(LostSoulRenderState state) {
        return sourceTint(state.soulType);
    }

    static int sourceTint(int soulType) {
        return switch (soulType) {
            case 1 -> TINT_GREEN;
            case 2 -> TINT_BLUE;
            default -> TINT_RED;
        };
    }

    @Override
    protected void scale(LostSoulRenderState state, PoseStack ps) {
        ps.translate(0.0F, SOURCE_PRE_BASE_TRANSLATE_Y, 0.0F);
        ps.scale(0.5F, 0.5F, 0.5F);
    }
}
