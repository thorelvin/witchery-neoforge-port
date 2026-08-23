package com.emoniph.witchery.client;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import com.emoniph.witchery.entity.VillageGuardEntity;
import com.emoniph.witchery.entity.WereVillagerEntity;
import com.emoniph.witchery.init.WitcheryAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Source-shaped Blood Power drops and selectable vampire abilities beside the hotbar. */
public final class VampirePowerHud {
    private static final Identifier BLOOD_DROP = texture("gui/bdrop.png");
    private static final Identifier BLOOD_DROP_FADED = texture("gui/bdropfaded.png");
    private static final Identifier TEETH = texture("item/vteeth.png");
    private static final Identifier EYE = texture("item/veye.png");
    private static final Identifier SPEED = texture("item/vspeed.png");
    private static final Identifier BAT = texture("item/vbat.png");
    private static final Identifier COFFIN = texture("item/vcoffin.png");
    private static final Identifier STORM = texture("item/vstorm.png");
    private static final Identifier FIST = texture("item/vfist.png");
    private static final Identifier HOTBAR_SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final int DROP_SIZE = 8;
    private static final int POWER_ICON_SIZE = 16;
    private static final int POWER_SPACING = 20;
    private static final String VILLAGER_BLOOD_TAG = "witchery_villager_blood";

    private VampirePowerHud() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        WitcheryPlayerData data = WitcheryAttachments.get(mc.player);
        if (!data.isVampire()) return;

        renderBloodPower(graphics, data);
        renderTargetBlood(graphics, mc);
        renderPowerStrip(graphics, data);
    }

    private static void renderBloodPower(GuiGraphics graphics, WitcheryPlayerData data) {
        int x = 10;
        int bottom = graphics.guiHeight() / 2 + 16;
        int capacityDrops = data.getMaxBloodPower() / 250;
        for (int i = 0; i < capacityDrops; i++) {
            drawFull(graphics, BLOOD_DROP_FADED, x, bottom - i * DROP_SIZE);
        }

        int blood = data.getBloodPower();
        int fullDrops = blood / 250;
        for (int i = 0; i < fullDrops; i++) {
            drawFull(graphics, BLOOD_DROP, x, bottom - i * DROP_SIZE);
        }
        int remainder = blood % 250;
        if (remainder > 0) {
            int fill = Math.max(1, Math.round(DROP_SIZE * remainder / 250.0F));
            int y = bottom - fullDrops * DROP_SIZE + DROP_SIZE - fill;
            graphics.blit(RenderPipelines.GUI_TEXTURED, BLOOD_DROP,
                    x, y, 0.0F, DROP_SIZE - fill, DROP_SIZE, fill, DROP_SIZE, DROP_SIZE);
        }
    }

    private static void renderTargetBlood(GuiGraphics graphics, Minecraft mc) {
        LivingEntity target = findBloodTarget(mc);
        if (target == null) return;
        int blood;
        if (target instanceof Player player) {
            WitcheryPlayerData targetData = WitcheryAttachments.get(player);
            if (targetData.isVampire()) return;
            blood = targetData.getHumanBlood();
        } else if (target instanceof VillageGuardEntity guard) {
            blood = guard.getBlood();
        } else if (target instanceof Villager villager && !(target instanceof WereVillagerEntity)) {
            blood = villager.getPersistentData().getIntOr(VILLAGER_BLOOD_TAG, 500);
        } else {
            return;
        }

        int x = graphics.guiWidth() / 2 - 16;
        int y = graphics.guiHeight() / 2 + 10;
        for (int i = 0; i < 4; i++) drawFull(graphics, BLOOD_DROP_FADED, x + i * DROP_SIZE, y);

        int fullDrops = blood / 125;
        for (int i = 0; i < fullDrops; i++) drawFull(graphics, BLOOD_DROP, x + i * DROP_SIZE, y);
        int remainder = blood % 125;
        if (remainder > 0 && fullDrops < 4) {
            int fill = Math.max(1, Math.round(DROP_SIZE * remainder / 125.0F));
            int fillY = y + DROP_SIZE - fill;
            graphics.blit(RenderPipelines.GUI_TEXTURED, BLOOD_DROP,
                    x + fullDrops * DROP_SIZE, fillY, 0.0F, DROP_SIZE - fill,
                    DROP_SIZE, fill, DROP_SIZE, DROP_SIZE);
        }
    }

    private static LivingEntity findBloodTarget(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        Vec3 end = eye.add(look.scale(5.0D));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.player, eye, end,
                mc.player.getBoundingBox().expandTowards(look.scale(5.0D)).inflate(1.0D),
                entity -> entity instanceof LivingEntity && entity != mc.player,
                25.0D);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static void renderPowerStrip(GuiGraphics graphics, WitcheryPlayerData data) {
        int y = graphics.guiHeight() - 19;
        int selected = data.getSelectedVampirePower().ordinal();
        int max = data.getMaxAvailableVampirePowerOrdinal();
        for (int ordinal = 1; ordinal <= max; ordinal++) {
            int x = graphics.guiWidth() / 2 - 90 - ordinal * POWER_SPACING + 2;
            if (ordinal == selected) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION, x - 4, y - 4, 24, 23);
            }
            Identifier icon = powerTexture(WitcheryPlayerData.VampirePower.fromOrdinal(ordinal), data);
            drawFull(graphics, icon, x, y, POWER_ICON_SIZE);
            if (ordinal == WitcheryPlayerData.VampirePower.ULTIMATE.ordinal()
                    && (data.getVampireUltimate() == WitcheryPlayerData.VampireUltimate.NONE
                    || data.getVampireUltimateCharges() <= 0)) {
                graphics.fill(x, y, x + POWER_ICON_SIZE, y + POWER_ICON_SIZE, 0xA0000000);
            }
        }
    }

    private static Identifier powerTexture(WitcheryPlayerData.VampirePower power, WitcheryPlayerData data) {
        return switch (power) {
            case DRINK -> TEETH;
            case MESMERIZE -> EYE;
            case SPEED -> SPEED;
            case BAT -> BAT;
            case ULTIMATE -> switch (data.getVampireUltimate()) {
                case FARM -> COFFIN;
                case STORM -> STORM;
                default -> FIST;
            };
            default -> TEETH;
        };
    }

    private static void drawFull(GuiGraphics graphics, Identifier texture, int x, int y) {
        drawFull(graphics, texture, x, y, DROP_SIZE);
    }

    private static void drawFull(GuiGraphics graphics, Identifier texture, int x, int y, int size) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y, 0.0F, 0.0F, size, size, size, size);
    }

    private static Identifier texture(String path) {
        return Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "textures/" + path);
    }
}
