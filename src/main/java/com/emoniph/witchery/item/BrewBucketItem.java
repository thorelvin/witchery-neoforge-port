package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.BrewFluidBlock;
import com.emoniph.witchery.brewing.BrewContents;
import com.emoniph.witchery.brewing.Brews;
import com.emoniph.witchery.init.WitcheryDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A bucketful of composed custom brew — the fluid-container form of a brew, scooped from a brewed
 * Witch's Cauldron (the whole batch at once). Pouring it out places a moving brew-fluid block
 * (liquid, or gas if the brew is gaseous) that spreads and applies the brew's effects on contact,
 * then leaves an empty Bucket.
 *
 * Source parity: 1.7.10 {@code ItemBrewBucket} (the FluidBrew container).
 */
public class BrewBucketItem extends Item {

    public BrewBucketItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    private static BrewContents contents(ItemStack stack) {
        return stack.getOrDefault(WitcheryDataComponents.BREW_CONTENTS.get(), BrewContents.EMPTY);
    }

    @Override
    public Component getName(ItemStack stack) {
        BrewContents brew = contents(stack);
        return brew.isEmpty() ? super.getName(stack) : brew.displayName();
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        BrewContents brew = contents(stack);
        if (brew.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Player player = ctx.getPlayer();
        Brews.Dispersal dispersal = brew.bottledDispersal() == Brews.Dispersal.GAS
                ? Brews.Dispersal.GAS : Brews.Dispersal.LIQUID;
        BlockPos clicked = ctx.getClickedPos();
        BlockPos target = BrewFluidBlock.canDisplace(level.getBlockState(clicked))
                ? clicked : clicked.relative(ctx.getClickedFace());

        if (!Brews.placeMovingBrewFluid(serverLevel, target, brew.items(), dispersal, player, true)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("witchery.brew.bucket.blocked")
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            serverLevel.playSound(null, target, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.45F, 1.4F);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                    8, 0.18D, 0.18D, 0.18D, 0.01D);
            return InteractionResult.FAIL;
        }
        level.playSound(null, target, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        serverLevel.sendParticles(new DustParticleOptions(brew.color(), dispersal == Brews.Dispersal.GAS ? 1.0F : 0.7F),
                target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D,
                dispersal == Brews.Dispersal.GAS ? 18 : 10, 0.25D, 0.25D, 0.25D, 0.02D);

        if (player == null || !player.getAbilities().instabuild) {
            ItemStack emptied = new ItemStack(Items.BUCKET);
            stack.shrink(1);
            if (player != null) {
                if (stack.isEmpty()) {
                    player.setItemInHand(ctx.getHand(), emptied);
                } else if (!player.getInventory().add(emptied)) {
                    player.drop(emptied, false);
                }
            }
        }
        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        BrewContents brew = contents(stack);
        if (brew.isEmpty()) {
            return;
        }
        tooltip.accept(Component.translatable(brew.bottledDispersal() == Brews.Dispersal.GAS
                ? "witchery.brew.dispersal.gas"
                : "witchery.brew.dispersal.liquid").withStyle(ChatFormatting.GRAY));
        for (var effect : Brews.buildEffects(brew.items())) {
            Component name = Component.translatable(effect.getEffect().value().getDescriptionId());
            if (effect.getAmplifier() > 0) {
                name = Component.translatable("potion.withAmplifier", name,
                        Component.translatable("potion.potency." + effect.getAmplifier()));
            }
            tooltip.accept(name.copy().withStyle(ChatFormatting.BLUE));
        }
        for (var action : Brews.buildActions(brew.items())) {
            Component name = Component.translatable(action.nameKey());
            if (action.strength() > 0) {
                name = Component.translatable("potion.withAmplifier", name,
                        Component.translatable("potion.potency." + action.strength()));
            }
            tooltip.accept(name.copy().withStyle(ChatFormatting.BLUE));
        }
    }
}
