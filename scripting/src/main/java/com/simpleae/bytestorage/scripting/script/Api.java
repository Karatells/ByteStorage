package com.simpleae.bytestorage.scripting.script;

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
