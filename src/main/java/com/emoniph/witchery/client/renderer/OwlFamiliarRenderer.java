package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.OwlModel;
import com.emoniph.witchery.entity.familiar.OwlFamiliarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class OwlFamiliarRenderer extends AgeableMobRenderer<OwlFamiliarEntity,
        OwlModel.OwlRenderState, OwlModel> {
    static final float SOURCE_SHADOW_RADIUS = 0.5F;
    static final float LEGACY_BASE_DELTA_Y = -0.0068125F;
    static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("witchery", "textures/entity/owl.png");
    private static final float[][] FLEECE_COLOR_TABLE = new float[][]{
            {1.0F, 1.0F, 1.0F}, {0.85F, 0.5F, 0.2F}, {0.7F, 0.3F, 0.85F},
            {0.4F, 0.6F, 0.85F}, {0.9F, 0.9F, 0.2F}, {0.5F, 0.8F, 0.1F},
            {0.95F, 0.5F, 0.65F}, {0.3F, 0.3F, 0.3F}, {0.6F, 0.6F, 0.6F},
            {0.3F, 0.5F, 0.6F}, {0.5F, 0.25F, 0.7F}, {0.2F, 0.3F, 0.7F},
            {0.4F, 0.3F, 0.2F}, {0.4F, 0.5F, 0.2F}, {0.6F, 0.2F, 0.2F},
            {0.1F, 0.1F, 0.1F}
    };

    public OwlFamiliarRenderer(EntityRendererProvider.Context context) {
        super(context,
                new OwlModel(context.bakeLayer(OwlModel.LAYER_LOCATION)),
                new OwlModel(context.bakeLayer(OwlModel.BABY_LAYER_LOCATION)),
                SOURCE_SHADOW_RADIUS);
    }

    @Override
    public OwlModel.OwlRenderState createRenderState() {
        return new OwlModel.OwlRenderState();
    }

    @Override
    public void extractRenderState(OwlFamiliarEntity entity, OwlModel.OwlRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 0.01D, entity.getZ());
        BlockState belowState = entity.level().getBlockState(below);
        state.landed = sourceLanded(entity.getDeltaMovement(),
                !belowState.getFluidState().isEmpty() || belowState.blocksMotion());
        state.sitting = entity.isOrderedToSit();
        state.featherColor = entity.getFeatherColor();
        state.tintColor = featherTint(state.featherColor);
    }

    @Override
    protected void scale(OwlModel.OwlRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, LEGACY_BASE_DELTA_Y, 0.0F);
    }

    @Override
    protected int getModelTint(OwlModel.OwlRenderState state) {
        return state.tintColor;
    }

    @Override
    public Identifier getTextureLocation(OwlModel.OwlRenderState state) {
        return TEXTURE;
    }

    static boolean sourceLanded(Vec3 velocity, boolean liquidOrSolidBelow) {
        return velocity.x == 0.0D && velocity.y == 0.0D && velocity.z == 0.0D && liquidOrSolidBelow;
    }

    static int featherTint(int featherColor) {
        int id = featherColor & 15;
        if (id == 0) {
            return -1;
        }
        float[] dye = FLEECE_COLOR_TABLE[id];
        return ARGB.color(255,
                toChannel(dye[0] * 0.15686274F + 0.41568628F),
                toChannel(dye[1] * 0.15686274F + 0.3137255F),
                toChannel(dye[2] * 0.15686274F + 0.24313726F));
    }

    private static int toChannel(float value) {
        return Mth.clamp((int) (value * 255.0F), 0, 255);
    }
}
