package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.BoltEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Witch-Hunter pistol crossbow.
 *
 * Source parity: ItemHandBow 1.7.10-0.24.1.
 * Current practical slice fires all promoted source bolt families:
 * wooden/stake, nullifying, bone/holy, splitting, and silver.
 */
public class HandBowItem extends Item {

    private static final int TICKS_TO_LOAD = 10;
    private static final int MAX_DRAW_TICKS = 20;
    private static final float BOLT_SPEED = 3.0F;
    private static final float BOLT_DAMAGE = 6.0F;
    private static final float SPLITTING_BOLT_DAMAGE = 1.0F;
    private static final String TAG_CURRENT = "WITCBoltTypeCurrent";
    private static final String TAG_PREFERRED = "WITCBoltTypePreferred";

    private static final int NO_BOLT = 0;
    private static final int BOLT_STAKE = 1;
    private static final int BOLT_NULLIFYING = 2;
    private static final int BOLT_BONE = 3;
    private static final int BOLT_SPLITTING = 4;
    private static final int BOLT_SILVER = 5;
    private static final int[] BOLT_ORDER = {
            BOLT_STAKE,
            BOLT_NULLIFYING,
            BOLT_BONE,
            BOLT_SPLITTING,
            BOLT_SILVER
    };

    public HandBowItem(Properties properties) {
        super(properties.durability(768));
    }

