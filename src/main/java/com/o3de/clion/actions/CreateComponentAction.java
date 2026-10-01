/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.o3de.clion.O3deIcons;
import com.o3de.clion.core.O3deCommandLine;
import com.o3de.clion.ui.CreateComponentDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Runs {@code o3de create-from-template} with one of the component templates, which is how the
 * engine itself generates component sources.
 */
public final class CreateComponentAction extends O3deAction {

    public CreateComponentAction() {
        super("Create Component...", "Create a component from an O3DE template", O3deIcons.Component);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        Path script = scriptOrExplain(project);
        if (script == null) {
            return;
        }
        CreateComponentDialog dialog = new CreateComponentDialog(project, selectedDirectory(event));
        if (!dialog.showAndGet()) {
            return;
        }
        start(project, "Create component", build(script, dialog), true);
    }

    private static @NotNull java.util.List<String> build(@NotNull Path script,
                                                         @NotNull CreateComponentDialog dialog) {
        return O3deCommandLine.createFromTemplate(
                script,
                dialog.getDestination().toString(),
                dialog.getTemplateName(),
                dialog.getComponentName(),
                dialog.getReplacements());
    }
}
