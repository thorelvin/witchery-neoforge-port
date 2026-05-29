package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmedEntityRenderState extends LivingEntityRenderState {
    public HumanoidArm mainArm = HumanoidArm.RIGHT;
    public HumanoidModel.ArmPose rightArmPose = HumanoidModel.ArmPose.EMPTY;
    public final ItemStackRenderState rightHandItemState = new ItemStackRenderState();
    public ItemStack rightHandItemStack = ItemStack.EMPTY;
    public HumanoidModel.ArmPose leftArmPose = HumanoidModel.ArmPose.EMPTY;
    public final ItemStackRenderState leftHandItemState = new ItemStackRenderState();
    public ItemStack leftHandItemStack = ItemStack.EMPTY;
    public SwingAnimationType swingAnimationType = SwingAnimationType.WHACK;
    public float attackTime;

    public ItemStackRenderState getMainHandItemState() {
        return this.mainArm == HumanoidArm.RIGHT ? this.rightHandItemState : this.leftHandItemState;
    }

    public ItemStack getMainHandItemStack() {
        return this.mainArm == HumanoidArm.RIGHT ? this.rightHandItemStack : this.leftHandItemStack;
    }

    public ItemStack getUseItemStackForArm(HumanoidArm p_456053_) {
        return p_456053_ == HumanoidArm.RIGHT ? this.rightHandItemStack : this.leftHandItemStack;
    }

    public float ticksUsingItem(HumanoidArm p_455723_) {
        return 0.0F;
    }

    public static void extractArmedEntityRenderState(LivingEntity p_387833_, ArmedEntityRenderState p_387185_, ItemModelResolver p_386820_, float p_455838_) {
        p_387185_.mainArm = p_387833_.getMainArm();
        ItemStack itemstack = p_387833_.getMainHandItem();
        p_387185_.swingAnimationType = itemstack.getSwingAnimation().type();
        p_387185_.attackTime = p_387833_.getAttackAnim(p_455838_);
        p_386820_.updateForLiving(
            p_387185_.rightHandItemState, p_387833_.getItemHeldByArm(HumanoidArm.RIGHT), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, p_387833_
        );
        p_386820_.updateForLiving(
            p_387185_.leftHandItemState, p_387833_.getItemHeldByArm(HumanoidArm.LEFT), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, p_387833_
        );
        p_387185_.leftHandItemStack = p_387833_.getItemHeldByArm(HumanoidArm.LEFT).copy();
        p_387185_.rightHandItemStack = p_387833_.getItemHeldByArm(HumanoidArm.RIGHT).copy();
    }
}
