package com.emoniph.witchery.item;

import com.emoniph.witchery.entity.WitchHunterEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;
import net.minecraft.world.InteractionResult;

/**
 * Voodoo Poppet — offensive. Right-click to apply Poison + Weakness to the bound player.
 * Each use consumes the poppet (one-time use per poppet).
 */
public class VoodooPoppetItem extends PoppetItem {

    public VoodooPoppetItem(Properties props) { super(props); }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS.heldItemTransformedTo(stack);

        if (!isBound(stack)) {
            user.displayClientMessage(Component.translatable("witchery.poppet.not_bound"), false);
            return net.minecraft.world.InteractionResult.PASS;
        }

        UUID boundUUID = getBoundUUID(stack).orElse(null);
        if (boundUUID == null) return net.minecraft.world.InteractionResult.PASS;

        // Find the bound player on this server
        ServerPlayer target = null;
        if (((net.minecraft.server.level.ServerLevel) level).getServer() != null) {
            target = ((net.minecraft.server.level.ServerLevel) level).getServer().getPlayerList().getPlayer(boundUUID);
        }

        if (target == null) {
            user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.not_online"), false);
            return net.minecraft.world.InteractionResult.PASS;
        }

        // Apply voodoo effects
        target.addEffect(new MobEffectInstance(MobEffects.POISON,   200, 1)); // 10s Poison II
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0)); // 15s Weakness
        if (user instanceof ServerPlayer serverPlayer) {
            WitchHunterEntity.blackMagicPerformed(serverPlayer);
        }

        user.displayClientMessage(Component.translatable("witchery.poppet.voodoo.success", getBoundName(stack)), false);

        // Consume the poppet
        stack.shrink(1);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
