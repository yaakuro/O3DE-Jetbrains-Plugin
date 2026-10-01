/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.o3de.clion.O3deIcons;
import org.jetbrains.annotations.NotNull;

/** Opens the O3DE settings page. */
public final class OpenSettingsAction extends O3deAction {

    public OpenSettingsAction() {
        super("O3DE Settings...", "Configure the o3de script and related options", O3deIcons.Settings);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabled(event.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "O3DE");
    }
}
