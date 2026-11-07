package com.simpleae.bytestorage.util.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Подсказки автодополнения: глобальные имена, сниппеты, переменные и функции самого скрипта,
 * члены объектов API после точки, значения строковых аргументов (id предметов, стороны, режимы),
 * а также подсказка сигнатуры вызова. Чистая логика — покрыта тестами.
 */
public final class CompletionEngine {

    public static final int MAX_SUGGESTIONS = 40;
    private static final int MAX_STRING_SUGGESTIONS = 40;
    private static final Pattern DECL = Pattern.compile("\\b(?:var|let|const)\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern FUNC = Pattern.compile("\\bfunction\\s+([A-Za-z_$][\\w$]*)\\s*\\(([^)]*)\\)");
    private static final Pattern EP_VAR = Pattern.compile("\\b([A-Za-z_$][\\w$]*)\\s*=\\s*side\\s*\\(");
    private static final Set<String> HANDLERS = Set.of("onCraft", "onLowStock", "onAlert");

    /** Источник значений для строковых аргументов: {@code call} — "net.craft", "side" и т.п. */
    public interface StringSource {
        List<String> complete(String call, int argIndex, String prefix);
    }

    /** @param qualifier "" — глобальное имя, иначе имя объекта ("net" для net.count) */
    public record Entry(String qualifier, String name, String detail, boolean call, String doc) {
        public Entry(String qualifier, String name, String detail, boolean call) {
            this(qualifier, name, detail, call, "");
        }
    }

    /** Подсказка для фрагмента кода. {@code insert} может содержать "$0" — позицию курсора после вставки. */
    public record Suggestion(String label, String insert, String detail, int replaceFrom, String doc) {
        public Suggestion(String label, String insert, String detail, int replaceFrom) {
            this(label, insert, detail, replaceFrom, "");
        }
    }

    /** Подсказка сигнатуры: {@code params} — параметры, {@code active} — индекс текущего аргумента. */
    public record Hint(String name, List<String> params, int active, String doc) {
    }

    private record Call(String name, int argIndex) {
    }

    private static final List<String> KEYWORD_HINTS = List.of(
            "function", "var", "let", "const", "return", "if", "else", "for", "while", "break", "continue",
            "switch", "case", "default", "try", "catch", "throw", "new", "typeof", "true", "false", "null", "undefined");

    private static final Map<String, String> SNIPPETS = new LinkedHashMap<>();

    static {
        SNIPPETS.put("tick", "function tick() {\n  $0\n}");
        SNIPPETS.put("onCraft", "function onCraft(e) {\n  $0\n}");
        SNIPPETS.put("onLowStock", "function onLowStock(e) {\n  $0\n}");
        SNIPPETS.put("onAlert", "function onAlert(e) {\n  $0\n}");
        SNIPPETS.put("onButton", "function onButton(id) {\n  $0\n}");
        SNIPPETS.put("for", "for (var i = 0; i < $0; i++) {\n  \n}");
        SNIPPETS.put("if", "if ($0) {\n  \n}");
        SNIPPETS.put("ifelse", "if ($0) {\n  \n} else {\n  \n}");
        SNIPPETS.put("each", "var items = net.list();\nfor (var i = 0; i < items.length; i++) {\n  var it = items[i];\n  $0\n}");
        SNIPPETS.put("while", "while ($0) {\n  \n}");
    }

    private final List<Entry> entries;
    private final Set<String> globals;
    private final StringSource strings;

    public CompletionEngine(List<Entry> entries, Set<String> globals, StringSource strings) {
        this.entries = entries;
        this.globals = globals;
        this.strings = strings;
    }

    public CompletionEngine(List<Entry> entries, Set<String> globals, Function<String, List<String>> stringProvider) {
        this(entries, globals, stringProvider == null ? null
                : (call, arg, prefix) -> stringProvider.apply(prefix));
    }

    // --- Основной вход ---

