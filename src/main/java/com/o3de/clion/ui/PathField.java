/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * A one line path editor with a browse button. Deliberately built from plain Swing controls so
 * that it does not depend on the deprecated {@code TextFieldWithBrowseButton} listener API.
 */
public final class PathField extends JPanel {

    /** What the chooser is allowed to select. */
    public enum Mode {
        DIRECTORY,
        FILE
    }

    private final JBTextField textField = new JBTextField();
    private final Mode mode;

    public PathField(@Nullable Project project, @NotNull Mode mode, @NotNull String chooserTitle) {
        super(new BorderLayout(JBUI.scale(4), 0));
        this.mode = mode;
        setOpaque(false);

        JButton browse = new JButton(AllIcons.Actions.MenuOpen);
        browse.setToolTipText(chooserTitle);
        browse.addActionListener(e -> browse(project, chooserTitle));

        add(textField, BorderLayout.CENTER);
        add(browse, BorderLayout.EAST);
    }

    public @NotNull String getText() {
        return textField.getText().trim();
    }

    public void setText(@Nullable String value) {
        textField.setText(value == null ? "" : value);
    }

    public @NotNull JBTextField getTextField() {
        return textField;
    }

    private void browse(@Nullable Project project, @NotNull String chooserTitle) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                mode == Mode.FILE, mode == Mode.DIRECTORY, false, false, false, false);
        descriptor.setTitle(chooserTitle);
        descriptor.setDescription("Select " + (mode == Mode.DIRECTORY ? "a folder" : "a file"));

        VirtualFile toSelect = null;
        String current = getText();
        if (!current.isEmpty()) {
            toSelect = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                    .findFileByPath(new java.io.File(current).getAbsolutePath());
        }

        VirtualFile chosen = FileChooser.chooseFile(descriptor, project, toSelect);
        if (chosen != null) {
            textField.setText(chosen.getPath());
        }
    }
}
