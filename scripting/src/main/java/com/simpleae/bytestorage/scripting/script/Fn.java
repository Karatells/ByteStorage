package com.simpleae.bytestorage.scripting.script;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/** Нативна JS-функція, реалізована на Java. Повертає лише примітиви та Scriptable (без обгорток Java-об'єктів). */
final class Fn extends BaseFunction {

    interface Impl {
        Object call(Context cx, Scriptable scope, Object[] args);
    }

    private final String name;
    private final Impl impl;

    Fn(Scriptable scope, String name, Impl impl) {
        this.name = name;
        this.impl = impl;
        org.mozilla.javascript.ScriptRuntime.setFunctionProtoAndParent(this, scope);
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        return impl.call(cx, scope, args);
    }

    @Override
    public String getFunctionName() {
        return name;
    }

    @Override
    public int getLength() {
        return 0;
    }
}
