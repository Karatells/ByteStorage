package com.simpleae.bytestorage.util.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Разбор JavaScript на токены для подсветки синтаксиса: по строкам, позиции относительно начала строки. */
public final class JsLexer {

    public enum Kind { PLAIN, KEYWORD, STRING, NUMBER, COMMENT, FUNCTION, CALL, PROPERTY, GLOBAL, PUNCT }

    public record Token(int start, int end, Kind kind) {
    }

    public static final Set<String> KEYWORDS = Set.of(
            "var", "let", "const", "function", "return", "if", "else", "for", "while", "do", "break", "continue",
            "switch", "case", "default", "new", "typeof", "in", "of", "true", "false", "null", "undefined", "this",
            "try", "catch", "finally", "throw", "delete", "instanceof", "void");

    private static final String PUNCTUATION = ".,;:(){}[]+-*/%=<>!&|?~^";

    private JsLexer() {
    }

    private static boolean identStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean identPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /** @return токены по строкам (список на каждую строку текста, пробелы токенами не являются) */
    public static List<List<Token>> tokenize(String text, Set<String> globals) {
        List<List<Token>> lines = new ArrayList<>();
        List<Token> cur = new ArrayList<>();
        lines.add(cur);
        int n = text.length();
        int i = 0;
        int lineStart = 0;
        String prevWord = null;
        boolean afterDot = false;
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\n') {
                cur = new ArrayList<>();
                lines.add(cur);
                lineStart = i + 1;
                i++;
                continue;
            }
            if (c == ' ' || c == '\t' || c == '\r') {
                i++;
                continue;
            }
            char next = i + 1 < n ? text.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                int e = text.indexOf('\n', i);
                if (e < 0) {
                    e = n;
                }
                cur.add(new Token(i - lineStart, e - lineStart, Kind.COMMENT));
                i = e;
                prevWord = null;
                afterDot = false;
                continue;
            }
            if (c == '/' && next == '*') {
                int j = i + 2;
                int segStart = i;
                while (j < n && !(text.charAt(j) == '*' && j + 1 < n && text.charAt(j + 1) == '/')) {
                    if (text.charAt(j) == '\n') {
                        cur.add(new Token(segStart - lineStart, j - lineStart, Kind.COMMENT));
                        cur = new ArrayList<>();
                        lines.add(cur);
                        lineStart = j + 1;
                        segStart = j + 1;
                    }
                    j++;
                }
                int end = j < n ? j + 2 : n;
                cur.add(new Token(segStart - lineStart, end - lineStart, Kind.COMMENT));
                i = end;
                prevWord = null;
                afterDot = false;
                continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                int j = i + 1;
                while (j < n && text.charAt(j) != c && text.charAt(j) != '\n') {
                    if (text.charAt(j) == '\\' && j + 1 < n && text.charAt(j + 1) != '\n') {
                        j++;
                    }
                    j++;
                }
                int end = j < n && text.charAt(j) == c ? j + 1 : j;
                cur.add(new Token(i - lineStart, end - lineStart, Kind.STRING));
                i = end;
                prevWord = null;
                afterDot = false;
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && Character.isDigit(next))) {
                int j = i + 1;
                while (j < n && (Character.isLetterOrDigit(text.charAt(j)) || text.charAt(j) == '.')) {
                    j++;
                }
                cur.add(new Token(i - lineStart, j - lineStart, Kind.NUMBER));
                i = j;
                prevWord = null;
                afterDot = false;
                continue;
            }
            if (identStart(c)) {
                int j = i + 1;
                while (j < n && identPart(text.charAt(j))) {
                    j++;
                }
                String word = text.substring(i, j);
                int k = j;
                while (k < n && (text.charAt(k) == ' ' || text.charAt(k) == '\t')) {
                    k++;
                }
                boolean call = k < n && text.charAt(k) == '(';
                Kind kind;
                if (afterDot) {
                    kind = call ? Kind.CALL : Kind.PROPERTY;
                } else if (KEYWORDS.contains(word)) {
                    kind = Kind.KEYWORD;
                } else if ("function".equals(prevWord)) {
                    kind = Kind.FUNCTION;
                } else if (globals.contains(word)) {
                    kind = Kind.GLOBAL;
                } else if (call) {
                    kind = Kind.CALL;
                } else {
                    kind = Kind.PLAIN;
                }
                cur.add(new Token(i - lineStart, j - lineStart, kind));
                prevWord = word;
                afterDot = false;
                i = j;
                continue;
            }
            Kind kind = PUNCTUATION.indexOf(c) >= 0 ? Kind.PUNCT : Kind.PLAIN;
            cur.add(new Token(i - lineStart, i + 1 - lineStart, kind));
            afterDot = c == '.';
            prevWord = null;
            i++;
        }
        return lines;
    }
}
