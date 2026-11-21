package com.simpleae.bytestorage.scripting.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Чистая логика онлайн-библиотеки скриптов: парсинг index.json, проверка совместимости версий и SHA-256.
 * Не зависит от классов Minecraft — полностью покрыта юнит-тестами.
 */
public final class ScriptLibraryIndex {

    public static final List<String> CATEGORIES = List.of(
            "all", "storage", "transfer", "automation", "energy", "fluids", "display", "ui", "other"
    );

    public record Entry(
            String id,
            String name,
            String author,
            String description,
            String category,
            String icon,
            List<String> tags,
            String minModVersion,
            String maxModVersion,
            boolean needsControl,
            boolean needsAlerts,
            String path,
            String sha256,
            int size
    ) {
    }

    private ScriptLibraryIndex() {
    }

    public static List<Entry> parseIndex(String jsonText) {
        List<Entry> out = new ArrayList<>();
        if (jsonText == null || jsonText.isBlank()) {
            return out;
        }
        JsonObject root = JsonParser.parseString(jsonText).getAsJsonObject();
        if (!root.has("scripts") || !root.get("scripts").isJsonArray()) {
            return out;
        }
        JsonArray arr = root.getAsJsonArray("scripts");
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String id = str(o, "id", "");
            String name = str(o, "name", id);
            String path = str(o, "path", "");
            if (id.isEmpty() || path.isEmpty() || path.contains("..") || path.startsWith("/")) {
                continue;
            }
            List<String> tags = new ArrayList<>();
            if (o.has("tags") && o.get("tags").isJsonArray()) {
                for (JsonElement t : o.getAsJsonArray("tags")) {
                    if (t.isJsonPrimitive()) tags.add(t.getAsString());
                }
            }
            String category = str(o, "category", "other").toLowerCase(Locale.ROOT);
            if (!CATEGORIES.contains(category) || category.equals("all")) {
                category = "other";
            }
            out.add(new Entry(
                    id,
                    name,
                    str(o, "author", "unknown"),
                    str(o, "description", ""),
                    category,
                    str(o, "icon", "simple_bytestorage_scripting:logic_bus"),
                    List.copyOf(tags),
                    str(o, "minModVersion", "1.0.0"),
                    str(o, "maxModVersion", "*"),
                    bool(o, "needsControl"),
                    bool(o, "needsAlerts"),
                    path,
                    str(o, "sha256", "").toLowerCase(Locale.ROOT),
                    o.has("size") && o.get("size").isJsonPrimitive() ? o.get("size").getAsInt() : 0
            ));
        }
        return out;
    }

    public static List<Entry> filterByCategory(List<Entry> entries, String category) {
        if (entries == null || category == null || category.equals("all")) {
            return entries == null ? List.of() : entries;
        }
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (category.equals(e.category())) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * Проверяет, совместима ли текущая версия мода (например, "1.4.0.40" или "1.3.1")
     * с диапазоном [minVersion, maxVersion] (поддерживаются "*" и "1.4.*").
     */
    public static boolean isCompatible(String currentVersion, String minVersion, String maxVersion) {
        int[] cur = parseVersion(currentVersion);
        if (minVersion != null && !minVersion.isBlank() && !minVersion.equals("*")) {
            if (compareMin(cur, minVersion) < 0) {
                return false;
            }
        }
        if (maxVersion != null && !maxVersion.isBlank() && !maxVersion.equals("*")) {
            if (compareMax(cur, maxVersion) > 0) {
                return false;
            }
        }
        return true;
    }

    private static int[] parseVersion(String raw) {
        int[] nums = new int[]{0, 0, 0};
        if (raw == null || raw.isBlank()) return nums;
        String[] parts = raw.trim().split("\\.");
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            try {
                nums[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {
                nums[i] = 0;
            }
        }
        return nums;
    }

    private static int compareMin(int[] cur, String min) {
        String[] parts = min.trim().split("\\.");
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            if (parts[i].equals("*")) return 0;
            int target = safeInt(parts[i]);
            if (cur[i] != target) return Integer.compare(cur[i], target);
        }
        return 0;
    }

    private static int compareMax(int[] cur, String max) {
        String[] parts = max.trim().split("\\.");
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            if (parts[i].equals("*")) return 0;
            int target = safeInt(parts[i]);
            if (cur[i] != target) return Integer.compare(cur[i], target);
        }
        return 0;
    }

    private static int safeInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String str(JsonObject o, String key, String def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
    }

    private static boolean bool(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() && o.get(key).getAsBoolean();
    }
}
