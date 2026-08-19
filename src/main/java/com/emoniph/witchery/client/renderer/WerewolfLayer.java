package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.WerewolfModel;
import com.emoniph.witchery.client.model.WerewolfWolfModel;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfForm;
import com.emoniph.witchery.init.WitcheryAttachments;
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

/**
 * Renders the wolfman body over the player whenever they are a werewolf.
 */
public class WerewolfLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier WOLFMAN_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/wolfman.png");
    private static final Identifier WOLF_TEXTURE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/wolf/wolf.png");

    private final WerewolfModel wolfmanModel;
    private final WerewolfWolfModel wolfModel;

    public WerewolfLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                         EntityRendererProvider.Context ctx) {
        super(parent);
        this.wolfmanModel = new WerewolfModel(ctx.bakeLayer(WerewolfModel.LAYER_LOCATION));
        this.wolfModel = new WerewolfWolfModel(ctx.bakeLayer(WerewolfWolfModel.LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack ps, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float f1, float f2) {
        // Look up the entity to check werewolf status
        Entity entity = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getEntity(state.id) : null;
        if (!(entity instanceof Player player)) return;
        var data = WitcheryAttachments.get(player);
        if (!data.isWerewolf() || data.getTransformationLevel() <= 0.01F) return;

        ps.pushPose();
        if (data.getWolfForm() == WolfForm.WOLF) {
            wolfModel.setupAnim(state);
            collector.submitModelPart(wolfModel.root(), ps,
                    RenderTypes.entityCutoutNoCull(WOLF_TEXTURE), light,
                    OverlayTexture.NO_OVERLAY, null);
        } else {
            ps.scale(1.1F, 1.1F, 1.1F);
            wolfmanModel.setupAnim(state);
            collector.submitModelPart(wolfmanModel.root(), ps,
                    RenderTypes.entityCutoutNoCull(WOLFMAN_TEXTURE), light,
                    OverlayTexture.NO_OVERLAY, null);
        }
        ps.popPose();
    }
}
