package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.client.model.BroomModel;
import com.emoniph.witchery.entity.BroomEntity;
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
        super(ctx, new BroomModel(ctx.bakeLayer(BroomModel.LAYER_LOCATION)), 0.3F);
        this.model.setBristlesVisible(false);
        this.addLayer(new BroomBristleLayer(this, ctx));
    }

    @Override public BroomRenderState createRenderState() { return new BroomRenderState(); }

    @Override
    public void extractRenderState(BroomEntity entity, BroomRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.enchanted = entity.isEnchanted();
        state.brushColor = entity.getBrushColor();
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
    }
}
