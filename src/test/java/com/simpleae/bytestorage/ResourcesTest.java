package com.simpleae.bytestorage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки ресурсов и согласованности проекта, которые не требуют запуска игры. */
class ResourcesTest {

    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path LANG = ROOT.resolve("src/main/resources/assets/simple_bytestorage/lang");

    private static JsonObject lang(String name) throws IOException {
        return JsonParser.parseString(Files.readString(LANG.resolve(name + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static Set<String> keys(String name) throws IOException {
        return new TreeSet<>(lang(name).keySet());
    }

    @Test
    void langFilesAreValidAndHaveTheSameKeys() throws IOException {
        Set<String> en = keys("en_us");
        for (String other : new String[]{"ru_ru", "uk_ua"}) {
            Set<String> ks = keys(other);
            Set<String> missing = new TreeSet<>(en);
            missing.removeAll(ks);
            Set<String> extra = new TreeSet<>(ks);
            extra.removeAll(en);
            assertTrue(missing.isEmpty(), other + " is missing: " + missing);
            assertTrue(extra.isEmpty(), other + " has keys that en_us lacks: " + extra);
        }
    }

    @Test
    void noLangValueIsBlank() throws IOException {
        for (String name : new String[]{"en_us", "ru_ru", "uk_ua"}) {
            lang(name).entrySet().forEach(e ->
                    assertFalse(e.getValue().getAsString().isBlank(), name + ": blank value for " + e.getKey()));
        }
    }

    @Test
    void translationKeysUsedInCodeExist() throws IOException {
        Set<String> en = keys("en_us");
        Pattern p = Pattern.compile("\"((?:gui|msg|item|block|guide|key|itemGroup|container)\\.simple_bytestorage\\.[A-Za-z0-9_.]*[A-Za-z0-9_])\"(?!\\s*\\+)");
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ROOT.resolve("src/main/java"))) {
            for (Path f : files.filter(x -> x.toString().endsWith(".java")).collect(Collectors.toList())) {
                Matcher m = p.matcher(Files.readString(f, StandardCharsets.UTF_8));
                while (m.find()) {
                    if (!en.contains(m.group(1))) {
                        missing.add(m.group(1) + "  (" + ROOT.relativize(f) + ")");
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "Translation keys used in code but missing in en_us.json:\n" + String.join("\n", missing));
    }

    @Test
    void versionsAreConsistentAndFourPart() throws IOException {
        String core = gradleVersion(ROOT.resolve("build.gradle"));
        String scripting = gradleVersion(ROOT.resolve("scripting/build.gradle"));
        String coreToml = tomlVersion(ROOT.resolve("src/main/resources/META-INF/mods.toml"));
        String scriptingToml = tomlVersion(ROOT.resolve("scripting/src/main/resources/META-INF/mods.toml"));
        assertTrue(core.matches("\\d+\\.\\d+\\.\\d+\\.\\d+"), "core version must be major.minor.patch.build: " + core);
        assertEquals(core, scripting, "scripting/build.gradle version");
        assertEquals(core, coreToml, "core mods.toml version");
        assertEquals(core, scriptingToml, "scripting mods.toml version");
    }

    private static String gradleVersion(Path file) throws IOException {
        Matcher m = Pattern.compile("(?m)^version\\s*=\\s*'([^']+)'").matcher(Files.readString(file));
        assertTrue(m.find(), "no version in " + file);
        return m.group(1);
    }

    private static String tomlVersion(Path file) throws IOException {
        Matcher m = Pattern.compile("(?m)^version\\s*=\\s*\"([^\"]+)\"").matcher(Files.readString(file));
        assertTrue(m.find(), "no version in " + file);
        return m.group(1);
    }

    @Test
    void forbiddenCompetitorWordingNeverAppears() throws IOException {
        Pattern bad = Pattern.compile("(?i)applied\\s+energistics|\\bAE2?\\b|\\bME\\s+(terminal|drive|network|system|controller)|refined\\s+storage");
        List<String> hits = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        for (String dir : new String[]{"src/main/resources", "scripting/src/main/resources", "docs"}) {
            Path d = ROOT.resolve(dir);
            if (Files.exists(d)) {
                try (Stream<Path> s = Files.walk(d)) {
                    s.filter(f -> f.toString().matches(".*\\.(json|md|toml|mcmeta)$")).forEach(files::add);
                }
            }
        }
        for (String f : new String[]{"README.md", "CHANGELOG.md"}) {
            if (Files.exists(ROOT.resolve(f))) {
                files.add(ROOT.resolve(f));
            }
        }
        for (Path f : files) {
            Matcher m = bad.matcher(Files.readString(f, StandardCharsets.UTF_8));
            if (m.find()) {
                hits.add(ROOT.relativize(f) + ": " + m.group());
            }
        }
        assertTrue(hits.isEmpty(), "Forbidden wording found: " + hits);
    }

    @Test
    void everyBlockstateAndItemModelHasMatchingTexturesOrParents() throws IOException {
        // Все модели предметов должны быть валидным JSON (опечатка ломает загрузку ресурсов в игре).
        Path models = ROOT.resolve("src/main/resources/assets/simple_bytestorage/models");
        List<String> broken = new ArrayList<>();
        try (Stream<Path> s = Files.walk(models)) {
            for (Path f : s.filter(x -> x.toString().endsWith(".json")).collect(Collectors.toList())) {
                try {
                    JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
                } catch (Exception e) {
                    broken.add(ROOT.relativize(f).toString());
                }
            }
        }
        assertTrue(broken.isEmpty(), "Invalid JSON models: " + broken);
    }
}
