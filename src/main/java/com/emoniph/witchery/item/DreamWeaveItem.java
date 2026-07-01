package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.DreamCatcherBlock;
import com.emoniph.witchery.blocks.DreamWeaveType;
import com.emoniph.witchery.blocks.entity.DreamCatcherBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/** Places the source-style Dream Catcher host block carrying this weave. */
public class DreamWeaveItem extends Item {
    private final DreamWeaveType weaveType;

    public DreamWeaveItem(DreamWeaveType weaveType, Properties props) {
        super(props);
        this.weaveType = weaveType;
    }

    public DreamWeaveType getWeaveType() { return weaveType; }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.getBlockEntity(ctx.getClickedPos()) instanceof DreamCatcherBlockEntity be) {
            if (be.getWeave() != null) {
                fail(level, ctx.getClickedPos(), ctx.getPlayer(), "witchery.dream_weaver.already_woven");
                return InteractionResult.FAIL;
            }
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!WitcheryTerrainProtection.canTransform(ctx.getPlayer(), level, ctx.getClickedPos())) {
                return InteractionResult.FAIL;
            }
            be.setWeave(weaveType);
            playWeaveEffects(level, ctx.getClickedPos(), true);
            if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative())
                ctx.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS_SERVER;
        }

        Direction face = ctx.getClickedFace();
        if (!face.getAxis().isHorizontal()) {
            return InteractionResult.PASS;
        }

        BlockPos placePos = ctx.getClickedPos().relative(face);
        BlockState placedState = WitcheryBlocks.DREAM_CATCHER.get()
                .defaultBlockState()
                .setValue(DreamCatcherBlock.FACING, face)
                .setValue(DreamCatcherBlock.WEAVE, weaveType.id + 1);
        if (!level.getBlockState(placePos).canBeReplaced() || !placedState.canSurvive(level, placePos)) {
            fail(level, placePos, ctx.getPlayer(), "witchery.dream_weaver.no_wall");
            return InteractionResult.FAIL;
        }
        if (!(level.getBlockState(placePos).isAir()
                ? WitcheryTerrainProtection.canPlace(ctx.getPlayer(), level, placePos)
                : WitcheryTerrainProtection.canTransform(ctx.getPlayer(), level, placePos))) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!level.setBlock(placePos, placedState, 3)) {
            return InteractionResult.FAIL;
        }
        if (level.getBlockEntity(placePos) instanceof DreamCatcherBlockEntity be) {
            be.setWeave(weaveType);
        }
        playWeaveEffects(level, placePos, true);
        if (ctx.getPlayer() != null && !ctx.getPlayer().isCreative())
            ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.witchery.dream_weaver.tip").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.witchery.dream_weaver.tip." + weaveType.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static void fail(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, String key) {
        if (level.isClientSide()) {
            return;
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        playWeaveEffects(level, pos, false);
    }

    private static void playWeaveEffects(Level level, BlockPos pos, boolean success) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, pos,
                success ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.NOTE_BLOCK_BASS.value(),
                SoundSource.BLOCKS, success ? 0.75F : 0.55F, success ? 1.45F : 0.6F);
        serverLevel.sendParticles(success ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                success ? 16 : 6, 0.2D, 0.25D, 0.2D, success ? 0.18D : 0.01D);
    }
}
