package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.init.WitcheryItems;
import com.emoniph.witchery.item.BarkBeltItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the old Witchery Bark Belt charge pips from {@code textures/gui/creatures.png}.
 */
public final class BarkBeltHud {

    private static final Identifier CREATURES =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "textures/gui/creatures.png");
    private static final int ICON_SIZE = 8;
    private static final int ICON_U = 0;
    private static final int ICON_V = 248;
    private static final int TEXTURE_SIZE = 256;

    private BarkBeltHud() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative()) {
            return;
        }

        ItemStack belt = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!belt.is(WitcheryItems.BARK_BELT.get())) {
            return;
        }

        int charges = Math.min(BarkBeltItem.getChargeLevel(belt), BarkBeltItem.getMaxChargeLevel(player));
        if (charges <= 0) {
            return;
        }

        int left = graphics.guiWidth() / 2 - 91;
        int top = barkRowY(graphics, player);
        for (int i = 0; i < charges; i++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, CREATURES,
                    left + i * ICON_SIZE, top,
                    ICON_U, ICON_V, ICON_SIZE, ICON_SIZE,
                    TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    private static int barkRowY(GuiGraphics graphics, LocalPlayer player) {
        int heartBaseY = graphics.guiHeight() - 39;
        float healthRowsSource = (player.getMaxHealth() + player.getAbsorptionAmount()) / 2.0F / 10.0F;
        int rows = Math.max(1, Mth.ceil(healthRowsSource));
        int rowSpacing = Math.max(10 - (rows - 2), 3);
        return heartBaseY - (rows - 1) * rowSpacing - 10;
    }
}
