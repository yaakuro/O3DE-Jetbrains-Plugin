/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.util.ui.FormBuilder;
import com.o3de.clion.core.O3deCommandLine;
import com.o3de.clion.core.O3deManifest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects the parameters for the generic {@code o3de create-from-template} command, which can
 * instantiate any registered template.
 */
public final class InstantiateTemplateDialog extends O3deDialog {

    private final JComboBox<O3deManifest.Template> templateCombo = new JComboBox<>();
    private final JTextField nameField = new JTextField();
    private final JTextArea replacementsArea = new JTextArea(5, 40);
    private final PathField pathField;

    public InstantiateTemplateDialog(@Nullable Project project, @Nullable Path startDirectory) {
        super(project, "Instantiate O3DE Template");
        setOKButtonText("Create");

        pathField = new PathField(project, PathField.Mode.DIRECTORY,
                "Choose the destination folder");
        if (startDirectory != null) {
            pathField.setText(startDirectory.toString());
        } else if (project != null && project.getBasePath() != null) {
            pathField.setText(project.getBasePath());
        }

        fillTemplates(templateCombo, null);
        replacementsArea.setToolTipText("Optional ${token} = value pairs, one per line");
        init();
    }

    @Override
    protected @NotNull JComponent createBody() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Template:", templateCombo)
                .addLabeledComponent("Name:", nameField)
                .addLabeledComponent("Destination:", pathField)
                .addLabeledComponent("Replacements:", scrollable(replacementsArea, 90))
                .addComponent(hint("Replacements are passed to o3de as -r pairs."))
                .getPanel();
    }

    @Override
    protected @Nullable ValidationInfo validateFields() {
        String template = selectedTemplate(templateCombo);
        if (template.isEmpty()) {
            return new ValidationInfo("A template is required", templateCombo);
        }
        ValidationInfo pathError = requireText(pathField.getText(), "A destination path is required",
                pathField.getTextField());
        if (pathError != null) {
            return pathError;
        }
        try {
            parseReplacements();
        } catch (IllegalArgumentException e) {
            return new ValidationInfo(e.getMessage(), replacementsArea);
        }
        return null;
    }

    /** Parses the "token = value" lines of the replacement area. */
    public @NotNull Map<String, String> parseReplacements() {
        Map<String, String> replacements = new LinkedHashMap<>();
        for (String rawLine : replacementsArea.getText().split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("Expected 'token = value' but got: " + line);
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Empty replacement token: " + line);
            }
            replacements.put(key, value);
        }
        return replacements;
    }

    public @NotNull String getTemplateName() {
        return selectedTemplate(templateCombo);
    }

    public @NotNull Path getDestination() {
        return Path.of(pathField.getText());
    }

    public @NotNull String getDestinationName() {
        return nameField.getText().trim();
    }

    /** The full argument list, ready to be executed. */
    public @NotNull java.util.List<String> buildCommand(@NotNull Path script) {
        return O3deCommandLine.createFromTemplate(
                script, pathField.getText(), getTemplateName(), getDestinationName(), parseReplacements());
    }
}
