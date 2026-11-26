package com.simpleae.bytestorage.scripting.client;

import java.util.List;

/** Готовые шаблоны скриптов для кнопки Templates в Логическом терминале. */
public final class ScriptTemplates {

    public record Template(String name, String code) {
    }

    public static final List<Template> ALL = List.of(
            new Template("Display Dashboard", String.join("\n",
                    "// Live Network Display dashboard (rendered in-world!)",
                    "function tick() {",
                    "  var e = net.energy();",
                    "  var pct = e ? e.percent : 0;",
                    "  display.set(0, \"&b=== BYTE STORAGE ===\");",
                    "  display.set(1, \"&fEnergy: \" + pct + \"%\");",
                    "  display.set(2, \"[bar:\" + pct + \":\" + (pct < 25 ? \"red\" : \"green\") + \"]\");",
                    "  display.set(3, \"&7Iron: &f\" + net.count(\"minecraft:iron_ingot\"));",
                    "  display.set(4, \"&7Diamonds: &b\" + net.count(\"minecraft:diamond\"));",
                    "  display.set(5, \"&eFree B: &f\" + net.freeBytes());",
                    "}",
                    "")),
            new Template("Auto-Pull Chest", String.join("\n",
                    "// Pull all items from chest above into network",
                    "var chest = side(\"up\");",
                    "",
                    "function tick() {",
                    "  if (chest) {",
                    "    var n = chest.pull(\"*\", 64);",
                    "    if (n > 0) log(\"Pulled \" + n + \" items\");",
                    "  }",
                    "}",
                    "")),
            new Template("Redstone Energy Guard", String.join("\n",
                    "// Emit redstone signal on NORTH when energy drops below 30%",
                    "function tick() {",
                    "  var e = net.energy();",
                    "  if (!e) return;",
                    "  var needPower = e.percent < 30;",
                    "  redstone.set(\"north\", needPower ? 15 : 0);",
                    "  store.set(\"low_power\", String(needPower));",
                    "}",
                    "")),
            new Template("Inter-Bus Messenger", String.join("\n",
                    "// Receive commands from other Logic Buses via bus.send(id, msg)",
                    "function onMessage(from, text) {",
                    "  log(\"From \" + from + \": \" + text);",
                    "  if (text === \"pulse\") {",
                    "    redstone.set(\"up\", 15);",
                    "  } else if (text === \"off\") {",
                    "    redstone.set(\"up\", 0);",
                    "  }",
                    "}",
                    "",
                    "function tick() {",
                    "  // Example: bus.send(\"farm_1\", \"pulse\");",
                    "}",
                    "")),
            new Template("Control Panel UI", String.join("\n",
                    "// Interactive Panel tab with status bar and craft button",
                    "function onButton(id) {",
                    "  if (id === \"craft_torches\") {",
                    "    net.craft(\"minecraft:torch\", 64);",
                    "    log(\"Ordered 64 torches\");",
                    "  }",
                    "}",
                    "",
                    "function tick() {",
                    "  ui.clear();",
                    "  var e = net.energy();",
                    "  ui.text(\"Network Status\", \"cyan\");",
                    "  if (e) ui.bar(\"Energy\", e.stored, e.max, \"green\");",
                    "  ui.item(\"minecraft:torch\", \"In stock: \" + net.count(\"minecraft:torch\"), \"yellow\");",
                    "  ui.button(\"craft_torches\", \"Craft 64 Torches\", \"cyan\");",
                    "}",
                    ""))
    );

    private ScriptTemplates() {
    }
}
