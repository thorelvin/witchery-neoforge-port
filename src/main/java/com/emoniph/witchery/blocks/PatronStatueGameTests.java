package com.emoniph.witchery.blocks;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.blocks.entity.StatueHobgoblinPatronBlockEntity;
import com.emoniph.witchery.entity.GoblinGulgEntity;
import com.emoniph.witchery.entity.GoblinMogEntity;
import com.emoniph.witchery.entity.HobgoblinEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.init.WitcheryEntities;
import com.emoniph.witchery.init.WitcheryItems;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server progression coverage for the bound Patron and paired Goblin God summon. */
public final class PatronStatueGameTests {
    private static final UUID OWNER_ID = UUID.fromString("74eb2127-5417-457a-a809-1f99415949ed");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LIFECYCLE =
            TEST_FUNCTIONS.register("patron_statue_lifecycle", () -> PatronStatueGameTests::lifecycle);

    private PatronStatueGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(PatronStatueGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "patron_statue"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 200, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "patron_statue_lifecycle"),
                new FunctionGameTestInstance(LIFECYCLE.getKey(), data));
    }

    private static void lifecycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixture = helper.absolutePos(new BlockPos(2, 0, 2));
        int y = Math.clamp(fixture.getY() + 100, level.getMinY() + 20, level.getMaxY() - 20);
        BlockPos statuePos = new BlockPos(fixture.getX(), y, fixture.getZ());
        buildArena(level, statuePos);
        StatueHobgoblinPatronBlock block = WitcheryBlocks.STATUE_HOBGOBLIN_PATRON.get();
        level.setBlock(statuePos, block.defaultBlockState(), Block.UPDATE_ALL);
        StatueHobgoblinPatronBlockEntity statue = (StatueHobgoblinPatronBlockEntity) level.getBlockEntity(statuePos);
        helper.assertTrue(statue != null, "The Patron block must create its block entity");
        if (statue == null) {
            throw new IllegalStateException("Missing Patron block entity");
        }

        ServerPlayer owner = createTestPlayer(level, statuePos);
        statue.setOwner(owner.getScoreboardName(), owner.getUUID());

        BlockPos otherStatuePos = statuePos.east(3);
        level.setBlock(otherStatuePos, block.defaultBlockState(), Block.UPDATE_ALL);
        StatueHobgoblinPatronBlockEntity otherStatue =
                (StatueHobgoblinPatronBlockEntity) level.getBlockEntity(otherStatuePos);
        helper.assertTrue(otherStatue != null, "The second Patron block must create its block entity");
        if (otherStatue == null) {
            throw new IllegalStateException("Missing second Patron block entity");
        }
        otherStatue.setOwner(owner.getScoreboardName(), owner.getUUID());
        List<HobgoblinEntity> otherWorshippers = worshippers(level, otherStatuePos, 5);
        ItemStack wrongStatueStar = new ItemStack(Items.NETHER_STAR);
        interact(block, level, statuePos, owner, wrongStatueStar);
        helper.assertTrue(wrongStatueStar.getCount() == 1,
                "Hobgoblins worshipping a different Patron must not pay this statue's summon cost");
        helper.assertTrue(otherWorshippers.stream().allMatch(Entity::isAlive),
                "A refused cross-statue summon must not sacrifice worshippers");
        otherWorshippers.forEach(Entity::discard);
        level.removeBlock(otherStatuePos, false);

        List<HobgoblinEntity> firstWorshippers = worshippers(level, statuePos, 5);

        ItemStack star = new ItemStack(Items.NETHER_STAR);
        interact(block, level, statuePos, owner, star);
        AABB arena = new AABB(statuePos).inflate(20.0D);
        List<GoblinMogEntity> mogs = level.getEntitiesOfClass(GoblinMogEntity.class, arena);
        List<GoblinGulgEntity> gulgs = level.getEntitiesOfClass(GoblinGulgEntity.class, arena);
        helper.assertTrue(star.isEmpty(), "A successful manual summon must consume one Nether Star");
        helper.assertTrue(firstWorshippers.stream().allMatch(Entity::isRemoved),
                "The five active worshippers must be sacrificed after a successful summon");
        helper.assertTrue(mogs.size() == 1 && gulgs.size() == 1,
                "The Patron must summon exactly one Mog and one Gulg together");
        helper.assertTrue(mogs.getFirst().getTarget() == owner && gulgs.getFirst().getTarget() == owner,
                "Both summoned gods must immediately target the bound owner");
        helper.assertTrue(mogs.getFirst().isPersistenceRequired() && gulgs.getFirst().isPersistenceRequired(),
                "Both summoned gods must be persistent");

        List<HobgoblinEntity> secondWorshippers = worshippers(level, statuePos, 5);
        ItemStack refusedStar = new ItemStack(Items.NETHER_STAR);
        interact(block, level, statuePos, owner, refusedStar);
        helper.assertTrue(refusedStar.getCount() == 1,
                "An existing Goblin God must prevent consuming another Nether Star");
        helper.assertTrue(secondWorshippers.stream().allMatch(Entity::isAlive),
                "A refused duplicate summon must not sacrifice worshippers");

        assertBoundItemRoundTrip(helper, level, block, statue, owner, statuePos.east(10));
        assertSequentialTrades(helper, level, statuePos.east(11));

        cleanup(level, statuePos, owner);
        helper.succeed();
    }

    private static void assertBoundItemRoundTrip(GameTestHelper helper, ServerLevel level,
                                                 StatueHobgoblinPatronBlock block,
                                                 StatueHobgoblinPatronBlockEntity source,
                                                 ServerPlayer owner, BlockPos destination) {
        ItemStack boundStatue = source.createDropStack(block);
        level.setBlock(destination, block.defaultBlockState(), Block.UPDATE_ALL);
        block.setPlacedBy(level, destination, level.getBlockState(destination), owner, boundStatue);
        StatueHobgoblinPatronBlockEntity restored =
                (StatueHobgoblinPatronBlockEntity) level.getBlockEntity(destination);
        helper.assertTrue(restored != null && restored.isOwner(owner)
                        && owner.getUUID().equals(restored.ownerUuid()),
                "Breaking and replacing a Patron must retain its owner name and UUID");
    }

    private static void assertSequentialTrades(GameTestHelper helper, ServerLevel level, BlockPos pos) {
        HobgoblinEntity merchant = createHobgoblin(level, pos);
        List<net.minecraft.world.item.Item> expectedResults = List.of(
                WitcheryItems.KOBOLDITE_DUST.get(), WitcheryItems.KOBOLDITE_NUGGET.get(),
                WitcheryItems.KOBOLDITE_NUGGET.get(), WitcheryItems.KOBOLDITE_INGOT.get(), Items.EMERALD);
        for (int visible = 1; visible <= expectedResults.size(); visible++) {
            var offers = merchant.getOffers();
            helper.assertTrue(offers.size() == visible,
                    "Completing the last Hobgoblin trade must unlock exactly one new offer");
            helper.assertTrue(offers.getLast().getResult().is(expectedResults.get(visible - 1)),
                    "The staged Hobgoblin trade result order must remain stable");
            merchant.notifyTrade(offers.getLast());
        }
        helper.assertTrue(merchant.getOffers().size() == 5,
                "The practical port trade chain must stop after five offers");
    }

    private static List<HobgoblinEntity> worshippers(ServerLevel level, BlockPos statuePos, int count) {
        List<HobgoblinEntity> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BlockPos pos = statuePos.offset(2 + i % 3, 0, 2 + i / 3);
            HobgoblinEntity entity = createHobgoblin(level, pos);
            entity.setNoAi(true);
            entity.beginPatronWorship(statuePos);
            entities.add(entity);
        }
        return entities;
    }

    private static HobgoblinEntity createHobgoblin(ServerLevel level, BlockPos pos) {
        HobgoblinEntity entity = WitcheryEntities.HOBGOBLIN.get().create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) {
            throw new IllegalStateException("Unable to create Hobgoblin test entity");
        }
        entity.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(entity);
        return entity;
    }

    private static void interact(StatueHobgoblinPatronBlock block, ServerLevel level, BlockPos pos,
                                 ServerPlayer player, ItemStack stack) {
        block.useItemOn(stack, level.getBlockState(pos), level, pos, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static void buildArena(ServerLevel level, BlockPos statuePos) {
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                BlockPos floor = statuePos.offset(x, -1, z);
                level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                for (int dy = 0; dy <= 4; dy++) {
                    level.setBlock(floor.above(1 + dy), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer player = new FakePlayer(level, new GameProfile(OWNER_ID, "patron-owner"));
        if (player.connection.getConnection().channel() == null) {
            new EmbeddedChannel(player.connection.getConnection());
        }
        player.snapTo(pos.getX() + 1.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addNewPlayer(player);
        return player;
    }

    private static void cleanup(ServerLevel level, BlockPos center, ServerPlayer owner) {
        for (Entity entity : level.getEntitiesOfClass(Entity.class, new AABB(center).inflate(24.0D))) {
            if (entity != owner) {
                entity.discard();
            }
        }
        if (!owner.isRemoved()) {
            level.removePlayerImmediately(owner, Entity.RemovalReason.DISCARDED);
        }
    }
}
