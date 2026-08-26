package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfForm;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfQuestState;
import com.emoniph.witchery.entity.HornedHuntsmanEntity;
import com.emoniph.witchery.entity.VillagerWerewolfConversion;
import com.emoniph.witchery.entity.WereVillagerEntity;
import com.emoniph.witchery.entity.WolfmanEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryEntityTags;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.item.WerewolfProgression;
import com.emoniph.witchery.item.WerewolfProgression.StatBoost;
import com.emoniph.witchery.item.WerewolfShiftUtil;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative player lycanthropy and Wolf Altar progression hooks. */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class WerewolfEvents {
    private static final int FORM_REFRESH_INTERVAL = 40;
    private static final int SYNC_INTERVAL = 200;
    private static final float INFECTION_HEALTH_FRACTION = 0.25F;
    private static final Identifier SPEED_MODIFIER_ID = id("werewolf_form_speed");
    private static final Identifier DAMAGE_MODIFIER_ID = id("werewolf_form_damage");
    private static final Identifier HEALTH_MODIFIER_ID = id("werewolf_form_health");
    private static final Identifier STEP_HEIGHT_MODIFIER_ID = id("werewolf_form_step_height");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private WerewolfEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!(player.level() instanceof ServerLevel level)) return;

        int tick = player.tickCount;
        if (data.isWerewolf() && tick % FORM_REFRESH_INTERVAL == 0) {
            updateMoonShift(player, level, data);
        }
        if (tick % FORM_REFRESH_INTERVAL == 0) {
            applyCurrentFormState(player, data);
        }
        if (data.isWerewolf() && tick % SYNC_INTERVAL == 0) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    player, SyncPlayerDataPayload.from(player, data));
        }
    }

    private static void updateMoonShift(ServerPlayer player, ServerLevel level, WitcheryPlayerData data) {
        boolean fullMoon = isFullMoonNight(level.getDayTime());
        boolean protectedByCharm = WerewolfShiftUtil.hasMoonCharm(player);
        boolean protectedByWolfsbane = WerewolfShiftUtil.isWolfsbaneActive(player, data);
        boolean transformed = WerewolfShiftUtil.inWolfShape(data);

        if (transformed && !fullMoon && !protectedByCharm && !protectedByWolfsbane) {
            WerewolfShiftUtil.shiftTo(player, data, WolfForm.NONE,
                    Component.translatable("witchery.werewolf.restored"));
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 1.5F, 1.0F);
        } else if (!transformed && fullMoon && !protectedByCharm && !protectedByWolfsbane) {
            WerewolfShiftUtil.shiftTo(player, data, WolfForm.WOLF,
                    Component.translatable("witchery.werewolf.transformed"));
            level.playSound(null, player.blockPosition(), WitcherySoundEvents.MOB_WOLFMAN_HOWL.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static void applyCurrentFormState(ServerPlayer player, WitcheryPlayerData data) {
        WolfForm form = WerewolfShiftUtil.inWolfShape(data) ? data.getWolfForm() : WolfForm.NONE;
        StatBoost boost = WerewolfProgression.stats(form, data.getWerewolfLevel());
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER_ID,
                boost.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER_ID,
                boost.damage(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(player.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID,
                boost.health(), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player.getAttribute(Attributes.STEP_HEIGHT), STEP_HEIGHT_MODIFIER_ID,
                form == WolfForm.NONE ? 0.0D : 0.4D, AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        if (form == WolfForm.NONE) return;
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false));
        player.removeEffect(MobEffects.SLOWNESS);
        shedIncompatibleEquipment(player, form == WolfForm.WOLFMAN);
    }

    private static void applyModifier(AttributeInstance attribute, Identifier id, double amount,
                                      AttributeModifier.Operation operation) {
        if (attribute == null) return;
        attribute.removeModifier(id);
        if (amount != 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    @SubscribeEvent
    public static void onWerewolfSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player) || !player.isAddedToLevel()) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (WerewolfShiftUtil.inWolfForm(data)) {
            event.setNewSize(EntityDimensions.scalable(0.6F, 0.8F).withEyeHeight(0.736F));
        }
    }

    private static void shedIncompatibleEquipment(ServerPlayer player, boolean wolfman) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            dropEquipment(player, slot);
        }
        if (wolfman) {
            dropEquipment(player, EquipmentSlot.MAINHAND);
            dropEquipment(player, EquipmentSlot.OFFHAND);
        }
    }

    private static void dropEquipment(ServerPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty() || stack.is(WitcheryItems.MOON_CHARM.get())) return;
        player.setItemSlot(slot, ItemStack.EMPTY);
        player.drop(stack, false);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onWerewolfCombatDamage(LivingDamageEvent.Pre event) {
        applyChargeDamage(event);
        applyWerewolfResistance(event);
    }

    private static void applyChargeDamage(LivingDamageEvent.Pre event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(attacker);
        if (!WerewolfShiftUtil.inWolfShape(data)) return;

        ItemStack held = attacker.getMainHandItem();
        double heldDamage = held.isEmpty() ? 0.0D : held.getAttributeModifiers().compute(
                Attributes.ATTACK_DAMAGE, 0.0D, EquipmentSlot.MAINHAND);
        if (heldDamage != 0.0D) {
            event.setNewDamage(2.0F);
        } else if (attacker.isSprinting()) {
            event.setNewDamage(event.getNewDamage()
                    + WerewolfProgression.stats(data.getWolfForm(), data.getWerewolfLevel()).damage());
        }
    }

    private static void applyWerewolfResistance(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!WerewolfShiftUtil.inWolfShape(data) || isUnmodifiedDamage(event.getSource())) return;

        StatBoost boost = WerewolfProgression.stats(data.getWolfForm(), data.getWerewolfLevel());
        float damage = event.getNewDamage();
        if (!event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            damage = Math.max(0.0F, damage - boost.resistance());
        }
        Entity directAttacker = event.getSource().getDirectEntity();
        if (!isWerewolf(directAttacker) && !SilverEvents.isSilverSource(event.getSource())) {
            damage = Math.max(Math.min(damage, boost.damageCap()), 0.5F);
        }
        event.setNewDamage(damage);
    }

    private static boolean isUnmodifiedDamage(DamageSource source) {
        return source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.FALL);
    }

    @SubscribeEvent
    public static void onWerewolfJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!WerewolfShiftUtil.inWolfShape(data)) return;

        StatBoost boost = WerewolfProgression.stats(data.getWolfForm(), data.getWerewolfLevel());
        Vec3 motion = player.getDeltaMovement().add(0.0D, boost.jump(), 0.0D);
        if (player.isSprinting()) {
            double yaw = Math.toRadians(player.getYRot());
            motion = motion.add(-Math.sin(yaw) * boost.leap(), 0.0D,
                    Math.cos(yaw) * boost.leap());
        }
        player.setDeltaMovement(motion);
        player.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onWerewolfFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!WerewolfShiftUtil.inWolfShape(data)) return;
        int reduction = WerewolfProgression.stats(data.getWolfForm(), data.getWerewolfLevel()).fall();
        event.setDistance(reduction == -1 ? 0.0F : Math.max(0.0F, event.getDistance() - reduction));
    }

    /** Level 3 wolf form can claw through the same five soft blocks as the source interaction hook. */
    @SubscribeEvent
    public static void onWolfClawDig(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative()) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.getWerewolfLevel() < 3 || !WerewolfShiftUtil.inWolfForm(data)
                || !player.isShiftKeyDown()) return;
        var state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.SAND) && !state.is(Blocks.DIRT)
                && !state.is(Blocks.CLAY) && !state.is(Blocks.GRAVEL)) return;

        player.gameMode.destroyBlock(event.getPos());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onWerewolfQuestKill(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isWerewolf() || data.getWolfQuestState() != WolfQuestState.STARTED) return;

        int level = data.getWerewolfLevel();
        LivingEntity victim = event.getEntity();
        if (level == 4 && victim instanceof HornedHuntsmanEntity) {
            data.setWolfQuestState(WolfQuestState.COMPLETE);
            sync(player, data);
            return;
        }

        boolean counted = switch (level) {
            case 5 -> WerewolfShiftUtil.inWolfShape(data) && victim instanceof Enemy && !player.onGround();
            case 8 -> WerewolfShiftUtil.inWolfmanForm(data) && victim instanceof ZombifiedPiglin;
            case 9 -> WerewolfShiftUtil.inWolfShape(data)
                    && (victim instanceof Villager || victim instanceof Player);
            default -> false;
        };
        if (!counted) return;
        data.incrementWolfQuestCounter();
        sync(player, data);
        player.displayClientMessage(Component.translatable(
                "witchery.werewolf.level" + (level + 1) + "progress",
                questTarget(level), Math.max(0, questTarget(level) - data.getWolfQuestCounter())), true);
    }

    @SubscribeEvent
    public static void onTameQuestWolf(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Wolf wolf)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.getWerewolfLevel() != 7 || data.getWolfQuestState() != WolfQuestState.STARTED
                || !WerewolfShiftUtil.inWolfForm(data) || wolf.isTame() || wolf.isBaby()) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        event.setCanceled(true);
        if (level.random.nextInt(3) == 0) {
            wolf.tame(player);
            wolf.getNavigation().stop();
            wolf.setTarget(null);
            wolf.setOrderedToSit(true);
            wolf.setHealth(20.0F);
            level.broadcastEntityEvent(wolf, (byte) 7);
            data.incrementWolfQuestCounter();
            sync(player, data);
            player.displayClientMessage(Component.translatable("witchery.werewolf.level8progress",
                    6, Math.max(0, 6 - data.getWolfQuestCounter())), true);
        } else {
            level.broadcastEntityEvent(wolf, (byte) 6);
            if (level.random.nextInt(10) == 0) {
                wolf.setLastHurtByMob(player);
                wolf.setTarget(player);
            }
        }
    }

    @SubscribeEvent
    public static void onInfectiousWolfmanDamage(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof WolfmanEntity wolfman)
                || !wolfman.isInfectious()
                || !(event.getEntity().level() instanceof ServerLevel level)) return;

        if (event.getEntity() instanceof Villager villager) {
            float healthAfter = villager.getHealth() - event.getNewDamage();
            if (healthAfter < villager.getMaxHealth() * INFECTION_HEALTH_FRACTION
                    && spawnWereVillager(level, villager)) {
                event.setNewDamage(0.0F);
            }
        } else if (event.getEntity() instanceof ServerPlayer victim) {
            infectPlayer(victim, WitcheryConfig.ALLOW_VAMPIRE_WOLF_HYBRIDS.get());
        }
    }

    @SubscribeEvent
    public static void onWerewolfPlayerBite(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        WitcheryPlayerData attackerData = WitcheryAttachments.get(attacker);
        if (attackerData.getWerewolfLevel() < 10 || !WerewolfShiftUtil.inWolfShape(attackerData)) return;

        LivingEntity victim = event.getEntity();
        float healthAfter = victim.getHealth() - event.getNewDamage();
        if (healthAfter <= 0.0F || healthAfter >= victim.getMaxHealth() * INFECTION_HEALTH_FRACTION
                || !(victim.level() instanceof ServerLevel level) || level.random.nextInt(4) != 1) return;

        if (victim instanceof Villager villager) {
            if (spawnWereVillager(level, villager)) {
                event.setNewDamage(0.0F);
            }
        } else if (victim instanceof ServerPlayer victimPlayer
                && WitcheryConfig.ALLOW_PLAYER_TO_PLAYER_WOLF_INFECTION.get()
                && !SilverEvents.wearsFullSilveredSet(victimPlayer)
                && (WitcheryConfig.ALLOW_VAMPIRE_WOLF_HYBRIDS.get()
                    || !WitcheryAttachments.get(victimPlayer).isVampire())) {
            infectPlayer(victimPlayer, WitcheryConfig.ALLOW_VAMPIRE_WOLF_HYBRIDS.get());
        }
    }

    private static void infectPlayer(ServerPlayer victim, boolean allowVampireHybrid) {
        WitcheryPlayerData data = WitcheryAttachments.get(victim);
        if (data.isWerewolf() || (!allowVampireHybrid && data.isVampire())) return;
        data.setWerewolfLevel(1);
        sync(victim, data);
        victim.displayClientMessage(Component.translatable("witchery.werewolf.infection")
                .withStyle(ChatFormatting.DARK_PURPLE), false);
    }

    @SubscribeEvent
    public static void onWolfFeed(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.getWerewolfLevel() < 4 || !WerewolfShiftUtil.inWolfShape(data)
                || event.getEntity().getType().is(EntityTypeTags.UNDEAD)) return;
        player.getFoodData().eat(8, 0.8F);
        if (player.level() instanceof ServerLevel level) {
            if (level.random.nextInt(3) == 0) {
                level.playSound(null, event.getEntity().blockPosition(),
                        com.emoniph.witchery.init.WitcherySoundEvents.MOB_WOLFMAN_EAT.get(),
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            level.sendParticles(DustParticleOptions.REDSTONE,
                    event.getEntity().getX(), event.getEntity().getY() + 0.7D,
                    event.getEntity().getZ(), 16, 0.5D, 0.8D, 0.5D, 0.0D);
        }
    }

    @SubscribeEvent
    public static void onWolfDig(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || event.getDrops().size() != 1
                || !event.getDrops().getFirst().getItem().is(Items.DIRT)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (data.getWerewolfLevel() < 3 || !WerewolfShiftUtil.inWolfForm(data)) return;

        ServerLevel level = event.getLevel();
        long now = System.currentTimeMillis();
        if (data.getLastBoneFind() != Long.MIN_VALUE
                && data.getLastBoneFind() + WerewolfProgression.BONE_FIND_COOLDOWN_MILLIS >= now) return;
        if (level.random.nextInt(20) != 0) return;
        data.setLastBoneFind(now);
        sync(player, data);
        event.getDrops().add(new ItemEntity(level,
                event.getPos().getX() + 0.5D, event.getPos().getY() + 0.5D,
                event.getPos().getZ() + 0.5D,
                new ItemStack(Items.BONE, level.random.nextInt(5) == 0 ? 2 : 1)));
    }

    @SubscribeEvent
    public static void onWolfmanRendArmor(LivingDamageEvent.Pre event) {
        if (event.getNewDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        WitcheryPlayerData data = WitcheryAttachments.get(attacker);
        if (data.getWerewolfLevel() < 9 || !WerewolfShiftUtil.inWolfmanForm(data)) return;

        LivingEntity victim = event.getEntity();
        EquipmentSlot slot = ARMOR_SLOTS[victim.getRandom().nextInt(ARMOR_SLOTS.length)];
        ItemStack armor = victim.getItemBySlot(slot);
        if (armor.isEmpty()) return;
        if (armor.isDamageableItem()) {
            armor.hurtAndBreak((int) Math.ceil(armor.getMaxDamage() * 0.25F), victim, slot);
        } else if (victim instanceof Player && victim.level() instanceof ServerLevel level) {
            ItemStack ripped = armor.copy();
            victim.setItemSlot(slot, ItemStack.EMPTY);
            ItemEntity drop = victim.spawnAtLocation(level, ripped);
            if (drop != null) drop.setPickUpDelay(100);
        }
    }

    static int questTarget(int level) {
        return WerewolfProgression.questTarget(level);
    }

    private static boolean spawnWereVillager(ServerLevel level, Villager villager) {
        return VillagerWerewolfConversion.convertVillager(level, villager, true) != null;
    }

    public static boolean isWerewolf(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return WitcheryAttachments.get(player).isWerewolf();
        }
        return entity instanceof WolfmanEntity || entity instanceof WereVillagerEntity;
    }

    public static boolean isVampire(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return WitcheryAttachments.get(player).isVampire();
        }
        return entity != null && entity.getType().is(WitcheryEntityTags.VAMPIRES);
    }

    static boolean isFullMoon(long dayTime) {
        return WerewolfProgression.isFullMoon(dayTime);
    }

    static boolean isFullMoonNight(long dayTime) {
        return WerewolfProgression.isFullMoonNight(dayTime);
    }

    private static void sync(ServerPlayer player, WitcheryPlayerData data) {
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path);
    }
}
