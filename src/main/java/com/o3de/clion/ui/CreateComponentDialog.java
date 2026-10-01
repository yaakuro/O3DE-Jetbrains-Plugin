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
import com.o3de.clion.core.O3deManifest;
import com.o3de.clion.core.O3deProjectContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Collects the parameters for {@code o3de create-from-template} using one of the component
 * templates shipped with the engine.
 */
public final class CreateComponentDialog extends O3deDialog {

    private final JTextField nameField = new JTextField();
    private final JTextField gemNameField = new JTextField();
    private final JComboBox<O3deManifest.Template> templateCombo = new JComboBox<>();
    private final PathField pathField;

    public CreateComponentDialog(@Nullable Project project, @Nullable Path startDirectory) {
        super(project, "Create O3DE Component");
        setOKButtonText("Create Component");

        pathField = new PathField(project, PathField.Mode.DIRECTORY,
                "Choose the folder the component is created in");
        Path detected = O3deProjectContext.defaultComponentDestination(project, startDirectory);
        if (detected != null) {
            pathField.setText(detected.toString());
        }
        O3deProjectContext.Gem gem = O3deProjectContext.findGem(project, startDirectory);
        if (gem != null) {
            gemNameField.setText(gem.getName());
        }

        fillTemplates(templateCombo, "DefaultComponent");
        restrictToComponentTemplates();
        init();
    }

    private void restrictToComponentTemplates() {
        List<O3deManifest.Template> all = O3deManifest.templates();
        templateCombo.removeAllItems();
        for (O3deManifest.Template template : all) {
            if (template.getName().toLowerCase(Locale.ROOT).contains("component")) {
                templateCombo.addItem(template);
            }
        }
        if (templateCombo.getItemCount() == 0) {
            templateCombo.setSelectedItem("DefaultComponent");
        } else {
            templateCombo.setSelectedItem(findByName(all, "DefaultComponent"));
        }
    }

    private static @Nullable O3deManifest.Template findByName(@NotNull List<O3deManifest.Template> templates,
                                                              @NotNull String name) {
        for (O3deManifest.Template template : templates) {
            if (template.getName().equalsIgnoreCase(name)) {
                return template;
            }
        }
        return null;
    }

    @Override
    protected @NotNull JComponent createBody() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Name:", nameField)
                .addLabeledComponent("Destination:", pathField)
                .addLabeledComponent("Gem name:", gemNameField)
                .addLabeledComponent("Template:", templateCombo)
                .addComponent(new JBLabel("The 'Component' suffix is added by the template."))
                .addComponent(new JBLabel("The gem name replaces ${GemName} in the generated files."))
                .getPanel();
    }

    @Override
    protected @Nullable ValidationInfo validateFields() {
        ValidationInfo error = requireName(nameField.getText(), "A component name is required", nameField);
        if (error != null) {
            return error;
        }
        String template = selectedTemplate(templateCombo);
        if (template.isEmpty()) {
            return new ValidationInfo("A template is required", templateCombo);
        }
        ValidationInfo pathError = requireText(pathField.getText(), "A destination path is required",
                pathField.getTextField());
        if (pathError != null) {
            return pathError;
        }
        return null;
    }

    public @NotNull String getComponentName() {
        return nameField.getText().trim();
    }

    public @NotNull Path getDestination() {
        return Path.of(pathField.getText());
    }

    public @NotNull String getTemplateName() {
        return selectedTemplate(templateCombo);
    }

    public @Nullable String getGemName() {
        String value = gemNameField.getText().trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * @return the placeholder replacements that have no dedicated command line flag. The
     * {@code ${Name}} family is already covered by {@code --destination-name}.
     */
    public @NotNull Map<String, String> getReplacements() {
        Map<String, String> replacements = new LinkedHashMap<>();
        String gemName = getGemName();
        if (gemName != null) {
            replacements.put("${GemName}", gemName);
        }
        return replacements;
    }
}
