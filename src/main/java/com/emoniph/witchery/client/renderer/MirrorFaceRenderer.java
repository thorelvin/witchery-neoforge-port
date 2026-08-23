package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.MirrorFaceModel;
import com.emoniph.witchery.entity.MirrorFaceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class MirrorFaceRenderer
        extends MobRenderer<MirrorFaceEntity, LivingEntityRenderState, MirrorFaceModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.0F;
    static final float SOURCE_MODEL_SCALE = 0.75F;
    static final float SOURCE_PRE_BASE_TRANSLATE_Y = -0.0820625F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/mirrorface.png");
    static final int SOURCE_ALPHA_WHITE = 0xCCFFFFFF;

    public MirrorFaceRenderer(EntityRendererProvider.Context context) {
        super(context, new MirrorFaceModel(context.bakeLayer(MirrorFaceModel.LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(LivingEntityRenderState state, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        return bodyVisible
                ? RenderTypes.entityTranslucent(TEXTURE)
                : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(LivingEntityRenderState state) {
        return SOURCE_ALPHA_WHITE;
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        // Rebase source T(-1.5078125) * S(0.75) * T(0.4) around modern T(-1.501).
        poseStack.translate(0.0F, SOURCE_PRE_BASE_TRANSLATE_Y, 0.0F);
        poseStack.scale(SOURCE_MODEL_SCALE, SOURCE_MODEL_SCALE, SOURCE_MODEL_SCALE);
    }
}
