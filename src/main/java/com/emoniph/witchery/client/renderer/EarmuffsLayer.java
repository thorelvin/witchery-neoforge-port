package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.EarmuffsModel;
import com.emoniph.witchery.item.EarmuffsItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class EarmuffsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/earmuffs.png");

    private final EarmuffsModel model;

    public EarmuffsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                         EntityRendererProvider.Context context) {
        super(parent);
        this.model = new EarmuffsModel(context.bakeLayer(EarmuffsModel.LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float f1, float f2) {
        Entity entity = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getEntity(state.id) : null;
        if (!(entity instanceof Player player) || !EarmuffsItem.isWorn(player)) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        collector.submitModelPart(model.root(), poseStack,
                RenderTypes.entityCutoutNoCull(TEXTURE), light, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }
}
