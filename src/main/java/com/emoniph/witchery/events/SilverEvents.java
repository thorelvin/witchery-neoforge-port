package com.emoniph.witchery.events;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.BoltEntity;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntityTags;
import com.emoniph.witchery.init.WitcheryItemTags;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.WerewolfShiftUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Centralized silver damage system.
 *
 * Silver weapon detection covers:
 *   - Melee: attacker's mainhand is in the witchery:silver_weapons item tag
 *   - Projectile: direct entity is a BoltEntity with silver type
 *
 * Effects:
 *   - Vampires and werewolves: +5 bonus damage from silver (source: GenericEvents 1.7.10)
 *   - WolfmanEntity: inherits 1.5× silver multiplier in its own hurtServer
 *   - Silvered Hunter armor: when a werewolf attacks the wearer, deal 1.0 damage back
 */
@EventBusSubscriber
public class SilverEvents {
    private static final float GARLICKED_VAMPIRE_DAMAGE_SCALE = 0.6F;
    private static final float SILVERED_WEREWOLF_DAMAGE_SCALE = 0.6F;
    private static final float HUNTER_MAGIC_PROTECTION_CHANCE = 0.25F;
    private static final float HUNTER_CURSE_PROTECTION_CHANCE = 0.9F;
    private static final ThreadLocal<Boolean> BREW_SILVER_DAMAGE = ThreadLocal.withInitial(() -> false);

    // ── Silver damage bonus (vampires + werewolves) ───────────────────────────

    @SubscribeEvent
    public static void onSilverDamage(LivingDamageEvent.Pre event) {
        if (!isSilverSource(event.getSource())) return;
        if (!isVampireOrWerewolf(event.getEntity())) return;
        if (BREW_SILVER_DAMAGE.get() && event.getEntity() instanceof WolfmanEntity) return;
        if (BREW_SILVER_DAMAGE.get()
                && event.getSource().getDirectEntity() instanceof LivingEntity direct
                && isBrewSourceWerewolf(direct)) return;
        // +5 silver bonus (source parity: GenericEvents adds +5 for werewolf players)
        event.setNewDamage(event.getNewDamage() + 5.0f);
    }

    // ── Silvered armor back-damage (werewolf hits wearer) ─────────────────────

    @SubscribeEvent
    public static void onSilveredArmorHit(LivingIncomingDamageEvent event) {
        LivingEntity target  = event.getEntity();
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
        if (attacker == null) return;
        if (!isWerewolf(attacker)) return;
        if (!wearsSilveredArmor(target)) return;
        if (!(attacker.level() instanceof ServerLevel sl)) return;
        // Deal 1.0 damage back to the werewolf attacker (source: ItemHunterClothes.getProperties)
        attacker.hurtServer(sl, attacker.damageSources().magic(), 1.0f);
    }

    @SubscribeEvent
    public static void onGarlickedHunterArmorHit(LivingDamageEvent.Pre event) {
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
        if (attacker == null) return;
        if (!isVampire(attacker)) return;
        if (!wearsGarlickedArmor(event.getEntity())) return;

        event.setNewDamage(event.getNewDamage() * GARLICKED_VAMPIRE_DAMAGE_SCALE);
    }

    @SubscribeEvent
    public static void onSilveredHunterArmorHit(LivingDamageEvent.Pre event) {
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
        if (attacker == null) return;
        if (!isWerewolf(attacker)) return;
        if (!wearsSilveredArmor(event.getEntity())) return;

        event.setNewDamage(event.getNewDamage() * SILVERED_WEREWOLF_DAMAGE_SCALE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHunterMagicProtection(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0F) return;
        if (!isMagicSource(event.getSource())) return;
        if (!wearsFullHunterSet(event.getEntity())) return;
        if (event.getEntity().level().random.nextFloat() >= HUNTER_MAGIC_PROTECTION_CHANCE) return;

        event.setNewDamage(0.0F);
    }

    @SubscribeEvent
    public static void onSilveredHunterWerewolfWearer(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 2) return;
        if (player.getAbilities().instabuild) return;
        if (!WitcheryAttachments.get(player).isWerewolf()) return;
        if (!wearsSilveredArmor(player)) return;

        player.hurt(player.damageSources().cactus(), 1.0F);
    }

    @SubscribeEvent
    public static void onGarlickedHunterVampireWearer(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 2) return;
        if (player.getAbilities().instabuild) return;
        if (!WitcheryAttachments.get(player).isVampire()) return;
        if (!wearsGarlickedArmor(player)) return;

        player.hurt(player.damageSources().cactus(), 1.0F);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * True if the damage source is silver: attacker holds silver weapon (tag)
     * OR the direct projectile is a silver BoltEntity.
     */
    public static boolean isSilverSource(net.minecraft.world.damagesource.DamageSource source) {
        if (BREW_SILVER_DAMAGE.get() && isMagicSource(source)) return true;
        // Silver bolt projectile
        if (source.getDirectEntity() instanceof BoltEntity bolt && bolt.isSilver()) return true;
        // Silver weapon held by attacker
        if (source.getEntity() instanceof LivingEntity attacker) {
            ItemStack held = attacker.getMainHandItem();
            return held.is(WitcheryItemTags.SILVER_WEAPONS)
                    || held.is(WitcheryItems.SILVER_INGOT.get()); // bare ingot still counts
        }
        return false;
    }

