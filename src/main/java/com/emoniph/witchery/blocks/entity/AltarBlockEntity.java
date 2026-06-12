package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.AltarBlock;
import com.emoniph.witchery.blocks.ChaliceBlock;
import com.emoniph.witchery.blocks.PlacedItemBlock;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.item.ArthanaItem;
import com.emoniph.witchery.item.MysticBranchItem;
import com.emoniph.witchery.item.PentacleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class AltarBlockEntity extends BlockEntity {

    private static final int SCAN_RADIUS = 14;
    private static final int SCAN_INTERVAL = 100;
    private static final int RECHARGE_INTERVAL = 20;
    private static final int BASE_RECHARGE = 10;

    private int power = 0;
    private int maxPower = 0;
    private int powerMultiplier = 1;
    private int rechargeScale = 1;
    private int rangeScale = 1;
    private int enhancementLevel = 0;
    private boolean validStructure = false;
    private int tickCount = 0;

    private record AltarLayout(Direction.Axis axis, Set<BlockPos> footprint, Map<BlockPos, AltarBlock.JoinedPart> parts) {
    }

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.ALTAR.get(), pos, state);
    }

    public int getPower() {
        return power;
    }

    public int getMaxPower() {
        return maxPower;
    }

    public int getEffectiveMaxPower() {
        return validStructure ? maxPower * powerMultiplier : 0;
    }

    public int getEffectiveRecharge() {
        return validStructure ? BASE_RECHARGE * rechargeScale : 0;
    }

    public int getRange() {
        return 16 * rangeScale;
    }

    public int getEnhancementLevel() {
        return enhancementLevel;
    }

    public boolean hasValidStructure() {
        return validStructure;
    }

    private enum Cat {
        DIRT_PODZOL("dirt_group", 1, 80),
        FARMLAND("farmland", 1, 100),
        MYCELIUM("mycelium", 1, 80),
        WATER("water", 1, 50),
        GRASS("grass", 2, 80),
        EARLY_MC_LOG("early_mc_log", 2, 50),
        LATER_MC_LOG("later_mc_log", 2, 50),
        VINE("vine", 2, 50),
        GLINT_WEED("glint_weed", 2, 20),
        GRASSPER("grassper", 2, 10),
        TALL_GRASS_FERN("tall_grass_fern", 3, 50),
        EARLY_MC_LEAVES("early_mc_leaves", 3, 100),
        LATER_MC_LEAVES("later_mc_leaves", 3, 100),
        CACTUS("cactus", 3, 50),
        SUGAR_CANE("sugar_cane", 3, 50),
        BROWN_MUSHROOM("brown_mushroom", 3, 20),
        RED_MUSHROOM("red_mushroom", 3, 20),
        BROWN_MUSHROOM_BLOCK("brown_mush_blk", 3, 20),
        RED_MUSHROOM_BLOCK("red_mush_blk", 3, 20),
        WITCHERY_LOG("witchery_log", 3, 100),
        SPANISH_MOSS("spanish_moss", 3, 20),
        WISPY_COTTON("wispy_cotton", 3, 20),
        WHEAT("wheat", 4, 20),
        CARROT("carrot", 4, 20),
        POTATO("potato", 4, 20),
        MELON("melon", 4, 20),
        PUMPKIN("pumpkin", 4, 20),
        VANILLA_SAPLING("vanilla_sap", 4, 20),
        WITCHERY_SAPLING("witchery_sap", 4, 20),
        DANDELION("dandelion", 4, 30),
        FLOWER("flower", 4, 30),
        WITCHERY_LEAVES("witchery_leaves", 4, 50),
        BELLADONNA("belladonna", 4, 20),
        ARTICHOKE("artichoke", 4, 20),
        EMBER_MOSS("ember_moss", 4, 20),
        MANDRAKE("mandrake", 4, 20),
        SNOWBELL("snowbell", 4, 20),
        BLOOD_ROSE("blood_rose", 2, 10),
        CRITTER_SNARE("critter_snare", 2, 10),
        DEMON_HEART("demon_heart", 100, 2),
        INFINITY_EGG("infinity_egg", 1000, 1),
        DRAGON_EGG("dragon_egg", 250, 1);

        final String groupKey;
        final int power;
        final int maxBlocks;

        Cat(String groupKey, int power, int maxBlocks) {
            this.groupKey = groupKey;
            this.power = power;
            this.maxBlocks = maxBlocks;
        }
    }

    private record UpgradeInfo(int powerMultiplier, int rechargeScale, int rangeScale, int enhancementLevel) {
    }

    private static @Nullable Cat classify(BlockState state) {
        if (state.is(Blocks.DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.COARSE_DIRT)) return Cat.DIRT_PODZOL;
        if (state.is(Blocks.FARMLAND)) return Cat.FARMLAND;
        if (state.is(Blocks.MYCELIUM)) return Cat.MYCELIUM;
        if (state.is(Blocks.WATER)) return Cat.WATER;
        if (state.is(Blocks.GRASS_BLOCK)) return Cat.GRASS;

        if (state.is(Blocks.OAK_LOG) || state.is(Blocks.BIRCH_LOG)
                || state.is(Blocks.SPRUCE_LOG) || state.is(Blocks.JUNGLE_LOG)
                || state.is(Blocks.OAK_WOOD) || state.is(Blocks.BIRCH_WOOD)
                || state.is(Blocks.SPRUCE_WOOD) || state.is(Blocks.JUNGLE_WOOD)) {
            return Cat.EARLY_MC_LOG;
        }
        if (state.is(Blocks.ACACIA_LOG) || state.is(Blocks.DARK_OAK_LOG)
                || state.is(Blocks.ACACIA_WOOD) || state.is(Blocks.DARK_OAK_WOOD)) {
            return Cat.LATER_MC_LOG;
        }

        if (state.is(Blocks.VINE)) return Cat.VINE;
        if (state.is(WitcheryBlocks.GLINT_WEED.get())) return Cat.GLINT_WEED;
        if (state.is(WitcheryBlocks.GRASSPER.get())) return Cat.GRASSPER;
        if (state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)) return Cat.TALL_GRASS_FERN;

        if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.BIRCH_LEAVES)
                || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.JUNGLE_LEAVES)) {
            return Cat.EARLY_MC_LEAVES;
        }
        if (state.is(Blocks.ACACIA_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES)) {
            return Cat.LATER_MC_LEAVES;
        }

        if (state.is(Blocks.CACTUS)) return Cat.CACTUS;
        if (state.is(Blocks.SUGAR_CANE)) return Cat.SUGAR_CANE;
        if (state.is(Blocks.BROWN_MUSHROOM)) return Cat.BROWN_MUSHROOM;
        if (state.is(Blocks.RED_MUSHROOM)) return Cat.RED_MUSHROOM;
        if (state.is(Blocks.BROWN_MUSHROOM_BLOCK)) return Cat.BROWN_MUSHROOM_BLOCK;
        if (state.is(Blocks.RED_MUSHROOM_BLOCK)) return Cat.RED_MUSHROOM_BLOCK;

        if (state.is(WitcheryBlocks.ROWAN_LOG.get()) || state.is(WitcheryBlocks.ROWAN_WOOD.get())
                || state.is(WitcheryBlocks.ALDER_LOG.get()) || state.is(WitcheryBlocks.HAWTHORN_LOG.get())) {
            return Cat.WITCHERY_LOG;
        }
        if (state.is(WitcheryBlocks.SPANISH_MOSS.get())) return Cat.SPANISH_MOSS;
        if (state.is(WitcheryBlocks.WISPY_COTTON.get()) || state.is(WitcheryBlocks.DISTURBED_COTTON.get())) return Cat.WISPY_COTTON;

        if (state.is(Blocks.WHEAT)) return Cat.WHEAT;
        if (state.is(Blocks.CARROTS)) return Cat.CARROT;
        if (state.is(Blocks.POTATOES)) return Cat.POTATO;
        if (state.is(Blocks.MELON)) return Cat.MELON;
        if (state.is(Blocks.PUMPKIN)) return Cat.PUMPKIN;
        if (state.is(WitcheryBlocks.ROWAN_SAPLING.get()) || state.is(WitcheryBlocks.ALDER_SAPLING.get())
                || state.is(WitcheryBlocks.HAWTHORN_SAPLING.get())) {
            return Cat.WITCHERY_SAPLING;
        }
        if (state.is(BlockTags.SAPLINGS)) return Cat.VANILLA_SAPLING;
        if (state.is(Blocks.DANDELION)) return Cat.DANDELION;
        if (state.is(Blocks.POPPY) || state.is(Blocks.BLUE_ORCHID) || state.is(Blocks.ALLIUM)
                || state.is(Blocks.AZURE_BLUET) || state.is(Blocks.RED_TULIP) || state.is(Blocks.ORANGE_TULIP)
                || state.is(Blocks.WHITE_TULIP) || state.is(Blocks.PINK_TULIP) || state.is(Blocks.OXEYE_DAISY)
                || state.is(Blocks.CORNFLOWER) || state.is(Blocks.LILY_OF_THE_VALLEY)) {
            return Cat.FLOWER;
        }

        if (state.is(WitcheryBlocks.ROWAN_LEAVES.get()) || state.is(WitcheryBlocks.ALDER_LEAVES.get())
                || state.is(WitcheryBlocks.HAWTHORN_LEAVES.get())) {
            return Cat.WITCHERY_LEAVES;
        }
        if (state.is(WitcheryBlocks.BELLADONNA.get())) return Cat.BELLADONNA;
        if (state.is(WitcheryBlocks.ARTICHOKE.get())) return Cat.ARTICHOKE;
        if (state.is(WitcheryBlocks.EMBER_MOSS.get())) return Cat.EMBER_MOSS;
        if (state.is(WitcheryBlocks.MANDRAKE.get())) return Cat.MANDRAKE;
        if (state.is(WitcheryBlocks.SNOWBELL.get())) return Cat.SNOWBELL;
        if (state.is(WitcheryBlocks.BLOOD_ROSE.get())) return Cat.BLOOD_ROSE;
        if (state.is(WitcheryBlocks.CRITTER_SNARE.get())) return Cat.CRITTER_SNARE;
        if (state.is(WitcheryBlocks.DEMON_HEART.get())) return Cat.DEMON_HEART;
        if (state.is(WitcheryBlocks.INFINITY_EGG.get())) return Cat.INFINITY_EGG;
        if (state.is(Blocks.DRAGON_EGG)) return Cat.DRAGON_EGG;

        return null;
    }

    /** Test hook: the altar power one block of {@code state} contributes (0 if it is not a source). */
    static int powerContribution(BlockState state) {
        Cat cat = classify(state);
        return cat == null ? 0 : cat.power;
    }

    /** Test hook: the cap on how many blocks of {@code state}'s group count (0 if it is not a source). */
    static int powerSourceCap(BlockState state) {
        Cat cat = classify(state);
        return cat == null ? 0 : cat.maxBlocks;
    }

    private static int calculateMaxPower(Level level, BlockPos center) {
        Map<String, Integer> counts = new HashMap<>();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                center.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {
            Cat cat = classify(level.getBlockState(pos));
            if (cat != null) {
                counts.merge(cat.groupKey, 1, Integer::sum);
            }
        }

        int total = 0;
        for (Cat cat : Cat.values()) {
            total += cat.power * Math.min(counts.getOrDefault(cat.groupKey, 0), cat.maxBlocks);
        }
        return total;
    }

    private static Set<BlockPos> findConnectedAltars(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!visited.add(current)) continue;

            for (net.minecraft.core.Direction direction : new net.minecraft.core.Direction[]{
                    net.minecraft.core.Direction.NORTH,
                    net.minecraft.core.Direction.SOUTH,
                    net.minecraft.core.Direction.EAST,
                    net.minecraft.core.Direction.WEST}) {
                BlockPos neighbor = current.relative(direction);
                if (!visited.contains(neighbor) && level.getBlockState(neighbor).is(WitcheryBlocks.ALTAR.get())) {
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }

    private static @Nullable AltarLayout findValidLayout(Set<BlockPos> footprint) {
        if (footprint.size() != 6) return null;

        int y = footprint.iterator().next().getY();
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos altarPos : footprint) {
            if (altarPos.getY() != y) return null;
            minX = Math.min(minX, altarPos.getX());
            maxX = Math.max(maxX, altarPos.getX());
            minZ = Math.min(minZ, altarPos.getZ());
            maxZ = Math.max(maxZ, altarPos.getZ());
        }

        int xSpan = maxX - minX + 1;
        int zSpan = maxZ - minZ + 1;
        Direction.Axis axis;
        if (xSpan == 3 && zSpan == 2) {
            axis = Direction.Axis.X;
        } else if (xSpan == 2 && zSpan == 3) {
            axis = Direction.Axis.Z;
        } else {
            return null;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!footprint.contains(new BlockPos(x, y, z))) {
                    return null;
                }
            }
        }

        Map<BlockPos, AltarBlock.JoinedPart> parts = new HashMap<>();
        for (BlockPos altarPos : footprint) {
            int row;
            int col;
            if (axis == Direction.Axis.X) {
                row = altarPos.getZ() - minZ;
                col = altarPos.getX() - minX;
            } else {
                row = altarPos.getX() - minX;
                col = altarPos.getZ() - minZ;
            }
            parts.put(altarPos, joinedPart(row, col));
        }

        return new AltarLayout(axis, footprint, parts);
    }

    private static AltarBlock.JoinedPart joinedPart(int row, int col) {
        return switch (row) {
            case 0 -> switch (col) {
                case 0 -> AltarBlock.JoinedPart.BACK_LEFT;
                case 1 -> AltarBlock.JoinedPart.BACK_MIDDLE;
                case 2 -> AltarBlock.JoinedPart.BACK_RIGHT;
                default -> AltarBlock.JoinedPart.NOT_CONNECTED;
            };
            case 1 -> switch (col) {
                case 0 -> AltarBlock.JoinedPart.FRONT_LEFT;
                case 1 -> AltarBlock.JoinedPart.FRONT_MIDDLE;
                case 2 -> AltarBlock.JoinedPart.FRONT_RIGHT;
                default -> AltarBlock.JoinedPart.NOT_CONNECTED;
            };
            default -> AltarBlock.JoinedPart.NOT_CONNECTED;
        };
    }

    private static UpgradeInfo scanUpgrades(Level level, Set<BlockPos> footprint) {
        if (footprint.isEmpty()) {
            return new UpgradeInfo(1, 1, 1, 0);
        }

        int skullPowerBonus = 0;
        int skullRechargeBonus = 0;
        int candleRechargeBonus = 0;
        int chalicePowerBonus = 0;
        boolean bookFound = false;
        boolean arthanaFound = false;
        boolean mysticBranchFound = false;
        boolean pentacleFound = false;
        boolean infinityEggFound = false;

        for (BlockPos altarPos : footprint) {
            BlockState above = level.getBlockState(altarPos.above());

            if (above.is(Blocks.SKELETON_SKULL) || above.is(Blocks.SKELETON_WALL_SKULL)) {
                skullRechargeBonus += 1;
                skullPowerBonus += 1;
            } else if (above.is(Blocks.WITHER_SKELETON_SKULL) || above.is(Blocks.WITHER_SKELETON_WALL_SKULL)) {
                skullRechargeBonus += 2;
                skullPowerBonus += 2;
            } else if (above.is(Blocks.PLAYER_HEAD) || above.is(Blocks.PLAYER_WALL_HEAD)) {
                skullRechargeBonus += 3;
                skullPowerBonus += 3;
            } else if (above.is(WitcheryBlocks.CANDELABRA.get())) {
                candleRechargeBonus = Math.max(candleRechargeBonus, 2);
            } else if (above.is(Blocks.TORCH) || above.is(Blocks.WALL_TORCH)) {
                candleRechargeBonus = Math.max(candleRechargeBonus, 1);
            } else if (above.is(WitcheryBlocks.CHALICE.get())) {
                chalicePowerBonus = Math.max(chalicePowerBonus, above.getValue(ChaliceBlock.FILLED) ? 2 : 1);
            } else if (above.is(WitcheryBlocks.PLACED_MODONOMICON.get())
                    && level.getBlockEntity(altarPos.above()) instanceof PlacedModonomiconBlockEntity placedBook
                    && !placedBook.getBookStack().isEmpty()) {
                bookFound = true;
            } else if (above.is(WitcheryBlocks.INFINITY_EGG.get())) {
                infinityEggFound = true;
            } else if (above.getBlock() instanceof PlacedItemBlock
                    && level.getBlockEntity(altarPos.above()) instanceof PlacedItemBlockEntity placedItem) {
                if (placedItem.getStack().getItem() instanceof ArthanaItem) {
                    arthanaFound = true;
                } else if (placedItem.getStack().getItem() instanceof MysticBranchItem) {
                    mysticBranchFound = true;
                } else if (placedItem.getStack().getItem() instanceof PentacleItem) {
                    pentacleFound = true;
                }
            }
        }

        int powerScale = 1 + skullPowerBonus + chalicePowerBonus;
        int rechargeScale = calculateRechargeScale(skullRechargeBonus, candleRechargeBonus,
                bookFound, pentacleFound, infinityEggFound);
        if (infinityEggFound) {
            powerScale *= 10;
        }

        return new UpgradeInfo(
                powerScale,
                rechargeScale,
                arthanaFound ? 2 : 1,
                mysticBranchFound ? 1 : 0
        );
    }

    static int calculateRechargeScale(int skullRechargeBonus, int candleRechargeBonus,
                                      boolean bookFound, boolean pentacleFound, boolean infinityEggFound) {
        int rechargeScale = 1 + skullRechargeBonus + candleRechargeBonus + (bookFound ? 1 : 0);
        if (pentacleFound) {
            rechargeScale *= 2;
        }
        if (infinityEggFound) {
            rechargeScale *= 10;
        }
        return rechargeScale;
    }

    private static void applyJoinedState(Level level, BlockPos pos, Direction.Axis axis, AltarBlock.JoinedPart part) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(WitcheryBlocks.ALTAR.get())) return;
        BlockState desired = current.setValue(AltarBlock.AXIS, axis).setValue(AltarBlock.PART, part);
        if (current != desired) {
            level.setBlock(pos, desired, 2);
        }
    }

    private static void syncVisualStates(Level level, Set<BlockPos> connectedAltars, @Nullable AltarLayout layout) {
        if (layout == null) {
            for (BlockPos altarPos : connectedAltars) {
                applyJoinedState(level, altarPos, Direction.Axis.Z, AltarBlock.JoinedPart.NOT_CONNECTED);
            }
            return;
        }

        for (BlockPos altarPos : connectedAltars) {
            AltarBlock.JoinedPart part = layout.parts().getOrDefault(altarPos, AltarBlock.JoinedPart.NOT_CONNECTED);
            applyJoinedState(level, altarPos, layout.axis(), part);
        }
    }

    private void refreshState(Level level, BlockState state) {
        Set<BlockPos> connectedAltars = findConnectedAltars(level, worldPosition);
        AltarLayout layout = findValidLayout(connectedAltars);
        boolean newValidStructure = layout != null;
        int newMaxPower = newValidStructure ? calculateMaxPower(level, worldPosition) : 0;
        UpgradeInfo upgrades = newValidStructure ? scanUpgrades(level, layout.footprint()) : new UpgradeInfo(1, 1, 1, 0);

        syncVisualStates(level, connectedAltars, layout);

        if (newValidStructure != validStructure
                || newMaxPower != maxPower
                || upgrades.powerMultiplier() != powerMultiplier
                || upgrades.rechargeScale() != rechargeScale
                || upgrades.rangeScale() != rangeScale
                || upgrades.enhancementLevel() != enhancementLevel) {
            validStructure = newValidStructure;
            maxPower = newMaxPower;
            powerMultiplier = upgrades.powerMultiplier();
            rechargeScale = upgrades.rechargeScale();
            rangeScale = upgrades.rangeScale();
            enhancementLevel = upgrades.enhancementLevel();
            power = Math.min(power, getEffectiveMaxPower());
            if (!validStructure) {
                power = 0;
            }
            setChanged();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AltarBlockEntity be) {
        be.tickCount++;

        if (be.tickCount == 1 || be.tickCount % SCAN_INTERVAL == 0) {
            be.refreshState(level, state);
        }

        int effectiveMax = be.getEffectiveMaxPower();
        if (effectiveMax <= 0) {
            if (be.power != 0) {
                be.power = 0;
                be.setChanged();
            }
            return;
        }

        if (be.tickCount % RECHARGE_INTERVAL == 0 && be.power < effectiveMax) {
            be.power = Math.min(be.power + be.getEffectiveRecharge(), effectiveMax);
            be.setChanged();
        }
    }

    public boolean consumePower(int amount) {
        if (!validStructure || power < amount) return false;
        power -= amount;
        setChanged();
        return true;
    }

    public void invalidate(Level level) {
        refreshState(level, getBlockState());
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        tag.putInt("Power", power);
        tag.putInt("MaxPower", maxPower);
        tag.putInt("PowerMultiplier", powerMultiplier);
        tag.putInt("RechargeScale", rechargeScale);
        tag.putInt("RangeScale", rangeScale);
        tag.putInt("EnhancementLevel", enhancementLevel);
        tag.putBoolean("ValidStructure", validStructure);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        power = tag.getIntOr("Power", 0);
        maxPower = tag.getIntOr("MaxPower", 0);
        powerMultiplier = tag.getIntOr("PowerMultiplier", 1);
        rechargeScale = tag.getIntOr("RechargeScale", Math.max(1, tag.getIntOr("RechargeBonus", 0) + 1));
        rangeScale = tag.getIntOr("RangeScale", 1);
        enhancementLevel = tag.getIntOr("EnhancementLevel", 0);
        validStructure = tag.getBooleanOr("ValidStructure", false);
    }
}
