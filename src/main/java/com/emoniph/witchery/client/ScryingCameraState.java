package com.emoniph.witchery.client;

import com.emoniph.witchery.network.ScryingCameraPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;

public final class ScryingCameraState {
    private static final long PACKET_TIMEOUT_MILLIS = 3000L;

    private static boolean active;
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;
    private static float cameraYaw;
    private static float cameraPitch;
    private static Marker cameraEntity;
    private static long lastKeepAliveMillis;
    private static Entity previousCameraEntity;
    private static boolean swappedThisFrame;

    private ScryingCameraState() {
    }

    public static void handle(ScryingCameraPayload payload) {
        if (!payload.active()) {
            stop();
            return;
        }

        active = true;
        lastKeepAliveMillis = System.currentTimeMillis();
        cameraX = payload.x();
        cameraY = payload.y();
        cameraZ = payload.z();
        cameraYaw = payload.yaw();
        cameraPitch = payload.pitch();
    }

    public static void clientTick() {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || timedOut()) {
            stop();
        }
    }

    public static boolean isActive() {
        return active && !timedOut();
    }

    public static void beforeRenderFrame() {
        if (!active) {
            return;
        }
        if (timedOut()) {
            stop();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.screen != null) {
            return;
        }

        Marker target = getOrCreateCameraEntity(mc);
        target.snapTo(cameraX, cameraY, cameraZ, cameraYaw, cameraPitch);

        previousCameraEntity = mc.getCameraEntity();
        if (previousCameraEntity != target) {
            mc.setCameraEntity(target);
            swappedThisFrame = true;
        }
    }

    public static void afterRenderFrame() {
        if (!swappedThisFrame) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Entity restore = previousCameraEntity != null && !previousCameraEntity.isRemoved()
                ? previousCameraEntity
                : mc.player;
        if (restore != null) {
            mc.setCameraEntity(restore);
        }
        previousCameraEntity = null;
        swappedThisFrame = false;
    }

    private static boolean timedOut() {
        return System.currentTimeMillis() - lastKeepAliveMillis > PACKET_TIMEOUT_MILLIS;
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        Entity camera = mc.getCameraEntity();
        if (cameraEntity != null && camera == cameraEntity && mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        active = false;
        cameraEntity = null;
        previousCameraEntity = null;
        swappedThisFrame = false;
    }

    private static Marker getOrCreateCameraEntity(Minecraft mc) {
        if (cameraEntity == null || cameraEntity.level() != mc.level) {
            cameraEntity = new Marker(EntityType.MARKER, mc.level);
            cameraEntity.setInvisible(true);
            cameraEntity.noPhysics = true;
        }
        return cameraEntity;
    }
}
