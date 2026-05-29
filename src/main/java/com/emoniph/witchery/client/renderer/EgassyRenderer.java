package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.EgassyEntity;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

public class EgassyRenderer extends MobRenderer<EgassyEntity, VillagerRenderState, VillagerModel> {

    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    public EgassyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new EgassyWartLayer(this, ctx));
    }

    @Override
    public VillagerRenderState createRenderState() { return new VillagerRenderState(); }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) { return TEXTURE; }
}