    public static boolean hasLoadedBolt(ItemStack stack) {
        return getBoltType(TAG_CURRENT, stack) != NO_BOLT;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean hasLoadedBolt = hasLoadedBolt(stack);
        boolean canLoadBolt = player.isShiftKeyDown() && findAvailableBolt(player, getPreferredBolt(stack), NO_BOLT) != NO_BOLT;
        if (!hasLoadedBolt && !canLoadBolt) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof Player player) || !player.isShiftKeyDown() || level.isClientSide()) {
            return;
        }

        int elapsedTicks = getUseDuration(stack, entity) - remainingUseDuration;
        if ((elapsedTicks == 5 || elapsedTicks == 10 || elapsedTicks == 15)
                && countAvailableBoltTypes(player, getBoltType(TAG_CURRENT, stack)) > 0) {
            player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.35F, 1.8F);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return false;

        int elapsedTicks = getUseDuration(stack, entity) - timeLeft;
        int loadedBolt = getBoltType(TAG_CURRENT, stack);

        if (player.isShiftKeyDown() && elapsedTicks >= TICKS_TO_LOAD) {
            if (!level.isClientSide()) {
                if (loadedBolt == NO_BOLT) {
                    loadPreferredBolt(stack, level, player);
                } else {
                    cycleLoadedBolt(stack, level, player, loadedBolt);
                }
            }
            return true;
        }

        if (loadedBolt == NO_BOLT) {
            return false;
        }

        if (!level.isClientSide()) {
            Vec3 look = player.getLookAngle();
            float charge = sourceCharge(elapsedTicks >= MAX_DRAW_TICKS ? MAX_DRAW_TICKS : MAX_DRAW_TICKS - 1);
            ShotModifiers modifiers = shotModifiers(level, stack, loadedBolt);
            AbstractArrow.Pickup pickup = modifiers.infinity() || player.getAbilities().instabuild
                    ? AbstractArrow.Pickup.DISALLOWED
                    : AbstractArrow.Pickup.ALLOWED;
            boolean poweredDraining = loadedBolt == BOLT_NULLIFYING && isFullHunterSet(player);
            if (poweredDraining) {
                BoltEntity.reducePowerLevels(player, BoltEntity.SHOOTER_POWER_REDUCTION);
            }
            if (loadedBolt == BOLT_SPLITTING) {
                launchBolt(level, player, BoltEntity.WOODEN, charge, SPLITTING_BOLT_DAMAGE,
                        look.yRot((float) Math.toRadians(-20.0D)), pickup, modifiers, false);
                launchBolt(level, player, BoltEntity.WOODEN, charge, SPLITTING_BOLT_DAMAGE, look, pickup, modifiers, false);
                launchBolt(level, player, BoltEntity.WOODEN, charge, SPLITTING_BOLT_DAMAGE,
                        look.yRot((float) Math.toRadians(20.0D)), pickup, modifiers, false);
            } else {
                launchBolt(level, player, boltEntityTypeFor(loadedBolt), charge, BOLT_DAMAGE, look, pickup, modifiers,
                        poweredDraining);
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);

            if (!modifiers.infinity()) {
                setBoltType(TAG_CURRENT, stack, NO_BOLT);
            }
            stack.hurtAndBreak(2, player, EquipmentSlot.MAINHAND);
        }
        return true;
    }

    private static void launchBolt(Level level, Player player, int boltType, float charge, float damage, Vec3 direction,
                                   AbstractArrow.Pickup pickup, ShotModifiers modifiers, boolean poweredDraining) {
        BoltEntity bolt = new BoltEntity(level, player, boltType);
        bolt.setBaseDamage((damage + modifiers.damageBonus()) * charge);
        bolt.setKnockbackStrength(modifiers.knockback());
        bolt.setPoweredDraining(poweredDraining);
        if (modifiers.flame()) {
            bolt.igniteForSeconds(100.0F);
        }
        if (charge >= 1.0F) {
            bolt.setCritArrow(true);
        }
        bolt.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        bolt.shoot(direction.x, direction.y, direction.z, BOLT_SPEED * charge, 1.0F);
        bolt.pickup = pickup;
        level.addFreshEntity(bolt);
    }

    private static boolean loadPreferredBolt(ItemStack bowStack, Level level, Player player) {
        int boltToLoad = findAvailableBolt(player, getPreferredBolt(bowStack), NO_BOLT);
        if (boltToLoad == NO_BOLT) {
            return false;
        }

        setBoltType(TAG_CURRENT, bowStack, boltToLoad);
        consumeLoadedBolt(level, player, boltToLoad);
        return true;
    }

    private static boolean cycleLoadedBolt(ItemStack bowStack, Level level, Player player, int loadedBolt) {
        int nextBolt = findAvailableBolt(player, nextBoltId(loadedBolt), loadedBolt);
        if (nextBolt == NO_BOLT) {
            return false;
        }

        setBoltType(TAG_CURRENT, bowStack, nextBolt);
        setBoltType(TAG_PREFERRED, bowStack, nextBolt);
        consumeLoadedBolt(level, player, nextBolt);
        if (!player.getAbilities().instabuild) {
            returnLoadedBolt(player, loadedBolt);
        }
        return true;
    }

    private static void consumeLoadedBolt(Level level, Player player, int boltId) {
        if (!player.getAbilities().instabuild) {
            consumeBolt(player, boltId);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 0.8F, 1.0F);
    }

    private static int findAvailableBolt(Player player, int startBoltId, int boltIdToIgnore) {
        int candidate = startBoltId == NO_BOLT ? BOLT_STAKE : startBoltId;
        for (int i = 0; i < BOLT_ORDER.length; i++) {
            if (candidate != boltIdToIgnore && hasBolt(player, candidate)) {
                return candidate;
            }
            candidate = nextBoltId(candidate);
        }
        return NO_BOLT;
    }

    private static int countAvailableBoltTypes(Player player, int boltIdToIgnore) {
        Set<Integer> boltTypes = new HashSet<>();
        for (int boltId : BOLT_ORDER) {
            if (boltId != boltIdToIgnore && hasBolt(player, boltId)) {
                boltTypes.add(boltId);
            }
        }
        return boltTypes.size();
    }

    private static boolean hasBolt(Player player, int boltId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (isBoltStack(player.getInventory().getItem(i), boltId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeBolt(Player player, int boltId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inventoryStack = player.getInventory().getItem(i);
            if (isBoltStack(inventoryStack, boltId)) {
                inventoryStack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void returnLoadedBolt(Player player, int boltId) {
        Item item = primaryItemForBoltId(boltId);
        if (item == null) return;

        ItemStack returnedBolt = new ItemStack(item);
        if (!player.getInventory().add(returnedBolt)) {
            player.drop(returnedBolt, false);
        }
    }

    private static int getPreferredBolt(ItemStack stack) {
        int preferredBolt = getBoltType(TAG_PREFERRED, stack);
        return preferredBolt == NO_BOLT ? BOLT_STAKE : preferredBolt;
    }

    private static int getBoltType(String key, ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int boltId = tag.getIntOr(key, NO_BOLT);
        return isValidBoltId(boltId) ? boltId : NO_BOLT;
    }

    private static void setBoltType(String key, ItemStack stack, int boltId) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (boltId == NO_BOLT) {
                tag.remove(key);
            } else {
                tag.putInt(key, boltId);
            }
        });
    }

    private static boolean isValidBoltId(int boltId) {
        for (int id : BOLT_ORDER) {
            if (id == boltId) return true;
        }
        return false;
    }

    private static int nextBoltId(int boltId) {
        int next = boltId + 1;
        return next > BOLT_ORDER.length ? BOLT_STAKE : next;
    }

    private static float sourceCharge(int ticks) {
        float charge = ticks / (float) MAX_DRAW_TICKS;
        charge = (charge * charge + charge * 2.0F) / 3.0F;
        if (charge > 1.0F) {
            charge = 1.0F;
        }
        return charge;
    }

    private static int boltEntityTypeFor(int boltId) {
        return switch (boltId) {
            case BOLT_SILVER -> BoltEntity.SILVER;
            case BOLT_NULLIFYING -> BoltEntity.NULLIFYING;
            case BOLT_BONE -> BoltEntity.BONE;
            default -> BoltEntity.WOODEN;
        };
    }

    private static Item primaryItemForBoltId(int boltId) {
        return switch (boltId) {
            case BOLT_STAKE -> WitcheryItems.WOODEN_BOLT.get();
            case BOLT_NULLIFYING -> WitcheryItems.NULLIFYING_BOLT.get();
            case BOLT_BONE -> WitcheryItems.BONE_BOLT.get();
            case BOLT_SPLITTING -> WitcheryItems.SPLITTING_BOLT.get();
            case BOLT_SILVER -> WitcheryItems.SILVER_BOLT.get();
            default -> null;
        };
    }

    private static boolean isBoltStack(ItemStack stack, int boltId) {
        return switch (boltId) {
            case BOLT_STAKE -> stack.is(WitcheryItems.WOODEN_BOLT.get())
                    || stack.is(WitcheryItems.BOLT_STAKE.get());
            case BOLT_NULLIFYING -> stack.is(WitcheryItems.NULLIFYING_BOLT.get())
                    || stack.is(WitcheryItems.BOLT_ANTI_MAGIC.get());
            case BOLT_BONE -> stack.is(WitcheryItems.BONE_BOLT.get())
                    || stack.is(WitcheryItems.BOLT_HOLY.get());
            case BOLT_SPLITTING -> stack.is(WitcheryItems.SPLITTING_BOLT.get())
                    || stack.is(WitcheryItems.BOLT_SPLITTING.get());
            case BOLT_SILVER -> stack.is(WitcheryItems.SILVER_BOLT.get());
            default -> false;
        };
    }

    private static boolean isFullHunterSet(LivingEntity entity) {
        return isAnyHunterPiece(entity.getItemBySlot(EquipmentSlot.HEAD),
                WitcheryItems.HUNTER_HELMET.get(),
                WitcheryItems.HUNTER_HELMET_SILVERED.get(),
                WitcheryItems.WITCH_HUNTER_DAWN_HAT.get())
                && isAnyHunterPiece(entity.getItemBySlot(EquipmentSlot.CHEST),
                WitcheryItems.HUNTER_CHESTPLATE.get(),
                WitcheryItems.HUNTER_CHESTPLATE_SILVERED.get(),
                WitcheryItems.WITCH_HUNTER_DAWN_COAT.get())
                && isAnyHunterPiece(entity.getItemBySlot(EquipmentSlot.LEGS),
                WitcheryItems.HUNTER_LEGGINGS.get(),
                WitcheryItems.HUNTER_LEGGINGS_SILVERED.get(),
                WitcheryItems.WITCH_HUNTER_DAWN_TROUSERS.get())
                && isAnyHunterPiece(entity.getItemBySlot(EquipmentSlot.FEET),
                WitcheryItems.HUNTER_BOOTS.get(),
                WitcheryItems.HUNTER_BOOTS_SILVERED.get(),
                WitcheryItems.WITCH_HUNTER_DAWN_BOOTS.get());
    }

    private static boolean isAnyHunterPiece(ItemStack stack, Item... items) {
        for (Item item : items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static ShotModifiers shotModifiers(Level level, ItemStack bowStack, int loadedBolt) {
        boolean infinity = enchantmentLevel(level, bowStack, Enchantments.INFINITY) > 0
                && level.random.nextDouble() < 0.25D;
        if (loadedBolt == BOLT_NULLIFYING) {
            return new ShotModifiers(infinity, 0.0D, 0, false);
        }

        int power = enchantmentLevel(level, bowStack, Enchantments.POWER);
        double damageBonus = power > 0 ? power * 0.5D + 0.5D : 0.0D;
        int punch = enchantmentLevel(level, bowStack, Enchantments.PUNCH);
        boolean flame = enchantmentLevel(level, bowStack, Enchantments.FLAME) > 0;
        return new ShotModifiers(infinity, damageBonus, punch, flame);
    }

    private static int enchantmentLevel(Level level, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return stack.getEnchantmentLevel(level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(enchantment));
    }

    private record ShotModifiers(boolean infinity, double damageBonus, int knockback, boolean flame) {}
}
