package com.simpleae.bytestorage.util;

import java.util.Locale;

/**
 * Разбор строки для Network Display (чистая логика без классов Minecraft — покрыта тестами).
 * Поддерживает:
 * - ведущий цветовой код {@code &a}, {@code &b}, {@code &c}, {@code &e}, {@code &f}, {@code &7}, {@code &6}, {@code &d};
 * - прогресс-бар {@code [bar:75]} или {@code [bar:75:green]} (0..100).
 */
public final class DisplayLineParser {

    public static final int DEFAULT_COLOR = 0xFF00F0FF; // фирменный циан SBS

    public record ParsedLine(String text, int color, boolean isBar, int barPercent) {
    }

    private DisplayLineParser() {
    }

    public static ParsedLine parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return new ParsedLine("", DEFAULT_COLOR, false, 0);
        }
        String s = raw;
        int color = DEFAULT_COLOR;
        if (s.length() >= 2 && s.charAt(0) == '&') {
            int mapped = colorCode(s.charAt(1));
            if (mapped != 0) {
                color = mapped;
                s = s.substring(2);
            }
        }
        String trimmed = s.trim();
        if (trimmed.startsWith("[bar:") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(5, trimmed.length() - 1).trim();
            String[] parts = inner.split(":");
            try {
                int pct = Math.max(0, Math.min(100, Integer.parseInt(parts[0].trim())));
                int barColor = parts.length > 1 ? namedColor(parts[1].trim(), color) : color;
                return new ParsedLine(pct + "%", barColor, true, pct);
            } catch (NumberFormatException ignored) {
                // невалидный бар — отображаем как обычный текст
            }
        }
        // убираем оставшиеся inline &x коды для чистого отображения
        String clean = stripInlineCodes(s);
        return new ParsedLine(clean, color, false, 0);
    }

    private static String stripInlineCodes(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length() && colorCode(s.charAt(i + 1)) != 0) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static int colorCode(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'a' -> 0xFF4ADE80; // зелёный
            case 'b' -> 0xFF00F0FF; // циан
            case 'c' -> 0xFFF87171; // красный
            case 'd' -> 0xFFE879F9; // пурпурный
            case 'e' -> 0xFFFACC15; // жёлтый
            case 'f' -> 0xFFF1F5F9; // белый
            case '6' -> 0xFFFB923C; // оранжевый
            case '7' -> 0xFF94A3B8; // серый
            case '9' -> 0xFF60A5FA; // синий
            default -> 0;
        };
    }

    private static int namedColor(String name, int fallback) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "green" -> 0xFF4ADE80;
            case "cyan" -> 0xFF00F0FF;
            case "red" -> 0xFFF87171;
            case "yellow" -> 0xFFFACC15;
            case "orange" -> 0xFFFB923C;
            case "blue" -> 0xFF60A5FA;
            case "purple", "magenta" -> 0xFFE879F9;
            case "white" -> 0xFFF1F5F9;
            case "gray", "grey" -> 0xFF94A3B8;
            default -> fallback;
        };
    }
}
