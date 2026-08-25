package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.item.BrewBagItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class BrewBagRadialHud {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            WitcheryMod.MODID, "textures/gui/radial.png");
    private static final int[] ITEM_X = {0, 24, 32, 24, 0, -24, -32, -24};
    private static final int[] ITEM_Y = {-32, -24, 0, 24, 32, 24, 0, -24};

    private BrewBagRadialHud() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || !BrewBagItem.isRadialActive(player)) {
            return;
        }
        ItemStack bag = player.getUseItem();
        if (!(bag.getItem() instanceof BrewBagItem)) {
            BrewBagItem.clearRadialUse(player);
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int selected = BrewBagItem.radialSelection(player);
        if (selected < 0) {
            int color = BrewBagItem.getColor(bag);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    centerX - 43, centerY - 43, 0.0F, 0.0F, 86, 86, 256, 256,
                    0xFF000000 | color);
            for (int slot = 0; slot < BrewBagItem.SLOT_COUNT; slot++) {
                renderSlot(graphics, minecraft.font, bag, slot, centerX, centerY, true);
            }
        } else {
            renderSlot(graphics, minecraft.font, bag, selected, centerX, centerY, false);
        }
    }

    private static void renderSlot(GuiGraphics graphics, Font font, ItemStack bag, int slot,
                                   int centerX, int centerY, boolean outerLabel) {
        ItemStack brew = BrewBagItem.getStoredBrew(bag, slot);
        if (brew.isEmpty()) {
            return;
        }
        int itemX = centerX - 8 + ITEM_X[slot];
        int itemY = centerY - 8 + ITEM_Y[slot];
        graphics.renderItem(brew, itemX, itemY);
        graphics.renderItemDecorations(font, brew, itemX, itemY);

        String name = font.plainSubstrByWidth(brew.getHoverName().getString(), outerLabel ? 88 : 120);
        int textWidth = font.width(name);
        int textX;
        int textY;
        if (!outerLabel) {
            textX = centerX - textWidth / 2;
            textY = centerY + 20;
        } else if (slot == 0 || slot == 4) {
            textX = centerX - textWidth / 2;
            textY = centerY + (slot == 0 ? -53 : 49);
        } else if (slot >= 1 && slot <= 3) {
            textX = centerX + 44;
            textY = centerY + ITEM_Y[slot] - 4;
        } else {
            textX = centerX - 44 - textWidth;
            textY = centerY + ITEM_Y[slot] - 4;
        }
        graphics.drawString(font, name, textX, textY, 0xE6FFFFFF, true);
    }
}
