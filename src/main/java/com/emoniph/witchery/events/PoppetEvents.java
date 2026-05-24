package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.PoppetShelfBlockEntity;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.WitchHunterEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.PoppetItem;
import com.emoniph.witchery.item.ProtectionPoppetItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import com.emoniph.witchery.item.VampiricPoppetItem;
import com.emoniph.witchery.item.VoodooPoppetItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@EventBusSubscriber
public class PoppetEvents {

    private static final String TAG_POPPET_DAMAGE = "witchery_damage";
    private static final int POPPET_MAX_DAMAGE = 1000;
    private static final int VAMPIRIC_POPPET_DAMAGE = 66;

    private record PoppetLocation(ItemStack stack, @org.jetbrains.annotations.Nullable Inventory inventory,
                                  @org.jetbrains.annotations.Nullable PoppetShelfBlockEntity shelf, int slot) {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onVampiricRedirect(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewDamage() <= 0) return;

        // Poppet must be source-bound to this player AND have a victim bound
        PoppetLocation location = findFirstBoundPoppet(player,
                stack -> stack.is(WitcheryItems.VAMPIRIC_POPPET.get())
                        && VampiricPoppetItem.getVictimUUID(stack).isPresent());
        if (location == null) return;

        UUID victimUUID = VampiricPoppetItem.getVictimUUID(location.stack()).orElse(null);
        if (victimUUID == null) return;

        LivingEntity victim = PoppetItem.findLivingEntity(((ServerLevel) player.level()).getServer(), victimUUID)
                .orElse(null);
        if (victim == null || victim == player) return;

        float redirectedDamage = event.getNewDamage();
        victim.hurt(event.getSource(), redirectedDamage);
        damagePoppet(location, VAMPIRIC_POPPET_DAMAGE);
        event.setNewDamage(0.0F);
        WitchHunterEntity.blackMagicPerformed(player);

        if (victim instanceof ServerPlayer victimPlayer) {
            victimPlayer.displayClientMessage(Component.translatable("witchery.poppet.vampiric.target_hurt",
                    player.getName()), false);
        }
        player.displayClientMessage(Component.translatable("witchery.poppet.vampiric.activated"), false);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProtectionDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewDamage() <= 0) return;

        DamageSource source = event.getSource();
        float healthAfterDamage = player.getHealth() - event.getNewDamage();

        PoppetLocation typedProtection = findFirstBoundPoppet(player,
                stack -> stack.getItem() instanceof ProtectionPoppetItem protection
                        && protection.getProtectionType() != ProtectionPoppetItem.ProtectionType.POPPET_SHIELD
                        && damageMatchesProtection(source, protection.getProtectionType()));
        if (typedProtection != null) {
            activateSpecificProtection(event, player, typedProtection,
                    ((ProtectionPoppetItem) typedProtection.stack().getItem()).getProtectionType());
            return;
        }

        if (healthAfterDamage <= 0.0F) {
            PoppetLocation deathProtection = findFirstBoundPoppet(player,
                    stack -> stack.is(WitcheryItems.DEATH_PROTECTION_POPPET.get()));
            if (deathProtection != null) {
                activateDeathProtection(event, player, deathProtection, source);
                return;
            }
        }