    public List<Suggestion> suggest(String text, int cursor, boolean force) {
        int pos = Math.max(0, Math.min(cursor, text.length()));
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
                    return stringSuggestions(text, lineStart + t.start() + 1, pos, force);
                }
            }
        }

        int j = pos;
        while (j > 0 && isIdentPart(text.charAt(j - 1))) {
            j--;
        }
        String prefix = text.substring(j, pos);
        List<Suggestion> result = new ArrayList<>();
        if (j > 0 && text.charAt(j - 1) == '.') {
            int q = j - 1;
            int qs = q;
            while (qs > 0 && isIdentPart(text.charAt(qs - 1))) {
                qs--;
            }
            String qualifier = text.substring(qs, q);
            if (qualifier.isEmpty()) {
                return List.of();
            }
            String effective = resolveQualifier(text, qualifier);
            for (Entry e : entries) {
                if (e.qualifier().equals(effective) && matches(e.name(), prefix) > 0) {
                    result.add(toSuggestion(e, text, pos, j));
                }
            }
            return rank(result, prefix);
        }
        if (prefix.length() < 2 && !force) {
            return List.of();
        }
        for (Map.Entry<String, String> s : SNIPPETS.entrySet()) {
            if (matches(s.getKey(), prefix) > 0) {
                result.add(new Suggestion(s.getKey(), s.getValue(), "snippet", j, "Insert a code template"));
            }
        }
        for (Entry e : entries) {
            if (e.qualifier().isEmpty() && matches(e.name(), prefix) > 0
                    && result.stream().noneMatch(s -> s.label().equals(e.name()))) {
                result.add(toSuggestion(e, text, pos, j));
            }
        }
        String rest = text.substring(0, j) + text.substring(pos);
        for (Map.Entry<String, String> sym : localSymbols(rest).entrySet()) {
            if (matches(sym.getKey(), prefix) > 0 && result.stream().noneMatch(s -> s.label().equals(sym.getKey()))) {
                result.add(new Suggestion(sym.getKey(), sym.getKey(), sym.getValue(), j));
            }
        }
        for (String k : KEYWORD_HINTS) {
            if (matches(k, prefix) > 0 && result.stream().noneMatch(s -> s.label().equals(k))) {
                result.add(new Suggestion(k, k, "keyword", j));
            }
        }
        return rank(result, prefix);
    }

    // --- Подсказка сигнатуры ---

    /** @return сигнатура функции, в вызове которой стоит курсор, или null */
    public Hint signature(String text, int cursor) {
        Call call = callAt(text, Math.max(0, Math.min(cursor, text.length())));
        if (call == null) {
            return null;
        }
        String qualifier = "";
        String name = call.name();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            qualifier = resolveQualifier(text, name.substring(0, dot));
            name = name.substring(dot + 1);
        }
        for (Entry e : entries) {
            if (e.call() && e.qualifier().equals(qualifier) && e.name().equals(name)) {
                List<String> params = paramsOf(e.detail());
                if (params == null) {
                    return null;
                }
                return new Hint(call.name(), params, call.argIndex(), e.doc());
            }
        }
        return null;
    }

    private static List<String> paramsOf(String detail) {
        int open = detail.indexOf('(');
        int close = detail.lastIndexOf(')');
        if (open < 0 || close < open) {
            return null;
        }
        String inner = detail.substring(open + 1, close).trim();
        List<String> params = new ArrayList<>();
        if (!inner.isEmpty()) {
            for (String p : inner.split(",")) {
                params.add(p.trim());
            }
        }
        return params;
    }

    /** Ищет назад незакрытую "(" и возвращает имя вызываемой функции и номер аргумента. */
    private static Call callAt(String text, int pos) {
        int depth = 0;
        int commas = 0;
        for (int i = pos - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == ')' || c == ']' || c == '}') {
                depth++;
            } else if (c == '[' || c == '{') {
                if (depth == 0) {
                    return null;
                }
                depth--;
            } else if (c == '(') {
                if (depth > 0) {
                    depth--;
                    continue;
                }
                int e = i;
                while (e > 0 && text.charAt(e - 1) == ' ') {
                    e--;
                }
                int s = e;
                while (s > 0 && (isIdentPart(text.charAt(s - 1)) || text.charAt(s - 1) == '.')) {
                    s--;
                }
                if (s == e) {
                    return null;
                }
                return new Call(text.substring(s, e), commas);
            } else if (c == ',' && depth == 0) {
                commas++;
            } else if (c == ';' && depth == 0) {
                return null;
            }
        }
        return null;
    }

    // --- Контекст ---

    private String resolveQualifier(String text, String qualifier) {
        if (entries.stream().anyMatch(e -> e.qualifier().equals(qualifier))) {
            return qualifier;
        }
        if (eventParams(text).contains(qualifier)) {
            return "event";
        }
        Matcher m = EP_VAR.matcher(text);
        while (m.find()) {
            if (m.group(1).equals(qualifier)) {
                return "ep";
            }
        }
        return qualifier;
    }

    private static Set<String> eventParams(String text) {
        Set<String> result = new java.util.HashSet<>();
        Matcher m = FUNC.matcher(text);
        while (m.find()) {
            if (HANDLERS.contains(m.group(1))) {
                for (String p : m.group(2).split(",")) {
                    if (!p.isBlank()) {
                        result.add(p.trim());
                    }
                }
            }
        }
        return result;
    }

    /** Имена, объявленные в скрипте: переменные, функции, параметры функций. */
    private Map<String, String> localSymbols(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        Matcher d = DECL.matcher(text);
        while (d.find()) {
            result.putIfAbsent(d.group(1), "variable");
        }
        Matcher f = FUNC.matcher(text);
        while (f.find()) {
            result.put(f.group(1), "function");
            for (String p : f.group(2).split(",")) {
                if (!p.isBlank()) {
                    result.putIfAbsent(p.trim(), "parameter");
                }
            }
        }
        for (Entry e : entries) {
            if (e.qualifier().isEmpty()) {
                result.remove(e.name());
            }
        }
        return result;
    }

    private List<Suggestion> stringSuggestions(String text, int contentStart, int pos, boolean force) {
        if (strings == null || pos < contentStart) {
            return List.of();
        }
        String prefix = text.substring(contentStart, pos);
        if (prefix.length() < 2 && !force) {
            // Короткие ограниченные наборы (стороны, режимы) показываем сразу, предметы — после 2 символов
            Call call = callAt(text, contentStart - 1);
            if (call == null || !isSmallSet(call.name())) {
                return List.of();
            }
        }
        Call call = callAt(text, contentStart - 1);
        String name = call == null ? "" : call.name();
        int index = call == null ? 0 : call.argIndex();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            name = resolveQualifier(text, name.substring(0, dot)) + name.substring(dot);
        }
        List<Suggestion> result = new ArrayList<>();
        for (String value : strings.complete(name, index, prefix)) {
            if (result.size() >= MAX_STRING_SUGGESTIONS) {
                break;
            }
            result.add(new Suggestion(value, value, isSmallSet(name) ? "value" : "item", contentStart));
        }
        if (result.size() == 1 && result.get(0).label().equals(prefix)) {
            return List.of();
        }
        return result;
    }

    private static boolean isSmallSet(String call) {
        return call.equals("side") || call.equals("stock.setMode");
    }

    // --- Сопоставление и ранжирование ---

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /** 3 — по префиксу, 2 — по подстроке, 1 — по буквам подряд ("sp" → setPriority), 0 — не подходит. */
    private static int matches(String name, String prefix) {
        if (prefix.isEmpty()) {
            return 3;
        }
        String n = name.toLowerCase(Locale.ROOT);
        String p = prefix.toLowerCase(Locale.ROOT);
        if (n.startsWith(p)) {
            return 3;
        }
        if (n.contains(p)) {
            return 2;
        }
        int at = 0;
        for (int i = 0; i < p.length(); i++) {
            at = n.indexOf(p.charAt(i), at);
            if (at < 0) {
                return 0;
            }
            at++;
        }
        return p.length() >= 2 ? 1 : 0;
    }

    private static Suggestion toSuggestion(Entry e, String text, int pos, int from) {
        boolean parenFollows = pos < text.length() && text.charAt(pos) == '(';
        String insert = e.name();
        if (e.call() && !parenFollows) {
            List<String> params = paramsOf(e.detail());
            insert = params != null && params.isEmpty() ? e.name() + "()" : e.name() + "($0)";
        }
        return new Suggestion(e.name(), insert, e.detail(), from, e.doc());
    }

    private static List<Suggestion> rank(List<Suggestion> list, String prefix) {
        list.sort(Comparator.<Suggestion, Integer>comparing(s -> -matches(s.label(), prefix))
                .thenComparing(s -> s.label().toLowerCase(Locale.ROOT)));
        if (list.size() == 1 && list.get(0).label().equals(prefix)) {
            return List.of();
        }
        return list.size() > MAX_SUGGESTIONS ? new ArrayList<>(list.subList(0, MAX_SUGGESTIONS)) : list;
    }
}
