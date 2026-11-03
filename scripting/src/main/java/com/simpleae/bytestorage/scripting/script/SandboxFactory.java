package com.simpleae.bytestorage.scripting.script;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/**
 * Фабрика контекстів Rhino з обмеженнями: інтерпретатор (-1), без доступу до Java (ClassShutter «нічого»),
 * ліміт інструкцій та часу через спостерігач, обмежена глибина стека.
 */
final class SandboxFactory extends ContextFactory {

    static final SandboxFactory INSTANCE = new SandboxFactory();
    static final Object BUDGET_KEY = new Object();

    private SandboxFactory() {
    }

    @Override
    protected Context makeContext() {
        Context cx = super.makeContext();
        cx.setOptimizationLevel(-1);
        cx.setLanguageVersion(Context.VERSION_ES6);
        cx.setMaximumInterpreterStackDepth(120);
        cx.setInstructionObserverThreshold(1000);
        cx.setClassShutter(name -> false);
        return cx;
    }

    @Override
    protected void observeInstructionCount(Context cx, int instructionCount) {
        Budget budget = (Budget) cx.getThreadLocal(BUDGET_KEY);
        if (budget == null) {
            return;
        }
        budget.ops += instructionCount;
        if (budget.ops > budget.maxOps) {
            throw new ScriptBudgetExceeded("instruction limit exceeded");
        }
        if (System.nanoTime() > budget.deadlineNanos) {
            throw new ScriptBudgetExceeded("time limit exceeded");
        }
    }
}
