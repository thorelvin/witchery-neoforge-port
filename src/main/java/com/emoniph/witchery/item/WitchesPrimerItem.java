package com.emoniph.witchery.item;

import com.emoniph.witchery.book.WitcheryBooks;
import com.emoniph.witchery.blocks.entity.PlacedModonomiconBlockEntity;
import com.emoniph.witchery.init.WitcheryBlocks;
import com.emoniph.witchery.protection.WitcheryTerrainProtection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Consumer;

/**
 * A guidebook item that opens a native Modonomicon book. Defaults to the Witches' Primer
 * (Book of Shadows); other guide items reuse this class with a different book id.
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
        if (level.isClientSide()) {
            openBookClient();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return use(context.getLevel(), player, context.getHand());
        }

        return placeBook(context, player);
    }

    public void openBookClient() {
        if (categoryId != null && entryId != null) {
            com.emoniph.witchery.client.WitcheryBookOpener.openEntry(bookId, categoryId, entryId);
        } else if (categoryId != null) {
            com.emoniph.witchery.client.WitcheryBookOpener.openCategoryList(bookId, categoryId);
        } else if (bookId.equals(WitcheryBooks.PRIMER)) {
            com.emoniph.witchery.client.WitcheryBookOpener.openResumingOrFirst(bookId,
                    Identifier.fromNamespaceAndPath("witchery", "getting_started"),
                    Identifier.fromNamespaceAndPath("witchery", "getting_started/first_steps"));
        } else {
            com.emoniph.witchery.client.WitcheryBookOpener.open(bookId);
        }
    }

    public Identifier getBookId() {
        return bookId;
    }

    public Identifier getCategoryId() {
        return categoryId;
    }

    public Identifier getEntryId() {
        return entryId;
    }

    private InteractionResult placeBook(UseOnContext context, Player player) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos placePos = placeContext.getClickedPos();
        BlockState state = WitcheryBlocks.PLACED_MODONOMICON.get().getStateForPlacement(placeContext);

        if (state == null || !placeContext.canPlace() || !state.canSurvive(level, placePos)) {
            return InteractionResult.FAIL;
        }
        if (!level.mayInteract(player, placePos) || !player.mayUseItemAt(placePos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (!WitcheryTerrainProtection.canPlace(player, level, placePos)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!level.setBlock(placePos, state, 3)) {
            return InteractionResult.FAIL;
        }
        if (level.getBlockEntity(placePos) instanceof PlacedModonomiconBlockEntity be) {
            be.setBookStack(stack);
        } else {
            level.removeBlock(placePos, false);
            return InteractionResult.FAIL;
        }

        level.playSound(null, placePos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, placePos);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
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
