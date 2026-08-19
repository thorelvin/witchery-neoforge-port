package com.emoniph.witchery.blocks;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.data.WitcheryPlayerData.WolfQuestState;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.blocks.entity.WolfAltarBlockEntity;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.item.WerewolfProgression;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Wolf Altar — the werewolf progression questline (source {@code BlockStatueWerewolf}). Right-clicking
 * with the right offering for the current level advances the climb 0→10; an empty hand (or wrong item)
 * shows the current step's hint. The offering gates 1→4 (gold ingots, mutton, Tongue of Dog, the Horn
 * of the Hunt quest) and the re-craft of spare Moon Charms are handled here; the counter-driven levels
 * 5→9 read the quest counter that gameplay events feed (Phase 2).
 */
public class WolfAltarBlock extends BaseEntityBlock {

    public static final MapCodec<WolfAltarBlock> CODEC = simpleCodec(WolfAltarBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final int GOLD_REQUIRED = 3;
    private static final int MUTTON_REQUIRED = 30;
    private static final int TONGUE_REQUIRED = 10;
    public WolfAltarBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WolfAltarBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // the statue is drawn by WolfAltarRenderer
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        return serve(level, pos, (ServerPlayer) player, stack);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return serve(level, pos, (ServerPlayer) player, ItemStack.EMPTY);
    }

    private InteractionResult serve(Level level, BlockPos pos, ServerPlayer player, ItemStack held) {
        WitcheryPlayerData data = WitcheryAttachments.get(player);
        playSourceLordVoice(player, pos);
        int wolfLevel = data.getWerewolfLevel();

        // Re-craft a spare Moon Charm: an established werewolf may always trade 3 gold for one.
        if (wolfLevel >= 2 && held.is(Items.GOLD_INGOT) && held.getCount() >= GOLD_REQUIRED) {
            consume(player, held, GOLD_REQUIRED);
            dropMoonCharm(level, pos);
            msg(player, "witchery.werewolf.mooncharmcrafted");
            return InteractionResult.SUCCESS;
        }

        switch (wolfLevel) {
            case 0 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 1200, 0));
                msg(player, "witchery.werewolf.notworthy");
            }
            case 1 -> offeringGate(player, held, Items.GOLD_INGOT, GOLD_REQUIRED, data, pos, level, true, "level2");
            case 2 -> offeringGate(player, held, Items.MUTTON, MUTTON_REQUIRED, data, pos, level, false, "level3");
            case 3 -> offeringGate(player, held, WitcheryItems.TONGUE_OF_DOG.get(), TONGUE_REQUIRED, data, pos, level, false, "level4");
            case 4 -> hornOfTheHuntQuest(player, data, level, pos);
            case 5, 6, 7, 8, 9 -> counterQuest(player, data, level, pos, wolfLevel);
            case 10 -> msg(player, "witchery.werewolf.level10complete");
            default -> { }
        }
        return InteractionResult.SUCCESS;
    }

    /** Levels 1→4 offering gates: hand over N of an item to advance. */
    private void offeringGate(ServerPlayer player, ItemStack held, net.minecraft.world.item.Item want, int need,
                              WitcheryPlayerData data, BlockPos pos, Level level, boolean givesMoonCharm, String key) {
        if (!held.is(want)) {
            msg(player, "witchery.werewolf." + key + "begin", need);
        } else if (held.getCount() >= need) {
            consume(player, held, need);
            if (givesMoonCharm) dropMoonCharm(level, pos);
            levelUp(player, data, level, pos, key + "complete");
        } else {
            msg(player, "witchery.werewolf." + key + "progress", need, need - held.getCount());
        }
    }

    /** Level 4→5: the Horn of the Hunt quest. COMPLETE is set by defeating the Horned Huntsman (Phase 2). */
    private void hornOfTheHuntQuest(ServerPlayer player, WitcheryPlayerData data, Level level, BlockPos pos) {
        switch (data.getWolfQuestState()) {
            case NOT_STARTED -> {
                data.setWolfQuestState(WolfQuestState.STARTED);
                Block.popResource(level, pos.above(), new ItemStack(WitcheryItems.HORN_OF_THE_HUNT.get()));
                sync(player, data);
                msg(player, "witchery.werewolf.level5begin");
            }
            case STARTED -> msg(player, "witchery.werewolf.level5progress");
            case COMPLETE -> levelUp(player, data, level, pos, "level5complete");
        }
    }

    /** Levels 5→9: advance once the gameplay quest counter reaches the per-level target. */
    private void counterQuest(ServerPlayer player, WitcheryPlayerData data, Level level, BlockPos pos, int wolfLevel) {
        int target = WerewolfProgression.questTarget(wolfLevel);
        if (data.getWolfQuestCounter() >= target) {
            data.setWolfQuestState(WolfQuestState.COMPLETE);
        }
        String key = "level" + (wolfLevel + 1);
        switch (data.getWolfQuestState()) {
            case NOT_STARTED -> {
                data.setWolfQuestState(WolfQuestState.STARTED);
                sync(player, data);
                msg(player, "witchery.werewolf." + key + "begin", target);
            }
            case STARTED -> msg(player, "witchery.werewolf." + key + "progress",
                    target, target - data.getWolfQuestCounter());
            case COMPLETE -> levelUp(player, data, level, pos, key + "complete");
        }
    }

    private void levelUp(ServerPlayer player, WitcheryPlayerData data, Level level, BlockPos pos, String key) {
        data.increaseWerewolfLevel(); // also resets quest state/counter for the next rung
        sync(player, data);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 0.8F);
        msg(player, "witchery.werewolf." + key);
    }

    private static void consume(ServerPlayer player, ItemStack held, int n) {
        if (!player.isCreative()) held.shrink(n);
    }

    private static void dropMoonCharm(Level level, BlockPos pos) {
        Block.popResource(level, pos.above(), new ItemStack(WitcheryItems.MOON_CHARM.get()));
    }

    private static void sync(ServerPlayer player, WitcheryPlayerData data) {
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));
    }

    private static void msg(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.GOLD), false);
    }

    private static void playSourceLordVoice(ServerPlayer player, BlockPos pos) {
        SoundEvent sound = WitcherySoundEvents.MOB_WOLFMAN_LORD.get();
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
        player.connection.send(new ClientboundSoundPacket(holder, SoundSource.BLOCKS,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                1.0F, 1.0F, player.level().random.nextLong()));
    }
}
