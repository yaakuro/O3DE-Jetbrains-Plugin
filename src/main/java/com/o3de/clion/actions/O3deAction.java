/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.o3de.clion.core.O3deCliLocator;
import com.o3de.clion.core.O3deProjectContext;
import com.o3de.clion.core.O3deService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Base class for every action in the {@code Tools | O3DE} menu plus the shared helpers that turn
 * a built command into a running process.
 */
public abstract class O3deAction extends AnAction {

    protected O3deAction() {
        super();
    }

    protected O3deAction(@Nullable String text, @Nullable String description,
                         @Nullable javax.swing.Icon icon) {
        super(text, description, icon);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /** @return the directory of the file or folder selected in the project view. */
    protected static @Nullable Path selectedDirectory(@NotNull AnActionEvent event) {
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        return O3deProjectContext.directoryOf(file);
    }

    /** @return the project base directory, or {@code null} for a default project. */
    protected static @Nullable Path baseDirectory(@Nullable Project project) {
        return project == null ? null : O3deProjectContext.projectBasePath(project);
    }

    /**
     * Locates the o3de script or tells the user how to configure it.
     *
     * @return the script path, or {@code null} when none could be found.
     */
    protected static @Nullable Path scriptOrExplain(@Nullable Project project) {
        Path script = O3deCliLocator.locate();
        if (script != null) {
            return script;
        }
        if (project != null) {
            Notification notification = NotificationGroupManager.getInstance()
                    .getNotificationGroup("O3DE")
                    .createNotification(
                            "O3DE script not found",
                            "Point the plugin at your o3de script (Settings | Tools | O3DE) or "
                                    + "register an engine with 'o3de register --this-engine'.",
                            NotificationType.WARNING);
            notification.addAction(NotificationAction.createSimple("Open Settings", () ->
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, "O3DE")));
            notification.notify(project);
        }
        return null;
    }

    /** Starts a command in the O3DE tool window. */
    protected static void start(@NotNull Project project, @NotNull String title,
                                @NotNull List<String> command, boolean writesToDisk) {
        O3deService.getInstance(project).run(
                title, command, baseDirectory(project), writesToDisk, null, null);
    }

    /** Starts a command with an explicit working directory. */
    protected static void start(@NotNull Project project, @NotNull String title,
                                @NotNull List<String> command, @Nullable Path workDir,
                                boolean writesToDisk) {
        O3deService.getInstance(project).run(
                title, command, workDir, writesToDisk, null, null);
    }
}
