package io.github.crystalprocesses.xml.debugger;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpointType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class XmlBreakpointType extends XLineBreakpointType<XBreakpointProperties<?>> {

    public static final String ID = "xml-breakpoint";

    public XmlBreakpointType() {
        super(ID, "XML Breakpoint");
    }

    @Override
    public boolean canPutAt(@NotNull VirtualFile file, int line, @NotNull Project project) {
        return isXmlFile(file) && hasEntry(file, line, project);
    }

    private static boolean isXmlFile(@NotNull VirtualFile file) {
        return file.getName().endsWith(".bpmn") || file.getName().endsWith(".xml");
    }

    private static boolean hasEntry(@NotNull VirtualFile file, int line, @NotNull Project project) {
        XmlTag tag = getXmlTag(file, line, project);
        if (tag != null) {
            XmlDebuggerSettings settings = XmlDebuggerSettings.getInstance(project);
            ExpressionEvaluator expressionEvaluator = ExpressionEvaluator.getInstance(project);
            return settings.entries.stream().anyMatch(
                    e -> StringUtils.isNotEmpty(e.xmlTag)
                            && (boolean) expressionEvaluator.evaluateExpression(e.xmlTag, tag)
            );
        }
        return false;
    }

    public static @Nullable XmlTag getXmlTag(@NotNull VirtualFile file, int line, @NotNull Project project) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return null;
        }
        PsiElement element = psiFile.findElementAt(psiFile.getViewProvider().getDocument().getLineStartOffset(line));
        return PsiTreeUtil.getNextSiblingOfType(element, XmlTag.class);
    }

    @Override
    public @Nullable XBreakpointProperties<?> createBreakpointProperties(@NotNull VirtualFile file, int line) {
        return null;
    }

    @Override
    public Icon getEnabledIcon() {
        return AllIcons.Debugger.Db_set_breakpoint;
    }

}
