package com.emoniph.witchery.book;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.resources.Identifier;

/**
 * Shared constants/helpers for the native Modonomicon books (Book of Shadows family).
 * Book ids are separate from the item ids that open them, though they may share a path.
 */
public final class WitcheryBooks {

    private WitcheryBooks() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(WitcheryMod.MODID, path);
    }

    /** The main guide, opened by the Witches' Primer (and the other generic guide items). */
    public static final Identifier PRIMER = id("witches_primer");
    /** Cauldron Book — brewing in the Witch's Cauldron. */
    public static final Identifier CAULDRON = id("cauldron_book");
    /** Observations of an Immortal — a vampire's lore book. */
    public static final Identifier OBSERVATIONS = id("observations");

    /** Sepia grimoire ink used for titles and body text across all Witchery books. */
    public static final int INK_COLOR = 0x53341a;
    private static final String INK_HEX = "53341a";

    /**
     * Wraps body text in a Modonomicon markdown colour span so it renders in the sepia ink.
     * Uses the {@code [#](rrggbb)…[#]()} link syntax handled by Modonomicon's {@code ColorLinkRenderer};
     * only works on string (markdown) text, not literal Components. Bold ({@code **x**}) / italic
     * ({@code *x*}) inside the span render normally.
     */
    public static String ink(String text) {
        return "[#](" + INK_HEX + ")" + text + "[#]()";
    }
}
