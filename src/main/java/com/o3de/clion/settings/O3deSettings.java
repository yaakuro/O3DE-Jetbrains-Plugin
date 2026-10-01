/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Application level settings for the O3DE tooling integration.
 *
 * <p>The state is persisted as XML in the IDE config directory ({@code o3de.xml}).</p>
 */
@Service(Service.Level.APP)
@State(name = "O3deSettings", storages = @Storage("o3de.xml"))
public final class O3deSettings implements PersistentStateComponent<O3deSettings.State> {

    public static final class State {
        /** Absolute path to {@code o3de.sh} (or {@code o3de.bat}). Empty means auto-detect. */
        public String scriptPath = "";
        /** Absolute path to an O3DE engine root. Empty means auto-detect. */
        public String enginePath = "";
        /** Extra arguments prepended to every invocation, e.g. {@code -v}. */
        public String extraArgs = "";
        /** Show the O3DE tool window whenever a command is started. */
        public boolean showToolWindowOnRun = true;
        /** Refresh the project file system after a command finished successfully. */
        public boolean refreshAfterSuccess = true;
        /** Ask for confirmation before running a command that writes to disk. */
        public boolean confirmWriteCommands = true;
        /** Listen for O3DE RemoteTools targets and expose their Lua API references. */
        public boolean luaReferencesEnabled = true;
        /** Offer Lua code completion based on the references read from a connected target. */
        public boolean luaCompletionEnabled = true;
        /** Absolute path to the O3DE Editor log file. Empty means auto-detect from the open project. */
        public String logFilePath = "";
    }

    private State state = new State();

    public static O3deSettings getInstance() {
        return ApplicationManager.getApplication().getService(O3deSettings.class);
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State newState) {
        state = newState;
    }

    public @Nullable String getConfiguredScriptPath() {
        return emptyToNull(state.scriptPath);
    }

    public @Nullable String getConfiguredEnginePath() {
        return emptyToNull(state.enginePath);
    }

    public @NotNull String getExtraArgs() {
        return state.extraArgs == null ? "" : state.extraArgs.trim();
    }

    public boolean isShowToolWindowOnRun() {
        return state.showToolWindowOnRun;
    }

    public boolean isRefreshAfterSuccess() {
        return state.refreshAfterSuccess;
    }

    public boolean isConfirmWriteCommands() {
        return state.confirmWriteCommands;
    }

    public boolean isLuaReferencesEnabled() {
        return state.luaReferencesEnabled;
    }

    public boolean isLuaCompletionEnabled() {
        return state.luaCompletionEnabled;
    }

    public @Nullable String getConfiguredLogFilePath() {
        return emptyToNull(state.logFilePath);
    }

    public void setScriptPath(@Nullable String path) {
        state.scriptPath = nullToEmpty(path);
    }

    public void setEnginePath(@Nullable String path) {
        state.enginePath = nullToEmpty(path);
    }

    public void setExtraArgs(@Nullable String args) {
        state.extraArgs = nullToEmpty(args);
    }

    public void setShowToolWindowOnRun(boolean value) {
        state.showToolWindowOnRun = value;
    }

    public void setRefreshAfterSuccess(boolean value) {
        state.refreshAfterSuccess = value;
    }

    public void setConfirmWriteCommands(boolean value) {
        state.confirmWriteCommands = value;
    }

    public void setLuaReferencesEnabled(boolean value) {
        state.luaReferencesEnabled = value;
    }

    public void setLuaCompletionEnabled(boolean value) {
        state.luaCompletionEnabled = value;
    }

    public void setLogFilePath(@Nullable String path) {
        state.logFilePath = nullToEmpty(path);
    }

    private static @Nullable String emptyToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static @NotNull String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
