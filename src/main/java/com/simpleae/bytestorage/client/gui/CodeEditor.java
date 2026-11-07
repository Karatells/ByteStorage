package com.simpleae.bytestorage.client.gui;

import com.simpleae.bytestorage.util.editor.CompletionEngine;
import com.simpleae.bytestorage.util.editor.JsLexer;
import com.simpleae.bytestorage.util.editor.TextBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Редактор кода: номера строк, подсветка синтаксиса JavaScript, выделение, отмена/повтор,
 * автоотступы, автозакрытие скобок и всплывающее автодополнение (Ctrl+Пробел — принудительно).
 */
public class CodeEditor extends AbstractWidget {

    private static final int LINE_H = 9;
    private static final int POPUP_ROWS = 8;

    private static final Map<JsLexer.Kind, Integer> COLORS = new EnumMap<>(JsLexer.Kind.class);

    static {
        COLORS.put(JsLexer.Kind.PLAIN, 0xFFE8F4FF);
        COLORS.put(JsLexer.Kind.KEYWORD, 0xFFC792EA);
        COLORS.put(JsLexer.Kind.STRING, 0xFFA5E075);
        COLORS.put(JsLexer.Kind.NUMBER, 0xFFF78C6C);
        COLORS.put(JsLexer.Kind.COMMENT, 0xFF5C6B7A);
        COLORS.put(JsLexer.Kind.FUNCTION, 0xFFFFD866);
        COLORS.put(JsLexer.Kind.CALL, 0xFF82AAFF);
        COLORS.put(JsLexer.Kind.PROPERTY, 0xFF9FD8E8);
        COLORS.put(JsLexer.Kind.GLOBAL, 0xFF00D8F0);
        COLORS.put(JsLexer.Kind.PUNCT, 0xFF8A98AA);
    }

    private final Font font;
    private final TextBuffer buf = new TextBuffer();
    private CompletionEngine engine;
    private Set<String> globals = Set.of();

    private int scrollLine;
    private int scrollX;
    private boolean dragging;
    private long blinkBase = System.currentTimeMillis();

    private String tokenText;
    private List<List<JsLexer.Token>> tokens = List.of();

    private List<CompletionEngine.Suggestion> suggestions = List.of();
    private int selectedSuggestion;
    private int suggestionScroll;

    public CodeEditor(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.font = font;
    }

    public void setCompletion(CompletionEngine engine, Set<String> globals) {
        this.engine = engine;
        this.globals = globals;
    }

    public String getValue() {
        return buf.text();
    }

    public void setValue(String value) {
        buf.setText(value);
        buf.setCursor(0, false);
        scrollLine = 0;
        scrollX = 0;
        closePopup();
    }

    public void setMaxLength(int max) {
        buf.setMaxLength(max);
    }

    public boolean isPopupOpen() {
        return !suggestions.isEmpty();
    }

    private int gutterWidth() {
        return font.width(String.valueOf(Math.max(99, buf.lineCount()))) + 8;
    }

    private int visibleLines() {
        return Math.max(1, (height - 4) / LINE_H);
    }

    private int textLeft() {
        return getX() + gutterWidth() + 3;
    }

    private int textWidth() {
        return width - gutterWidth() - 8;
    }

    // --- Рендер ---

