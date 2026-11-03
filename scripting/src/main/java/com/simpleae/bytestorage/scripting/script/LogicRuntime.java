package com.simpleae.bytestorage.scripting.script;

import com.simpleae.bytestorage.scripting.ScriptingConfig;
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
            Object tick = scope.get("tick", scope);
            if (tick == UniqueTag.NOT_FOUND || !(tick instanceof Function)) {
                return "script must define function tick()";
            }
            tickFn = (Function) tick;
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

    /** Один запуск tick(). Повертає текст помилки або null. */
    public String tick() {
        if (tickFn == null) {
            return "not compiled";
        }
        Context cx = SandboxFactory.INSTANCE.enterContext();
        try {
            Budget budget = newBudget();
            cx.putThreadLocal(SandboxFactory.BUDGET_KEY, budget);
            api.beginRun(budget);
            tickFn.call(cx, scope, scope, new Object[0]);
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
