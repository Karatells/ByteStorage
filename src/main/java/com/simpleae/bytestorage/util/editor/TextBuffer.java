package com.simpleae.bytestorage.util.editor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель текста редактора кода: текст, курсор, выделение, отмена/повтор, автоотступы, автозакрытие скобок.
 * Чистая логика без Minecraft — покрыта тестами.
 */
public final class TextBuffer {

    private static final int MAX_UNDO = 200;
    private static final String OPENERS = "([{\"'";
    private static final String CLOSERS = ")]}\"'";

    private record Snapshot(String text, int cursor, int anchor) {
    }

    private StringBuilder text = new StringBuilder();
    private int cursor;
    private int anchor;
    private int preferredColumn = -1;
    private int maxLength = Integer.MAX_VALUE;
    private boolean lastWasTyping;
    private final ArrayDeque<Snapshot> undo = new ArrayDeque<>();
    private final ArrayDeque<Snapshot> redo = new ArrayDeque<>();

    // --- Содержимое ---

    public String text() {
        return text.toString();
    }

    public int length() {
        return text.length();
    }

    public void setMaxLength(int max) {
        this.maxLength = Math.max(1, max);
    }

    /** Заменяет весь текст (например, пришёл скрипт с сервера); история отмены очищается. */
    public void setText(String value) {
        text = new StringBuilder(normalize(value == null ? "" : value));
        cursor = Math.min(cursor, text.length());
        anchor = cursor;
        preferredColumn = -1;
        undo.clear();
        redo.clear();
        lastWasTyping = false;
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "  ");
    }

    // --- Курсор и выделение ---

    public int cursor() {
        return cursor;
    }

    public int anchor() {
        return anchor;
    }

    public boolean hasSelection() {
        return cursor != anchor;
    }

    public int selStart() {
        return Math.min(cursor, anchor);
    }

    public int selEnd() {
        return Math.max(cursor, anchor);
    }

    public String selectedText() {
        return text.substring(selStart(), selEnd());
    }

    public void selectAll() {
        anchor = 0;
        cursor = text.length();
        preferredColumn = -1;
        lastWasTyping = false;
    }

    public void setCursor(int index, boolean select) {
        cursor = Math.max(0, Math.min(text.length(), index));
        if (!select) {
            anchor = cursor;
        }
        preferredColumn = -1;
        lastWasTyping = false;
    }

    // --- Строки ---

    public int lineCount() {
        int n = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    public int lineStart(int line) {
        int index = 0;
        for (int l = 0; l < line; l++) {
            int nl = text.indexOf("\n", index);
            if (nl < 0) {
                return text.length();
            }
            index = nl + 1;
        }
        return index;
    }

    public int lineEnd(int line) {
        int nl = text.indexOf("\n", lineStart(line));
        return nl < 0 ? text.length() : nl;
    }

    public String lineText(int line) {
        return text.substring(lineStart(line), lineEnd(line));
    }

    public int lineOf(int index) {
        int line = 0;
        int limit = Math.min(index, text.length());
        for (int i = 0; i < limit; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    public int columnOf(int index) {
        int limit = Math.min(index, text.length());
        int col = 0;
        for (int i = limit - 1; i >= 0 && text.charAt(i) != '\n'; i--) {
            col++;
        }
        return col;
    }

    public List<String> lines() {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (true) {
            int nl = text.indexOf("\n", start);
            if (nl < 0) {
                result.add(text.substring(start));
                return result;
            }
            result.add(text.substring(start, nl));
            start = nl + 1;
        }
    }

    // --- Движение ---

    public void moveLeft(boolean select) {
        if (hasSelection() && !select) {
            setCursor(selStart(), false);
        } else {
            setCursor(cursor - 1, select);
        }
    }

    public void moveRight(boolean select) {
        if (hasSelection() && !select) {
            setCursor(selEnd(), false);
        } else {
            setCursor(cursor + 1, select);
        }
    }

    public void moveUp(boolean select) {
        moveLines(-1, select);
    }

    public void moveDown(boolean select) {
        moveLines(1, select);
    }

    /** Сдвиг на {@code delta} строк с сохранением желаемой колонки. */
    public void moveLines(int delta, boolean select) {
        int line = lineOf(cursor);
        int col = preferredColumn >= 0 ? preferredColumn : columnOf(cursor);
        int target = line + delta;
        int last = lineCount() - 1;
        int index;
        if (target < 0) {
            index = 0;
        } else if (target > last) {
            index = text.length();
        } else {
            index = lineStart(target) + Math.min(col, lineEnd(target) - lineStart(target));
        }
        int keep = col;
        cursor = index;
        if (!select) {
            anchor = cursor;
        }
        preferredColumn = keep;
        lastWasTyping = false;
    }

    /** Home: сначала к первому непробельному символу, повторно — к началу строки. */
    public void moveHome(boolean select) {
        int line = lineOf(cursor);
        int start = lineStart(line);
        int firstText = start;
        while (firstText < lineEnd(line) && text.charAt(firstText) == ' ') {
            firstText++;
        }
        setCursor(cursor == firstText ? start : firstText, select);
    }

    public void moveEnd(boolean select) {
        setCursor(lineEnd(lineOf(cursor)), select);
    }

    public void moveWord(int direction, boolean select) {
        int i = cursor;
        if (direction < 0) {
            while (i > 0 && !isWordChar(text.charAt(i - 1)) && text.charAt(i - 1) != '\n') {
                i--;
            }
            while (i > 0 && isWordChar(text.charAt(i - 1))) {
                i--;
            }
            if (i == cursor && i > 0) {
                i--;
            }
        } else {
            while (i < text.length() && !isWordChar(text.charAt(i)) && text.charAt(i) != '\n') {
                i++;
            }
            while (i < text.length() && isWordChar(text.charAt(i))) {
                i++;
            }
            if (i == cursor && i < text.length()) {
                i++;
            }
        }
        setCursor(i, select);
    }

    public static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    // --- Правки ---

    private void checkpoint(boolean typing) {
        if (typing && lastWasTyping) {
            return;
        }
        undo.addLast(new Snapshot(text.toString(), cursor, anchor));
        while (undo.size() > MAX_UNDO) {
            undo.removeFirst();
        }
        redo.clear();
        lastWasTyping = typing;
    }

    private void removeSelection() {
        if (hasSelection()) {
            int s = selStart();
            text.delete(s, selEnd());
            cursor = s;
            anchor = s;
        }
    }

    private String fit(String s, int removing) {
        int room = maxLength - (text.length() - removing);
        if (room <= 0) {
            return "";
        }
        return s.length() > room ? s.substring(0, room) : s;
    }

    /** Вставка текста на место выделения (вставка из буфера обмена). */
    public void insert(String s) {
        String clean = normalize(s);
        if (clean.isEmpty()) {
            return;
        }
        checkpoint(false);
        String part = fit(clean, selEnd() - selStart());
        removeSelection();
        text.insert(cursor, part);
        cursor += part.length();
        anchor = cursor;
        preferredColumn = -1;
    }

    /** Заменяет диапазон [from, to) одним шагом отмены (принятие подсказки). */
    public void replaceRange(int from, int to, String s) {
        int a = Math.max(0, Math.min(from, text.length()));
        int b = Math.max(a, Math.min(to, text.length()));
        checkpoint(false);
        String part = fit(normalize(s), b - a);
        text.replace(a, b, part);
        cursor = a + part.length();
        anchor = cursor;
        preferredColumn = -1;
    }

    /** Ввод символа с автозакрытием скобок/кавычек и «перешагиванием» через закрывающий символ. */
    public void typeChar(char c) {
        if (c == '\n') {
            newline();
            return;
        }
        if (c < 32) {
            return;
        }
        int closeIdx = CLOSERS.indexOf(c);
        // Перешагнуть закрывающий символ, если он уже стоит справа
        if (!hasSelection() && closeIdx >= 0 && cursor < text.length() && text.charAt(cursor) == c) {
            setCursor(cursor + 1, false);
            return;
        }
        int openIdx = OPENERS.indexOf(c);
        if (openIdx >= 0) {
            char close = CLOSERS.charAt(openIdx);
            if (hasSelection()) {
                // Обернуть выделение
                checkpoint(false);
                int s = selStart();
                String inner = selectedText();
                text.delete(s, selEnd());
                text.insert(s, c + inner + close);
                anchor = s + 1;
                cursor = s + 1 + inner.length();
                return;
            }
            boolean quote = c == '"' || c == '\'';
            char prev = cursor > 0 ? text.charAt(cursor - 1) : ' ';
            char next = cursor < text.length() ? text.charAt(cursor) : ' ';
            boolean nextOk = next == ' ' || next == '\n' || CLOSERS.indexOf(next) >= 0 && !(quote && isWordChar(next)) || next == ';' || next == ',';
            boolean allow = quote ? !isWordChar(prev) && !isWordChar(next) : nextOk;
            if (allow && text.length() + 2 <= maxLength) {
                checkpoint(false);
                text.insert(cursor, "" + c + close);
                cursor++;
                anchor = cursor;
                preferredColumn = -1;
                return;
            }
        }
        checkpoint(true);
        String part = fit(String.valueOf(c), selEnd() - selStart());
        removeSelection();
        text.insert(cursor, part);
        cursor += part.length();
        anchor = cursor;
        preferredColumn = -1;
    }

    /** Enter: сохраняет отступ строки, добавляет уровень после {, ( или [; между {} раскрывает блок. */
    public void newline() {
        checkpoint(false);
        removeSelection();
        int line = lineOf(cursor);
        int ls = lineStart(line);
        int indentEnd = ls;
        while (indentEnd < text.length() && text.charAt(indentEnd) == ' ' && indentEnd < cursor) {
            indentEnd++;
        }
        String indent = text.substring(ls, indentEnd);
        char before = cursor > 0 ? text.charAt(cursor - 1) : ' ';
        char after = cursor < text.length() ? text.charAt(cursor) : ' ';
        boolean opens = before == '{' || before == '(' || before == '[';
        boolean between = opens && (after == '}' || after == ')' || after == ']');
        if (between) {
            String ins = "\n" + indent + "  " + "\n" + indent;
            int mid = cursor + 1 + indent.length() + 2;
            if (text.length() + ins.length() <= maxLength) {
                text.insert(cursor, ins);
                cursor = mid;
            }
        } else {
            String ins = "\n" + indent + (opens ? "  " : "");
            if (text.length() + ins.length() <= maxLength) {
                text.insert(cursor, ins);
                cursor += ins.length();
            }
        }
        anchor = cursor;
        preferredColumn = -1;
    }

    public void backspace() {
        if (hasSelection()) {
            checkpoint(false);
            removeSelection();
            return;
        }
        if (cursor == 0) {
            return;
        }
        checkpoint(true);
        char prev = text.charAt(cursor - 1);
        int oi = OPENERS.indexOf(prev);
        if (oi >= 0 && cursor < text.length() && text.charAt(cursor) == CLOSERS.charAt(oi)) {
            text.delete(cursor - 1, cursor + 1);
        } else {
            text.deleteCharAt(cursor - 1);
        }
        cursor--;
        anchor = cursor;
        preferredColumn = -1;
    }

    public void delete() {
        if (hasSelection()) {
            checkpoint(false);
            removeSelection();
            return;
        }
        if (cursor >= text.length()) {
            return;
        }
        checkpoint(true);
        text.deleteCharAt(cursor);
        preferredColumn = -1;
    }

    /** Tab: с выделением — отступ всем строкам, без — пробелы до следующей позиции кратной 2. */
    public void tab() {
        if (hasSelection() && lineOf(selStart()) != lineOf(selEnd())) {
            indentSelectedLines(true);
            return;
        }
        int spaces = 2 - (columnOf(cursor) % 2);
        insert(" ".repeat(spaces));
    }

    public void shiftTab() {
        indentSelectedLines(false);
    }

    private void indentSelectedLines(boolean add) {
        int firstLine = lineOf(selStart());
        int lastLine = lineOf(hasSelection() ? Math.max(selStart(), selEnd() - 1) : cursor);
        checkpoint(false);
        int startAnchor = anchor;
        int startCursor = cursor;
        int deltaTotal = 0;
        for (int line = firstLine; line <= lastLine; line++) {
            int ls = lineStart(line);
            if (add) {
                if (lineEnd(line) > ls) {
                    text.insert(ls, "  ");
                    deltaTotal += 2;
                    startAnchor = shift(startAnchor, ls, 2);
                    startCursor = shift(startCursor, ls, 2);
                }
            } else {
                int remove = 0;
                while (remove < 2 && ls + remove < lineEnd(line) && text.charAt(ls + remove) == ' ') {
                    remove++;
                }
                if (remove > 0) {
                    text.delete(ls, ls + remove);
                    deltaTotal -= remove;
                    startAnchor = shift(startAnchor, ls, -remove);
                    startCursor = shift(startCursor, ls, -remove);
                }
            }
        }
        anchor = Math.max(0, Math.min(text.length(), startAnchor));
        cursor = Math.max(0, Math.min(text.length(), startCursor));
    }

    private static int shift(int pos, int at, int delta) {
        if (delta > 0) {
            return pos >= at ? pos + delta : pos;
        }
        int removed = -delta;
        if (pos <= at) {
            return pos;
        }
        return Math.max(at, pos - removed);
    }

    /** Ctrl+/: закомментировать или раскомментировать выделенные (или текущую) строки. */
    public void toggleComment() {
        int firstLine = lineOf(selStart());
        int lastLine = lineOf(hasSelection() ? Math.max(selStart(), selEnd() - 1) : cursor);
        boolean allCommented = true;
        boolean any = false;
        for (int line = firstLine; line <= lastLine; line++) {
            String t = lineText(line).stripLeading();
            if (t.isEmpty()) {
                continue;
            }
            any = true;
            if (!t.startsWith("//")) {
                allCommented = false;
            }
        }
        if (!any) {
            return;
        }
        checkpoint(false);
        int startAnchor = anchor;
        int startCursor = cursor;
        for (int line = firstLine; line <= lastLine; line++) {
            int ls = lineStart(line);
            String t = lineText(line);
            if (t.stripLeading().isEmpty()) {
                continue;
            }
            int indent = t.length() - t.stripLeading().length();
            if (allCommented) {
                int len = t.startsWith("// ", indent) ? 3 : 2;
                text.delete(ls + indent, ls + indent + len);
                startAnchor = shift(startAnchor, ls + indent, -len);
                startCursor = shift(startCursor, ls + indent, -len);
            } else {
                text.insert(ls + indent, "// ");
                startAnchor = shift(startAnchor, ls + indent, 3);
                startCursor = shift(startCursor, ls + indent, 3);
            }
        }
        anchor = Math.max(0, Math.min(text.length(), startAnchor));
        cursor = Math.max(0, Math.min(text.length(), startCursor));
    }

    /** Вырезать: возвращает вырезанный текст ("" — выделения не было). */
    public String cut() {
        if (!hasSelection()) {
            return "";
        }
        String s = selectedText();
        checkpoint(false);
        removeSelection();
        return s;
    }

    // --- Отмена ---

    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        redo.addLast(new Snapshot(text.toString(), cursor, anchor));
        restore(undo.removeLast());
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        undo.addLast(new Snapshot(text.toString(), cursor, anchor));
        restore(redo.removeLast());
        return true;
    }

    private void restore(Snapshot s) {
        text = new StringBuilder(s.text());
        cursor = Math.min(s.cursor(), text.length());
        anchor = Math.min(s.anchor(), text.length());
        preferredColumn = -1;
        lastWasTyping = false;
    }
}
