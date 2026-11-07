package com.simpleae.bytestorage.scripting.script;

import com.simpleae.bytestorage.api.NetworkControl;
import com.simpleae.bytestorage.api.network.IGrid;
import com.simpleae.bytestorage.api.storage.INetworkStorage;
import com.simpleae.bytestorage.scripting.ScriptingConfig;
import com.simpleae.bytestorage.util.CraftingService;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * JS API скрипта. Усе подається як нативні JS-об'єкти/функції: Java-класи скрипту недоступні.
 *
 * Глобально: net, side(name), log(...), time(). Предмети задаються рядком "mod:item" (NBT не враховується).
 */
final class Api {

    private static final int LIST_LIMIT = 512;

    private final BusContext bus;
    private final Scriptable scope;
    private final Map<Scriptable, Port> endpoints = new IdentityHashMap<>();
    Budget budget;

    Api(BusContext bus, Scriptable scope) {
        this.bus = bus;
        this.scope = scope;
    }

    void beginRun(Budget budget) {
        this.budget = budget;
        endpoints.clear();
        ScriptableObject.putProperty(scope, "busId", bus.identifier());
    }

    void install(Context cx) {
        ScriptableObject.putProperty(scope, "net", buildNet(cx));
        // side(dir) — інвентар біля цієї шини; side(dir, id) — біля ВСІХ шин мережі з таким ідентифікатором
        ScriptableObject.putProperty(scope, "side", new Fn(scope, "side", (c, s, a) -> {
            budget.spendCall();
            String name = str(a, 0);
            Direction dir = name == null ? null : Direction.byName(name.toLowerCase(Locale.ROOT));
            if (dir == null) {
                throw org.mozilla.javascript.ScriptRuntime.typeError("side(): expected down/up/north/south/west/east");
            }
            String id = str(a, 1);
            if (id == null) {
                Port port = portAt(bus, dir);
                return port == null ? null : buildEndpoint(c, port);
            }
            java.util.List<Port> ports = new java.util.ArrayList<>();
            for (BusContext other : bus.busesWithId(id)) {
                Port port = portAt(other, dir);
                if (port != null) {
                    ports.add(port);
                }
            }
            if (ports.isEmpty()) {
                return null;
            }
            return buildEndpoint(c, ports.size() == 1 ? ports.get(0) : new Port.Multi(ports));
        }));
        // buses(id) — скільки шин мережі мають цей ідентифікатор
        ScriptableObject.putProperty(scope, "buses", new Fn(scope, "buses", (c, s, a) -> {
            budget.spendCall();
            String id = str(a, 0);
            return id == null ? 0 : bus.busesWithId(id).size();
        }));
        // busId — ідентифікатор цієї шини ("" якщо не заданий)
        ScriptableObject.putProperty(scope, "busId", bus.identifier());
        ScriptableObject.putProperty(scope, "log", new Fn(scope, "log", (c, s, a) -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.length; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(Context.toString(a[i]));
                if (sb.length() > 200) {
                    break;
                }
            }
            bus.log(sb.toString());
            return Undefined.instance;
        }));
        ScriptableObject.putProperty(scope, "time", new Fn(scope, "time", (c, s, a) -> bus.level().getGameTime()));
        installControl(cx);
    }

    // --- Керування мережею: журнал, черга, запас, рецепти, попередження ---

    private static final String[] JOURNAL_TYPES = {"order", "crafted", "started", "stock_order", "stock_failed",
            "cancelled", "stuck", "script", "script_warn"};
    private static final String[] STOCK_STATES = {"off", "ok", "crafting", "failed"};

    private void installControl(Context cx) {
        Scriptable net = (Scriptable) ScriptableObject.getProperty(scope, "net");
        // --- net: резерв, енергія, статистика ---
        def(net, "reserved", (c, s, a) -> {
            budget.spendCall();
            return NetworkControl.reserved(bus.grid(), new ItemStack(requireItem(str(a, 0))));
        });
        def(net, "available", (c, s, a) -> {
            budget.spendCall();
            Item item = requireItem(str(a, 0));
            long total = 0;
            for (ItemStack stack : storage().getAllItems()) {
                if (stack.getItem() == item) {
                    total += stack.getCount();
                }
            }
            return (double) Math.max(0, total - NetworkControl.reserved(bus.grid(), new ItemStack(item)));
        });
        def(net, "totalReserved", (c, s, a) -> {
            budget.spendCall();
            return NetworkControl.totalReserved(bus.grid());
        });
        def(net, "energy", (c, s, a) -> {
            budget.spendCall();
            NetworkControl.EnergyInfo e = NetworkControl.energy(bus.grid());
            if (e == null) {
                return null;
            }
            return obj(c, "stored", e.stored(), "max", e.max(), "percent", e.max() <= 0 ? 0 : e.stored() * 100 / e.max());
        });
        def(net, "stats", (c, s, a) -> {
            budget.spendCall();
            com.simpleae.bytestorage.util.NetworkStats st = NetworkControl.stats(bus.grid());
            if (st == null) {
                return null;
            }
            return obj(c, "patterns", st.patterns, "assemblers", st.assemblers, "assemblersBusy", st.assemblersBusy,
                    "patternBuses", st.buses, "busJobs", st.busJobs, "libraryRecipes", st.libraryRecipes,
                    "reserved", NetworkControl.totalReserved(bus.grid()));
        });
        // --- net: планування ---
        def(net, "canCraft", (c, s, a) -> {
            budget.spendCall();
            return NetworkControl.canCraft(bus.grid(), new ItemStack(requireItem(str(a, 0))), Math.max(1, num(a, 1, 1)));
        });
        def(net, "why", (c, s, a) -> {
            budget.spendCall();
            com.simpleae.bytestorage.util.CraftingService.Diagnosis d =
                    NetworkControl.why(bus.grid(), new ItemStack(requireItem(str(a, 0))), Math.max(1, num(a, 1, 1)));
            if (d == null) {
                return null;
            }
            return d.item().isEmpty() ? obj(c, "code", d.code()) : obj(c, "code", d.code(), "id", idOf(d.item()), "count", d.count());
        });
        def(net, "plan", (c, s, a) -> {
            budget.spendCall();
            NetworkControl.PlanInfo plan = NetworkControl.plan(bus.grid(), new ItemStack(requireItem(str(a, 0))), Math.max(1, num(a, 1, 1)));
            if (plan == null) {
                return null;
            }
            java.util.List<Object> steps = new java.util.ArrayList<>();
            for (NetworkControl.StepInfo st : plan.steps()) {
                steps.add(obj(c, "id", idOf(st.output()), "crafts", st.crafts()));
            }
            java.util.List<Object> missing = new java.util.ArrayList<>();
            for (ItemStack m : plan.missing()) {
                missing.add(obj(c, "id", idOf(m), "count", m.getCount()));
            }
            return obj(c, "ok", plan.ok(), "steps", c.newArray(scope, steps.toArray()), "missing", c.newArray(scope, missing.toArray()));
        });
        // --- net: состояние Асемблеров и Шин шаблонов ---
        def(net, "machines", (c, s, a) -> {
            budget.spendCall();
            java.util.List<Object> out = new java.util.ArrayList<>();
            for (NetworkControl.MachineInfo m : NetworkControl.machines(bus.grid())) {
                if (out.size() >= 64) {
                    break;
                }
                out.add(obj(c, "kind", m.kind(), "x", m.pos().getX(), "y", m.pos().getY(), "z", m.pos().getZ(),
                        "busy", m.busy(), "pending", m.pending(), "wait", m.wait(),
                        "output", m.output().isEmpty() ? null : idOf(m.output())));
            }
            return c.newArray(scope, out.toArray());
        });
        // --- net: черга замовлень ---
        def(net, "queue", (c, s, a) -> {
            budget.spendCall();
            java.util.List<Object> out = new java.util.ArrayList<>();
            for (NetworkControl.QueueInfo q : NetworkControl.queue(bus.grid())) {
                out.add(obj(c, "id", idOf(q.output()), "crafts", q.crafts(), "active", q.active(), "priority", q.priority(),
                        "paused", q.paused(), "eta", q.etaSeconds(), "progress", q.percent()));
            }
            return c.newArray(scope, out.toArray());
        });
        def(net, "setPriority", (c, s, a) -> queueAction(str(a, 0), (g, q) -> NetworkControl.setPriority(g, q.id(), (int) Context.toNumber(a.length > 1 ? a[1] : 0))));
        def(net, "pause", (c, s, a) -> queueAction(str(a, 0), (g, q) -> NetworkControl.setPaused(g, q.id(), true)));
        def(net, "resume", (c, s, a) -> queueAction(str(a, 0), (g, q) -> NetworkControl.setPaused(g, q.id(), false)));
        def(net, "cancel", (c, s, a) -> queueAction(str(a, 0), (g, q) -> NetworkControl.cancel(g, q.id())));

        // --- stock: правила запасу ---
        Scriptable stock = cx.newObject(scope);
        def(stock, "list", (c, s, a) -> {
            budget.spendCall();
            java.util.List<Object> out = new java.util.ArrayList<>();
            for (NetworkControl.StockInfo r : NetworkControl.stockRules(bus.grid())) {
                out.add(obj(c, "slot", r.slot(), "id", idOf(r.item()), "min", r.minimum(), "have", r.have(),
                        "state", STOCK_STATES[Math.max(0, Math.min(STOCK_STATES.length - 1, r.state()))]));
            }
            return c.newArray(scope, out.toArray());
        });
        // set(id, min) — створює або змінює правило для предмета; повертає номер слота правила або -1, якщо вільних немає
        def(stock, "set", (c, s, a) -> {
            budget.spendCall();
            requireControl("stock.set");
            ItemStack item = new ItemStack(requireItem(str(a, 0)));
            int min = num(a, 1, 0);
            int free = -1;
            int existing = -1;
            java.util.Map<Integer, Boolean> used = new java.util.HashMap<>();
            for (NetworkControl.StockInfo r : NetworkControl.stockRules(bus.grid())) {
                used.put(r.slot(), true);
                if (r.item().getItem() == item.getItem()) {
                    existing = r.slot();
                }
            }
            for (int i = 0; i < com.simpleae.bytestorage.block.entity.NetworkControllerBlockEntity.STOCK_RULES; i++) {
                if (!used.containsKey(i)) {
                    free = i;
                    break;
                }
            }
            int slot = existing >= 0 ? existing : free;
            if (slot < 0 || !NetworkControl.setStockRule(bus.grid(), slot, item, min)) {
                return -1;
            }
            return slot;
        });
        // clear(id) — видаляє правила для предмета; повертає, скільки видалено
        def(stock, "clear", (c, s, a) -> {
            budget.spendCall();
            requireControl("stock.clear");
            Item item = requireItem(str(a, 0));
            int removed = 0;
            for (NetworkControl.StockInfo r : NetworkControl.stockRules(bus.grid())) {
                if (r.item().getItem() == item && NetworkControl.setStockRule(bus.grid(), r.slot(), ItemStack.EMPTY, 0)) {
                    removed++;
                }
            }
            return removed;
        });
        ScriptableObject.putProperty(scope, "stock", stock);

        // --- recipes: налаштування Бібліотеки рецептів ---
        Scriptable recipes = cx.newObject(scope);
        def(recipes, "blacklist", (c, s, a) -> {
            budget.spendCall();
            return idArray(c, NetworkControl.recipeBlacklist(bus.grid()));
        });
        def(recipes, "preferred", (c, s, a) -> {
            budget.spendCall();
            return idArray(c, NetworkControl.recipePreferred(bus.grid()));
        });
        def(recipes, "block", (c, s, a) -> setGhost("recipes.block", a, true, true));
        def(recipes, "unblock", (c, s, a) -> setGhost("recipes.unblock", a, true, false));
        def(recipes, "prefer", (c, s, a) -> setGhost("recipes.prefer", a, false, true));
        def(recipes, "unprefer", (c, s, a) -> setGhost("recipes.unprefer", a, false, false));
        // skipUnpacking() — читає; skipUnpacking(true/false) — змінює
        def(recipes, "skipUnpacking", (c, s, a) -> {
            budget.spendCall();
            if (a.length > 0 && a[0] != Undefined.instance) {
                requireControl("recipes.skipUnpacking");
                NetworkControl.setSkipUnpacking(bus.grid(), Context.toBoolean(a[0]));
            }
            return NetworkControl.isSkipUnpacking(bus.grid());
        });
        ScriptableObject.putProperty(scope, "recipes", recipes);

        // --- journal: журнал контролера ---
        Scriptable journal = cx.newObject(scope);
        def(journal, "write", (c, s, a) -> journalWrite(a, false));
        def(journal, "warn", (c, s, a) -> journalWrite(a, true));
        def(journal, "read", (c, s, a) -> {
            budget.spendCall();
            int limit = Math.min(30, Math.max(1, num(a, 0, 10)));
            java.util.List<Object> out = new java.util.ArrayList<>();
            for (NetworkControl.JournalInfo e : NetworkControl.readJournal(bus.grid(), limit)) {
                out.add(obj(c, "type", JOURNAL_TYPES[Math.max(0, Math.min(JOURNAL_TYPES.length - 1, e.type()))],
                        "id", e.item(), "count", e.amount(), "age", e.ageSeconds(), "text", e.text()));
            }
            return c.newArray(scope, out.toArray());
        });
        ScriptableObject.putProperty(scope, "journal", journal);

        // alert(text) — повідомлення в чат власникам бездротових терміналів (не частіше разу на 30 с з однієї шини)
        ScriptableObject.putProperty(scope, "alert", new Fn(scope, "alert", (c, s, a) -> {
            budget.spendCall();
            if (!ScriptingConfig.ALLOW_ALERTS.get()) {
                throw org.mozilla.javascript.ScriptRuntime.typeError("alert is disabled by the server admin");
            }
            if (--budget.alerts < 0) {
                throw new ScriptBudgetExceeded("alert call limit exceeded");
            }
            String text = str(a, 0);
            return text != null && NetworkControl.alert(bus.grid(), "bus:" + bus.pos().asLong(), trim(text, 120));
        }));
    }

    private interface QueueOp {
        boolean apply(IGrid grid, NetworkControl.QueueInfo entry);
    }

    private Object queueAction(String id, QueueOp op) {
        budget.spendCall();
        requireControl("queue control");
        Item item = requireItem(id);
        int affected = 0;
        for (NetworkControl.QueueInfo q : NetworkControl.queue(bus.grid())) {
            if (q.output().getItem() == item && op.apply(bus.grid(), q)) {
                affected++;
            }
        }
        return affected;
    }

    private Object setGhost(String what, Object[] a, boolean blacklist, boolean listed) {
        budget.spendCall();
        requireControl(what);
        ItemStack item = new ItemStack(requireItem(str(a, 0)));
        return blacklist ? NetworkControl.setBlacklisted(bus.grid(), item, listed) : NetworkControl.setPreferred(bus.grid(), item, listed);
    }

    private Object journalWrite(Object[] a, boolean warning) {
        budget.spendCall();
        if (--budget.journalWrites < 0) {
            throw new ScriptBudgetExceeded("journal write limit exceeded");
        }
        String text = str(a, 0);
        String icon = str(a, 1);
        ItemStack stack = icon == null ? ItemStack.EMPTY : new ItemStack(requireItem(icon));
        return text != null && NetworkControl.writeJournal(bus.grid(), trim(text, 60), stack, warning);
    }

    private static void requireControl(String what) {
        if (!ScriptingConfig.ALLOW_CONTROL.get()) {
            throw org.mozilla.javascript.ScriptRuntime.typeError(what + " is disabled by the server admin");
        }
    }

    private static String trim(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String idOf(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "unknown" : key.toString();
    }

    private Scriptable idArray(Context cx, java.util.List<ItemStack> stacks) {
        java.util.List<Object> out = new java.util.ArrayList<>();
        for (ItemStack stack : stacks) {
            out.add(idOf(stack));
        }
        return cx.newArray(scope, out.toArray());
    }

    /** Новий JS-об'єкт з пар «ім'я, значення». */
    /** Объект события для обработчиков onCraft / onLowStock / onAlert. */
    public Scriptable eventObject(Context cx, NetworkControl.EventInfo e) {
        return obj(cx, "type", JOURNAL_TYPES[Math.max(0, Math.min(JOURNAL_TYPES.length - 1, e.type()))],
                "id", e.item(), "count", e.amount(), "text", e.text());
    }

    private Scriptable obj(Context cx, Object... pairs) {
        Scriptable o = cx.newObject(scope);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            ScriptableObject.putProperty(o, (String) pairs[i], pairs[i + 1]);
        }
        return o;
    }

    private static Port portAt(BusContext ctx, Direction dir) {
        BlockEntity be = ctx.level().getBlockEntity(ctx.pos().relative(dir));
        if (be == null || be instanceof com.simpleae.bytestorage.api.network.IGridNode) {
            return null;
        }
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
        return handler == null ? null : new Port.Handler(handler);
    }

    // --- net ---

    private Scriptable buildNet(Context cx) {
        Scriptable net = cx.newObject(scope);
        def(net, "count", (c, s, a) -> {
            budget.spendCall();
            Item item = requireItem(str(a, 0));
            int total = 0;
            for (ItemStack stack : storage().getAllItems()) {
                if (stack.getItem() == item) {
                    total += stack.getCount();
                }
            }
            return total;
        });
        def(net, "has", (c, s, a) -> {
            budget.spendCall();
            Item item = requireItem(str(a, 0));
            int need = num(a, 1, 1);
            long total = 0;
            for (ItemStack stack : storage().getAllItems()) {
                if (stack.getItem() == item) {
                    total += stack.getCount();
                }
            }
            return total >= need;
        });
        def(net, "list", (c, s, a) -> {
            budget.spendCall();
            return listOf(c, new Port.Network(storage()));
        });
        def(net, "usedBytes", (c, s, a) -> storage().getTotalUsedBytes());
        def(net, "freeBytes", (c, s, a) -> storage().getRemainingBytes());
        def(net, "craft", (c, s, a) -> {
            budget.spendCall();
            if (!ScriptingConfig.ALLOW_CRAFTING.get()) {
                throw org.mozilla.javascript.ScriptRuntime.typeError("net.craft is disabled by the server admin");
            }
            if (--budget.crafts < 0) {
                throw new ScriptBudgetExceeded("craft call limit exceeded");
            }
            Item item = requireItem(str(a, 0));
            int amount = Math.max(1, num(a, 1, 1));
            IGrid grid = bus.grid();
            return CraftingService.requestCraft(grid, new ItemStack(item), amount);
        });
        return net;
    }

    private INetworkStorage storage() {
        IGrid grid = bus.grid();
        if (grid == null || grid.getStorage() == null) {
            throw org.mozilla.javascript.ScriptRuntime.typeError("network is offline");
        }
        return grid.getStorage();
    }

    // --- endpoint ---

    private Scriptable buildEndpoint(Context cx, Port port) {
        Scriptable ep = cx.newObject(scope);
        endpoints.put(ep, port);
        def(ep, "slots", (c, s, a) -> port.slots());
        def(ep, "list", (c, s, a) -> {
            budget.spendCall();
            return listOf(c, port);
        });
        def(ep, "count", (c, s, a) -> {
            budget.spendCall();
            Item item = requireItem(str(a, 0));
            int total = 0;
            for (int i = 0; i < port.slots(); i++) {
                ItemStack stack = port.get(i);
                if (stack.getItem() == item) {
                    total += stack.getCount();
                }
            }
            return total;
        });
        // pull([id], [n]) — з інвентаря в мережу
        def(ep, "pull", (c, s, a) -> {
            budget.spendCall();
            return Port.transfer(port, new Port.Network(storage()), optItem(str(a, 0)), num(a, 1, Integer.MAX_VALUE), budget);
        });
        // push(id, [n]) — з мережі в інвентар
        def(ep, "push", (c, s, a) -> {
            budget.spendCall();
            Item item = requireItem(str(a, 0));
            return Port.transfer(new Port.Network(storage()), port, item, num(a, 1, Integer.MAX_VALUE), budget);
        });
        // move(other, [id], [n]) — між двома інвентарями напряму
        def(ep, "move", (c, s, a) -> {
            budget.spendCall();
            Port other = a.length > 0 && a[0] instanceof Scriptable sc ? endpoints.get(sc) : null;
            if (other == null) {
                throw org.mozilla.javascript.ScriptRuntime.typeError("move(): first argument must be an endpoint from side()");
            }
            return Port.transfer(port, other, optItem(str(a, 1)), num(a, 2, Integer.MAX_VALUE), budget);
        });
        return ep;
    }

    // --- утиліти ---

    private Scriptable listOf(Context cx, Port port) {
        java.util.List<Object> out = new java.util.ArrayList<>();
        for (int i = 0; i < port.slots() && out.size() < LIST_LIMIT; i++) {
            ItemStack stack = port.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            Scriptable entry = cx.newObject(scope);
            ScriptableObject.putProperty(entry, "slot", i);
            ScriptableObject.putProperty(entry, "id", key == null ? "unknown" : key.toString());
            ScriptableObject.putProperty(entry, "count", stack.getCount());
            out.add(entry);
        }
        return cx.newArray(scope, out.toArray());
    }

    private void def(Scriptable target, String name, Fn.Impl impl) {
        ScriptableObject.putProperty(target, name, new Fn(scope, name, impl));
    }

    private static String str(Object[] a, int i) {
        if (a.length <= i || a[i] == null || a[i] == Undefined.instance) {
            return null;
        }
        return Context.toString(a[i]);
    }

    private static int num(Object[] a, int i, int def) {
        if (a.length <= i || a[i] == null || a[i] == Undefined.instance) {
            return def;
        }
        double d = Context.toNumber(a[i]);
        if (Double.isNaN(d)) {
            return 0;
        }
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, d));
    }

    /** null або "*" — будь-який предмет. */
    private static Item optItem(String id) {
        return id == null || id.equals("*") ? null : requireItem(id);
    }

    private static Item requireItem(String id) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
        Item item = rl == null ? null : ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            throw org.mozilla.javascript.ScriptRuntime.typeError("unknown item: " + id);
        }
        return item;
    }
}
