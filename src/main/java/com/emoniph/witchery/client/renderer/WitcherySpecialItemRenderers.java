package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.client.model.BrazierModel;
import com.emoniph.witchery.client.model.CandelabraModel;
import com.emoniph.witchery.client.model.ChaliceModel;
import com.emoniph.witchery.client.model.CoffinModel;
import com.emoniph.witchery.client.model.DistilleryModel;
import com.emoniph.witchery.client.model.DreamCatcherModel;
import com.emoniph.witchery.client.model.FumeFunnelModel;
import com.emoniph.witchery.client.model.FetishModel;
import com.emoniph.witchery.client.model.FetishTrentModel;
import com.emoniph.witchery.client.model.GrassperModel;
import com.emoniph.witchery.client.model.LeechChestModel;
import com.emoniph.witchery.client.model.PoppetShelfModel;
import com.emoniph.witchery.client.model.SpinningWheelModel;
import com.emoniph.witchery.client.model.StatueGoddessModel;
import com.emoniph.witchery.client.model.WitchesOvenModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
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
        event.register(id("grassper"), GrassperUnbaked.MAP_CODEC);
        event.register(id("spinning_wheel"), SpinningWheelUnbaked.MAP_CODEC);
        event.register(id("poppet_shelf"), PoppetShelfUnbaked.MAP_CODEC);
        event.register(id("fume_funnel"), FumeFunnelUnbaked.MAP_CODEC);
        event.register(id("witches_oven"), WitchesOvenUnbaked.MAP_CODEC);
        event.register(id("brazier"), BrazierUnbaked.MAP_CODEC);
        event.register(id("candelabra"), CandelabraUnbaked.MAP_CODEC);
        event.register(id("chalice"), ChaliceUnbaked.MAP_CODEC);
        event.register(id("leech_chest"), LeechChestUnbaked.MAP_CODEC);
        event.register(id("refilling_chest"), RefillingChestUnbaked.MAP_CODEC);
        event.register(id("dream_catcher"), DreamCatcherUnbaked.MAP_CODEC);
        event.register(id("coffin"), CoffinUnbaked.MAP_CODEC);
        event.register(id("fetish_scarecrow"), FetishScarecrowUnbaked.MAP_CODEC);
        event.register(id("fetish_trent_effigy"), FetishTrentUnbaked.MAP_CODEC);
        event.register(id("fetish_witchs_ladder"), FetishWitchsLadderUnbaked.MAP_CODEC);
        event.register(id("statue_goddess"), StatueGoddessUnbaked.MAP_CODEC);
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

    public record GrassperSpecialRenderer(GrassperModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("grassper");

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

    public record GrassperUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<GrassperUnbaked> MAP_CODEC = MapCodec.unit(new GrassperUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new GrassperSpecialRenderer(new GrassperModel(context.entityModelSet().bakeLayer(GrassperModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<GrassperUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record SpinningWheelSpecialRenderer(SpinningWheelModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("spinningwheel");

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
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            poseStack.translate(-0.5, 0, -0.5);
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            consumer.accept(new Vector3f(0.0f, 0.0f, 0.0f));
            consumer.accept(new Vector3f(1.0f, 0.5f, 1.0f));
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

    public record WitchesOvenSpecialRenderer(WitchesOvenModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("witches_oven");

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

    public record WitchesOvenUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<WitchesOvenUnbaked> MAP_CODEC = MapCodec.unit(new WitchesOvenUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new WitchesOvenSpecialRenderer(new WitchesOvenModel(context.entityModelSet().bakeLayer(WitchesOvenModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<WitchesOvenUnbaked> type() {
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

    public record ChaliceSpecialRenderer(ChaliceModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("chalice");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            for (ModelPart part : model.bodyParts()) {
                collector.submitModelPart(part, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            }
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record ChaliceUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<ChaliceUnbaked> MAP_CODEC = MapCodec.unit(new ChaliceUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new ChaliceSpecialRenderer(new ChaliceModel(context.entityModelSet().bakeLayer(ChaliceModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<ChaliceUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record LeechChestSpecialRenderer(LeechChestModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("leechchest");

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

    public record LeechChestUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<LeechChestUnbaked> MAP_CODEC = MapCodec.unit(new LeechChestUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new LeechChestSpecialRenderer(new LeechChestModel(context.entityModelSet().bakeLayer(LeechChestModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<LeechChestUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record RefillingChestSpecialRenderer(ModelPart bottom, ModelPart lid, ModelPart lock) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/chest/normal.png");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            poseStack.translate(-0.5, -0.5, -0.5);
            collector.submitModelPart(bottom, poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            collector.submitModelPart(lid, poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            collector.submitModelPart(lock, poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record RefillingChestUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<RefillingChestUnbaked> MAP_CODEC = MapCodec.unit(new RefillingChestUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            ModelPart root = context.entityModelSet().bakeLayer(ModelLayers.CHEST);
            return new RefillingChestSpecialRenderer(root.getChild("bottom"), root.getChild("lid"), root.getChild("lock"));
        }

        @Override
        public MapCodec<RefillingChestUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record DreamCatcherSpecialRenderer(DreamCatcherModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("dream_catcher");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.translate(0.0, 0.0, -0.5);
            int effectiveLight = displayContext == ItemDisplayContext.GUI ? 0xF000F0 : lightCoords;
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE), effectiveLight, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record DreamCatcherUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<DreamCatcherUnbaked> MAP_CODEC = MapCodec.unit(new DreamCatcherUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new DreamCatcherSpecialRenderer(new DreamCatcherModel(context.entityModelSet().bakeLayer(DreamCatcherModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<DreamCatcherUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record CoffinSpecialRenderer(CoffinModel headModel, CoffinModel bottomModel) implements NoDataSpecialModelRenderer {
        private static final Identifier HEAD_TEXTURE = blockTexture("coffin_head");
        private static final Identifier BOTTOM_TEXTURE = blockTexture("coffin_bottom");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            poseStack.pushPose();
            poseStack.translate(0.5, displayContext == ItemDisplayContext.GUI ? 0.42 : 0.5, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(0.56F, 0.56F, 0.56F);
            poseStack.translate(0.0F, 1.0F, 0.55F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            int effectiveLight = displayContext == ItemDisplayContext.GUI ? 0xF000F0 : lightCoords;
            collector.submitModelPart(headModel.root(), poseStack, RenderTypes.entityCutout(HEAD_TEXTURE),
                    effectiveLight, OverlayTexture.NO_OVERLAY, null);
            poseStack.translate(0.0F, 0.0F, -1.0F);
            collector.submitModelPart(bottomModel.root(), poseStack, RenderTypes.entityCutout(BOTTOM_TEXTURE),
                    effectiveLight, OverlayTexture.NO_OVERLAY, null);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record CoffinUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<CoffinUnbaked> MAP_CODEC = MapCodec.unit(new CoffinUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new CoffinSpecialRenderer(
                    new CoffinModel(context.entityModelSet().bakeLayer(CoffinModel.HEAD_LAYER_LOCATION)),
                    new CoffinModel(context.entityModelSet().bakeLayer(CoffinModel.BOTTOM_LAYER_LOCATION))
            );
        }

        @Override
        public MapCodec<CoffinUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record FetishSpecialRenderer(ModelPart root, Identifier texture) implements NoDataSpecialModelRenderer {
        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            collector.submitModelPart(root, poseStack, RenderTypes.entityCutout(texture), lightCoords, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record FetishScarecrowUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<FetishScarecrowUnbaked> MAP_CODEC = MapCodec.unit(new FetishScarecrowUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new FetishSpecialRenderer(
                    new FetishModel(context.entityModelSet().bakeLayer(FetishModel.LAYER_LOCATION)).root(),
                    blockTexture("scarecrow"));
        }

        @Override
        public MapCodec<FetishScarecrowUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record FetishTrentUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<FetishTrentUnbaked> MAP_CODEC = MapCodec.unit(new FetishTrentUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new FetishSpecialRenderer(
                    new FetishTrentModel(context.entityModelSet().bakeLayer(FetishTrentModel.LAYER_LOCATION)).root(),
                    blockTexture("trent"));
        }

        @Override
        public MapCodec<FetishTrentUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record FetishWitchsLadderUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<FetishWitchsLadderUnbaked> MAP_CODEC = MapCodec.unit(new FetishWitchsLadderUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new FetishSpecialRenderer(
                    new FetishModel(context.entityModelSet().bakeLayer(FetishModel.LAYER_LOCATION)).root(),
                    blockTexture("witchsladder"));
        }

        @Override
        public MapCodec<FetishWitchsLadderUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record StatueGoddessSpecialRenderer(StatueGoddessModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("statuegoddess");

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

    public record StatueGoddessUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<StatueGoddessUnbaked> MAP_CODEC = MapCodec.unit(new StatueGoddessUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new StatueGoddessSpecialRenderer(
                    new StatueGoddessModel(context.entityModelSet().bakeLayer(StatueGoddessModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<StatueGoddessUnbaked> type() {
            return MAP_CODEC;
        }
    }
}
