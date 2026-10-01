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
import com.o3de.clion.ui.CreateProjectDialog;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/** Opens the create project dialog and runs {@code o3de create-project}. */
public final class CreateProjectAction extends O3deAction {

    public CreateProjectAction() {
        super("Create Project...", "Create a new project with the o3de script", O3deIcons.Project);
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
        CreateProjectDialog dialog = new CreateProjectDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }
        List<String> command = O3deCommandLine.createProject(
                script,
                dialog.getProjectPath().toString(),
                dialog.getProjectName(),
                dialog.getTemplateName(),
                dialog.getProjectId(),
                dialog.isNoRegister(),
                Collections.emptyMap());
        start(project, "Create project", command, true);
    }
}
