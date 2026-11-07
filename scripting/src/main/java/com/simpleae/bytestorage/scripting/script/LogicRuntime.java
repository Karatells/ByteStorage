package com.simpleae.bytestorage.scripting.script;

import com.simpleae.bytestorage.scripting.ScriptingConfig;
import com.simpleae.bytestorage.api.NetworkControl;
import com.simpleae.bytestorage.util.JournalLog;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UniqueTag;

/**
 * Пісочниця одного скрипта (одна Логічна шина). Верхній рівень скрипта виконується один раз при компіляції,
 * потім кожні N тіків викликається {@code function tick() { ... }}. Глобальні змінні живуть між викликами
 * (до перезавантаження світу або зміни скрипта).
 */
public final class LogicRuntime {

    private final BusContext bus;
    private ScriptableObject scope;
    private Function tickFn;
    private Function onCraft;
    private Function onLowStock;
    private Function onAlert;
    /** Номер последнего обработанного события журнала; -1 — ещё не определён (сеть не была доступна). */
    private long eventCursor = -1;
    private static final int MAX_EVENTS_PER_RUN = 10;
    private Api api;

    public LogicRuntime(BusContext bus) {
        this.bus = bus;
    }

    private Budget newBudget() {
        return new Budget(
                ScriptingConfig.MAX_INSTRUCTIONS.get(),
                ScriptingConfig.MAX_MILLIS.get(),
                ScriptingConfig.MAX_ITEMS_PER_RUN.get(),
                ScriptingConfig.MAX_CALLS_PER_RUN.get());
    }

    /** Компілює та виконує верхній рівень. Повертає текст помилки або null. */
    public String compile(String source) {
        tickFn = null;
        onCraft = null;
        onLowStock = null;
        onAlert = null;
        eventCursor = -1;
        Context cx = SandboxFactory.INSTANCE.enterContext();
        try {
            Budget budget = newBudget();
            cx.putThreadLocal(SandboxFactory.BUDGET_KEY, budget);
            scope = cx.initSafeStandardObjects();
            hardenGlobals(scope);
            api = new Api(bus, scope);
            api.beginRun(budget);
            api.install(cx);
            Script script = cx.compileString(source, "script", 1, null);
            script.exec(cx, scope);
            tickFn = function("tick");
            onCraft = function("onCraft");
            onLowStock = function("onLowStock");
            onAlert = function("onAlert");
            if (tickFn == null && onCraft == null && onLowStock == null && onAlert == null) {
                return "script must define function tick() or an event handler (onCraft, onLowStock, onAlert)";
            }
            return null;
        } catch (RhinoException e) {
            return describe(e);
        } catch (ScriptBudgetExceeded e) {
            return e.getMessage();
        } catch (RuntimeException | StackOverflowError e) {
            return "internal error: " + e;
        } finally {
            cx.removeThreadLocal(SandboxFactory.BUDGET_KEY);
            Context.exit();
        }
    }

    private Function function(String name) {
        Object f = scope.get(name, scope);
        return f instanceof Function fn && f != UniqueTag.NOT_FOUND ? fn : null;
    }

    private boolean compiled() {
        return tickFn != null || onCraft != null || onLowStock != null || onAlert != null;
    }

    /** Передаёт скрипту новые события журнала (до {@link #MAX_EVENTS_PER_RUN} за запуск, остальные отбрасываются). */
    private void dispatchEvents(Context cx) {
        if (onCraft == null && onLowStock == null && onAlert == null) {
            return;
        }
        if (bus.grid() == null) {
            return;
        }
        if (eventCursor < 0) {
            eventCursor = NetworkControl.eventCursor(bus.grid());
            return; // старые события не проигрываем
        }
        int handled = 0;
        for (NetworkControl.EventInfo e : NetworkControl.eventsSince(bus.grid(), eventCursor)) {
            eventCursor = e.seq();
            Function handler = switch (e.type()) {
                case JournalLog.CRAFTED -> onCraft;
                case JournalLog.STOCK_ORDER -> onLowStock;
                case JournalLog.STOCK_FAILED, JournalLog.STUCK -> onAlert;
                default -> null;
            };
            if (handler != null && handled++ < MAX_EVENTS_PER_RUN) {
                handler.call(cx, scope, scope, new Object[]{api.eventObject(cx, e)});
            }
        }
    }

    /** Один запуск: события, затем tick(). Повертає текст помилки або null. */
    public String tick() {
        if (!compiled()) {
            return "not compiled";
        }
        Context cx = SandboxFactory.INSTANCE.enterContext();
        try {
            Budget budget = newBudget();
            cx.putThreadLocal(SandboxFactory.BUDGET_KEY, budget);
            api.beginRun(budget);
            dispatchEvents(cx);
            if (tickFn != null) {
                tickFn.call(cx, scope, scope, new Object[0]);
            }
            return null;
        } catch (RhinoException e) {
            return describe(e);
        } catch (ScriptBudgetExceeded e) {
            return e.getMessage();
        } catch (RuntimeException | StackOverflowError e) {
            return "internal error: " + e;
        } finally {
            cx.removeThreadLocal(SandboxFactory.BUDGET_KEY);
            Context.exit();
        }
    }

    private static String describe(RhinoException e) {
        String msg = e.details();
        return e.lineNumber() > 0 ? msg + " (line " + e.lineNumber() + ")" : msg;
    }

    /** Прибирає вбудовані методи, що дозволяють одним викликом виділити гігантську пам'ять. */
    private static void hardenGlobals(Scriptable scope) {
        removeFrom(scope, "String", "repeat", "padStart", "padEnd");
        removeFrom(scope, "Array", "fill");
    }

    private static void removeFrom(Scriptable scope, String className, String... names) {
        Scriptable proto = ScriptableObject.getClassPrototype(scope, className);
        if (proto == null) {
            return;
        }
        for (String name : names) {
            proto.delete(name);
        }
    }
}
