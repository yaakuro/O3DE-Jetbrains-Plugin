/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.lua;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.o3de.clion.remote.script.LuaReferenceData;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Locale;

/**
 * Tool window tab listing the Lua API references (Classes, EBuses, Globals) read from a connected
 * O3DE target.
 */
public final class LuaReferencePanel extends JPanel implements LuaReferenceService.Listener, Disposable {

    private final Project project;
    private final LuaReferenceService service;

    private final JBLabel status = new JBLabel("Not connected");
    private final JBTextField filter = new JBTextField();
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("O3DE Lua API");
    private final DefaultTreeModel model = new DefaultTreeModel(root);
    private final JTree tree = new JTree(model);

    public LuaReferencePanel(@NotNull Project project) {
        super(new BorderLayout(0, 4));
        this.project = project;
        this.service = LuaReferenceService.getInstance(project);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> service.refresh());

        JPanel top = new JPanel(new BorderLayout(4, 4));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.add(refresh);
        actions.add(status);
        top.add(actions, BorderLayout.NORTH);
        filter.getEmptyText().setText("Filter classes, EBus events and globals");
        top.add(filter, BorderLayout.SOUTH);
        top.setBorder(JBUI.Borders.empty(4));

        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rebuild();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rebuild();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rebuild();
            }
        });

        add(top, BorderLayout.NORTH);
        add(new JBScrollPane(tree), BorderLayout.CENTER);

        service.addListener(this);
        update();
        if (service.isEnabled() && service.getData() == null) {
            service.refresh();
        }
    }

    @Override
    public void referenceDataChanged() {
        ApplicationManager.getApplication().invokeLater(this::update);
    }

    private void update() {
        status.setText(service.isRefreshing() ? service.getStatus() + " (refreshing...)" : service.getStatus());
        rebuild();
    }

    private void rebuild() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::rebuild);
            return;
        }
        root.removeAllChildren();
        LuaReferenceData data = service.getData();
        if (data != null) {
            String needle = filter.getText() == null ? "" : filter.getText().trim().toLowerCase(Locale.ROOT);
            addClasses(data, needle);
            addEbus(data, needle);
            addGlobals(data, needle);
        }
        model.reload();
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void addClasses(LuaReferenceData data, String needle) {
        DefaultMutableTreeNode classes = new DefaultMutableTreeNode("Classes (" + data.classes().size() + ")");
        for (LuaReferenceData.ScriptClass scriptClass : data.classes()) {
            if (!matches(needle, scriptClass.name())) {
                continue;
            }
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(scriptClass.name());
            for (LuaReferenceData.Method method : scriptClass.methods()) {
                node.add(new DefaultMutableTreeNode(method.name() + "(" + method.paramInfo() + ")"));
            }
            for (LuaReferenceData.Property property : scriptClass.properties()) {
                node.add(new DefaultMutableTreeNode(property.name() + " (property)"));
            }
            classes.add(node);
        }
        if (classes.getChildCount() > 0) {
            root.add(classes);
        }
    }

    private void addEbus(LuaReferenceData data, String needle) {
        DefaultMutableTreeNode ebuses = new DefaultMutableTreeNode("EBuses (" + data.ebuses().size() + ")");
        for (LuaReferenceData.Ebus ebus : data.ebuses()) {
            if (!matches(needle, ebus.name())) {
                continue;
            }
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(ebus.name());
            for (LuaReferenceData.Method event : ebus.events()) {
                node.add(new DefaultMutableTreeNode(event.name() + "(" + event.paramInfo() + ")"));
            }
            ebuses.add(node);
        }
        if (ebuses.getChildCount() > 0) {
            root.add(ebuses);
        }
    }

    private void addGlobals(LuaReferenceData data, String needle) {
        DefaultMutableTreeNode globals = new DefaultMutableTreeNode("Globals");
        for (LuaReferenceData.Method method : data.globalMethods()) {
            if (matches(needle, method.name())) {
                globals.add(new DefaultMutableTreeNode(method.name() + "(" + method.paramInfo() + ")"));
            }
        }
        for (LuaReferenceData.Property property : data.globalProperties()) {
            if (matches(needle, property.name())) {
                globals.add(new DefaultMutableTreeNode(property.name() + " (property)"));
            }
        }
        if (globals.getChildCount() > 0) {
            root.add(globals);
        }
    }

    private static boolean matches(String needle, String value) {
        return needle.isEmpty() || value.toLowerCase(Locale.ROOT).contains(needle);
    }

    @Override
    public void dispose() {
        service.removeListener(this);
    }
}
