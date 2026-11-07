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

    private static final List<String> DIRECTIONS = List.of("down", "up", "north", "south", "west", "east");
    private static final List<String> STOCK_MODES = List.of("always", "night", "day", "energy");
    private static final Set<String> TEXT_CALLS = Set.of("log", "alert", "journal.write", "journal.warn");

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static List<String> itemIds;

    static {
        global("net", "network API", false, "Read the network and order crafts.");
        global("stock", "stock targets", false, "Stock rules: keep an item at a target amount.");
        global("recipes", "recipe rules", false, "Block or prefer recipes, skip unpacking.");
        global("journal", "network journal", false, "Write to or read the Controller's Journal tab.");
        global("side", "side(name, id)", true, "Container next to this bus: \"up\", \"north\"... With an id it finds the first matching slot.");
        global("buses", "buses()", true, "List of Logic Buses on this network.");
        global("log", "log(text)", true, "Write a line to this bus's log (shown in the Logic Terminal).");
        global("time", "time()", true, "Current game tick.");
        global("alert", "alert(text)", true, "Raise an alert: journal entry and chat warning (needs allowAlerts).");
        global("busId", "this bus id", false, "The identifier assigned to this bus.");
        global("tick", "handler", false, "Optional. Called every run of the bus.");
        global("onCraft", "handler", false, "Optional. Called when a craft finishes; e has type, id, count, text.");
        global("onLowStock", "handler", false, "Optional. Called when a stock rule fails; e has type, id, count, text.");
        global("onAlert", "handler", false, "Optional. Called when the network raises an alert; e has type, id, count, text.");

        member("net", "available(id)", "Items of this kind not reserved by orders.");
        member("net", "canCraft(id, n)", "True if n items can be crafted now. Orders nothing.");
        member("net", "cancel(q)", "Cancel an order from net.queue() (needs allowControl).");
        member("net", "count(id)", "How many items of this kind the network holds.");
        member("net", "craft(id, n)", "Order n crafts of an item.");
        member("net", "energy()", "Network energy: stored, capacity, balance.");
        member("net", "freeBytes()", "Free storage bytes.");
        member("net", "has(id, n)", "True if the network holds at least n of the item.");
        member("net", "list()", "All stored items as {id, count}.");
        member("net", "machines()", "Assemblers and Pattern Buses with busy / pending / wait / output.");
        member("net", "pause(q)", "Pause an order (needs allowControl).");
        member("net", "plan(id, n)", "Dry-run an order: what would be crafted and what is missing.");
        member("net", "queue()", "Current craft orders.");
        member("net", "reserved(id)", "Amount reserved for running orders.");
        member("net", "resume(q)", "Resume a paused order (needs allowControl).");
        member("net", "setPriority(q, p)", "Change an order's priority (needs allowControl).");
        member("net", "stats()", "Network statistics: stored items, orders, energy.");
        member("net", "totalReserved()", "Total items reserved by all orders.");
        member("net", "usedBytes()", "Used storage bytes.");
        member("net", "why(id)", "Why an item cannot be crafted: reason code, missing item and count.");

        member("stock", "clear(id)", "Remove the stock rule for an item.");
        member("stock", "list()", "All stock rules with their state.");
        member("stock", "mode()", "Current stock schedule: always, night, day or energy.");
        member("stock", "set(id, n)", "Keep n of an item in stock.");
        member("stock", "setMode(m)", "Set the schedule: \"always\", \"night\", \"day\" or \"energy\".");

        member("recipes", "blacklist()", "Blocked recipe outputs.");
        member("recipes", "block(id)", "Never craft this item.");
        member("recipes", "prefer(id)", "Prefer the recipe for this item.");
        member("recipes", "preferred()", "Preferred recipe outputs.");
        member("recipes", "skipUnpacking(b)", "Do not unpack storage blocks when crafting.");
        member("recipes", "unblock(id)", "Allow crafting this item again.");
        member("recipes", "unprefer(id)", "Remove the preference for this item.");

        member("journal", "read(n)", "The last n journal entries.");
        member("journal", "warn(text)", "Write a warning entry (limited per run).");
        member("journal", "write(text)", "Write an entry (limited per run).");

        member("ep", "count(id)", "Items of this kind in the container.");
        member("ep", "list()", "Container contents as {slot, id, count}.");
        member("ep", "move(from, to, n)", "Move items between slots.");
        member("ep", "pull(id, n)", "Take items from the container into the network.");
        member("ep", "push(id, n)", "Put items from the network into the container.");
        member("ep", "slots()", "Number of slots.");

        field("event", "type", "order, crafted, started, stock_order, stock_failed, cancelled, stuck, script...");
        field("event", "id", "Item id of the event.");
        field("event", "count", "Amount.");
        field("event", "text", "Event text.");
    }

    private ApiCatalog() {
    }

    private static void global(String name, String detail, boolean call, String doc) {
        ENTRIES.add(new Entry("", name, detail, call, doc));
    }

    private static void member(String qualifier, String signature, String doc) {
        int paren = signature.indexOf('(');
        ENTRIES.add(new Entry(qualifier, signature.substring(0, paren), signature, true, doc));
    }

    private static void field(String qualifier, String name, String doc) {
        ENTRIES.add(new Entry(qualifier, name, "property", false, doc));
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

    private static List<String> matchValues(List<String> values, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String v : values) {
            if (v.startsWith(p)) {
                result.add(v);
            }
        }
        return result;
    }

    private static List<String> complete(String call, int argIndex, String prefix) {
        if (call.equals("side") && argIndex == 0) {
            return matchValues(DIRECTIONS, prefix);
        }
        if (call.equals("stock.setMode")) {
            return matchValues(STOCK_MODES, prefix);
        }
        if (TEXT_CALLS.contains(call) || (argIndex >= 1 && !call.equals("side"))) {
            return List.of();
        }
        return matchItems(prefix);
    }

    static CompletionEngine engine() {
        return new CompletionEngine(ENTRIES, GLOBALS, ApiCatalog::complete);
    }
}
