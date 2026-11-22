package com.simpleae.bytestorage.dimension;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверка целостности data-driven ресурсов измерения, рецептов, ачивок и локализаций.
 */
class DimensionResourcesTest {

    private static final Gson GSON = new Gson();

    @Test
    void allJsonResourcesAreValid() throws IOException {
        Path resRoot = Path.of("src/main/resources");
        assertTrue(Files.isDirectory(resRoot));
        try (Stream<Path> walk = Files.walk(resRoot)) {
            List<Path> jsonFiles = walk.filter(p -> p.toString().endsWith(".json")).toList();
            assertTrue(jsonFiles.size() >= 30, "Expected at least 30 JSON files, got " + jsonFiles.size());
            for (Path file : jsonFiles) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                JsonElement parsed = assertDoesNotThrow(() -> GSON.fromJson(text, JsonElement.class),
                        "Invalid JSON: " + file);
                assertNotNull(parsed, "Empty JSON: " + file);
            }
        }
    }

    @Test
    void allLanguagesHaveMatchingKeys() throws IOException {
        JsonObject en = loadLang("en_us");
        JsonObject ru = loadLang("ru_ru");
        JsonObject uk = loadLang("uk_ua");

        Set<String> enKeys = en.keySet();
        assertEquals(enKeys, ru.keySet(), "ru_ru keys must match en_us");
        assertEquals(enKeys, uk.keySet(), "uk_ua keys must match en_us");
        assertTrue(enKeys.contains("advancement.simple_bytestorage_dimension.wrong_biome_signal.title"));
        assertTrue(enKeys.contains("item.simple_bytestorage_dimension.cell_256k"));
    }

    @Test
    void coreDimensionAndAdvancementFilesExist() {
        String[] required = {
                "data/simple_bytestorage_dimension/dimension_type/silicon_plane.json",
                "data/simple_bytestorage_dimension/dimension/silicon_plane.json",
                "data/simple_bytestorage_dimension/worldgen/noise_settings/silicon_plane.json",
                "data/simple_bytestorage_dimension/worldgen/biome/silicon_terraces.json",
                "data/simple_bytestorage_dimension/worldgen/biome/optic_spirewood.json",
                "data/simple_bytestorage_dimension/worldgen/biome/bus_canyons.json",
                "data/simple_bytestorage_dimension/advancements/wrong_biome_signal.json",
                "data/simple_bytestorage_dimension/advancements/enter_silicon_plane.json"
        };
        for (String rel : required) {
            assertNotNull(getClass().getClassLoader().getResource(rel), "Missing resource: " + rel);
        }
    }

    private JsonObject loadLang(String code) throws IOException {
        String path = "assets/simple_bytestorage_dimension/lang/" + code + ".json";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "Missing lang file: " + path);
            return GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
        }
    }
}
