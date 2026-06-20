package com.emoniph.witchery.client;

import com.emoniph.witchery.client.model.VampireArmorModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Supplies the custom {@link VampireArmorModel} for a vampire clothing variant.
 *
 * <p>1.21.11 has no {@code setPartVisibility}; vanilla scopes each armor slot with a separate baked
 * model that only contains that slot's cubes. We instead return one full model and scope it here:
 * {@code getGenericArmorModel} copies the (already-posed) original's part transforms, then we hide
 * every part and re-enable only the ones for this item's actual equipment slot (read from the stack's
 * {@link Equippable}). Decorative cubes parented to head/body/arms inherit that visibility, so a
 * hat shows only on the head, the coat only on body+arms, etc. — no bleed onto other body parts.
 *
 * <p><b>Slim arms.</b> Vanilla armor is slim-agnostic (both wide and slim players bake the same armor
 * {@code ArmorModelSet}), so the {@code original} model carries no skin info and this hook gets no entity.
 * {@link com.emoniph.witchery.events.WitcheryClientEvents} sets {@link #SLIM_RENDER} from the player render
 * state for the duration of each player render; we read it here to pick the slim model + texture. Masculine
 * and feminine ship slim variants; metallic reuses its single model (matches source).
 */
public class VampireArmorClientExtensions implements IClientItemExtensions {

    /** Set true while rendering a slim-skinned player (see WitcheryClientEvents render hooks). */
    public static final ThreadLocal<Boolean> SLIM_RENDER = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final VampireArmorModel.Variant variant;
    private VampireArmorModel wideModel;
    private VampireArmorModel slimModel;

    public VampireArmorClientExtensions(VampireArmorModel.Variant variant) {
        this.variant = variant;
    }

    private boolean useSlim() {
        return variant.hasSlim && Boolean.TRUE.equals(SLIM_RENDER.get());
    }

    private VampireArmorModel model(boolean slim) {
        if (slim) {
            if (slimModel == null) {
                slimModel = new VampireArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(variant.slimLayer));
            }
            return slimModel;
        }
        if (wideModel == null) {
            wideModel = new VampireArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(variant.layer));
        }
        return wideModel;
    }

    @Override
    public Model getGenericArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
        VampireArmorModel model = model(useSlim());
        if (original instanceof HumanoidModel<?> originalHumanoid) {
            copyPose(originalHumanoid.head, model.head);
            copyPose(originalHumanoid.hat, model.hat);
            copyPose(originalHumanoid.body, model.body);
            copyPose(originalHumanoid.rightArm, model.rightArm);
            copyPose(originalHumanoid.leftArm, model.leftArm);
            copyPose(originalHumanoid.rightLeg, model.rightLeg);
            copyPose(originalHumanoid.leftLeg, model.leftLeg);
        }
        applySlotVisibility(model, equipmentSlot(stack));
        return model;
    }

    @Override
    public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type,
                                      EquipmentClientInfo.Layer layer, Identifier defaultTexture) {
        // Only the chest (HUMANOID) layer has a slim variant; legs use the standard texture (leg width
        // is identical for slim and wide), so don't redirect HUMANOID_LEGGINGS to a non-existent _slim.
        if (type != EquipmentClientInfo.LayerType.HUMANOID || !useSlim()) {
            return defaultTexture;
        }
        String path = defaultTexture.getPath();
        if (!path.endsWith(".png")) {
            return defaultTexture;
        }
        return Identifier.fromNamespaceAndPath(defaultTexture.getNamespace(),
                path.substring(0, path.length() - 4) + "_slim.png");
    }

    private static EquipmentSlot equipmentSlot(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : EquipmentSlot.CHEST;
    }

    /** Show only the parts belonging to {@code slot} (children inherit their parent's visibility). */
    private static void applySlotVisibility(VampireArmorModel model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> showLegGroup(model, "legs");
            case FEET -> showLegGroup(model, "boots");
            default -> {
            }
        }
    }

    /**
     * Show the legs and enable only one of their child groups: {@code "legs"} (trousers / leg-armor) for the
     * LEGS slot, {@code "boots"} (footwear) for the FEET slot. Both groups live under each leg part (built by
     * {@code VampireArmorModel.addLegSlot}); scoping them here keeps the leggings piece and the boots piece
     * from rendering each other's geometry.
     */
    private static void showLegGroup(VampireArmorModel model, String group) {
        model.rightLeg.visible = true;
        model.leftLeg.visible = true;
        setGroupVisible(model.rightLeg, group);
        setGroupVisible(model.leftLeg, group);
    }

    private static void setGroupVisible(ModelPart leg, String shownGroup) {
        leg.getChild("legs").visible = "legs".equals(shownGroup);
        leg.getChild("boots").visible = "boots".equals(shownGroup);
    }

    private static void copyPose(ModelPart from, ModelPart to) {
        to.x = from.x;
        to.y = from.y;
        to.z = from.z;
        to.xRot = from.xRot;
        to.yRot = from.yRot;
        to.zRot = from.zRot;
        to.xScale = from.xScale;
        to.yScale = from.yScale;
        to.zScale = from.zScale;
    }
}
