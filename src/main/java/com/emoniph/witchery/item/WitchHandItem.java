package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.BarrierBlock;
import com.emoniph.witchery.data.CreaturePowerType;
import com.emoniph.witchery.entity.EarthRockEntity;
import com.emoniph.witchery.events.WitcheryPlayerEvents;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.emoniph.witchery.util.EarthItems;
import com.emoniph.witchery.util.Enslaved;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import com.emoniph.witchery.data.InfusionType;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcheryDimensions;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.emoniph.witchery.protection.WitcheryProtectionAction;
import com.emoniph.witchery.protection.WitcheryProtectionService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public class WitchHandItem extends Item {
    private static final int USE_DURATION = 400;
    private static final int OTHERWHERE_RECALL_TICKS = 60;
    private static final String RECALL_X = "WITCRecallX";
    private static final String RECALL_Y = "WITCRecallY";
    private static final String RECALL_Z = "WITCRecallZ";
    private static final String RECALL_DIM = "WITCRecallDim";
    private static final String COOLDOWN_UNTIL = "WITCHandCooldownUntil";
    private static final int OTHERWHERE_COOLDOWN_TICKS = 1500;

    public WitchHandItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            long remaining = cooldownRemaining(serverPlayer, stack);
            if (remaining > 0L) {
                serverPlayer.displayClientMessage(Component.translatable("item.witchery.witch_hand.cooldown",
                        Math.max(1L, (remaining + 19L) / 20L)).withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        int elapsed = getUseDuration(stack, entity) - remainingUseDuration;
        switch (data.getInfusionType()) {
            case LIGHT -> sustainLight(player, data, elapsed);
            case OVERWORLD -> sustainOverworld(player, data, elapsed);
            case OTHERWHERE -> otherwhereFeedback(player, elapsed);
            default -> {
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return true;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        int elapsed = getUseDuration(stack, entity) - timeCharged;
        boolean success = switch (data.getInfusionType()) {
            case LIGHT -> releaseLight(player, data, elapsed);
            case OVERWORLD -> releaseOverworld(player, data, elapsed);
            case OTHERWHERE -> releaseOtherwhere(stack, player, data, elapsed);
            case INFERNAL -> releaseInfernal(player, data);
            default -> false;
        };

        if (!success && data.getInfusionType() == InfusionType.NONE) {
            player.displayClientMessage(Component.translatable("item.witchery.witch_hand.no_infusion")
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.witch_hand.tip")
                .withStyle(ChatFormatting.GRAY));
        CompoundTag tag = getTag(stack);
        if (hasRecall(tag)) {
            tooltip.accept(Component.translatable("item.witchery.witch_hand.recall",
                            tag.getIntOr(RECALL_X, 0), tag.getIntOr(RECALL_Y, 0), tag.getIntOr(RECALL_Z, 0))
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    public static boolean isWitchHand(ItemStack stack) {
        return stack.is(WitcheryItems.WITCH_HAND.get()) || stack.is(WitcheryItems.WITCHES_HAND.get());
    }

    public static boolean leftClickEntity(ItemStack stack, ServerPlayer player, LivingEntity target) {
        if (!isWitchHand(stack)) {
            return false;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        return switch (data.getInfusionType()) {
            case LIGHT -> lightTarget(player, data, target);
            case OVERWORLD -> overworldPush(player, data, target);
            case OTHERWHERE -> player.isShiftKeyDown()
                    ? otherwhereRecallDrag(stack, player, data, target)
                    : otherwhereHike(player, data, target);
            case INFERNAL -> infernalCommandOrEnslave(player, data, target);
            default -> false;
        };
    }

    private static void sustainLight(ServerPlayer player, WitcheryPlayerData data, int elapsed) {
        if (elapsed <= 20 || elapsed % 30 != 0 || !spendInfusionEnergy(player, data, 1)) {
            return;
        }

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.INVISIBILITY, LIGHT_BEND_INVISIBILITY_TICKS, 0, true, false));
        AABB area = lightAggroDropArea(player.getX(), player.getY(), player.getZ());
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, mob -> mob.getTarget() == player)) {
            if (mob.distanceTo(player) <= 20.0F) {
                mob.setTarget(null);
            }
        }
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                8, 0.5D, 0.6D, 0.5D, 0.01D);
    }

    private static void stopSustainedLight(ServerPlayer player) {
        player.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
    }

    static AABB lightAggroDropArea(double x, double y, double z) {
        return new AABB(x - 20.0D, y, z - 20.0D, x + 20.0D, y + 2.0D, z + 20.0D);
    }

    /** Source InfusionOverworld channel (sneak): magnetise nearby metal items and smelt ores around. */
    private static void sustainOverworld(ServerPlayer player, WitcheryPlayerData data, int elapsed) {
        if (!player.isShiftKeyDown() || elapsed < 40 || elapsed % 4 != 0 || !spendInfusionEnergy(player, data, 1)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();

        // Magnetise loose metal items toward the witch.
        Vec3 pullTo = player.position().add(0.0D, player.getEyeHeight() * 0.5D, 0.0D);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6.0D))) {
            if (!EarthItems.isMetal(item.getItem())) {
                continue;
            }
            Vec3 delta = pullTo.subtract(item.position());
            if (delta.lengthSqr() > 0.04D) {
                item.setDeltaMovement(delta.normalize().scale(0.4D));
            }
        }

        // Smelt ores in a 13x7x13 box, spending 2 energy each until the pool runs dry.
        int energy = player.isCreative() ? Integer.MAX_VALUE : data.getInfusionEnergy();
        boolean smelted = false;
        BlockPos centre = player.blockPosition();
        for (BlockPos scan : BlockPos.betweenClosed(centre.offset(-6, -3, -6), centre.offset(6, 3, 6))) {
            if (energy < 2) {
                break;
            }
            Item ingot = EarthItems.oreToIngot(level.getBlockState(scan));
            if (ingot != null) {
                BlockPos pos = scan.immutable();
                if (!canModifyTerrain(player, level, pos, WitcheryProtectionAction.BLOCK_TRANSFORM)) {
                    continue;
                }
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                Block.popResource(level, pos, new ItemStack(ingot));
                energy -= 2;
                smelted = true;
            }
        }
        if (smelted && !player.isCreative()) {
            data.setInfusionEnergy(energy, data.getInfusionMaxEnergy());
            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.25F, 1.5F);
    }

    private static void otherwhereFeedback(ServerPlayer player, int elapsed) {
        if (player.isShiftKeyDown() && elapsed == OTHERWHERE_RECALL_TICKS) {
            player.displayClientMessage(Component.translatable("item.witchery.witch_hand.can_set_recall")
                    .withStyle(ChatFormatting.GRAY), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
                    SoundSource.PLAYERS, 0.5F, 1.2F);
        } else if (!player.isShiftKeyDown() && elapsed > 0 && elapsed % 20 == 0) {
            int range = Math.min(120, 40 + 20 * Math.max(0, elapsed / 20));
            HitResult hit = player.pick(range, 1.0F, true);
            boolean canTeleport = hit instanceof BlockHitResult blockHit
                    && findSafeLanding((ServerLevel) player.level(), blockHit) != null;
            player.displayClientMessage(Component.translatable(canTeleport
                    ? "item.witchery.witch_hand.canteleport"
                    : "item.witchery.witch_hand.cannotteleport").withStyle(canTeleport
                    ? ChatFormatting.GRAY : ChatFormatting.RED), true);
            player.level().playSound(null, player.blockPosition(),
                    canTeleport ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.NOTE_BLOCK_SNARE.value(),
                    SoundSource.PLAYERS, 0.25F, canTeleport ? 1.0F : 0.7F);
        }
    }

    private static boolean releaseOtherwhere(ItemStack stack, ServerPlayer player, WitcheryPlayerData data, int elapsed) {
        if (player.isShiftKeyDown() && elapsed >= OTHERWHERE_RECALL_TICKS) {
            storeRecall(stack, player);
            feedback(player, ParticleTypes.PORTAL, SoundEvents.FIRE_EXTINGUISH);
            return true;
        }
        if (player.isShiftKeyDown()) {
            boolean recalled = recall(stack, player, data);
            if (recalled) {
                setCooldown(stack, player, OTHERWHERE_COOLDOWN_TICKS);
            }
            return recalled;
        }
        boolean teleported = teleportToLook(player, data, Math.min(120.0D, 40.0D + 20.0D * Math.max(0, elapsed / 20)));
        if (teleported) {
            setCooldown(stack, player, OTHERWHERE_COOLDOWN_TICKS);
        }
        return teleported;
    }

    /** Source InfusionOverworld block-top release: raise a pillar of earth, lifting whatever stands on it. */
    private static boolean releaseOverworld(ServerPlayer player, WitcheryPlayerData data, int elapsed) {
        ServerLevel level = (ServerLevel) player.level();

        // Source onPlayerStoppedUsing ENTITY branch: disarm a struck metal-wielding mob.
        EntityHitResult entityHit = pickEntity(player, 4.0D);
        if (entityHit != null && entityHit.getEntity() instanceof Mob mob && !player.isShiftKeyDown()) {
            return overworldDisarm(player, data, mob);
        }

        HitResult hit = player.pick(4.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS) {
            return overworldShockwave(player, data, elapsed);
        }
        BlockPos pos = blockHit.getBlockPos();

        // Sneak on an ore: smelt it to stone and drop two ingots.
        if (player.isShiftKeyDown()) {
            Item ingot = EarthItems.oreToIngot(level.getBlockState(pos));
            if (ingot == null
                    || !canModifyTerrain(player, level, pos, WitcheryProtectionAction.BLOCK_TRANSFORM)
                    || !spendInfusionEnergy(player, data, 2)) {
                fail(player);
                return true;
            }
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
            Block.popResource(level, pos, new ItemStack(ingot, 2));
            feedback(player, ParticleTypes.POOF, SoundEvents.IRON_GOLEM_REPAIR);
            return true;
        }

        Direction face = blockHit.getDirection();

        // Aim at the side of a loose earthen block with open air in front: hurl it as a rock.
        if (face != Direction.UP && face != Direction.DOWN
                && EarthItems.isThrowableRock(level.getBlockState(pos))
                && level.getBlockState(pos.relative(face)).isAir()) {
            if (!canModifyTerrain(player, level, pos, WitcheryProtectionAction.BLOCK_BREAK)
                    || !spendInfusionEnergy(player, data, 3)) {
                fail(player);
                return true;
            }
            Vec3 centre = Vec3.atCenterOf(pos);
            level.removeBlock(pos, false);
            level.addFreshEntity(EarthRockEntity.hurl(level, player, centre.x, centre.y, centre.z, player.getLookAngle()));
            level.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 1, 0.0, 0.0, 0.0, 0.0);
            feedback(player, ParticleTypes.POOF, SoundEvents.IRON_GOLEM_ATTACK);
            return true;
        }

        // Aim at a block top: raise an earth pillar.
        if (face == Direction.UP) {
            if (level.getBlockState(pos.below(10)).isAir()) {
                fail(player); // needs solid ground to push up from
                return true;
            }
            if (!spendInfusionEnergy(player, data, 2)) {
                fail(player);
                return true;
            }
            raiseColumn(player, level, pos, 6, 3);
            feedback(player, ParticleTypes.POOF, SoundEvents.IRON_GOLEM_ATTACK);
            return true;
        }
        return overworldShockwave(player, data, elapsed);
    }

    /** Source InfusionOverworld fallback: a long open-ground release sends out a damaging shockwave. */
    private static boolean overworldShockwave(ServerPlayer player, WitcheryPlayerData data, int elapsed) {
        if (player.isShiftKeyDown() || elapsed < 40) {
            return false;
        }
        int seconds = elapsed / 20;
        if (!spendInfusionEnergy(player, data, 6 * seconds)) {
            fail(player);
            return false;
        }
        com.emoniph.witchery.events.EarthShockwaveEvents.start((ServerLevel) player.level(), player, 2 * seconds);
        feedback(player, ParticleTypes.EXPLOSION, SoundEvents.IRON_GOLEM_ATTACK);
        return true;
    }

    private static boolean overworldDisarm(ServerPlayer player, WitcheryPlayerData data, Mob mob) {
        ItemStack held = mob.getMainHandItem();
        if (!EarthItems.isMetal(held)) {
            return false;
        }
        if (!spendInfusionEnergy(player, data, 2)) {
            fail(player);
            return true;
        }
        ServerLevel level = (ServerLevel) player.level();
        mob.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ItemEntity drop = new ItemEntity(level, mob.getX(), mob.getY() + 0.5D, mob.getZ(), held);
        drop.setDeltaMovement(player.getLookAngle().scale(-0.2D));
        level.addFreshEntity(drop);
        feedback(player, ParticleTypes.CRIT, SoundEvents.ITEM_PICKUP);
        return true;
    }

    private static EntityHitResult pickEntity(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0F).scale(range);
        return ProjectileUtil.getEntityHitResult(player, eye, eye.add(reach),
                player.getBoundingBox().expandTowards(reach).inflate(1.0D),
                e -> e instanceof LivingEntity && e != player && e.isPickable(), range * range);
    }

    private static void raiseColumn(ServerPlayer player, ServerLevel level, BlockPos top, int depth, int rise) {
        for (int h = 0; h < depth; h++) {
            BlockPos from = top.below(h);
            BlockState moving = level.getBlockState(from);
            if (moving.isAir() || moving.getDestroySpeed(level, from) < 0.0F || moving.hasBlockEntity()) {
                continue;
            }
            BlockPos to = from.above(rise);
            BlockState dest = level.getBlockState(to);
            if (!dest.isAir() && !dest.canBeReplaced()) {
                continue;
            }
            if (!canModifyTerrain(player, level, from, WitcheryProtectionAction.BLOCK_TRANSFORM)
                    || !canModifyTerrain(player, level, to, WitcheryProtectionAction.BLOCK_TRANSFORM)) {
                continue;
            }
            level.removeBlock(from, false);
            level.setBlock(to, moving, 3);
            AABB column = new AABB(from).expandTowards(0.0D, 1.0D, 0.0D);
            for (LivingEntity rider : level.getEntitiesOfClass(LivingEntity.class, column)) {
                rider.fallDistance = 0.0F;
                rider.teleportTo(rider.getX(), rider.getY() + rise, rider.getZ());
            }
        }
    }

    private static boolean canModifyTerrain(ServerPlayer player, ServerLevel level, BlockPos pos,
                                            WitcheryProtectionAction action) {
        return WitcheryProtectionService.canModifyBlock(player, level, pos, action);
    }

    private static boolean releaseInfernal(ServerPlayer player, WitcheryPlayerData data) {
        if (player.isShiftKeyDown()) {
            HitResult hit = pickEntityOrBlock(player, 15.0D);
            if (hit instanceof EntityHitResult
                    || !(hit instanceof BlockHitResult blockHit)
                    || blockHit.getType() == HitResult.Type.MISS) {
                fail(player);
                return true;
            }
            BlockPos destination = infernalCommandDestination(blockHit);
            if (destination == null) {
                return true;
            }
            int minions = commandEnslavedMinions(player, destination);
            if (minions > 0) {
                feedback(player, ParticleTypes.WITCH, SoundEvents.CHORUS_FRUIT_TELEPORT);
            }
            return true;
        }

        CreaturePowerType power = CreaturePowerType.fromString(data.getCreaturePowerType());
        if (power == null || data.getCreaturePowerCharges() <= 0 || !spendInfusionEnergy(player, data, 1)) {
            fail(player);
            return false;
        }
        if (!data.spendCreaturePowerCharge()) {
            fail(player);
            return false;
        }
        power.activate(player);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        return true;
    }

    static BlockPos infernalCommandDestination(BlockHitResult hit) {
        if (hit == null || hit.getType() == HitResult.Type.MISS || hit.getDirection() != Direction.UP) {
            return null;
        }
        return hit.getBlockPos().above();
    }

    private static int commandEnslavedMinions(ServerPlayer player, BlockPos destination) {
        int count = 0;
        AABB area = player.getBoundingBox().inflate(50.0D, 15.0D, 50.0D);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, mob -> Enslaved.isEnslavedBy(mob, player))) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, 1.0D);
            count++;
        }
        return count;
    }

    // ── Light: barrier powers (source InfusionLight) ───────────────────────────
    private static final int BARRIER_TICKS = 200; // source BlockBarrier 200-tick life
    static final int LIGHT_BEND_INVISIBILITY_TICKS = 30;

    /** Source onLeftClickEntity: hoist the target up and cage it in light-barrier. */
    private static boolean lightTarget(ServerPlayer player, WitcheryPlayerData data, LivingEntity target) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos dest = target.blockPosition().above(4);
        if (!hasLightHoistSpace(level, dest)) {
            fail(player);
            return true;
        }
        if (!spendInfusionEnergy(player, data, 5)) {
            fail(player);
            return true;
        }
        target.fallDistance = 0.0F;
        target.teleportTo(dest.getX() + 0.5D, dest.getY(), dest.getZ() + 0.5D);
        buildLightHoistCage(level, dest);
        lightFeedback(level, dest);
        return true;
    }

    /** Source onPlayerStoppedUsing (sneaking, short channel): cage, shield, or sprout a light barrier. */
    private static boolean releaseLight(ServerPlayer player, WitcheryPlayerData data, int elapsed) {
        stopSustainedLight(player);
        if (!player.isShiftKeyDown() || elapsed >= 20) {
            return false; // the long channel is the light-bend (sustainLight); only a quick sneak-release shields
        }
        HitResult hit = pickEntityOrBlock(player, 16.0D);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            if (!spendInfusionEnergy(player, data, 3)) {
                fail(player);
                return true;
            }
            ServerLevel level = (ServerLevel) player.level();
            BlockPos center = target.blockPosition();
            buildLightReleaseEntityCage(level, center);
            lightFeedback(level, center);
            return true;
        }
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS) {
            return false;
        }
        if (!spendInfusionEnergy(player, data, 3)) {
            fail(player);
            return true;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (blockHit.getDirection() == Direction.UP) {
            BlockPos base = lightShieldBase(level, blockHit);
            buildBarrierShield(level, base, player);
            lightFeedback(level, base);
        } else {
            BlockPos last = buildBarrierLine(level, blockHit);
            lightFeedback(level, last == null ? blockHit.getBlockPos() : last);
        }
        return true;
    }

    private static HitResult pickEntityOrBlock(ServerPlayer player, double range) {
        return pickEntityOrBlock(player, range, false);
    }

    private static HitResult pickEntityOrBlock(ServerPlayer player, double range, boolean includeFluids) {
        HitResult blockHit = player.pick(range, 1.0F, includeFluids);
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0F).scale(range);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS
                ? range * range
                : blockHit.getLocation().distanceToSqr(eye);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(reach),
                player.getBoundingBox().expandTowards(reach).inflate(1.0D),
                e -> e instanceof LivingEntity && e != player && e.isPickable(), maxDistance);
        return entityHit == null ? blockHit : entityHit;
    }

    private static boolean placeBarrier(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (canPlaceLightBarrier(state)) {
            BarrierBlock.setBarrier(level, pos, BARRIER_TICKS, true);
            return level.getBlockState(pos).is(WitcheryBlocks.BARRIER.get());
        }
        return false;
    }

    private static boolean canPlaceLightBarrier(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || !state.getFluidState().isEmpty()
                || state.is(WitcheryBlocks.BARRIER.get());
    }

    private static boolean hasLightHoistSpace(ServerLevel level, BlockPos center) {
        for (BlockPos pos : lightHoistClearancePositions(center)) {
            if (!level.getBlockState(pos).isAir()) {
                return false;
            }
        }
        return true;
    }

    // Source clearance before hoisting: center plus four cardinal spaces for three body layers.
    static Set<BlockPos> lightHoistClearancePositions(BlockPos center) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        for (int y = 0; y < 3; y++) {
            BlockPos layer = center.above(y);
            positions.add(layer);
            positions.add(layer.east());
            positions.add(layer.south());
            positions.add(layer.west());
            positions.add(layer.north());
        }
        return positions;
    }

    private static void buildLightHoistCage(ServerLevel level, BlockPos center) {
        for (BlockPos pos : lightHoistCagePositions(center)) {
            placeBarrier(level, pos);
        }
    }

    static Set<BlockPos> lightHoistCagePositions(BlockPos center) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        addFilledCircle(positions, center.getX(), center.getZ(), center.getY() - 1, 2);
        for (int y = center.getY(); y < center.getY() + 3; y++) {
            addCircle(positions, center.getX(), center.getZ(), y, 2);
        }
        addFilledCircle(positions, center.getX(), center.getZ(), center.getY() + 3, 2);
        return positions;
    }

    private static void buildLightReleaseEntityCage(ServerLevel level, BlockPos center) {
        for (BlockPos pos : lightReleaseEntityCagePositions(center)) {
            placeBarrier(level, pos);
        }
    }

    static Set<BlockPos> lightReleaseEntityCagePositions(BlockPos center) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        addFilledCircle(positions, center.getX(), center.getZ(), center.getY() - 1, 1);
        for (int y = center.getY(); y < center.getY() + 3; y++) {
            addCircle(positions, center.getX(), center.getZ(), y, 2);
        }
        addFilledCircle(positions, center.getX(), center.getZ(), center.getY() + 3, 2);
        return positions;
    }

    private static void addCircle(Set<BlockPos> positions, int x0, int z0, int y, int radius) {
        int x = radius;
        int z = 0;
        int radiusError = 1 - radius;
        while (x >= z) {
            addPixel(positions, x0 + x, y, z0 + z);
            addPixel(positions, x0 + z, y, z0 + x);
            addPixel(positions, x0 - x, y, z0 + z);
            addPixel(positions, x0 - z, y, z0 + x);
            addPixel(positions, x0 - x, y, z0 - z);
            addPixel(positions, x0 - z, y, z0 - x);
            addPixel(positions, x0 + x, y, z0 - z);
            addPixel(positions, x0 + z, y, z0 - x);
            z++;
            if (radiusError < 0) {
                radiusError += 2 * z + 1;
            } else {
                x--;
                radiusError += 2 * (z - x + 1);
            }
        }
    }

    private static void addFilledCircle(Set<BlockPos> positions, int x0, int z0, int y, int radius) {
        int x = radius;
        int z = 0;
        int radiusError = 1 - radius;
        while (x >= z) {
            addLine(positions, x0 - x, x0 + x, y, z0 + z);
            addLine(positions, x0 - z, x0 + z, y, z0 + x);
            addLine(positions, x0 - x, x0 + x, y, z0 - z);
            addLine(positions, x0 - z, x0 + z, y, z0 - x);
            z++;
            if (radiusError < 0) {
                radiusError += 2 * z + 1;
            } else {
                x--;
                radiusError += 2 * (z - x + 1);
            }
        }
    }

    private static void addLine(Set<BlockPos> positions, int x1, int x2, int y, int z) {
        for (int x = x1; x <= x2; x++) {
            addPixel(positions, x, y, z);
        }
    }

    private static void addPixel(Set<BlockPos> positions, int x, int y, int z) {
        positions.add(new BlockPos(x, y, z));
    }

    /** A 3-wide, 3-tall barrier wall across the player's facing. */
    private static void buildBarrierShield(ServerLevel level, BlockPos base, ServerPlayer player) {
        Direction across = player.getDirection().getClockWise();
        for (int w = -1; w <= 1; w++) {
            BlockPos column = base.relative(across, w);
            for (int y = 0; y < 3; y++) {
                placeBarrier(level, column.above(y));
            }
        }
    }

    private static BlockPos lightShieldBase(ServerLevel level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() ? pos : pos.above();
    }

    private static BlockPos buildBarrierLine(ServerLevel level, BlockHitResult hit) {
        BlockPos last = null;
        for (BlockPos pos : lightBarrierLinePositions(hit.getBlockPos(), hit.getDirection())) {
            if (!placeBarrier(level, pos)) {
                break;
            }
            last = pos;
        }
        return last;
    }

    static Set<BlockPos> lightBarrierLinePositions(BlockPos hitPos, Direction face) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        if (face == null || face == Direction.UP) {
            return positions;
        }
        for (int i = 1; i < 16; i++) {
            positions.add(hitPos.relative(face, i));
        }
        return positions;
    }

    private static void lightFeedback(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, 1.3F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                24, 0.6D, 0.8D, 0.6D, 0.02D);
    }

    /** Source InfusionOverworld onLeftClickEntity: fling a metal-bearing target by the iron on them. */
    private static boolean overworldPush(ServerPlayer player, WitcheryPlayerData data, LivingEntity target) {
        if (!wearsOrHoldsMetal(target)) {
            fail(player);
            return true;
        }
        boolean high = player.isShiftKeyDown();
        if (!spendInfusionEnergy(player, data, high ? 4 : 2)) {
            fail(player);
            return true;
        }
        Vec3 look = player.getLookAngle();
        double horizontal = 0.8D * 3.0D;
        target.push(look.x * horizontal, high ? 1.5D : 0.3D, look.z * horizontal);
        target.hurtMarked = true;
        feedback(player, ParticleTypes.POOF, SoundEvents.IRON_GOLEM_ATTACK);
        return true;
    }

    private static boolean wearsOrHoldsMetal(LivingEntity entity) {
        return EarthItems.isMetal(entity.getMainHandItem())
                || EarthItems.isMetal(entity.getItemBySlot(EquipmentSlot.HEAD))
                || EarthItems.isMetal(entity.getItemBySlot(EquipmentSlot.CHEST))
                || EarthItems.isMetal(entity.getItemBySlot(EquipmentSlot.LEGS))
                || EarthItems.isMetal(entity.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean otherwhereHike(ServerPlayer player, WitcheryPlayerData data, LivingEntity target) {
        if (isEnderInhibited(player) || !spendInfusionEnergy(player, data, 2)) {
            fail(player);
            return true;
        }
        double selfLift = clearLiftDistance(player, 8);
        if (selfLift <= 0.0D) {
            fail(player);
            return true;
        }
        player.fallDistance = 0.0F;
        player.teleportTo(player.getX(), player.getY() + selfLift, player.getZ());
        // The target only rides along if it is not warded against the far country.
        if (!isEnderInhibited(target)) {
            double lift = clearLiftDistance(target, 8);
            if (lift > 0.0D) {
                target.fallDistance = 0.0F;
                target.teleportTo(target.getX(), target.getY() + lift, target.getZ());
            }
        }
        feedback(player, ParticleTypes.PORTAL, SoundEvents.ENDERMAN_TELEPORT);
        return true;
    }

    private static boolean infernalCommandOrEnslave(ServerPlayer player, WitcheryPlayerData data, LivingEntity target) {
        if (player.isShiftKeyDown()) {
            if (!(target instanceof Mob mob) || !Enslaved.canBeEnslaved(mob)) {
                fail(player);
                return true;
            }
            // Source InfusionInfernal.onLeftClickEntity: sneak on a mob already enslaved by you
            // sacrifices it for its creature power; otherwise it binds the mob.
            if (Enslaved.isEnslavedBy(mob, player)) {
                return sacrificeCreature(player, data, mob);
            }
            if (!spendInfusionEnergy(player, data, 5) || !Enslaved.enslave(mob, player)) {
                fail(player);
                return true;
            }
            mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 200, 0));
            feedback(player, ParticleTypes.WITCH, SoundEvents.ZOMBIE_INFECT);
            return true;
        }

        if (!spendInfusionEnergy(player, data, 1)) {
            fail(player);
            return true;
        }
        int count = 0;
        AABB area = player.getBoundingBox().inflate(50.0D, 15.0D, 50.0D);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, mob -> Enslaved.isEnslavedBy(mob, player))) {
            mob.setTarget(target);
            count++;
        }
        if (count > 0) {
            feedback(player, ParticleTypes.CRIT, SoundEvents.BREEZE_CHARGE);
        } else {
            fail(player);
        }
        return true;
    }

    /**
     * Source {@code InfusionInfernal.trySacrificeCreature}: slay one of your own thralls to take on
     * (or recharge) its creature power. Shares the port's altar-kill charge tables
     * ({@link WitcheryPlayerEvents}) so both acquisition paths grant identical charges.
     * <p>Divergence: the enslave that precedes this already cost 5 energy, so the sacrifice itself is
     * free (source consumes 1 charge, which is impossible to pay before you own any power).
     */
    private static boolean sacrificeCreature(ServerPlayer player, WitcheryPlayerData data, Mob mob) {
        CreaturePowerType power = WitcheryPlayerEvents.creaturePowerFor(mob);
        if (power == null) {
            fail(player);
            return true;
        }
        int charges = WitcheryPlayerEvents.creaturePowerChargesFor(mob, power);
        if (power.name().equals(data.getCreaturePowerType())) {
            data.addCreaturePowerCharges(charges);
        } else {
            data.setCreaturePower(power.name(), charges);
        }
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));

        ServerLevel level = (ServerLevel) player.level();
        feedback(player, ParticleTypes.SOUL, SoundEvents.SOUL_ESCAPE.value());
        level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY(0.6), mob.getZ(),
                24, 0.3, 0.5, 0.3, 0.02);
        // Mark it so the altar-kill acquisition does not also fire for this lethal blow.
        mob.getPersistentData().putBoolean(Enslaved.SACRIFICED_TAG, true);
        mob.hurtServer(level, level.damageSources().playerAttack(player), mob.getHealth() + 1.0F);
        player.displayClientMessage(Component.translatable("witchery.creature_power.absorbed",
                power.displayName(), data.getCreaturePowerCharges()).withStyle(ChatFormatting.DARK_PURPLE), false);
        return true;
    }

    private static boolean spendInfusionEnergy(ServerPlayer player, WitcheryPlayerData data, int amount) {
        if (player.isCreative()) {
            return true;
        }
        int current = data.getInfusionEnergy();
        if (current < amount) {
            return false;
        }
        data.setInfusionEnergy(current - amount, data.getInfusionMaxEnergy());
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
        return true;
    }

    private static void storeRecall(ItemStack stack, ServerPlayer player) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(RECALL_X, player.blockPosition().getX());
            tag.putInt(RECALL_Y, player.blockPosition().getY());
            tag.putInt(RECALL_Z, player.blockPosition().getZ());
            tag.putString(RECALL_DIM, player.level().dimension().identifier().toString());
        });
        player.displayClientMessage(Component.translatable("item.witchery.witch_hand.set_recall",
                        player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ())
                .withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    private static boolean recall(ItemStack stack, ServerPlayer player, WitcheryPlayerData data) {
        CompoundTag tag = getTag(stack);
        if (!hasRecall(tag) || isEnderInhibited(player)) {
            fail(player);
            return false;
        }
        try {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                    Identifier.parse(tag.getStringOr(RECALL_DIM, "")));
            if (isOtherwhereBlocked(player.level().dimension(), dimension)) {
                player.displayClientMessage(Component.translatable("item.witchery.witch_hand.dimension_blocked")
                        .withStyle(ChatFormatting.RED), true);
                fail(player);
                return false;
            }
            ServerLevel destination = ((ServerLevel) player.level()).getServer().getLevel(dimension);
            if (destination == null || !spendInfusionEnergy(player, data, 2)) {
                fail(player);
                return false;
            }
            player.fallDistance = 0.0F;
            player.teleportTo(destination, tag.getIntOr(RECALL_X, 0) + 0.5D, tag.getIntOr(RECALL_Y, 0),
                    tag.getIntOr(RECALL_Z, 0) + 0.5D, Set.of(), player.getYRot(), player.getXRot(), false);
            feedback(player, ParticleTypes.PORTAL, SoundEvents.ENDERMAN_TELEPORT);
            return true;
        } catch (IllegalArgumentException ex) {
            fail(player);
            return false;
        }
    }

    /** Source onLeftClickEntity (sneaking): drag self and the grasped creature to the stored recall point. */
    private static boolean otherwhereRecallDrag(ItemStack stack, ServerPlayer player, WitcheryPlayerData data, LivingEntity target) {
        CompoundTag tag = getTag(stack);
        if (!hasRecall(tag) || isEnderInhibited(player)) {
            fail(player);
            return true;
        }
        ResourceKey<Level> recallDimension = recallDimension(tag);
        if (recallDimension == null || isOtherwhereBlocked(player.level().dimension(), recallDimension)) {
            player.displayClientMessage(Component.translatable("item.witchery.witch_hand.dimension_blocked")
                    .withStyle(ChatFormatting.RED), true);
            fail(player);
            return true;
        }
        ServerLevel dest = resolveRecallLevel(player, tag);
        if (dest == null || !spendInfusionEnergy(player, data, 4)) {
            fail(player);
            return true;
        }
        double x = tag.getIntOr(RECALL_X, 0) + 0.5D;
        double y = tag.getIntOr(RECALL_Y, 0);
        double z = tag.getIntOr(RECALL_Z, 0) + 0.5D;
        player.fallDistance = 0.0F;
        player.teleportTo(dest, x, y, z, Set.of(), player.getYRot(), player.getXRot(), false);
        if (!isEnderInhibited(target)) {
            target.fallDistance = 0.0F;
            target.teleportTo(dest, x, y, z, Set.of(), target.getYRot(), target.getXRot(), false);
        }
        feedback(player, ParticleTypes.PORTAL, SoundEvents.ENDERMAN_TELEPORT);
        return true;
    }

    private static ServerLevel resolveRecallLevel(ServerPlayer player, CompoundTag tag) {
        try {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                    Identifier.parse(tag.getStringOr(RECALL_DIM, "")));
            return ((ServerLevel) player.level()).getServer().getLevel(dimension);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ResourceKey<Level> recallDimension(CompoundTag tag) {
        try {
            return ResourceKey.create(Registries.DIMENSION, Identifier.parse(tag.getStringOr(RECALL_DIM, "")));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isOtherwhereBlocked(ResourceKey<Level> source, ResourceKey<Level> target) {
        return isSpecialVeil(source) || isSpecialVeil(target);
    }

    private static boolean isSpecialVeil(ResourceKey<Level> dimension) {
        return dimension.equals(WitcheryDimensions.SPIRIT_WORLD)
                || dimension.equals(WitcheryDimensions.TORMENT)
                || dimension.equals(WitcheryDimensions.MIRROR_WORLD);
    }

    private static boolean isEnderInhibited(LivingEntity entity) {
        return entity.hasEffect(WitcheryMobEffects.ENDER_INHIBITION);
    }

    private static boolean teleportToLook(ServerPlayer player, WitcheryPlayerData data, double range) {
        if (isEnderInhibited(player) || !spendInfusionEnergy(player, data, 1)) {
            fail(player);
            return false;
        }
        HitResult hit = pickEntityOrBlock(player, range, true);
        Vec3 entityDestination = otherwhereEntityTeleportDestination(hit);
        if (entityDestination != null) {
            player.fallDistance = 0.0F;
            player.teleportTo(entityDestination.x, entityDestination.y, entityDestination.z);
            feedback(player, ParticleTypes.PORTAL, SoundEvents.ENDERMAN_TELEPORT);
            return true;
        }
        if (!(hit instanceof BlockHitResult blockHit)) {
            player.displayClientMessage(Component.translatable("item.witchery.witch_hand.cannotteleport")
                    .withStyle(ChatFormatting.RED), true);
            fail(player);
            return false;
        }
        BlockPos landing = findSafeLanding((ServerLevel) player.level(), blockHit);
        if (landing == null) {
            player.displayClientMessage(Component.translatable("item.witchery.witch_hand.cannotteleport")
                    .withStyle(ChatFormatting.RED), true);
            fail(player);
            return false;
        }
        player.fallDistance = 0.0F;
        player.teleportTo(landing.getX() + 0.5D, landing.getY(), landing.getZ() + 0.5D);
        feedback(player, ParticleTypes.PORTAL, SoundEvents.ENDERMAN_TELEPORT);
        return true;
    }

    static Vec3 otherwhereEntityTeleportDestination(HitResult hit) {
        return hit != null && hit.getType() == HitResult.Type.ENTITY ? hit.getLocation() : null;
    }

    private static BlockPos findSafeLanding(ServerLevel level, BlockHitResult hit) {
        BlockPos base = hit.getBlockPos().relative(hit.getDirection());
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos pos = base.above(dy);
            if (hasStandingRoom(level, pos)) {
                return pos;
            }
        }
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos pos = base.below(dy);
            if (hasStandingRoom(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean hasStandingRoom(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos.below()).isAir()
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
    }

    private static double clearLiftDistance(LivingEntity entity, int max) {
        Level level = entity.level();
        for (int i = max; i > 0; i--) {
            AABB moved = entity.getBoundingBox().move(0.0D, i, 0.0D);
            if (level.noCollision(entity, moved)) {
                return i;
            }
        }
        return 0.0D;
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static long cooldownRemaining(ServerPlayer player, ItemStack stack) {
        long until = getTag(stack).getLongOr(COOLDOWN_UNTIL, 0L);
        return Math.max(0L, until - player.level().getGameTime());
    }

    private static void setCooldown(ItemStack stack, ServerPlayer player, int ticks) {
        long until = player.level().getGameTime() + Math.max(0, ticks);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(COOLDOWN_UNTIL, until));
    }

    private static boolean hasRecall(CompoundTag tag) {
        return tag.contains(RECALL_X) && tag.contains(RECALL_Y) && tag.contains(RECALL_Z) && tag.contains(RECALL_DIM);
    }

    private static void feedback(ServerPlayer player, net.minecraft.core.particles.ParticleOptions particle,
                                 net.minecraft.sounds.SoundEvent sound) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.7F, 1.0F);
        ((ServerLevel) player.level()).sendParticles(particle,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                16, 0.5D, 0.7D, 0.5D, 0.02D);
    }

    private static void fail(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.PLAYERS, 0.5F, 0.7F);
    }
}
