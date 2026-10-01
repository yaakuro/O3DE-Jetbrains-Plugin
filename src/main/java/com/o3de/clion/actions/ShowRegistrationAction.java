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
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/** Runs {@code o3de register-show} and prints the manifest to the O3DE console. */
public final class ShowRegistrationAction extends O3deAction {

    public ShowRegistrationAction() {
        super("Show Registration", "Run o3de register-show", O3deIcons.Register);
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
        start(project, "Show registration", O3deCommandLine.registerShow(script), false);
    }
}
