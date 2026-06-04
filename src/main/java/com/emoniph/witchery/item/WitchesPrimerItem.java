package com.emoniph.witchery.item;

import com.emoniph.witchery.book.WitcheryBooks;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A guidebook item that opens a native Modonomicon book. Defaults to the Witches' Primer
 * (Book of Shadows); other guide items (e.g. the Cauldron Book) reuse this class with a different
 * book id.
 */
public class WitchesPrimerItem extends Item {
    private final String tooltipKey;
    private final Identifier bookId;

    public WitchesPrimerItem(Properties props) {
        this(props, null, WitcheryBooks.PRIMER);
    }

    public WitchesPrimerItem(Properties props, String tooltipKey) {
        this(props, tooltipKey, WitcheryBooks.PRIMER);
    }

    public WitchesPrimerItem(Properties props, String tooltipKey, Identifier bookId) {
        super(props);
        this.tooltipKey = tooltipKey;
        this.bookId = bookId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // The Modonomicon book GUI is purely client-side (book data is datapack-synced), so open it
        // directly on the logical client. The opener lives in a client-only class.
        if (level.isClientSide()) {
            com.emoniph.witchery.client.WitcheryBookOpener.open(bookId);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS_SERVER;
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
