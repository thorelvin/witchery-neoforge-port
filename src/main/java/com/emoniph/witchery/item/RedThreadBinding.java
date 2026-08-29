package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.UUID;

/** Item-stack state for the one Red Thread curse-anchor path. */
public final class RedThreadBinding {
    private static final String TAG_BINDING_ID = "red_thread_binding_id";
    private static final String TAG_CASTER_ID = "red_thread_caster_id";
    private static final String TAG_CASTER_NAME = "red_thread_caster_name";
    private static final String TAG_ACTIVE_CURSE = "red_thread_active_curse";

    private RedThreadBinding() {
    }

    public static boolean canPrepare(ItemStack stack) {
        return stack.is(WitcheryItems.POPPET.get())
                && PoppetItem.isBound(stack)
                && !PoppetItem.isMobBound(stack)
                && !isPrepared(stack);
    }

    public static boolean prepare(ItemStack stack, ServerPlayer caster) {
        if (!canPrepare(stack) || caster == null) {
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_BINDING_ID, UUID.randomUUID().toString());
            tag.putString(TAG_CASTER_ID, caster.getStringUUID());
            tag.putString(TAG_CASTER_NAME, caster.getName().getString());
            tag.remove(TAG_ACTIVE_CURSE);
        });
        return true;
    }

    public static boolean isPrepared(ItemStack stack) {
        return bindingId(stack).isPresent() && casterId(stack).isPresent()
                && PoppetItem.getBoundUUID(stack).isPresent();
    }

    public static boolean isActive(ItemStack stack) {
        return isPrepared(stack) && !activeCurse(stack).isBlank();
    }

    public static Optional<UUID> bindingId(ItemStack stack) {
        return readUuid(stack, TAG_BINDING_ID);
    }

    public static Optional<UUID> casterId(ItemStack stack) {
        return readUuid(stack, TAG_CASTER_ID);
    }

    public static String casterName(ItemStack stack) {
        return tag(stack).getStringOr(TAG_CASTER_NAME, "");
    }

    public static String activeCurse(ItemStack stack) {
        return tag(stack).getStringOr(TAG_ACTIVE_CURSE, "");
    }

    public static boolean activate(ItemStack stack, UUID bindingId, String curseId) {
        if (!isPrepared(stack) || !RedThreadBinding.bindingId(stack).filter(bindingId::equals).isPresent()
                || curseId == null || curseId.isBlank()) {
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(TAG_ACTIVE_CURSE, curseId));
        return true;
    }

    public static void clear(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(TAG_BINDING_ID);
            tag.remove(TAG_CASTER_ID);
            tag.remove(TAG_CASTER_NAME);
            tag.remove(TAG_ACTIVE_CURSE);
        });
    }

    private static Optional<UUID> readUuid(ItemStack stack, String key) {
        String value = tag(stack).getStringOr(key, "");
        if (value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static CompoundTag tag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
