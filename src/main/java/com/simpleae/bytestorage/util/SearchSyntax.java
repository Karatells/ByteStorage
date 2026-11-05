package com.simpleae.bytestorage.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Разбор строки поиска терминала (чистая логика без классов Minecraft — покрыта тестами).
 * Слова через пробел; {@code @мод}, {@code #тег}, {@code >N} {@code <N} {@code =N}, {@code $флаг}, иначе — часть названия.
 */
public final class SearchSyntax {

    public enum Kind { TEXT, MOD, TAG, COUNT, FLAG, INVALID }

    /** @param op для COUNT: '>', '<' или '='; для остальных — 0. */
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
            } else if ((c == '>' || c == '<' || c == '=') && !rest.isEmpty()) {
                try {
                    result.add(new Token(Kind.COUNT, rest, c, Integer.parseInt(rest)));
                } catch (NumberFormatException e) {
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

    public static boolean countMatches(Token token, int count) {
        return switch (token.op()) {
            case '>' -> count > token.number();
            case '<' -> count < token.number();
            default -> count == token.number();
        };
    }
}