        if (healthAfterDamage <= 2.0F && source.is(DamageTypes.STARVE)) {
            PoppetLocation hungerRescue = findFirstBoundPoppet(player,
                    stack -> stack.getItem() instanceof ProtectionPoppetItem protection
                            && protection.getProtectionType() == ProtectionPoppetItem.ProtectionType.HUNGER);
            if (hungerRescue != null) {
                activateSpecificProtection(event, player, hungerRescue, ProtectionPoppetItem.ProtectionType.HUNGER);
            }
        }
    }

    private static boolean damageMatchesProtection(DamageSource source, ProtectionPoppetItem.ProtectionType type) {
        return switch (type) {
            case EARTH -> source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL);
            case WATER -> source.is(DamageTypes.DROWN);
            case FIRE -> source.is(DamageTypeTags.IS_FIRE);
            case HUNGER -> source.is(DamageTypes.STARVE);
            case VOODOO -> source.is(DamageTypes.MAGIC) || source.is(DamageTypeTags.BYPASSES_ARMOR);
            case POPPET_SHIELD -> false;
        };
    }

    private static void activateSpecificProtection(LivingDamageEvent.Pre event, ServerPlayer player,
                                                   PoppetLocation location,
                                                   ProtectionPoppetItem.ProtectionType type) {
        String itemId = location.stack().getItem().getDescriptionId();
        consumeOne(location);
        event.setNewDamage(0.0F);

        switch (type) {
            case FIRE -> player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0));
            case WATER -> player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0));
            case HUNGER -> player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 60, 0));
            default -> {
            }
        }

        playProtectionEffects(player, itemId);
    }

    private static void activateDeathProtection(LivingDamageEvent.Pre event, ServerPlayer player,
                                                PoppetLocation location, DamageSource source) {
        String itemId = location.stack().getItem().getDescriptionId();
        consumeOne(location);
        event.setNewDamage(0.0F);

        if (player.getHealth() < 10.0F) {
            player.setHealth(10.0F);
        }

        if (source.is(DamageTypeTags.IS_FIRE)) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0));
        } else if (source.is(DamageTypes.DROWN)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 120, 0));
        } else if (source.is(DamageTypes.STARVE)) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 120, 0));
        }

        playProtectionEffects(player, itemId);
    }

    private static void playProtectionEffects(ServerPlayer player, String itemId) {
        player.displayClientMessage(Component.translatable("witchery.poppet.activated")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    30, 0.5, 0.5, 0.5, 0.3);
        }

        WitcheryMod.LOGGER.info("Poppet ({}) saved {} from damage.",
                itemId, player.getName().getString());
    }

    @SubscribeEvent
    public static void onSympatheticPoppet(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewDamage() <= 0) return;

        PoppetLocation location = findFirstBoundPoppet(player,
                stack -> stack.is(WitcheryItems.POPPET.get()) && PoppetItem.isBound(stack));
        if (location == null) return;

        Optional<UUID> targetUUID = PoppetItem.getBoundUUID(location.stack());
        if (targetUUID.isEmpty()) return;

        LivingEntity target = PoppetItem.findLivingEntity(((ServerLevel) player.level()).getServer(), targetUUID.get())
                .orElse(null);
        if (target == null || target == player) return;

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 0.4F, 0.5F);

        target.hurt(player.damageSources().magic(), event.getNewDamage());
        if (target instanceof ServerPlayer targetPlayer) {
            targetPlayer.displayClientMessage(Component.translatable("witchery.poppet.sympathetic.target_hurt",
                    player.getName()), false);
        }

        int charges = getSympathyCharges(location.stack());
        if (charges <= 1) {
            consumeOne(location);
            player.displayClientMessage(Component.translatable("witchery.poppet.sympathetic.consumed",
                    PoppetItem.getBoundName(location.stack())), false);
        } else {
            setSympathyCharges(location.stack(), charges - 1);
            markDirty(location);
        }
    }

    private static int getSympathyCharges(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr("sympathy_charges", 5);
    }

    private static void setSympathyCharges(ItemStack stack, int charges) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt("sympathy_charges", charges));
    }

    /**
     * When a Witch Hunter damages a player, 30% chance to disrupt one bound protective poppet.
     * Replicates the original Nullifying Bolt counterplay.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWitchHunterPoppetDisrupt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getSource().getDirectEntity() instanceof WitchHunterEntity)) return;
        if (player.level().random.nextFloat() >= 0.3f) return;

        // Try to destroy one bound protection poppet from inventory first, then shelves
        UUID playerUUID = player.getUUID();
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof ProtectionPoppetItem
                    && PoppetItem.isBound(stack)
                    && PoppetItem.getBoundUUID(stack).map(playerUUID::equals).orElse(false)) {
                inv.setItem(i, ItemStack.EMPTY);
                player.displayClientMessage(Component.translatable("witchery.witch_hunter.poppet_disrupted")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
        }
        for (PoppetShelfBlockEntity shelf : PoppetShelfBlockEntity.getActiveShelves(
                ((ServerLevel) player.level()).getServer())) {
            for (int i = 0; i < shelf.getContainerSize(); i++) {
                ItemStack stack = shelf.getItem(i);
                if (stack.getItem() instanceof ProtectionPoppetItem
                        && PoppetItem.isBound(stack)
                        && PoppetItem.getBoundUUID(stack).map(playerUUID::equals).orElse(false)) {
                    shelf.removeItem(i, 1);
                    shelf.setChanged();
                    player.displayClientMessage(Component.translatable("witchery.witch_hunter.poppet_disrupted")
                            .withStyle(ChatFormatting.RED), false);
                    return;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDeathTrack(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        data.setLastDeathPos(player.blockPosition(), player.level().dimension());
    }

    @SubscribeEvent
    public static void onToolCheckRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        checkToolPoppet(event.getEntity(), event.getItemStack());
    }

    @SubscribeEvent
    public static void onToolCheckLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        checkToolPoppet(event.getEntity(), event.getItemStack());
    }

    @SubscribeEvent
    public static void onToolCheckRightClickItem(PlayerInteractEvent.RightClickItem event) {
        checkToolPoppet(event.getEntity(), event.getItemStack());
    }

    private static void checkToolPoppet(net.minecraft.world.entity.player.Player player, ItemStack held) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!held.isDamageableItem()) return;
        if (held.getDamageValue() < held.getMaxDamage() * 0.9) return;

        if (findAndConsumePoppet(serverPlayer, WitcheryItems.TOOL_PROTECTION_POPPET.get())) {
            held.setDamageValue(0);
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.5F, 0.4F / (serverPlayer.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }

    @SubscribeEvent
    public static void onLivingDamageArmorCheck(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewDamage() <= 0) return;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isDamageableItem()) continue;
            if (armor.getDamageValue() < armor.getMaxDamage() * 0.9) continue;

            if (findAndConsumePoppet(player, WitcheryItems.ARMOR_PROTECTION_POPPET.get())) {
                armor.setDamageValue(0);
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                        0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickVoodoo(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(WitcheryItems.VOODOO_POPPET.get())) {
                VoodooPoppetItem.tickUnderwaterEffect(player, stack);
            }
        }
    }

    private static PoppetLocation findFirstBoundPoppet(ServerPlayer player, Predicate<ItemStack> matcher) {
        UUID playerUUID = player.getUUID();
        Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && matchesBound(stack, playerUUID) && matcher.test(stack)) {
                return new PoppetLocation(stack, inventory, null, i);
            }
        }

        for (PoppetShelfBlockEntity shelf : PoppetShelfBlockEntity.getActiveShelves(((ServerLevel) player.level()).getServer())) {
            for (int i = 0; i < shelf.getContainerSize(); i++) {
                ItemStack stack = shelf.getItem(i);
                if (!stack.isEmpty() && matchesBound(stack, playerUUID) && matcher.test(stack)) {
                    return new PoppetLocation(stack, null, shelf, i);
                }
            }
        }

        return null;
    }

    private static boolean findAndConsumePoppet(ServerPlayer player, Item poppetItem) {
        PoppetLocation location = findFirstBoundPoppet(player, stack -> stack.is(poppetItem));
        if (location == null) return false;
        consumeOne(location);
        return true;
    }

    public static boolean damageBoundPoppet(ServerPlayer player, Item poppetItem, int amount) {
        PoppetLocation location = findFirstBoundPoppet(player, stack -> stack.is(poppetItem));
        if (location == null) return false;
        damagePoppet(location, amount);
        return true;
    }

    public static int destroyBoundPoppets(ServerPlayer player, Item poppetItem, int amount, int maxCount) {
        int destroyed = 0;
        while (destroyed < maxCount && damageBoundPoppet(player, poppetItem, amount)) {
            destroyed++;
        }
        return destroyed;
    }

    public static boolean hasBoundPoppet(ServerPlayer player, Item poppetItem) {
        return findFirstBoundPoppet(player, stack -> stack.is(poppetItem)) != null;
    }

    private static void consumeOne(PoppetLocation location) {
        if (location.inventory() != null) {
            location.stack().shrink(1);
            if (location.stack().isEmpty()) {
                location.inventory().setItem(location.slot(), ItemStack.EMPTY);
            }
        } else if (location.shelf() != null) {
            location.shelf().removeItem(location.slot(), 1);
        }
        markDirty(location);
    }

    private static void damagePoppet(PoppetLocation location, int amount) {
        int damage = location.stack().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr(TAG_POPPET_DAMAGE, 0);
        damage = Math.min(damage + amount, POPPET_MAX_DAMAGE);

        if (damage >= POPPET_MAX_DAMAGE) {
            consumeOne(location);
            return;
        }

        final int newDamage = damage;
        CustomData.update(DataComponents.CUSTOM_DATA, location.stack(),
                tag -> tag.putInt(TAG_POPPET_DAMAGE, newDamage));
        markDirty(location);
    }

    private static void markDirty(PoppetLocation location) {
        if (location.inventory() != null) {
            location.inventory().setChanged();
        }
        if (location.shelf() != null) {
            location.shelf().setChanged();
        }
    }
    private static boolean matchesBound(ItemStack stack, UUID playerUUID) {
        return PoppetItem.isBound(stack)
                && PoppetItem.getBoundUUID(stack).map(playerUUID::equals).orElse(false);
    }
}
