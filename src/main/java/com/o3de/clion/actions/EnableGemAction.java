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
import com.o3de.clion.ui.EnableGemDialog;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/** Adds a gem to a project through {@code o3de enable-gem}. */
public final class EnableGemAction extends O3deAction {

    public EnableGemAction() {
        super("Enable Gem in Project...", "Add an existing gem to a project", O3deIcons.Gem);
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
        EnableGemDialog dialog = new EnableGemDialog(project, selectedDirectory(event));
        if (!dialog.showAndGet()) {
            return;
        }
        start(project, "Enable gem",
                O3deCommandLine.enableGem(script, dialog.getProjectPath().toString(),
                        dialog.getGemPath().toString(), dialog.isForce()),
                true);
    }
}
