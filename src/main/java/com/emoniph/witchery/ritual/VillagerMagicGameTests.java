package com.emoniph.witchery.ritual;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.events.PoppetEvents;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.PoppetItem;
import com.emoniph.witchery.item.TaglockKitItem;
import com.emoniph.witchery.item.VampiricPoppetItem;
import com.emoniph.witchery.item.VoodooPoppetItem;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Dedicated-server contract for villager Taglocks, poppets, curses, and rite sacrifices. */
public final class VillagerMagicGameTests {
    private static final UUID PLAYER_ID = UUID.fromString("6a53a810-4f99-40e1-9d61-13d9f5cd812f");
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, WitcheryMod.MODID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MAGIC =
            TEST_FUNCTIONS.register("villager_magic_and_sacrifices", () -> VillagerMagicGameTests::magic);

    private VillagerMagicGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_FUNCTIONS.register(modBus);
        modBus.addListener(VillagerMagicGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "villager_magic"),
                new TestEnvironmentDefinition.AllOf());
        TestData<Holder<TestEnvironmentDefinition>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), 140, 0,
                true, Rotation.NONE, true, 1, 1, false);
        event.registerTest(Identifier.fromNamespaceAndPath(WitcheryMod.MODID,
                        "villager_magic_and_sacrifices"),
                new FunctionGameTestInstance(MAGIC.getKey(), data));
    }

    private static void magic(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 2, 2));
        List<Entity> cleanup = new ArrayList<>();
        ServerPlayer player = createTestPlayer(level, origin);
        Villager target = villager(level, origin.offset(4, 0, 0), cleanup);

        try {
            ItemStack taglock = new ItemStack(WitcheryItems.TAGLOCK_KIT.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, taglock);
            ((TaglockKitItem) WitcheryItems.TAGLOCK_KIT.get()).interactLivingEntity(
                    taglock, player, target, InteractionHand.MAIN_HAND);
            taglock = player.getMainHandItem();
            helper.assertTrue(TaglockKitItem.getUUID(taglock).orElseThrow().equals(target.getUUID()),
                    "A creature Taglock must retain the villager UUID");
            helper.assertTrue(TaglockKitItem.getBoundEntityTypeId(taglock)
                            .filter(EntityType.VILLAGER.builtInRegistryHolder()::is).isPresent(),
                    "A creature Taglock must retain the villager entity type");

            ItemStack voodoo = new ItemStack(WitcheryItems.VOODOO_POPPET.get());
            PoppetItem.bindFromTaglock(voodoo, taglock);
            player.setItemInHand(InteractionHand.MAIN_HAND, voodoo);
            ((VoodooPoppetItem) WitcheryItems.VOODOO_POPPET.get()).use(level, player, InteractionHand.MAIN_HAND);
            ((VoodooPoppetItem) WitcheryItems.VOODOO_POPPET.get()).releaseUsing(voodoo, level, player, 60);
            helper.assertTrue(target.hasEffect(MobEffects.LEVITATION)
                            && target.getDeltaMovement().horizontalDistanceSqr() > 0.01D,
                    "A villager-bound Voodoo Poppet must throw and levitate the villager");
            target.removeEffect(MobEffects.LEVITATION);
            target.setDeltaMovement(0.0D, 0.0D, 0.0D);

            ItemStack vampiric = new ItemStack(WitcheryItems.VAMPIRIC_POPPET.get());
            PoppetItem.bind(vampiric, player);
            VampiricPoppetItem.bindVictim(vampiric, target);
            player.getInventory().add(vampiric);
            float targetHealth = target.getHealth();
            LivingDamageEvent.Pre damageEvent = new LivingDamageEvent.Pre(player,
                    new DamageContainer(level.damageSources().magic(), 4.0F));
            PoppetEvents.onVampiricRedirect(damageEvent);
            helper.assertTrue(Math.abs(damageEvent.getNewDamage()) < 0.01F
                            && target.getHealth() < targetHealth,
                    "A Vampiric Poppet must redirect player damage into its villager victim");

            helper.assertTrue(RitualExecutor.resolveCreatureCurseTarget(level, taglock)
                            .filter(entity -> entity == target).isPresent(),
                    "Curse Creature must resolve a living villager from a creature Taglock");
            RitualExecutor.applyCreatureCurseEffects(target, 1);
            helper.assertTrue(target.hasEffect(MobEffects.WITHER)
                            && target.hasEffect(MobEffects.SLOWNESS)
                            && target.hasEffect(MobEffects.WEAKNESS),
                    "Curse Creature must apply all three effects to a villager target");

            verifyVillagerSacrifices(helper, level, origin.offset(20, 0, 0), cleanup);
            helper.succeed();
        } finally {
            cleanup.forEach(Entity::discard);
            removeTestPlayer(player);
        }
    }

    private static void verifyVillagerSacrifices(GameTestHelper helper, ServerLevel level,
                                                 BlockPos base, List<Entity> cleanup) {
        Villager witherVictim = villager(level, base, cleanup);
        drop(level, base, new ItemStack(WitcheryItems.DIAMOND_VAPOUR.get()), cleanup);
        drop(level, base, new ItemStack(Items.ENDER_PEARL), cleanup);
        helper.assertTrue(RitualOfferings.findSummonWitherOfferings(level, base)
                        .filter(found -> found.villager() == witherVictim).isPresent(),
                "The Wither rite must select its nearby villager sacrifice");

        BlockPos demonCenter = base.offset(20, 0, 0);
        Villager demonVictim = villager(level, demonCenter, cleanup);
        drop(level, demonCenter, new ItemStack(WitcheryItems.REFINED_EVIL.get()), cleanup);
        drop(level, demonCenter, new ItemStack(Items.BLAZE_POWDER), cleanup);
        drop(level, demonCenter, new ItemStack(Items.ENDER_PEARL), cleanup);
        helper.assertTrue(RitualOfferings.findSummonDemonOfferings(level, demonCenter)
                        .filter(found -> found.villager() == demonVictim).isPresent(),
                "The Demon rite must select its nearby villager sacrifice");

        BlockPos hellCenter = base.offset(40, 0, 0);
        Villager hellVictim = villager(level, hellCenter, cleanup);
        drop(level, hellCenter, new ItemStack(WitcheryItems.REDSTONE_SOUP.get()), cleanup);
        drop(level, hellCenter, new ItemStack(WitcheryItems.DEMON_HEART.get()), cleanup);
        drop(level, hellCenter, new ItemStack(WitcheryItems.WAYSTONE.get()), cleanup);
        drop(level, hellCenter, new ItemStack(Items.NETHER_STAR), cleanup);
        helper.assertTrue(RitualOfferings.findHellOnEarthOfferings(level, hellCenter)
                        .filter(found -> found.villager() == hellVictim).isPresent(),
                "Hell on Earth must select its nearby villager sacrifice");
    }

    private static Villager villager(ServerLevel level, BlockPos pos, List<Entity> cleanup) {
        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.TRIGGERED);
        if (villager == null) throw new IllegalStateException("Unable to create villager fixture");
        villager.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addFreshEntity(villager);
        cleanup.add(villager);
        return villager;
    }

    private static ItemEntity drop(ServerLevel level, BlockPos pos, ItemStack stack, List<Entity> cleanup) {
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, stack);
        level.addFreshEntity(item);
        cleanup.add(item);
        return item;
    }

    private static ServerPlayer createTestPlayer(ServerLevel level, BlockPos pos) {
        ServerPlayer player = new FakePlayer(level, new GameProfile(PLAYER_ID, "villager-magic"));
        if (player.connection.getConnection().channel() == null) {
            new EmbeddedChannel(player.connection.getConnection());
        }
        player.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        level.addNewPlayer(player);
        return player;
    }

    private static void removeTestPlayer(ServerPlayer player) {
        if (player != null && player.level() instanceof ServerLevel level && !player.isRemoved()) {
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }
}
