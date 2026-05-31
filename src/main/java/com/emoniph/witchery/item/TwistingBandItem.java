package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.function.Consumer;

public class TwistingBandItem extends Item {
    private static final double RANGE = 16.0D;
    private static final int EFFECT_TICKS = 100;

    public TwistingBandItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (slot != EquipmentSlot.HEAD || !(entity instanceof ServerPlayer wearer)) {
            return;
        }
        if (level.getGameTime() % 5L != 1L) {
            return;
        }

        AABB bounds = wearer.getBoundingBox().inflate(RANGE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                target -> target != wearer && target.isAlive() && shouldAffect(wearer, target))) {
            if (target instanceof ServerPlayer player) {
                twistPlayer(player);
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0, true, false));
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, EFFECT_TICKS, 0, true, false));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.witchery.twisting_band.tip").withStyle(ChatFormatting.BLUE));
    }

    public static boolean isWorn(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(WitcheryItems.TWISTING_BAND.get());
    }

    private static boolean shouldAffect(ServerPlayer wearer, LivingEntity target) {
        if (target.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)) {
            return false;
        }
        if (!target.hasLineOfSight(wearer)) {
            return false;
        }

        Vec3 eyes = target.getEyePosition();
        Vec3 wearerCenter = wearer.position().add(0.0D, wearer.getBbHeight() * 0.5D, 0.0D);
        Vec3 toWearer = wearerCenter.subtract(eyes);
        double distance = toWearer.length();
        if (distance <= 0.0001D || distance > RANGE) {
            return false;
        }

        double dot = target.getLookAngle().normalize().dot(toWearer.normalize());
        return dot > 1.0D - 0.025D / distance;
    }

    private static void twistPlayer(ServerPlayer player) {
        float yaw = Mth.wrapDegrees(player.getYRot() + 180.0F);
        player.teleportTo((ServerLevel) player.level(), player.getX(), player.getY(), player.getZ(),
                Set.of(), yaw, player.getXRot(), false);
    }
}
