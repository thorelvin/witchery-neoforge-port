package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BroomModel;
import com.emoniph.witchery.entity.BroomEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class BroomRenderer extends MobRenderer<BroomEntity, BroomRenderer.BroomRenderState, BroomModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/broom.png");

    public BroomRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BroomModel(ctx.bakeLayer(BroomModel.LAYER_LOCATION)), 0.3F);
    }

    @Override public BroomRenderState createRenderState() { return new BroomRenderState(); }

    @Override
    public void extractRenderState(BroomEntity entity, BroomRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.enchanted = entity.isEnchanted();
        if (state.enchanted) {
            state.lightCoords = LightTexture.FULL_BRIGHT;
        }
    }

    @Override public Identifier getTextureLocation(BroomRenderState state) { return TEXTURE; }

    public static class BroomRenderState extends LivingEntityRenderState {
        public boolean enchanted;
    }
}
