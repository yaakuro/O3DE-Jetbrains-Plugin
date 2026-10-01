/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.o3de.clion.O3deIcons;
import com.o3de.clion.ui.InstantiateTemplateDialog;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/** Instantiates any registered template through {@code o3de create-from-template}. */
public final class InstantiateTemplateAction extends O3deAction {

    public InstantiateTemplateAction() {
        super("Instantiate Template...", "Create files from any registered O3DE template",
                O3deIcons.Template);
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
        InstantiateTemplateDialog dialog = new InstantiateTemplateDialog(project, selectedDirectory(event));
        if (!dialog.showAndGet()) {
            return;
        }
        start(project, "Instantiate template", dialog.buildCommand(script), true);
    }
}
