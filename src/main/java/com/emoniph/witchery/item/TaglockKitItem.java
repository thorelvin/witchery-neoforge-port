package com.emoniph.witchery.item;

import com.emoniph.witchery.blocks.entity.BloodRoseBlockEntity;
import com.emoniph.witchery.blocks.entity.LeechChestBlockEntity;
import com.emoniph.witchery.data.BedTaglockSavedData;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.FlyingMonkeyEntity;
import com.emoniph.witchery.entity.TreefydEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.scrying.ScryingSessions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Taglock Kit — right-click a player to collect their taglock (UUID + name).
 * Used as a ritual ingredient and to identify target players for poppets.
 */
public class TaglockKitItem extends Item {

    private static final String TAG_UUID = "taglock_uuid";
    private static final String TAG_NAME = "taglock_name";
    private static final String TAG_TYPE = "taglock_entity_type";
    private static final double PLAYER_TAGLOCK_BASE_FAIL_CHANCE = 0.2;
    private static final double PLAYER_TAGLOCK_NOT_CROUCHING_PENALTY = 0.2;
    private static final double PLAYER_TAGLOCK_NOT_BEHIND_PENALTY = 0.4;
    private static final int SCRY_USE_DURATION = 1200;
    private static final int SCRY_FEEDBACK_INTERVAL = 20;

