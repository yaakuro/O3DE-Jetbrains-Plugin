/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.o3de.clion.O3deIcons;
import com.o3de.clion.core.O3deCommandLine;
import com.o3de.clion.core.O3deCommandLine.RegisterKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.nio.file.Path;

/**
 * Base for the {@code o3de register} actions. Sub classes only decide which kind of object is
 * registered; the folder chooser and the command construction are shared.
 */
public abstract class RegisterAction extends O3deAction {

    private final RegisterKind kind;

    protected RegisterAction(@NotNull RegisterKind kind, @NotNull String text, @Nullable Icon icon) {
        super(text, "Register a " + kind.getDescription() + " with O3DE", icon);
        this.kind = kind;
    }

    public @NotNull RegisterKind getKind() {
        return kind;
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
        Path target = null;
        if (kind != RegisterKind.ENGINE) {
            target = chooseTarget(project, event);
            if (target == null) {
                return;
            }
        }
        start(project,
                "Register " + kind.getDescription(),
                O3deCommandLine.register(script, kind, target == null ? null : target.toString()),
                true);
    }

    /** Opens a folder chooser pre-filled with the selection from the project view. */
    protected @Nullable Path chooseTarget(@NotNull Project project, @NotNull AnActionEvent event) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(
                false, true, false, false, false, false);
        descriptor.setTitle("Choose the " + kind.getDescription() + " to register");

        VirtualFile toSelect = null;
        Path preset = defaultTarget(project, event);
        if (preset != null) {
            toSelect = LocalFileSystem.getInstance().findFileByPath(preset.toString());
        }
        VirtualFile chosen = FileChooser.chooseFile(descriptor, project, toSelect);
        return chosen == null ? null : Path.of(chosen.getPath());
    }

    protected static @Nullable Path defaultTarget(@NotNull Project project,
                                                  @NotNull AnActionEvent event) {
        Path selected = selectedDirectory(event);
        if (selected != null) {
            return selected;
        }
        return baseDirectory(project);
    }
}
