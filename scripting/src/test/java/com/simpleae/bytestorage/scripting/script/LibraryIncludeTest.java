package com.simpleae.bytestorage.scripting.script;

import com.simpleae.bytestorage.api.network.IGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юніт-тести механізму {@code include(id)} / {@code require(id)} у пісочниці Rhino:
 * експорт, кешування, вкладені імпорти, виявлення циклів, ліміт глибини, ізоляція та безпека.
 */
class LibraryIncludeTest {

    private static final class StubBus implements BusContext {
        final List<String> logs = new ArrayList<>();
        final Map<String, String> store = new LinkedHashMap<>();

        @Override public IGrid grid() { return null; }
        @Override public Level level() { return null; }
        @Override public BlockPos pos() { return BlockPos.ZERO; }
        @Override public String identifier() { return "test-bus"; }
        @Override public List<BusContext> busesWithId(String id) { return List.of(); }
        @Override public void log(String message) { logs.add(message); }
        @Override public void setPanel(List<UiElement> elements) {}
        @Override public List<String> takeButtons() { return List.of(); }
        @Override public Map<String, String> memoryStore() { return store; }
        @Override public void markStoreDirty() {}
        @Override public int getRedstone(Direction dir) { return 0; }
        @Override public void setRedstone(Direction dir, int power) {}
        @Override public void pushMessage(String from, String text) {}
        @Override public List<BusMessage> takeMessages() { return List.of(); }
    }

    private static LibraryResolver mapResolver(Map<String, String> libs) {
        return id -> {
            String src = libs.get(id);
            return src != null ? LibraryResolver.Resolution.ok(src) : LibraryResolver.Resolution.notFound(id);
        };
    }

    @Test
    void includeExportsAndModuleExportsAndCaching() {
        StubBus bus = new StubBus();
        LibraryResolver resolver = mapResolver(Map.of(
                "acme/math", """
                        var runs = 0;
                        runs++;
                        exports.add = function(a, b) { return a + b; };
                        exports.runs = function() { return runs; };
                        """,
                "acme/greeter", """
                        module.exports = function(name) { return "hi " + name; };
                        """
        ));
        LogicRuntime rt = new LogicRuntime(bus, resolver);
        String err = rt.compile("""
                var m1 = include("acme/math");
                var m2 = require("acme/math");
                var greet = include("acme/greeter");
                function tick() {
                  log(m1.add(2, 3), m1 === m2, m2.runs(), greet("SBS"));
                }
                """);
        assertNull(err, "unexpected compile error: " + err);
        assertNull(rt.tick());
        assertEquals(List.of("5 true 1 hi SBS"), bus.logs);
    }

    @Test
    void nestedIncludeWorks() {
        StubBus bus = new StubBus();
        LibraryResolver resolver = mapResolver(Map.of(
                "sbs/base", "exports.mul = function(a, b) { return a * b; };",
                "sbs/derived", """
                        var base = include("sbs/base");
                        exports.square = function(x) { return base.mul(x, x); };
                        """
        ));
        LogicRuntime rt = new LogicRuntime(bus, resolver);
        assertNull(rt.compile("""
                var d = include("sbs/derived");
                function tick() { log(d.square(7)); }
                """));
        assertNull(rt.tick());
        assertEquals(List.of("49"), bus.logs);
    }

    @Test
    void circularIncludeIsRejectedCleanly() {
        StubBus bus = new StubBus();
        LibraryResolver resolver = mapResolver(Map.of(
                "sbs/a", "var b = include('sbs/b'); exports.x = 1;",
                "sbs/b", "var a = include('sbs/a'); exports.y = 2;"
        ));
        LogicRuntime rt = new LogicRuntime(bus, resolver);
        String err = rt.compile("var a = include('sbs/a'); function tick() {}");
        assertNotNull(err);
        assertTrue(err.toLowerCase().contains("circular"), "expected circular error, got: " + err);
    }

    @Test
    void maxNestingDepthIsEnforced() {
        StubBus bus = new StubBus();
        LibraryResolver resolver = mapResolver(Map.of(
                "sbs/d1", "exports.v = include('sbs/d2').v;",
                "sbs/d2", "exports.v = include('sbs/d3').v;",
                "sbs/d3", "exports.v = include('sbs/d4').v;",
                "sbs/d4", "exports.v = include('sbs/d5').v;",
                "sbs/d5", "exports.v = 42;"
        ));
        LogicRuntime rt = new LogicRuntime(bus, resolver);
        String err = rt.compile("var x = include('sbs/d1'); function tick() {}");
        assertNotNull(err);
        assertTrue(err.toLowerCase().contains("depth"), "expected depth error, got: " + err);
    }

    @Test
    void libraryObeysSandboxClassShutterAndBudget() {
        StubBus bus = new StubBus();
        LibraryResolver javaEscape = mapResolver(Map.of(
                "bad/java", "exports.pwn = java.lang.System.exit(1);"
        ));
        LogicRuntime rt1 = new LogicRuntime(bus, javaEscape);
        assertNotNull(rt1.compile("var b = include('bad/java'); function tick() {}"));

        LibraryResolver infLoop = mapResolver(Map.of(
                "bad/loop", "while (true) {}"
        ));
        LogicRuntime rt2 = new LogicRuntime(bus, infLoop);
        String loopErr = rt2.compile("var b = include('bad/loop'); function tick() {}");
        assertNotNull(loopErr);
        assertTrue(loopErr.toLowerCase().contains("instruction") || loopErr.toLowerCase().contains("time"),
                "expected budget error, got: " + loopErr);
    }

    @Test
    void loadingStatusReturnsNonFatalLoadingPrefix() {
        StubBus bus = new StubBus();
        LibraryResolver loading = id -> LibraryResolver.Resolution.loading(id);
        LogicRuntime rt = new LogicRuntime(bus, loading);
        String err = rt.compile("var fmt = include('simplebytestorage/format'); function tick() {}");
        assertEquals(LogicRuntime.LOADING_PREFIX + "simplebytestorage/format", err);
    }
}
