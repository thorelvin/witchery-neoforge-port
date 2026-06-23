package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class DeathsHandItem extends Item {
    public static final String TAG_SCYTHE_DEPLOYED = "WITCScytheDeployed";

    private final boolean deployedVariant;

    public DeathsHandItem(Properties properties) {
        this(properties, false);
    }

    public DeathsHandItem(Properties properties, boolean deployedVariant) {
        super(properties);
        this.deployedVariant = deployedVariant;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!DeathsClothingItem.isFullSetWorn(player)) {
            if (isDeployed(stack)) {
                if (level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }
                playToggleEffects(level, player, false);
                return InteractionResult.SUCCESS.heldItemTransformedTo(normalCopy(stack));
            }
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        boolean deployed = !isDeployed(stack);
        ItemStack transformed = stack.transmuteCopy(WitcheryItems.DEATHS_HAND.get(), stack.getCount());
        setDeployed(transformed, deployed);

        playToggleEffects(level, player, deployed);

        return InteractionResult.SUCCESS_SERVER.heldItemTransformedTo(transformed);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!isDeployed(stack) || !(entity instanceof Player player)) {
            return;
        }

        if (level.getGameTime() % 20L != 0L) {
            return;
        }

        if (!DeathsClothingItem.isFullSetWorn(player)) {
            if (isSourceDeployedItem(stack)) {
                player.setItemSlot(slot, normalCopy(stack));
            } else {
                setDeployed(stack, false);
                player.containerMenu.broadcastChanges();
            }
            return;
        }

        int foodLevel = player.getFoodData().getFoodLevel();
        if (foodLevel > 0) {
            player.getFoodData().eat(foodLevel == 1 ? -1 : -2, 0.0F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.deaths_hand.tip").withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable(isDeployed(stack)
                ? "item.witchery.deaths_hand.state.deployed"
                : "item.witchery.deaths_hand.state.sheathed").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static boolean isDeployed(ItemStack stack) {
        if (isSourceDeployedItem(stack)) {
            return true;
        }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getBooleanOr(TAG_SCYTHE_DEPLOYED, false);
    }

    public static boolean isSourceDeployedItem(ItemStack stack) {
        return stack.getItem() instanceof DeathsHandItem hand && hand.deployedVariant;
    }

    private static ItemStack normalCopy(ItemStack stack) {
        ItemStack normal = stack.transmuteCopy(WitcheryItems.DEATHS_HAND.get(), stack.getCount());
        setDeployed(normal, false);
        return normal;
    }

    public static void setDeployed(ItemStack stack, boolean deployed) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (deployed) {
                tag.putBoolean(TAG_SCYTHE_DEPLOYED, true);
            } else {
                tag.remove(TAG_SCYTHE_DEPLOYED);
            }
        });
    }

    private static void playToggleEffects(Level level, Player player, boolean deployed) {
        level.playSound(null, player.blockPosition(),
                deployed ? SoundEvents.ENDER_DRAGON_GROWL : SoundEvents.WITHER_SKELETON_STEP,
                SoundSource.PLAYERS, deployed ? 1.0F : 0.7F, deployed ? 2.0F : 0.6F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(deployed ? ParticleTypes.WITCH : ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    deployed ? 16 : 12, 0.45D, 0.75D, 0.45D, deployed ? 0.02D : 0.01D);
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    player.getX(), player.getY() + 0.9D, player.getZ(),
                    deployed ? 8 : 4, 0.25D, 0.45D, 0.25D, 0.01D);
        }
    }
}
