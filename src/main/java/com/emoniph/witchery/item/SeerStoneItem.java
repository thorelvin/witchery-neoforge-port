package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.familiar.FamiliarHelper;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Seer Stone — reads the current player state and can call a nearby Coven Witch closer.
 * This restores the practical utility of the original item without depending on the
 * deeper coven-membership persistence that the port still lacks.
 */
public class SeerStoneItem extends Item {

    private static final double TARGET_RANGE = 3.0;
    private static final double COVEN_CALL_RADIUS_XZ = 64.0;
    private static final double COVEN_CALL_RADIUS_Y = 16.0;

    public SeerStoneItem(Properties props) {
        super(props.rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        // Looking at another player → read their state.
        ServerPlayer target = findLookedAtPlayer(sp, TARGET_RANGE);
        if (target != null) {
            readPlayer(sp, target);
            return InteractionResult.CONSUME;
        }

        // Sneak → call a coven witch closer; a plain use reads your own state.
        if (sp.isSecondaryUseActive()) {
            summonNearbyCovenMember(sp);
        } else {
            readPlayer(sp, sp);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!(user instanceof ServerPlayer reader) || !(target instanceof ServerPlayer targetPlayer)) {
            return InteractionResult.PASS;
        }
        if (user.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        readPlayer(reader, targetPlayer);
        return InteractionResult.CONSUME;
    }

    @Nullable
    private static ServerPlayer findLookedAtPlayer(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        AABB bounds = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eye, end, bounds,
                entity -> entity instanceof ServerPlayer target && target != player && !target.isSpectator(),
                range * range);
        return hit != null && hit.getEntity() instanceof ServerPlayer target ? target : null;
    }

    private static void readPlayer(ServerPlayer reader, ServerPlayer target) {
        WitcheryPlayerData data = WitcheryAttachments.get(target);
        reader.displayClientMessage(
                Component.translatable("item.witchery.seer_stone.reading", target.getName())
                        .withStyle(ChatFormatting.BLUE),
                false);

        if (data.getManifestationTime() > 0) {
            reader.displayClientMessage(
                    Component.translatable("item.witchery.seer_stone.manifestationtime", data.getManifestationTime())
                            .withStyle(ChatFormatting.AQUA),
                    false);
        } else {
            reader.displayClientMessage(
                    Component.translatable("item.witchery.seer_stone.nomanifestationtime")
                            .withStyle(ChatFormatting.DARK_AQUA),
                    false);
        }

        var familiar = FamiliarHelper.findBoundFamiliarEntity(target);
        String familiarType = familiar != null ? FamiliarHelper.describeFamiliarEntity(familiar) : data.getFamiliarType();
        String familiarName = familiar != null && familiar.hasCustomName()
                ? familiar.getCustomName().getString()
                : data.getFamiliarName();
        if (familiarType != null && !familiarType.isBlank()) {
            String familiarDescription = (familiarName != null && !familiarName.isBlank())
                    ? familiarType + " (" + familiarName + ")"
                    : familiarType;
            reader.displayClientMessage(
                    Component.translatable("item.witchery.seer_stone.familiar", familiarDescription)
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    false);
        } else {
            reader.displayClientMessage(
                    Component.translatable("item.witchery.seer_stone.nofamiliar")
                            .withStyle(ChatFormatting.DARK_PURPLE),
                    false);
        }

        int covenSize = target.level() instanceof ServerLevel targetLevel
                ? CovenWitchEntity.covenSize(targetLevel, target.getUUID())
                : countNearbyCovenWitches(target);
        if (covenSize > 0) {
            reader.displayClientMessage(
                    Component.translatable("item.witchery.seer_stone.covensize", covenSize)
                            .withStyle(ChatFormatting.GOLD),
                    false);
        } else {
            reader.displayClientMessage(
                    Component.translatable("item.witchery.seer_stone.nocoven")
                            .withStyle(ChatFormatting.GRAY),
                    false);
        }

        var offered = com.emoniph.witchery.entity.CovenWitchEntity.findOfferedQuest(target);
        if (offered != null) {
            reader.displayClientMessage(
                    Component.translatable("item.witchery.seer_stone.quest",
                            offered.witchName(),
                            com.emoniph.witchery.entity.CovenWitchEntity.describeQuest(offered))
                            .withStyle(ChatFormatting.YELLOW),
                    false);
        }

        List<String> curses = describeTrackedCurses(target, data);
        if (curses.isEmpty()) {
            reader.displayClientMessage(
                    Component.translatable("item.witchery.seer_stone.notcursed")
                            .withStyle(ChatFormatting.GRAY),
                    false);
        } else {
            reader.displayClientMessage(
                    Component.translatable("item.witchery.seer_stone.curses", String.join(", ", curses))
                            .withStyle(ChatFormatting.RED),
                    false);
        }
    }

    private static List<String> describeTrackedCurses(ServerPlayer target, WitcheryPlayerData data) {
        List<String> curses = new ArrayList<>();
        if (target.hasEffect(net.minecraft.world.effect.MobEffects.UNLUCK)) {
            curses.add(Component.translatable("item.witchery.seer_stone.misfortune").getString());
        }
        if (target.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA)) {
            curses.add(Component.translatable("item.witchery.seer_stone.insanity").getString());
        }
        if (target.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)
                && target.hasEffect(net.minecraft.world.effect.MobEffects.MINING_FATIGUE)) {
            curses.add(Component.translatable("item.witchery.seer_stone.sinking").getString());
        }
        if (target.getRemainingFireTicks() > 0 || target.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS)) {
            curses.add(Component.translatable("item.witchery.seer_stone.overheating").getString());
        }
        if (data.isInSpiritWorld() && data.isNightmare()) {
            curses.add(Component.translatable("item.witchery.seer_stone.nightmare").getString());
        }
        return curses;
    }

    private static int countNearbyCovenWitches(ServerPlayer player) {
        AABB bounds = player.getBoundingBox().inflate(COVEN_CALL_RADIUS_XZ, COVEN_CALL_RADIUS_Y, COVEN_CALL_RADIUS_XZ);
        return player.level().getEntitiesOfClass(CovenWitchEntity.class, bounds).size();
    }

    private static void summonNearbyCovenMember(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        AABB bounds = player.getBoundingBox().inflate(COVEN_CALL_RADIUS_XZ, COVEN_CALL_RADIUS_Y, COVEN_CALL_RADIUS_XZ);
        List<CovenWitchEntity> witches = new ArrayList<>(level.getEntitiesOfClass(CovenWitchEntity.class, bounds));
        if (witches.isEmpty()) {
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.6f, 0.7f);
            player.displayClientMessage(Component.translatable("item.witchery.seer_stone.noanswer")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        CovenWitchEntity witch = witches.get(level.random.nextInt(witches.size()));
        BlockPos summonPos = findSummonPosition(level, player.blockPosition());
        if (summonPos == null) {
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.6f, 0.7f);
            player.displayClientMessage(Component.translatable("item.witchery.seer_stone.noanswer")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        witch.teleportTo(summonPos.getX() + 0.5, summonPos.getY(), summonPos.getZ() + 0.5);
        witch.getNavigation().stop();
        level.sendParticles(ParticleTypes.PORTAL,
                summonPos.getX() + 0.5, summonPos.getY() + 1.0, summonPos.getZ() + 0.5,
                24, 0.4, 0.6, 0.4, 0.2);
        level.playSound(null, summonPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0f, 1.15f);
        player.displayClientMessage(Component.translatable("item.witchery.seer_stone.call")
                .withStyle(ChatFormatting.GREEN), false);
    }

    @Nullable
    private static BlockPos findSummonPosition(ServerLevel level, BlockPos around) {
        int baseY = around.getY();
        for (int dy = -1; dy <= 1; dy++) {
            int y = baseY + dy;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(around.getX() + dx, y, around.getZ() + dz);
                    if (canStandAt(level, pos)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static boolean canStandAt(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        return level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.above()).canBeReplaced();
    }
}
