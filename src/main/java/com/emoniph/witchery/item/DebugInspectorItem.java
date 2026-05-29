package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Debug Inspector — a QA wand that prints Witchery state to chat or the action bar.
 *
 * Right-click player  → full WitcheryPlayerData dump in chat.
 * Right-click altar   → altar power / structure / range / enhancement breakdown.
 * Right-click other   → entity type + health.
 * Use on air          → own active brew timers in the action bar (no chat scroll).
 */
public class DebugInspectorItem extends Item {

    public DebugInspectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx,
                                TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.witchery.debug_inspector.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }

    // ── Right-click entity ────────────────────────────────────────────────────

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                  LivingEntity entity, InteractionHand hand) {
        if (user.level().isClientSide()) return InteractionResult.SUCCESS;

        if (entity instanceof ServerPlayer target) {
            WitcheryPlayerData d = WitcheryAttachments.get(target);
            String msg = "[Inspector] " + target.getName().getString() + "\n"
                    + "  vampire=" + d.isVampire() + "  blood=" + d.getBloodLevel() + "/500\n"
                    + "  werewolf=" + d.isWerewolf()
                    + "  transform=" + String.format("%.2f", d.getTransformationLevel())
                    + "  wolfmastery=" + d.getWolfMastery() + "/3\n"
                    + "  soaring=" + tickStr(d.getSoaringTicks())
                    + "  depths=" + tickStr(d.getDepthsTicks())
                    + "  grotesque=" + tickStr(d.getGrotesqueTicks()) + "\n"
                    + "  ghostWalker=" + d.isGhostWalkerActive()
                    + "  infusion=" + d.getInfusionType()
                    + "  energy=" + d.getInfusionEnergy() + "/" + d.getInfusionMaxEnergy() + "\n"
                    + "  creaturePower=" + (d.getCreaturePowerType().isEmpty() ? "none" : d.getCreaturePowerType())
                    + "  charges=" + d.getCreaturePowerCharges() + "/100\n"
                    + "  familiar=" + d.getFamiliarUUID().map(u -> u.toString().substring(0, 8) + "..." + " (" + d.getFamiliarType() + ")").orElse("none") + "\n"
                    + "  inSpiritWorld=" + d.isInSpiritWorld()
                    + (d.isInSpiritWorld() ? " (" + (d.isSpiritWorldDream() ? "dream" : "nightmare") + "  ghost=" + d.isManifestedGhost() + ")" : "");
            user.displayClientMessage(Component.literal(msg), false);
        } else {
            // Non-player entity — show type + health
            String type = entity.getType().toShortString();
            String msg = "[Inspector] " + type
                    + "  hp=" + String.format("%.1f", entity.getHealth())
                    + "/" + String.format("%.1f", entity.getMaxHealth());
            user.displayClientMessage(Component.literal(msg), false);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    // ── Right-click block ─────────────────────────────────────────────────────

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (level.isClientSide() || player == null) return InteractionResult.PASS;

        // Altar inspection
        if (level.getBlockState(pos).is(WitcheryBlocks.ALTAR.get())
                && level.getBlockEntity(pos) instanceof AltarBlockEntity altar) {
            String msg = "[Inspector] Altar @ " + pos.toShortString() + "\n"
                    + "  power=" + altar.getPower() + "/" + altar.getEffectiveMaxPower()
                    + "  (raw max=" + altar.getMaxPower() + ")\n"
                    + "  recharge=" + altar.getEffectiveRecharge() + "/t"
                    + "  range=" + altar.getRange()
                    + "  enhancement=" + altar.getEnhancementLevel() + "\n"
                    + "  validStructure=" + altar.hasValidStructure();
            player.displayClientMessage(Component.literal(msg), false);
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.PASS;
    }

    // ── Use on air → own brew state in action bar ─────────────────────────────

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        WitcheryPlayerData d = WitcheryAttachments.get(player);

        String bar = "Soaring:" + tickStr(d.getSoaringTicks())
                + " | Depths:" + tickStr(d.getDepthsTicks())
                + " | Grotesque:" + tickStr(d.getGrotesqueTicks())
                + " | Ghost:" + (d.isGhostWalkerActive() ? "ON" : "off")
                + " | " + d.getInfusionType();

        // true = action bar, doesn't scroll chat
        player.displayClientMessage(Component.literal(bar), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static String tickStr(int t) {
        return t == 0 ? "0t" : t + "t(" + (t / 20) + "s)";
    }
}
