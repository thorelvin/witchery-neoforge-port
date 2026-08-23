package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.ParasyticLouseModel;
import com.emoniph.witchery.entity.ParasyticLouseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class ParasyticLouseRenderer extends MobRenderer<ParasyticLouseEntity, LivingEntityRenderState, ParasyticLouseModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.3F;
    static final float SOURCE_DEATH_FLIP_DEGREES = 180.0F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/louse.png");

    public ParasyticLouseRenderer(EntityRendererProvider.Context context) {
        super(context, new ParasyticLouseModel(context.bakeLayer(ParasyticLouseModel.LAYER_LOCATION)),
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
    protected float getFlipDegrees() {
        return SOURCE_DEATH_FLIP_DEGREES;
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }
}
