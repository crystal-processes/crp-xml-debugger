package io.github.crystalprocesses.xml.debugger;


import com.google.gson.Gson;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@State(name = "XmlDebuggerSettings", storages = @Storage("XmlDebuggerSettings.xml"))
public class XmlDebuggerSettings implements PersistentStateComponent<XmlDebuggerSettings> {

    public List<BreakpointConditionEntry> entries = new ArrayList<>();
    @Override
    public @Nullable XmlDebuggerSettings getState() {
        return this;
    }

    @Override
    public void loadState(@Nullable XmlDebuggerSettings state) {
        if (state != null) this.entries = state.entries;
    }

    public static XmlDebuggerSettings getInstance(Project project) {
        return project.getService(XmlDebuggerSettings.class);
    }

    public static XmlDebuggerSettings loadDefaults() {
        try (InputStream is = XmlDebuggerSettings.class.getResourceAsStream("/defaults.json")) {
            if (is != null) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return new Gson().fromJson(json, XmlDebuggerSettings.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new XmlDebuggerSettings(); // fallback
    }
}