    public static boolean hurtWithBrewSilver(ServerLevel level, LivingEntity target, DamageSource source, float amount) {
        boolean previous = BREW_SILVER_DAMAGE.get();
        BREW_SILVER_DAMAGE.set(true);
        try {
            return target.hurtServer(level, source, amount);
        } finally {
            BREW_SILVER_DAMAGE.set(previous);
        }
    }

    public static boolean hasHunterCurseProtection(LivingEntity entity) {
        return wearsFullHunterSet(entity)
                && entity.level().random.nextFloat() < HUNTER_CURSE_PROTECTION_CHANCE;
    }

    private static boolean isMagicSource(DamageSource source) {
        return source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    private static boolean isVampireOrWerewolf(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            WitcheryPlayerData data = WitcheryAttachments.get(sp);
            return data.isVampire() || data.isWerewolf();
        }
        return entity instanceof WolfmanEntity
                || entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }

    private static boolean isVampire(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            return WitcheryAttachments.get(sp).isVampire();
        }
        return entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }

    private static boolean isWerewolf(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            return WitcheryAttachments.get(sp).isWerewolf();
        }
        return entity instanceof WolfmanEntity;
    }

    private static boolean isBrewSourceWerewolf(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            WitcheryPlayerData data = WitcheryAttachments.get(sp);
            return data.isWerewolf() && WerewolfShiftUtil.isTransformed(data);
        }
        return entity instanceof WolfmanEntity;
    }

    private static boolean wearsSilveredArmor(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(WitcheryItems.SILVERED_HUNTER_HAT.get())
                || entity.getItemBySlot(EquipmentSlot.HEAD).is(WitcheryItems.DAWN_HUNTER_HAT.get())
                || entity.getItemBySlot(EquipmentSlot.CHEST).is(WitcheryItems.SILVERED_HUNTER_COAT.get())
                || entity.getItemBySlot(EquipmentSlot.CHEST).is(WitcheryItems.DAWN_HUNTER_COAT.get())
                || entity.getItemBySlot(EquipmentSlot.LEGS).is(WitcheryItems.SILVERED_HUNTER_LEGGINGS.get())
                || entity.getItemBySlot(EquipmentSlot.LEGS).is(WitcheryItems.DAWN_HUNTER_LEGGINGS.get())
                || entity.getItemBySlot(EquipmentSlot.FEET).is(WitcheryItems.SILVERED_HUNTER_BOOTS.get())
                || entity.getItemBySlot(EquipmentSlot.FEET).is(WitcheryItems.DAWN_HUNTER_BOOTS.get());
    }

    private static boolean wearsGarlickedArmor(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(WitcheryItems.DAWN_HUNTER_HAT.get())
                || entity.getItemBySlot(EquipmentSlot.CHEST).is(WitcheryItems.DAWN_HUNTER_COAT.get())
                || entity.getItemBySlot(EquipmentSlot.LEGS).is(WitcheryItems.DAWN_HUNTER_LEGGINGS.get())
                || entity.getItemBySlot(EquipmentSlot.FEET).is(WitcheryItems.DAWN_HUNTER_BOOTS.get());
    }

    public static boolean wearsFullHunterSet(LivingEntity entity) {
        return isHunterHead(entity.getItemBySlot(EquipmentSlot.HEAD))
                && isHunterChest(entity.getItemBySlot(EquipmentSlot.CHEST))
                && isHunterLegs(entity.getItemBySlot(EquipmentSlot.LEGS))
                && isHunterFeet(entity.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean isHunterHead(ItemStack stack) {
        return stack.is(WitcheryItems.HUNTER_HAT.get())
                || stack.is(WitcheryItems.SILVERED_HUNTER_HAT.get())
                || stack.is(WitcheryItems.DAWN_HUNTER_HAT.get());
    }

    private static boolean isHunterChest(ItemStack stack) {
        return stack.is(WitcheryItems.HUNTER_COAT.get())
                || stack.is(WitcheryItems.SILVERED_HUNTER_COAT.get())
                || stack.is(WitcheryItems.DAWN_HUNTER_COAT.get());
    }

    private static boolean isHunterLegs(ItemStack stack) {
        return stack.is(WitcheryItems.HUNTER_LEGGINGS.get())
                || stack.is(WitcheryItems.SILVERED_HUNTER_LEGGINGS.get())
                || stack.is(WitcheryItems.DAWN_HUNTER_LEGGINGS.get());
    }

    private static boolean isHunterFeet(ItemStack stack) {
        return stack.is(WitcheryItems.HUNTER_BOOTS.get())
                || stack.is(WitcheryItems.SILVERED_HUNTER_BOOTS.get())
                || stack.is(WitcheryItems.DAWN_HUNTER_BOOTS.get());
    }
}
