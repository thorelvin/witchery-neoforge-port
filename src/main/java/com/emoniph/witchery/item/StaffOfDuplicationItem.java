package com.emoniph.witchery.item;

import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class StaffOfDuplicationItem extends Item {
    private static final String TAG_MARKER = "Marker";
    private static final String TAG_SCHEMATIC = "SavedSchematic";
    private static final String TAG_BLOCKS = "Blocks";
    private static final String TAG_STATE = "State";
    private static final String TAG_X = "X";
    private static final String TAG_Y = "Y";
    private static final String TAG_Z = "Z";
    private static final String TAG_X_MAX = "xMax";
    private static final String TAG_Y_MAX = "yMax";
    private static final String TAG_Z_MAX = "zMax";
    private static final String TAG_ROTATION = "Rotation";
    private static final int MAX_DIMENSION = 32;
    private static final int MAX_BLOCKS = 8192;

    public StaffOfDuplicationItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !hasStoredData(stack)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            clearData(stack);
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.5F, 1.1F);
            player.displayClientMessage(Component.translatable("item.witchery.staff_of_duplication.cleared"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clicked = context.getClickedPos();
        if (!level.mayInteract(player, clicked)) {
            return InteractionResult.FAIL;
        }

        CompoundTag root = customData(stack);
        if (root.contains(TAG_SCHEMATIC)) {
            placeStoredSchematic(stack, (ServerLevel) level, clicked.above(), player);
        } else if (root.contains(TAG_MARKER)) {
            saveSchematic(stack, (ServerLevel) level, clicked, player);
        } else {
            setMarker(stack, (ServerLevel) level, clicked, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        CompoundTag root = customData(stack);
        if (root.contains(TAG_SCHEMATIC)) {
            CompoundTag schematic = root.getCompoundOrEmpty(TAG_SCHEMATIC);
            tooltip.accept(Component.translatable("item.witchery.staff_of_duplication.saved",
                    schematic.getIntOr(TAG_X_MAX, 0), schematic.getIntOr(TAG_Y_MAX, 0),
                    schematic.getIntOr(TAG_Z_MAX, 0)).withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.accept(Component.translatable("item.witchery.staff_of_duplication.rotation",
                    rotationName(root.getIntOr(TAG_ROTATION, 0))).withStyle(ChatFormatting.GRAY));
        } else if (root.contains(TAG_MARKER)) {
            BlockPos marker = readMarker(root);
            tooltip.accept(Component.translatable("item.witchery.staff_of_duplication.marked",
                    marker.getX(), marker.getY(), marker.getZ()).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.accept(Component.translatable("item.witchery.staff_of_duplication.empty")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static void setMarker(ItemStack stack, ServerLevel level, BlockPos pos, Player player) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag marker = new CompoundTag();
            marker.putInt(TAG_X, pos.getX());
            marker.putInt(TAG_Y, pos.getY());
            marker.putInt(TAG_Z, pos.getZ());
            tag.put(TAG_MARKER, marker);
            tag.remove(TAG_SCHEMATIC);
            tag.putInt(TAG_ROTATION, 0);
        });
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.25, 0.25, 0.25, 0.03);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.5F, 1.3F);
        player.displayClientMessage(Component.translatable("item.witchery.staff_of_duplication.marker"), true);
    }

    private static void saveSchematic(ItemStack stack, ServerLevel level, BlockPos second, Player player) {
        CompoundTag root = customData(stack);
        BlockPos marker = readMarker(root);
        int minX = Math.min(marker.getX(), second.getX()) + 1;
        int maxX = Math.max(marker.getX(), second.getX()) - 1;
        int minY = Math.min(marker.getY(), second.getY()) + 1;
        int maxY = Math.max(marker.getY(), second.getY()) - 1;
        int minZ = Math.min(marker.getZ(), second.getZ()) + 1;
        int maxZ = Math.max(marker.getZ(), second.getZ()) - 1;

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        if (width <= 0 || height <= 0 || depth <= 0
                || width > MAX_DIMENSION || height > MAX_DIMENSION || depth > MAX_DIMENSION
                || width * height * depth > MAX_BLOCKS) {
            fail(level, second, player, "item.witchery.staff_of_duplication.too_large");
            return;
        }

        ListTag blocks = new ListTag();
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    BlockPos pos = new BlockPos(minX + dx, minY + dy, minZ + dz);
                    CompoundTag entry = new CompoundTag();
                    entry.putInt(TAG_X, dx);
                    entry.putInt(TAG_Y, dy);
                    entry.putInt(TAG_Z, dz);
                    entry.put(TAG_STATE, NbtUtils.writeBlockState(level.getBlockState(pos)));
                    blocks.add(entry);
                }
            }
        }

        CompoundTag schematic = new CompoundTag();
        schematic.put(TAG_BLOCKS, blocks);
        schematic.putInt(TAG_X_MAX, width);
        schematic.putInt(TAG_Y_MAX, height);
        schematic.putInt(TAG_Z_MAX, depth);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.put(TAG_SCHEMATIC, schematic);
            tag.remove(TAG_MARKER);
            tag.putInt(TAG_ROTATION, 0);
        });
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH,
                second.getX() + 0.5, second.getY() + 0.5, second.getZ() + 0.5,
                24, 0.5, 0.5, 0.5, 0.04);
        level.playSound(null, second, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.6F, 1.8F);
        player.displayClientMessage(Component.translatable("item.witchery.staff_of_duplication.saved",
                width, height, depth), true);
    }

    private static void placeStoredSchematic(ItemStack stack, ServerLevel level, BlockPos origin, Player player) {
        CompoundTag root = customData(stack);
        CompoundTag schematic = root.getCompoundOrEmpty(TAG_SCHEMATIC);
        int rotationIndex;
        if (player.isShiftKeyDown()) {
            rotationIndex = 0;
        } else {
            rotationIndex = (root.getIntOr(TAG_ROTATION, 0) + 1) & 3;
        }
        Rotation rotation = toRotation(rotationIndex);
        int width = schematic.getIntOr(TAG_X_MAX, 0);
        int depth = schematic.getIntOr(TAG_Z_MAX, 0);
        ListTag blocks = schematic.getListOrEmpty(TAG_BLOCKS);
        HolderLookup.RegistryLookup<Block> blockLookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompoundOrEmpty(i);
            BlockState state = readState(blockLookup, entry);
            BlockPos target = origin.offset(rotatedX(entry.getIntOr(TAG_X, 0), entry.getIntOr(TAG_Z, 0), width, depth, rotationIndex),
                    entry.getIntOr(TAG_Y, 0),
                    rotatedZ(entry.getIntOr(TAG_X, 0), entry.getIntOr(TAG_Z, 0), width, depth, rotationIndex));
            if (canPlaceSchematicBlock(level, player, target)) {
                level.setBlock(target, state.rotate(rotation), Block.UPDATE_ALL);
            }
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_ROTATION, rotationIndex));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5,
                24, 0.6, 0.6, 0.6, 0.03);
        level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.2F);
        player.displayClientMessage(Component.translatable("item.witchery.staff_of_duplication.placed",
                rotationName(rotationIndex)), true);
    }

    private static BlockState readState(HolderLookup.RegistryLookup<Block> blockLookup, CompoundTag entry) {
        if (!entry.contains(TAG_STATE)) {
            return Blocks.AIR.defaultBlockState();
        }
        return NbtUtils.readBlockState(blockLookup, entry.getCompoundOrEmpty(TAG_STATE));
    }

    private static boolean canPlaceSchematicBlock(ServerLevel level, Player player, BlockPos target) {
        BlockState current = level.getBlockState(target);
        return current.isAir()
                ? WitcheryTerrainProtection.canPlace(player, level, target)
                : WitcheryTerrainProtection.canTransform(player, level, target);
    }

    private static int rotatedX(int x, int z, int width, int depth, int rotation) {
        return switch (rotation & 3) {
            case 1 -> z;
            case 2 -> width - 1 - x;
            case 3 -> depth - 1 - z;
            default -> x;
        };
    }

    private static int rotatedZ(int x, int z, int width, int depth, int rotation) {
        return switch (rotation & 3) {
            case 1 -> width - 1 - x;
            case 2 -> depth - 1 - z;
            case 3 -> x;
            default -> z;
        };
    }

    private static Rotation toRotation(int rotation) {
        return switch (rotation & 3) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private static String rotationName(int rotation) {
        return switch (rotation & 3) {
            case 1 -> "90";
            case 2 -> "180";
            case 3 -> "270";
            default -> "0";
        };
    }

    private static BlockPos readMarker(CompoundTag root) {
        CompoundTag marker = root.getCompoundOrEmpty(TAG_MARKER);
        return new BlockPos(marker.getIntOr(TAG_X, 0), marker.getIntOr(TAG_Y, 0), marker.getIntOr(TAG_Z, 0));
    }

    private static void clearData(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(TAG_MARKER);
            tag.remove(TAG_SCHEMATIC);
            tag.remove(TAG_ROTATION);
        });
    }

    private static boolean hasStoredData(ItemStack stack) {
        CompoundTag tag = customData(stack);
        return tag.contains(TAG_MARKER) || tag.contains(TAG_SCHEMATIC);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void fail(ServerLevel level, BlockPos pos, Player player, String key) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.5F, 0.7F);
        player.displayClientMessage(Component.translatable(key), true);
    }
}
