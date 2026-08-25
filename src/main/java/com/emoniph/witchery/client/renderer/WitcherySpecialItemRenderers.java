package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.client.model.BrazierModel;
import com.emoniph.witchery.client.model.BeartrapModel;
import com.emoniph.witchery.client.model.BrewBottleModel;
import com.emoniph.witchery.client.model.CandelabraModel;
import com.emoniph.witchery.client.model.CaneSwordModel;
import com.emoniph.witchery.client.model.ChaliceModel;
import com.emoniph.witchery.client.model.CoffinModel;
import com.emoniph.witchery.client.model.CrystalBallModel;
import com.emoniph.witchery.client.model.DeathHandModel;
import com.emoniph.witchery.client.model.DistilleryModel;
import com.emoniph.witchery.client.model.DreamCatcherModel;
import com.emoniph.witchery.client.model.FumeFunnelModel;
import com.emoniph.witchery.client.model.FetishModel;
import com.emoniph.witchery.client.model.FetishTrentModel;
import com.emoniph.witchery.client.model.GrassperModel;
import com.emoniph.witchery.client.model.HandBowModel;
import com.emoniph.witchery.client.model.HuntsmanSpearModel;
import com.emoniph.witchery.client.model.KettleModel;
import com.emoniph.witchery.client.model.LeechChestModel;
import com.emoniph.witchery.client.model.MandrakeStatueModel;
import com.emoniph.witchery.client.model.MysticBranchModel;
import com.emoniph.witchery.client.model.PoppetShelfModel;
import com.emoniph.witchery.client.model.SilverVatModel;
import com.emoniph.witchery.client.model.SpinningWheelModel;
import com.emoniph.witchery.client.model.StatueGoddessModel;
import com.emoniph.witchery.client.model.WolfAltarModel;
import com.emoniph.witchery.client.model.WolfStatueModel;
import com.emoniph.witchery.client.model.WitchesOvenModel;
import com.emoniph.witchery.blocks.entity.StatueHobgoblinPatronBlockEntity;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.HandBowItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.UUID;

public final class WitcherySpecialItemRenderers {
    private WitcherySpecialItemRenderers() {}

