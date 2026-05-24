package com.emoniph.witchery.entity;

import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.TaglockKitItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Treefyd - wooden golem summoned by ritual. Guards the caster's base. */
public class TreefydEntity extends Monster {
    // wanderGoal is NOT a field initializer — field inits run after super(), but registerGoals()
    // is called from inside super(). A null goal crashes GoalSelector.addGoal via hashCode().
    @Nullable
    private UUID ownerUuid;
    private boolean sentinel;
    private final Map<UUID, String> ignoredPlayers = new LinkedHashMap<>();
    private final Map<UUID, String> ignoredCreatures = new LinkedHashMap<>();

    public TreefydEntity(EntityType<? extends TreefydEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(4, new SentinelAwareWanderGoal(this, 0.7));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, 10, true, false,
                (entity, serverLevel) -> shouldTarget(entity)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<Monster>(this, Monster.class, 10, true, false,
                (entity, serverLevel) -> entity != this && !(entity instanceof TreefydEntity) && shouldTarget(entity)));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
    }

    public boolean isOwnedBy(Player player) {
        return ownerUuid != null && ownerUuid.equals(player.getUUID());
    }

    public boolean isSentinel() {
        return sentinel;
    }

    private boolean shouldTarget(@Nullable LivingEntity entity) {
        return !isFriendly(entity);
    }

    private boolean isFriendly(@Nullable LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (ownerUuid != null && ownerUuid.equals(entity.getUUID())) {
            return true;
        }
        if (entity instanceof Player && ignoredPlayers.containsKey(entity.getUUID())) {
            return true;
        }
        if (ignoredCreatures.containsKey(entity.getUUID())) {
            return true;
        }
        if (entity instanceof TreefydEntity other) {
            return ownerUuid != null && ownerUuid.equals(other.ownerUuid);
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && getTarget() != null && isFriendly(getTarget())) {
            setTarget(null);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isFriendly(target) && super.canAttack(target);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }

        if (stack.is(WitcheryItems.TREEFYD_SEEDS.get())) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            spawnClone((ServerLevel) level(), player);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(WitcheryItems.TAGLOCK_KIT.get()) && TaglockKitItem.isFilled(stack)) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            return handleTaglockCommand(player, stack);
        }

        if (stack.is(WitcheryItems.CREEPER_HEART.get())) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            applyUpgrade(100.0D, 4.0D);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(WitcheryItems.DEMON_HEART.get())) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            applyUpgrade(150.0D, 5.0D);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(WitcheryItems.BOLINE.get())) {
            if (level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            sentinel = !sentinel;
            if (sentinel) {
                getNavigation().stop();
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private void spawnClone(ServerLevel level, Player owner) {
        TreefydEntity clone = WitcheryEntities.TREEFYD.get().create(level, EntitySpawnReason.TRIGGERED);
        if (clone == null) {
            return;
        }
        clone.snapTo(getX() + 0.5D, getY(), getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        clone.finalizeSpawn(level, level.getCurrentDifficultyAt(clone.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        clone.setOwner(owner);
        clone.setPersistenceRequired();
        clone.ignoredPlayers.putAll(this.ignoredPlayers);
        clone.ignoredCreatures.putAll(this.ignoredCreatures);
        level.addFreshEntity(clone);
    }

    private void applyUpgrade(double maxHealth, double attackDamage) {
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (health != null) {
            health.setBaseValue(maxHealth);
            setHealth(getMaxHealth());
        }
        if (attack != null) {
            attack.setBaseValue(attackDamage);
        }
    }

    private InteractionResult handleTaglockCommand(Player player, ItemStack stack) {
        Optional<UUID> boundUuid = TaglockKitItem.getUUID(stack);
        if (boundUuid.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (TaglockKitItem.isPlayerTaglock(stack)) {
            return handlePlayerTaglockCommand(player, stack, boundUuid.get());
        }
        return handleCreatureTaglockCommand(player, stack, boundUuid.get());
    }

    private InteractionResult handlePlayerTaglockCommand(Player player, ItemStack stack, UUID targetUuid) {
        setTarget(null);

        if (targetUuid.equals(player.getUUID())) {
            showCurrentKnownEntities(player);
            return InteractionResult.SUCCESS;
        }

        String targetName = TaglockKitItem.getBoundName(stack);
        if (player.isShiftKeyDown()) {
            if (ignoredPlayers.remove(targetUuid) != null) {
                player.displayClientMessage(Component.translatable("witchery.treefyd.ignore_player_removed",
                        displayKnownPlayerName(targetUuid, targetName)).withStyle(ChatFormatting.YELLOW), false);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            showCurrentKnownEntities(player);
            return InteractionResult.SUCCESS;
        }

        if (!ignoredPlayers.containsKey(targetUuid)) {
            ignoredPlayers.put(targetUuid, targetName);
            player.displayClientMessage(Component.translatable("witchery.treefyd.ignore_player_added",
                    displayKnownPlayerName(targetUuid, targetName)).withStyle(ChatFormatting.GREEN), false);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        showCurrentKnownEntities(player);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult handleCreatureTaglockCommand(Player player, ItemStack stack, UUID targetUuid) {
        setTarget(null);

        if (targetUuid.equals(getUUID())) {
            showCurrentKnownEntities(player);
            return InteractionResult.SUCCESS;
        }

        String targetName = TaglockKitItem.getBoundName(stack);
        if (player.isShiftKeyDown()) {
            if (ignoredCreatures.remove(targetUuid) != null) {
                player.displayClientMessage(Component.translatable("witchery.treefyd.ignore_creature_removed",
                        targetName).withStyle(ChatFormatting.YELLOW), false);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            showCurrentKnownEntities(player);
            return InteractionResult.SUCCESS;
        }

        if (!ignoredCreatures.containsKey(targetUuid)) {
            ignoredCreatures.put(targetUuid, targetName);
            player.displayClientMessage(Component.translatable("witchery.treefyd.ignore_creature_added",
                    targetName).withStyle(ChatFormatting.GREEN), false);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        showCurrentKnownEntities(player);
        return InteractionResult.SUCCESS;
    }

    private void showCurrentKnownEntities(Player player) {
        player.displayClientMessage(Component.translatable("witchery.treefyd.known_entities",
                describeKnownEntities()).withStyle(ChatFormatting.GRAY), false);
    }

    private String describeKnownEntities() {
        List<String> names = new ArrayList<>();
        if (ownerUuid != null) {
            names.add(displayKnownPlayerName(ownerUuid, null));
        }
        for (Map.Entry<UUID, String> entry : ignoredPlayers.entrySet()) {
            if (!entry.getKey().equals(ownerUuid)) {
                names.add(displayKnownPlayerName(entry.getKey(), entry.getValue()));
            }
        }
        for (String name : ignoredCreatures.values()) {
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return String.join(", ", names);
    }

    private String displayKnownPlayerName(UUID uuid, @Nullable String fallbackName) {
        if (level() instanceof ServerLevel serverLevel) {
            ServerPlayer online = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (online != null) {
                return online.getName().getString();
            }
        }
        if (fallbackName != null && !fallbackName.isBlank()) {
            return fallbackName;
        }
        return uuid.toString();
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) {
            tag.putString("OwnerUUID", ownerUuid.toString());
        }
        tag.putBoolean("Sentinel", sentinel);
        tag.putInt("IgnoredPlayerCount", ignoredPlayers.size());
        int i = 0;
        for (Map.Entry<UUID, String> entry : ignoredPlayers.entrySet()) {
            tag.putString("IgnoredPlayerUUID" + i, entry.getKey().toString());
            tag.putString("IgnoredPlayerName" + i, entry.getValue());
            i++;
        }
        tag.putInt("IgnoredCreatureCount", ignoredCreatures.size());
        i = 0;
        for (Map.Entry<UUID, String> entry : ignoredCreatures.entrySet()) {
            tag.putString("IgnoredCreatureUUID" + i, entry.getKey().toString());
            tag.putString("IgnoredCreatureName" + i, entry.getValue());
            i++;
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        String owner = tag.getStringOr("OwnerUUID", "");
        if (owner.isEmpty()) {
            ownerUuid = null;
        } else {
            try {
                ownerUuid = UUID.fromString(owner);
            } catch (IllegalArgumentException ignored) {
                ownerUuid = null;
            }
        }
        sentinel = tag.getBooleanOr("Sentinel", false);
        ignoredPlayers.clear();
        int ignoredCount = tag.getIntOr("IgnoredPlayerCount", 0);
        for (int i = 0; i < ignoredCount; i++) {
            String ignoredUuid = tag.getStringOr("IgnoredPlayerUUID" + i, "");
            if (ignoredUuid.isEmpty()) {
                continue;
            }
            try {
                ignoredPlayers.put(UUID.fromString(ignoredUuid), tag.getStringOr("IgnoredPlayerName" + i, ""));
            } catch (IllegalArgumentException ignored) {
            }
        }
        ignoredCreatures.clear();
        int ignoredCreatureCount = tag.getIntOr("IgnoredCreatureCount", 0);
        for (int i = 0; i < ignoredCreatureCount; i++) {
            String ignoredUuid = tag.getStringOr("IgnoredCreatureUUID" + i, "");
            if (ignoredUuid.isEmpty()) {
                continue;
            }
            try {
                ignoredCreatures.put(UUID.fromString(ignoredUuid), tag.getStringOr("IgnoredCreatureName" + i, ""));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static final class SentinelAwareWanderGoal extends WaterAvoidingRandomStrollGoal {
        private final TreefydEntity treefyd;

        private SentinelAwareWanderGoal(TreefydEntity treefyd, double speedModifier) {
            super(treefyd, speedModifier);
            this.treefyd = treefyd;
        }

        @Override
        public boolean canUse() {
            return !treefyd.isSentinel() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !treefyd.isSentinel() && super.canContinueToUse();
        }
    }
}
