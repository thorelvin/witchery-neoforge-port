package com.emoniph.witchery.client.renderer;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A baked {@link BlockStateModel} for Wispy Cotton that renders the cotton's <em>disturbed</em>
 * appearance whenever the local client is in a nightmare ({@link SpiritWorldSpecialEffects#isNightmareLike()}),
 * and its ordinary wispy appearance otherwise. This is the visual half of the source rule that
 * harvesting cotton during a nightmare yields Disturbed Cotton — the same block, but the dream's mood
 * shows in the plant. Installed via {@code ModelEvent.ModifyBakingResult}.
 *
 * <p>Returning {@code null} from {@code createGeometryKey} (the Neo default) keeps the model dynamic;
 * a chunk re-mesh on nightmare transitions (see {@code WitcheryClientEvents}) makes the swap visible.
 */
public class NightmareCottonModel implements BlockStateModel {

    private final BlockStateModel wispy;
    private final BlockStateModel disturbed;

    public NightmareCottonModel(BlockStateModel wispy, BlockStateModel disturbed) {
        this.wispy = wispy;
        this.disturbed = disturbed;
    }

    private BlockStateModel active() {
        return SpiritWorldSpecialEffects.isNightmareLike() ? disturbed : wispy;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockModelPart> parts) {
        active().collectParts(random, parts);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state,
                             RandomSource random, List<BlockModelPart> parts) {
        active().collectParts(level, pos, state, random, parts);
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return active().particleIcon();
    }

    @Override
    public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return active().particleIcon(level, pos, state);
    }
}
