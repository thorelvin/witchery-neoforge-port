package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.ThrownBrewEntity;
import com.emoniph.witchery.menu.BrewBagMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;
import java.util.WeakHashMap;
import java.util.function.Consumer;

@SuppressWarnings({"deprecation", "removal"})
public class BrewBagItem extends Item {
    private static final String TAG_COLOR = "WITCColor";
    private static final int USE_DURATION = 36_000;
    private static final float ORTHOGONAL_THRESHOLD = 7.0F;
    private static final float DIAGONAL_THRESHOLD = 3.5F;

    public static final int DEFAULT_COLOR = 5_057_301;
    public static final int SLOT_COUNT = 8;

    private static final Map<Player, RadialUseState> RADIAL_USES =
            Collections.synchronizedMap(new WeakHashMap<>());

    public BrewBagItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack bag = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            clearRadialUse(player);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, menuPlayer) -> new BrewBagMenu(containerId, inventory, hand),
                        bag.getHoverName()), data -> data.writeEnum(hand));
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }

        RADIAL_USES.put(player, new RadialUseState(bag, player.getXRot(), player.getYRot()));
        player.startUsingItem(hand);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player)) {
            return;
        }
        RadialUseState state = RADIAL_USES.get(player);
        if (state == null || state.carrier != stack) {
            state = new RadialUseState(stack, player.getXRot(), player.getYRot());
            RADIAL_USES.put(player, state);
        }
        if (state.selectedSlot >= 0) {
            return;
        }

        float pitchDifference = state.anchorPitch - player.getXRot();
        float yawDifference = Mth.wrapDegrees(state.anchorYaw - player.getYRot());
        int slot = slotForMovement(pitchDifference, yawDifference);
        if (slot < 0) {
            return;
        }
        if (getStoredBrew(stack, slot).isEmpty()) {
            state.anchorPitch = player.getXRot();
            state.anchorYaw = player.getYRot();
        } else {
            state.selectedSlot = slot;
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        RadialUseState state = RADIAL_USES.remove(player);
        if (!level.isClientSide() && state != null && state.carrier == stack && state.selectedSlot >= 0) {
            throwStoredBrew(level, player, stack, state.selectedSlot);
        }
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.witchery.brew_bag.tip")
                .withStyle(ChatFormatting.GRAY));
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stored = getStoredBrew(stack, slot);
            if (!stored.isEmpty()) {
                tooltip.accept(Component.translatable("item.witchery.brew_bag.tooltip.slot",
                        slot + 1, stored.getCount(), stored.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    public static boolean throwStoredBrew(Level level, Player player, ItemStack bag, int slot) {
        ItemStack stored = getStoredBrew(bag, slot);
        if (!isStorableBrew(stored)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
            return false;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        ThrownBrewEntity projectile = ThrowableBrewSupport.createProjectile(level, player, stored);
        if (projectile == null) {
            return false;
        }
        projectile.shootFromSourceThrower(player);
        level.addFreshEntity(projectile);

        if (!player.getAbilities().instabuild) {
            stored = stored.copy();
            stored.shrink(1);
            StoredBrewInventory.set(bag, slot, stored);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        return true;
    }

    public static ItemStack getStoredBrew(ItemStack bag, int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? StoredBrewInventory.get(bag, slot) : ItemStack.EMPTY;
    }

    public static void setStoredBrew(ItemStack bag, int slot, ItemStack brew) {
        if (slot >= 0 && slot < SLOT_COUNT && (brew.isEmpty() || isStorableBrew(brew))) {
            StoredBrewInventory.set(bag, slot, brew);
        }
    }

    public static boolean isStorableBrew(ItemStack stack) {
        return ThrowableBrewSupport.isThrowableBrew(stack);
    }

    public static boolean isRadialActive(Player player) {
        return RADIAL_USES.containsKey(player);
    }

    public static int radialSelection(Player player) {
        RadialUseState state = RADIAL_USES.get(player);
        return state == null ? -1 : state.selectedSlot;
    }

    public static void clearRadialUse(Player player) {
        RADIAL_USES.remove(player);
    }

    /** Slot order is clockwise from the top, matching the original radial and inventory row. */
    public static int slotForMovement(float pitchDifference, float yawDifference) {
        if (pitchDifference >= DIAGONAL_THRESHOLD && yawDifference <= -DIAGONAL_THRESHOLD) return 1;
        if (pitchDifference <= -DIAGONAL_THRESHOLD && yawDifference <= -DIAGONAL_THRESHOLD) return 3;
        if (pitchDifference <= -DIAGONAL_THRESHOLD && yawDifference >= DIAGONAL_THRESHOLD) return 5;
        if (pitchDifference >= DIAGONAL_THRESHOLD && yawDifference >= DIAGONAL_THRESHOLD) return 7;
        if (pitchDifference >= ORTHOGONAL_THRESHOLD) return 0;
        if (pitchDifference <= -ORTHOGONAL_THRESHOLD) return 4;
        if (yawDifference <= -ORTHOGONAL_THRESHOLD) return 2;
        if (yawDifference >= ORTHOGONAL_THRESHOLD) return 6;
        return -1;
    }

    public static int getColor(ItemStack stack) {
        return customData(stack).copyTag().getIntOr(TAG_COLOR, DEFAULT_COLOR);
    }

    public static void setColor(ItemStack stack, int color) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                root -> root.putInt(TAG_COLOR, color & 0xFFFFFF));
    }

    public static OptionalInt colorForDye(ItemStack stack) {
        if (stack.is(Items.BLACK_DYE)) return OptionalInt.of(1_118_481);
        if (stack.is(Items.RED_DYE)) return OptionalInt.of(12_464_176);
        if (stack.is(Items.GREEN_DYE)) return OptionalInt.of(5_732_898);
        if (stack.is(Items.BROWN_DYE)) return OptionalInt.of(DEFAULT_COLOR);
        if (stack.is(Items.BLUE_DYE)) return OptionalInt.of(3_944_738);
        if (stack.is(Items.PURPLE_DYE)) return OptionalInt.of(8_998_492);
        if (stack.is(Items.CYAN_DYE)) return OptionalInt.of(14_837_520);
        if (stack.is(Items.LIGHT_GRAY_DYE)) return OptionalInt.of(11_513_789);
        if (stack.is(Items.GRAY_DYE)) return OptionalInt.of(7_763_574);
        if (stack.is(Items.PINK_DYE)) return OptionalInt.of(15_574_987);
        if (stack.is(Items.LIME_DYE)) return OptionalInt.of(8_639_516);
        if (stack.is(Items.YELLOW_DYE)) return OptionalInt.of(15_197_994);
        if (stack.is(Items.LIGHT_BLUE_DYE)) return OptionalInt.of(12_179_199);
        if (stack.is(Items.MAGENTA_DYE)) return OptionalInt.of(14_383_829);
        if (stack.is(Items.ORANGE_DYE)) return OptionalInt.of(15_113_780);
        if (stack.is(Items.WHITE_DYE)) return OptionalInt.of(0xFFFFFF);
        return OptionalInt.empty();
    }

    private static CustomData customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    }

    private static final class RadialUseState {
        private final ItemStack carrier;
        private float anchorPitch;
        private float anchorYaw;
        private int selectedSlot = -1;

        private RadialUseState(ItemStack carrier, float anchorPitch, float anchorYaw) {
            this.carrier = carrier;
            this.anchorPitch = anchorPitch;
            this.anchorYaw = anchorYaw;
        }
    }
}
