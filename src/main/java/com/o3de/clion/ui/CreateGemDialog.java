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
 * Collects the parameters for {@code o3de create-gem}.
 */
public final class CreateGemDialog extends O3deDialog {

    private final JTextField nameField = new JTextField();
    private final JTextField displayNameField = new JTextField();
    private final JTextField summaryField = new JTextField();
    private final JComboBox<O3deManifest.Template> templateCombo = new JComboBox<>();
    private final JBCheckBox noRegister = new JBCheckBox("Do not register the gem afterwards");
    private final PathField pathField;
    private String lastAutoName = "";

    public CreateGemDialog(@Nullable Project project) {
        super(project, "Create O3DE Gem");
        setOKButtonText("Create Gem");
        pathField = new PathField(project, PathField.Mode.DIRECTORY,
                "Choose where the gem should be created");
        fillTemplates(templateCombo, "DefaultGem");
        updateOnType(nameField, this::syncPath);
        syncPath();
        init();
    }

    private void syncPath() {
        String name = nameField.getText().trim();
        String current = pathField.getText();
        if (current.isEmpty() || (lastAutoName.length() > 0 && current.endsWith(lastAutoName))) {
            pathField.setText(defaultGemFolder().resolve(name).toString());
        }
        lastAutoName = name;
    }

    private static @NotNull Path defaultGemFolder() {
        String folder = O3deManifest.defaultFolder("default_gems_folder");
        if (folder != null) {
            return Path.of(folder);
        }
        return Path.of(System.getProperty("user.home", "."), "O3DE", "Gems");
    }

    @Override
    protected @NotNull JComponent createBody() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Name:", nameField)
                .addLabeledComponent("Path:", pathField)
                .addLabeledComponent("Template:", templateCombo)
                .addLabeledComponent("Display name:", displayNameField)
                .addLabeledComponent("Summary:", summaryField)
                .addComponent(noRegister)
                .addComponent(new JBLabel("The gem name may contain letters, digits, '_' and '-'."))
                .getPanel();
    }

    @Override
    protected @Nullable ValidationInfo validateFields() {
        ValidationInfo error = requireName(nameField.getText(), "A gem name is required", nameField);
        if (error != null) {
            return error;
        }
        return requireText(pathField.getText(), "A destination path is required", pathField.getTextField());
    }

    public @NotNull String getGemName() {
        return nameField.getText().trim();
    }

    public @NotNull Path getGemPath() {
        return Path.of(pathField.getText());
    }

    public @NotNull String getTemplateName() {
        return selectedTemplate(templateCombo);
    }

    public @Nullable String getDisplayName() {
        return emptyToNull(displayNameField.getText());
    }

    public @Nullable String getSummary() {
        return emptyToNull(summaryField.getText());
    }

    public boolean isNoRegister() {
        return noRegister.isSelected();
    }

    private static @Nullable String emptyToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
