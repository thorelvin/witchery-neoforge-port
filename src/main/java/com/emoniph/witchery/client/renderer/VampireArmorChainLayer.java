package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.VampireArmorModel;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Reproduces the source metal-clothing chain pass, including its cumulative second scale. */
public class VampireArmorChainLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    static final float BODY_SCALE = 1.06F;
    static final float ARM_SCALE = BODY_SCALE * BODY_SCALE;
    static final float ARM_PIVOT_Y_DELTA = -0.05F;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "witchery", "textures/entity/vampirearmor_chain.png");

    private final VampireArmorModel masculine;
    private final VampireArmorModel feminine;

    public VampireArmorChainLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                                  EntityRendererProvider.Context context) {
        super(parent);
        this.masculine = new VampireArmorModel(
                context.bakeLayer(VampireArmorModel.Variant.METALLIC.layer),
                VampireArmorModel.Variant.METALLIC);
        this.feminine = new VampireArmorModel(
                context.bakeLayer(VampireArmorModel.Variant.METALLIC_FEMININE.layer),
                VampireArmorModel.Variant.METALLIC_FEMININE);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float f1, float f2) {
        Entity entity = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getEntity(state.id) : null;
        if (!(entity instanceof Player player)) return;

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean female = chestStack.is(WitcheryItems.VAMPIRE_CHAIN_COAT_LADIES.get());
        boolean metal = female
                || chestStack.is(WitcheryItems.VAMPIRE_CHAIN_COAT.get())
                || chestStack.is(WitcheryItems.VAMPIRE_CHESTPLATE.get());
        if (!metal) return;

        VampireArmorModel model = female ? this.feminine : this.masculine;
        copyPose(this.getParentModel().body, model.body);
        copyPose(this.getParentModel().rightArm, model.rightArm);
        copyPose(this.getParentModel().leftArm, model.leftArm);
        model.chainBody().visible = true;
        model.chainChest().visible = true;
        var renderType = RenderTypes.entityCutoutNoCull(TEXTURE);

        poseStack.pushPose();
        poseStack.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
        if (female) {
            collector.submitModelPart(model.chainChest(), poseStack, renderType,
                    light, OverlayTexture.NO_OVERLAY, null);
        }

        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        collector.submitModelPart(model.chainBody(), poseStack, renderType,
                light, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();

        poseStack.scale(BODY_SCALE, BODY_SCALE, BODY_SCALE);
        model.rightArm.y += ARM_PIVOT_Y_DELTA;
        model.leftArm.y += ARM_PIVOT_Y_DELTA;
        collector.submitModelPart(model.rightArm, poseStack, renderType,
                light, OverlayTexture.NO_OVERLAY, null);
        collector.submitModelPart(model.leftArm, poseStack, renderType,
                light, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }

    private static void copyPose(ModelPart from, ModelPart to) {
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
        to.xScale = from.xScale;
        to.yScale = from.yScale;
        to.zScale = from.zScale;
    }
}
