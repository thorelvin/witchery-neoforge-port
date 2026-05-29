package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.VillageGuardModel;
import com.emoniph.witchery.entity.VampireEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Vampire renderer — uses the villager-proportion VillageGuardModel (same UV layout
 * as ModelVampire) with the original vampire.png skin texture.
 * HumanoidMobRenderer automatically adds ItemInHandLayer; we add HumanoidArmorLayer
 * so the vampire clothing (coat, legs, boots) renders correctly.
 */
public class VampireRenderer extends HumanoidMobRenderer<VampireEntity, VampireRenderer.State, VillageGuardModel<VampireRenderer.State>> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/vampire.png");

    public VampireRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillageGuardModel<>(ctx.bakeLayer(VillageGuardModel.LAYER_LOCATION)), 0.5F);

        // Head armor: custom 8×8×8 model (stops at y=-2, doesn't cover the face)
        VillageGuardModel<State> headArmor =
                new VillageGuardModel<>(ctx.getModelSet().bakeLayer(VillageGuardModel.ARMOR_HEAD_LAYER));

        ArmorModelSet<HumanoidModel<State>> zombieArmor =
                ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, ctx.getModelSet(),
                        root -> new HumanoidModel<>(root));

        @SuppressWarnings("unchecked")
        ArmorModelSet<HumanoidModel<State>> armorSet = new ArmorModelSet<>(
                headArmor,
                zombieArmor.chest(),
                zombieArmor.legs(),
                zombieArmor.feet()
        );
        this.addLayer(new HumanoidArmorLayer<>(this, armorSet, ctx.getEquipmentRenderer()));
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public Identifier getTextureLocation(State state) { return TEXTURE; }

    public static class State extends HumanoidRenderState {}
}
