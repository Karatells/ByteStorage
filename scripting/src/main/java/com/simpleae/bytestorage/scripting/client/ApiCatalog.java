package com.simpleae.bytestorage.scripting.client;

import com.simpleae.bytestorage.util.editor.CompletionEngine;
import com.simpleae.bytestorage.util.editor.CompletionEngine.Entry;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Каталог JS API скриптов для подсветки и автодополнения редактора. */
final class ApiCatalog {

    static final Set<String> GLOBALS = Set.of("net", "stock", "recipes", "journal", "side", "buses", "log",
            "time", "alert", "busId");

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static List<String> itemIds;

    static {
        global("net", "network API", false);
        global("stock", "stock targets", false);
        global("recipes", "recipe rules", false);
        global("journal", "network journal", false);
        global("side", "side(name) → endpoint", true);
        global("buses", "buses() → list", true);
        global("log", "log(text)", true);
        global("time", "time() → game tick", true);
        global("alert", "alert(text)", true);
        global("busId", "this bus id", false);
        global("tick", "handler: every run", true);
        global("onCraft", "handler: craft finished", true);
        global("onLowStock", "handler: stock low", true);
        global("onAlert", "handler: alert raised", true);

        for (String m : new String[]{
                "available(id)", "canCraft(id,n)", "cancel(q)", "count(id)", "craft(id,n)", "energy()",
                "freeBytes()", "has(id,n)", "list()", "machines()", "pause(q)", "plan(id,n)", "queue()",
                "reserved(id)", "resume(q)", "setPriority(q,p)", "stats()", "totalReserved()", "usedBytes()", "why(id)"}) {
            member("net", m);
        }
        for (String m : new String[]{"clear(id)", "list()", "mode()", "set(id,n)", "setMode(m)"}) {
            member("stock", m);
        }
        for (String m : new String[]{"blacklist()", "block(id)", "prefer(id)", "preferred()", "skipUnpacking(b)",
                "unblock(id)", "unprefer(id)"}) {
            member("recipes", m);
        }
        for (String m : new String[]{"read(n)", "warn(text)", "write(text)"}) {
            member("journal", m);
        }
        for (String m : new String[]{"count(id)", "list()", "move(from,to,n)", "pull(id,n)", "push(id,n)", "slots()"}) {
            member("ep", m);
        }
    }

    private ApiCatalog() {
    }

    private static void global(String name, String detail, boolean call) {
        ENTRIES.add(new Entry("", name, detail, call));
    }

    private static void member(String qualifier, String signature) {
        int paren = signature.indexOf('(');
        ENTRIES.add(new Entry(qualifier, signature.substring(0, paren), signature, true));
    }

    private static synchronized List<String> allItemIds() {
        if (itemIds == null) {
            TreeSet<String> set = new TreeSet<>();
            ForgeRegistries.ITEMS.getKeys().forEach(k -> set.add(k.toString()));
            itemIds = new ArrayList<>(set);
        }
        return itemIds;
    }

    private static List<String> matchItems(String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String id : allItemIds()) {
            int colon = id.indexOf(':');
            String path = id.substring(colon + 1);
            if (id.startsWith(p) || path.startsWith(p)) {
                result.add(id);
            }
        }
        return result;
    }

    static CompletionEngine engine() {
        return new CompletionEngine(ENTRIES, GLOBALS, ApiCatalog::matchItems);
    }
}
