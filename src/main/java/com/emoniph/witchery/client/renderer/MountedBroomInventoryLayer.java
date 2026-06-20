package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BroomModel;
import com.emoniph.witchery.entity.BroomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class MountedBroomInventoryLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/broom.png");

    private final BroomModel model;

    public MountedBroomInventoryLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                                      EntityRendererProvider.Context ctx) {
        super(parent);
        this.model = new BroomModel(ctx.bakeLayer(BroomModel.LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float f1, float f2) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isInventoryPreviewScreen(minecraft.screen) || minecraft.level == null) return;
        if (!state.shadowPieces.isEmpty()) return;

        Entity entity = minecraft.level.getEntity(state.id);
        if (!(entity instanceof Player player) || !(player.getVehicle() instanceof BroomEntity)) return;

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.2F, 0.0F);
        model.setupAnim(state);
        collector.submitModel(model, state, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                -1, null, state.outlineColor, null);
        poseStack.popPose();
    }

    private static boolean isInventoryPreviewScreen(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }
}
