package io.github.crystalprocesses.xml.debugger;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class XmlDebuggerPostStartupActivity implements ProjectActivity {

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        addBreakpointListener(project);
        loadDefaultSettings(project);
        return continuation;
    }

    private static void loadDefaultSettings(@NotNull Project project) {
        XmlDebuggerSettings projectSettings = XmlDebuggerSettings.getInstance(project);
        if (projectSettings.entries.isEmpty()) {
            projectSettings.entries = new ArrayList<>(XmlDebuggerSettings.loadDefaults().entries);
        }
    }

    private static void addBreakpointListener(@NotNull Project project) {
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project)
                .getBreakpointManager();
        breakpointManager.addBreakpointListener(
                XBreakpointType.EXTENSION_POINT_NAME.findExtension(XmlBreakpointType.class),
                new XmlBreakpointListener(project)
        );
    }
}

