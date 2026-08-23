package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.ReflectionEntity;
import com.emoniph.witchery.client.model.LegacyBipedModel;
import com.emoniph.witchery.client.model.WolfmanModel;
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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;

import java.util.UUID;

public class ReflectionRenderer extends HumanoidMobRenderer<ReflectionEntity,
        ReflectionRenderer.ReflectionRenderState, LegacyBipedModel<ReflectionRenderer.ReflectionRenderState>> {

    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final float SOURCE_SHADOW_RADIUS = 0.5F;

    static final Identifier SOURCE_FALLBACK =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/reflection.png");
    static final Identifier WOLFMAN_TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/wolfman_entity.png");

    public static class ReflectionRenderState extends LegacyBipedModel.State {
        public Identifier skinTexture = null;
        public boolean wolfman;
        public float wolfmanAttackTimer;
        public float attackProgress;
    }

    private static final class ReflectionModel extends LegacyBipedModel<ReflectionRenderState> {
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
        super(ctx, new ReflectionModel(ctx.bakeLayer(LegacyBipedModel.LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
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
        int attackTimer = entity.getAttackTimer();
        state.wolfmanAttackTimer = attackTimer > 0
                ? Math.max(0.0F, attackTimer - partialTick) : 0.0F;
        state.attackProgress = entity.getAttackAnim(partialTick);
        state.sourceCrouching = entity.isCrouching();
        state.sourceRiding = entity.isPassenger();
        state.swingProgress = state.attackProgress;
        populateHeldItemState(entity.getMainHandItem(), entity.isUsingItem(), state);
    }

    @Override
    public Identifier getTextureLocation(ReflectionRenderState state) {
        return state.skinTexture != null ? state.skinTexture : SOURCE_FALLBACK;
    }

    @Override
    protected int getBlockLightLevel(ReflectionEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected void scale(ReflectionRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F,
                LEGACY_BASE_DELTA_Y - (state.sourceCrouching ? 0.125F : 0.0F), 0.0F);
    }

    private static void populateHeldItemState(ItemStack heldItem, boolean usingItem,
                                              ReflectionRenderState state) {
        state.heldItemLeft = 0;
        state.heldItemRight = heldItem.isEmpty() ? 0 : 1;
        state.aimedBow = false;
        if (!heldItem.isEmpty() && usingItem) {
            ItemUseAnimation animation = heldItem.getUseAnimation();
            if (animation == ItemUseAnimation.BLOCK) state.heldItemRight = 3;
            else if (animation == ItemUseAnimation.BOW) state.aimedBow = true;
        }
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
            LegacyBipedModel<ReflectionRenderState>> {
        private final WolfmanModel model;
        private final WolfmanRenderer.WolfmanRenderState wolfmanState =
                new WolfmanRenderer.WolfmanRenderState();

        private WolfmanLayer(RenderLayerParent<ReflectionRenderState,
                LegacyBipedModel<ReflectionRenderState>> parent, EntityRendererProvider.Context context) {
            super(parent);
            model = new WolfmanModel(context.bakeLayer(WolfmanModel.LAYER_LOCATION));
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                           ReflectionRenderState state, float limbSwing, float limbSwingAmount) {
            if (!state.wolfman) return;
            poseStack.pushPose();
            WolfmanRenderer.copyAnimationState(state, wolfmanState);
            wolfmanState.attackTimer = state.wolfmanAttackTimer;
            wolfmanState.attackProgress = state.attackProgress;
            wolfmanState.sourceSitting = false;
            wolfmanState.sourceCrouching = state.sourceCrouching;
            wolfmanState.heldItemLeft = 0;
            wolfmanState.heldItemRight = 0;
            wolfmanState.aimedBow = false;
            model.setupAnim(wolfmanState);
            collector.submitModelPart(model.root(), poseStack,
                    RenderTypes.entityCutoutNoCull(WOLFMAN_TEXTURE), light,
                    OverlayTexture.NO_OVERLAY, null);
            poseStack.popPose();
        }
    }
}
