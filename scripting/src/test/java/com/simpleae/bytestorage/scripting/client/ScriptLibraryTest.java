package com.simpleae.bytestorage.scripting.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScriptLibraryTest {

    @Test
    void versionCompatibilityChecks() {
        assertTrue(ScriptLibraryIndex.isCompatible("1.4.0.40", "1.4.0", "*"));
        assertTrue(ScriptLibraryIndex.isCompatible("1.4.0.40", "1.2.0", "1.4.*"));
        assertTrue(ScriptLibraryIndex.isCompatible("1.4.2", "1.4.0", "1.4.*"));
        assertFalse(ScriptLibraryIndex.isCompatible("1.3.1", "1.4.0", "*"));
        assertFalse(ScriptLibraryIndex.isCompatible("1.5.0", "1.2.0", "1.4.*"));
    }

    @Test
    void parseIndexAndRejectTraversalPaths() {
        String json = """
                {
                  "format": 1,
                  "scripts": [
                    {
                      "id": "demo",
                      "name": "Demo Script",
                      "author": "Tester",
                      "description": "Test desc",
                      "category": "energy",
                      "icon": "minecraft:redstone",
                      "tags": ["energy"],
                      "minModVersion": "1.4.0",
                      "maxModVersion": "*",
                      "needsControl": true,
                      "needsAlerts": false,
                      "path": "scripts/demo/script.js",
                      "sha256": "abc123",
                      "size": 42
                    },
                    {
                      "id": "evil",
                      "name": "Bad",
                      "path": "../secret.js"
                    }
                  ]
                }
                """;
        List<ScriptLibraryIndex.Entry> list = ScriptLibraryIndex.parseIndex(json);
        assertEquals(1, list.size());
        assertEquals("demo", list.get(0).id());
        assertEquals("energy", list.get(0).category());
        assertEquals("minecraft:redstone", list.get(0).icon());
        assertEquals(1, ScriptLibraryIndex.filterByCategory(list, "energy").size());
        assertEquals(0, ScriptLibraryIndex.filterByCategory(list, "fluids").size());
        assertTrue(list.get(0).needsControl());
        assertEquals("abc123", list.get(0).sha256());
    }

    @Test
    void sha256MatchesKnownDigest() {
        String hash = ScriptLibraryIndex.sha256Hex("hello".getBytes(StandardCharsets.UTF_8));
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
    }
}