    private void refreshTokens(String text) {
        if (!text.equals(tokenText)) {
            tokenText = text;
            tokens = JsLexer.tokenize(text, globals);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        GuiStyle.display(g, x, y, width, height);
        String text = buf.text();
        refreshTokens(text);
        List<String> lines = buf.lines();
        int gutter = gutterWidth();
        int visible = visibleLines();
        scrollLine = Math.max(0, Math.min(scrollLine, Math.max(0, lines.size() - visible)));
        int cursorLine = buf.lineOf(buf.cursor());

        // Жёлоб с номерами строк
        g.fill(x + 1, y + 1, x + gutter, y + height - 1, 0xFF101822);
        g.fill(x + gutter, y + 1, x + gutter + 1, y + height - 1, 0xFF22313F);
        for (int i = 0; i < visible; i++) {
            int line = scrollLine + i;
            if (line >= lines.size()) {
                break;
            }
            String number = String.valueOf(line + 1);
            g.drawString(font, number, x + gutter - 4 - font.width(number), y + 3 + i * LINE_H,
                    line == cursorLine && isFocused() ? GuiStyle.ACCENT : GuiStyle.TEXT_MUTED, false);
        }

        int tx = textLeft();
        g.enableScissor(tx - 1, y + 1, x + width - 1, y + height - 1);
        for (int i = 0; i < visible; i++) {
            int line = scrollLine + i;
            if (line >= lines.size()) {
                break;
            }
            int ly = y + 3 + i * LINE_H;
            if (line == cursorLine && isFocused()) {
                g.fill(tx - 1, ly - 1, x + width - 1, ly + LINE_H - 1, 0x2000D8F0);
            }
            drawSelection(g, lines.get(line), line, tx, ly);
            String lineText = lines.get(line);
            if (line < tokens.size()) {
                for (JsLexer.Token t : tokens.get(line)) {
                    int s = Math.min(t.start(), lineText.length());
                    int e = Math.min(t.end(), lineText.length());
                    if (e <= s) {
                        continue;
                    }
                    int px = tx - scrollX + font.width(lineText.substring(0, s));
                    if (px > x + width) {
                        break;
                    }
                    g.drawString(font, lineText.substring(s, e), px, ly, COLORS.get(t.kind()), false);
                }
            }
        }
        if (isFocused() && ((System.currentTimeMillis() - blinkBase) / 500) % 2 == 0) {
            int cl = cursorLine - scrollLine;
            if (cl >= 0 && cl < visible) {
                String lineText = lines.get(cursorLine);
                int col = Math.min(buf.columnOf(buf.cursor()), lineText.length());
                int cx = tx - scrollX + font.width(lineText.substring(0, col));
                g.fill(cx, y + 2 + cl * LINE_H, cx + 1, y + 2 + cl * LINE_H + LINE_H, 0xFFFFFFFF);
            }
        }
        g.disableScissor();

        // Вертикальная полоса прокрутки
        if (lines.size() > visible) {
            int trackH = height - 4;
            int thumbH = Math.max(8, trackH * visible / lines.size());
            int thumbY = y + 2 + (trackH - thumbH) * scrollLine / Math.max(1, lines.size() - visible);
            g.fill(x + width - 4, y + 2, x + width - 2, y + height - 2, 0xFF101822);
            g.fill(x + width - 4, thumbY, x + width - 2, thumbY + thumbH, GuiStyle.ACCENT_DIM);
        }
        if (isFocused()) {
            g.renderOutline(x - 1, y - 1, width + 2, height + 2, GuiStyle.ACCENT_DIM);
        }
        renderPopup(g, lines);
    }

    private void drawSelection(GuiGraphics g, String lineText, int line, int tx, int ly) {
        if (!buf.hasSelection()) {
            return;
        }
        int ls = buf.lineStart(line);
        int le = ls + lineText.length();
        int s = Math.max(buf.selStart(), ls);
        int e = Math.min(buf.selEnd(), le);
        boolean wraps = buf.selStart() <= le && buf.selEnd() > le; // выделен и перевод строки
        if (e < s || (e == s && !wraps)) {
            return;
        }
        int x1 = tx - scrollX + font.width(lineText.substring(0, s - ls));
        int x2 = tx - scrollX + font.width(lineText.substring(0, e - ls)) + (wraps ? 3 : 0);
        g.fill(x1, ly - 1, Math.max(x2, x1 + 1), ly + LINE_H - 1, 0x8800A8C0);
    }

    private void renderPopup(GuiGraphics g, List<String> lines) {
        if (suggestions.isEmpty() || !isFocused()) {
            return;
        }
        int cursorLine = buf.lineOf(buf.cursor()) - scrollLine;
        if (cursorLine < 0 || cursorLine >= visibleLines()) {
            return;
        }
        String lineText = lines.get(buf.lineOf(buf.cursor()));
        int from = suggestions.get(0).replaceFrom();
        int fromCol = Math.max(0, Math.min(from - buf.lineStart(buf.lineOf(buf.cursor())), lineText.length()));
        int px = textLeft() - scrollX + font.width(lineText.substring(0, fromCol));
        int rows = Math.min(POPUP_ROWS, suggestions.size());
        int w = 60;
        for (CompletionEngine.Suggestion s : suggestions) {
            w = Math.max(w, font.width(s.label()) + font.width(s.detail()) + 22);
        }
        w = Math.min(w, 230);
        int py = getY() + 3 + (cursorLine + 1) * LINE_H;
        int h = rows * 10 + 4;
        if (py + h > Minecraft.getInstance().getWindow().getGuiScaledHeight()) {
            py = getY() + 3 + cursorLine * LINE_H - h;
        }
        px = Math.max(2, Math.min(px, Minecraft.getInstance().getWindow().getGuiScaledWidth() - w - 2));
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(px - 1, py - 1, px + w + 1, py + h + 1, GuiStyle.PANEL_EDGE_DARK);
        g.fill(px, py, px + w, py + h, 0xFF16202C);
        for (int i = 0; i < rows; i++) {
            int index = suggestionScroll + i;
            if (index >= suggestions.size()) {
                break;
            }
            CompletionEngine.Suggestion s = suggestions.get(index);
            int ry = py + 2 + i * 10;
            if (index == selectedSuggestion) {
                g.fill(px + 1, ry - 1, px + w - 1, ry + 9, GuiStyle.ACCENT_DIM);
            }
            g.drawString(font, font.plainSubstrByWidth(s.label(), w - 8), px + 4, ry, GuiStyle.TEXT_TITLE, false);
            int dw = font.width(s.detail());
            if (px + 8 + font.width(s.label()) + dw < px + w - 2) {
                g.drawString(font, s.detail(), px + w - 4 - dw, ry, GuiStyle.TEXT_MUTED, false);
            }
        }
        g.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    // --- Подсказки ---

    private void closePopup() {
        suggestions = List.of();
        selectedSuggestion = 0;
        suggestionScroll = 0;
    }

    private void refreshPopup(boolean force) {
        if (engine == null) {
            closePopup();
            return;
        }
        List<CompletionEngine.Suggestion> found = engine.suggest(buf.text(), buf.cursor(), force);
        if (found.isEmpty()) {
            closePopup();
            return;
        }
        suggestions = found;
        selectedSuggestion = Math.min(selectedSuggestion, found.size() - 1);
        suggestionScroll = Math.max(0, Math.min(suggestionScroll, found.size() - POPUP_ROWS));
    }

    private void acceptSuggestion() {
        CompletionEngine.Suggestion s = suggestions.get(selectedSuggestion);
        buf.replaceRange(s.replaceFrom(), buf.cursor(), s.insert());
        closePopup();
        afterEdit(false);
    }

    private void afterEdit(boolean reopen) {
        blinkBase = System.currentTimeMillis();
        ensureVisible();
        if (reopen) {
            refreshPopup(false);
        } else {
            closePopup();
        }
    }

    private void ensureVisible() {
        int line = buf.lineOf(buf.cursor());
        int visible = visibleLines();
        if (line < scrollLine) {
            scrollLine = line;
        } else if (line >= scrollLine + visible) {
            scrollLine = line - visible + 1;
        }
        String lineText = buf.lineText(line);
        int col = Math.min(buf.columnOf(buf.cursor()), lineText.length());
        int cx = font.width(lineText.substring(0, col));
        int tw = Math.max(20, textWidth());
        if (cx - scrollX > tw - 6) {
            scrollX = cx - tw + 12;
        } else if (cx - scrollX < 0) {
            scrollX = Math.max(0, cx - 12);
        }
    }

    // --- Ввод ---

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) {
            return false;
        }
        boolean shift = Screen.hasShiftDown();
        boolean ctrl = Screen.hasControlDown();

        if (!suggestions.isEmpty()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_ESCAPE -> {
                    closePopup();
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    selectedSuggestion = (selectedSuggestion + 1) % suggestions.size();
                    scrollToSelection();
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    selectedSuggestion = (selectedSuggestion - 1 + suggestions.size()) % suggestions.size();
                    scrollToSelection();
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_TAB -> {
                    acceptSuggestion();
                    return true;
                }
                default -> {
                }
            }
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_SPACE) {
            refreshPopup(true);
            return true;
        }
        if (Screen.isSelectAll(keyCode)) {
            buf.selectAll();
            afterEdit(false);
            return true;
        }
        if (Screen.isCopy(keyCode)) {
            if (buf.hasSelection()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(buf.selectedText());
            }
            return true;
        }
        if (Screen.isCut(keyCode)) {
            String cut = buf.cut();
            if (!cut.isEmpty()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(cut);
            }
            afterEdit(false);
            return true;
        }
        if (Screen.isPaste(keyCode)) {
            buf.insert(Minecraft.getInstance().keyboardHandler.getClipboard());
            afterEdit(false);
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_Z) {
            if (shift) {
                buf.redo();
            } else {
                buf.undo();
            }
            afterEdit(false);
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_Y) {
            buf.redo();
            afterEdit(false);
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_SLASH) {
            buf.toggleComment();
            afterEdit(false);
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> {
                if (ctrl) {
                    buf.moveWord(-1, shift);
                } else {
                    buf.moveLeft(shift);
                }
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (ctrl) {
                    buf.moveWord(1, shift);
                } else {
                    buf.moveRight(shift);
                }
            }
            case GLFW.GLFW_KEY_UP -> buf.moveUp(shift);
            case GLFW.GLFW_KEY_DOWN -> buf.moveDown(shift);
            case GLFW.GLFW_KEY_PAGE_UP -> buf.moveLines(-visibleLines(), shift);
            case GLFW.GLFW_KEY_PAGE_DOWN -> buf.moveLines(visibleLines(), shift);
            case GLFW.GLFW_KEY_HOME -> {
                if (ctrl) {
                    buf.setCursor(0, shift);
                } else {
                    buf.moveHome(shift);
                }
            }
            case GLFW.GLFW_KEY_END -> {
                if (ctrl) {
                    buf.setCursor(buf.length(), shift);
                } else {
                    buf.moveEnd(shift);
                }
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                buf.backspace();
                afterEdit(true);
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> buf.delete();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> buf.newline();
            case GLFW.GLFW_KEY_TAB -> {
                if (shift) {
                    buf.shiftTab();
                } else {
                    buf.tab();
                }
            }
            default -> {
                return false;
            }
        }
        afterEdit(false);
        return true;
    }

    private void scrollToSelection() {
        if (selectedSuggestion < suggestionScroll) {
            suggestionScroll = selectedSuggestion;
        } else if (selectedSuggestion >= suggestionScroll + POPUP_ROWS) {
            suggestionScroll = selectedSuggestion - POPUP_ROWS + 1;
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused() || codePoint < 32 || codePoint == 127) {
            return false;
        }
        buf.typeChar(codePoint);
        afterEdit(true);
        return true;
    }

    private int indexAt(double mouseX, double mouseY) {
        int row = (int) Math.floor((mouseY - getY() - 3) / LINE_H);
        int line = Math.max(0, Math.min(buf.lineCount() - 1, scrollLine + row));
        String lineText = buf.lineText(line);
        double rel = mouseX - textLeft() + scrollX;
        int col = 0;
        while (col < lineText.length() && font.width(lineText.substring(0, col + 1)) - font.width(lineText.charAt(col) + "") / 2.0 < rel) {
            col++;
        }
        return buf.lineStart(line) + col;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != 0) {
            return false;
        }
        if (!suggestions.isEmpty() && clickPopup(mouseX, mouseY)) {
            return true;
        }
        if (!clicked(mouseX, mouseY)) {
            setFocused(false);
            closePopup();
            return false;
        }
        setFocused(true);
        buf.setCursor(indexAt(mouseX, mouseY), Screen.hasShiftDown());
        dragging = true;
        closePopup();
        blinkBase = System.currentTimeMillis();
        return true;
    }

