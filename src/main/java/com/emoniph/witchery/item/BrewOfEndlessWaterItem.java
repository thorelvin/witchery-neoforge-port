package com.emoniph.witchery.item;

import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Source-style utility brew: a reusable, glowing bottle that fills cauldrons or
 * places water sources instead of being drunk like ordinary brew items.
 */
public class BrewOfEndlessWaterItem extends Item {
    private static final int SOURCE_USE_COUNT = 100;

    public BrewOfEndlessWaterItem(Properties properties) {
        super(properties.durability(SOURCE_USE_COUNT));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clickedPos = ctx.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        if (canFillCauldron(clickedState)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!WitcheryTerrainProtection.canTransform(ctx.getPlayer(), level, clickedPos)) {
                return InteractionResult.FAIL;
            }
            fillCauldron(level, clickedPos, clickedState);
            spendUse(ctx);
            playWaterEffect(level, clickedPos);
            awardUse(ctx.getPlayer());
            level.gameEvent(ctx.getPlayer(), GameEvent.BLOCK_CHANGE, clickedPos);
            return InteractionResult.CONSUME;
        }

        BlockPos placePos = clickedPos.relative(ctx.getClickedFace());
        if (!canPlaceWater(level, placePos)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockState placeState = level.getBlockState(placePos);
        if (!(placeState.isAir()
                ? WitcheryTerrainProtection.canPlace(ctx.getPlayer(), level, placePos)
                : WitcheryTerrainProtection.canTransform(ctx.getPlayer(), level, placePos))) {
            return InteractionResult.FAIL;
        }

        level.setBlockAndUpdate(placePos, Blocks.WATER.defaultBlockState());
        spendUse(ctx);
        playWaterEffect(level, placePos);
        awardUse(ctx.getPlayer());
        level.gameEvent(ctx.getPlayer(), GameEvent.FLUID_PLACE, placePos);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, tooltip, flag);
        int maxUses = stack.getMaxDamage();
        int usesLeft = Math.max(0, maxUses - stack.getDamageValue());
        tooltip.accept(Component.translatable("item.witchery.brew_of_endless_water.tip", usesLeft, maxUses)
                .withStyle(ChatFormatting.GRAY));
    }

    private static boolean canFillCauldron(BlockState state) {
        if (state.is(Blocks.CAULDRON)) {
            return true;
        }
        return state.is(Blocks.WATER_CAULDRON)
                && state.getValue(LayeredCauldronBlock.LEVEL) < 3;
    }

    private static void fillCauldron(Level level, BlockPos pos, BlockState current) {
        BlockState fullCauldron = Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 3);
        if (current.is(Blocks.WATER_CAULDRON)) {
            fullCauldron = current.setValue(LayeredCauldronBlock.LEVEL, 3);
        }
        level.setBlockAndUpdate(pos, fullCauldron);
    }

    private static boolean canPlaceWater(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.is(Blocks.WATER)
                && !state.hasBlockEntity()
                && (state.isAir() || state.canBeReplaced());
    }

    private static void spendUse(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        ctx.getItemInHand().hurtAndBreak(1, player, slot);
    }

    private static void playWaterEffect(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                    12, 0.35D, 0.2D, 0.35D, 0.02D);
        }
    }

    private void awardUse(Player player) {
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }
}
