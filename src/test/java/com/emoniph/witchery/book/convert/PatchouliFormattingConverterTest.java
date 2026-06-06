package com.emoniph.witchery.book.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emoniph.witchery.book.convert.PatchouliFormattingConverter.Result;
import org.junit.jupiter.api.Test;

/** Golden-string tests for the Patchouli {@code $()} → Modonomicon Markdown converter. */
class PatchouliFormattingConverterTest {

    private static String md(String in) {
        return PatchouliFormattingConverter.convert(in).markdown();
    }

    @Test
    void plainTextUnchanged() {
        assertEquals("A heated cauldron.", md("A heated cauldron."));
    }

    @Test
    void paragraphAndLineBreaks() {
        assertEquals("one\n\ntwo", md("one$(br2)two"));
        assertEquals("one\ntwo", md("one$(br)two"));
    }

    @Test
    void boldViaLAndBoldAndReset() {
        assertEquals("Use **Glowstone** here.", md("Use $(l)Glowstone$() here."));
        assertEquals("Use **Glowstone** here.", md("Use $(bold)Glowstone$(/) here."));
    }

    @Test
    void itemAndThingBecomeBold() {
        assertEquals("Add **redstone** now.", md("Add $(item)redstone$() now."));
        assertEquals("the **forest** grows", md("the $(thing)forest$() grows"));
    }

    @Test
    void italic() {
        assertEquals("add these *before* it", md("add these $(o)before$() it"));
    }

    @Test
    void listItem() {
        assertEquals("Sizes:\n- 7x7\n- 11x11", md("Sizes:$(li)7x7$(li)11x11"));
    }

    @Test
    void entryLinkWithWarning() {
        Result r = PatchouliFormattingConverter.convert("see $(l:circle_magic/rites)the rites$(/l).");
        assertEquals("see [the rites](entry://witchery:circle_magic/rites).", r.markdown());
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("circle_magic/rites")));
    }

    @Test
    void unknownTokenDroppedWithWarning() {
        Result r = PatchouliFormattingConverter.convert("x$(k)y");
        assertEquals("xy", r.markdown());
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("$(k)")));
    }

    @Test
    void unclosedEmphasisClosedAtEnd() {
        assertEquals("tail **bold**", md("tail $(l)bold"));
        assertEquals("tail *italic*", md("tail $(o)italic"));
    }

    @Test
    void nestedItalicInsideBoldClosesInOrderOnReset() {
        // bold then italic, single reset closes both (italic innermost first)
        assertEquals("**a*b***", md("$(l)a$(o)b$()"));
    }
}
