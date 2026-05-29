package com.emoniph.witchery.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class EgassyWartLayer extends RenderLayer<VillagerRenderState, VillagerModel> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("witchery", "egassy_wart"), "main");

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/egassy_wart.png");

    private final ModelPart wart;

    public EgassyWartLayer(RenderLayerParent<VillagerRenderState, VillagerModel> parent,
                           EntityRendererProvider.Context context) {
        super(parent);
        this.wart = context.bakeLayer(LAYER_LOCATION).getChild("wart");
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Wart cube sitting just in front of the villager nose tip (nose tip is at z=-6 in nose-local space)
        root.addOrReplaceChild("wart",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 1.0F, -6.5F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 8, 8);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       VillagerRenderState state, float f1, float f2) {
        ModelPart head = getParentModel().getHead();
        ModelPart nose = head.getChild("nose");
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        nose.translateAndRotate(poseStack);
        collector.submitModelPart(wart, poseStack,
                RenderTypes.entityCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }
}
