package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.BinkyEntity;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.resources.Identifier;

/** Binky uses the vanilla skeleton-horse visual base. */
public class BinkyRenderer extends MobRenderer<BinkyEntity, EquineRenderState, HorseModel> {

    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/horse/horse_skeleton.png");

    public BinkyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HorseModel(ctx.bakeLayer(ModelLayers.SKELETON_HORSE)), 0.75f);
    }

    @Override
    public EquineRenderState createRenderState() {
        return new EquineRenderState();
    }

    @Override
    public Identifier getTextureLocation(EquineRenderState state) {
        return TEXTURE;
    }
}
