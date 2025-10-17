package io.github.crystalprocesses.xml.debugger;

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpointListener;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class XmlBreakpointListener implements XBreakpointListener<XLineBreakpoint<XBreakpointProperties<?>>> {

    private final Project project;

    public XmlBreakpointListener(Project project) {
        this.project = project;
    }

    @Override
    public void breakpointAdded(@NotNull XLineBreakpoint<XBreakpointProperties<?>> breakpoint) {
        if (breakpoint.getType() instanceof XmlBreakpointType) {
            createLinkedJavaBreakpoint(project, convertToJavaBreakpoint(breakpoint));
        }
    }

    @Override
    public void breakpointRemoved(@NotNull XLineBreakpoint<XBreakpointProperties<?>> breakpoint) {
        if (breakpoint.getType() instanceof XmlBreakpointType) {
            getAssociatedBreakpoint(convertToJavaBreakpoint(breakpoint)).ifPresent(
                    point ->
                            XDebuggerManager.getInstance(project)
                                    .getBreakpointManager()
                                    .removeBreakpoint(point)

            );
        }
    }

    private Optional<XLineBreakpoint<JavaLineBreakpointProperties>> getAssociatedBreakpoint(JavaBreakPoint internalBreakPoint) {
        return getBreakpointsInFile(internalBreakPoint).stream()
                .filter(javaBreakpoint -> internalBreakPoint.condition.equals(Objects.requireNonNull(javaBreakpoint.getConditionExpression()).getExpression()))
                .findFirst();
    }

    private @NotNull Collection<XLineBreakpoint<JavaLineBreakpointProperties>> getBreakpointsInFile(JavaBreakPoint javaBreakPoint) {
        return XDebuggerManager.getInstance(project)
                .getBreakpointManager()
                .findBreakpointsAtLine(XBreakpointType.EXTENSION_POINT_NAME.findExtension(JavaLineBreakpointType.class),
                        Objects.requireNonNull(VirtualFileManager.getInstance().findFileByUrl(javaBreakPoint.resource())), javaBreakPoint.line());
    }

    @Override
    public void breakpointChanged(@NotNull XLineBreakpoint<XBreakpointProperties<?>> breakpoint) {
        // React if condition or enabled state changes
    }

    private static void createLinkedJavaBreakpoint(Project project, JavaBreakPoint breakPoint) {
        XDebuggerManager.getInstance(project)
                .getBreakpointManager()
                .addLineBreakpoint(
                        XBreakpointType.EXTENSION_POINT_NAME.findExtension(JavaLineBreakpointType.class),
                        breakPoint.resource(), breakPoint.line(), new JavaLineBreakpointProperties()
                ).setCondition(breakPoint.condition);
    }

    private @NotNull JavaBreakPoint convertToJavaBreakpoint(XLineBreakpoint<XBreakpointProperties<?>> bpmnBreakpoint) {
        try {
            XmlTag xmlTag = getXmlTag(bpmnBreakpoint);
            return createJavaBreakPoint(
                    getFirstBreakpointCondition(XmlDebuggerSettings.getInstance(project), xmlTag),
                    xmlTag
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private @Nullable XmlTag getXmlTag(XLineBreakpoint<XBreakpointProperties<?>> bpmnBreakpoint) {
        VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(bpmnBreakpoint.getFileUrl());
        return XmlBreakpointType.getXmlTag(virtualFile, bpmnBreakpoint.getLine(), project);
    }

    private @NotNull JavaBreakPoint createJavaBreakPoint(BreakpointConditionEntry breakpointConditionEntry, XmlTag xmlTag) throws MalformedURLException {
        return new JavaBreakPoint(
                findClassUrl(project, breakpointConditionEntry.className),
                breakpointConditionEntry.lineNumber,
                ExpressionEvaluator.getInstance(project).evaluateExpression(breakpointConditionEntry.condition, xmlTag).toString());
    }

    private @NotNull BreakpointConditionEntry getFirstBreakpointCondition(XmlDebuggerSettings xmlDebuggerSettings, XmlTag xmlTag) {
        return xmlDebuggerSettings.entries.stream().filter(
                breakpointConditionEntry -> breakpointConditionEntry.xmlTag != null &&
                        (boolean) ExpressionEvaluator.getInstance(project).evaluateExpression(breakpointConditionEntry.xmlTag, xmlTag)
        ).findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported xml tag " + xmlTag.getName() + " for debugging."));
    }

    private static @NotNull String findClassUrl(Project project, String fqName) throws MalformedURLException {
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(fqName, GlobalSearchScope.allScope(project));
        if (psiClass == null) return null;

        PsiFile psiFile = psiClass.getNavigationElement().getContainingFile();
        VirtualFile vFile = psiFile.getVirtualFile();
        if (vFile == null) return null;

        return vFile.getUrl();
    }

    private record JavaBreakPoint(String resource, int line, String condition) {

    }
}
