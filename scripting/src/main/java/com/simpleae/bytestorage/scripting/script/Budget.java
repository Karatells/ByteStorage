package com.simpleae.bytestorage.scripting.script;

/** Ресурси одного запуску скрипта. */
public final class Budget {
    public long ops;
    public final long maxOps;
    public final long deadlineNanos;
    public int items;
    public int calls;
    public int crafts = 4;
    public int journalWrites = 4;
    public int alerts = 1;

    public Budget(long maxOps, long maxMillis, int items, int calls) {
        this.maxOps = maxOps;
        this.deadlineNanos = System.nanoTime() + maxMillis * 1_000_000L;
        this.items = items;
        this.calls = calls;
    }

    /** Списує один виклик API; при вичерпанні зупиняє скрипт. */
    public void spendCall() {
        if (--calls < 0) {
            throw new ScriptBudgetExceeded("API call limit exceeded");
        }
    }
}
