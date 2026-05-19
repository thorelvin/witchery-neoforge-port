package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.KettleModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public final class KettleSpecialRenderer implements NoDataSpecialModelRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/kettle.png");

    private final KettleModel model;

    public KettleSpecialRenderer(KettleModel model) {
        this.model = model;
    }

    @Override
    public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                       int packedLight, int packedOverlay, boolean foil, int tint) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        collector.submitModelPart(
                model.root(),
                poseStack,
                RenderTypes.entityCutout(TEXTURE),
                packedLight,
                packedOverlay,
                null,
                false,
                foil,
                -1,
                (ModelFeatureRenderer.CrumblingOverlay) null,
                tint);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        model.root().getExtentsForGui(poseStack, consumer);
    }

    public static final class Unbaked implements SpecialModelRenderer.Unbaked {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        private Unbaked() {}

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new KettleSpecialRenderer(new KettleModel(context.entityModelSet().bakeLayer(KettleModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
