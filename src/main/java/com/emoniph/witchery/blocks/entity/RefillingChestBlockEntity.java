package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Witchery Refilling Chest using vanilla chest lid/open behavior with Witchery's
 * Torment-only timed refill mechanic.
 */
public class RefillingChestBlockEntity extends ChestBlockEntity {

    public static final long REFILL_TICKS = 72000L;
    private static final String LIFE_TICKS_TAG = "WITCLifeTicks";
    private static final String RESURRECTED_LIFE_TICKS_TAG = "WitcheryLifeTicks";
    private static final String LEGACY_COOLDOWN_TAG = "RefillCooldown";
    public static final Identifier LOOT_TABLE =
            Identifier.fromNamespaceAndPath("minecraft", "chests/simple_dungeon");

    private long lifeTicks = 0L;

    public RefillingChestBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.REFILLING_CHEST.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RefillingChestBlockEntity be) {
        if (be.lifeTicks == 0L) {
            be.tryRefill(level, pos);
        } else if (be.lifeTicks >= Long.MAX_VALUE) {
            be.lifeTicks = 1L;
        }

        be.lifeTicks++;
        be.tryRefill(level, pos);
        be.recheckOpen();
    }

    private void tryRefill(Level level, BlockPos pos) {
        if (!shouldRefill(level.dimension().equals(WitcheryDimensions.TORMENT), isEmpty(), lifeTicks)) return;
        fillFromLootTable(level, pos);
    }

    public static boolean shouldRefill(boolean inTorment, boolean empty, long lifeTicks) {
        return inTorment && empty && lifeTicks % REFILL_TICKS == 0L;
    }

    public long getLifeTicks() {
        return lifeTicks;
    }

    private void fillFromLootTable(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LootTable table = serverLevel.getServer().reloadableRegistries()
                .getLootTable(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.LOOT_TABLE, LOOT_TABLE));
        if (table == LootTable.EMPTY) {
            return;
        }

        LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .create(LootContextParamSets.CHEST);

        List<ItemStack> drops = new ArrayList<>(table.getRandomItems(params));
        int maxStacks = 2 + serverLevel.getRandom().nextInt(4);

        clearContent();

        List<Integer> slots = new ArrayList<>(getContainerSize());
        for (int i = 0; i < getContainerSize(); i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, new Random(serverLevel.getRandom().nextLong()));

        int slotIndex = 0;
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) {
                continue;
            }
            if (slotIndex >= maxStacks || slotIndex >= getContainerSize()) {
                break;
            }
            setItem(slots.get(slotIndex++), stack);
        }

        setChanged();
        serverLevel.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.BLOCKS, 0.45F, 0.75F);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D,
                10, 0.25D, 0.2D, 0.25D, 0.0D);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.witchery.refilling_chest");
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putLong(LIFE_TICKS_TAG, lifeTicks);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        lifeTicks = tag.getLongOr(
                LIFE_TICKS_TAG,
                tag.getLongOr(RESURRECTED_LIFE_TICKS_TAG, (long) tag.getIntOr(LEGACY_COOLDOWN_TAG, 0))
        );
    }
}
