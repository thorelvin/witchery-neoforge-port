package com.emoniph.witchery.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.function.Consumer;

/** The Witches' Primer opens the Patchouli guidebook when Patchouli is installed. */
public class WitchesPrimerItem extends Item {
    private final String tooltipKey;

    public WitchesPrimerItem(Properties props) {
        this(props, null);
    }

    public WitchesPrimerItem(Properties props, String tooltipKey) {
        super(props);
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && ModList.get().isLoaded("patchouli")) {
            if (PatchouliCompat.openBook((ServerPlayer) player)) {
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        if (!level.isClientSide()) {
            player.displayClientMessage(Component.translatable("item.witchery.witches_primer.patchouli_missing"), false);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        if (tooltipKey != null && !tooltipKey.isBlank()) {
            tooltip.accept(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
        }
    }
}
