package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ambient.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.BatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Renders the source vanilla-bat proxy over a player using the Vampire Bat power. */
public class VampireBatLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier BAT_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/bat.png");

    private final BatModel model;
    private final BatRenderState batState = new BatRenderState();

    public VampireBatLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                           EntityRendererProvider.Context context) {
        super(parent);
        this.model = new BatModel(context.bakeLayer(ModelLayers.BAT));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        Entity entity = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getEntity(state.id) : null;
        if (!(entity instanceof Player player) || !WitcheryAttachments.get(player).isVampireBatForm()) return;

        batState.ageInTicks = state.ageInTicks;
        batState.yRot = state.yRot;
        batState.xRot = state.xRot;
        batState.isResting = false;
        batState.flyAnimationState.startIfStopped(player.tickCount);
        batState.restAnimationState.stop();
        model.setupAnim(batState);

        poseStack.pushPose();
        collector.submitModelPart(model.root(), poseStack,
                RenderTypes.entityCutoutNoCull(BAT_TEXTURE), packedLight,
                OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }
}
