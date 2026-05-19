package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.KettleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public class KettleRenderer implements BlockEntityRenderer<KettleBlockEntity, KettleRenderer.KettleRenderState> {

    private static final Identifier LIQUID_TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/block/kettle.png");

    private static final float INNER_HW = 0.25f;
    private static final float KETTLE_BOTTOM_Y = 0.08f;
    private static final float KETTLE_FULL_Y   = 0.25f;
    private static final float LIQ_U0 = 0f,    LIQ_U1 = 0.25f;
    private static final float LIQ_V0 = 0.25f, LIQ_V1 = 0.375f;

    public static class KettleRenderState extends BlockEntityRenderState {
        public int waterLevel = 0;
        public int color = 0;
        public boolean brewing = false;
        public Level level = null;
        public BlockPos bePos = BlockPos.ZERO;
    }

    public KettleRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public KettleRenderState createRenderState() { return new KettleRenderState(); }

    @Override
    public void extractRenderState(KettleBlockEntity be, KettleRenderState state, float partialTick,
                                   Vec3 offset, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, offset, crumbling);
        state.waterLevel = be.getWaterLevel();
        state.color = be.getColor();
        state.brewing = be.isBrewing();
        state.level = be.getLevel();
        state.bePos = be.getBlockPos();
    }

    @Override
    public void submit(KettleRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.waterLevel > 0) {
            float surfaceY = KETTLE_BOTTOM_Y + (KETTLE_FULL_Y - KETTLE_BOTTOM_Y) * (state.waterLevel / 3f);
            int liquidColor = (state.color != 0) ? state.color : 0x2050CC;
            renderLiquidSurface(ps, collector, surfaceY, liquidColor, state.lightCoords);
        }

        if (state.brewing && state.level != null && state.level.isClientSide()) {
            spawnBubbles(state.level, state.bePos);
        }
    }

    private void renderLiquidSurface(PoseStack ps, SubmitNodeCollector collector, float surfaceY, int color, int light) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8)  & 0xFF) / 255f;
        float b = ((color)       & 0xFF) / 255f;
        float hw = INNER_HW;

        ps.pushPose();
        ps.translate(0.5, surfaceY, 0.5);

        collector.submitCustomGeometry(ps, RenderTypes.entityTranslucent(LIQUID_TEXTURE), (pose, vc) -> {
            Matrix4f m = pose.pose();
            vc.addVertex(m, -hw, 0, -hw).setColor(r, g, b, 0.85f).setUv(LIQ_U0, LIQ_V0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            vc.addVertex(m, -hw, 0,  hw).setColor(r, g, b, 0.85f).setUv(LIQ_U0, LIQ_V1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            vc.addVertex(m,  hw, 0,  hw).setColor(r, g, b, 0.85f).setUv(LIQ_U1, LIQ_V1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
            vc.addVertex(m,  hw, 0, -hw).setColor(r, g, b, 0.85f).setUv(LIQ_U1, LIQ_V0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
        });
        ps.popPose();
    }

    private void spawnBubbles(Level level, BlockPos pos) {
        RandomSource rand = level.random;
        if (rand.nextInt(5) != 0) return;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.25, cz = pos.getZ() + 0.5;
        double ox = (rand.nextDouble() - 0.5) * 0.35, oz = (rand.nextDouble() - 0.5) * 0.35;
        level.addParticle(ParticleTypes.SMOKE, cx + ox, cy, cz + oz, 0, 0.04, 0);
        if (rand.nextInt(3) == 0) level.addParticle(ParticleTypes.SPLASH, cx + ox, cy + 0.02, cz + oz, 0, 0.08, 0);
        if (rand.nextInt(8) == 0) level.addParticle(ParticleTypes.POOF, cx + ox * 0.5, cy, cz + oz * 0.5, 0, 0.02, 0);
    }

    @Override
    public boolean shouldRenderOffScreen() { return true; }
}
