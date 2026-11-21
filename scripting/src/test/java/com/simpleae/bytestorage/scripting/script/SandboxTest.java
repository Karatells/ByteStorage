package com.simpleae.bytestorage.scripting.script;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юніт-тести пісочниці Rhino: ліміти інструкцій, часу та блокування доступу до Java.
 */
class SandboxTest {

    private Object eval(String source, Budget budget) {
        Context cx = SandboxFactory.INSTANCE.enterContext();
        try {
            cx.putThreadLocal(SandboxFactory.BUDGET_KEY, budget);
            ScriptableObject scope = cx.initSafeStandardObjects();
            return cx.evaluateString(scope, source, "test", 1, null);
        } finally {
            cx.removeThreadLocal(SandboxFactory.BUDGET_KEY);
            Context.exit();
        }
    }

    @Test
    void normalScriptExecutesWithinBudget() {
        Budget budget = new Budget(10_000, 1_000, 256, 200);
        Object res = eval("const arr = [1, 2, 3]; arr.reduce((a, b) => a + b, 0);", budget);
        assertEquals(6.0, ((Number) res).doubleValue(), 0.0001);
        assertTrue(budget.ops < 10_000);
    }

    @Test
    void infiniteLoopHitsInstructionLimit() {
        Budget budget = new Budget(2_000, 5_000, 256, 200);
        ScriptBudgetExceeded ex = assertThrows(ScriptBudgetExceeded.class,
                () -> eval("while (true) {}", budget));
        assertTrue(ex.getMessage().contains("instruction"));
    }

    @Test
    void expiredDeadlineHitsTimeLimit() {
        Budget budget = new Budget(10_000_000, 0, 256, 200);
        ScriptBudgetExceeded ex = assertThrows(ScriptBudgetExceeded.class,
                () -> eval("let x = 0; for (let i = 0; i < 100000; i++) { x += i; }", budget));
        assertTrue(ex.getMessage().contains("time"));
    }

    @Test
    void javaAccessIsBlockedByClassShutter() {
        Budget budget = new Budget(10_000, 1_000, 256, 200);
        assertThrows(RhinoException.class,
                () -> eval("java.lang.System.exit(1);", budget));
        assertThrows(RhinoException.class,
                () -> eval("Packages.java.lang.Runtime.getRuntime();", budget));
    }

    @Test
    void allScriptTemplatesCompileCleanly() {
        for (var tpl : com.simpleae.bytestorage.scripting.client.ScriptTemplates.ALL) {
            Context cx = SandboxFactory.INSTANCE.enterContext();
            try {
                cx.putThreadLocal(SandboxFactory.BUDGET_KEY, new Budget(50_000, 2_000, 256, 200));
                assertNotNull(cx.compileString(tpl.code(), tpl.name(), 1, null), tpl.name());
            } finally {
                cx.removeThreadLocal(SandboxFactory.BUDGET_KEY);
                Context.exit();
            }
        }
    }
}
