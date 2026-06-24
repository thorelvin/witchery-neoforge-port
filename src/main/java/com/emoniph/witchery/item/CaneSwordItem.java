package com.emoniph.witchery.item;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.init.WitcheryAttachments;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.init.WitcherySoundEvents;
import com.emoniph.witchery.network.SyncPlayerDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

public class CaneSwordItem extends Item {
    private static final String TAG_BLOOD_RESERVE = "WITCBloodReserve";
    private static final int SOURCE_DIAMOND_DURABILITY = 1561;
    private static final int SOURCE_IRON_ENCHANTABILITY = 14;
    private static final int MAX_BLOOD_RESERVE = 250;
    private static final double SHEATHED_ATTACK_DAMAGE = 1.0D;
    private static final double DRAWN_ATTACK_DAMAGE = 11.0D;
    private static final double DRAWN_ATTACK_SPEED = -2.4D;

    private final boolean sheathed;

    public CaneSwordItem(Properties properties, boolean sheathed) {
        super(properties.stacksTo(1)
                .durability(SOURCE_DIAMOND_DURABILITY)
                .enchantable(SOURCE_IRON_ENCHANTABILITY)
                .repairable(Items.DIAMOND)
                .rarity(Rarity.RARE)
                .attributes(sourceAttributes(sheathed)));
        this.sheathed = sheathed;
    }

    private static ItemAttributeModifiers sourceAttributes(boolean sheathed) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(id(sheathed ? "cane_sword_sheathed_attack_damage" : "cane_sword_drawn_attack_damage"),
                                sheathed ? SHEATHED_ATTACK_DAMAGE : DRAWN_ATTACK_DAMAGE,
                                AttributeModifier.Operation.ADD_VALUE),
                        net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        if (!sheathed) {
            builder.add(Attributes.ATTACK_SPEED,
                    new AttributeModifier(id("cane_sword_drawn_attack_speed"),
                            DRAWN_ATTACK_SPEED, AttributeModifier.Operation.ADD_VALUE),
                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            ItemStack transformed = stack.transmuteCopy(
                    sheathed ? WitcheryItems.DRAWN_CANE_SWORD.get() : WitcheryItems.CANE_SWORD.get(),
                    stack.getCount());
            if (!level.isClientSide()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        sheathed ? WitcherySoundEvents.RANDOM_SWORD_DRAW.get()
                                : WitcherySoundEvents.RANDOM_SWORD_SHEATHE.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS.heldItemTransformedTo(transformed);
        }

        if (!sheathed) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer && extractReserve(serverPlayer, stack)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_SNARE.value(),
                SoundSource.PLAYERS, 0.5F, 0.6F);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean extractReserve(ServerPlayer player, ItemStack stack) {
        int reserve = getBloodReserve(stack);
        if (reserve <= 0) {
            return false;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        int missingPower = data.getMaxBloodPower() - data.getBloodPower();
        if (!data.isVampire() || missingPower <= 0) {
            return false;
        }

        int extracted = Math.min(reserve, missingPower);
        data.addBloodPower(extracted);
        setBloodReserve(stack, reserve - extracted);
        PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.from(player, data));

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT,
                SoundSource.PLAYERS, 0.6F, 0.5F);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                12, 0.35D, 0.55D, 0.35D, 0.02D);
        player.displayClientMessage(Component.translatable("item.witchery.cane_sword.extracted",
                extracted, data.getBloodPower(), data.getMaxBloodPower()).withStyle(ChatFormatting.DARK_RED), true);
        return true;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!sheathed) {
            stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.accept(Component.translatable("item.witchery.cane_sword.tip.toggle")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.witchery.cane_sword.tip.capture")
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.witchery.cane_sword.tip.reserve",
                getBloodReserve(stack), MAX_BLOOD_RESERVE).withStyle(ChatFormatting.DARK_RED));
    }

    public static void tryAbsorbBloodFromKill(ServerPlayer player, LivingEntity killed) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(WitcheryItems.DRAWN_CANE_SWORD.get())) {
            return;
        }

        WitcheryPlayerData data = WitcheryAttachments.get(player);
        if (!data.isVampire()) {
            return;
        }

        int amount = bloodReserveValue(killed);
        if (amount <= 0) {
            return;
        }

        int oldReserve = getBloodReserve(stack);
        int newReserve = Math.min(MAX_BLOOD_RESERVE, oldReserve + amount);
        if (newReserve <= oldReserve) {
            return;
        }

        setBloodReserve(stack, newReserve);
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable("item.witchery.cane_sword.reserve",
                newReserve, MAX_BLOOD_RESERVE).withStyle(ChatFormatting.DARK_RED), true);
    }

    private static int bloodReserveValue(LivingEntity killed) {
        if (killed instanceof ServerPlayer player) {
            return Math.max(50, WitcheryAttachments.get(player).getBloodLevel());
        }
        if (killed instanceof Player || killed instanceof AbstractVillager) {
            return MAX_BLOOD_RESERVE;
        }
        return 0;
    }

    public static int getBloodReserve(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .getIntOr(TAG_BLOOD_RESERVE, 0), 0, MAX_BLOOD_RESERVE);
    }

    private static void setBloodReserve(ItemStack stack, int reserve) {
        int clamped = Math.clamp(reserve, 0, MAX_BLOOD_RESERVE);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (clamped > 0) {
                tag.putInt(TAG_BLOOD_RESERVE, clamped);
            } else {
                tag.remove(TAG_BLOOD_RESERVE);
            }
        });
    }
}