    public static void register(RegisterSpecialModelRendererEvent event) {
        event.register(id("distillery"), DistilleryUnbaked.MAP_CODEC);
        event.register(id("kettle"), KettleUnbaked.MAP_CODEC);
        event.register(id("grassper"), GrassperUnbaked.MAP_CODEC);
        event.register(id("spinning_wheel"), SpinningWheelUnbaked.MAP_CODEC);
        event.register(id("poppet_shelf"), PoppetShelfUnbaked.MAP_CODEC);
        event.register(id("fume_funnel"), FumeFunnelUnbaked.MAP_CODEC);
        event.register(id("witches_oven"), WitchesOvenUnbaked.MAP_CODEC);
        event.register(id("brazier"), BrazierUnbaked.MAP_CODEC);
        event.register(id("beartrap"), BeartrapUnbaked.MAP_CODEC);
        event.register(id("wolftrap"), WolftrapUnbaked.MAP_CODEC);
        event.register(id("deaths_hand"), DeathHandUnbaked.MAP_CODEC);
        event.register(id("deaths_hand_deployed"), DeployedDeathHandUnbaked.MAP_CODEC);
        event.register(id("brew_bag_held"), BrewBagHeldUnbaked.MAP_CODEC);
        event.register(id("cane_sword_held"), CaneSwordHeldUnbaked.MAP_CODEC);
        event.register(id("drawn_cane_sword_held"), DrawnCaneSwordHeldUnbaked.MAP_CODEC);
        event.register(id("hand_bow_held"), HandBowHeldUnbaked.MAP_CODEC);
        event.register(id("huntsman_spear_held"), HuntsmanSpearHeldUnbaked.MAP_CODEC);
        event.register(id("mystic_branch_held"), MysticBranchHeldUnbaked.MAP_CODEC);
        event.register(id("candelabra"), CandelabraUnbaked.MAP_CODEC);
        event.register(id("crystal_ball"), CrystalBallUnbaked.MAP_CODEC);
        event.register(id("chalice"), ChaliceUnbaked.MAP_CODEC);
        event.register(id("leech_chest"), LeechChestUnbaked.MAP_CODEC);
        event.register(id("refilling_chest"), RefillingChestUnbaked.MAP_CODEC);
        event.register(id("dream_catcher"), DreamCatcherUnbaked.MAP_CODEC);
        event.register(id("coffin"), CoffinUnbaked.MAP_CODEC);
        event.register(id("fetish_scarecrow"), FetishScarecrowUnbaked.MAP_CODEC);
        event.register(id("fetish_trent_effigy"), FetishTrentUnbaked.MAP_CODEC);
        event.register(id("fetish_witchs_ladder"), FetishWitchsLadderUnbaked.MAP_CODEC);
        event.register(id("statue_goddess"), StatueGoddessUnbaked.MAP_CODEC);
        event.register(id("statue_hobgoblin_patron"), PatronStatueUnbaked.MAP_CODEC);
        event.register(id("statue_mandrake"), MandrakeStatueUnbaked.MAP_CODEC);
        event.register(id("statue_wolf"), WolfStatueUnbaked.MAP_CODEC);
        event.register(id("wolf_altar"), WolfAltarUnbaked.MAP_CODEC);
        event.register(id("silver_vat"), SilverVatUnbaked.MAP_CODEC);
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

    private static boolean isFirstPerson(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    private static boolean isRightHand(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static void submitItemPart(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack,
                                       net.minecraft.client.renderer.rendertype.RenderType renderType,
                                       int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        collector.submitModelPart(part, poseStack, renderType, lightCoords, overlayCoords, null,
                false, hasFoil, -1, null, outlineColor);
    }

    public record DistillerySpecialRenderer(DistilleryModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("distillery");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            model.setJarCount(0);
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

    public record KettleSpecialRenderer(KettleModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("kettle");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
            for (ModelPart part : model.bodyParts()) {
                collector.submitModelPart(part, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                        lightCoords, OverlayTexture.NO_OVERLAY, null);
            }
            collector.submitModelPart(model.crossbar(), poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                    lightCoords, OverlayTexture.NO_OVERLAY, null);
            for (ModelPart part : model.chainParts()) {
                collector.submitModelPart(part, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                        lightCoords, OverlayTexture.NO_OVERLAY, null);
            }
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            consumer.accept(new Vector3f(-0.25f, 0.0f, 0.0f));
            consumer.accept(new Vector3f(1.25f, 1.0f, 1.0f));
        }
    }

    public record KettleUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<KettleUnbaked> MAP_CODEC = MapCodec.unit(new KettleUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new KettleSpecialRenderer(new KettleModel(context.entityModelSet().bakeLayer(KettleModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<KettleUnbaked> type() {
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
            model.setupAnimation(false, 0L);
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
            for (ModelPart part : model.wideBodyParts()) {
                collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE),
                        lightCoords, OverlayTexture.NO_OVERLAY, null);
            }
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

    public record BeartrapSpecialRenderer(BeartrapModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("beartrap");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            model.setClosed(false);
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE),
                    lightCoords, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record BeartrapUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<BeartrapUnbaked> MAP_CODEC = MapCodec.unit(new BeartrapUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new BeartrapSpecialRenderer(
                    new BeartrapModel(context.entityModelSet().bakeLayer(BeartrapModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<BeartrapUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record WolftrapUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<WolftrapUnbaked> MAP_CODEC = MapCodec.unit(new WolftrapUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new BeartrapSpecialRenderer(
                    new BeartrapModel(context.entityModelSet().bakeLayer(BeartrapModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<WolftrapUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record DeathHandSpecialRenderer(DeathHandModel model, boolean deployed) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "textures/entity/deathshand.png");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            boolean firstPersonView = usesFirstPersonGeometry(displayContext);
            boolean rightHand = isRightHand(displayContext);
            model.setup(firstPersonView, deployed);

            poseStack.pushPose();
            if (firstPersonView) {
                applyFirstPersonPose(poseStack);
            } else {
                applyThirdPersonPose(poseStack, rightHand);
            }
            if (deployed) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(firstPersonView ? -90.0F : 90.0F));
                if (!firstPersonView) {
                    poseStack.translate(thirdPersonDeployedXOffset(rightHand), -0.12F, 0.01F);
                }
            }
            for (ModelPart part : model.handParts(firstPersonView)) {
                submitItemPart(collector, part, poseStack, RenderTypes.entityTranslucent(TEXTURE),
                        lightCoords, overlayCoords, hasFoil, outlineColor);
            }
            if (deployed) {
                poseStack.pushPose();
                poseStack.scale(1.2F, 1.2F, 1.2F);
                submitItemPart(collector, model.scythe(), poseStack, RenderTypes.entityTranslucent(TEXTURE),
                        lightCoords, overlayCoords, hasFoil, outlineColor);
                poseStack.popPose();
            }
            poseStack.popPose();
        }

        static boolean usesFirstPersonGeometry(ItemDisplayContext displayContext) {
            return displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    || displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }

        static float thirdPersonDeployedXOffset(boolean rightHand) {
            return rightHand ? 0.19F : -0.19F;
        }

        static void applyFirstPersonPose(PoseStack poseStack) {
            LegacyHeldItemTransforms.beginFirstPerson(poseStack);
            applySourcePose(poseStack);
            LegacyHeldItemTransforms.finishFirstPerson(poseStack);
        }

        static void applyThirdPersonPose(PoseStack poseStack, boolean rightHand) {
            LegacyHeldItemTransforms.beginThirdPerson(poseStack, rightHand);
            applySourcePose(poseStack);
        }

        static void applySourcePose(PoseStack poseStack) {
            poseStack.mulPose(Axis.XP.rotationDegrees(100.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-51.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-81.0F));
            poseStack.translate(0.125F, 0.12F, -0.85F);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            consumer.accept(new Vector3f(-1.5F, -2.5F, -1.5F));
            consumer.accept(new Vector3f(1.5F, 2.5F, 1.5F));
        }
    }

    public record DeathHandUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<DeathHandUnbaked> MAP_CODEC = MapCodec.unit(new DeathHandUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new DeathHandSpecialRenderer(
                    new DeathHandModel(context.entityModelSet().bakeLayer(DeathHandModel.LAYER_LOCATION)), false);
        }

        @Override
        public MapCodec<DeathHandUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record DeployedDeathHandUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<DeployedDeathHandUnbaked> MAP_CODEC = MapCodec.unit(new DeployedDeathHandUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new DeathHandSpecialRenderer(
                    new DeathHandModel(context.entityModelSet().bakeLayer(DeathHandModel.LAYER_LOCATION)), true);
        }

        @Override
        public MapCodec<DeployedDeathHandUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record BrewBagHeldSpecialRenderer(BrewBottleModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = id("textures/entity/brewbottle.png");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            boolean firstPerson = isFirstPerson(displayContext);
            poseStack.pushPose();
            if (firstPerson) {
                LegacyHeldItemTransforms.beginFirstPerson(poseStack);
            } else {
                LegacyHeldItemTransforms.beginThirdPerson(poseStack, isRightHand(displayContext));
            }
            applySourcePose(poseStack, firstPerson);
            if (firstPerson) {
                LegacyHeldItemTransforms.finishFirstPerson(poseStack);
            }
            for (ModelPart part : model.parts()) {
                submitItemPart(collector, part, poseStack, RenderTypes.entityTranslucent(TEXTURE),
                        lightCoords, overlayCoords, hasFoil, outlineColor);
            }
            poseStack.popPose();
        }

        static void applySourcePose(PoseStack poseStack, boolean firstPerson) {
            poseStack.mulPose(Axis.XP.rotationDegrees(100.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-41.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-81.0F));
            poseStack.translate(0.075F, 0.32F, -0.75F);
            if (firstPerson) {
                poseStack.mulPose(Axis.YP.rotationDegrees(10.0F));
                poseStack.translate(0.3F, 0.05F, 0.0F);
                poseStack.scale(1.5F, 1.5F, 1.5F);
            } else {
                poseStack.scale(1.3F, 1.3F, 1.3F);
            }
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            consumer.accept(new Vector3f(-1.5F, -2.0F, -1.5F));
            consumer.accept(new Vector3f(1.5F, 2.0F, 1.5F));
        }
    }

    public record BrewBagHeldUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<BrewBagHeldUnbaked> MAP_CODEC = MapCodec.unit(new BrewBagHeldUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new BrewBagHeldSpecialRenderer(
                    new BrewBottleModel(context.entityModelSet().bakeLayer(BrewBottleModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<BrewBagHeldUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record CaneSwordHeldSpecialRenderer(CaneSwordModel model, boolean deployed)
            implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = id("textures/entity/canesword.png");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            boolean firstPerson = isFirstPerson(displayContext);
            poseStack.pushPose();
            if (firstPerson) {
                LegacyHeldItemTransforms.beginFirstPerson(poseStack);
            } else {
                LegacyHeldItemTransforms.beginThirdPerson(poseStack, isRightHand(displayContext));
            }
            applySourcePose(poseStack, firstPerson, deployed);
            if (firstPerson) {
                LegacyHeldItemTransforms.finishFirstPerson(poseStack);
            }

            if (deployed) {
                submitScaledPart(collector, model.blade3(), poseStack, 0.0F, 3.15F, 0.0F,
                        0.2F, 4.3F, 0.2F, lightCoords, overlayCoords, hasFoil, outlineColor);
                submitScaledPart(collector, model.blade2(), poseStack, 0.0F, 2.1F, 0.0F,
                        0.15F, 3.7F, 0.7F, lightCoords, overlayCoords, hasFoil, outlineColor);
                submitScaledPart(collector, model.blade1(), poseStack, 0.0F, 0.0F, 0.0F,
                        0.21F, 1.4F, 0.5F, lightCoords, overlayCoords, hasFoil, outlineColor);
            }
            submitScaledPart(collector, model.ball(), poseStack, 0.0F, 0.0F, 0.0F,
                    0.8F, 1.1F, 0.8F, lightCoords, overlayCoords, hasFoil, outlineColor);
            if (!deployed) {
                submitScaledPart(collector, model.sheath(), poseStack, 0.0F, 0.0F, 0.0F,
                        0.8F, 1.0F, 0.8F, lightCoords, overlayCoords, hasFoil, outlineColor);
            }
            poseStack.popPose();
        }

        static void applySourcePose(PoseStack poseStack, boolean firstPerson, boolean deployed) {
            poseStack.mulPose(Axis.XP.rotationDegrees(100.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(19.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-86.0F));
            poseStack.translate(0.475F, 0.12F, 0.0F);
            if (firstPerson) {
                if (deployed) {
                    poseStack.translate(0.4F, 0.3F, -0.2F);
                    poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(1.0F));
                    poseStack.mulPose(Axis.YP.rotationDegrees(20.0F));
                } else {
                    poseStack.mulPose(Axis.XP.rotationDegrees(70.0F));
                    poseStack.translate(0.0F, -0.5F, -0.9F);
                }
            } else if (deployed) {
                poseStack.mulPose(Axis.XP.rotationDegrees(80.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
                poseStack.translate(0.0F, -0.3F, -0.5F);
            }
        }

        static void applyPartScale(PoseStack poseStack, ModelPart part, float x, float y, float z,
                                   float scaleX, float scaleY, float scaleZ) {
            poseStack.translate(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
            poseStack.translate(x, y, z);
            poseStack.scale(scaleX, scaleY, scaleZ);
            poseStack.translate(-part.x / 16.0F, -part.y / 16.0F, -part.z / 16.0F);
        }

        private static void submitScaledPart(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack,
                                             float x, float y, float z, float scaleX, float scaleY, float scaleZ,
                                             int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            poseStack.pushPose();
            applyPartScale(poseStack, part, x, y, z, scaleX, scaleY, scaleZ);
            submitItemPart(collector, part, poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                    lightCoords, overlayCoords, hasFoil, outlineColor);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            consumer.accept(new Vector3f(-2.0F, -4.0F, -2.0F));
            consumer.accept(new Vector3f(2.0F, 4.0F, 2.0F));
        }
    }

    public record CaneSwordHeldUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<CaneSwordHeldUnbaked> MAP_CODEC = MapCodec.unit(new CaneSwordHeldUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new CaneSwordHeldSpecialRenderer(
                    new CaneSwordModel(context.entityModelSet().bakeLayer(CaneSwordModel.LAYER_LOCATION)), false);
        }

        @Override
        public MapCodec<CaneSwordHeldUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record DrawnCaneSwordHeldUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<DrawnCaneSwordHeldUnbaked> MAP_CODEC =
                MapCodec.unit(new DrawnCaneSwordHeldUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new CaneSwordHeldSpecialRenderer(
                    new CaneSwordModel(context.entityModelSet().bakeLayer(CaneSwordModel.LAYER_LOCATION)), true);
        }

        @Override
        public MapCodec<DrawnCaneSwordHeldUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record HandBowHeldSpecialRenderer(HandBowModel model, int stage, boolean pulling)
            implements SpecialModelRenderer<HandBowItem.LoadedBolt> {
        private static final Identifier TEXTURE = id("textures/entity/handbow.png");

        @Override
        public HandBowItem.LoadedBolt extractArgument(ItemStack stack) {
            return HandBowItem.loadedBolt(stack);
        }

        @Override
        public void submit(HandBowItem.LoadedBolt loadedBolt, ItemDisplayContext displayContext,
                           PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            boolean firstPerson = isFirstPerson(displayContext);
            model.setup(loadedBolt == null || loadedBolt == HandBowItem.LoadedBolt.NONE ? stage : 2,
                    modelBolt(loadedBolt));

            poseStack.pushPose();
            if (firstPerson) {
                LegacyHeldItemTransforms.beginFirstPerson(poseStack);
            } else {
                LegacyHeldItemTransforms.beginThirdPerson(poseStack, isRightHand(displayContext));
            }
            applySourcePose(poseStack, firstPerson, pulling);
            if (firstPerson) {
                LegacyHeldItemTransforms.finishFirstPerson(poseStack);
            }
            submitItemPart(collector, model.root(), poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                    lightCoords, overlayCoords, hasFoil, outlineColor);
            poseStack.popPose();
        }

        static HandBowModel.Bolt modelBolt(HandBowItem.LoadedBolt bolt) {
            if (bolt == null) return HandBowModel.Bolt.NONE;
            return switch (bolt) {
                case STAKE -> HandBowModel.Bolt.STAKE;
                case DRAINING -> HandBowModel.Bolt.DRAINING;
                case HOLY -> HandBowModel.Bolt.HOLY;
                case SPLITTING -> HandBowModel.Bolt.SPLITTING;
                case SILVER -> HandBowModel.Bolt.SILVER;
                default -> HandBowModel.Bolt.NONE;
            };
        }

        static void applySourcePose(PoseStack poseStack, boolean firstPerson, boolean pulling) {
            poseStack.mulPose(Axis.XP.rotationDegrees(100.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-51.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-81.0F));
            poseStack.translate(0.095F, -0.01F, -0.72F);
            if (firstPerson) {
                if (pulling) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-25.0F));
                    poseStack.translate(0.0F, 0.1F, 0.0F);
                }
                poseStack.translate(0.2F, 0.1F, 0.0F);
            }
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            consumer.accept(new Vector3f(-1.5F, -1.5F, -1.5F));
            consumer.accept(new Vector3f(1.5F, 1.5F, 1.5F));
        }
    }

    public record HandBowHeldUnbaked(int stage, boolean pulling) implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<HandBowHeldUnbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.INT.optionalFieldOf("stage", 0).forGetter(HandBowHeldUnbaked::stage),
                        Codec.BOOL.optionalFieldOf("pulling", false).forGetter(HandBowHeldUnbaked::pulling)
                ).apply(instance, HandBowHeldUnbaked::new));

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new HandBowHeldSpecialRenderer(
                    new HandBowModel(context.entityModelSet().bakeLayer(HandBowModel.LAYER_LOCATION)),
                    Math.max(0, Math.min(2, stage)), pulling);
        }

        @Override
        public MapCodec<HandBowHeldUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record HuntsmanSpearHeldSpecialRenderer(HuntsmanSpearModel model)
            implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = id("textures/entity/huntsmanspear.png");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            boolean firstPerson = isFirstPerson(displayContext);
            poseStack.pushPose();
            if (firstPerson) {
                LegacyHeldItemTransforms.beginFirstPerson(poseStack);
            } else {
                LegacyHeldItemTransforms.beginThirdPerson(poseStack, isRightHand(displayContext));
            }
            applySourcePose(poseStack, firstPerson, localSwingProgress(displayContext));
            if (firstPerson) {
                LegacyHeldItemTransforms.finishFirstPerson(poseStack);
            }
            submitItemPart(collector, model.root(), poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                    lightCoords, overlayCoords, hasFoil, outlineColor);
            poseStack.popPose();
        }

        static float localSwingProgress(ItemDisplayContext displayContext) {
            var player = Minecraft.getInstance().player;
            if (player == null) return 0.0F;
            ItemStack held = displayUsesMainHand(displayContext, player.getMainArm())
                    ? player.getMainHandItem()
                    : player.getOffhandItem();
            return held.is(WitcheryItems.HUNTSMAN_SPEAR.get()) ? player.getAttackAnim(0.0F) : 0.0F;
        }

        static boolean displayUsesMainHand(ItemDisplayContext displayContext, HumanoidArm mainArm) {
            return isRightHand(displayContext) == (mainArm == HumanoidArm.RIGHT);
        }

        static void applySourcePose(PoseStack poseStack, boolean firstPerson, float swingProgress) {
            poseStack.mulPose(Axis.XP.rotationDegrees(100.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-51.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-81.0F));
            poseStack.translate(0.1F, 0.12F, -0.72F);
            if (swingProgress <= 0.0F) return;

            if (firstPerson) {
                poseStack.translate(0.0F, 0.15F, 0.0F);
                float curve = swingProgress <= 0.5F ? 1.0F - swingProgress : swingProgress;
                poseStack.mulPose(Axis.XP.rotationDegrees(50.0F * curve));
            } else {
                if (swingProgress > 0.3F) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(50.0F));
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(-20.0F));
            }
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            consumer.accept(new Vector3f(-2.0F, -3.0F, -2.0F));
            consumer.accept(new Vector3f(2.0F, 3.0F, 2.0F));
        }
    }

    public record HuntsmanSpearHeldUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<HuntsmanSpearHeldUnbaked> MAP_CODEC =
                MapCodec.unit(new HuntsmanSpearHeldUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new HuntsmanSpearHeldSpecialRenderer(
                    new HuntsmanSpearModel(context.entityModelSet().bakeLayer(HuntsmanSpearModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<HuntsmanSpearHeldUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record MysticBranchHeldSpecialRenderer(MysticBranchModel model)
            implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = id("textures/entity/mysticbranch.png");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            boolean firstPerson = isFirstPerson(displayContext);
            poseStack.pushPose();
            if (firstPerson) {
                LegacyHeldItemTransforms.beginFirstPerson(poseStack);
            } else {
                LegacyHeldItemTransforms.beginThirdPerson(poseStack, isRightHand(displayContext));
            }
            applySourcePose(poseStack);
            if (firstPerson) {
                LegacyHeldItemTransforms.finishFirstPerson(poseStack);
            }
            submitItemPart(collector, model.root(), poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                    lightCoords, overlayCoords, hasFoil, outlineColor);
            poseStack.popPose();
        }

        static void applySourcePose(PoseStack poseStack) {
            poseStack.mulPose(Axis.XP.rotationDegrees(100.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-51.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-81.0F));
            poseStack.translate(0.045F, 0.37F, -0.75F);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            consumer.accept(new Vector3f(-1.5F, -2.0F, -1.5F));
            consumer.accept(new Vector3f(1.5F, 2.0F, 1.5F));
        }
    }

    public record MysticBranchHeldUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<MysticBranchHeldUnbaked> MAP_CODEC =
                MapCodec.unit(new MysticBranchHeldUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new MysticBranchHeldSpecialRenderer(
                    new MysticBranchModel(context.entityModelSet().bakeLayer(MysticBranchModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<MysticBranchHeldUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record CandelabraSpecialRenderer(CandelabraModel model) implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("candelabra");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            poseStack.pushPose();
            if (displayContext == ItemDisplayContext.GUI) {
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(0.82F, 0.82F, 0.82F);
                poseStack.translate(-0.5, -0.6, -0.5);
            }
            startBlockEntityPose(poseStack);
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
            poseStack.popPose();
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

    public record CrystalBallSpecialRenderer(CrystalBallModel model) implements NoDataSpecialModelRenderer {
        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            CrystalBallRenderer.submitModel(model, poseStack, collector, lightCoords,
                    CrystalBallRenderer.innerGlobeColor(1.0F));
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record CrystalBallUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<CrystalBallUnbaked> MAP_CODEC = MapCodec.unit(new CrystalBallUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new CrystalBallSpecialRenderer(
                    new CrystalBallModel(context.entityModelSet().bakeLayer(CrystalBallModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<CrystalBallUnbaked> type() {
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
            collector.submitModelPart(model.chalice(), poseStack, RenderTypes.entityCutoutNoCull(TEXTURE),
                    lightCoords, OverlayTexture.NO_OVERLAY, null);
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
            model.setLidAngle(0.0F);
            model.setSacCount(0);
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
        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.translate(0.0, 0.0, -0.5);
            int effectiveLight = displayContext == ItemDisplayContext.GUI ? 0xF000F0 : lightCoords;
            DreamCatcherRenderer.submitModel(model, poseStack, collector, effectiveLight, null);
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

    public record CoffinSpecialRenderer(CoffinModel model) implements NoDataSpecialModelRenderer {
        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            poseStack.translate(0.0F, 0.9F, 0.0F);
            int effectiveLight = displayContext == ItemDisplayContext.GUI ? 0xF000F0 : lightCoords;
            CoffinRenderer.submitModel(model, poseStack, collector, effectiveLight, BedPart.HEAD, 0.0F);
            finishPose(poseStack);
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
                    new CoffinModel(context.entityModelSet().bakeLayer(CoffinModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<CoffinUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record FetishSpecialRenderer(ModelPart root, Identifier texture, boolean facePlayerInGui) implements NoDataSpecialModelRenderer {
        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            if (facePlayerInGui && displayContext == ItemDisplayContext.GUI) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            collector.submitModelPart(root, poseStack, RenderTypes.entityCutout(texture), lightCoords, OverlayTexture.NO_OVERLAY, null);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record FetishScarecrowSpecialRenderer(FetishModel model) implements NoDataSpecialModelRenderer {
        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            if (displayContext == ItemDisplayContext.GUI) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            FetishRenderer.submitScarecrow(model, poseStack, collector, lightCoords, 0, false);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record FetishTrentSpecialRenderer(FetishTrentModel model) implements NoDataSpecialModelRenderer {
        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            FetishRenderer.submitTrent(model, poseStack, collector, lightCoords, 0, false);
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
            return new FetishScarecrowSpecialRenderer(
                    new FetishModel(context.entityModelSet().bakeLayer(FetishModel.LAYER_LOCATION)));
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
            return new FetishTrentSpecialRenderer(
                    new FetishTrentModel(context.entityModelSet().bakeLayer(FetishTrentModel.LAYER_LOCATION)));
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
                    blockTexture("witchsladder"),
                    false);
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
            poseStack.pushPose();
            StatueGoddessRenderer.applySourceItemPose(poseStack);
            model.setupSourcePose();
            collector.submitModelPart(model.root(), poseStack, RenderTypes.entityCutout(TEXTURE), lightCoords, OverlayTexture.NO_OVERLAY, null);
            poseStack.popPose();
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

    public record PatronOwner(String name, UUID uuid) {}

    public record PatronStatueSpecialRenderer(HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState> model)
            implements SpecialModelRenderer<PatronOwner> {
        @Override
        public PatronOwner extractArgument(ItemStack stack) {
            var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            String name = tag.getStringOr(StatueHobgoblinPatronBlockEntity.ITEM_OWNER_KEY, "");
            String uuidText = tag.getStringOr(StatueHobgoblinPatronBlockEntity.ITEM_OWNER_UUID_KEY, "");
            UUID uuid = null;
            try {
                uuid = uuidText.isBlank() ? null : UUID.fromString(uuidText);
            } catch (IllegalArgumentException ignored) {
                // Malformed imported item data falls back to the name-based skin lookup.
            }
            return new PatronOwner(name, uuid);
        }

        @Override
        public void submit(PatronOwner owner, ItemDisplayContext displayContext, PoseStack poseStack,
                           SubmitNodeCollector collector, int lightCoords, int overlayCoords,
                           boolean hasFoil, int outlineColor) {
            poseStack.pushPose();
            PatronStatueRenderer.applySourceItemPose(poseStack);
            PatronStatueRenderer.submitSourceModel(model,
                    PatronStatueRenderer.resolveSkin(owner == null ? null : owner.uuid(),
                            owner == null ? "" : owner.name()),
                    poseStack, collector, lightCoords, overlayCoords, hasFoil, outlineColor);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record PatronStatueUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<PatronStatueUnbaked> MAP_CODEC = MapCodec.unit(new PatronStatueUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new PatronStatueSpecialRenderer(new HumanoidModel<>(
                    context.entityModelSet().bakeLayer(ModelLayers.PLAYER)));
        }

        @Override
        public MapCodec<PatronStatueUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record DecurseStatueSpecialRenderer(ModelPart model, Identifier texture, float verticalOffset)
            implements NoDataSpecialModelRenderer {
        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            poseStack.pushPose();
            DecurseStatueRenderer.applySourceItemPose(poseStack, verticalOffset);
            collector.submitModelPart(model, poseStack, RenderTypes.entityCutout(texture),
                    lightCoords, OverlayTexture.NO_OVERLAY, null);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record MandrakeStatueUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<MandrakeStatueUnbaked> MAP_CODEC =
                MapCodec.unit(new MandrakeStatueUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            MandrakeStatueModel model = new MandrakeStatueModel(
                    context.entityModelSet().bakeLayer(MandrakeStatueModel.LAYER_LOCATION));
            return new DecurseStatueSpecialRenderer(
                    model.root(), blockTexture("statuemandrake"), 0.255F);
        }

        @Override
        public MapCodec<MandrakeStatueUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record WolfStatueUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<WolfStatueUnbaked> MAP_CODEC =
                MapCodec.unit(new WolfStatueUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            WolfStatueModel model = new WolfStatueModel(
                    context.entityModelSet().bakeLayer(WolfStatueModel.LAYER_LOCATION));
            return new DecurseStatueSpecialRenderer(
                    model.root(), blockTexture("statuewolf"), 0.26F);
        }

        @Override
        public MapCodec<WolfStatueUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record WolfAltarSpecialRenderer(WolfAltarModel model) implements NoDataSpecialModelRenderer {
        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            poseStack.pushPose();
            WolfAltarRenderer.applySourceItemPose(poseStack);
            WolfAltarRenderer.submitModel(model, poseStack, collector,
                    lightCoords, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            consumer.accept(new Vector3f(-0.5F, 0.0F, -0.5F));
            consumer.accept(new Vector3f(1.5F, 2.0F, 1.5F));
        }
    }

    public record WolfAltarUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<WolfAltarUnbaked> MAP_CODEC = MapCodec.unit(new WolfAltarUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new WolfAltarSpecialRenderer(
                    new WolfAltarModel(context.entityModelSet().bakeLayer(WolfAltarModel.LAYER_LOCATION)));
        }

        @Override
        public MapCodec<WolfAltarUnbaked> type() {
            return MAP_CODEC;
        }
    }

    public record SilverVatSpecialRenderer(SilverVatModel model, ModelPart liquid)
            implements NoDataSpecialModelRenderer {
        private static final Identifier TEXTURE = blockTexture("silvervat");

        @Override
        public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector collector,
                           int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
            startBlockEntityPose(poseStack);
            for (ModelPart part : model.bodyParts()) {
                collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE),
                        lightCoords, OverlayTexture.NO_OVERLAY, null);
            }
            for (var direction : List.of(
                    net.minecraft.core.Direction.NORTH,
                    net.minecraft.core.Direction.SOUTH,
                    net.minecraft.core.Direction.WEST,
                    net.minecraft.core.Direction.EAST)) {
                for (ModelPart part : model.spout(direction)) {
                    collector.submitModelPart(part, poseStack, RenderTypes.entityCutout(TEXTURE),
                            lightCoords, OverlayTexture.NO_OVERLAY, null);
                }
            }
            SilverVatRenderer.submitLiquid(liquid, poseStack, collector, lightCoords);
            finishPose(poseStack);
        }

        @Override
        public void getExtents(Consumer<Vector3fc> consumer) {
            unitBlockExtents(consumer);
        }
    }

    public record SilverVatUnbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<SilverVatUnbaked> MAP_CODEC =
                MapCodec.unit(new SilverVatUnbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new SilverVatSpecialRenderer(
                    new SilverVatModel(context.entityModelSet().bakeLayer(SilverVatModel.LAYER_LOCATION)),
                    context.entityModelSet().bakeLayer(SilverVatModel.LIQUID_LAYER_LOCATION)
                            .getChild("liquid"));
        }

        @Override
        public MapCodec<SilverVatUnbaked> type() {
            return MAP_CODEC;
        }
    }
}
