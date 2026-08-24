package com.emoniph.witchery.datagen;

import com.emoniph.witchery.book.CauldronBookProvider;
import com.emoniph.witchery.book.WitchesPrimerBookProvider;
import com.klikli_dev.modonomicon.api.datagen.NeoBookProvider;
import net.minecraft.data.DataGenerator;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Mod-bus data generation entry point. Registered from {@code WitcheryMod}.
 *
 * <p>Modonomicon book JSON (data) and its lang (assets) are both produced on the
 * {@link GatherDataEvent.Client} event, mirroring Modonomicon's own datagen wiring on
 * NeoForge 21.11 (where {@code GatherDataEvent} is split into Client/Server).
 */
public final class WitcheryDataGenerators {

    private WitcheryDataGenerators() {}

    public static void onGatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        generator.addProvider(true, NeoBookProvider.of(event,
                new WitchesPrimerBookProvider(),
                new CauldronBookProvider()));
    }
}
