package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.EntModel;
import com.emoniph.witchery.entity.EntEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class EntRenderer extends MobRenderer<EntEntity, EntModel.EntRenderState, EntModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/ent.png");

    public EntRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new EntModel(ctx.bakeLayer(EntModel.LAYER_LOCATION)), 0.8F);
    }

    @Override public EntModel.EntRenderState createRenderState() { return new EntModel.EntRenderState(); }

    @Override
    public void extractRenderState(EntEntity entity, EntModel.EntRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        int attackTimer = entity.getAttackTimer();
        state.attackTimer = attackTimer > 0 ? Math.max(0.0f, attackTimer - partialTick) : 0.0f;
        state.screaming = entity.isScreaming();
    }

    @Override public Identifier getTextureLocation(EntModel.EntRenderState state) { return TEXTURE; }
}
