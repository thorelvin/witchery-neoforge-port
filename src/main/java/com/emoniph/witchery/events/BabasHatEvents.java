package com.emoniph.witchery.events;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

@EventBusSubscriber
@SuppressWarnings({"deprecation", "removal"})
public final class BabasHatEvents {
    private static final int TELEPORT_COST = 5;
    private static final double TELEPORT_CHANCE = 0.25D;
    private static final int TELEPORT_DISTANCE = 6;
    private static final int TELEPORT_ATTEMPTS = 16;
    private static final int VERTICAL_SEARCH = 16;

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

        ItemStack hat = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!hat.is(WitcheryItems.BABAS_HAT.get()) || !isEligibleDamage(event.getSource())) {
            return;
        }
        if (player.getRandom().nextDouble() >= TELEPORT_CHANCE) {
            return;
        }

        BlockPos target = findRandomSafePosition(player);
        if (target == null || !spendInfusionEnergy(player)) {
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        sendTeleportFeedback(level, player.getX(), player.getY(), player.getZ());
        player.teleportTo(level, target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D,
                Set.of(), player.getYRot(), player.getXRot(), false);
        sendTeleportFeedback(level, player.getX(), player.getY(), player.getZ());
        event.setNewDamage(0.0F);
    }

    private static boolean isEligibleDamage(DamageSource source) {
        return source.getEntity() != null
                || source.getDirectEntity() != null
                || source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypeTags.BYPASSES_ARMOR);
    }

    private static boolean spendInfusionEnergy(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        int current = data.getInfusionEnergy();
        if (current < TELEPORT_COST) {
            return false;
        }

        data.setInfusionEnergy(current - TELEPORT_COST, data.getInfusionMaxEnergy());
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        return true;
    }

    private static BlockPos findRandomSafePosition(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        int spread = TELEPORT_DISTANCE * 2 + 1;

        for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
            int x = origin.getX() + player.getRandom().nextInt(spread) - TELEPORT_DISTANCE;
            int z = origin.getZ() + player.getRandom().nextInt(spread) - TELEPORT_DISTANCE;
            BlockPos column = new BlockPos(x, origin.getY(), z);
            if (!level.getWorldBorder().isWithinBounds(column)) {
                continue;
            }

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= level.getMinY() + 2) {
                y = Math.max(origin.getY(), level.getMinY() + 2);
            }
            y = Math.clamp(y, level.getMinY() + 2, level.getMaxY() - 2);

            for (int dy = 0; dy <= VERTICAL_SEARCH; dy++) {
                int up = y + dy;
                if (up < level.getMaxY() - 1 && isSafeLandingAt(level, x, up, z)) {
                    return new BlockPos(x, up, z);
                }

                int down = y - dy;
                if (down > level.getMinY() + 1 && isSafeLandingAt(level, x, down, z)) {
                    return new BlockPos(x, down, z);
                }
            }
        }

        return null;
    }

    private static boolean isSafeLandingAt(ServerLevel level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos head = feet.above();
        BlockPos groundPos = feet.below();
        BlockState ground = level.getBlockState(groundPos);
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);
        return !ground.isAir() && !ground.liquid() && ground.isSolid()
                && isOpenForPlayer(feetState) && isOpenForPlayer(headState);
    }

    private static boolean isOpenForPlayer(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && !state.liquid());
    }

    private static void sendTeleportFeedback(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0D, z,
                24, 0.35D, 0.55D, 0.35D, 0.1D);
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.75F, 1.1F);
    }
}
