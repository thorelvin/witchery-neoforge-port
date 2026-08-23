package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.WolfmanModel;
import com.emoniph.witchery.client.model.WerewolfWolfModel;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfForm;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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

    static final Identifier WOLFMAN_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/wolfman_entity.png");
    static final Identifier WOLF_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/werewolf_wolf.png");

    private final WolfmanModel wolfmanModel;
    private final WerewolfWolfModel wolfModel;
    private final WerewolfWolfModel.WolfRenderState wolfState =
            new WerewolfWolfModel.WolfRenderState();
    private final WolfmanRenderer.WolfmanRenderState wolfmanState =
            new WolfmanRenderer.WolfmanRenderState();

    public WerewolfLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                         EntityRendererProvider.Context ctx) {
        super(parent);
        this.wolfmanModel = new WolfmanModel(ctx.bakeLayer(WolfmanModel.LAYER_LOCATION));
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
            prepareWolfState(state, player, wolfState);
            float crouchOffset = wolfState.sourceCrouching && !(player instanceof LocalPlayer)
                    ? 0.125F : 0.0F;
            ps.translate(0.0F, WolfmanRenderer.LEGACY_BASE_DELTA_Y - crouchOffset, 0.0F);
            wolfModel.setupAnim(wolfState);
            collector.submitModelPart(wolfModel.root(), ps,
                    RenderTypes.entityCutoutNoCull(WOLF_TEXTURE), light,
                    OverlayTexture.NO_OVERLAY, null);
        } else {
            prepareWolfmanState(state, player, wolfmanState);
            float crouchOffset = wolfmanState.sourceCrouching
                    ? player instanceof LocalPlayer ? 0.125F : 0.25F
                    : 0.0F;
            ps.translate(0.0F, WolfmanRenderer.LEGACY_BASE_DELTA_Y - crouchOffset, 0.0F);
            wolfmanModel.setupAnim(wolfmanState);
            collector.submitModelPart(wolfmanModel.root(), ps,
                    RenderTypes.entityCutoutNoCull(WOLFMAN_TEXTURE), light,
                    OverlayTexture.NO_OVERLAY, null);
        }
        ps.popPose();
    }

    static void prepareWolfmanState(AvatarRenderState source, Player player,
                                    WolfmanRenderer.WolfmanRenderState target) {
        WolfmanRenderer.copyAnimationState(source, target);
        target.attackTimer = 0.0F;
        target.attackProgress = player.getAttackAnim(source.partialTick);
        target.sourceSitting = player.isPassenger();
        target.sourceCrouching = player.isCrouching();
        WolfmanRenderer.populateHeldItemState(player.getMainHandItem(), player.isUsingItem(), target);
    }

    static void prepareWolfState(AvatarRenderState source, Player player,
                                 WerewolfWolfModel.WolfRenderState target) {
        target.walkAnimationPos = source.walkAnimationPos;
        target.walkAnimationSpeed = source.walkAnimationSpeed;
        target.ageInTicks = source.ageInTicks;
        target.yRot = source.yRot;
        target.xRot = source.xRot;
        target.partialTick = source.partialTick;
        target.sourceCrouching = player.isCrouching();
        target.sitting = player.isPassenger();
        target.angry = false;
        target.interestedAngle = 0.0F;
        target.headShakeAngle = 0.0F;
        target.upperBodyShakeAngle = 0.0F;
        target.bodyShakeAngle = 0.0F;
        target.tailShakeAngle = 0.0F;
    }
}
