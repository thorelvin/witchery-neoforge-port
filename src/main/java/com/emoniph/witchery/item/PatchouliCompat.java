package com.emoniph.witchery.item;

import com.emoniph.witchery.WitcheryMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Optional Patchouli integration kept reflection-based so Witchery can still
 * compile and run without Patchouli on the classpath.
 */
public final class PatchouliCompat {
    private static final Identifier WITCHES_PRIMER_BOOK =
            Identifier.fromNamespaceAndPath(WitcheryMod.MODID, "witches_primer");

    private PatchouliCompat() {}

    public static boolean openBook(ServerPlayer player) {
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            api.getClass()
                    .getMethod("openBookGUI", ServerPlayer.class, Identifier.class)
                    .invoke(api, player, WITCHES_PRIMER_BOOK);
            return true;
        } catch (ReflectiveOperationException | LinkageError ex) {
            WitcheryMod.LOGGER.debug("Patchouli is unavailable or failed to open the Witches' Primer", ex);
            return false;
        }
    }
}
