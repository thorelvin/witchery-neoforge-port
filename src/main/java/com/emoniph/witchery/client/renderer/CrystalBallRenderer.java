package com.emoniph.witchery.client.renderer;

import com.emoniph.witchery.blocks.entity.CrystalBallBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;

public class CrystalBallRenderer implements BlockEntityRenderer<CrystalBallBlockEntity, BlockEntityRenderState> {
    public CrystalBallRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public BlockEntityRenderState createRenderState() { return new BlockEntityRenderState(); }

    @Override
    public void submit(BlockEntityRenderState state, PoseStack ps, SubmitNodeCollector collector, CameraRenderState camera) {
        // Static block model now provides the placed crystal ball visuals so item and block match.
    }
}
