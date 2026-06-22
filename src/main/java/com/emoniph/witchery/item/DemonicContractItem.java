package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.events.FlameImpContractEffects;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** Source-style Flame Imp contracts: ownership signs with the caster; spell contracts bind to a target. */
public class DemonicContractItem extends Item {
    private static final int FIRE_RESISTANCE_TICKS = 20 * 60 * 15;
    private static final int IMP_EFFECT_TICKS = 20 * 60 * 10;

    public enum ContractKind {
        OWNERSHIP,
        LIVING_FLAME,
        FIERY_TOLERANCE,
        EVAPORATION,
        FIERY_TOUCH,
        MELTING_TOUCH
    }

    public DemonicContractItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ContractKind kind = kind(stack).orElse(null);
        if (kind == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (isBound(stack)) {
            player.displayClientMessage(Component.translatable("item.witchery.demonic_contract.already_signed",
                    getBoundName(stack)).withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.CONSUME;
        }

        if (kind == ContractKind.OWNERSHIP || player.isSecondaryUseActive()) {
            ItemStack signed = signedCopy(stack, player);
            replaceOneWithSigned(player, hand, stack, signed);
            player.displayClientMessage(Component.translatable(kind == ContractKind.OWNERSHIP
                            ? "item.witchery.demonic_contract.signed_self"
                            : "item.witchery.demonic_contract.signed_target",
                    player.getName()).withStyle(ChatFormatting.DARK_RED), false);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.translatable("item.witchery.demonic_contract.needs_target")
                .withStyle(ChatFormatting.GRAY), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (target instanceof FlameImpEntity) {
            return InteractionResult.PASS;
        }

        ContractKind kind = kind(stack).orElse(null);
        if (kind == null) {
            return InteractionResult.PASS;
        }
        if (isBound(stack)) {
            user.displayClientMessage(Component.translatable("item.witchery.demonic_contract.already_signed",
                    getBoundName(stack)).withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.CONSUME;
        }
        if (kind == ContractKind.OWNERSHIP) {
            user.displayClientMessage(Component.translatable("item.witchery.demonic_contract.ownership_self")
                    .withStyle(ChatFormatting.DARK_RED), false);
            return InteractionResult.CONSUME;
        }

        ItemStack signed = signedCopy(stack, target);
        replaceOneWithSigned(user, hand, stack, signed);
        user.displayClientMessage(Component.translatable("item.witchery.demonic_contract.signed_target",
                target.getDisplayName()).withStyle(ChatFormatting.DARK_RED), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (isBound(stack)) {
            tooltip.accept(Component.translatable("item.witchery.demonic_contract.tooltip.bound", getBoundName(stack))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("item.witchery.demonic_contract.tooltip.unbound")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        kind(stack).ifPresent(kind -> tooltip.accept(Component.translatable("item.witchery.demonic_contract.tooltip."
                + kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.DARK_GRAY)));
    }

    public static Optional<ContractKind> kind(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        if (isOwnershipContract(stack)) return Optional.of(ContractKind.OWNERSHIP);
        if (stack.is(WitcheryItems.LIVING_FLAME_CONTRACT.get())) {
            return Optional.of(ContractKind.LIVING_FLAME);
        }
        if (stack.is(WitcheryItems.FIERY_TOLERANCE_CONTRACT.get())) {
            return Optional.of(ContractKind.FIERY_TOLERANCE);
        }
        if (stack.is(WitcheryItems.EVAPORATION_CONTRACT.get())) {
            return Optional.of(ContractKind.EVAPORATION);
        }
        if (stack.is(WitcheryItems.FIERY_TOUCH_CONTRACT.get())) {
            return Optional.of(ContractKind.FIERY_TOUCH);
        }
        if (stack.is(WitcheryItems.MELTING_TOUCH_CONTRACT.get())) {
            return Optional.of(ContractKind.MELTING_TOUCH);
        }
        return Optional.empty();
    }

    public static boolean isOwnershipContract(ItemStack stack) {
        return stack.is(WitcheryItems.DEMONIC_CONTRACT.get());
    }

    public static boolean isSpellContract(ItemStack stack) {
        return kind(stack).filter(kind -> kind != ContractKind.OWNERSHIP).isPresent();
    }

    public static boolean isBound(ItemStack stack) {
        return TaglockKitItem.getUUID(stack).isPresent();
    }

    public static Optional<UUID> getBoundUUID(ItemStack stack) {
        return TaglockKitItem.getUUID(stack);
    }

    public static String getBoundName(ItemStack stack) {
        String name = TaglockKitItem.getBoundName(stack);
        return name.isBlank() ? "Unknown" : name;
    }

    public static Optional<LivingEntity> findBoundTarget(MinecraftServer server, ItemStack stack) {
        return getBoundUUID(stack).flatMap(uuid -> PoppetItem.findLivingEntity(server, uuid))
                .filter(LivingEntity::isAlive);
    }

    public static boolean activate(ItemStack stack, LivingEntity target) {
        ContractKind kind = kind(stack).orElse(null);
        if (kind == null || kind == ContractKind.OWNERSHIP) {
            return false;
        }

        return switch (kind) {
            case LIVING_FLAME -> summonBlaze(target);
            case FIERY_TOLERANCE -> {
                target.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_RESISTANCE_TICKS));
                target.level().playSound(null, target.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS, 0.7F, 1.5F);
                yield true;
            }
            case EVAPORATION -> {
                if (!(target instanceof ServerPlayer player)) yield false;
                FlameImpContractEffects.applyEvaporation(player, IMP_EFFECT_TICKS);
                yield true;
            }
            case FIERY_TOUCH -> {
                if (!(target instanceof ServerPlayer player)) yield false;
                FlameImpContractEffects.applyFieryTouch(player, IMP_EFFECT_TICKS);
                yield true;
            }
            case MELTING_TOUCH -> {
                if (!(target instanceof ServerPlayer player)) yield false;
                FlameImpContractEffects.applyMeltingTouch(player, IMP_EFFECT_TICKS);
                yield true;
            }
            default -> false;
        };
    }

    private static boolean summonBlaze(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        Blaze blaze = EntityType.BLAZE.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (blaze == null) {
            return false;
        }
        BlockPos spawnPos = findBlazeSpawn(level, target.blockPosition());
        blaze.snapTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F, 0.0F);
        blaze.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50.0D);
        blaze.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7.0D);
        blaze.setHealth(50.0F);
        blaze.setTarget(target);
        if (!level.addFreshEntity(blaze)) {
            return false;
        }
        level.playSound(null, spawnPos, SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.85F);
        level.sendParticles(ParticleTypes.FLAME, spawnPos.getX() + 0.5D, spawnPos.getY() + 0.8D, spawnPos.getZ() + 0.5D,
                24, 0.35D, 0.45D, 0.35D, 0.04D);
        return true;
    }

    private static BlockPos findBlazeSpawn(ServerLevel level, BlockPos center) {
        for (int i = 0; i < 16; i++) {
            BlockPos candidate = center.offset(
                    level.random.nextInt(5) - 2,
                    level.random.nextInt(3),
                    level.random.nextInt(5) - 2);
            if (level.getBlockState(candidate).canBeReplaced()
                    && level.getBlockState(candidate.above()).canBeReplaced()
                    && !level.getBlockState(candidate.below()).is(Blocks.WATER)) {
                return candidate;
            }
        }
        return center.above();
    }

    private static ItemStack signedCopy(ItemStack stack, LivingEntity target) {
        ItemStack signed = stack.copyWithCount(1);
        if (target instanceof Player player) {
            TaglockKitItem.bind(signed, player.getUUID(), player.getName().getString());
        } else {
            TaglockKitItem.bindEntity(signed, target);
        }
        return signed;
    }

    private static void replaceOneWithSigned(Player player, InteractionHand hand, ItemStack source, ItemStack signed) {
        if (source.getCount() == 1) {
            player.setItemInHand(hand, signed);
            return;
        }
        source.shrink(1);
        if (!player.getInventory().add(signed)) {
            player.drop(signed, false);
        }
    }
}
