package org.mozilla.javascript.tests;

import junit.framework.TestCase;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.tools.shell.Global;

import java.util.ArrayList;
import java.util.List;

public class AccessingJavaList extends TestCase {
    protected final Global global = new Global();

    public AccessingJavaList() {
        global.init(ContextFactory.getGlobal());
    }


    public void testAccessingJavaListIntegerValues() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(2, runScriptAsInt("value[1]", list));
        assertEquals(3, runScriptAsInt("value[2]", list));
    }

    public void testUpdateingJavaListIntegerValues() {
        List<Number> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertEquals(2, runScriptAsInt("value[1]", list));
        assertEquals(5, runScriptAsInt("value[1]=5;value[1]", list));
        assertEquals(5, list.get(1).intValue());
    }

    public void testAccessingJavaListStringValues() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        assertEquals("b", runScriptAsString("value[1]", list));
        assertEquals("c", runScriptAsString("value[2]", list));
    }

    public void testUpdatetingJavaListStringValues() {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        assertEquals("b", runScriptAsString("value[1]", list));
        assertEquals("f", runScriptAsString("value[1]=\"f\";value[1]", list));
        assertEquals("f", list.get(1));
    }

    private int runScriptAsInt(final String scriptSourceText, final Object value) {
        return ContextFactory.getGlobal().call(context -> {
            Scriptable scope = context.initStandardObjects(global);
            scope.put("value", scope, Context.javaToJS(value, scope));
            return (int)Context.toNumber(context.evaluateString(scope, scriptSourceText, "", 1, null));
        });
    }

    private String runScriptAsString(final String scriptSourceText, final Object value) {
        return ContextFactory.getGlobal().call(context -> {
            Scriptable scope = context.initStandardObjects(global);
            scope.put("value", scope, Context.javaToJS(value, scope));
            return Context.toString(context.evaluateString(scope, scriptSourceText, "", 1, null));
        });
    }
}