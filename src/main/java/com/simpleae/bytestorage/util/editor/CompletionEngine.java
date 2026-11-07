package com.simpleae.bytestorage.util.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Подсказки автодополнения: глобальные имена и ключевые слова, члены объектов API после точки,
 * идентификаторы предметов внутри строк (через внешний поставщик). Чистая логика — покрыта тестами.
 */
public final class CompletionEngine {

    public static final int MAX_SUGGESTIONS = 40;
    private static final int MAX_ITEM_SUGGESTIONS = 40;

    /** @param qualifier "" — глобальное имя, иначе имя объекта ("net" для net.count) */
    public record Entry(String qualifier, String name, String detail, boolean call) {
    }

    /** @param replaceFrom с какой позиции текста подставляется {@code insert} (до курсора) */
    public record Suggestion(String label, String insert, String detail, int replaceFrom) {
    }

    private static final List<String> KEYWORD_HINTS = List.of(
            "function", "var", "let", "const", "return", "if", "else", "for", "while", "break", "continue",
            "switch", "case", "default", "try", "catch", "throw", "new", "typeof", "true", "false", "null", "undefined");

    private final List<Entry> entries;
    private final Set<String> globals;
    private final Function<String, List<String>> stringProvider;

    public CompletionEngine(List<Entry> entries, Set<String> globals, Function<String, List<String>> stringProvider) {
        this.entries = entries;
        this.globals = globals;
        this.stringProvider = stringProvider;
    }

    public List<Suggestion> suggest(String text, int cursor, boolean force) {
        int pos = Math.max(0, Math.min(cursor, text.length()));
        // Где стоит курсор: в комментарии, в строке или в коде
        List<List<JsLexer.Token>> lines = JsLexer.tokenize(text, globals);
        int line = 0;
        int lineStart = 0;
        for (int i = 0; i < pos; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                lineStart = i + 1;
            }
        }
        int col = pos - lineStart;
        if (line < lines.size()) {
            for (JsLexer.Token t : lines.get(line)) {
                if (t.kind() == JsLexer.Kind.COMMENT && t.start() < col && col <= t.end()) {
                    return List.of();
                }
                if (t.kind() == JsLexer.Kind.STRING && t.start() < col && col <= t.end()) {
                    boolean closed = t.end() - t.start() >= 2
                            && text.charAt(lineStart + t.end() - 1) == text.charAt(lineStart + t.start());
                    if (closed && col == t.end()) {
                        break; // курсор сразу после закрывающей кавычки
                    }
                    return itemSuggestions(text, lineStart + t.start() + 1, pos, force);
                }
            }
        }

        int j = pos;
        while (j > 0 && BufferChars.isIdentPart(text.charAt(j - 1))) {
            j--;
        }
        String prefix = text.substring(j, pos);
        List<Suggestion> result = new ArrayList<>();
        if (j > 0 && text.charAt(j - 1) == '.') {
            int q = j - 1;
            int qs = q;
            while (qs > 0 && BufferChars.isIdentPart(text.charAt(qs - 1))) {
                qs--;
            }
            String qualifier = text.substring(qs, q);
            if (qualifier.isEmpty()) {
                return List.of();
            }
            boolean known = entries.stream().anyMatch(e -> e.qualifier().equals(qualifier));
            // Неизвестная переменная: если в скрипте есть side(...), считаем её портом ("ep")
            String effective = known ? qualifier : (text.contains("side(") ? "ep" : qualifier);
            for (Entry e : entries) {
                if (e.qualifier().equals(effective) && matches(e.name(), prefix)) {
                    result.add(toSuggestion(e, text, pos, j));
                }
            }
            sort(result, prefix);
            return limit(result, prefix);
        }
        if (prefix.length() < 2 && !force) {
            return List.of();
        }
        for (Entry e : entries) {
            if (e.qualifier().isEmpty() && matches(e.name(), prefix)) {
                result.add(toSuggestion(e, text, pos, j));
            }
        }
        for (String k : KEYWORD_HINTS) {
            if (matches(k, prefix) && result.stream().noneMatch(s -> s.label().equals(k))) {
                result.add(new Suggestion(k, k, "keyword", j));
            }
        }
        sort(result, prefix);
        return limit(result, prefix);
    }

    private List<Suggestion> itemSuggestions(String text, int contentStart, int pos, boolean force) {
        if (stringProvider == null || pos < contentStart) {
            return List.of();
        }
        String prefix = text.substring(contentStart, pos);
        if (prefix.length() < 2 && !force) {
            return List.of();
        }
        List<Suggestion> result = new ArrayList<>();
        for (String id : stringProvider.apply(prefix)) {
            if (result.size() >= MAX_ITEM_SUGGESTIONS) {
                break;
            }
            result.add(new Suggestion(id, id, "item", contentStart));
        }
        if (result.size() == 1 && result.get(0).label().equals(prefix)) {
            return List.of();
        }
        return result;
    }

    private static boolean matches(String name, String prefix) {
        return prefix.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }

    private static Suggestion toSuggestion(Entry e, String text, int pos, int from) {
        boolean parenFollows = pos < text.length() && text.charAt(pos) == '(';
        String insert = e.call() && !parenFollows ? e.name() + "(" : e.name();
        return new Suggestion(e.name(), insert, e.detail(), from);
    }

    private static void sort(List<Suggestion> list, String prefix) {
        list.sort(Comparator.<Suggestion, Boolean>comparing(s -> !s.label().startsWith(prefix))
                .thenComparing(s -> s.label().toLowerCase(Locale.ROOT)));
    }

    private static List<Suggestion> limit(List<Suggestion> list, String prefix) {
        if (list.size() == 1 && list.get(0).label().equals(prefix)) {
            return List.of();
        }
        return list.size() > MAX_SUGGESTIONS ? new ArrayList<>(list.subList(0, MAX_SUGGESTIONS)) : list;
    }

    /** Мелкие помощники символов без зависимости от TextBuffer. */
    private static final class BufferChars {
        static boolean isIdentPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$';
        }
    }
}
