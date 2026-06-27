package com.emoniph.witchery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.function.Consumer;

public class WoodenScannerItem extends Item {
    private static final int SCAN_TICKS = 32;
    private static final int MAX_USE_TICKS = 72000;
    private static final double SCAN_RANGE = 16.0D;

    private static final String TAG_POINTS = "WitcheryWildPoints";
    private static final String TAG_LEGACY_RESEARCH_POINTS = "WitcheryResearchPoints";
    private static final String TAG_SCANNED = "WitcheryScannedTargets";

    public WoodenScannerItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_TICKS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(livingEntity instanceof Player player)) {
            return;
        }

        int elapsedTicks = getUseDuration(stack, livingEntity) - remainingUseDuration;
        HitResult hit = pickScanTarget(player, SCAN_RANGE);
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            if (elapsedTicks >= SCAN_TICKS) {
                failScan((ServerLevel) level, player);
                player.stopUsingItem();
            }
            return;
        }

        if (elapsedTicks % 8 == 0) {
            Vec3 loc = hit.getLocation();
            ((ServerLevel) level).sendParticles(ParticleTypes.PORTAL,
                    loc.x, loc.y, loc.z, 3, 0.12D, 0.12D, 0.12D, 0.02D);
        }

        if (elapsedTicks < SCAN_TICKS) {
            return;
        }

        ScanTarget target = scanTarget(level, hit);
        if (target == null) {
            failScan((ServerLevel) level, player);
        } else if (markScanned(stack, target.key(), target.points())) {
            completeNewScan((ServerLevel) level, player, hit.getLocation(), target);
            EquipmentSlot slot = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, player, slot);
        } else {
            completeKnownScan((ServerLevel) level, player, hit.getLocation(), target);
        }
        player.stopUsingItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.witchery.wooden_scanner.tip")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.accept(Component.translatable("item.witchery.wooden_scanner.points", wildPoints(stack))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.translatable("item.witchery.wooden_scanner.scanned", scannedCount(stack))
                .withStyle(ChatFormatting.GRAY));
    }

    public static boolean isUsing(ItemStack stack, ItemOwner owner) {
        return owner instanceof LivingEntity living && living.isUsingItem() && living.getUseItem() == stack;
    }

    public static int wildPoints(ItemStack stack) {
        CompoundTag tag = customData(stack);
        return tag.getIntOr(TAG_POINTS, tag.getIntOr(TAG_LEGACY_RESEARCH_POINTS, 0));
    }

    public static int scannedCount(ItemStack stack) {
        return customData(stack).getCompoundOrEmpty(TAG_SCANNED).size();
    }

    private static boolean markScanned(ItemStack stack, String key, int points) {
        if (key.isBlank()) {
            return false;
        }
        CompoundTag root = customData(stack);
        if (root.getCompoundOrEmpty(TAG_SCANNED).contains(key)) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag scanned = tag.getCompoundOrEmpty(TAG_SCANNED);
            scanned.putBoolean(key, true);
            tag.put(TAG_SCANNED, scanned);
            int currentPoints = Math.max(0, tag.getIntOr(TAG_POINTS, tag.getIntOr(TAG_LEGACY_RESEARCH_POINTS, 0)));
            tag.putInt(TAG_POINTS, currentPoints + Math.max(1, points));
            tag.remove(TAG_LEGACY_RESEARCH_POINTS);
        });
        return true;
    }

    private static ScanTarget scanTarget(Level level, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityHit.getEntity().getType());
            return new ScanTarget("entity:" + id, entityHit.getEntity().getType().getDescription(), 2);
        }
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR)) {
                return null;
            }
            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            int points = level.getBlockEntity(pos) != null ? 2 : 1;
            return new ScanTarget("block:" + id, state.getBlock().getName(), points);
        }
        return null;
    }

    private static HitResult pickScanTarget(Player player, double range) {
        HitResult blockHit = player.pick(range, 1.0F, false);
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 reach = player.getViewVector(1.0F).scale(range);
        AABB searchBox = player.getBoundingBox().expandTowards(reach).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(reach), searchBox,
                entity -> entity != player && entity.isPickable(), range * range);
        if (entityHit == null) {
            return blockHit;
        }
        if (blockHit.getType() == HitResult.Type.MISS) {
            return entityHit;
        }
        return entityHit.getLocation().distanceToSqr(eye) < blockHit.getLocation().distanceToSqr(eye)
                ? entityHit
                : blockHit;
    }

    private static void completeNewScan(ServerLevel level, Player player, Vec3 loc, ScanTarget target) {
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.75F, 1.45F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                loc.x, loc.y, loc.z, 18, 0.28D, 0.28D, 0.28D, 0.04D);
        level.sendParticles(ParticleTypes.WITCH,
                loc.x, loc.y, loc.z, 10, 0.24D, 0.24D, 0.24D, 0.02D);
        player.displayClientMessage(Component.translatable("item.witchery.wooden_scanner.discovered",
                target.name(), target.points()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static void completeKnownScan(ServerLevel level, Player player, Vec3 loc, ScanTarget target) {
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.4F, 0.85F);
        level.sendParticles(ParticleTypes.PORTAL,
                loc.x, loc.y, loc.z, 8, 0.18D, 0.18D, 0.18D, 0.01D);
        player.displayClientMessage(Component.translatable("item.witchery.wooden_scanner.known",
                target.name()).withStyle(ChatFormatting.GRAY), true);
    }

    private static void failScan(ServerLevel level, Player player) {
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.PLAYERS, 0.5F, 0.65F);
        player.displayClientMessage(Component.translatable("item.witchery.wooden_scanner.failed")
                .withStyle(ChatFormatting.DARK_GRAY), true);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private record ScanTarget(String key, Component name, int points) {
        private ScanTarget {
            key = key == null ? "" : key.toLowerCase(Locale.ROOT);
            points = Math.max(1, points);
        }
    }
}
