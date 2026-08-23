package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.LeonardModel;
import com.emoniph.witchery.entity.LeonardEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renderer for the Shade of Leonard. Source {@code RenderLeonard} drew the demon at full
 * brightness ({@code getBrightness → 1.0}) so it glows in the dark like a shade; this mirrors
 * that by forcing the render state's light coordinates to full-bright.
 */
public class LeonardRenderer extends MobRenderer<LeonardEntity, LeonardModel.LeonardRenderState, LeonardModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/leonard.png");

    public LeonardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LeonardModel(ctx.bakeLayer(LeonardModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public LeonardModel.LeonardRenderState createRenderState() {
        return new LeonardModel.LeonardRenderState();
    }

    @Override
    public void extractRenderState(LeonardEntity entity, LeonardModel.LeonardRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.attackTimer = Math.max(0.0F, entity.getAttackTimer() - partialTick);
        state.attackProgress = entity.getAttackAnim(partialTick);
        state.riding = entity.isPassenger();
        state.lightCoords = LightTexture.FULL_BRIGHT; // source full-bright shade glow
    }

    @Override
    public Identifier getTextureLocation(LeonardModel.LeonardRenderState state) {
        return TEXTURE;
    }
}
