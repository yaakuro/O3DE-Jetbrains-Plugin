/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.o3de.clion.core.O3deCliLocator;
import com.o3de.clion.core.O3deCommandLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Free form escape hatch: runs any {@code o3de} sub command with arguments typed by the user.
 */
public final class RunCommandDialog extends O3deDialog {

    /** Every sub command the {@code o3de} script currently knows about. */
    private static final String[] KNOWN_SUB_COMMANDS = {
            "create-gem", "create-project", "create-template", "create-from-template",
            "create-repo", "register", "register-show", "get-registered",
            "enable-gem", "disable-gem", "edit-gem-properties", "edit-project-properties",
            "edit-engine-properties", "edit-repo-properties", "get-global-project",
            "set-global-project", "download", "repo", "sha256", "export-project",
            "export-project-configure", "android-configure", "android-generate",
    };

    private final JComboBox<String> subCommandCombo = new JComboBox<>(new DefaultComboBoxModel<>(KNOWN_SUB_COMMANDS));
    private final JTextField argumentsField = new JTextField();
    private final PathField workDirField;
    private final JBLabel preview = new JBLabel(" ");

    public RunCommandDialog(@Nullable Project project) {
        super(project, "Run O3DE Command");
        setOKButtonText("Run");

        subCommandCombo.setEditable(true);
        workDirField = new PathField(project, PathField.Mode.DIRECTORY, "Choose the working directory");
        if (project != null && project.getBasePath() != null) {
            workDirField.setText(project.getBasePath());
        }

        subCommandCombo.addActionListener(e -> updatePreview());
        updateOnType(argumentsField, this::updatePreview);
        updateOnType(workDirField.getTextField(), this::updatePreview);
        updatePreview();
        init();
    }

    private void updatePreview() {
        preview.setText("<html><pre>" + escape(O3deCommandLine.render(buildCommand())) + "</pre></html>");
    }

    private static @NotNull String escape(@NotNull String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    protected @NotNull JComponent createBody() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Sub command:", subCommandCombo)
                .addLabeledComponent("Arguments:", argumentsField)
                .addLabeledComponent("Working dir:", workDirField)
                .addComponent(preview)
                .addComponent(hint("Run 'o3de --help' to see the arguments of a sub command."))
                .getPanel();
    }

    @Override
    protected @Nullable ValidationInfo validateFields() {
        Object item = subCommandCombo.getSelectedItem();
        String subCommand = item == null ? "" : item.toString().trim();
        if (subCommand.isEmpty()) {
            return new ValidationInfo("A sub command is required", subCommandCombo);
        }
        if (!subCommand.matches("[A-Za-z0-9\\-_]+")) {
            return new ValidationInfo("Invalid sub command", subCommandCombo);
        }
        Path script = O3deCliLocator.locate();
        if (script == null) {
            return new ValidationInfo("No o3de script configured", subCommandCombo);
        }
        return null;
    }

    public @NotNull String getSubCommand() {
        Object item = subCommandCombo.getSelectedItem();
        return item == null ? "" : item.toString().trim();
    }

    public @NotNull String getArguments() {
        return argumentsField.getText();
    }

    public @Nullable Path getWorkingDirectory() {
        String text = workDirField.getText();
        return text.isEmpty() ? null : Path.of(text);
    }

    public @NotNull List<String> buildCommand() {
        Path script = O3deCliLocator.locate();
        if (script == null) {
            return new ArrayList<>();
        }
        return O3deCommandLine.custom(script, getSubCommand(), getArguments());
    }
}
