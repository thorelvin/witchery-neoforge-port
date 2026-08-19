package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.init.WitcheryAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Draws the live player skin at the source's 51% manifested-ghost opacity.
 */
public class ManifestedGhostLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    static final float SOURCE_ALPHA = 0.51F;

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

        int tint = ARGB.colorFromFloat(SOURCE_ALPHA, 1.0F, 1.0F, 1.0F);
        collector.submitModel(this.getParentModel(), state, poseStack,
                RenderTypes.entityTranslucent(state.skin.body().texturePath()), light,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), tint, null,
                state.outlineColor, null);
    }
}
