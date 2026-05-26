package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.IShearable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Boline — an iron-durability ritual knife that doubles as shears.
 *
 * Source: 1.7.10 ItemBoline extends ItemSword(IRON)
 *   - Acts as shears on IShearable entities (sheep, mushroom cow, snow golem…)
 *   - Faster break on cobwebs (15×), leaves (15×), vines (2×) — mirrors ShearsItem
 *   - Tooltip: "Like shears, but can harvest trapped plants and cobwebs."
 *   - Uncommon rarity
 */
public class BolineItem extends Item {

    public BolineItem(Properties properties) {
        super(properties.rarity(Rarity.UNCOMMON));
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        String tooltipText = Component.translatable("item.witchery.boline.tip").getString();
        for (String line : tooltipText.split("\\n")) {
            tooltip.accept(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }

    // ── Shear entities on right-click ────────────────────────────────────────

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity entity, InteractionHand hand) {
        if (entity instanceof IShearable target) {
            BlockPos pos = entity.blockPosition();
            boolean isClient = entity.level().isClientSide();
            if (target.isShearable(player, stack, entity.level(), pos)) {
                List<ItemStack> drops = target.onSheared(player, stack, entity.level(), pos);
                if (!isClient) {
                    for (ItemStack drop : drops) {
                        target.spawnShearedDrop((ServerLevel) entity.level(), pos, drop);
                    }
                }
                entity.gameEvent(GameEvent.SHEAR, player);
                if (!isClient) {
                    EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    stack.hurtAndBreak(1, player, slot);
                }
                return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        }
        return InteractionResult.PASS;
    }

    // ── Faster break speed on plant-like blocks ───────────────────────────────

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Mirror ShearsItem speeds so boline is a usable harvest tool
        if (state.is(Blocks.COBWEB)) {
            return 15.0f;
        }
        if (state.is(net.minecraft.tags.BlockTags.LEAVES)) {
            return 15.0f;
        }
        if (state.is(net.minecraft.tags.BlockTags.WOOL)) {
            return 5.0f;
        }
        if (state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN)) {
            return 2.0f;
        }
        // Grass-like (for mutandis / crop blocks)
        if (state.is(net.minecraft.tags.BlockTags.SWORD_EFFICIENT)) {
            return 1.5f;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility)
                || super.canPerformAction(stack, itemAbility);
    }
}
