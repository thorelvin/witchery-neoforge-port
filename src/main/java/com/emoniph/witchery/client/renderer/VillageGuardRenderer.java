package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.VillageGuardModel;
import com.emoniph.witchery.entity.VillageGuardEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class VillageGuardRenderer
        extends HumanoidMobRenderer<VillageGuardEntity, VillageGuardRenderer.State, VillageGuardModel<VillageGuardRenderer.State>> {

    private static final Identifier[] SKINS = {
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/village_guard1.png"),
        Identifier.fromNamespaceAndPath("witchery", "textures/entity/village_guard2.png"),
    };

    public VillageGuardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillageGuardModel<>(ctx.bakeLayer(VillageGuardModel.LAYER_LOCATION)), 0.5F);

        // HEAD: custom partial model — head only, shifted up 0.5 so the deformed
        //        bottom lands at y=0 (not y=+0.5), eliminating the grey chin artifact.
        // CHEST/LEGS/FEET: standard zombie armor models (correct partial geometry).
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
    public void extractRenderState(VillageGuardEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.guardType = entity.getGuardType();
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return SKINS[Math.floorMod(state.guardType, SKINS.length)];
    }

    public static class State extends HumanoidRenderState {
        public int guardType;
    }
}
