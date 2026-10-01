/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.lua;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.o3de.clion.remote.script.LuaReferenceData;
import com.o3de.clion.remote.script.ScriptDebugClient;
import com.o3de.clion.settings.O3deSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Project level service that owns the RemoteTools connection to a running O3DE target and caches
 * the Lua API references read from it.
 */
@Service(Service.Level.PROJECT)
public final class LuaReferenceService implements Disposable {

    public interface Listener {
        void referenceDataChanged();
    }

    private final Project project;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private ScriptDebugClient client;
    private volatile LuaReferenceData data;
    private volatile String status = "Not connected";
    private volatile boolean refreshing;

    public LuaReferenceService(@NotNull Project project) {
        this.project = project;
    }

    public static LuaReferenceService getInstance(@NotNull Project project) {
        return project.getService(LuaReferenceService.class);
    }

    public @Nullable LuaReferenceData getData() {
        return data;
    }

    public @NotNull String getStatus() {
        return status;
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    public boolean isEnabled() {
        return O3deSettings.getInstance().isLuaReferencesEnabled();
    }

    public void addListener(@NotNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NotNull Listener listener) {
        listeners.remove(listener);
    }

    /** Starts listening (if needed) and reads the reference data in the background. */
    public synchronized void refresh() {
        if (refreshing) {
            return;
        }
        if (!isEnabled()) {
            setStatus("Lua references are disabled in Settings | Tools | O3DE");
            return;
        }
        refreshing = true;
        setStatus("Starting RemoteTools listener...");
        ApplicationManager.getApplication().executeOnPooledThread(this::doRefresh);
    }

    private void doRefresh() {
        try {
            if (client == null) {
                client = new ScriptDebugClient(new ScriptDebugClient.Listener() {
                    @Override
                    public void onStatus(String value) {
                        setStatus(value);
                    }

                    @Override
                    public void onData(LuaReferenceData value) {
                        data = value;
                        fireChanged();
                    }

                    @Override
                    public void onError(String message) {
                        setStatus("Error: " + message);
                    }
                });
            }
            client.refresh();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            setStatus("Refresh interrupted");
        } catch (Exception e) {
            setStatus("Error: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        } finally {
            refreshing = false;
            fireChanged();
        }
    }

    private void setStatus(String value) {
        status = value;
        fireChanged();
    }

    private void fireChanged() {
        for (Listener listener : listeners) {
            listener.referenceDataChanged();
        }
    }

    @Override
    public void dispose() {
        if (client != null) {
            client.close();
            client = null;
        }
        listeners.clear();
    }
}
