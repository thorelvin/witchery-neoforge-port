package com.emoniph.witchery.events;

import com.emoniph.witchery.WitcheryMod;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Game-event listeners for the Witch's Cauldron.
 *
 * The cauldron-anointing behaviour (Anointing Paste on a vanilla Cauldron)
 * was moved to {@link com.emoniph.witchery.item.AnointingPasteItem#useOn}
 * so the item itself owns the interaction, matching the standard NeoForge
 * item-use pattern and enabling proper client-side particle feedback.
 *
 * Keep this class as a home for any future cauldron block-event hooks.
 */
@EventBusSubscriber(modid = WitcheryMod.MODID)
public class WitchCauldronEvents {
    // Future cauldron event handlers go here.
}
