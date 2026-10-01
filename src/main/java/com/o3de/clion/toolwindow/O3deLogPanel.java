/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.o3de.clion.core.O3deLogService;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tool window tab that shows the tailed O3DE Editor log with a small control bar.
 */
public final class O3deLogPanel extends JPanel implements O3deLogService.Listener, Disposable {

    private final Project project;
    private final O3deLogService service;

    private final JBLabel status = new JBLabel(" ");
    private final JButton followButton = new JButton("Pause");

    public O3deLogPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.service = O3deLogService.getInstance(project);

        JButton clearButton = new JButton("Clear");
        clearButton.setToolTipText("Clear the log shown in this tab (the file on disk is not changed)");
        clearButton.addActionListener(e -> service.clear());

        JButton openButton = new JButton("Open Log File");
        openButton.setToolTipText("Open the Editor log file in the editor");
        openButton.addActionListener(e -> openLogFile());

        JButton reloadButton = new JButton("Reload");
        reloadButton.setToolTipText("Re-resolve the log path and start again from the beginning");
        reloadButton.addActionListener(e -> service.reload());

        followButton.setToolTipText("Pause or resume tailing");
        followButton.addActionListener(e -> {
            service.setPaused(!service.isPaused());
            updateControls();
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        controls.add(followButton);
        controls.add(clearButton);
        controls.add(openButton);
        controls.add(reloadButton);
        controls.add(status);

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(JBUI.Borders.empty(4));
        top.add(controls, BorderLayout.WEST);

        add(top, BorderLayout.NORTH);
        add(service.getConsole().getComponent(), BorderLayout.CENTER);

        service.addListener(this);
        service.start();
        updateControls();
    }

    private void openLogFile() {
        Path file = service.resolveLogFile();
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(file.toString());
        if (virtualFile != null) {
            new OpenFileDescriptor(project, virtualFile).navigate(true);
        }
    }

    @Override
    public void logStateChanged() {
        ApplicationManager.getApplication().invokeLater(this::updateControls);
    }

    private void updateControls() {
        if (service.isPaused()) {
            followButton.setText("Follow");
        } else {
            followButton.setText("Pause");
        }
        String state = service.isRunning() ? (service.isPaused() ? "paused" : "tailing") : "stopped";
        status.setText("[" + state + "] " + service.describeLogFile());
    }

    @Override
    public void dispose() {
        service.removeListener(this);
        service.stop();
    }
}
