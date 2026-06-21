package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = WitcheryMod.MODID)
public final class FlameImpContractEffects {
    private static final String EVAPORATION_UNTIL = "witchery.imp.evaporation_until";
    private static final String FIERY_TOUCH_UNTIL = "witchery.imp.firetouch_until";
    private static final String MELTING_TOUCH_UNTIL = "witchery.imp.meltingtouch_until";

    private FlameImpContractEffects() {
    }

    public static void applyEvaporation(ServerPlayer player, int durationTicks) {
        apply(player, EVAPORATION_UNTIL, durationTicks, "witchery.imp_contract.evaporation");
    }

    public static void applyFieryTouch(ServerPlayer player, int durationTicks) {
        apply(player, FIERY_TOUCH_UNTIL, durationTicks, "witchery.imp_contract.fiery_touch");
    }

    public static void applyMeltingTouch(ServerPlayer player, int durationTicks) {
        apply(player, MELTING_TOUCH_UNTIL, durationTicks, "witchery.imp_contract.melting_touch");
    }

    private static void apply(ServerPlayer player, String key, int durationTicks, String messageKey) {
        player.getPersistentData().putLong(key, player.level().getGameTime() + durationTicks);
        player.displayClientMessage(Component.translatable(messageKey).withStyle(ChatFormatting.DARK_RED), true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 3) {
            return;
        }

        clearExpired(player, EVAPORATION_UNTIL);
        clearExpired(player, FIERY_TOUCH_UNTIL);
        clearExpired(player, MELTING_TOUCH_UNTIL);

        if (active(player, EVAPORATION_UNTIL) && player.level() instanceof ServerLevel level
                && level.random.nextInt(5) == 0) {
            evaporateNearbyWater(player, level);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        placeFieryTouch(event.getEntity(), event.getPos(), event.getFace(), false);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        placeFieryTouch(event.getEntity(), event.getPos(), event.getFace(), true);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || !active(player, MELTING_TOUCH_UNTIL)) {
            return;
        }
        ServerLevel level = event.getLevel();
        List<ItemEntity> replacements = new ArrayList<>();
        boolean changed = false;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack smelted = smelt(level, drop.getItem());
            if (smelted.isEmpty()) {
                replacements.add(drop);
                continue;
            }
            Vec3 pos = drop.position();
            replacements.add(new ItemEntity(level, pos.x, pos.y, pos.z, smelted));
            changed = true;
        }
        if (changed) {
            event.getDrops().clear();
            event.getDrops().addAll(replacements);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAll(player);
        }
    }

    private static void placeFieryTouch(net.minecraft.world.entity.player.Player player, BlockPos clicked,
                                        Direction face, boolean randomizeHorizontal) {
        if (!(player instanceof ServerPlayer serverPlayer) || !active(serverPlayer, FIERY_TOUCH_UNTIL)
                || !(player.level() instanceof ServerLevel level) || face == null) {
            return;
        }
        if (level.random.nextDouble() >= 0.2D) {
            return;
        }
        if (level.getBlockState(clicked).isAir()) {
            return;
        }

        BlockPos placePos = clicked.relative(face);
        if (randomizeHorizontal) {
            placePos = placePos.offset(level.random.nextInt(3) - 1, 0, level.random.nextInt(3) - 1);
        }
        if (!level.getBlockState(placePos).isAir() || level.getBlockState(placePos.below()).isAir()) {
            return;
        }
        BlockState fire = Blocks.FIRE.defaultBlockState();
        if (!fire.canSurvive(level, placePos)) {
            return;
        }
        level.setBlockAndUpdate(placePos, fire);
        level.playSound(null, placePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                level.random.nextFloat() * 0.4F + 0.8F);
    }

    private static void evaporateNearbyWater(ServerPlayer player, ServerLevel level) {
        BlockPos center = player.blockPosition();
        boolean found = false;
        for (int x = center.getX() - 3; x <= center.getX() + 3; x++) {
            for (int z = center.getZ() - 3; z <= center.getZ() + 3; z++) {
                for (int y = center.getY() + 2; y >= center.getY() - 1; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 9.0D) {
                        continue;
                    }
                    if (!level.getBlockState(pos).is(Blocks.WATER) || !level.getBlockState(pos.above()).isAir()) {
                        continue;
                    }
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                    found = true;
                }
            }
        }
        if (found) {
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F,
                    2.2F + (level.random.nextFloat() - level.random.nextFloat()) * 0.5F);
        }
    }

    private static ItemStack smelt(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack singleInput = stack.copyWithCount(1);
        Optional<RecipeHolder<SmeltingRecipe>> recipe =
                level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(singleInput), level);
        if (recipe.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(singleInput), level.registryAccess());
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack smelted = result.copy();
        smelted.setCount(result.getCount() * stack.getCount());
        if (level.random.nextDouble() < 0.25D) {
            smelted.grow(1);
        }
        return smelted;
    }

    private static boolean active(ServerPlayer player, String key) {
        return player.getPersistentData().getLongOr(key, 0L) > player.level().getGameTime();
    }

    private static void clearExpired(ServerPlayer player, String key) {
        long expires = player.getPersistentData().getLongOr(key, 0L);
        if (expires > 0L && expires <= player.level().getGameTime()) {
            player.getPersistentData().remove(key);
        }
    }

    private static void clearAll(ServerPlayer player) {
        player.getPersistentData().remove(EVAPORATION_UNTIL);
        player.getPersistentData().remove(FIERY_TOUCH_UNTIL);
        player.getPersistentData().remove(MELTING_TOUCH_UNTIL);
    }
}
