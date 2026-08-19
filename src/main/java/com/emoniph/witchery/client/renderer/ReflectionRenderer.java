package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.client.model.WerewolfModel;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class ReflectionRenderer extends HumanoidMobRenderer<ReflectionEntity, ReflectionRenderer.ReflectionRenderState, HumanoidModel<ReflectionRenderer.ReflectionRenderState>> {

    private static final Identifier SOURCE_FALLBACK =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/reflection.png");
    private static final Identifier WOLFMAN_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/wolfman.png");

    public static class ReflectionRenderState extends HumanoidRenderState {
        public Identifier skinTexture = null;
        public boolean wolfman;
    }

    private static final class ReflectionModel extends HumanoidModel<ReflectionRenderState> {
        private ReflectionModel(net.minecraft.client.model.geom.ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(ReflectionRenderState state) {
            super.setupAnim(state);
            boolean visible = !state.wolfman;
            head.visible = visible;
            hat.visible = visible;
            body.visible = visible;
            rightArm.visible = visible;
            leftArm.visible = visible;
            rightLeg.visible = visible;
            leftLeg.visible = visible;
        }
    }

    public ReflectionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ReflectionModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        ArmorModelSet<HumanoidModel<ReflectionRenderState>> armor =
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, ctx.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<>(this, armor, ctx.getEquipmentRenderer()) {
            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                               ReflectionRenderState state, float limbSwing, float limbSwingAmount) {
                if (!state.wolfman) {
                    super.submit(poseStack, collector, light, state, limbSwing, limbSwingAmount);
                }
            }
        });
        this.addLayer(new WolfmanLayer(this, ctx));
    }

    @Override
    public ReflectionRenderState createRenderState() {
        return new ReflectionRenderState();
    }

    @Override
    public void extractRenderState(ReflectionEntity entity, ReflectionRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skinTexture = resolveSkin(entity);
        state.wolfman = entity.isWolfmanReflection();
    }

    @Override
    public Identifier getTextureLocation(ReflectionRenderState state) {
        return state.skinTexture != null ? state.skinTexture : SOURCE_FALLBACK;
    }

    @Override
    protected int getBlockLightLevel(ReflectionEntity entity, BlockPos pos) {
        return 15;
    }

    private static Identifier resolveSkin(ReflectionEntity entity) {
        UUID ownerUUID = entity.getOwnerUUID();
        if (ownerUUID == null) {
            return SOURCE_FALLBACK;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(ownerUUID);
            if (player instanceof AbstractClientPlayer acp) {
                return acp.getSkin().body().texturePath();
            }
        }

        // Player offline — use SkinManager (returns cached or default immediately)
        String name = entity.getOwnerName();
        GameProfile profile = new GameProfile(ownerUUID, name.isEmpty() ? null : name);
        return mc.getSkinManager().createLookup(profile, false).get().body().texturePath();
    }

    private static final class WolfmanLayer extends RenderLayer<ReflectionRenderState,
            HumanoidModel<ReflectionRenderState>> {
        private final WerewolfModel model;

        private WolfmanLayer(RenderLayerParent<ReflectionRenderState,
                HumanoidModel<ReflectionRenderState>> parent, EntityRendererProvider.Context context) {
            super(parent);
            model = new WerewolfModel(context.bakeLayer(WerewolfModel.LAYER_LOCATION));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                           ReflectionRenderState state, float limbSwing, float limbSwingAmount) {
            if (!state.wolfman) return;
            poseStack.pushPose();
            model.setupAnim(state);
            collector.submitModelPart(model.root(), poseStack,
                    RenderTypes.entityCutoutNoCull(WOLFMAN_TEXTURE), light,
                    OverlayTexture.NO_OVERLAY, null);
            poseStack.popPose();
        }
    }
}
