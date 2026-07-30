package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.entity.CorpseEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Renders the Spirit World sleeping body with the owner player's own skin (source
 * {@code EntityCorpse} resolved the skin from the owner username). Falls back to the default
 * player skin when the owner is unknown or offline and uncached.
 */
public class CorpseRenderer extends HumanoidMobRenderer<CorpseEntity, CorpseRenderer.CorpseRenderState, HumanoidModel<CorpseRenderer.CorpseRenderState>> {

    public static class CorpseRenderState extends HumanoidRenderState {
        public Identifier skinTexture = null;
    }

    private static final class CorpseModel extends HumanoidModel<CorpseRenderState> {
        private CorpseModel(net.minecraft.client.model.geom.ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(CorpseRenderState state) {
            this.resetPose();
        }
    }

    public CorpseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CorpseModel(ctx.bakeLayer(ModelLayers.PLAYER)), CorpseEntity.SOURCE_SHADOW_RADIUS);
    }

    @Override
    public CorpseRenderState createRenderState() {
        return new CorpseRenderState();
    }

    @Override
    public void extractRenderState(CorpseEntity entity, CorpseRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skinTexture = resolveSkin(entity);
    }

    @Override
    public Identifier getTextureLocation(CorpseRenderState state) {
        return state.skinTexture != null ? state.skinTexture : DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    protected void setupRotations(CorpseRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        poseStack.translate(CorpseEntity.SOURCE_RENDER_X_OFFSET, CorpseEntity.SOURCE_RENDER_Y_OFFSET, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(CorpseEntity.SOURCE_PRONE_ROTATION_DEGREES));
        poseStack.mulPose(Axis.YP.rotationDegrees(CorpseEntity.SOURCE_PRONE_ROTATION_DEGREES));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));

        if (state.deathTime > 0.0F) {
            float deathRotation = Mth.sqrt(state.deathTime / 20.0F * 1.6F);
            poseStack.mulPose(Axis.YP.rotationDegrees(
                    Math.min(deathRotation, 1.0F) * CorpseEntity.SOURCE_PRONE_ROTATION_DEGREES));
        } else if (state.isUpsideDown) {
            poseStack.translate(0.0F, state.boundingBoxHeight + 0.1F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }

    private static Identifier resolveSkin(CorpseEntity entity) {
        UUID ownerUUID = entity.getOwnerUUID().orElse(null);
        String name = entity.getOwnerName();
        if (ownerUUID == null && name.isEmpty()) {
            return DefaultPlayerSkin.getDefaultTexture();
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Player player = ownerUUID != null ? mc.level.getPlayerByUUID(ownerUUID) : null;
            if (player == null && !name.isEmpty()) {
                player = mc.level.players().stream()
                        .filter(candidate -> candidate.getScoreboardName().equalsIgnoreCase(name))
                        .findFirst()
                        .orElse(null);
            }
            if (player instanceof AbstractClientPlayer acp) {
                return acp.getSkin().body().texturePath();
            }
        }

        // Owner offline — resolve through the skin cache (returns cached or default immediately).
        UUID profileUUID = ownerUUID != null
                ? ownerUUID
                : UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(profileUUID, name.isEmpty() ? null : name);
        return mc.getSkinManager().createLookup(profile, false).get().body().texturePath();
    }
}
