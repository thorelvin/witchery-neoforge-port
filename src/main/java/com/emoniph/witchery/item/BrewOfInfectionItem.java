package com.emoniph.witchery.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

/** Brew of Infection - source-style infection hit behavior with a drink fallback. */
public class BrewOfInfectionItem extends BrewItem {

    public BrewOfInfectionItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Optional<BlockState> infected = infectedStoneState(level.getBlockState(pos));
        if (infected.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        level.setBlock(pos, infected.get(), Block.UPDATE_ALL);
        if (level instanceof ServerLevel serverLevel) {
            playInfectionEffects(serverLevel, pos);
        }
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

        infectLivingTarget(level, player, target);
        consumeBrew(player, hand, stack);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void applyEffect(Player player, Level level) {
        AABB area = new AABB(player.blockPosition()).inflate(4.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player);
        for (LivingEntity target : targets) {
            if (level instanceof ServerLevel serverLevel) {
                infectLivingTarget(serverLevel, player, target);
            }
        }
    }

    private static Optional<BlockState> infectedStoneState(BlockState state) {
        if (state.is(Blocks.STONE)) {
            return Optional.of(Blocks.INFESTED_STONE.defaultBlockState());
        }
        if (state.is(Blocks.COBBLESTONE)) {
            return Optional.of(Blocks.INFESTED_COBBLESTONE.defaultBlockState());
        }
        if (state.is(Blocks.STONE_BRICKS)) {
            return Optional.of(Blocks.INFESTED_STONE_BRICKS.defaultBlockState());
        }
        return Optional.empty();
    }

    private static void infectLivingTarget(ServerLevel level, Player player, LivingEntity target) {
        if (target instanceof Villager villager) {
            zombifyVillager(level, villager);
            return;
        }

        target.hurt(player.damageSources().magic(), 1.0F);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 8));
        level.sendParticles(ParticleTypes.POOF,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                8, target.getBbWidth() * 0.4D, target.getBbHeight() * 0.25D,
                target.getBbWidth() * 0.4D, 0.02D);
    }

    private static void zombifyVillager(ServerLevel level, Villager villager) {
        ZombieVillager zombie = EntityType.ZOMBIE_VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
        if (zombie == null) {
            return;
        }

        VillagerData villagerData = villager.getVillagerData();
        zombie.setVillagerData(villagerData);
        zombie.setPos(villager.getX(), villager.getY(), villager.getZ());
        zombie.setYRot(villager.getYRot());
        zombie.setXRot(villager.getXRot());
        zombie.setBaby(villager.isBaby());
        if (villager.hasCustomName()) {
            zombie.setCustomName(villager.getCustomName());
        }
        level.addFreshEntity(zombie);
        villager.discard();
        level.levelEvent(null, 1016, zombie.blockPosition(), 0);
    }

    private static void playInfectionEffects(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.SILVERFISH_HURT, SoundSource.BLOCKS,
                0.5F, 0.8F + level.random.nextFloat() * 0.4F);
        level.sendParticles(ParticleTypes.POOF,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                10, 0.35D, 0.35D, 0.35D, 0.02D);
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
