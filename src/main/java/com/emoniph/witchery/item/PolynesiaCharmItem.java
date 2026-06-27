package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.CovenWitchEntity;
import com.emoniph.witchery.entity.DemonEntity;
import com.emoniph.witchery.entity.FlameImpEntity;
import com.emoniph.witchery.entity.familiar.FamiliarEntity;
import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class PolynesiaCharmItem extends Item {
    private static final String STOCK_INVENTORY_TAG = "WitcheryShopStock";
    private static final String NO_DROPS_TAG = "WITCNoDrops";
    private static final double RANGE = 5.0D;
    private static final int SOURCE_DURABILITY = 50;
    private static final int MAX_MERCHANT_NAME = 30;
    private static final int TRADE_XP = 0;
    private static final float PRICE_MULTIPLIER = 0.05F;

    private final boolean charmsDemons;

    public PolynesiaCharmItem(Properties properties) {
        this(properties, false);
    }

    public PolynesiaCharmItem(Properties properties, boolean charmsDemons) {
        super(properties.stacksTo(1).durability(SOURCE_DURABILITY).rarity(Rarity.UNCOMMON));
        this.charmsDemons = charmsDemons;
    }

    public boolean charmsDemons() {
        return charmsDemons;
    }

    public static boolean hasStockInventory(LivingEntity entity) {
        return entity.getPersistentData().contains(STOCK_INVENTORY_TAG);
    }

    public static void setEmptyStockInventory(Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            entity.getPersistentData().put(STOCK_INVENTORY_TAG, new CompoundTag());
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        LivingEntity target = findLookedAtLiving(player, RANGE);
        if (target == null || !tryOpenAnimalMerchant(player, target, player.getItemInHand(hand), hand)) {
            fail(player);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!tryOpenAnimalMerchant(player, target, stack, hand)) {
            fail(player);
        }
        return InteractionResult.CONSUME;
    }

    private static boolean tryOpenAnimalMerchant(Player player, LivingEntity target, ItemStack stack, InteractionHand hand) {
        if (target instanceof DemonEntity demon) {
            return stack.getItem() instanceof PolynesiaCharmItem charm
                    && charm.charmsDemons()
                    && demon.openDevilsTongueTrade(player, hand, stack);
        }
        if (!canCharm(player, target)) {
            return false;
        }

        AnimalMerchant merchant = new AnimalMerchant(target);
        merchant.setTradingPlayer(player);
        merchant.openTradingScreen(player, merchantDisplayName(target), 1);
        player.awardStat(Stats.TALKED_TO_VILLAGER);
        playGreeting(target);

        if (!player.getAbilities().instabuild) {
            EquipmentSlot slot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
            stack.hurtAndBreak(1, player, slot);
        }
        return true;
    }

    private static boolean canCharm(Player player, LivingEntity target) {
        if (!target.isAlive() || target.isBaby()) {
            return false;
        }
        if (target instanceof Mob mob && mob.getTarget() != null) {
            return false;
        }
        if (target instanceof FamiliarEntity
                || target instanceof CovenWitchEntity
                || target instanceof FlameImpEntity) {
            return false;
        }
        if (target instanceof Bat && target.getPersistentData().getBooleanOr(NO_DROPS_TAG, false)) {
            return false;
        }

        if (target instanceof Animal
                || target instanceof AmbientCreature
                || target instanceof Spider
                || target instanceof Squid) {
            return true;
        }
        if (target instanceof Creeper) {
            return WitcheryItems.isWitchesRobe(player.getItemBySlot(EquipmentSlot.CHEST));
        }
        if (target.getType().is(EntityTypeTags.UNDEAD)) {
            return player.getItemBySlot(EquipmentSlot.CHEST).is(WitcheryItems.NECROMANCER_ROBES.get());
        }
        return false;
    }

    @Nullable
    private static LivingEntity findLookedAtLiving(Player player, double range) {
        HitResult blockHit = player.pick(range, 1.0F, true);
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 reach = player.getViewVector(1.0F).scale(range);
        AABB searchBox = player.getBoundingBox().expandTowards(reach).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(reach), searchBox,
                entity -> entity != player && entity.isPickable() && entity instanceof LivingEntity, range * range);
        if (entityHit == null || !(entityHit.getEntity() instanceof LivingEntity living)) {
            return null;
        }
        if (blockHit.getType() != HitResult.Type.MISS
                && blockHit.getLocation().distanceToSqr(eye) <= entityHit.getLocation().distanceToSqr(eye)) {
            return null;
        }
        return living;
    }

    private static Component merchantDisplayName(LivingEntity target) {
        String name = target.getDisplayName().getString();
        if (name.length() > MAX_MERCHANT_NAME) {
            name = name.substring(0, MAX_MERCHANT_NAME);
        }
        return Component.literal(name);
    }

    private static void fail(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.PLAYERS, 0.45F, 0.6F);
        level.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                8, 0.25D, 0.35D, 0.25D, 0.01D);
    }

    private static void playGreeting(LivingEntity target) {
        if (target instanceof Mob mob) {
            mob.playAmbientSound();
            mob.playAmbientSound();
            mob.playAmbientSound();
        }
    }

    private static final class AnimalMerchant implements Merchant {
        private final LivingEntity animal;
        @Nullable
        private Player tradingPlayer;
        @Nullable
        private MerchantOffers offers;

        private AnimalMerchant(LivingEntity animal) {
            this.animal = animal;
        }

        @Override
        public void setTradingPlayer(@Nullable Player tradingPlayer) {
            this.tradingPlayer = tradingPlayer;
        }

        @Nullable
        @Override
        public Player getTradingPlayer() {
            return tradingPlayer;
        }

        @Override
        public MerchantOffers getOffers() {
            if (offers == null) {
                offers = loadOrCreateOffers(animal);
            }
            return offers;
        }

        @Override
        public void overrideOffers(MerchantOffers offers) {
            this.offers = offers;
            saveOffers(animal, offers);
        }

        @Override
        public void notifyTrade(MerchantOffer offer) {
            offer.increaseUses();
            saveOffers(animal, getOffers());
            playGreeting(animal);
        }

        @Override
        public void notifyTradeUpdated(ItemStack stack) {
            if (!stack.isEmpty()) {
                playGreeting(animal);
            }
        }

        @Override
        public int getVillagerXp() {
            return 0;
        }

        @Override
        public void overrideXp(int xp) {
        }

        @Override
        public boolean showProgressBar() {
            return false;
        }

        @Override
        public SoundEvent getNotifyTradeSound() {
            return SoundEvents.VILLAGER_YES;
        }

        @Override
        public boolean stillValid(Player player) {
            return animal.isAlive() && animal.distanceToSqr(player) <= 64.0D;
        }

        @Override
        public boolean isClientSide() {
            return animal.level().isClientSide();
        }
    }

    private static MerchantOffers loadOrCreateOffers(LivingEntity animal) {
        CompoundTag data = animal.getPersistentData();
        if (data.contains(STOCK_INVENTORY_TAG)) {
            Tag stored = data.get(STOCK_INVENTORY_TAG);
            if (stored instanceof CompoundTag compound && compound.isEmpty()) {
                return new MerchantOffers();
            }
            return MerchantOffers.CODEC.parse(NbtOps.INSTANCE, stored)
                    .result()
                    .orElseGet(MerchantOffers::new);
        }

        MerchantOffers generated = createOffers(animal);
        saveOffers(animal, generated);
        return generated;
    }

    private static void saveOffers(LivingEntity animal, MerchantOffers offers) {
        MerchantOffers.CODEC.encodeStart(NbtOps.INSTANCE, offers)
                .result()
                .ifPresent(tag -> animal.getPersistentData().put(STOCK_INVENTORY_TAG, tag));
    }

    private static MerchantOffers createOffers(LivingEntity animal) {
        RandomSource random = animal.getRandom();
        List<ItemStack> goods = new ArrayList<>();
        List<ItemLike> currencies = new ArrayList<>();

        goods.add(randomGenericGood(random));
        if (random.nextInt(100) < 3) {
            goods.add(new ItemStack(WitcheryItems.TREEFYD_SEEDS.get()));
        }
        populateAnimalStock(animal, goods, currencies, random);
        if (currencies.isEmpty()) {
            populateDefaultCurrencies(currencies);
        }

        shuffle(goods, random);
        MerchantOffers offers = new MerchantOffers();
        int count = Math.min(goods.size(), 1 + random.nextInt(2));
        for (int i = 0; i < count; i++) {
            ItemStack template = goods.get(i);
            boolean premium = isPremiumGood(template, animal);
            ItemStack result = randomizeGoodCount(template, random, premium);
            ItemLike currency = currencies.get(random.nextInt(currencies.size()));
            int cost = randomCost(currency, result, premium, random);
            offers.add(new MerchantOffer(new ItemCost(currency, cost), result,
                    2 + random.nextInt(2), TRADE_XP, PRICE_MULTIPLIER));
        }
        return offers;
    }

    private static ItemStack randomGenericGood(RandomSource random) {
        return switch (random.nextInt(16)) {
            case 0 -> new ItemStack(WitcheryItems.MANDRAKE_ROOT.get(), 3);
            case 1 -> new ItemStack(WitcheryItems.BELLADONNA.get(), 3);
            case 2 -> new ItemStack(WitcheryItems.ARTICHOKE.get(), 3);
            case 3 -> new ItemStack(Blocks.OAK_SAPLING, 4);
            case 4 -> new ItemStack(Blocks.SPRUCE_SAPLING, 4);
            case 5 -> new ItemStack(Blocks.BIRCH_SAPLING, 4);
            case 6 -> new ItemStack(Blocks.JUNGLE_SAPLING, 4);
            case 7 -> new ItemStack(Blocks.RED_MUSHROOM, 2);
            case 8 -> new ItemStack(Blocks.BROWN_MUSHROOM, 2);
            case 9 -> new ItemStack(Items.APPLE, 5);
            case 10 -> new ItemStack(Items.IRON_INGOT, 2);
            case 11 -> new ItemStack(Items.WHEAT_SEEDS, 4);
            case 12 -> new ItemStack(Items.SLIME_BALL, 5);
            case 13 -> new ItemStack(WitcheryItems.TONGUE_OF_DOG.get(), 2);
            case 14 -> new ItemStack(Items.BONE, 5);
            default -> random.nextBoolean()
                    ? new ItemStack(Items.SPIDER_EYE, 2)
                    : new ItemStack(Items.ROTTEN_FLESH, 5);
        };
    }

    private static void populateAnimalStock(LivingEntity animal, List<ItemStack> goods,
                                            List<ItemLike> currencies, RandomSource random) {
        if (animal instanceof Pig) {
            addCurrencies(currencies, Items.ROTTEN_FLESH, Items.GOLD_NUGGET, Items.BONE);
            addGoods(goods, new ItemStack(Items.CARROT, 5), new ItemStack(Items.POTATO, 5));
            addRareTack(goods, random, true, false);
        } else if (animal instanceof AbstractHorse) {
            addCurrencies(currencies, Items.ROTTEN_FLESH, Items.GOLD_NUGGET, Items.LEATHER);
            addRareTack(goods, random, false, true);
        } else if (animal instanceof Wolf) {
            addCurrencies(currencies, Items.GUNPOWDER, Items.SLIME_BALL, Items.STRING);
            goods.add(new ItemStack(Items.WHEAT_SEEDS, 5));
            addRareTack(goods, random, true, false);
        } else if (animal instanceof Cat || animal instanceof Ocelot) {
            addCurrencies(currencies, Items.COD, Items.SUGAR);
        } else if (animal instanceof MushroomCow) {
            addCurrencies(currencies, Items.CARROT, Items.POTATO);
        } else if (animal instanceof Cow) {
            currencies.add(Items.LEATHER);
        } else if (animal instanceof Chicken) {
            currencies.add(Items.FEATHER);
            addGoods(goods, new ItemStack(Items.EGG, 10), new ItemStack(Items.CHICKEN, 5));
        } else if (animal instanceof Turtle) {
            addCurrencies(currencies, Items.SEAGRASS, Items.KELP, Items.COD);
            addGoods(goods,
                    new ItemStack(Items.TURTLE_EGG, 2),
                    new ItemStack(Items.TURTLE_SCUTE, 3),
                    new ItemStack(Items.TURTLE_HELMET));
        } else if (animal instanceof Sheep) {
            currencies.add(Items.LEATHER);
        } else if (animal instanceof Squid) {
            currencies.add(Items.SUGAR);
            goods.add(new ItemStack(Items.INK_SAC, 10));
        } else if (animal instanceof Bat) {
            addCurrencies(currencies, Items.FEATHER, Items.LEATHER, Items.GUNPOWDER, Items.SLIME_BALL);
            goods.add(new ItemStack(WitcheryItems.BAT_WOOL.get(), 5));
        } else if (animal instanceof Spider) {
            addCurrencies(currencies, Items.GUNPOWDER, Items.SLIME_BALL, Items.STRING, Items.SUGAR);
            addGoods(goods, new ItemStack(Items.SPIDER_EYE, 8), new ItemStack(Blocks.COBWEB, 4));
        } else if (animal instanceof Creeper) {
            addCurrencies(currencies, Items.GUNPOWDER, Items.SUGAR);
            if (random.nextInt(100) < 5) {
                goods.add(new ItemStack(WitcheryItems.SPECTRAL_DUST.get(), 2));
            }
            if (random.nextInt(100) < 10) {
                goods.add(new ItemStack(WitcheryItems.TREEFYD_SEEDS.get()));
            }
            if (random.nextInt(100) < 2) {
                goods.add(new ItemStack(WitcheryItems.CREEPER_HEART.get()));
            }
        } else if (animal.getType().is(EntityTypeTags.UNDEAD)) {
            currencies.add(Items.WHEAT_SEEDS);
            goods.add(new ItemStack(WitcheryItems.SPECTRAL_DUST.get()));
        } else {
            populateDefaultCurrencies(currencies);
        }
    }

    private static void populateDefaultCurrencies(List<ItemLike> currencies) {
        addCurrencies(currencies, Items.GUNPOWDER, Items.SLIME_BALL, Items.STRING, Items.SUGAR,
                Items.LEATHER, Items.FEATHER, Items.ROTTEN_FLESH, Items.GOLD_NUGGET, Items.BONE);
    }

    private static void addCurrencies(List<ItemLike> currencies, ItemLike... items) {
        Collections.addAll(currencies, items);
    }

    private static void addGoods(List<ItemStack> goods, ItemStack... stacks) {
        Collections.addAll(goods, stacks);
    }

    private static void addRareTack(List<ItemStack> goods, RandomSource random,
                                    boolean saddleAndIron, boolean goldHorseArmor) {
        if (saddleAndIron) {
            if (random.nextInt(100) < 2) {
                goods.add(new ItemStack(Items.SADDLE));
            }
            if (random.nextInt(100) < 1) {
                goods.add(new ItemStack(Items.IRON_HORSE_ARMOR));
            }
        }
        if (goldHorseArmor && random.nextInt(100) < 1) {
            goods.add(new ItemStack(Items.GOLDEN_HORSE_ARMOR));
        }
    }

    private static ItemStack randomizeGoodCount(ItemStack template, RandomSource random, boolean premium) {
        ItemStack result = template.copy();
        int max = Math.max(1, template.getCount());
        int minimum = max > 4 ? 3 : 1;
        int count = premium ? template.getCount() : Math.min(random.nextInt(max) + minimum, result.getMaxStackSize());
        result.setCount(Math.max(1, count));
        return result;
    }

    private static int randomCost(ItemLike currency, ItemStack result, boolean premium, RandomSource random) {
        int factor = result.getCount() > 4 ? 1 : 2;
        int multiplier = premium ? 2 : 1;
        int cost = random.nextInt(2) + result.getCount() * multiplier * (random.nextInt(2) + factor);
        return Math.max(1, Math.min(cost, currency.asItem().getDefaultMaxStackSize()));
    }

    private static boolean isPremiumGood(ItemStack stack, LivingEntity animal) {
        return stack.is(Items.SADDLE)
                || stack.is(Items.IRON_HORSE_ARMOR)
                || stack.is(Items.GOLDEN_HORSE_ARMOR)
                || stack.is(Items.TURTLE_HELMET)
                || stack.is(WitcheryItems.TREEFYD_SEEDS.get())
                || animal.getType().is(EntityTypeTags.UNDEAD);
    }

    private static <T> void shuffle(List<T> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            Collections.swap(list, i, random.nextInt(i + 1));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        String tooltipText = Component.translatable(getDescriptionId() + ".tip").getString();
        for (String line : tooltipText.split("\\n")) {
            if (!line.isEmpty()) {
                tooltip.accept(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
