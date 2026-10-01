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
import com.o3de.clion.ui.CreateGemDialog;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/** Opens the create gem dialog and runs {@code o3de create-gem}. */
public final class CreateGemAction extends O3deAction {

    public CreateGemAction() {
        super("Create Gem...", "Create a new gem with the o3de script", O3deIcons.Gem);
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
        CreateGemDialog dialog = new CreateGemDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }
        List<String> command = O3deCommandLine.createGem(
                script,
                dialog.getGemPath().toString(),
                dialog.getGemName(),
                dialog.getTemplateName(),
                dialog.getDisplayName(),
                dialog.getSummary(),
                dialog.isNoRegister(),
                java.util.Collections.emptyMap());
        start(project, "Create gem", command, true);
    }
}
