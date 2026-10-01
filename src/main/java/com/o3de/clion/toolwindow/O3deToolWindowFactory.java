/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.content.ContentFactory;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.o3de.clion.O3deIcons;
import com.o3de.clion.core.O3deService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Creates the "O3DE" tool window: an output console for everything the {@code o3de} script prints
 * plus a registry tab listing the registered engines, projects, gems and templates.
 */
public final class O3deToolWindowFactory implements ToolWindowFactory, DumbAware {

    public static final String ID = "O3DE";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        O3deService service = O3deService.getInstance(project);
        ContentFactory contents = ContentFactory.getInstance();

        Content console = contents.createContent(service.getConsole().getComponent(), "Console", false);
        console.setCloseable(false);
        toolWindow.getContentManager().addContent(console);

        O3deRegistryPanel registryPanel = new O3deRegistryPanel(project);
        Content registry = contents.createContent(registryPanel, "Registry", false);
        registry.setCloseable(false);
        toolWindow.getContentManager().addContent(registry);

        com.o3de.clion.lua.LuaReferencePanel luaPanel = new com.o3de.clion.lua.LuaReferencePanel(project);
        Content lua = contents.createContent(luaPanel, "Lua API", false);
        lua.setCloseable(false);
        lua.setDisposer(luaPanel);
        toolWindow.getContentManager().addContent(lua);

        O3deLogPanel logPanel = new O3deLogPanel(project);
        Content log = contents.createContent(logPanel, "Editor Log", false);
        log.setCloseable(false);
        log.setDisposer(logPanel);
        toolWindow.getContentManager().addContent(log);

        toolWindow.setTitleActions(List.of(
                new ClearAction(project),
                new StopAction(project),
                new SettingsAction(project)));
        toolWindow.setAdditionalGearActions(null);
    }

    private abstract static class BaseAction extends AnAction {
        private final Project project;

        BaseAction(@NotNull Project project, @NotNull String text, @NotNull String description,
                   @NotNull javax.swing.Icon icon) {
            super(text, description, icon);
            this.project = project;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @NotNull Project project() {
            return project;
        }
    }

    private static final class ClearAction extends BaseAction {
        ClearAction(@NotNull Project project) {
            super(project, "Clear", "Clear the O3DE console", AllIcons.General.Delete);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            O3deService.getInstance(project()).clearConsole();
        }
    }

    private static final class StopAction extends BaseAction {
        StopAction(@NotNull Project project) {
            super(project, "Stop", "Terminate the running O3DE command", AllIcons.Actions.Suspend);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(O3deService.getInstance(project()).isRunning());
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            O3deService.getInstance(project()).stop();
        }
    }

    private static final class SettingsAction extends BaseAction {
        SettingsAction(@NotNull Project project) {
            super(project, "O3DE Settings", "Open the O3DE settings", O3deIcons.Settings);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                    .showSettingsDialog(project(), "O3DE");
        }
    }
}
