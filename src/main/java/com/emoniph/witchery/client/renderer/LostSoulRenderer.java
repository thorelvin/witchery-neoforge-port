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

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/lost_soul.png");

    // ARGB tints per soul type
    private static final int TINT_GREEN = 0xCC40FF40;
    private static final int TINT_RED   = 0xCCFF4040;
    private static final int TINT_BLACK = 0xCC303030;

    public static class LostSoulRenderState extends LivingEntityRenderState {
        public int soulType = LostSoulEntity.GREEN;
    }

    public LostSoulRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LostSoulModel(ctx.bakeLayer(LostSoulModel.LAYER_LOCATION)), 0.0F);
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
        return switch (state.soulType) {
            case LostSoulEntity.RED   -> TINT_RED;
            case LostSoulEntity.BLACK -> TINT_BLACK;
            default                   -> TINT_GREEN;
        };
    }

    @Override
    protected void scale(LostSoulRenderState state, PoseStack ps) {
        ps.scale(0.5F, 0.5F, 0.5F);
    }
}
