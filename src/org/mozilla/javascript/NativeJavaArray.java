/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * This class reflects Java arrays into the JavaScript environment.
 *
 * @author Mike Shaver
 * @see NativeJavaClass
 * @see NativeJavaObject
 * @see NativeJavaPackage
 */

public class NativeJavaArray
    extends NativeJavaObject
    implements SymbolScriptable
{
    private static final long serialVersionUID = -924022554283675333L;

    /**
     * Implementation of the Array.indexOf method.
     */
    private static class IndexOfMethod extends BaseFunction {
        private Object array;

        IndexOfMethod(Object array) {
            this.array = array;
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            Object arg = args[0];
            if (arg instanceof Wrapper)
                arg = ((Wrapper)arg).unwrap();

            int start = 0;
            if (args.length > 1)
                start = ((Number)args[1]).intValue();
            if (start < 0)
                start = args.length + start;

            int length = Array.getLength(array);
            for (int index = start; index < length; index++)
                if (Objects.deepEquals(arg, Array.get(array, index)))
                    return index;
            return -1;
        }
    }

    /**
     * Implementation of the Array.includes method.
     */
    private static class IncludesMethod extends BaseFunction {
        private Object array;

        IncludesMethod(Object array) {
            this.array = array;
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            Object arg = args[0];
            if (arg instanceof Wrapper)
                arg = ((Wrapper)arg).unwrap();

            int start = 0;
            if (args.length > 1)
                start = ((Number)args[1]).intValue();

            int length = Array.getLength(array);
            for (int index = start; index < length; index++)
                if (Objects.deepEquals(arg, Array.get(array, index)))
                    return true;
            return false;
        }
    }

    @Override
    public String getClassName() {
        return "JavaArray";
    }

    public static NativeJavaArray wrap(Scriptable scope, Object array) {
        return new NativeJavaArray(scope, array);
    }

    @Override
    public Object unwrap() {
        return array;
    }

    public NativeJavaArray(Scriptable scope, Object array) {
        super(scope, null, ScriptRuntime.ObjectClass);
        Class<?> cl = array.getClass();
        if (!cl.isArray()) {
            throw new RuntimeException("Array expected");
        }
        this.array = array;
        this.length = Array.getLength(array);
        this.cls = cl.getComponentType();
    }

    @Override
    public boolean has(String id, Scriptable start) {
        return id.equals("length")  || id.equals("indexOf") || id.equals("includes") || super.has(id, start);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return 0 <= index && index < length;
    }

    @Override
    public boolean has(Symbol key, Scriptable start) {
        return SymbolKey.IS_CONCAT_SPREADABLE.equals(key);
    }

    @Override
    public Object get(String id, Scriptable start) {
        if (id.equals("length"))
            return Integer.valueOf(length);
        else if (id.equals("indexOf"))
            return new IndexOfMethod(array);
        else if (id.equals("includes"))
            return new IncludesMethod(array);

        Object result = super.get(id, start);
        if (result == NOT_FOUND &&
            !ScriptableObject.hasProperty(getPrototype(), id))
        {
            throw Context.reportRuntimeError2(
                "msg.java.member.not.found", array.getClass().getName(), id);
        }
        return result;
    }

    @Override
    public Object get(int index, Scriptable start) {
        if (0 <= index && index < length) {
            Context cx = Context.getContext();
            Object obj = Array.get(array, index);
            return cx.getWrapFactory().wrap(cx, this, obj, cls);
        }
        return Undefined.instance;
    }

    @Override
    public Object get(Symbol key, Scriptable start) {
        if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
            return true;
        }
        return Scriptable.NOT_FOUND;
    }

    @Override
    public void put(String id, Scriptable start, Object value) {
        // Ignore assignments to "length"--it's readonly.
        if (!id.equals("length"))
            throw Context.reportRuntimeError1(
                "msg.java.array.member.not.found", id);
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        if (0 <= index && index < length) {
            Array.set(array, index, Context.jsToJava(value, cls));
        }
        else {
            throw Context.reportRuntimeError2(
                "msg.java.array.index.out.of.bounds", String.valueOf(index),
                String.valueOf(length - 1));
        }
    }

    @Override
    public void delete(Symbol key) {
        // All symbols are read-only
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == null || hint == ScriptRuntime.StringClass)
            return array.toString();
        if (hint == ScriptRuntime.BooleanClass)
            return Boolean.TRUE;
        if (hint == ScriptRuntime.NumberClass)
            return ScriptRuntime.NaNobj;
        return this;
    }

    @Override
    public Object[] getIds() {
        Object[] result = new Object[length];
        int i = length;
        while (--i >= 0)
            result[i] = Integer.valueOf(i);
        return result;
    }

    @Override
    public boolean hasInstance(Scriptable value) {
        if (!(value instanceof Wrapper))
            return false;
        Object instance = ((Wrapper)value).unwrap();
        return cls.isInstance(instance);
    }

    @Override
    public Scriptable getPrototype() {
        if (prototype == null) {
            prototype =
                ScriptableObject.getArrayPrototype(this.getParentScope());
        }
        return prototype;
    }

    Object array;
    int length;
    Class<?> cls;
}
