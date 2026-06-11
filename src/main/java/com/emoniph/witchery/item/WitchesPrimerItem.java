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
    private final Identifier categoryId;
    private final Identifier entryId;

    public WitchesPrimerItem(Properties props) {
        this(props, null, WitcheryBooks.PRIMER);
    }

    public WitchesPrimerItem(Properties props, String tooltipKey) {
        this(props, tooltipKey, WitcheryBooks.PRIMER);
    }

    public WitchesPrimerItem(Properties props, String tooltipKey, Identifier bookId) {
        this(props, tooltipKey, bookId, null, null);
    }

    /** Opens {@code bookId} directly at {@code entryId} (within {@code categoryId}) — for topic guides. */
    public WitchesPrimerItem(Properties props, String tooltipKey, Identifier bookId,
                             Identifier categoryId, Identifier entryId) {
        super(props);
        this.tooltipKey = tooltipKey;
        this.bookId = bookId;
        this.categoryId = categoryId;
        this.entryId = entryId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // The Modonomicon book GUI is purely client-side (book data is datapack-synced), so open it
        // directly on the logical client. The opener lives in a client-only class.
        if (level.isClientSide()) {
            if (categoryId != null) {
                // Topic guides (book_oven, book_herbology, …) open straight to their chapter's entries list.
                com.emoniph.witchery.client.WitcheryBookOpener.openCategoryList(bookId, categoryId);
            } else if (bookId.equals(WitcheryBooks.PRIMER)) {
                // The Primer resumes the last page read, or opens the first entry on first use.
                com.emoniph.witchery.client.WitcheryBookOpener.openResumingOrFirst(bookId,
                        Identifier.fromNamespaceAndPath("witchery", "getting_started"),
                        Identifier.fromNamespaceAndPath("witchery", "getting_started/first_steps"));
            } else {
                com.emoniph.witchery.client.WitcheryBookOpener.open(bookId);
            }
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
