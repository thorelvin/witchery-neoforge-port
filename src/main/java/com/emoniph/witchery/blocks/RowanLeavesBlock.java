package com.emoniph.witchery.blocks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class RowanLeavesBlock extends UntintedParticleLeavesBlock {

    public RowanLeavesBlock(BlockBehaviour.Properties properties) {
        super(0.02f, ParticleTypes.PALE_OAK_LEAVES, properties);
    }
}
