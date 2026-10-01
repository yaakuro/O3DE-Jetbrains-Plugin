/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.core;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.o3de.clion.settings.O3deSettings;
import com.o3de.clion.toolwindow.O3deToolWindowFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Project level service that owns the O3DE console and executes {@code o3de} commands.
 *
 * <p>Commands are run with {@link ProcessBuilder} instead of the platform execution API so that
 * the plugin stays independent of the command line configuration model, which changes far more
 * often than the rest of the platform.</p>
 */
@Service(Service.Level.PROJECT)
public final class O3deService implements Disposable {

    /** Callback invoked on the EDT once a command finished. */
    public interface Completion {
        void finished(int exitCode, @Nullable String output);
    }

    private static final Logger LOG = Logger.getInstance(O3deService.class);

    private final Project project;
    private final ConsoleView console;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Process process;

    public O3deService(Project project) {
        this.project = project;
        this.console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        Disposer.register(this, console);
    }

    public static @NotNull O3deService getInstance(@NotNull Project project) {
        return project.getService(O3deService.class);
    }

    public @NotNull ConsoleView getConsole() {
        return console;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Terminates the currently running command, if any. */
    public void stop() {
        Process current = process;
        if (current != null) {
            current.destroy();
            println("\n[o3de] terminated by user\n", ConsoleViewContentType.ERROR_OUTPUT);
        }
    }

    public void clearConsole() {
        console.clear();
    }

    public void println(@NotNull String text, @NotNull ConsoleViewContentType type) {
        console.print(text, type);
    }

    /**
     * Runs a command and streams its output into the O3DE console.
     *
     * @param title               short human readable description used for notifications
     * @param command             full argument vector, element zero is the script
     * @param workDir             working directory, {@code null} inherits the IDE working dir
     * @param requiresConfirmation ask the user before starting (used for write operations)
     * @param collector           receives the complete output when the process ends
     * @param completion          invoked on the EDT when the process ended
     */
    public void run(@NotNull String title,
                    @NotNull List<String> command,
                    @Nullable Path workDir,
                    boolean requiresConfirmation,
                    @Nullable Consumer<String> collector,
                    @Nullable Completion completion) {
        if (command.isEmpty()) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            notifyUser(title, "Another O3DE command is still running.", NotificationType.WARNING);
            return;
        }
        if (requiresConfirmation && !confirm(title, command)) {
            running.set(false);
            return;
        }

        String rendered = O3deCommandLine.render(command);
        if (O3deSettings.getInstance().isShowToolWindowOnRun()) {
            activateToolWindow();
        }
        println("\n$ " + rendered + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);

        long startedAt = System.currentTimeMillis();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            int exitCode = -1;
            String failure = null;
            StringBuilder collected = new StringBuilder();
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                if (workDir != null) {
                    builder.directory(workDir.toFile());
                }
                Charset charset = StandardCharsets.UTF_8;
                Process started = builder.start();
                process = started;
                Thread outReader = pump(started, false, collected);
                Thread errReader = pump(started, true, collected);
                exitCode = started.waitFor();
                outReader.join(5_000);
                errReader.join(5_000);
            } catch (IOException e) {
                failure = e.getMessage();
                LOG.info("Failed to start " + rendered, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                failure = "interrupted";
            } finally {
                process = null;
            }

            String output = collected.toString();
            if (collector != null) {
                final String captured = output;
                ApplicationManager.getApplication().invokeLater(() -> collector.accept(captured));
            }
            final int finalExit = exitCode;
            final String finalFailure = failure;
            ApplicationManager.getApplication().invokeLater(() -> {
                running.set(false);
                long elapsed = System.currentTimeMillis() - startedAt;
                if (finalFailure != null) {
                    println("[o3de] " + finalFailure + "\n", ConsoleViewContentType.ERROR_OUTPUT);
                    notifyUser(title, finalFailure, NotificationType.ERROR);
                } else if (finalExit == 0) {
                    println(String.format("[o3de] finished in %d ms (exit code 0)%n", elapsed),
                            ConsoleViewContentType.NORMAL_OUTPUT);
                    notifyUser(title, "Finished successfully.", NotificationType.INFORMATION);
                    if (O3deSettings.getInstance().isRefreshAfterSuccess()) {
                        VirtualFileManager.getInstance().asyncRefresh(null);
                    }
                } else {
                    println(String.format("[o3de] failed with exit code %d%n", finalExit),
                            ConsoleViewContentType.ERROR_OUTPUT);
                    notifyUser(title, "Failed with exit code " + finalExit + ".",
                            NotificationType.ERROR);
                }
                if (completion != null) {
                    completion.finished(finalExit, output);
                }
            });
        });
    }

    private boolean confirm(@NotNull String title, @NotNull List<String> command) {
        if (!O3deSettings.getInstance().isConfirmWriteCommands()) {
            return true;
        }
        String rendered = O3deCommandLine.render(command);
        Boolean[] result = new Boolean[]{Boolean.FALSE};
        Runnable ask = () -> result[0] = Messages.showYesNoDialog(
                project,
                "Run the following command?\n\n" + rendered,
                title + " - O3DE",
                Messages.getQuestionIcon()) == Messages.YES;
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ask.run();
        } else {
            ApplicationManager.getApplication().invokeAndWait(ask);
        }
        return Boolean.TRUE.equals(result[0]);
    }

    private @NotNull Thread pump(@NotNull Process process, boolean stderr, @NotNull StringBuilder sink) {
        Charset charset = StandardCharsets.UTF_8;
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stderr ? process.getErrorStream() : process.getInputStream(), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String text = line + "\n";
                    synchronized (sink) {
                        sink.append(text);
                    }
                    println(text, stderr ? ConsoleViewContentType.ERROR_OUTPUT
                            : ConsoleViewContentType.NORMAL_OUTPUT);
                }
            } catch (IOException e) {
                // Stream closed while the process was being destroyed.
            }
        }, stderr ? "o3de-stderr" : "o3de-stdout");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public void activateToolWindow() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(
                O3deToolWindowFactory.ID);
        if (toolWindow != null) {
            toolWindow.activate(null);
        }
    }

    private void notifyUser(@NotNull String title, @NotNull String message,
                            @NotNull NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("O3DE")
                .createNotification(title, message, type)
                .notify(project);
    }

    @Override
    public void dispose() {
        stop();
    }
}
