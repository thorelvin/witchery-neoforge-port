package com.emoniph.witchery.client.screen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.menu.BrewBagMenu;
import com.emoniph.witchery.menu.BrewStorageLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class BrewBagScreen extends AbstractContainerScreen<BrewBagMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            WitcheryMod.MODID, "textures/gui/generic_48.png");

    public BrewBagScreen(BrewBagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = BrewStorageLayout.Bag.WIDTH;
        imageHeight = BrewStorageLayout.Bag.HEIGHT;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 38;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                left, top, 0.0F, 0.0F, imageWidth, 35, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                left, top + 35, 0.0F, 126.0F, imageWidth, 96, 256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
