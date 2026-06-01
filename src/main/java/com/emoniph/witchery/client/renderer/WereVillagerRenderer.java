package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.WereVillagerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/** Source parity: EntityVillagerWere used vanilla RenderVillager, not a Witch Hunter biped. */
public class WereVillagerRenderer extends MobRenderer<WereVillagerEntity, VillagerRenderState, VillagerModel> {

    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/villager/villager.png");

    public WereVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerModel(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override public VillagerRenderState createRenderState() { return new VillagerRenderState(); }
    @Override public Identifier getTextureLocation(VillagerRenderState state) { return TEXTURE; }
}
