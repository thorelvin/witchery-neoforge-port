package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.client.model.BrazierModel;
import com.emoniph.witchery.client.model.CandelabraModel;
import com.emoniph.witchery.client.model.DistilleryModel;
import com.emoniph.witchery.client.model.FumeFunnelModel;
import com.emoniph.witchery.client.model.PoppetShelfModel;
import com.emoniph.witchery.client.model.SpinningWheelModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class WitcherySpecialItemRenderers {
    private WitcherySpecialItemRenderers() {}

    public static void register(RegisterSpecialModelRendererEvent event) {
        event.register(id("distillery"), DistilleryUnbaked.MAP_CODEC);
        event.register(id("spinning_wheel"), SpinningWheelUnbaked.MAP_CODEC);
        event.register(id("poppet_shelf"), PoppetShelfUnbaked.MAP_CODEC);
        event.register(id("fume_funnel"), FumeFunnelUnbaked.MAP_CODEC);
        event.register(id("brazier"), BrazierUnbaked.MAP_CODEC);
        event.register(id("candelabra"), CandelabraUnbaked.MAP_CODEC);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path);
    }

    private static Identifier blockTexture(String path) {
        return Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "textures/block/" + path + ".png");
    }

    private static void startBlockEntityPose(PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));
        poseStack.translate(0, -1, 0);
    }

    private static void finishPose(PoseStack poseStack) {
        poseStack.popPose();
    }

    private static void unitBlockExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(0.0f, 0.0f, 0.0f));
        consumer.accept(new Vector3f(1.0f, 1.0f, 1.0f));
    }

    public record DistillerySpecialRenderer(DistilleryModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("distillery");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record DistilleryUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<DistilleryUnbaked> MAP_CODEC = MapCodec.unit(new DistilleryUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new DistillerySpecialRenderer(new DistilleryModel(context.entityModelSet().bakeLayer(DistilleryModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<DistilleryUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record SpinningWheelSpecialRenderer(SpinningWheelModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("spinning_wheel");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record SpinningWheelUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<SpinningWheelUnbaked> MAP_CODEC = MapCodec.unit(new SpinningWheelUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new SpinningWheelSpecialRenderer(new SpinningWheelModel(context.entityModelSet().bakeLayer(SpinningWheelModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<SpinningWheelUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record PoppetShelfSpecialRenderer(PoppetShelfModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("poppet_shelf");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.translate(-0.5, 0, -0.5);
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record PoppetShelfUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<PoppetShelfUnbaked> MAP_CODEC = MapCodec.unit(new PoppetShelfUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new PoppetShelfSpecialRenderer(new PoppetShelfModel(context.entityModelSet().bakeLayer(PoppetShelfModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<PoppetShelfUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record FumeFunnelSpecialRenderer(FumeFunnelModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("fumefunnel");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record FumeFunnelUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<FumeFunnelUnbaked> MAP_CODEC = MapCodec.unit(new FumeFunnelUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new FumeFunnelSpecialRenderer(new FumeFunnelModel(context.entityModelSet().bakeLayer(FumeFunnelModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<FumeFunnelUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record BrazierSpecialRenderer(BrazierModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("brazier");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            for (var part : model.structureParts()) {
                collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            }
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record BrazierUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<BrazierUnbaked> MAP_CODEC = MapCodec.unit(new BrazierUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new BrazierSpecialRenderer(new BrazierModel(context.entityModelSet().bakeLayer(BrazierModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<BrazierUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record CandelabraSpecialRenderer(CandelabraModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("candelabra");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record CandelabraUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<CandelabraUnbaked> MAP_CODEC = MapCodec.unit(new CandelabraUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new CandelabraSpecialRenderer(new CandelabraModel(context.entityModelSet().bakeLayer(CandelabraModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<CandelabraUnbaked> type() {
            return MAP_CODEC;
        }
    }
}
