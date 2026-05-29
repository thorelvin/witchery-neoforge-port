package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.BoltEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

public class BoltRenderer extends ArrowRenderer<BoltEntity, ArrowRenderState> {

    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public BoltRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ArrowRenderState createRenderState() { return new ArrowRenderState(); }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) { return TEXTURE; }
}
