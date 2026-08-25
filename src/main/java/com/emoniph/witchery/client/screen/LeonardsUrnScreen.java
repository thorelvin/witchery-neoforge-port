package com.emoniph.witchery.client.screen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.menu.LeonardsUrnMenu;
import com.emoniph.witchery.menu.BrewStorageLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class LeonardsUrnScreen extends AbstractContainerScreen<LeonardsUrnMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            WitcheryMod.MODID, "textures/gui/urn.png");

    public LeonardsUrnScreen(LeonardsUrnMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = BrewStorageLayout.Urn.WIDTH;
        imageHeight = BrewStorageLayout.Urn.HEIGHT;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 90;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        for (int slot = 0; slot < menu.capacity(); slot++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    leftPos + menu.brewSlotX(slot) - 1,
                    topPos + menu.brewSlotY(slot) - 1,
                    176.0F, 0.0F, 18, 18, 256, 256);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
