package com.emoniph.witchery.brewing;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Text bound into a warning brew. Stored separately from {@link BrewContents} so the composed brew
 * remains source-style item ids, while player-authored words travel with the bottle/item/block.
 */
public record BrewWarningText(String message) {
    public static final int MAX_LENGTH = 96;
    public static final int COLOR = 0xB260FF;
    public static final int TRIGGER_COOLDOWN_TICKS = 100;

    public static final Codec<BrewWarningText> CODEC =
            Codec.STRING.xmap(BrewWarningText::sanitize, BrewWarningText::message);

    public static final StreamCodec<RegistryFriendlyByteBuf, BrewWarningText> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public BrewWarningText decode(RegistryFriendlyByteBuf buffer) {
                    return BrewWarningText.sanitize(ByteBufCodecs.STRING_UTF8.decode(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, BrewWarningText value) {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, value.message());
                }
            };

    public BrewWarningText {
        message = clean(message);
    }

    public boolean isEmpty() {
        return message.isEmpty();
    }

    public static BrewWarningText sanitize(String raw) {
        return new BrewWarningText(clean(raw));
    }

    private static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(Math.min(raw.length(), MAX_LENGTH));
        boolean pendingSpace = false;
        int written = 0;
        for (int offset = 0; offset < raw.length() && written < MAX_LENGTH;) {
            int codePoint = raw.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint == '\u00a7') {
                if (offset < raw.length()) {
                    offset += Character.charCount(raw.codePointAt(offset));
                }
                continue;
            }
            if (Character.isISOControl(codePoint)) {
                continue;
            }
            if (Character.isWhitespace(codePoint)) {
                pendingSpace = out.length() > 0;
                continue;
            }
            if (pendingSpace && out.length() > 0 && written < MAX_LENGTH) {
                out.append(' ');
                written++;
                pendingSpace = false;
                if (written >= MAX_LENGTH) {
                    break;
                }
            }
            out.appendCodePoint(codePoint);
            written++;
        }
        return out.toString().trim();
    }
}
