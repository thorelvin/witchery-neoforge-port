package com.emoniph.witchery.client.screen;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.menu.MachinePresentationLayout;
import com.emoniph.witchery.menu.SpinningWheelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SpinningWheelScreen extends AbstractContainerScreen<SpinningWheelMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "textures/gui/spinningwheel.png");

    public SpinningWheelScreen(SpinningWheelMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = (this.imageWidth - this.font.width(title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);

        if (!menu.hasPower()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    x + MachinePresentationLayout.SpinningWheel.NO_POWER_X,
                    y + MachinePresentationLayout.SpinningWheel.NO_POWER_Y,
                    MachinePresentationLayout.SpinningWheel.NO_POWER_U,
                    MachinePresentationLayout.SpinningWheel.NO_POWER_V,
                    MachinePresentationLayout.SpinningWheel.NO_POWER_WIDTH,
                    MachinePresentationLayout.SpinningWheel.NO_POWER_HEIGHT, 256, 256);
        }

        int arrowWidth = menu.getSpinProgress();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                x + MachinePresentationLayout.SpinningWheel.PROGRESS_X,
                y + MachinePresentationLayout.SpinningWheel.PROGRESS_Y,
                MachinePresentationLayout.SpinningWheel.PROGRESS_U,
                MachinePresentationLayout.SpinningWheel.PROGRESS_V,
                arrowWidth + 1, MachinePresentationLayout.SpinningWheel.PROGRESS_HEIGHT, 256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
