package com.simpleae.bytestorage.scripting.script;

/** Кидається, коли скрипт вичерпав ліміт (Error, щоб Rhino не дозволив скрипту його перехопити через try/catch). */
public final class ScriptBudgetExceeded extends Error {
    public ScriptBudgetExceeded(String message) {
        super(message, null, false, false);
    }
}
