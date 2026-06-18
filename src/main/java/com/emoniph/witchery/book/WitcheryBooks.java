package com.emoniph.witchery.book;

import com.emoniph.witchery.WitcheryMod;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

    /** Horizontal text padding so prose does not run hard against the page edges. */
    public static final int TEXT_OFFSET_X = 6;
    public static final int TEXT_OFFSET_WIDTH = -12;

    /**
     * Wraps body text in a Modonomicon markdown colour span so it renders in the sepia ink.
     * Uses the {@code [#](rrggbb)…[#]()} link syntax handled by Modonomicon's {@code ColorLinkRenderer};
     * only works on string (markdown) text, not literal Components. Bold ({@code **x**}) / italic
     * ({@code *x*}) inside the span render normally.
     *
     * <p>Modonomicon's {@code CoreComponentNodeRenderer} has no {@code visit(Paragraph)} handling, so
     * CommonMark paragraphs separated by a blank line are concatenated into one component with
     * <em>nothing</em> between them — no newline and no space (verified against the decompiled
     * renderer). On its own that produces run-on text like "Imprisonment.Cast" when two paragraphs
     * land on the same page. {@link #prose(String, String)} normally hides this by flushing each
     * paragraph onto its own page, but paragraphs short enough to share a page still ran together.
     * To make the join read correctly we collapse each {@code \n\n} into a single space here; a true
     * blank-line gap cannot be rendered on a shared page by this engine, and a space matches how the
     * surrounding prose already flows.
     */
    public static String ink(String text) {
        StringBuilder sb = new StringBuilder();
        for (String paragraph : text.split("\n\n", -1)) {
            if (paragraph.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append("[#](").append(INK_HEX).append(')').append(paragraph).append("[#]()");
        }
        if (sb.length() == 0) {
            sb.append("[#](").append(INK_HEX).append(')').append("[#]()");
        }
        return sb.toString();
    }

    // Modonomicon does not auto-paginate; text longer than a page spills past the book edge.
    // Every prose page now reserves a title row (real title on the first page, a blank spacer on the
    // rest), and the horizontal padding narrows the line. This budget leaves bottom breathing room
    // while using the page instead of forcing short paragraphs onto separate leaves.
    private static final int PAGE_CHARS = 250;
    // A single sentence is kept whole on its own page up to this length (still within a page's real
    // height); only sentences longer than this are broken at word boundaries. Keeping near-page-length
    // sentences intact avoids stranding a tiny trailing word ("it.") on the next page.
    private static final int SENTENCE_KEEP_MAX = PAGE_CHARS;
    // Keep numbered headings such as "**2. Herbology**" whole instead of splitting after "2.".
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[!?])\\s+|(?<=\\.)(?<!\\d\\.)\\s+");

    /**
     * Splits prose into as many sepia text pages as needed to fit the page height, breaking at
     * sentence boundaries — and at word boundaries within a sentence too long for one page, so
     * nothing ever overflows. A paragraph break ({@code \n\n}) prefers to start a new page; when two
     * paragraphs are short enough to share a page, {@link #ink(String)} joins them with a space
     * (this engine cannot render a blank-line gap mid-page). The first page carries the optional
     * {@code title}. Use this for all body prose.
     */
    public static List<BookPageModel<?>> prose(String title, String body) {
        List<String> pageTexts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : body.split("\n\n", -1)) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            boolean firstPieceInParagraph = true;
            for (String sentence : SENTENCE_BOUNDARY.split(trimmed)) {
                for (String piece : splitToBudget(sentence)) {
                    String separator = current.length() == 0 ? "" : firstPieceInParagraph ? "\n\n" : " ";
                    if (current.length() > 0 && current.length() + separator.length() + piece.length() > PAGE_CHARS) {
                        pageTexts.add(current.toString());
                        current.setLength(0);
                        separator = "";
                    }
                    if (!separator.isEmpty()) {
                        current.append(separator);
                    }
                    current.append(piece);
                    firstPieceInParagraph = false;
                }
            }
        }
        if (current.length() > 0) {
            pageTexts.add(current.toString());
        }

        List<BookPageModel<?>> pages = new ArrayList<>();
        for (int i = 0; i < pageTexts.size(); i++) {
            BookTextPageModel page = BookTextPageModel.create().withText(ink(pageTexts.get(i)));
            if (i == 0 && title != null) {
                page.withTitle(Component.literal(title));
            } else {
                // Modonomicon starts a title-less page's text at the very top line, which reads as
                // "too high". Give continuation pages a blank, separator-less title so they reserve
                // the title row and their body begins at the same height as a titled page.
                page.withTitle(Component.literal(" ")).withShowTitleSeparator(false);
            }
            pages.add(page);
        }
        return pages;
    }

    /**
     * Builds a single sepia text page that renders {@code items} as a Modonomicon ordered list — each
     * item on its own line with an auto-incrementing "N." counter.
     *
     * <p>Authored as CommonMark ordered-list markdown (the "N." marker sits at the start of each line)
     * so Modonomicon's list handling finalizes a component per item and the page renderer draws a real
     * line break between them. A plain prose page cannot do this: {@link #prose} splits on sentence
     * boundaries (period + any whitespace, newlines included) and rejoins with spaces, and this engine
     * renders neither {@code \n\n} nor a soft {@code \n} as a visible break. Each item's text is colour-
     * wrapped individually so the list marker stays at the line start where CommonMark needs it.
     *
     * <p>The first page of an entry should carry the entry title; pass {@code null} here for a
     * continuation page, which then reserves a blank spacer title row exactly like {@link #prose}.
     */
    public static BookPageModel<?> orderedListPage(String title, List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(i + 1).append(". ").append(ink(items.get(i)));
        }
        BookTextPageModel page = BookTextPageModel.create().withText(sb.toString());
        if (title != null) {
            page.withTitle(Component.literal(title));
        } else {
            page.withTitle(Component.literal(" ")).withShowTitleSeparator(false);
        }
        return page;
    }

    /** Breaks a string too long for one page into word-boundary pieces that each fit the budget. */
    private static List<String> splitToBudget(String text) {
        if (text.length() <= SENTENCE_KEEP_MAX) {
            return List.of(text);
        }
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String token : splitBudgetTokens(text)) {
            if (current.length() > 0 && current.length() + 1 + token.length() > PAGE_CHARS) {
                pieces.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(token);
        }
        if (current.length() > 0) {
            pieces.add(current.toString());
        }
        return pieces;
    }

    private static List<String> splitBudgetTokens(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inBold = false;
        for (String word : text.split(" ")) {
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
            inBold ^= hasOddDelimiterCount(word, "**");
            if (!inBold) {
                tokens.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static boolean hasOddDelimiterCount(String text, String delimiter) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(delimiter, index)) >= 0) {
            count++;
            index += delimiter.length();
        }
        return (count & 1) == 1;
    }
}
