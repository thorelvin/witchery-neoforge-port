package com.emoniph.witchery.book.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts Patchouli {@code $()}-style formatting tokens into Modonomicon Markdown.
 *
 * <p>Conservative and lossless-by-warning: any token it does not recognise is dropped from the
 * output and recorded in {@link Result#warnings()} (never silently swallowed). The emphasis model is
 * a small state machine — Patchouli toggles (bold-on / reset) become paired Markdown delimiters
 * ({@code **bold**}, {@code *italic*}), closed on reset or end-of-string.
 *
 * <p>Deliberate mapping decisions (documented for Phase-3 review):
 * <ul>
 *   <li>{@code $(item)} and {@code $(thing)} (Patchouli's coloured item/concept accents) map to
 *       <b>bold</b> rather than colour, to stay readable in the book's single sepia ink.</li>
 *   <li>{@code $(l:target)…$(/l)} entry links become {@code [text](entry://witchery:target)} and are
 *       flagged as warnings so the target can be verified during entry migration.</li>
 * </ul>
 *
 * This class has no game dependencies and is unit-tested with golden strings.
 */
public final class PatchouliFormattingConverter {

    public record Result(String markdown, List<String> warnings) {}

    private static final Pattern TOKEN = Pattern.compile("\\$\\(([^)]*)\\)");

    private PatchouliFormattingConverter() {}

    public static Result convert(String input) {
        if (input == null || input.isEmpty()) {
            return new Result(input == null ? "" : input, List.of());
        }
        List<String> warnings = new ArrayList<>();
        StringBuilder out = new StringBuilder();
        boolean bold = false;
        boolean italic = false;
        String linkTarget = null;

        Matcher m = TOKEN.matcher(input);
        int last = 0;
        while (m.find()) {
            out.append(input, last, m.start());
            last = m.end();
            String tok = m.group(1).trim();

            // Link open: $(l:target)
            if (tok.startsWith("l:")) {
                String target = tok.substring(2).trim();
                // Markdown link inside emphasis is fine; close emphasis is handled at /l.
                linkTarget = target;
                out.append('[');
                warnings.add("entry link to '" + target + "' converted to entry://witchery:" + target
                        + " — verify target during migration");
                continue;
            }

            switch (tok) {
                case "br" -> out.append('\n');
                case "br2", "p", "2br" -> out.append("\n\n");
                case "li" -> out.append("\n- ");
                // bold sources
                case "l", "bold" -> { if (!bold) { out.append("**"); bold = true; } }
                case "item", "thing" -> { if (!bold) { out.append("**"); bold = true; } }
                // italic sources
                case "o", "italic", "italics" -> { if (!italic) { out.append('*'); italic = true; } }
                // strikethrough / underline (Modonomicon bundles commonmark gfm-strikethrough + ins)
                case "m", "strike", "strikethrough" -> out.append("~~");
                case "n", "underline" -> out.append("++");
                // link close
                case "/l" -> {
                    if (italic) { out.append('*'); italic = false; }
                    if (bold) { out.append("**"); bold = false; }
                    if (linkTarget != null) {
                        out.append("](entry://witchery:").append(linkTarget).append(')');
                        linkTarget = null;
                    }
                }
                // resets: close any open emphasis (innermost first)
                case "", "/", "r", "reset", "nocolor" -> {
                    if (italic) { out.append('*'); italic = false; }
                    if (bold) { out.append("**"); bold = false; }
                }
                default -> warnings.add("unhandled token $(" + tok + ") dropped");
            }
        }
        out.append(input, last, input.length());

        // Close anything left open at end-of-string.
        if (italic) out.append('*');
        if (bold) out.append("**");
        if (linkTarget != null) {
            out.append("](entry://witchery:").append(linkTarget).append(')');
        }
        return new Result(out.toString(), warnings);
    }
}
