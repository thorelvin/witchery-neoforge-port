package com.emoniph.witchery.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class PlacedModonomiconPagesTest {
    @Test
    void spriteArtParentsStayFlatWithoutDepthFlattening() {
        assertTrue(PlacedModonomiconPages.isSpriteArtParent("minecraft:item/generated"));
        assertTrue(PlacedModonomiconPages.isSpriteArtParent("minecraft:item/handheld"));
        assertTrue(PlacedModonomiconPages.isSpriteArtParent("minecraft:block/cross"));
        assertTrue(PlacedModonomiconPages.isSpriteArtParent("minecraft:block/crop"));
    }

    @Test
    void cubeBlockParentsCanUseDepthFlattening() {
        assertFalse(PlacedModonomiconPages.isSpriteArtParent("minecraft:block/cube"));
        assertFalse(PlacedModonomiconPages.isSpriteArtParent("minecraft:block/cube_bottom_top"));
        assertFalse(PlacedModonomiconPages.isSpriteArtParent("witchery:block/altar"));
    }

    @Test
    void blockItemFlatteningCompressesOnlyPlaneDepthAxis() {
        Matrix4f matrix = new Matrix4f();
        matrix.m00(1.0F).m01(2.0F).m02(3.0F).m03(4.0F);
        matrix.m10(5.0F).m11(6.0F).m12(7.0F).m13(8.0F);
        matrix.m20(9.0F).m21(10.0F).m22(11.0F).m23(12.0F);
        matrix.m30(13.0F).m31(14.0F).m32(15.0F).m33(16.0F);

        PlacedModonomiconPages.flattenItemDepthForPage(matrix);

        assertEquals(1.0F, matrix.m00());
        assertEquals(2.0F, matrix.m01());
        assertEquals(0.03F, matrix.m02(), 0.0001F);
        assertEquals(4.0F, matrix.m03());
        assertEquals(5.0F, matrix.m10());
        assertEquals(6.0F, matrix.m11());
        assertEquals(0.07F, matrix.m12(), 0.0001F);
        assertEquals(8.0F, matrix.m13());
        assertEquals(9.0F, matrix.m20());
        assertEquals(10.0F, matrix.m21());
        assertEquals(0.11F, matrix.m22(), 0.0001F);
        assertEquals(12.0F, matrix.m23());
        assertEquals(13.0F, matrix.m30());
        assertEquals(14.0F, matrix.m31());
        assertEquals(0.15F, matrix.m32(), 0.0001F);
        assertEquals(16.0F, matrix.m33());
    }

    @Test
    void blockItemFlatteningPreservesIconProjectionAndCollapsesDepth() {
        Matrix4f before = new Matrix4f()
                .translation(2.0F, 4.0F, 6.0F)
                .rotateXYZ(0.35F, -0.45F, 0.2F)
                .scale(16.0F, -16.0F, 16.0F);
        Matrix4f after = new Matrix4f(before);
        Vector3f nearBefore = before.transformPosition(new Vector3f(0.2F, 0.4F, 0.0F));
        Vector3f farBefore = before.transformPosition(new Vector3f(0.2F, 0.4F, 1.0F));
        Vector3f depthBefore = farBefore.sub(nearBefore, new Vector3f());

        PlacedModonomiconPages.flattenItemDepthForPage(after);

        Vector3f nearAfter = after.transformPosition(new Vector3f(0.2F, 0.4F, 0.0F));
        Vector3f farAfter = after.transformPosition(new Vector3f(0.2F, 0.4F, 1.0F));
        Vector3f depthAfter = farAfter.sub(nearAfter, new Vector3f());
        assertEquals(nearBefore.x, nearAfter.x, 0.0001F);
        assertEquals(nearBefore.y, nearAfter.y, 0.0001F);
        assertEquals(farBefore.x, farAfter.x, 0.0001F);
        assertEquals(farBefore.y, farAfter.y, 0.0001F);
        assertEquals(nearBefore.z * 0.01F, nearAfter.z, 0.0001F);
        assertEquals(depthBefore.x, depthAfter.x, 0.0001F);
        assertEquals(depthBefore.y, depthAfter.y, 0.0001F);
        assertEquals(depthBefore.z * 0.01F, depthAfter.z, 0.0001F);
    }

    @Test
    void blockItemFlatteningKeepsVanillaGuiBlockAngle() {
        Matrix4f pagePlane = new Matrix4f()
                .translation(2.0F, 4.0F, 6.0F)
                .rotateXYZ(0.35F, -0.45F, 0.2F)
                .scale(16.0F, -16.0F, 16.0F);
        Matrix4f before = new Matrix4f(pagePlane)
                .rotateXYZ((float) Math.toRadians(30.0D), (float) Math.toRadians(225.0D), 0.0F);
        Matrix4f after = new Matrix4f(before);

        PlacedModonomiconPages.flattenItemDepthForPage(after, pagePlane);

        assertVanillaGuiProjectionPreserved(pagePlane, before, after, new Vector3f(0.0F, 0.0F, 0.0F));
        assertVanillaGuiProjectionPreserved(pagePlane, before, after, new Vector3f(1.0F, 0.0F, 0.0F));
        assertVanillaGuiProjectionPreserved(pagePlane, before, after, new Vector3f(0.0F, 1.0F, 0.0F));
        assertVanillaGuiProjectionPreserved(pagePlane, before, after, new Vector3f(0.0F, 0.0F, 1.0F));
        assertVanillaGuiProjectionPreserved(pagePlane, before, after, new Vector3f(1.0F, 1.0F, 1.0F));
    }

    @Test
    void realSubmitStorageCapturesFlattenedSpecialModelPartPose() throws Exception {
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().m02(3.0F).m12(5.0F).m22(16.0F).m32(7.0F);
        SubmitNodeStorage storage = new SubmitNodeStorage();
        Object collector = pageItemCollector(storage, true);

        invokeSubmitModelPart(collector, poseStack);

        Object collection = storage.getSubmitsPerOrder().get(4);
        Object modelPartStorage = collection.getClass().getMethod("getModelPartSubmits").invoke(collection);
        var field = modelPartStorage.getClass().getDeclaredField("modelPartSubmits");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<RenderType, List<Object>> submits = (Map<RenderType, List<Object>>) field.get(modelPartStorage);
        Object modelPartSubmit = submits.values().iterator().next().getFirst();
        Object capturedPose = modelPartSubmit.getClass().getMethod("pose").invoke(modelPartSubmit);
        Matrix4f capturedMatrix = (Matrix4f) capturedPose.getClass().getMethod("pose").invoke(capturedPose);

        assertEquals(0.03F, capturedMatrix.m02(), 0.0001F);
        assertEquals(0.05F, capturedMatrix.m12(), 0.0001F);
        assertEquals(0.16F, capturedMatrix.m22(), 0.0001F);
        assertEquals(0.07F, capturedMatrix.m32(), 0.0001F);
        assertEquals(3.0F, poseStack.last().pose().m02(), 0.0001F);
        assertEquals(5.0F, poseStack.last().pose().m12(), 0.0001F);
        assertEquals(16.0F, poseStack.last().pose().m22(), 0.0001F);
        assertEquals(7.0F, poseStack.last().pose().m32(), 0.0001F);
    }

    @Test
    void blockItemFlatteningAlsoAppliesToSpecialModelParts() throws Exception {
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().m22(16.0F);
        AtomicReference<Float> submittedDepthScale = new AtomicReference<>();
        OrderedSubmitNodeCollector ordered = orderedCollectorRecordingModelPartDepth(submittedDepthScale);
        SubmitNodeCollector target = (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class},
                (proxy, method, args) -> "order".equals(method.getName()) ? ordered : null);

        Object collector = pageItemCollector(target, true);
        Method submitModelPart = collector.getClass().getDeclaredMethod("submitModelPart",
                ModelPart.class, PoseStack.class, RenderType.class, int.class, int.class, TextureAtlasSprite.class,
                boolean.class, boolean.class, int.class, ModelFeatureRenderer.CrumblingOverlay.class, int.class);
        submitModelPart.setAccessible(true);
        submitModelPart.invoke(collector, null, poseStack, null, 0, 0, null, false, false, -1, null, 0);

        assertEquals(0.16F, submittedDepthScale.get(), 0.0001F);
        assertEquals(16.0F, poseStack.last().pose().m22(), 0.0001F);
    }

    @Test
    void nonBlockItemCollectorLeavesModelPartsUnflattened() throws Exception {
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().m22(16.0F);
        AtomicReference<Float> submittedDepthScale = new AtomicReference<>();
        OrderedSubmitNodeCollector ordered = orderedCollectorRecordingModelPartDepth(submittedDepthScale);
        SubmitNodeCollector target = (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class},
                (proxy, method, args) -> "order".equals(method.getName()) ? ordered : null);

        Object collector = pageItemCollector(target, false);
        Method submitModelPart = collector.getClass().getDeclaredMethod("submitModelPart",
                ModelPart.class, PoseStack.class, RenderType.class, int.class, int.class, TextureAtlasSprite.class,
                boolean.class, boolean.class, int.class, ModelFeatureRenderer.CrumblingOverlay.class, int.class);
        submitModelPart.setAccessible(true);
        submitModelPart.invoke(collector, null, poseStack, null, 0, 0, null, false, false, -1, null, 0);

        assertEquals(16.0F, submittedDepthScale.get(), 0.0001F);
        assertEquals(16.0F, poseStack.last().pose().m22(), 0.0001F);
    }

    private static Object pageItemCollector(SubmitNodeCollector target, boolean flattenItems) throws Exception {
        Class<?> collectorClass = Class.forName("com.emoniph.witchery.client.PlacedModonomiconPages$PageItemCollector");
        var constructor = collectorClass.getDeclaredConstructor(SubmitNodeCollector.class, int.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(target, 4, flattenItems);
    }

    private static void invokeSubmitModelPart(Object collector, PoseStack poseStack) throws Exception {
        Method submitModelPart = collector.getClass().getDeclaredMethod("submitModelPart",
                ModelPart.class, PoseStack.class, RenderType.class, int.class, int.class, TextureAtlasSprite.class,
                boolean.class, boolean.class, int.class, ModelFeatureRenderer.CrumblingOverlay.class, int.class);
        submitModelPart.setAccessible(true);
        submitModelPart.invoke(collector, null, poseStack, null, 0, 0, null, false, false, -1, null, 0);
    }

    private static void assertVanillaGuiProjectionPreserved(Matrix4f pagePlane, Matrix4f before,
                                                            Matrix4f after, Vector3f vertex) {
        Matrix4f inversePagePlane = new Matrix4f(pagePlane).invert();
        Vector3f beforeLocal = inversePagePlane.transformPosition(before.transformPosition(new Vector3f(vertex)));
        Vector3f afterLocal = inversePagePlane.transformPosition(after.transformPosition(new Vector3f(vertex)));
        assertEquals(beforeLocal.x, afterLocal.x, 0.0001F);
        assertEquals(beforeLocal.y, afterLocal.y, 0.0001F);
        assertEquals(beforeLocal.z * 0.01F, afterLocal.z, 0.0001F);
    }

    private static OrderedSubmitNodeCollector orderedCollectorRecordingModelPartDepth(
            AtomicReference<Float> submittedDepthScale) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("submitModelPart".equals(method.getName())) {
                submittedDepthScale.set(((PoseStack) args[1]).last().pose().m22());
            }
            return null;
        };
        return (OrderedSubmitNodeCollector) Proxy.newProxyInstance(
                OrderedSubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{OrderedSubmitNodeCollector.class},
                handler);
    }
}
