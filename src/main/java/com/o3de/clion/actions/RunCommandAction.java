/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.o3de.clion.O3deIcons;
import com.o3de.clion.ui.RunCommandDialog;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/** Free form runner for any {@code o3de} sub command. */
public final class RunCommandAction extends O3deAction {

    public RunCommandAction() {
        super("Run O3DE Command...", "Run an arbitrary o3de sub command", O3deIcons.Run);
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
        if (scriptOrExplain(project) == null) {
            return;
        }
        RunCommandDialog dialog = new RunCommandDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }
        start(project, "Run " + dialog.getSubCommand(), dialog.buildCommand(),
                dialog.getWorkingDirectory(), true);
    }
}
