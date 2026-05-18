package com.emoniph.witchery.init;

import com.emoniph.witchery.WitcheryMod;
import com.emoniph.witchery.data.WitcheryPlayerData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class WitcheryAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, WitcheryMod.MODID);

    public static final Supplier<AttachmentType<WitcheryPlayerData>> PLAYER_DATA =
            ATTACHMENT_TYPES.register("player_data", () ->
                    AttachmentType.builder(WitcheryPlayerData::new)
                            .serialize(WitcheryPlayerData.CODEC)
                            .build());

    // Convenience helpers — call these server-side only unless data has been synced
    public static WitcheryPlayerData get(net.minecraft.world.entity.player.Player player) {
        return player.getData(PLAYER_DATA.get());
    }
}
