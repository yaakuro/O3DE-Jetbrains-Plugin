/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.o3de.clion.core.O3deManifest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.nio.file.Path;

/**
 * Collects the parameters for {@code o3de create-project}.
 */
public final class CreateProjectDialog extends O3deDialog {

    private final JTextField nameField = new JTextField();
    private final JTextField projectIdField = new JTextField();
    private final JComboBox<O3deManifest.Template> templateCombo = new JComboBox<>();
    private final JBCheckBox noRegister =
            new JBCheckBox("Do not register the project afterwards");
    private final PathField pathField;
    private String lastAutoName = "";

    public CreateProjectDialog(@Nullable Project project) {
        super(project, "Create O3DE Project");
        setOKButtonText("Create Project");
        pathField = new PathField(project, PathField.Mode.DIRECTORY,
                "Choose where the project should be created");
        fillTemplates(templateCombo, "DefaultProject");
        updateOnType(nameField, this::syncPath);
        syncPath();
        init();
    }

    private void syncPath() {
        String name = nameField.getText().trim();
        String current = pathField.getText();
        if (current.isEmpty() || (lastAutoName.length() > 0 && current.endsWith(lastAutoName))) {
            pathField.setText(defaultProjectFolder().resolve(name).toString());
        }
        lastAutoName = name;
    }

    private static @NotNull Path defaultProjectFolder() {
        String folder = O3deManifest.defaultFolder("default_projects_folder");
        if (folder != null) {
            return Path.of(folder);
        }
        return Path.of(System.getProperty("user.home", "."), "O3DE", "Projects");
    }

    @Override
    protected @NotNull JComponent createBody() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Name:", nameField)
                .addLabeledComponent("Path:", pathField)
                .addLabeledComponent("Template:", templateCombo)
                .addLabeledComponent("Project id:", projectIdField)
                .addComponent(noRegister)
                .addComponent(new JBLabel("Leave the project id empty to derive it from the name."))
                .getPanel();
    }

    @Override
    protected @Nullable ValidationInfo validateFields() {
        ValidationInfo error = requireName(nameField.getText(), "A project name is required", nameField);
        if (error != null) {
            return error;
        }
        return requireText(pathField.getText(), "A destination path is required", pathField.getTextField());
    }

    public @NotNull String getProjectName() {
        return nameField.getText().trim();
    }

    public @NotNull Path getProjectPath() {
        return Path.of(pathField.getText());
    }

    public @NotNull String getTemplateName() {
        return selectedTemplate(templateCombo);
    }

    public @Nullable String getProjectId() {
        String value = projectIdField.getText().trim();
        return value.isEmpty() ? null : value;
    }

    public boolean isNoRegister() {
        return noRegister.isSelected();
    }
}
