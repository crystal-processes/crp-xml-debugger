package io.github.crystalprocesses.xml.debugger;


import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class XmlDebuggerConfigurable implements Configurable {
    private final Project project;
    private JTable table;
    private EntryTableModel tableModel;
    private List<BreakpointConditionEntry> workingCopy;

    public XmlDebuggerConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Xml Debugger Plugin Breakpoint Conditions";
    }

    @Override
    public @Nullable JComponent createComponent() {
        workingCopy = new ArrayList<>(XmlDebuggerSettings.getInstance(project).entries);
        tableModel = new EntryTableModel();
        table = new JBTable(tableModel);

        // Wrap table with IntelliJ's ToolbarDecorator for add/remove buttons
        JPanel panel = ToolbarDecorator.createDecorator(table)
                .setAddAction(button -> {
                    int row = workingCopy.size();
                    workingCopy.add(new BreakpointConditionEntry());
                    tableModel.fireTableRowsInserted(row, row);  // notify table
                    table.setRowSelectionInterval(row, row);
                })
                .setRemoveAction(button -> {
                    int row = table.getSelectedRow();
                    if (row >= 0 && row < workingCopy.size()) {
                        workingCopy.remove(row);
                        if (row > 0) {
                            table.setRowSelectionInterval(row-1, row-1);
                        } else {
                            table.setRowSelectionInterval(0,0);
                        }
                        tableModel.fireTableRowsDeleted(row, row);  // notify table
                    }
                })
                .setMoveUpAction(button -> {
                    int row = table.getSelectedRow();
                    if (row >= 1 && row < workingCopy.size()) {
                        BreakpointConditionEntry temp = workingCopy.get(row);
                        workingCopy.set(row, workingCopy.get(row-1));
                        workingCopy.set(row-1, temp);
                        table.setRowSelectionInterval(row-1, row-1);
                        tableModel.fireTableRowsUpdated(row-1, row);  // notify table
                    }
                })
                .setMoveDownAction(button -> {
                    int row = table.getSelectedRow();
                    if (row >= 0  && row < workingCopy.size()-1) {
                        BreakpointConditionEntry temp = workingCopy.get(row);
                        workingCopy.set(row, workingCopy.get(row+1));
                        workingCopy.set(row+1, temp);
                        table.setRowSelectionInterval(row+1, row+1);
                        tableModel.fireTableRowsUpdated(row, row+1);  // notify table
                    }
                })
                .createPanel();

        panel.setPreferredSize(new Dimension(700, 300));
        return new JBScrollPane(panel);
    }

    @Override
    public boolean isModified() {
        return !workingCopy.equals(XmlDebuggerSettings.getInstance(project).entries);
    }

    @Override
    public void apply() {
        XmlDebuggerSettings.getInstance(project).entries = new java.util.ArrayList<>(workingCopy);
    }

    @Override
    public void reset() {
        workingCopy = new java.util.ArrayList<>(
                XmlDebuggerSettings.getInstance(project).entries
        );
        table.setModel(new EntryTableModel());
    }

    private class EntryTableModel extends AbstractTableModel {
        private final String[] columns = {"XML Tag", "Class", "Line", "Condition"};

        @Override
        public int getRowCount() { return workingCopy.size(); }

        @Override
        public int getColumnCount() { return columns.length; }

        @Override
        public String getColumnName(int col) { return columns[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            BreakpointConditionEntry e = workingCopy.get(row);
            return switch (col) {
                case 0 -> e.xmlTag;
                case 1 -> e.className;
                case 2 -> e.lineNumber;
                case 3 -> e.condition;
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int row, int col) { return true; }

        @Override
        public void setValueAt(Object value, int row, int col) {
            BreakpointConditionEntry e = workingCopy.get(row);
            switch (col) {
                case 0 -> e.xmlTag = (String) value;
                case 1 -> e.className = (String) value;
                case 2 -> e.lineNumber = Integer.parseInt(value.toString());
                case 3 -> e.condition = (String) value;
            }
            fireTableCellUpdated(row, col);
        }
    }
}