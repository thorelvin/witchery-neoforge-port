package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.FlyingMonkeyModel;
import com.emoniph.witchery.entity.FlyingMonkeyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FlyingMonkeyRenderer extends MobRenderer<FlyingMonkeyEntity, FlyingMonkeyModel.FlyingMonkeyRenderState, FlyingMonkeyModel> {

    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("witchery", "textures/entity/flying_monkey.png");
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

    public FlyingMonkeyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new FlyingMonkeyModel(ctx.bakeLayer(FlyingMonkeyModel.LAYER_LOCATION)), SOURCE_SHADOW_RADIUS);
    }

    @Override
    public FlyingMonkeyModel.FlyingMonkeyRenderState createRenderState() {
        return new FlyingMonkeyModel.FlyingMonkeyRenderState();
    }

    @Override
    public void extractRenderState(FlyingMonkeyEntity entity, FlyingMonkeyModel.FlyingMonkeyRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.sitting = entity.isOrderedToSit();
        BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 0.01D, entity.getZ());
        BlockState belowState = entity.level().getBlockState(below);
        state.landed = sourceLanded(entity.getDeltaMovement(),
                !belowState.getFluidState().isEmpty() || belowState.blocksMotion());
        state.attackTimer = entity.getAttackTimer();
        state.tintColor = featherTint(entity.getDyeColor());
    }

    @Override
    protected void scale(FlyingMonkeyModel.FlyingMonkeyRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    @Override
    public Identifier getTextureLocation(FlyingMonkeyModel.FlyingMonkeyRenderState state) { return TEXTURE; }

    @Override
    protected int getModelTint(FlyingMonkeyModel.FlyingMonkeyRenderState state) {
        return state.tintColor;
    }

    static boolean sourceLanded(Vec3 velocity, boolean liquidOrSolidBelow) {
        return velocity.x == 0.0D && velocity.y == 0.0D && velocity.z == 0.0D && liquidOrSolidBelow;
    }

    static int featherTint(DyeColor color) {
        int id = color.getId() & 15;
        if (id == 0) {
            return -1;
        }

        float[] dye = FLEECE_COLOR_TABLE[id];
        int red = toChannel(dye[0] * 0.15686274F + 0.41568628F);
        int green = toChannel(dye[1] * 0.15686274F + 0.3137255F);
        int blue = toChannel(dye[2] * 0.15686274F + 0.24313726F);
        return ARGB.color(255, red, green, blue);
    }

    private static int toChannel(float value) {
        return Mth.clamp((int)(value * 255.0F), 0, 255);
    }
}
