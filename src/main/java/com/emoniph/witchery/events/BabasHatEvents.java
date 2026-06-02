package com.emoniph.witchery.events;

import com.emoniph.witchery.blocks.VoidBrambleBlock;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.BoltEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.WerewolfShiftUtil;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
@SuppressWarnings({"deprecation", "removal"})
public final class BabasHatEvents {
    private static final int TELEPORT_COST = 5;
    private static final double TELEPORT_CHANCE = 0.25D;
    private static final int TELEPORT_DISTANCE = 6;

    private BabasHatEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getNewDamage() <= 0.0F || player.level().dimension().equals(WitcheryDimensions.TORMENT)) {
            return;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (WerewolfShiftUtil.isTransformed(data)) {
            return;
        }

        ItemStack hat = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!hat.is(WitcheryItems.BABAS_HAT.get()) || !isEligibleDamage(event.getSource())) {
            return;
        }
        if (player.getRandom().nextDouble() >= TELEPORT_CHANCE) {
            return;
        }

        if (!spendInfusionEnergy(player, data)) {
            return;
        }

        VoidBrambleBlock.teleportRandomly(player.level(), player.blockPosition(), player, TELEPORT_DISTANCE);
        event.setNewDamage(0.0F);
    }

    private static boolean isEligibleDamage(DamageSource source) {
        if (source.getDirectEntity() instanceof BoltEntity bolt && bolt.isPoweredDraining()) {
            return false;
        }
        return source.getEntity() != null
                || source.getDirectEntity() != null
                || source.is(DamageTypeTags.IS_PROJECTILE);
    }

    private static boolean spendInfusionEnergy(ServerPlayer player, WitcheryPlayerData data) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        int current = data.getInfusionEnergy();
        if (current < TELEPORT_COST) {
            return false;
        }

        data.setInfusionEnergy(current - TELEPORT_COST, data.getInfusionMaxEnergy());
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        return true;
    }
}
