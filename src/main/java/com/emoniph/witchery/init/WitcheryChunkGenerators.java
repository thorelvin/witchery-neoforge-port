package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.world.MirrorWorldChunkGenerator;
import com.emoniph.witchery.world.TormentChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WitcheryChunkGenerators {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, WitcheryMod.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<TormentChunkGenerator>> TORMENT_MAZE =
            CHUNK_GENERATORS.register("torment_maze", () -> TormentChunkGenerator.CODEC);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<MirrorWorldChunkGenerator>> MIRROR_WORLD =
            CHUNK_GENERATORS.register("mirror_world", () -> MirrorWorldChunkGenerator.CODEC);
}
