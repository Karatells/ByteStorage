package com.simpleae.bytestorage;

import com.simpleae.bytestorage.util.editor.CompletionEngine;
import com.simpleae.bytestorage.util.editor.JsLexer;
import com.simpleae.bytestorage.util.editor.TextBuffer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorLogicTest {

    private static TextBuffer buf(String s) {
        TextBuffer b = new TextBuffer();
        b.setText(s);
        b.setCursor(s.length(), false);
        return b;
    }

    @Test
    void autoClosesBracketsAndStepsOver() {
        TextBuffer b = buf("");
        b.typeChar('(');
        assertEquals("()", b.text());
        assertEquals(1, b.cursor());
        b.typeChar(')');
        assertEquals("()", b.text());
        assertEquals(2, b.cursor());
    }

    @Test
    void backspaceRemovesPair() {
        TextBuffer b = buf("");
        b.typeChar('[');
        b.backspace();
        assertEquals("", b.text());
    }

    @Test
    void quoteNotDoubledInsideWord() {
        TextBuffer b = buf("don");
        b.typeChar('\'');
        assertEquals("don'", b.text());
    }

    @Test
    void newlineKeepsIndentAndExpandsBlock() {
        TextBuffer b = buf("  if (x) {");
        b.typeChar('\n');
        assertEquals("  if (x) {\n    ", b.text());
        TextBuffer c = buf("");
        c.typeChar('{');
        c.typeChar('\n');
        assertEquals("{\n  \n}", c.text());
        assertEquals(4, c.cursor());
    }

    @Test
    void undoRedoGroupsTyping() {
        TextBuffer b = buf("");
        b.typeChar('a');
        b.typeChar('b');
        b.typeChar('c');
        assertTrue(b.undo());
        assertEquals("", b.text());
        assertTrue(b.redo());
        assertEquals("abc", b.text());
    }

    @Test
    void movesVerticallyKeepingColumn() {
        TextBuffer b = buf("hello\nhi\nworld");
        b.setCursor(4, false);
        b.moveDown(false);
        assertEquals(8, b.cursor());
        b.moveDown(false);
        assertEquals(13, b.cursor());
    }

    @Test
    void tabIndentsSelectedLinesAndShiftTabRemoves() {
        TextBuffer b = buf("a\nb");
        b.selectAll();
        b.tab();
        assertEquals("  a\n  b", b.text());
        b.shiftTab();
        assertEquals("a\nb", b.text());
    }

    @Test
    void toggleCommentRoundTrip() {
        TextBuffer b = buf("  x = 1;\ny = 2;");
        b.selectAll();
        b.toggleComment();
        assertEquals("  // x = 1;\n// y = 2;", b.text());
        b.toggleComment();
        assertEquals("  x = 1;\ny = 2;", b.text());
    }

    @Test
    void maxLengthIsRespected() {
        TextBuffer b = buf("");
        b.setMaxLength(3);
        b.insert("abcdef");
        assertEquals("abc", b.text());
        b.typeChar('x');
        assertEquals("abc", b.text());
    }

    @Test
    void pasteNormalizesLineEndingsAndTabs() {
        TextBuffer b = buf("");
        b.insert("a\r\n\tb");
        assertEquals("a\n  b", b.text());
    }

    @Test
    void lexerClassifiesTokens() {
        List<List<JsLexer.Token>> lines = JsLexer.tokenize(
                "var n = net.count(\"a\"); // hi\nfunction tick(){}", Set.of("net"));
        assertEquals(2, lines.size());
        List<JsLexer.Token> l0 = lines.get(0);
        assertEquals(JsLexer.Kind.KEYWORD, l0.get(0).kind());
        assertEquals(JsLexer.Kind.GLOBAL, l0.get(3).kind());
        assertEquals(JsLexer.Kind.CALL, l0.get(5).kind());
        assertEquals(JsLexer.Kind.COMMENT, l0.get(l0.size() - 1).kind());
        assertEquals(JsLexer.Kind.FUNCTION, lines.get(1).get(1).kind());
    }

    @Test
    void lexerHandlesMultilineCommentAndUnclosedString() {
        List<List<JsLexer.Token>> lines = JsLexer.tokenize("/* a\nb */ x\n\"open", Set.of());
        assertEquals(3, lines.size());
        assertEquals(JsLexer.Kind.COMMENT, lines.get(0).get(0).kind());
        assertEquals(JsLexer.Kind.COMMENT, lines.get(1).get(0).kind());
        assertEquals(JsLexer.Kind.STRING, lines.get(2).get(0).kind());
    }

    private static CompletionEngine engine() {
        return new CompletionEngine(List.of(
                new CompletionEngine.Entry("", "net", "network", false),
                new CompletionEngine.Entry("net", "count", "count(id)", true),
                new CompletionEngine.Entry("net", "craft", "craft(id,n)", true),
                new CompletionEngine.Entry("stock", "set", "set()", true)),
                Set.of("net", "stock"),
                prefix -> prefix.startsWith("minecraft:co") ? List.of("minecraft:coal", "minecraft:cobblestone") : List.of());
    }

    @Test
    void completesMembersAfterDot() {
        String t = "net.c";
        List<CompletionEngine.Suggestion> s = engine().suggest(t, t.length(), false);
        assertEquals(2, s.size());
        assertEquals("count($0)", s.get(0).insert());
        assertEquals(4, s.get(0).replaceFrom());
    }

    @Test
    void completesGlobalsNeedsTwoLettersUnlessForced() {
        assertTrue(engine().suggest("n", 1, false).isEmpty());
        assertFalse(engine().suggest("n", 1, true).isEmpty());
        assertFalse(engine().suggest("ne", 2, false).isEmpty());
    }

    @Test
    void completesItemIdsInsideStrings() {
        String t = "net.count(\"minecraft:co";
        List<CompletionEngine.Suggestion> s = engine().suggest(t, t.length(), false);
        assertEquals(2, s.size());
        assertEquals("minecraft:coal", s.get(0).insert());
        assertEquals(11, s.get(0).replaceFrom());
    }

    @Test
    void noSuggestionsInComments() {
        String t = "// net.c";
        assertTrue(engine().suggest(t, t.length(), true).isEmpty());
    }

    private static CompletionEngine richEngine() {
        return new CompletionEngine(List.of(
                new CompletionEngine.Entry("", "side", "side(name, id)", true, "port"),
                new CompletionEngine.Entry("", "net", "network", false),
                new CompletionEngine.Entry("net", "list", "list()", true),
                new CompletionEngine.Entry("net", "craft", "craft(id, n)", true, "order"),
                new CompletionEngine.Entry("net", "setPriority", "setPriority(q, p)", true),
                new CompletionEngine.Entry("ep", "push", "push(id, n)", true),
                new CompletionEngine.Entry("event", "count", "count", false),
                new CompletionEngine.Entry("event", "id", "id", false)),
                Set.of("net", "side"),
                (call, arg, prefix) -> call.equals("side") ? List.of("north", "south", "up")
                        : List.of("minecraft:coal"));
    }

    @Test
    void suggestsSnippetsAndLocalSymbols() {
        String t = "var counter = 1;\nfunction helper(amount) {}\nco";
        List<CompletionEngine.Suggestion> s = richEngine().suggest(t, t.length(), false);
        assertTrue(s.stream().anyMatch(x -> x.label().equals("counter") && x.detail().equals("variable")));
        List<CompletionEngine.Suggestion> t2 = richEngine().suggest("tic", 3, false);
        assertEquals("snippet", t2.get(0).detail());
        List<CompletionEngine.Suggestion> p = richEngine().suggest("function helper(amount) { am", 28, false);
        assertTrue(p.stream().anyMatch(x -> x.label().equals("amount") && x.detail().equals("parameter")));
    }

    @Test
    void handlerParameterGetsEventFields() {
        String t = "function onCraft(e) {\n  var n = e.";
        List<CompletionEngine.Suggestion> s = richEngine().suggest(t, t.length(), false);
        assertEquals(2, s.size());
    }

    @Test
    void sideVariableGetsPortMembers() {
        String t = "var chest = side(\"up\");\nchest.pu";
        List<CompletionEngine.Suggestion> s = richEngine().suggest(t, t.length(), false);
        assertEquals("push", s.get(0).label());
    }

    @Test
    void fuzzyMatchesLettersInOrder() {
        List<CompletionEngine.Suggestion> s = richEngine().suggest("net.sp", 6, false);
        assertEquals("setPriority", s.get(0).label());
    }

    @Test
    void stringArgumentsAreContextAware() {
        String t = "side(\"";
        List<CompletionEngine.Suggestion> s = richEngine().suggest(t, t.length(), false);
        assertEquals(3, s.size());
        assertEquals("value", s.get(0).detail());
        String t2 = "net.craft(\"co";
        assertEquals("minecraft:coal", richEngine().suggest(t2, t2.length(), false).get(0).label());
    }

    @Test
    void signatureHintTracksArgument() {
        String t = "net.craft(\"minecraft:coal\", ";
        CompletionEngine.Hint h = richEngine().signature(t, t.length());
        assertEquals(1, h.active());
        assertEquals(List.of("id", "n"), h.params());
        assertTrue(richEngine().signature("net.craft(1);", 13) == null);
    }

    @Test
    void snippetKeepsIndentAndPlacesCursor() {
        TextBuffer b = buf("  ti");
        b.replaceWithSnippet(2, 4, "function tick() {\n  $0\n}");
        assertEquals("  function tick() {\n    \n  }", b.text());
        assertEquals(b.text().indexOf("\n  }"), b.cursor());
    }
}
