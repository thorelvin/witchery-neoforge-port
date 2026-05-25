package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Brew of Erosion - acid damage plus source-style no-drop terrain erosion. */
public class BrewOfErosionItem extends BrewItem {
    private static final int BLOCK_RADIUS = 2;
    private static final float ACID_DAMAGE = 8.0F;
    private static final int EQUIPMENT_DAMAGE = 100;

    public BrewOfErosionItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        if (!canErode(level, center, level.getBlockState(center))) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        erodeBlocks((ServerLevel) level, center);
        consumeBrew(context.getPlayer(), context.getHand(), context.getItemInHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        acidTarget(level, player, target);
        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos center = player.blockPosition();
        erodeBlocks(serverLevel, center);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(3.0),
                target -> target != player);
        for (LivingEntity target : targets) {
            acidTarget(serverLevel, player, target);
        }
    }

    private static void acidTarget(ServerLevel level, Player player, LivingEntity target) {
        target.hurt(player.damageSources().magic(), ACID_DAMAGE);
        for (EquipmentSlot slot : List.of(EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack equipment = target.getItemBySlot(slot);
            if (equipment.isDamageableItem()) {
                equipment.hurtAndBreak(EQUIPMENT_DAMAGE, target, slot);
            }
        }
        level.sendParticles(ParticleTypes.SPLASH,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                12, target.getBbWidth() * 0.35D, target.getBbHeight() * 0.25D,
                target.getBbWidth() * 0.35D, 0.02D);
    }

    private static void erodeBlocks(ServerLevel level, BlockPos center) {
        int obsidianMelted = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-BLOCK_RADIUS, -BLOCK_RADIUS, -BLOCK_RADIUS),
                center.offset(BLOCK_RADIUS, BLOCK_RADIUS, BLOCK_RADIUS))) {
            if (pos.distSqr(center) > BLOCK_RADIUS * BLOCK_RADIUS) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!canErode(level, pos, state)) {
                continue;
            }
            if (state.is(Blocks.OBSIDIAN)) {
                obsidianMelted++;
            }
            level.destroyBlock(pos, false);
            level.sendParticles(ParticleTypes.SPLASH,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    3, 0.2D, 0.2D, 0.2D, 0.0D);
        }

        if (obsidianMelted > 0) {
            level.addFreshEntity(new ItemEntity(level, center.getX() + 0.5D, center.getY() + 0.5D,
                    center.getZ() + 0.5D, new ItemStack(Blocks.OBSIDIAN, obsidianMelted)));
        }
    }

    private static boolean canErode(Level level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && !state.liquid()
                && !state.hasBlockEntity()
                && !state.is(Blocks.FIRE)
                && !state.is(Blocks.SOUL_FIRE)
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static void consumeBrew(Player player, InteractionHand hand, ItemStack stack) {
        if (player != null && player.getAbilities().instabuild) {
            return;
        }

        stack.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (player == null || !player.getInventory().add(bottle)) {
            if (player != null) {
                player.drop(bottle, false);
            }
        }
        if (player != null) {
            player.setItemInHand(hand, stack);
        }
    }
}
