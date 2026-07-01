package com.emoniph.witchery.blocks;

import com.emoniph.witchery.ai.NpcBrainService;
import com.emoniph.witchery.ai.NpcPersonas;
import com.emoniph.witchery.blocks.entity.StatueHobgoblinPatronBlockEntity;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.init.WitcheryEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Statue of Hobgoblin Patron — nearby hobgoblins worship it instead of their natural shrine.
 * Right-click to collect worship power (stealthy prayer theft).
 * Each use has a chance to anger Gulg or Mog, who send avatars to attack the witch.
 */
public class StatueHobgoblinPatronBlock extends BaseEntityBlock {
    public static final MapCodec<StatueHobgoblinPatronBlock> CODEC = simpleCodec(StatueHobgoblinPatronBlock::new);

    private static final int SEARCH_RADIUS = 16;
    private static final float ANGER_CHANCE = 0.25f; // 25% chance each use angers a god
    private static final int BOSS_SPAWN_RADIUS = 5;

    public StatueHobgoblinPatronBlock(BlockBehaviour.Properties props) { super(props); }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatueHobgoblinPatronBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()
                || !(level.getBlockEntity(pos) instanceof StatueHobgoblinPatronBlockEntity statue)) {
            return;
        }
        String owner = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getStringOr(StatueHobgoblinPatronBlockEntity.ITEM_OWNER_KEY, "");
        if (!owner.isEmpty()) {
            statue.setOwner(owner);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof StatueHobgoblinPatronBlockEntity statue)) {
            return InteractionResult.PASS;
        }

        AABB range = new AABB(pos).inflate(SEARCH_RADIUS);
        List<HobgoblinEntity> hobgoblins = sl.getEntitiesOfClass(HobgoblinEntity.class, range);

        int prayers = statue.drainPrayers();
        if (prayers <= 0) {
            if (hobgoblins.isEmpty()) {
                player.displayClientMessage(Component.translatable("witchery.statue_patron.no_worshippers"), false);
            } else {
                player.displayClientMessage(Component.translatable("witchery.statue_patron.no_prayers"), false);
            }
            smokePrayerTheft(sl, pos, false);
            return InteractionResult.FAIL;
        }

        player.displayClientMessage(Component.translatable("witchery.statue_patron.power_gained", prayers), false);
        smokePrayerTheft(sl, pos, true);

        float angerChance = Math.min(0.65F, ANGER_CHANCE + prayers * 0.02F);
        if (sl.getRandom().nextFloat() < angerChance) {
            boolean gulg = sl.getRandom().nextBoolean();
            player.displayClientMessage(Component.translatable(
                    gulg ? "witchery.statue_patron.gulg_angered" : "witchery.statue_patron.mog_angered"), false);
            for (HobgoblinEntity hob : hobgoblins) {
                hob.setTarget(player);
            }
            spawnAngryPatron(sl, pos, player, gulg);
            announceBossToHobgoblins(hobgoblins, player, gulg);
        }

        return InteractionResult.SUCCESS;
    }

    private static void smokePrayerTheft(ServerLevel level, BlockPos pos, boolean successful) {
        level.playSound(null, pos, successful ? SoundEvents.ENCHANTMENT_TABLE_USE : SoundEvents.GRINDSTONE_USE,
                SoundSource.BLOCKS, successful ? 0.85F : 0.45F, successful ? 0.6F : 0.8F);
        level.sendParticles(successful ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                successful ? 20 : 8, 0.45D, 0.35D, 0.45D, successful ? 0.04D : 0.01D);
    }

    private static void spawnAngryPatron(ServerLevel level, BlockPos statuePos, Player player, boolean gulg) {
        Mob boss = gulg
                ? WitcheryEntities.GOBLIN_GULG.get().create(level, EntitySpawnReason.EVENT)
                : WitcheryEntities.GOBLIN_MOG.get().create(level, EntitySpawnReason.EVENT);
        if (boss == null) {
            return;
        }
        BlockPos spawnPos = findBossSpawnPos(level, statuePos);
        boss.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.EVENT, null);
        boss.setPersistenceRequired();
        boss.setTarget(player);
        level.addFreshEntityWithPassengers(boss);
        level.playSound(null, spawnPos, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0F, gulg ? 0.7F : 0.9F);
        level.sendParticles(ParticleTypes.WITCH,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D,
                32, 0.65D, 0.8D, 0.65D, 0.05D);
    }

    private static BlockPos findBossSpawnPos(ServerLevel level, BlockPos statuePos) {
        for (int i = 0; i < 16; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = 2 + level.random.nextInt(BOSS_SPAWN_RADIUS);
            int x = statuePos.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = statuePos.getZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                    && !level.getBlockState(feet.below()).isAir()) {
                return feet;
            }
        }
        return statuePos.above();
    }

    private static void announceBossToHobgoblins(List<HobgoblinEntity> hobgoblins, Player player, boolean gulg) {
        HobgoblinEntity witness = hobgoblins.stream()
                .filter(HobgoblinEntity::isAlive)
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
        if (witness == null) {
            return;
        }
        String speech = gulg
                ? "*shrugs* \"Gulg comes. Big fists. Bad prayer.\""
                : "*gestures* \"Mog sees. Arrows soon. Hide shiny.\"";
        witness.sayToNearby(speech);
        if (player instanceof ServerPlayer serverPlayer) {
            NpcBrainService.INSTANCE.rememberExchange(NpcPersonas.HOBGOBLIN, witness, serverPlayer,
                    "the patron statue answered stolen prayers", speech);
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
