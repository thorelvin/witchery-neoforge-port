package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.SpectralWitchEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Renders the {@link SpectralWitchEntity}: the vanilla witch model on the coven-witch skins, but drawn
 * translucent (a ghostly blue-white tint with reduced alpha) and with the leg/arm walk animation frozen
 * so the conjured witch appears to float rather than walk. Mirrors the translucency approach used by
 * {@link LostSoulRenderer}.
 */
public class SpectralWitchRenderer
        extends MobRenderer<SpectralWitchEntity, SpectralWitchRenderer.SpectralWitchRenderState, WitchModel> {

    private static final Identifier[] SKINS = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch1.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch2.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch3.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch4.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/covenwitch5.png"),
    };

    /** Ghostly tint: alpha ~0x99 (60%) with a cold blue-white cast. */
    private static final int SPECTRAL_TINT = 0x99CCDDFF;

    public SpectralWitchRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new WitchModel(ctx.bakeLayer(ModelLayers.WITCH)), 0.5F);
    }

    @Override
    public SpectralWitchRenderState createRenderState() { return new SpectralWitchRenderState(); }

    @Override
    public void extractRenderState(SpectralWitchEntity entity, SpectralWitchRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = entity.getSkin();
        // Freeze the gait: a floating witch never moves her feet.
        state.walkAnimationPos = 0.0F;
        state.walkAnimationSpeed = 0.0F;
    }

    @Override
    public Identifier getTextureLocation(SpectralWitchRenderState state) {
        return SKINS[Math.floorMod(state.skin, SKINS.length)];
    }

    @Override
    protected RenderType getRenderType(SpectralWitchRenderState state, boolean bodyVisible,
                                       boolean translucent, boolean glowing) {
        Identifier texture = getTextureLocation(state);
        return bodyVisible ? RenderTypes.entityTranslucent(texture)
                           : super.getRenderType(state, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getModelTint(SpectralWitchRenderState state) {
        return SPECTRAL_TINT;
    }

    public static class SpectralWitchRenderState extends WitchRenderState {
        public int skin;
    }
}
