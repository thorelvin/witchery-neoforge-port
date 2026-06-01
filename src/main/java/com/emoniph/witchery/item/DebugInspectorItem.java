package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlockTags;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.ritual.AltarPowerSourceLookup;
import com.emoniph.witchery.ritual.RitualCircleDetector;
import com.emoniph.witchery.ritual.RitualExecutor;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Debug Inspector — a QA wand that prints Witchery state to chat or the action bar.
 *
 * Right-click player  → full WitcheryPlayerData dump in chat.
 * Right-click altar   → altar power / structure / range / enhancement breakdown.
 * Right-click block   → ritual circle report using the clicked block as center.
 * Right-click other   → entity type + health.
 * Use on air          → own active brew timers and any center underfoot in the action bar.
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

        player.displayClientMessage(Component.literal(ritualCenterMessage(level, pos)), false);
        return InteractionResult.SUCCESS_SERVER;
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

        String center = ritualCenterSummaryUnderPlayer(level, player);
        if (!center.isEmpty()) {
            bar += " | Center:" + center;
        }

        // true = action bar, doesn't scroll chat
        player.displayClientMessage(Component.literal(bar), true);
        return InteractionResult.SUCCESS_SERVER;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static String tickStr(int t) {
        return t == 0 ? "0t" : t + "t(" + (t / 20) + "s)";
    }

    private static String ritualCenterMessage(Level level, BlockPos clicked) {
        CenterScan scan = scanRitualCenter(level, clicked);
        BlockPos center = scan.center();
        int[] variants = scan.variants();
        String rings = describeDetectedRings(variants);
        String origin = center.equals(clicked) ? "" : " (clicked glyph @ " + clicked.toShortString() + ")";
        String altar = describeAvailableRitualAltar(level, center);
        String active = RitualExecutor.describeActiveRitual(level, center);
        if (rings.isEmpty()) {
            return "[Inspector] Ritual center @ " + center.toShortString() + origin + "\n"
                    + "  no complete rings\n"
                    + "  " + active + "\n"
                    + "  " + altar;
        }
        return "[Inspector] Ritual center @ " + center.toShortString() + origin + "\n"
                + "  rings=" + rings + "\n"
                + "  " + active + "\n"
                + "  " + altar;
    }

    private static String ritualCenterSummaryUnderPlayer(Level level, Player player) {
        BlockPos center = player.blockPosition().below();
        String rings = describeDetectedRings(scanRitualCenter(level, center).variants());
        if (!rings.isEmpty()) {
            return rings + " " + RitualExecutor.describeActiveRitual(level, center);
        }
        center = player.blockPosition();
        rings = describeDetectedRings(scanRitualCenter(level, center).variants());
        return rings.isEmpty() ? "" : rings + " " + RitualExecutor.describeActiveRitual(level, center);
    }

    private static CenterScan scanRitualCenter(Level level, BlockPos clicked) {
        int[] variants = RitualCircleDetector.detectAllVariants(level, clicked);
        if (hasDetectedRing(variants) || !level.getBlockState(clicked).is(WitcheryBlockTags.CHALK_CIRCLES)) {
            return new CenterScan(clicked, variants);
        }

        BlockPos floorCenter = clicked.below();
        int[] floorVariants = RitualCircleDetector.detectAllVariants(level, floorCenter);
        return hasDetectedRing(floorVariants)
                ? new CenterScan(floorCenter, floorVariants)
                : new CenterScan(clicked, variants);
    }

    private static boolean hasDetectedRing(int[] variants) {
        for (int variant : variants) {
            if (variant != 0) {
                return true;
            }
        }
        return false;
    }

    private static String describeDetectedRings(int[] variants) {
        List<String> parts = new ArrayList<>();
        appendDetectedRing(parts, "small", variants[0]);
        appendDetectedRing(parts, "medium", variants[1]);
        appendDetectedRing(parts, "large", variants[2]);
        return String.join(", ", parts);
    }

    private static void appendDetectedRing(List<String> parts, String size, int variant) {
        if (variant != 0) {
            parts.add(size + " " + chalkVariantName(variant));
        }
    }

    private static String chalkVariantName(int variant) {
        return switch (variant) {
            case 1 -> "ritual";
            case 2 -> "otherwhere";
            case 3 -> "infernal";
            case 4 -> "golden";
            default -> "mixed";
        };
    }

    private static String describeAvailableRitualAltar(Level level, BlockPos center) {
        AltarBlockEntity altar = AltarPowerSourceLookup.findClosest(level, center);
        if (altar == null) {
            return "altar=none in range"
                    + " (base " + AltarPowerSourceLookup.BASE_ALTAR_RANGE
                    + ", upgraded " + AltarPowerSourceLookup.MAX_ALTAR_RANGE + ")";
        }

        BlockPos pos = altar.getBlockPos();
        double distance = Math.sqrt(AltarPowerSourceLookup.distanceSqr(center, pos));
        return "altar=" + altar.getPower() + "/" + altar.getEffectiveMaxPower()
                + " @ " + pos.toShortString()
                + " distance=" + String.format(Locale.ROOT, "%.1f", distance) + "/" + altar.getRange()
                + " recharge=" + altar.getEffectiveRecharge() + "/t"
                + " range=" + altar.getRange();
    }

    private record CenterScan(BlockPos center, int[] variants) {}
}