    private boolean clickPopup(double mouseX, double mouseY) {
        // Клик по строке подсказки принимает её (границы совпадают с renderPopup)
        int cursorLine = buf.lineOf(buf.cursor()) - scrollLine;
        String lineText = buf.lineText(buf.lineOf(buf.cursor()));
        int fromCol = Math.max(0, Math.min(suggestions.get(0).replaceFrom() - buf.lineStart(buf.lineOf(buf.cursor())), lineText.length()));
        int px = textLeft() - scrollX + font.width(lineText.substring(0, fromCol));
        int w = 60;
        for (CompletionEngine.Suggestion s : suggestions) {
            w = Math.max(w, font.width(s.label()) + font.width(s.detail()) + 22);
        }
        w = Math.min(w, 230);
        int rows = Math.min(POPUP_ROWS, suggestions.size());
        int py = getY() + 3 + (cursorLine + 1) * LINE_H;
        int h = rows * 10 + 4;
        if (py + h > Minecraft.getInstance().getWindow().getGuiScaledHeight()) {
            py = getY() + 3 + cursorLine * LINE_H - h;
        }
        px = Math.max(2, Math.min(px, Minecraft.getInstance().getWindow().getGuiScaledWidth() - w - 2));
        if (mouseX >= px && mouseX < px + w && mouseY >= py && mouseY < py + h) {
            int row = (int) ((mouseY - py - 2) / 10);
            int index = suggestionScroll + row;
            if (row >= 0 && index < suggestions.size()) {
                selectedSuggestion = index;
                acceptSuggestion();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            buf.setCursor(indexAt(mouseX, mouseY), true);
            ensureVisible();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!clicked(mouseX, mouseY) && suggestions.isEmpty()) {
            return false;
        }
        if (!suggestions.isEmpty() && suggestions.size() > POPUP_ROWS) {
            suggestionScroll = Math.max(0, Math.min(suggestions.size() - POPUP_ROWS, suggestionScroll - (int) Math.signum(delta)));
            return true;
        }
        if (Screen.hasShiftDown()) {
            scrollX = Math.max(0, scrollX - (int) Math.signum(delta) * 24);
        } else {
            int max = Math.max(0, buf.lineCount() - visibleLines());
            scrollLine = Math.max(0, Math.min(max, scrollLine - (int) Math.signum(delta) * 3));
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return clicked(mouseX, mouseY);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            closePopup();
        }
    }
}
