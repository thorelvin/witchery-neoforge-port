package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.CursedBlocks;
import com.emoniph.witchery.brewing.BrewWarningText;
import com.emoniph.witchery.brewing.CursedBlockData;
import com.emoniph.witchery.brewing.CursedBlockWarningData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Triggers a triggered-dispersal block curse on activation (source {@code BlockButtonBase} etc. +
 * {@code TileEntityCursedBlock.applyToEntityAndDestroy}). Buttons / levers / doors fire on the player's
 * right-click; pressure plates have no interaction event, so they are polled on the rising edge of
 * POWERED. Ladders fire when a living entity enters the ladder block. Warning-only cursed blocks share
 * the ladder contact path, but do not consume brew charges. The curse data lives in {@link CursedBlockData};
 * the block itself is never modified.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class CursedBlockEvents {

    private CursedBlockEvents() {}

    /** Transient per-level record of which cursed plates were powered last tick (rising-edge detection). */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> POWERED_PLATES = new HashMap<>();
    /** Transient per-level record of entities already touching each cursed ladder (entry detection). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Set<UUID>>> LADDER_CONTACTS = new HashMap<>();
    /** Transient per-level warning cooldowns, keyed by warned position and player. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Map<UUID, Long>>> WARNING_COOLDOWNS = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // Sneaking with an item places/uses the item instead of activating the block — don't trigger then.
        if (player.isSecondaryUseActive() && !event.getItemStack().isEmpty()) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!CursedBlocks.isRightClickActivated(state)) {
            return;
        }
        // Doors store the curse on the lower half.
        BlockPos target = state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER
                ? pos.below() : pos;
        applyWarning(level, target, player, warningCooldowns(level));
        applyCurse(level, target, player);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        CursedBlockData curses = CursedBlockData.get(level);
        CursedBlockWarningData warnings = CursedBlockWarningData.get(level);
        if (curses.isEmpty() && warnings.isEmpty()) {
            POWERED_PLATES.remove(level.dimension());
            LADDER_CONTACTS.remove(level.dimension());
            WARNING_COOLDOWNS.remove(level.dimension());
            return;
        }
        Set<BlockPos> powered = POWERED_PLATES.computeIfAbsent(level.dimension(), k -> new HashSet<>());
        Map<BlockPos, Set<UUID>> ladderContacts = LADDER_CONTACTS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        Map<BlockPos, Map<UUID, Long>> warningCooldowns = warningCooldowns(level);
        List<BlockPos> stale = null;
        for (BlockPos pos : watchedPositions(curses, warnings)) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if ((curses.get(pos) != null || warnings.get(pos) != null) && CursedBlocks.isPressurePlate(state)) {
                ladderContacts.remove(pos);
                boolean on = state.getValue(BlockStateProperties.POWERED);
                boolean wasOn = powered.contains(pos);
                if (on && !wasOn) {
                    LivingEntity stepper = findStepper(level, pos);
                    if (stepper != null) {
                        applyWarning(level, pos, stepper, warningCooldowns);
                        applyCurse(level, pos, stepper);
                    }
                }
                if (on) {
                    powered.add(pos.immutable());
                } else {
                    powered.remove(pos);
                }
            } else if (CursedBlocks.isLadder(state)) {
                powered.remove(pos);
                handleLadderContact(level, pos, ladderContacts, warningCooldowns);
            } else if (!CursedBlocks.isSupportedCursedBlock(state)) {
                // The cursed block was broken or replaced — drop the stale record.
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(pos);
                powered.remove(pos);
                ladderContacts.remove(pos);
                warningCooldowns.remove(pos);
            } else {
                ladderContacts.remove(pos);
                warningCooldowns.remove(pos);
            }
        }
        if (stale != null) {
            for (BlockPos pos : stale) {
                curses.remove(pos);
                warnings.remove(pos);
            }
        }
    }

    private static List<BlockPos> watchedPositions(CursedBlockData curses, CursedBlockWarningData warnings) {
        Set<BlockPos> positions = new HashSet<>();
        positions.addAll(curses.positions());
        positions.addAll(warnings.positions());
        return new ArrayList<>(positions);
    }

    private static Map<BlockPos, Map<UUID, Long>> warningCooldowns(ServerLevel level) {
        return WARNING_COOLDOWNS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
    }

    private static void applyCurse(ServerLevel level, BlockPos pos, LivingEntity activator) {
        CursedBlockData.Curse curse = CursedBlockData.get(level).takeCharge(pos);
        if (curse == null || curse.brew().isEmpty()) {
            return;
        }
        curse.brew().applyToEntity(activator, resolveThrower(level, curse));
        int color = curse.brew().color();
        level.sendParticles(new DustParticleOptions(color, 1.0F),
                activator.getX(), activator.getY() + activator.getBbHeight() * 0.5, activator.getZ(),
                12, 0.3, 0.4, 0.3, 0.0);
        level.sendParticles(ParticleTypes.WITCH,
                activator.getX(), activator.getY() + activator.getBbHeight() * 0.5, activator.getZ(),
                8, 0.3, 0.4, 0.3, 0.0);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.7F);
    }

    @Nullable
    private static Entity resolveThrower(ServerLevel level, CursedBlockData.Curse curse) {
        return curse.thrower().map(level::getEntity).orElse(null);
    }

    private static void handleLadderContact(ServerLevel level, BlockPos pos,
                                            Map<BlockPos, Set<UUID>> ladderContacts,
                                            Map<BlockPos, Map<UUID, Long>> warningCooldowns) {
        Set<UUID> current = new HashSet<>();
        LivingEntity entrant = null;
        Set<UUID> previous = ladderContacts.get(pos);

        for (LivingEntity entity : findLadderContacts(level, pos)) {
            UUID id = entity.getUUID();
            current.add(id);
            if (entrant == null && (previous == null || !previous.contains(id))) {
                entrant = entity;
            }
        }

        if (entrant != null) {
            applyWarning(level, pos, entrant, warningCooldowns);
            applyCurse(level, pos, entrant);
        }

        if ((CursedBlockData.get(level).get(pos) == null
                && CursedBlockWarningData.get(level).get(pos) == null) || current.isEmpty()) {
            ladderContacts.remove(pos);
        } else {
            ladderContacts.put(pos.immutable(), current);
        }
    }

    private static void applyWarning(ServerLevel level, BlockPos pos, LivingEntity activator,
                                     Map<BlockPos, Map<UUID, Long>> warningCooldowns) {
        CursedBlockWarningData.Warning warning = CursedBlockWarningData.get(level).get(pos);
        if (warning == null || !(activator instanceof Player player)) {
            return;
        }
        long now = level.getGameTime();
        Map<UUID, Long> playerCooldowns = warningCooldowns.computeIfAbsent(pos.immutable(), k -> new HashMap<>());
        Long nextAllowed = playerCooldowns.get(player.getUUID());
        if (nextAllowed != null && nextAllowed > now) {
            return;
        }
        playerCooldowns.put(player.getUUID(), now + warning.cooldownTicks());
        player.displayClientMessage(Component.literal(warning.message()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.sendParticles(new DustParticleOptions(BrewWarningText.COLOR, 0.85F),
                player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                8, 0.25, 0.35, 0.25, 0.0);
        level.sendParticles(ParticleTypes.WITCH,
                player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                3, 0.2, 0.3, 0.2, 0.0);
        level.playSound(null, pos, SoundEvents.AMBIENT_CAVE.value(), SoundSource.BLOCKS, 0.45F, 1.2F);
    }

    @Nullable
    private static LivingEntity findStepper(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
                pos.getX() + 0.9, pos.getY() + 0.25, pos.getZ() + 0.9);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> !e.isSpectator() && !e.isIgnoringBlockTriggers())) {
            return entity;
        }
        return null;
    }

    private static List<LivingEntity> findLadderContacts(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                e -> !e.isSpectator() && !e.isIgnoringBlockTriggers());
    }
}
