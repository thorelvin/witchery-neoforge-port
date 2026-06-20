package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.CursedBlocks;
import com.emoniph.witchery.brewing.CursedBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
 * POWERED. Ladders fire when a living entity enters the ladder block. The curse data lives in
 * {@link CursedBlockData}; the block itself is never modified.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class CursedBlockEvents {

    private CursedBlockEvents() {}

    /** Transient per-level record of which cursed plates were powered last tick (rising-edge detection). */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> POWERED_PLATES = new HashMap<>();
    /** Transient per-level record of entities already touching each cursed ladder (entry detection). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Set<UUID>>> LADDER_CONTACTS = new HashMap<>();

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
        applyCurse(level, target, player);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        CursedBlockData data = CursedBlockData.get(level);
        if (data.isEmpty()) {
            POWERED_PLATES.remove(level.dimension());
            LADDER_CONTACTS.remove(level.dimension());
            return;
        }
        Set<BlockPos> powered = POWERED_PLATES.computeIfAbsent(level.dimension(), k -> new HashSet<>());
        Map<BlockPos, Set<UUID>> ladderContacts = LADDER_CONTACTS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        List<BlockPos> stale = null;
        for (BlockPos pos : data.positions()) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (CursedBlocks.isPressurePlate(state)) {
                ladderContacts.remove(pos);
                boolean on = state.getValue(BlockStateProperties.POWERED);
                boolean wasOn = powered.contains(pos);
                if (on && !wasOn) {
                    LivingEntity stepper = findStepper(level, pos);
                    if (stepper != null) {
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
                handleLadderContact(level, pos, ladderContacts);
            } else if (!CursedBlocks.isSupportedCursedBlock(state)) {
                // The cursed block was broken or replaced — drop the stale record.
                if (stale == null) {
                    stale = new ArrayList<>();
                }
                stale.add(pos);
                powered.remove(pos);
                ladderContacts.remove(pos);
            } else {
                ladderContacts.remove(pos);
            }
        }
        if (stale != null) {
            stale.forEach(data::remove);
        }
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
                                            Map<BlockPos, Set<UUID>> ladderContacts) {
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
            applyCurse(level, pos, entrant);
        }

        if (CursedBlockData.get(level).get(pos) == null || current.isEmpty()) {
            ladderContacts.remove(pos);
        } else {
            ladderContacts.put(pos.immutable(), current);
        }
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
