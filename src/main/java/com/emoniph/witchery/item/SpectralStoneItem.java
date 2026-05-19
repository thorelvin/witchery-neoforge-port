package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.BansheeEntity;
import com.emoniph.witchery.entity.PoltergeistEntity;
import com.emoniph.witchery.entity.SpectreEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpectralStoneItem extends Item {
    private static final int TICKS_TO_ACTIVATE = 40;
    private static final int MAX_USE_DURATION = 400;

    private static final String TAG_TYPE = "SpectralType";
    private static final String TAG_COUNT = "SpectralCount";

    public enum StoredType {
        NONE(""),
        SPECTRE("entity.witchery.spectre"),
        BANSHEE("entity.witchery.banshee"),
        POLTERGEIST("entity.witchery.poltergeist");

        private final String translationKey;

        StoredType(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    public SpectralStoneItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        int elapsedTicks = getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (elapsedTicks == TICKS_TO_ACTIVATE && level.isClientSide()) {
            livingEntity.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!(livingEntity instanceof Player player)) {
            return false;
        }

        int elapsedTicks = getUseDuration(stack, livingEntity) - timeCharged;
        StoredType storedType = getStoredType(stack);
        int storedCount = Math.min(getStoredCount(stack), 3);
        if (elapsedTicks < TICKS_TO_ACTIVATE || storedType == StoredType.NONE || storedCount <= 0) {
            player.playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 1.0f, 1.0f);
            return false;
        }

        HitResult hitResult = player.pick(16.0, 1.0f, true);
        Vec3 spawnOrigin = getSpawnOrigin(level, hitResult);
        if (spawnOrigin == null) {
            player.playSound(SoundEvents.NOTE_BLOCK_SNARE.value(), 1.0f, 1.0f);
            return false;
        }

        if (!level.isClientSide()) {
            for (int i = 0; i < storedCount; i++) {
                spawnStoredSpirit(level, player, storedType, spawnOrigin, i, storedCount);
            }

            if (!player.isCreative()) {
                if (stack.getCount() > 1) {
                    stack.shrink(1);
                    ItemStack emptyStone = new ItemStack(WitcheryItems.SPECTRAL_STONE.get());
                    if (!player.getInventory().add(emptyStone)) {
                        player.drop(emptyStone, false);
                    }
                } else {
                    clearBinding(stack);
                }
            }
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        StoredType type = getStoredType(stack);
        int count = getStoredCount(stack);
        if (type == StoredType.NONE || count <= 0) {
            tooltip.accept(Component.translatable("witchery.spectral_stone.tooltip.empty")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            return;
        }

        tooltip.accept(Component.translatable(type.translationKey())
                .append(Component.literal(": " + count))
                .withStyle(ChatFormatting.GRAY));
    }

    public static boolean isBound(ItemStack stack) {
        return getStoredType(stack) != StoredType.NONE && getStoredCount(stack) > 0;
    }

    public static void clearBinding(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(TAG_TYPE);
            tag.remove(TAG_COUNT);
        });
    }

    public static void bind(ItemStack stack, StoredType type, int count) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (type == StoredType.NONE || count <= 0) {
                tag.remove(TAG_TYPE);
                tag.remove(TAG_COUNT);
            } else {
                tag.putString(TAG_TYPE, type.name());
                tag.putInt(TAG_COUNT, Math.min(count, 3));
            }
        });
    }

    public static StoredType getStoredType(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(TAG_TYPE)) {
            return StoredType.NONE;
        }

        try {
            return StoredType.valueOf(tag.getStringOr(TAG_TYPE, StoredType.NONE.name()));
        } catch (IllegalArgumentException ignored) {
            return StoredType.NONE;
        }
    }

    public static int getStoredCount(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(TAG_COUNT)) {
            return 0;
        }
        return Math.max(0, Math.min(3, tag.getIntOr(TAG_COUNT, 0)));
    }

    private static Vec3 getSpawnOrigin(Level level, HitResult hitResult) {
        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos placePos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
            if (!level.getBlockState(placePos).canBeReplaced()) {
                BlockPos clickedPos = blockHitResult.getBlockPos();
                if (level.getBlockState(clickedPos).canBeReplaced()) {
                    placePos = clickedPos;
                }
            }
            return new Vec3(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5);
        }

        Vec3 location = hitResult.getLocation();
        return new Vec3(location.x, location.y - 1.0, location.z);
    }

    private static void spawnStoredSpirit(Level level, Player player, StoredType storedType, Vec3 spawnOrigin, int index, int totalCount) {
        Mob mob = createStoredSpirit(level, storedType);
        if (mob == null) {
            return;
        }

        double spread = totalCount <= 1 ? 0.0 : 0.6;
        double angle = totalCount <= 1 ? 0.0 : (Math.PI * 2.0 * index) / totalCount;
        double x = spawnOrigin.x + Math.cos(angle) * spread;
        double z = spawnOrigin.z + Math.sin(angle) * spread;
        mob.setPos(x, spawnOrigin.y, z);
        mob.setYRot(player.getYRot());
        mob.setXRot(0.0f);
        if (mob instanceof SpectreEntity spectre) {
            spectre.setOwner(player);
        } else if (mob instanceof BansheeEntity banshee) {
            banshee.setOwner(player);
        } else if (mob instanceof PoltergeistEntity poltergeist) {
            poltergeist.setOwner(player);
        }
        level.addFreshEntity(mob);
    }

    private static Mob createStoredSpirit(Level level, StoredType storedType) {
        return switch (storedType) {
            case SPECTRE -> WitcheryEntities.SPECTRE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            case BANSHEE -> WitcheryEntities.BANSHEE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            case POLTERGEIST -> WitcheryEntities.POLTERGEIST.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            case NONE -> null;
        };
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
