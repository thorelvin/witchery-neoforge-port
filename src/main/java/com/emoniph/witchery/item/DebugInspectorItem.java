package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.AltarBlockEntity;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlockTags;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.ritual.AltarPowerSourceLookup;
import com.emoniph.witchery.ritual.RitualCircleDetector;
import com.emoniph.witchery.ritual.RitualExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
    private static final int VARIANT_RITUAL = 1;
    private static final int VARIANT_INFERNAL = 3;
    private static final int BLIGHT_RITE_POWER = 3000;
    private static final double INSPECTOR_DROPPED_OFFERING_RADIUS = 4.0D;
    private static final double BIND_SPECTRAL_STONE_RADIUS = 16.0D;

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
                    + "  wolflevel=" + d.getWerewolfLevel() + "/10 (" + d.getWolfForm() + ")\n"
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
        BlockState state = level.getBlockState(pos);
        if (state.is(WitcheryBlocks.ALTAR.get())
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

        if (!state.is(WitcheryBlockTags.CHALK_CIRCLES)) {
            String msg = "[Inspector] Block @ " + pos.toShortString() + "\n"
                    + "  block=" + BuiltInRegistries.BLOCK.getKey(state.getBlock())
                    + "\n"
                    + "  no ritual glyph selected";
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
        String blight = describeBlightReadiness(level, center, variants);
        String dropped = describeDroppedRiteReadiness(level, center, variants);
        String readinessLine = readinessLine(blight, dropped);
        if (rings.isEmpty()) {
            return "[Inspector] Ritual center @ " + center.toShortString() + origin + "\n"
                    + "  no complete rings\n"
                    + "  " + active + readinessLine + "\n"
                    + "  " + altar;
        }
        return "[Inspector] Ritual center @ " + center.toShortString() + origin + "\n"
                + "  rings=" + rings + "\n"
                + "  " + active + readinessLine + "\n"
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

    private static String describeBlightReadiness(Level level, BlockPos center, int[] variants) {
        if (variants.length < 2 || variants[1] != VARIANT_INFERNAL) {
            return "";
        }

        List<String> missing = new ArrayList<>();
        appendMissingOffering(missing, "Wither Rose", level, center, stack -> stack.is(Items.WITHER_ROSE));
        appendMissingOffering(missing, "Charged Attuned Stone", level, center,
                stack -> stack.is(WitcheryItems.ATTUNED_STONE_CHARGED.get()));
        appendMissingOffering(missing, "Redstone Soup", level, center,
                stack -> stack.is(WitcheryItems.REDSTONE_SOUP.get()));
        appendMissingOffering(missing, "Reek of Misfortune", level, center,
                stack -> stack.is(WitcheryItems.REEK_OF_MISFORTUNE.get()));
        appendMissingOffering(missing, "Fermented Spider Eye", level, center,
                stack -> stack.is(Items.FERMENTED_SPIDER_EYE));
        appendMissingOffering(missing, "Poisonous Potato", level, center,
                stack -> stack.is(Items.POISONOUS_POTATO));
        appendMissingOffering(missing, "Rotten Flesh", level, center,
                stack -> stack.is(Items.ROTTEN_FLESH));
        appendMissingOffering(missing, "Diamond", level, center,
                stack -> stack.is(Items.DIAMOND));
        if (!hasBlightAltarPower(level, center)) {
            missing.add(BLIGHT_RITE_POWER + " altar power");
        }
        return missing.isEmpty() ? "blight=ready" : "blight missing=" + String.join(", ", missing);
    }

    private static String describeDroppedRiteReadiness(Level level, BlockPos center, int[] variants) {
        if (variants.length < 2 || variants[1] != VARIANT_RITUAL) {
            return "";
        }

        boolean blankStone = hasDroppedOffering(level, center,
                stack -> stack.is(WitcheryItems.SPECTRAL_STONE.get()) && !SpectralStoneItem.isBound(stack));
        boolean spectralDust = hasDroppedOffering(level, center, stack -> stack.is(WitcheryItems.SPECTRAL_DUST.get()));
        boolean boline = hasDroppedOffering(level, center, stack -> stack.is(WitcheryItems.BOLINE.get()));
        SpectralBindCandidate candidate = nearestSpectralBindCandidate(level, center);

        if (!blankStone && !spectralDust && !boline && candidate.count() <= 0) {
            return "";
        }

        List<String> missing = new ArrayList<>();
        if (!blankStone) {
            missing.add("Blank Spectral Stone");
        }
        if (!spectralDust) {
            missing.add("Spectral Dust");
        }
        if (!boline) {
            missing.add("Boline");
        }
        if (candidate.count() <= 0) {
            missing.add("spectre, banshee, or poltergeist within 16 blocks");
        }

        return missing.isEmpty()
                ? "bind spectral stone=ready (" + candidate.name() + " x" + candidate.count() + ")"
                : "bind spectral stone missing=" + String.join(", ", missing);
    }

    private static String readinessLine(String... lines) {
        List<String> present = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                present.add(line);
            }
        }
        return present.isEmpty() ? "" : "\n  " + String.join("\n  ", present);
    }

    private static void appendMissingOffering(List<String> missing, String name, Level level, BlockPos center,
                                              Predicate<ItemStack> matcher) {
        if (!hasDroppedOffering(level, center, matcher)) {
            missing.add(name);
        }
    }

    private static boolean hasDroppedOffering(Level level, BlockPos center, Predicate<ItemStack> matcher) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        double radiusSq = INSPECTOR_DROPPED_OFFERING_RADIUS * INSPECTOR_DROPPED_OFFERING_RADIUS;
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(INSPECTOR_DROPPED_OFFERING_RADIUS);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            if (matcher.test(itemEntity.getItem()) && itemEntity.position().distanceToSqr(centerPoint) < radiusSq) {
                return true;
            }
        }
        return false;
    }

    private static SpectralBindCandidate nearestSpectralBindCandidate(Level level, BlockPos center) {
        SpectralBindCandidate best = SpectralBindCandidate.NONE;
        best = nearer(best, spectralCandidate(level, center,
                com.emoniph.witchery.entity.SpectreEntity.class, "Spectre"));
        best = nearer(best, spectralCandidate(level, center,
                com.emoniph.witchery.entity.BansheeEntity.class, "Banshee"));
        best = nearer(best, spectralCandidate(level, center,
                com.emoniph.witchery.entity.PoltergeistEntity.class, "Poltergeist"));
        return best;
    }

    private static SpectralBindCandidate nearer(SpectralBindCandidate current, SpectralBindCandidate candidate) {
        if (candidate.count() <= 0) {
            return current;
        }
        return current.count() <= 0 || candidate.nearestDistanceSqr() < current.nearestDistanceSqr()
                ? candidate
                : current;
    }

    private static <T extends LivingEntity> SpectralBindCandidate spectralCandidate(Level level, BlockPos center,
                                                                                   Class<T> entityClass,
                                                                                   String name) {
        Vec3 centerPoint = Vec3.atCenterOf(center);
        double radiusSq = BIND_SPECTRAL_STONE_RADIUS * BIND_SPECTRAL_STONE_RADIUS;
        AABB bounds = new AABB(centerPoint, centerPoint).inflate(BIND_SPECTRAL_STONE_RADIUS);
        int count = 0;
        double nearestDistanceSqr = Double.MAX_VALUE;
        for (T entity : level.getEntitiesOfClass(entityClass, bounds)) {
            double distance = entity.position().distanceToSqr(centerPoint);
            if (distance > radiusSq) {
                continue;
            }
            count++;
            nearestDistanceSqr = Math.min(nearestDistanceSqr, distance);
        }
        return count == 0 ? SpectralBindCandidate.NONE
                : new SpectralBindCandidate(name, Math.min(count, 3), nearestDistanceSqr);
    }

    private static boolean hasBlightAltarPower(Level level, BlockPos center) {
        AltarBlockEntity altar = AltarPowerSourceLookup.findClosest(level, center);
        return altar != null && altar.hasValidStructure() && altar.getPower() >= BLIGHT_RITE_POWER;
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
    private record SpectralBindCandidate(String name, int count, double nearestDistanceSqr) {
        private static final SpectralBindCandidate NONE = new SpectralBindCandidate("", 0, Double.MAX_VALUE);
    }
}
