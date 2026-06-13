package com.emoniph.witchery.scrying;

import com.emoniph.witchery.entity.ScryingEyeEntity;
import com.emoniph.witchery.network.ScryingCameraPayload;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScryingSessions {
    private static final Map<UUID, ScryingEyeEntity> EYES_BY_PLAYER = new ConcurrentHashMap<>();
    private static final double LIVING_CAMERA_DISTANCE = 3.0D;
    private static final double LIVING_CAMERA_HEIGHT = 0.65D;
    private static final double LIVING_CAMERA_GROUND_CLEARANCE = 1.35D;
    private static final double LIVING_CAMERA_OBSTRUCTION_MARGIN = 0.25D;

    private ScryingSessions() {
    }

    public static void focusLiving(ServerPlayer player, ServerLevel level, LivingEntity target) {
        if (target == player) {
            focusEye(player, level, player.getX(), player.getEyeY(), player.getZ(), player.getYRot(), 90.0F);
            return;
        }

        clearEye(player, level);
        LivingCameraView view = livingCameraView(level, target);
        ScryingCameraPayload.sendStart(player, view.x(), view.y(), view.z(), view.yaw(), view.pitch());
    }

    private static LivingCameraView livingCameraView(ServerLevel level, LivingEntity target) {
        float yaw = target.getYRot();
        double yawRadians = Math.toRadians(yaw);
        Vec3 forward = new Vec3(-Mth.sin((float) yawRadians), 0.0D, Mth.cos((float) yawRadians));
        Vec3 focus = new Vec3(target.getX(), target.getEyeY() - 0.2D, target.getZ());
        Vec3 camera = focus.subtract(forward.scale(LIVING_CAMERA_DISTANCE)).add(0.0D, LIVING_CAMERA_HEIGHT, 0.0D);
        camera = liftAboveTerrain(level, camera);
        camera = pullInFrontOfObstruction(level, target, focus, camera);
        Vec3 toFocus = focus.subtract(camera);
        double horizontal = Math.sqrt(toFocus.x * toFocus.x + toFocus.z * toFocus.z);
        float pitch = (float) (-Math.toDegrees(Math.atan2(toFocus.y, horizontal)));
        return new LivingCameraView(camera.x, camera.y, camera.z, yaw, pitch);
    }

    private static Vec3 liftAboveTerrain(ServerLevel level, Vec3 camera) {
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(camera.x), Mth.floor(camera.z));
        double safeY = groundY + LIVING_CAMERA_GROUND_CLEARANCE;
        return camera.y < safeY ? new Vec3(camera.x, safeY, camera.z) : camera;
    }

    private static Vec3 pullInFrontOfObstruction(ServerLevel level, LivingEntity target, Vec3 focus, Vec3 camera) {
        BlockHitResult hit = level.clip(new ClipContext(focus, camera,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return camera;
        }

        Vec3 fromHitToFocus = focus.subtract(hit.getLocation());
        if (fromHitToFocus.lengthSqr() < 0.0001D) {
            return liftAboveTerrain(level, focus.add(0.0D, 0.35D, 0.0D));
        }
        Vec3 safeCamera = hit.getLocation().add(fromHitToFocus.normalize().scale(LIVING_CAMERA_OBSTRUCTION_MARGIN));
        return liftAboveTerrain(level, safeCamera);
    }

    public static void focusEye(ServerPlayer player, ServerLevel level,
                                double x, double y, double z, float yaw, float pitch) {
        ScryingEyeEntity eye = currentEye(player, level);
        if (eye == null) {
            eye = ScryingEyeEntity.spawn(level, x, y, z, yaw, pitch);
            if (eye == null) {
                ScryingCameraPayload.sendStop(player);
                return;
            }
            EYES_BY_PLAYER.put(player.getUUID(), eye);
        }

        eye.refreshLife();
        ScryingCameraPayload.sendStart(player, eye);
    }

    public static void stop(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            clearEye(player, level);
        }
        ScryingCameraPayload.sendStop(player);
    }

    private static ScryingEyeEntity currentEye(ServerPlayer player, ServerLevel level) {
        ScryingEyeEntity eye = EYES_BY_PLAYER.get(player.getUUID());
        if (eye == null) {
            return null;
        }
        if (!eye.isAlive() || eye.isRemoved() || eye.level() != level) {
            EYES_BY_PLAYER.remove(player.getUUID());
            return null;
        }
        return eye;
    }

    private static void clearEye(ServerPlayer player, ServerLevel level) {
        ScryingEyeEntity eye = EYES_BY_PLAYER.remove(player.getUUID());
        if (eye != null && !eye.isRemoved() && eye.level() == level) {
            eye.discard();
        }
    }

    private record LivingCameraView(double x, double y, double z, float yaw, float pitch) {}
}