    public TaglockKitItem(Properties props) { super(props); }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return isFilled(stack) ? SCRY_USE_DURATION : super.getUseDuration(stack, entity);
    }

    @Override
    public void onUseTick(net.minecraft.world.level.Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)
                || !(entity instanceof ServerPlayer player) || !isFilled(stack)) {
            return;
        }

        int elapsedTicks = getUseDuration(stack, entity) - remainingUseDuration;
        boolean feedbackTick = elapsedTicks % SCRY_FEEDBACK_INTERVAL == 0;

        Optional<UUID> uuid = getUUID(stack);
        if (uuid.isEmpty()) {
            ScryingSessions.stop(player);
            player.stopUsingItem();
            return;
        }

        Optional<LivingEntity> target = PoppetItem.findLivingEntity(serverLevel.getServer(), uuid.get());
        if (target.isEmpty() || !target.get().isAlive()) {
            if (feedbackTick && elapsedTicks == 0) {
                player.displayClientMessage(Component.translatable("witchery.crystal_ball.taglock_missing",
                        getBoundName(stack)).withStyle(ChatFormatting.DARK_PURPLE), false);
            }
            ScryingSessions.stop(player);
            player.stopUsingItem();
            return;
        }

        LivingEntity living = target.get();
        if (!living.level().dimension().equals(level.dimension())) {
            if (feedbackTick && elapsedTicks == 0) {
                player.displayClientMessage(Component.translatable("witchery.crystal_ball.taglock_other_dimension",
                        living.getDisplayName()).withStyle(ChatFormatting.DARK_PURPLE), false);
            }
            ScryingSessions.stop(player);
            player.stopUsingItem();
            return;
        }

        ScryingSessions.focusLiving(player, serverLevel, living);
        if (!feedbackTick) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.WITCH,
                living.getX(),
                living.getY() + living.getBbHeight() * 0.5,
                living.getZ(),
                6, 0.35, 0.4, 0.35, 0.02);
        if (elapsedTicks == 0) {
            serverLevel.playSound(null, living.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.4f, 1.35f);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, net.minecraft.world.level.Level level, LivingEntity entity, int timeCharged) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player && isFilled(stack)) {
            int elapsedTicks = getUseDuration(stack, entity) - timeCharged;
            ScryingSessions.stop(player);
            if (elapsedTicks > 0) {
                player.displayClientMessage(Component.translatable("witchery.crystal_ball.taglock_fades")
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
        return true;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, net.minecraft.world.level.Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player && isFilled(stack)) {
            ScryingSessions.stop(player);
            player.displayClientMessage(Component.translatable("witchery.crystal_ball.taglock_fades")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        return stack;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (target instanceof TreefydEntity || target instanceof FlameImpEntity || target instanceof FlyingMonkeyEntity) {
            return InteractionResult.PASS;
        }
        if (user.level().isClientSide()) return InteractionResult.SUCCESS;

        if (isFilled(stack)) {
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.already_full",
                    getBoundName(stack)).withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        if (target instanceof Player targetPlayer) {
            if (playerTaglockAttemptFails(user, targetPlayer)) {
                user.displayClientMessage(Component.translatable("witchery.taglock_kit.failed")
                        .withStyle(ChatFormatting.RED), false);
                targetPlayer.displayClientMessage(Component.translatable("witchery.taglock_kit.discovered")
                        .withStyle(ChatFormatting.GREEN), false);
                return InteractionResult.CONSUME;
            }

            bindSinglePlayerTaglock(user, hand, stack, targetPlayer.getUUID(), targetPlayer.getName().getString());

            user.displayClientMessage(Component.translatable("witchery.taglock_kit.collected",
                    targetPlayer.getName()).withStyle(ChatFormatting.GREEN), false);
            return InteractionResult.CONSUME;
        }

        bindSingleEntityTaglock(user, hand, stack, target);
        user.displayClientMessage(Component.translatable("witchery.taglock_kit.collected",
                target.getDisplayName()).withStyle(ChatFormatting.GREEN), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        BlockState clickedState = ctx.getLevel().getBlockState(ctx.getClickedPos());
        boolean bloodRose = ctx.getLevel().getBlockState(ctx.getClickedPos()).is(WitcheryBlocks.BLOOD_ROSE.get());
        boolean leechChest = ctx.getLevel().getBlockState(ctx.getClickedPos()).is(WitcheryBlocks.LEECH_CHEST.get());
        boolean bed = clickedState.getBlock() instanceof BedBlock;
        if (!bloodRose && !leechChest && !bed) {
            return InteractionResult.PASS;
        }
        if (ctx.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = ctx.getItemInHand();
        Player user = ctx.getPlayer();
        if (user == null) {
            return InteractionResult.PASS;
        }
        if (isFilled(stack)) {
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.already_full",
                    getBoundName(stack)).withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        if (bed) {
            BlockPos headPos = getBedHeadPos(clickedState, ctx.getClickedPos());
            Optional<BedTaglockSavedData.BedTaglockEntry> entry = BedTaglockSavedData.getEntry(ctx.getLevel(), headPos);
            if (entry.isPresent()) {
                bindSinglePlayerTaglock(user, ctx.getHand(), stack, entry.get().uuid(), entry.get().name());
                BedTaglockSavedData.clearEntry(ctx.getLevel(), headPos);
                user.displayClientMessage(Component.translatable("witchery.taglock_kit.collected_bed",
                        entry.get().name()).withStyle(ChatFormatting.GREEN), false);
                return InteractionResult.CONSUME;
            }

            user.displayClientMessage(Component.translatable("witchery.taglock_kit.no_bed_sample")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        if (bloodRose) {
            if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof BloodRoseBlockEntity be)) {
                return InteractionResult.PASS;
            }

            BloodRoseBlockEntity.DrainResult drainResult = be.drainStoredPlayer(user, true);
            if (drainResult.player() != null) {
                bindSinglePlayerTaglock(user, ctx.getHand(), stack, drainResult.player().uuid(), drainResult.player().name());
                boolean selfSample = drainResult.player().uuid().equals(user.getUUID());
                user.displayClientMessage(Component.translatable(selfSample
                                ? "witchery.taglock_kit.collected_rose_self"
                                : "witchery.taglock_kit.collected_rose",
                        drainResult.player().name()).withStyle(ChatFormatting.GREEN), false);
                return InteractionResult.CONSUME;
            }

            if (drainResult.onlyOwnTaglock()) {
                user.displayClientMessage(Component.translatable("witchery.taglock_kit.only_own_taglock")
                        .withStyle(ChatFormatting.RED), false);
                return InteractionResult.CONSUME;
            }
            if (!drainResult.missingPlayers().isEmpty()) {
                user.displayClientMessage(Component.translatable("witchery.taglock_kit.player_not_logged_in",
                        String.join(", ", drainResult.missingPlayers())).withStyle(ChatFormatting.RED), false);
                return InteractionResult.CONSUME;
            }

            user.displayClientMessage(Component.translatable("witchery.taglock_kit.no_blood_rose_sample")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof LeechChestBlockEntity be)) {
            return InteractionResult.PASS;
        }

        LeechChestBlockEntity.DrainResult drainResult = be.drainStoredPlayerExcept(user);
        if (drainResult.player() != null) {
            bindSinglePlayerTaglock(user, ctx.getHand(), stack, drainResult.player().uuid(), drainResult.player().name());
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.collected_leech_chest",
                    drainResult.player().name()).withStyle(ChatFormatting.GREEN), false);
            return InteractionResult.CONSUME;
        }

        if (drainResult.onlyOwnTaglock()) {
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.only_own_taglock")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }
        if (!drainResult.missingPlayers().isEmpty()) {
            user.displayClientMessage(Component.translatable("witchery.taglock_kit.player_not_logged_in",
                    String.join(", ", drainResult.missingPlayers())).withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        user.displayClientMessage(Component.translatable("witchery.taglock_kit.no_leech_chest_sample")
                .withStyle(ChatFormatting.RED), false);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay _display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        if (isFilled(stack)) {
            tooltip.accept(Component.translatable("witchery.taglock_kit.tooltip.filled", getBoundName(stack))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("witchery.taglock_kit.tooltip.empty")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    public static boolean isFilled(ItemStack stack) {
        return getTag(stack).contains(TAG_UUID);
    }

    public static boolean isTaglockKit(ItemStack stack) {
        return stack.getItem() instanceof TaglockKitItem;
    }

    public static Optional<UUID> getUUID(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(TAG_UUID)) return Optional.empty();
        try { return Optional.of(UUID.fromString(tag.getStringOr(TAG_UUID, ""))); }
        catch (IllegalArgumentException e) { return Optional.empty(); }
    }

    public static String getBoundName(ItemStack stack) {
        return getTag(stack).getStringOr(TAG_NAME, "");
    }

    public static boolean isPlayerTaglock(ItemStack stack) {
        return isFilled(stack) && getBoundEntityTypeId(stack).isEmpty();
    }

    public static Optional<Identifier> getBoundEntityTypeId(ItemStack stack) {
        String typeId = getTag(stack).getStringOr(TAG_TYPE, "");
        if (typeId.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Identifier.parse(typeId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<EntityType<?>> getBoundEntityType(ItemStack stack) {
        Optional<Identifier> typeId = getBoundEntityTypeId(stack);
        if (typeId.isEmpty()) {
            return Optional.empty();
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId.get()).orElse(null);
        return Optional.ofNullable(type);
    }

    public static void bind(ItemStack stack, UUID uuid, String name) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_UUID, uuid.toString());
            tag.putString(TAG_NAME, name);
            tag.remove(TAG_TYPE);
        });
    }

    public static void bindEntity(ItemStack stack, LivingEntity entity) {
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_UUID, entity.getStringUUID());
            tag.putString(TAG_NAME, entity.getDisplayName().getString());
            tag.putString(TAG_TYPE, typeId.toString());
        });
    }

    public static ItemStack filledPlayerTaglock(UUID uuid, String name) {
        ItemStack filledStack = new ItemStack(WitcheryItems.BOUND_TAGLOCK_KIT.get());
        bind(filledStack, uuid, name);
        return filledStack;
    }

    public static ItemStack filledEntityTaglock(LivingEntity entity) {
        ItemStack filledStack = new ItemStack(WitcheryItems.BOUND_TAGLOCK_KIT.get());
        bindEntity(filledStack, entity);
        return filledStack;
    }

    public static void replaceOneWithFilled(Player user, InteractionHand hand, ItemStack sourceStack, ItemStack filledStack) {
        if (sourceStack.getCount() == 1) {
            user.setItemInHand(hand, filledStack);
            return;
        }

        sourceStack.shrink(1);

        if (!user.getInventory().add(filledStack)) {
            user.drop(filledStack, false);
        }
    }

    private static void bindSinglePlayerTaglock(Player user, InteractionHand hand, ItemStack sourceStack, UUID uuid, String name) {
        replaceOneWithFilled(user, hand, sourceStack, filledPlayerTaglock(uuid, name));
    }

    private static void bindSingleEntityTaglock(Player user, InteractionHand hand, ItemStack sourceStack, LivingEntity entity) {
        replaceOneWithFilled(user, hand, sourceStack, filledEntityTaglock(entity));
    }

    private static boolean playerTaglockAttemptFails(Player user, Player target) {
        return user.getRandom().nextDouble()
                < playerTaglockFailChance(user.isCrouching(), isBehindTarget(user, target));
    }

    /**
     * Chance a player-taglock grab fails: 0.2 base, +0.2 when not crouching, +0.4 when not behind the
     * target. So success is 80% (behind + crouching), 60% (behind, standing), 40% (front, crouching),
     * 20% (front, standing) — the odds the Book of Shadows quotes.
     */
    static double playerTaglockFailChance(boolean crouching, boolean behind) {
        double failChance = PLAYER_TAGLOCK_BASE_FAIL_CHANCE;
        if (!crouching) {
            failChance += PLAYER_TAGLOCK_NOT_CROUCHING_PENALTY;
        }
        if (!behind) {
            failChance += PLAYER_TAGLOCK_NOT_BEHIND_PENALTY;
        }
        return failChance;
    }

    private static boolean isBehindTarget(Player user, Player target) {
        Vec3 targetLook = target.getLookAngle();
        Vec3 horizontalLook = new Vec3(targetLook.x, 0.0, targetLook.z);
        if (horizontalLook.lengthSqr() < 1.0E-6) {
            return false;
        }

        Vec3 toUser = user.position().subtract(target.position());
        Vec3 horizontalToUser = new Vec3(toUser.x, 0.0, toUser.z);
        if (horizontalToUser.lengthSqr() < 1.0E-6) {
            return false;
        }

        return horizontalLook.normalize().dot(horizontalToUser.normalize()) < 0.0;
    }

    private static BlockPos getBedHeadPos(BlockState state, BlockPos pos) {
        if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) == BedPart.HEAD) {
            return pos;
        }
        return pos.relative(BedBlock.getConnectedDirection(state));
    }

    private static CompoundTag getTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

}
