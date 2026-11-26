package com.simpleae.bytestorage.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Разбор строки поиска терминала (чистая логика без классов Minecraft — покрыта тестами).
 * Слова через пробел; {@code @мод}, {@code #тег}, {@code >N} {@code <N} {@code =N}, {@code $флаг}, иначе — часть названия.
 */
public final class SearchSyntax {

    public enum Kind { TEXT, MOD, TAG, TOOLTIP, COUNT, FLAG, INVALID }

    /** @param op для COUNT: '>', '<', '=', 'G' (>=), 'L' (<=); для остальных — 0. */
    public record Token(Kind kind, String text, char op, int number) {
    }

    public static final String FLAG_CRAFTABLE = "craftable";
    public static final String FLAG_STOCK = "stock";
    public static final String FLAG_FAV = "fav";

    private SearchSyntax() {
    }

    public static List<Token> parse(String query) {
        List<Token> result = new ArrayList<>();
        if (query == null) {
            return result;
        }
        for (String raw : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (raw.isEmpty()) {
                continue;
            }
            char c = raw.charAt(0);
            String rest = raw.substring(1);
            if (c == '@' && !rest.isEmpty()) {
                result.add(new Token(Kind.MOD, rest, (char) 0, 0));
            } else if (c == '#' && !rest.isEmpty()) {
                result.add(new Token(Kind.TAG, rest, (char) 0, 0));
            } else if (c == '%' && !rest.isEmpty()) {
                result.add(new Token(Kind.TOOLTIP, rest, (char) 0, 0));
            } else if ((c == '>' || c == '<' || c == '=') && !rest.isEmpty()) {
                char op = c;
                String numPart = rest;
                if ((c == '>' || c == '<') && rest.startsWith("=")) {
                    op = c == '>' ? 'G' : 'L';
                    numPart = rest.substring(1);
                }
                Integer parsed = parseAmount(numPart);
                if (parsed != null) {
                    result.add(new Token(Kind.COUNT, numPart, op, parsed));
                } else {
                    result.add(new Token(Kind.INVALID, raw, (char) 0, 0));
                }
            } else if (c == '$' && !rest.isEmpty()) {
                String flag = resolveFlag(rest);
                result.add(flag == null ? new Token(Kind.INVALID, raw, (char) 0, 0) : new Token(Kind.FLAG, flag, (char) 0, 0));
            } else {
                result.add(new Token(Kind.TEXT, raw, (char) 0, 0));
            }
        }
        return result;
    }

    /** Флаг по началу слова ({@code $craft} = {@code $craftable}); null — неизвестный. */
    public static String resolveFlag(String prefix) {
        if (prefix.isEmpty()) {
            return null;
        }
        for (String flag : new String[]{FLAG_CRAFTABLE, FLAG_STOCK, FLAG_FAV}) {
            if (flag.startsWith(prefix)) {
                return flag;
            }
        }
        return null;
    }

    private static Integer parseAmount(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        long mult = 1;
        char last = s.charAt(s.length() - 1);
        String digits = s;
        if (last == 'k') {
            mult = 1_000L;
            digits = s.substring(0, s.length() - 1);
        } else if (last == 'm') {
            mult = 1_000_000L;
            digits = s.substring(0, s.length() - 1);
        }
        if (digits.isEmpty()) {
            return null;
        }
        try {
            long val = Long.parseLong(digits);
            if (val < 0) return null;
            long scaled = Math.multiplyExact(val, mult);
            if (scaled > Integer.MAX_VALUE) return null;
            return (int) scaled;
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    public static boolean countMatches(Token token, int count) {
        return switch (token.op()) {
            case '>' -> count > token.number();
            case '<' -> count < token.number();
            case 'G' -> count >= token.number();
            case 'L' -> count <= token.number();
            default -> count == token.number();
        };
    }
}
