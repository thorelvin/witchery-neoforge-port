package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.StatueHobgoblinPatronBlock;
import com.emoniph.witchery.blocks.entity.StatueHobgoblinPatronBlockEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public final class PatronStatueRenderer implements BlockEntityRenderer<StatueHobgoblinPatronBlockEntity,
        PatronStatueRenderer.State> {
    public static final Identifier OVERLAY_TEXTURE = Identifier.fromNamespaceAndPath(
            "witchery", "textures/block/statueofworship.png");
    private static final int SKIN_TINT = 0xFFB3B3B3;
    private static final int STONE_TINT = 0xFFCCCCCC;

    private final HumanoidModel<HumanoidRenderState> model;

    public PatronStatueRenderer(BlockEntityRendererProvider.Context context) {
        model = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER));
    }

    public static final class State extends BlockEntityRenderState {
        private Direction facing = Direction.NORTH;
        private String ownerName = "";
        @Nullable
        private UUID ownerUuid;
        private Identifier skin = net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(StatueHobgoblinPatronBlockEntity statue, State state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderer.super.extractRenderState(statue, state, partialTick, cameraPos, overlay);
        var blockState = statue.getBlockState();
        if (blockState.hasProperty(StatueHobgoblinPatronBlock.FACING)) {
            state.facing = blockState.getValue(StatueHobgoblinPatronBlock.FACING);
        }
        if (!Objects.equals(state.ownerUuid, statue.ownerUuid())
                || !state.ownerName.equals(statue.ownerName())) {
            state.ownerUuid = statue.ownerUuid();
            state.ownerName = statue.ownerName();
            state.skin = resolveSkin(state.ownerUuid, state.ownerName);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        applySourceWorldPose(poseStack, state.facing);
        submitSourceModel(model, state.skin, poseStack, collector, state.lightCoords,
                OverlayTexture.NO_OVERLAY, false, 0);
        poseStack.popPose();
    }

    public static void applySourceWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> throw new IllegalArgumentException("Patron statues require a horizontal facing");
        }));
    }

    public static void applySourceItemPose(PoseStack poseStack) {
        applySourceWorldPose(poseStack, Direction.SOUTH);
        poseStack.scale(0.6F, 0.6F, 0.6F);
        poseStack.translate(0.0F, 1.5F, 0.0F);
    }

    public static void submitSourceModel(HumanoidModel<HumanoidRenderState> model, Identifier skin,
                                         PoseStack poseStack, SubmitNodeCollector collector,
                                         int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        setupSourcePose(model);
        submitChildPass(model, skin, poseStack, collector, lightCoords, overlayCoords, hasFoil,
                outlineColor, false, SKIN_TINT);
        submitChildPass(model, OVERLAY_TEXTURE, poseStack, collector, lightCoords, overlayCoords, hasFoil,
                outlineColor, true, STONE_TINT);
    }

    private static void setupSourcePose(HumanoidModel<HumanoidRenderState> model) {
        model.head.resetPose();
        model.hat.resetPose();
        model.body.resetPose();
        model.rightArm.resetPose();
        model.leftArm.resetPose();
        model.rightLeg.resetPose();
        model.leftLeg.resetPose();
        model.rightArm.xRot = -Mth.PI / 10.0F;
        model.leftArm.xRot = -Mth.PI / 10.0F;
    }

    private static void submitChildPass(HumanoidModel<HumanoidRenderState> model, Identifier texture,
                                        PoseStack poseStack, SubmitNodeCollector collector,
                                        int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor,
                                        boolean translucent, int tint) {
        var renderType = translucent ? RenderTypes.entityTranslucent(texture) : RenderTypes.entityCutout(texture);
        poseStack.pushPose();
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.translate(0.0F, 1.0F, 0.0F);
        submitPart(model.head, poseStack, collector, renderType, lightCoords, overlayCoords, hasFoil, outlineColor, tint);
        submitPart(model.hat, poseStack, collector, renderType, lightCoords, overlayCoords, hasFoil, outlineColor, tint);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0F, 1.5F, 0.0F);
        submitPart(model.body, poseStack, collector, renderType, lightCoords, overlayCoords, hasFoil, outlineColor, tint);
        submitPart(model.rightArm, poseStack, collector, renderType, lightCoords, overlayCoords, hasFoil, outlineColor, tint);
        submitPart(model.leftArm, poseStack, collector, renderType, lightCoords, overlayCoords, hasFoil, outlineColor, tint);
        submitPart(model.rightLeg, poseStack, collector, renderType, lightCoords, overlayCoords, hasFoil, outlineColor, tint);
        submitPart(model.leftLeg, poseStack, collector, renderType, lightCoords, overlayCoords, hasFoil, outlineColor, tint);
        poseStack.popPose();
    }

    private static void submitPart(ModelPart part, PoseStack poseStack, SubmitNodeCollector collector,
                                   net.minecraft.client.renderer.rendertype.RenderType renderType,
                                   int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor, int tint) {
        collector.submitModelPart(part, poseStack, renderType, lightCoords, overlayCoords, null,
                false, hasFoil, tint, null, outlineColor);
    }

    public static Identifier resolveSkin(@Nullable UUID uuid, String name) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            Player player = uuid == null ? null : minecraft.level.getPlayerByUUID(uuid);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                return clientPlayer.getSkin().body().texturePath();
            }
        }
        if (uuid == null && name.isBlank()) {
            return net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
        }
        UUID profileUuid = uuid != null ? uuid
                : UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return minecraft.getSkinManager().createLookup(
                new GameProfile(profileUuid, name.isBlank() ? null : name), false).get().body().texturePath();
    }
}
