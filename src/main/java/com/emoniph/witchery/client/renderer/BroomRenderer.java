package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BroomModel;
import com.emoniph.witchery.entity.BroomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class BroomRenderer extends MobRenderer<BroomEntity, BroomRenderer.BroomRenderState, BroomModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LIVING_RENDERER_VERTICAL_COMPENSATION = 0.501F;

    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/broom.png");
    private static final float[][] FLEECE_COLOR_TABLE = new float[][]{
            {1.0F, 1.0F, 1.0F},
            {0.85F, 0.5F, 0.2F},
            {0.7F, 0.3F, 0.85F},
            {0.4F, 0.6F, 0.85F},
            {0.9F, 0.9F, 0.2F},
            {0.5F, 0.8F, 0.1F},
            {0.95F, 0.5F, 0.65F},
            {0.3F, 0.3F, 0.3F},
            {0.6F, 0.6F, 0.6F},
            {0.3F, 0.5F, 0.6F},
            {0.5F, 0.25F, 0.7F},
            {0.2F, 0.3F, 0.7F},
            {0.4F, 0.3F, 0.2F},
            {0.4F, 0.5F, 0.2F},
            {0.6F, 0.2F, 0.2F},
            {0.1F, 0.1F, 0.1F}
    };

    public BroomRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BroomModel(ctx.bakeLayer(BroomModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
        this.model.setBristlesVisible(false);
        this.addLayer(new BroomBristleLayer(this, ctx));
    }

    @Override public BroomRenderState createRenderState() { return new BroomRenderState(); }

    @Override
    public void extractRenderState(BroomEntity entity, BroomRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.enchanted = entity.isEnchanted();
        state.brushColor = entity.getBrushColor();
        state.hitWobbleDegrees = sourceHitWobble(entity.getTimeSinceHit(), entity.getDamageTaken(),
                entity.getForwardDirection(), partialTick);
        Entity passenger = entity.getFirstPassenger();
        if (passenger instanceof LivingEntity rider) {
            state.bodyRot = rider.getYRot(partialTick);
            state.yRot = 0.0F;
        }
        if (state.enchanted) {
            state.lightCoords = LightTexture.FULL_BRIGHT;
        }
    }

    @Override public Identifier getTextureLocation(BroomRenderState state) { return TEXTURE; }

    @Override
    protected void setupRotations(BroomRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        applySourceRotations(poseStack, bodyRot, state.hitWobbleDegrees);
    }

    @Override
    protected void scale(BroomRenderState state, PoseStack poseStack) {
        applyLivingRendererCompensation(poseStack);
    }

    static void applySourceRotations(PoseStack poseStack, float yaw, float hitWobbleDegrees) {
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - yaw));
        if (hitWobbleDegrees != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(hitWobbleDegrees));
        }
    }

    static void applyLivingRendererCompensation(PoseStack poseStack) {
        poseStack.translate(0.0F, LIVING_RENDERER_VERTICAL_COMPENSATION, 0.0F);
    }

    static float sourceHitWobble(int timeSinceHit, float damageTaken, int forwardDirection, float partialTick) {
        float hitTime = timeSinceHit - partialTick;
        float damage = Math.max(damageTaken - partialTick, 0.0F);
        return hitTime > 0.0F
                ? Mth.sin(hitTime) * hitTime * damage / 10.0F * forwardDirection
                : 0.0F;
    }

    static int brushColor(int brushColor) {
        int id = brushColor >= 0 && brushColor <= 15 ? brushColor : 12;
        float[] dye = FLEECE_COLOR_TABLE[id];
        return ARGB.color(255, toChannel(dye[0]), toChannel(dye[1]), toChannel(dye[2]));
    }

    private static int toChannel(float value) {
        return Mth.clamp((int)(value * 255.0F), 0, 255);
    }

    public static class BroomRenderState extends LivingEntityRenderState {
        public boolean enchanted;
        public int brushColor = -1;
        public float hitWobbleDegrees;
    }
}
