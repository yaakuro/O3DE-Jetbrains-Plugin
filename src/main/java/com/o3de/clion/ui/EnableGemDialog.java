/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import com.o3de.clion.core.O3deProjectContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.nio.file.Path;

/**
 * Collects the parameters for {@code o3de enable-gem}.
 */
public final class EnableGemDialog extends O3deDialog {

    private final PathField projectField;
    private final PathField gemField;
    private final JBCheckBox force = new JBCheckBox("Skip the version compatibility check");

    public EnableGemDialog(@Nullable Project project, @Nullable Path startDirectory) {
        super(project, "Enable Gem in Project");
        setOKButtonText("Enable Gem");

        projectField = new PathField(project, PathField.Mode.DIRECTORY, "Choose the O3DE project");
        gemField = new PathField(project, PathField.Mode.DIRECTORY, "Choose the gem folder");

        O3deProjectContext.ProjectInfo o3deProject =
                O3deProjectContext.findProject(project, startDirectory);
        if (o3deProject != null) {
            projectField.setText(o3deProject.getRoot().toString());
        } else if (project != null && project.getBasePath() != null) {
            projectField.setText(project.getBasePath());
        }
        O3deProjectContext.Gem gem = O3deProjectContext.findGem(project, startDirectory);
        if (gem != null) {
            gemField.setText(gem.getRoot().toString());
        }
        init();
    }

    @Override
    protected @NotNull JComponent createBody() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Project:", projectField)
                .addLabeledComponent("Gem:", gemField)
                .addComponent(force)
                .getPanel();
    }

    @Override
    protected @Nullable ValidationInfo validateFields() {
        ValidationInfo error = requireText(projectField.getText(), "A project folder is required",
                projectField.getTextField());
        if (error != null) {
            return error;
        }
        return requireText(gemField.getText(), "A gem folder is required", gemField.getTextField());
    }

    public @NotNull Path getProjectPath() {
        return Path.of(projectField.getText());
    }

    public @NotNull Path getGemPath() {
        return Path.of(gemField.getText());
    }

    public boolean isForce() {
        return force.isSelected();
    }
}
