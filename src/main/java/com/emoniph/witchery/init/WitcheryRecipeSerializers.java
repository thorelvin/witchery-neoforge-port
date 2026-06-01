package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.crafting.ConcentratedBatBallRecipe;
import com.emoniph.witchery.crafting.KettleBrewingRecipe;
import com.emoniph.witchery.crafting.KettleBrewingRecipeSerializer;
import com.emoniph.witchery.crafting.OvenRecipe;
import com.emoniph.witchery.crafting.BiomeNoteCopyRecipe;
import com.emoniph.witchery.crafting.DistilleryRecipe;
import com.emoniph.witchery.crafting.ImpregnatedLeatherRepairRecipe;
import com.emoniph.witchery.crafting.BitingBeltPotionRecipe;
import com.emoniph.witchery.crafting.ParasyticLousePotionRecipe;
import com.emoniph.witchery.crafting.PlayerCompassBindingRecipe;
import com.emoniph.witchery.crafting.PoppetTaglockBindingRecipe;
import com.emoniph.witchery.crafting.DistilleryRecipeSerializer;
import com.emoniph.witchery.crafting.RowanKeyringRecipe;
import com.emoniph.witchery.crafting.SpinningRecipe;
import com.emoniph.witchery.crafting.SpinningRecipeSerializer;
import com.emoniph.witchery.crafting.UpdateImmortalsObservationsBookRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class WitcheryRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, WitcheryMod.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, KettleBrewingRecipeSerializer> KETTLE_BREWING =
            RECIPE_SERIALIZERS.register("kettle_brewing", KettleBrewingRecipeSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.AbstractCookingRecipe.Serializer<OvenRecipe>> OVEN =
            RECIPE_SERIALIZERS.register("oven", () -> new net.minecraft.world.item.crafting.AbstractCookingRecipe.Serializer<>(OvenRecipe::new, 200));

    public static final DeferredHolder<RecipeSerializer<?>, DistilleryRecipeSerializer> DISTILLERY =
            RECIPE_SERIALIZERS.register("distillery", DistilleryRecipeSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, SpinningRecipeSerializer> SPINNING =
            RECIPE_SERIALIZERS.register("spinning", SpinningRecipeSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<RowanKeyringRecipe>> ROWAN_KEYRING =
            RECIPE_SERIALIZERS.register("rowan_keyring",
                    () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(RowanKeyringRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<ConcentratedBatBallRecipe>> CONCENTRATED_BAT_BALL =
            RECIPE_SERIALIZERS.register("concentrated_bat_ball",
                    () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(ConcentratedBatBallRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<ParasyticLousePotionRecipe>> PARASYTIC_LOUSE_POTION =
            RECIPE_SERIALIZERS.register("parasytic_louse_potion",
                    () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(ParasyticLousePotionRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<BitingBeltPotionRecipe>> BITING_BELT_POTION =
            RECIPE_SERIALIZERS.register("biting_belt_potion",
                    () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(BitingBeltPotionRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<PoppetTaglockBindingRecipe>> POPPET_TAGLOCK_BINDING =
            RECIPE_SERIALIZERS.register("poppet_taglock_binding",
                    () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(PoppetTaglockBindingRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<PlayerCompassBindingRecipe>> PLAYER_COMPASS_BINDING =
            RECIPE_SERIALIZERS.register("player_compass_binding",
                    () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(PlayerCompassBindingRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<BiomeNoteCopyRecipe>> BIOME_NOTE_COPY =
            RECIPE_SERIALIZERS.register("biome_note_copy",
                    () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(BiomeNoteCopyRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<UpdateImmortalsObservationsBookRecipe>> UPDATE_IMMORTALS_OBSERVATIONS_BOOK =
            RECIPE_SERIALIZERS.register("update_immortals_observations_book",
                    () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(UpdateImmortalsObservationsBookRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<ImpregnatedLeatherRepairRecipe>> IMPREGNATED_LEATHER_REPAIR =
            RECIPE_SERIALIZERS.register("impregnated_leather_repair",
                    () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(ImpregnatedLeatherRepairRecipe::new));
}
