package com.emoniph.witchery.blocks;

import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.ai.NpcPersonas;
import com.emoniph.witchery.blocks.entity.StatueHobgoblinPatronBlockEntity;
import com.emoniph.witchery.config.WitcheryConfig;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.events.WitcheryPlayerEvents;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryMobEffects;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bound Statue of Hobgoblin Patron. Nearby hobgoblins worship it, feeding its owner's
 * infusion and Worship effect; five worshippers and a Nether Star call Mog and Gulg.
 */
public class StatueHobgoblinPatronBlock extends BaseEntityBlock {
    public static final MapCodec<StatueHobgoblinPatronBlock> CODEC = simpleCodec(StatueHobgoblinPatronBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 24.0D, 14.0D);
    private static final int WORSHIP_RADIUS = 8;

    public StatueHobgoblinPatronBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatueHobgoblinPatronBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, WitcheryBlockEntities.STATUE_HOBGOBLIN_PATRON.get(),
                StatueHobgoblinPatronBlock::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()
                || !(level.getBlockEntity(pos) instanceof StatueHobgoblinPatronBlockEntity statue)) {
            return;
        }
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String owner = tag.getStringOr(StatueHobgoblinPatronBlockEntity.ITEM_OWNER_KEY, "");
        if (!owner.isEmpty()) {
            UUID ownerUuid = parseUuid(tag.getStringOr(
                    StatueHobgoblinPatronBlockEntity.ITEM_OWNER_UUID_KEY, ""));
            statue.setOwner(owner, ownerUuid);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.NETHER_STAR)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof StatueHobgoblinPatronBlockEntity statue)) {
            return InteractionResult.PASS;
        }
        if (!statue.isOwner(player)) {
            player.displayClientMessage(Component.translatable("witchery.statue_patron.not_owner"), false);
            failedSummonFeedback(serverLevel, pos);
            return InteractionResult.CONSUME;
        }

        List<HobgoblinEntity> hobgoblins = nearbyHobgoblins(serverLevel, pos);
        int worshippers = updateWorshippersAndGetLevel(serverLevel, pos, hobgoblins);
        if (!PatronWorshipContracts.canManuallySummon(worshippers)) {
            player.displayClientMessage(Component.translatable("witchery.statue_patron.requires_worshippers",
                    PatronWorshipContracts.MANUAL_SUMMON_WORSHIPPERS, worshippers), false);
            failedSummonFeedback(serverLevel, pos);
            return InteractionResult.CONSUME;
        }

        SummonResult result = summonGoblinGods(serverLevel, pos, serverPlayer, 16.0D, 8);
        if (result != SummonResult.SUCCESS) {
            player.displayClientMessage(Component.translatable(result.translationKey), false);
            failedSummonFeedback(serverLevel, pos);
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        sacrificeWorshippers(serverLevel, pos, hobgoblins);
        player.displayClientMessage(Component.translatable("witchery.statue_patron.gods_summoned"), false);
        successfulSummonFeedback(serverLevel, pos);
        announceBossToHobgoblins(hobgoblins, serverPlayer);
        return InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    private static void serverTick(Level level, BlockPos pos, BlockState state,
                                   StatueHobgoblinPatronBlockEntity statue) {
        if (!(level instanceof ServerLevel serverLevel) || !statue.hasOwner()
                || serverLevel.getGameTime() % PatronWorshipContracts.PULSE_TICKS != 0L) {
            return;
        }

        List<HobgoblinEntity> hobgoblins = nearbyHobgoblins(serverLevel, pos);
        int worshippers = updateWorshippersAndGetLevel(serverLevel, pos, hobgoblins);
        ServerPlayer owner = resolveOwner(serverLevel, statue);
        if (owner == null || owner.level() != serverLevel || owner.distanceToSqr(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D * 64.0D) {
            return;
        }

        if (PatronWorshipContracts.canRechargeInfusion(worshippers)) {
            WitcheryPlayerData data = WitcheryAttachments.get(owner);
            int current = data.getInfusionEnergy();
            int maximum = data.getInfusionMaxEnergy();
            if (current < maximum) {
                data.setInfusionEnergy(Math.min(current + PatronWorshipContracts.INFUSION_PER_PULSE, maximum), maximum);
                WitcheryPlayerEvents.syncToClient(owner);
                serverLevel.sendParticles(SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, 0xFFFFFF, 1.0F),
                        owner.getX(), owner.getY() + 1.0D,
                        owner.getZ(), 8, 0.4D, 0.8D, 0.4D, 0.05D);
                serverLevel.playSound(null, owner.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
                        SoundSource.PLAYERS, 1.0F, 2.0F);
            }
        }

        int amplifier = PatronWorshipContracts.worshipAmplifier(worshippers);
        if (amplifier >= 0) {
            owner.addEffect(new MobEffectInstance(WitcheryMobEffects.WORSHIP,
                    PatronWorshipContracts.WORSHIP_EFFECT_TICKS, amplifier, true, true));
        }

        double summonChance = PatronWorshipContracts.automaticSummonChance(
                WitcheryConfig.HOBGOBLIN_GOD_SPAWN_CHANCE_PERCENT.get());
        if (amplifier >= 1 && serverLevel.random.nextDouble() < summonChance
                && summonGoblinGods(serverLevel, pos, owner, 64.0D, 16) == SummonResult.SUCCESS) {
            successfulSummonFeedback(serverLevel, pos);
            announceBossToHobgoblins(hobgoblins, owner);
        }
    }

    static int updateWorshippersAndGetLevel(ServerLevel level, BlockPos pos, List<HobgoblinEntity> hobgoblins) {
        int worshippers = 0;
        for (HobgoblinEntity hobgoblin : hobgoblins) {
            if (hobgoblin.isWorshippingAt(pos)) {
                worshippers++;
            } else if (!hobgoblin.isWorshipping()) {
                hobgoblin.beginPatronWorship(pos);
            }
        }
        return worshippers;
    }

    private static List<HobgoblinEntity> nearbyHobgoblins(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(HobgoblinEntity.class, new AABB(pos).inflate(WORSHIP_RADIUS),
                HobgoblinEntity::isAlive);
    }

    private static SummonResult summonGoblinGods(ServerLevel level, BlockPos statuePos, ServerPlayer player,
                                                 double detectDistance, int spawnDistance) {
        AABB detection = new AABB(statuePos).inflate(detectDistance);
        if (!level.getEntitiesOfClass(com.emoniph.witchery.entity.GoblinGulgEntity.class, detection).isEmpty()
                || !level.getEntitiesOfClass(com.emoniph.witchery.entity.GoblinMogEntity.class, detection).isEmpty()) {
            return SummonResult.ALREADY_PRESENT;
        }

        Mob mog = WitcheryEntities.GOBLIN_MOG.get().create(level, EntitySpawnReason.EVENT);
        Mob gulg = WitcheryEntities.GOBLIN_GULG.get().create(level, EntitySpawnReason.EVENT);
        if (mog == null || gulg == null) {
            return SummonResult.NO_ROOM;
        }
        Optional<BlockPos> spawnPos = findBossSpawnPos(level, statuePos, spawnDistance, mog);
        if (spawnPos.isEmpty()) {
            return SummonResult.NO_ROOM;
        }

        BlockPos feet = spawnPos.get();
        prepareBoss(level, feet, player, mog);
        prepareBoss(level, feet, player, gulg);
        if (!level.addFreshEntity(mog)) {
            return SummonResult.NO_ROOM;
        }
        if (!level.addFreshEntity(gulg)) {
            mog.discard();
            return SummonResult.NO_ROOM;
        }
        level.playSound(null, feet, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.FLAME, feet.getX() + 0.5D, feet.getY() + 1.0D,
                feet.getZ() + 0.5D, 48, 0.75D, 1.0D, 0.75D, 0.06D);
        return SummonResult.SUCCESS;
    }

    private static void prepareBoss(ServerLevel level, BlockPos spawnPos, Player player, Mob boss) {
        boss.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.EVENT, null);
        boss.setPersistenceRequired();
        boss.setTarget(player);
    }

    private static Optional<BlockPos> findBossSpawnPos(ServerLevel level, BlockPos statuePos,
                                                       int spawnDistance, Mob probe) {
        for (int i = 0; i < 32; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = 2 + level.random.nextInt(Math.max(1, spawnDistance - 1));
            int x = statuePos.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = statuePos.getZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            probe.setPos(x + 0.5D, y, z + 0.5D);
            if (level.getWorldBorder().isWithinBounds(feet)
                    && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()
                    && level.noCollision(probe)) {
                return Optional.of(feet);
            }
        }
        return Optional.empty();
    }

    private static void sacrificeWorshippers(ServerLevel level, BlockPos pos, List<HobgoblinEntity> hobgoblins) {
        int remaining = PatronWorshipContracts.MANUAL_SUMMON_WORSHIPPERS;
        for (HobgoblinEntity hobgoblin : hobgoblins) {
            if (remaining <= 0) {
                break;
            }
            if (hobgoblin.isWorshippingAt(pos)) {
                level.sendParticles(ParticleTypes.SOUL, hobgoblin.getX(), hobgoblin.getY() + 0.5D,
                        hobgoblin.getZ(), 12, 0.25D, 0.45D, 0.25D, 0.04D);
                hobgoblin.discard();
                remaining--;
            }
        }
    }

    private static void successfulSummonFeedback(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5D, pos.getY() + 1.0D,
                pos.getZ() + 0.5D, 32, 0.5D, 0.5D, 0.5D, 0.05D);
    }

    private static void failedSummonFeedback(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5F, 0.8F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 1.0D,
                pos.getZ() + 0.5D, 8, 0.4D, 0.35D, 0.4D, 0.01D);
    }

    private static void announceBossToHobgoblins(List<HobgoblinEntity> hobgoblins, Player player) {
        HobgoblinEntity witness = hobgoblins.stream()
                .filter(HobgoblinEntity::isAlive)
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
        if (witness == null) {
            return;
        }
        String speech = "*bows low* \"The old gods answer. Five voices fall silent.\"";
        witness.sayToNearby(speech);
        if (player instanceof ServerPlayer serverPlayer) {
            NpcBrainService.INSTANCE.rememberExchange(NpcPersonas.HOBGOBLIN, witness, serverPlayer,
                    "the Patron called both gods and five worshippers fell silent", speech);
        }
    }

    @Nullable
    private static ServerPlayer resolveOwner(ServerLevel level, StatueHobgoblinPatronBlockEntity statue) {
        ServerPlayer owner = statue.ownerUuid() == null
                ? null
                : level.getServer().getPlayerList().getPlayer(statue.ownerUuid());
        return owner != null ? owner : level.getServer().getPlayerList().getPlayerByName(statue.ownerName());
    }

    @Nullable
    private static UUID parseUuid(String value) {
        try {
            return value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private enum SummonResult {
        SUCCESS(""),
        ALREADY_PRESENT("witchery.statue_patron.gods_present"),
        NO_ROOM("witchery.statue_patron.no_room");

        private final String translationKey;

        SummonResult(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof StatueHobgoblinPatronBlockEntity statue) {
            return List.of(statue.createDropStack(this));
        }
        return List.of(new ItemStack(this));
    }
}
