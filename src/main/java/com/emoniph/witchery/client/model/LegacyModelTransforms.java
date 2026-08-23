package com.emoniph.witchery.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Coordinate adapters for 1.7.10 {@code ModelRenderer} parts. */
public final class LegacyModelTransforms {
    private LegacyModelTransforms() {
    }

    /**
     * Adapts a part rendered with {@code ModelRenderer.render}. Both renderers compose rotations as
     * Z, Y, X, and both store pivots in model pixels, so the values transfer without conversion.
     */
    public static PartPose render(float x, float y, float z, float xRot, float yRot, float zRot) {
        return PartPose.offsetAndRotation(x, y, z, xRot, yRot, zRot);
    }

    /**
     * Adapts a part rendered with {@code ModelRenderer.renderWithRotation}. The legacy method
     * composes rotations as Y, X, Z; modern {@link ModelPart} always evaluates Euler angles as
     * Z, Y, X, so preserve the source quaternion and recover equivalent modern Euler angles.
     */
    public static PartPose renderWithRotation(float x, float y, float z,
                                              float xRot, float yRot, float zRot) {
        Vector3f modernRotation = renderWithRotationEuler(xRot, yRot, zRot);
        return PartPose.offsetAndRotation(x, y, z,
                modernRotation.x, modernRotation.y, modernRotation.z);
    }

    public static void setRenderWithRotation(ModelPart part, float xRot, float yRot, float zRot) {
        Vector3f modernRotation = renderWithRotationEuler(xRot, yRot, zRot);
        part.setRotation(modernRotation.x, modernRotation.y, modernRotation.z);
    }

    static Vector3f renderWithRotationEuler(float xRot, float yRot, float zRot) {
        Quaternionf legacyRotation = new Quaternionf().rotateY(yRot).rotateX(xRot).rotateZ(zRot);
        return legacyRotation.getEulerAnglesZYX(new Vector3f());
    }
}
