package io.github.crystalprocesses.xml.debugger;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.xml.XmlTag;
import groovy.lang.Singleton;
import org.jetbrains.annotations.NotNull;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;

@Singleton
@Service(Service.Level.PROJECT)
public final class ExpressionEvaluator {
    private static final Logger LOG = Logger.getInstance(ExpressionEvaluator.class);

    private final ScriptEngine engine;

    public ExpressionEvaluator() {
        this.engine = new ScriptEngineManager().getEngineByName("groovy");
    }

    public static ExpressionEvaluator getInstance(Project project) {
        return project.getService(ExpressionEvaluator.class);
    }

    public Object evaluateExpression(String expr, XmlTag xmlTag) {
        Object result;
        try {
            result = engine.eval(expr, createBindings(Map.of("xmlTag", xmlTag)));
        } catch (ScriptException e) {
            LOG.error("Unable to evaluate script '"+ expr+"' for tag '"+xmlTag+"'", e);
            throw new RuntimeException(e);
        }
        return result;
    }
    private @NotNull Bindings createBindings(Map<String, Object> vars) {
        Bindings bindings = engine.createBindings();
        bindings.putAll(vars);
        return bindings;
    }

}