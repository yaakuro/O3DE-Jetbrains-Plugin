/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.toolwindow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.o3de.clion.O3deIcons;
import com.o3de.clion.core.O3deCliLocator;
import com.o3de.clion.core.O3deCommandLine;
import com.o3de.clion.core.O3deManifest;
import com.o3de.clion.core.O3deService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.MouseInputAdapter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

/**
 * Read only view over {@code ~/.o3de/o3de_manifest.json} showing everything that is currently
 * registered with O3DE. Double clicking an entry reveals it in the project view.
 */
public final class O3deRegistryPanel extends JPanel {

    private static final String[][] CATEGORIES = {
            {"engines", "Engines"},
            {"projects", "Projects"},
            {"gems", "Gems"},
            {"templates", "Templates"},
            {"external_subdirectories", "External folders"},
            {"restricted", "Restricted"},
            {"repos", "Repos"},
    };

    private final Project project;
    private final Tree tree;

    /** A single registered entry. */
    private static final class Entry {
        private final String label;
        private final String path;

        private Entry(@NotNull String label, @NotNull String path) {
            this.label = label;
            this.path = path;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public O3deRegistryPanel(@NotNull Project project) {
        super(new BorderLayout(0, JBUI.scale(4)));
        this.project = project;

        tree = new Tree(buildModel());
        tree.setShowsRootHandles(true);
        tree.setRootVisible(true);
        tree.setCellRenderer(new EntryRenderer());
        installMouseSupport(tree);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), JBUI.scale(2)));
        toolbar.setBorder(JBUI.Borders.empty(2, 4));
        JButton reload = new JButton("Reload", O3deIcons.Register);
        reload.setToolTipText("Re-read ~/.o3de/o3de_manifest.json");
        reload.addActionListener(e -> reload());
        JButton verify = new JButton("Verify with o3de", O3deIcons.Run);
        verify.setToolTipText("Run `o3de register-show` and print the result to the console");
        verify.addActionListener(e -> verifyWithCli());
        toolbar.add(reload);
        toolbar.add(verify);

        add(toolbar, BorderLayout.NORTH);
        add(new JBScrollPane(tree), BorderLayout.CENTER);
    }

    public @NotNull JTree getTree() {
        return tree;
    }

    /** Re-reads the manifest from disk. Safe to call from the EDT. */
    public void reload() {
        ((DefaultTreeModel) tree.getModel()).setRoot(buildModel());
        for (int row = 0; row < tree.getRowCount(); row++) {
            tree.expandRow(row);
        }
    }

    private void verifyWithCli() {
        Path script = O3deCliLocator.locate();
        if (script == null) {
            return;
        }
        O3deService.getInstance(project).run(
                "Show registration",
                O3deCommandLine.registerShow(script),
                null,
                false,
                null,
                (exitCode, output) -> reload());
    }

    private @NotNull DefaultMutableTreeNode buildModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("O3DE registration");
        JsonObject manifest = O3deManifest.read();
        if (manifest == null) {
            root.add(new DefaultMutableTreeNode("No manifest found at ~/.o3de/o3de_manifest.json"));
            return root;
        }
        for (String[] category : CATEGORIES) {
            JsonElement value = manifest.get(category[0]);
            if (value == null || !value.isJsonArray()) {
                continue;
            }
            JsonArray array = value.getAsJsonArray();
            DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                    category[1] + " (" + array.size() + ")");
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    String path = element.getAsString();
                    group.add(new DefaultMutableTreeNode(new Entry(labelOf(path), path)));
                }
            }
            root.add(group);
        }
        return root;
    }

    private static @NotNull String labelOf(@NotNull String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return name.isEmpty() ? path : name;
    }

    private void installMouseSupport(@NotNull JTree target) {
        target.addMouseMotionListener(new MouseInputAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                target.setToolTipText(entryPathAt(target, e));
            }
        });
        target.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                String path = entryPathAt(target, e);
                if (path != null) {
                    reveal(path);
                }
            }
        });
    }

    private static @Nullable String entryPathAt(@NotNull JTree target, @NotNull MouseEvent event) {
        TreePath treePath = target.getPathForLocation(event.getX(), event.getY());
        if (treePath == null || !(treePath.getLastPathComponent() instanceof DefaultMutableTreeNode)) {
            return null;
        }
        Object userObject = ((DefaultMutableTreeNode) treePath.getLastPathComponent()).getUserObject();
        return userObject instanceof Entry ? ((Entry) userObject).path : null;
    }

    private void reveal(@NotNull String rawPath) {
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(rawPath);
        if (file == null) {
            return;
        }
        if (file.isDirectory()) {
            ProjectView.getInstance(project).select(file, null, true);
        } else {
            new OpenFileDescriptor(project, file).navigate(true);
        }
    }

    private static final class EntryRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode
                    && ((DefaultMutableTreeNode) value).getUserObject() instanceof Entry) {
                setToolTipText(((Entry) ((DefaultMutableTreeNode) value).getUserObject()).path);
            } else {
                setToolTipText(null);
            }
            return this;
        }
    }
}
