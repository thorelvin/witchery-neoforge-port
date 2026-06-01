package com.emoniph.witchery.item;

import com.emoniph.witchery.init.WitcheryItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class BiomeNoteItem extends Item {
    private static final String TAG_BIOME = "Biome";
    private static final String TAG_BIOME_NAME = "BiomeName";

    public static final List<ResourceKey<Biome>> BIOME_PAGES = List.of(
            Biomes.BEACH,
            Biomes.FOREST,
            Biomes.WINDSWEPT_HILLS,
            Biomes.BADLANDS,
            Biomes.JAGGED_PEAKS,
            Biomes.PLAINS,
            Biomes.DESERT,
            Biomes.SNOWY_PLAINS,
            Biomes.SWAMP,
            Biomes.RIVER,
            Biomes.OCEAN,
            Biomes.DARK_FOREST,
            Biomes.CHERRY_GROVE,
            Biomes.MUSHROOM_FIELDS
    );

    public BiomeNoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.witchery.biome_note.named", getBiomeDisplayName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("item.witchery.biome_note.target", getBiomeDisplayName(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.witchery.biome_note.tip")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    public static ItemStack create(ResourceKey<Biome> biomeKey) {
        ItemStack stack = new ItemStack(WitcheryItems.BIOME_NOTE.get());
        setBiome(stack, biomeKey);
        return stack;
    }

    public static void setBiome(ItemStack stack, ResourceKey<Biome> biomeKey) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_BIOME, biomeId(biomeKey));
            tag.putString(TAG_BIOME_NAME, displayName(biomeKey));
        });
    }

    public static Optional<ResourceKey<Biome>> getBiomeKey(ItemStack stack) {
        CompoundTag tag = customData(stack);
        String id = tag.getStringOr(TAG_BIOME, "");
        if (id.isBlank()) {
            return Optional.of(Biomes.PLAINS);
        }
        try {
            return Optional.of(ResourceKey.create(Registries.BIOME, Identifier.parse(id)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static String getBiomeDisplayName(ItemStack stack) {
        CompoundTag tag = customData(stack);
        String stored = tag.getStringOr(TAG_BIOME_NAME, "");
        if (!stored.isBlank()) {
            return stored;
        }
        return getBiomeKey(stack).map(BiomeNoteItem::displayName).orElse("Plains");
    }

    public static String displayName(ResourceKey<Biome> biomeKey) {
        String id = biomeId(biomeKey);
        String path = id.substring(id.indexOf(':') + 1).replace('_', ' ');
        StringBuilder result = new StringBuilder(path.length());
        boolean capitalize = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (Character.isWhitespace(c)) {
                capitalize = true;
                result.append(c);
            } else if (capitalize) {
                result.append(String.valueOf(c).toUpperCase(Locale.ROOT));
                capitalize = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String biomeId(ResourceKey<Biome> biomeKey) {
        String text = biomeKey.toString();
        int split = text.lastIndexOf(" / ");
        int end = text.lastIndexOf(']');
        if (split >= 0 && end > split) {
            return text.substring(split + 3, end);
        }
        return "minecraft:plains";
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
