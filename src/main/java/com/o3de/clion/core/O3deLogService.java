/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.core;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.o3de.clion.settings.O3deSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Project level service that tails the O3DE Editor log ({@code <project>/user/log/Editor.log})
 * and streams newly appended lines into a dedicated console.
 *
 * <p>The engine keeps the log file open ({@code KEEP_LOG_FILE_OPEN}) and never flushes explicitly,
 * so the file is polled for growth rather than watched for file events.</p>
 */
@Service(Service.Level.PROJECT)
public final class O3deLogService implements Disposable {

    public interface Listener {
        void logStateChanged();
    }

    private static final Logger LOG = Logger.getInstance(O3deLogService.class);
    private static final long POLL_INTERVAL_MS = 250;
    private static final long MAX_INITIAL_BYTES = 200_000;

    private final Project project;
    private final ConsoleView console;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final StringBuilder partialLine = new StringBuilder();

    private volatile boolean stopped = true;
    private volatile boolean paused;
    private volatile Path currentFile;
    private long position;
    private boolean skipToNewline;
    private Thread thread;

    public O3deLogService(Project project) {
        this.project = project;
        this.console = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        Disposer.register(this, console);
    }

    public static @NotNull O3deLogService getInstance(@NotNull Project project) {
        return project.getService(O3deLogService.class);
    }

    public @NotNull ConsoleView getConsole() {
        return console;
    }

    public boolean isRunning() {
        return !stopped;
    }

    public boolean isPaused() {
        return paused;
    }

    public void addListener(@NotNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NotNull Listener listener) {
        listeners.remove(listener);
    }

    /** Resolves the log file from the settings override or the open project. */
    public @Nullable Path resolveLogFile() {
        String override = O3deSettings.getInstance().getConfiguredLogFilePath();
        if (override != null) {
            return Path.of(override);
        }
        O3deProjectContext.ProjectInfo info = O3deProjectContext.findProject(project, null);
        if (info == null) {
            return null;
        }
        return info.getRoot().resolve("user").resolve("log").resolve("Editor.log");
    }

    public @NotNull String describeLogFile() {
        Path file = currentFile != null ? currentFile : resolveLogFile();
        return file == null ? "<not found - set the log path in Settings | Tools | O3DE>" : file.toString();
    }

    public synchronized void start() {
        if (!stopped) {
            return;
        }
        stopped = false;
        resetToFile(resolveLogFile(), true);
        thread = new Thread(this::pump, "o3de-log-tailer");
        thread.setDaemon(true);
        thread.start();
        fireChanged();
    }

    public synchronized void stop() {
        if (stopped) {
            return;
        }
        stopped = true;
        Thread current = thread;
        thread = null;
        if (current != null) {
            current.interrupt();
        }
        fireChanged();
    }

    /** Re-resolves the log file and starts from its beginning again. */
    public synchronized void reload() {
        resetToFile(resolveLogFile(), true);
        fireChanged();
    }

    public void setPaused(boolean value) {
        paused = value;
        fireChanged();
    }

    public void clear() {
        partialLine.setLength(0);
        ApplicationManager.getApplication().invokeLater(console::clear);
    }

    private void resetToFile(@Nullable Path file, boolean announce) {
        currentFile = file;
        partialLine.setLength(0);
        position = 0;
        skipToNewline = false;
        if (file != null && Files.isRegularFile(file)) {
            try {
                long size = Files.size(file);
                if (size > MAX_INITIAL_BYTES) {
                    position = size - MAX_INITIAL_BYTES;
                    skipToNewline = true;
                }
            } catch (IOException ignored) {
                position = 0;
            }
        }
        if (announce) {
            Path shown = file;
            ApplicationManager.getApplication().invokeLater(() ->
                    print(shown == null
                            ? "\n[o3de-log] No Editor log found. Set the path in Settings | Tools | O3DE.\n"
                            : "\n[o3de-log] Tailing " + shown + "\n",
                            ConsoleViewContentType.LOG_INFO_OUTPUT));
        }
    }

    private void pump() {
        while (!stopped) {
            try {
                Path file = resolveLogFile();
                if (file != null && Files.isRegularFile(file)) {
                    if (currentFile == null || !currentFile.equals(file)) {
                        resetToFile(file, true);
                    }
                    if (!paused) {
                        readAppended(file);
                    }
                }
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                LOG.info("Failed while tailing " + currentFile, e);
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void readAppended(Path file) throws IOException {
        long size = Files.size(file);
        if (size < position) {
            // The file was truncated or rotated.
            position = 0;
            partialLine.setLength(0);
        }
        if (size == position) {
            return;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            raf.seek(position);
            byte[] buffer = new byte[(int) Math.min(size - position, 1 << 20)];
            int read = raf.read(buffer);
            if (read <= 0) {
                return;
            }
            position += read;
            String text = new String(buffer, 0, read, StandardCharsets.UTF_8);
            if (skipToNewline) {
                int newline = text.indexOf('\n');
                if (newline < 0) {
                    return;
                }
                text = text.substring(newline + 1);
                skipToNewline = false;
            }
            appendText(text);
        }
    }

    private void appendText(String text) {
        partialLine.append(text);
        int newline;
        while ((newline = partialLine.indexOf("\n")) >= 0) {
            String line = partialLine.substring(0, newline);
            partialLine.delete(0, newline + 1);
            if (!line.isEmpty()) {
                print(line + "\n", severityOf(line));
            }
        }
    }

    private static ConsoleViewContentType severityOf(String line) {
        if (line.contains("[Error]") || line.contains("[Exception]")) {
            return ConsoleViewContentType.LOG_ERROR_OUTPUT;
        }
        if (line.contains("[Warning]")) {
            return ConsoleViewContentType.LOG_WARNING_OUTPUT;
        }
        return ConsoleViewContentType.NORMAL_OUTPUT;
    }

    private void print(String text, ConsoleViewContentType type) {
        ApplicationManager.getApplication().invokeLater(() -> console.print(text, type));
    }

    private void fireChanged() {
        for (Listener listener : listeners) {
            listener.logStateChanged();
        }
    }

    @Override
    public void dispose() {
        stop();
        listeners.clear();
    }
}
