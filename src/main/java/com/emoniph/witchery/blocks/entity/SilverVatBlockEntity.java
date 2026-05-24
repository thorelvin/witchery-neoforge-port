package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class SilverVatBlockEntity extends BlockEntity {

    private static final int MAX_STORED = 64;
    private static final float SILVER_CHANCE = 0.25f;
    private static final int TICK_INTERVAL = 40;

    private int storedSilver = 0;

    public SilverVatBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.SILVER_VAT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SilverVatBlockEntity be) {
        if (level.getGameTime() % TICK_INTERVAL != 0) return;
        if (be.storedSilver >= MAX_STORED) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!(level.getBlockEntity(neighbor) instanceof AbstractFurnaceBlockEntity furnace)) continue;

            ItemStack inputSlot = furnace.getItem(0);
            if (!isGoldOre(inputSlot)) continue;

            BlockState neighborState = level.getBlockState(neighbor);
            if (!neighborState.hasProperty(AbstractFurnaceBlock.LIT)
                    || !neighborState.getValue(AbstractFurnaceBlock.LIT)) continue;

            if (level.random.nextFloat() < SILVER_CHANCE) {
                be.storedSilver = Math.min(be.storedSilver + 1, MAX_STORED);
                be.setChanged();
            }
        }
    }

    private static boolean isGoldOre(ItemStack stack) {
        return stack.is(Items.GOLD_ORE)
                || stack.is(Items.DEEPSLATE_GOLD_ORE)
                || stack.is(Items.NETHER_GOLD_ORE)
                || stack.is(Items.RAW_GOLD);
    }

    public int collectSilver() {
        int amount = storedSilver;
        storedSilver = 0;
        setChanged();
        return amount;
    }

    public int getStoredSilver() {
        return storedSilver;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredSilver", storedSilver);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        storedSilver = tag.getIntOr("StoredSilver", 0);
    }
}
