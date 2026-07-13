package com.emoniph.witchery.item;

import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class VampireBloodItem extends Item {
    public enum Kind {
        WARM,
        LILITH
    }

    private static final int FULL_BLOOD = 500;
    private static final int STARTING_VAMPIRE_BLOOD = 250;

    private final Kind kind;

    public VampireBloodItem(Kind kind, Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .nutrition(0)
                .saturationModifier(0.0F)
                .alwaysEdible()
                .build()));
        this.kind = kind;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, net.minecraft.world.level.Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!(entity instanceof ServerPlayer player)) {
            return result;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (kind == Kind.WARM) {
            drinkWarmBlood(player, data);
        } else {
            drinkLilithsBlood(player, data);
        }
        return result;
    }

    private static void drinkWarmBlood(ServerPlayer player, WitcheryPlayerData data) {
        if (data.isVampire()) {
            data.addBlood(FULL_BLOOD);
            sync(player, data);
            player.displayClientMessage(Component.translatable("witchery.vampire.warm_blood.feed",
                    data.getBloodLevel()).withStyle(ChatFormatting.DARK_RED), false);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0, true, true));
            player.displayClientMessage(Component.translatable("witchery.vampire.warm_blood.poison")
                    .withStyle(ChatFormatting.DARK_GREEN), false);
        }
        playDrink(player);
    }

    private static void drinkLilithsBlood(ServerPlayer player, WitcheryPlayerData data) {
        if (!data.isVampire()) {
            if (data.isWerewolf()) {
                player.displayClientMessage(Component.translatable("rite.witchery.wolf_curse.hybridsnotallow")
                        .withStyle(ChatFormatting.DARK_RED), false);
                playDrink(player);
                return;
            }
            data.setVampire(true);
            if (data.getBloodLevel() < STARTING_VAMPIRE_BLOOD) {
                data.setBloodLevel(STARTING_VAMPIRE_BLOOD);
            }
            sync(player, data);
            player.displayClientMessage(Component.translatable("witchery.vampire.liliths_blood.transformed")
                    .withStyle(ChatFormatting.DARK_RED), false);
        } else {
            data.addBlood(FULL_BLOOD);
            sync(player, data);
            player.displayClientMessage(Component.translatable("witchery.vampire.liliths_blood.feed",
                    data.getBloodLevel()).withStyle(ChatFormatting.DARK_RED), false);
        }
        playDrink(player);
    }

    private static void sync(ServerPlayer player, WitcheryPlayerData data) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, SyncPlayerDataPayload.from(player, data));
    }

    private static void playDrink(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), WitcherySoundEvents.RANDOM_DRINK.get(),
                SoundSource.PLAYERS, 0.5F, 1.0F);
    }
}
