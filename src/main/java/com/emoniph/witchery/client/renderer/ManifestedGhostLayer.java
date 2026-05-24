package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * First-pass manifested ghost presentation.
 * Reuses the live posed player model and draws a dedicated translucent
 * ghost overlay so manifested travelers stop looking fully mortal.
 */
public class ManifestedGhostLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/manifested_ghost.png");

    public ManifestedGhostLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float f1, float f2) {
        Entity entity = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getEntity(state.id) : null;
        if (!(entity instanceof Player player)) return;
        if (!WitcheryAttachments.get(player).isManifestedGhost()) return;

        poseStack.pushPose();
        poseStack.scale(1.01f, 1.01f, 1.01f);
        // Full-bright so the ghost is visible in darkness; translucent renders the
        // overlay with the ghost texture alpha
        collector.submitModelPart(this.getParentModel().root(), poseStack,
                RenderTypes.entityTranslucent(TEXTURE), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }
}
