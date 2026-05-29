package com.emoniph.witchery.events;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.BoltEntity;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntityTags;
import com.emoniph.witchery.init.WitcheryItemTags;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

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

    // ── Silver damage bonus (vampires + werewolves) ───────────────────────────

    @SubscribeEvent
    public static void onSilverDamage(LivingDamageEvent.Pre event) {
        if (!isSilverSource(event.getSource())) return;
        if (!isVampireOrWerewolf(event.getEntity())) return;
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * True if the damage source is silver: attacker holds silver weapon (tag)
     * OR the direct projectile is a silver BoltEntity.
     */
    public static boolean isSilverSource(net.minecraft.world.damagesource.DamageSource source) {
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

    private static boolean isVampireOrWerewolf(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            WitcheryPlayerData data = WitcheryAttachments.get(sp);
            return data.isVampire() || data.isWerewolf();
        }
        return entity instanceof WolfmanEntity
                || entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }

    private static boolean isWerewolf(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            return WitcheryAttachments.get(sp).isWerewolf();
        }
        return entity instanceof WolfmanEntity;
    }

    private static boolean wearsSilveredArmor(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(WitcheryItems.HUNTER_HELMET_SILVERED.get())
                || entity.getItemBySlot(EquipmentSlot.CHEST).is(WitcheryItems.HUNTER_CHESTPLATE_SILVERED.get())
                || entity.getItemBySlot(EquipmentSlot.LEGS).is(WitcheryItems.HUNTER_LEGGINGS_SILVERED.get())
                || entity.getItemBySlot(EquipmentSlot.FEET).is(WitcheryItems.HUNTER_BOOTS_SILVERED.get());
    }
}
