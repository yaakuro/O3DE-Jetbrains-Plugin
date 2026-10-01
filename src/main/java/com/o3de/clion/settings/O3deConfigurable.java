/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.o3de.clion.O3deIcons;
import com.o3de.clion.core.O3deCliLocator;
import com.o3de.clion.ui.PathField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Settings page reachable through {@code Settings | Tools | O3DE}.
 */
public final class O3deConfigurable implements Configurable {

    private final O3deSettings settings = O3deSettings.getInstance();

    private JPanel panel;
    private PathField scriptField;
    private PathField engineField;
    private JTextField extraArgsField;
    private JBCheckBox showToolWindow;
    private JBCheckBox refreshAfterSuccess;
    private JBCheckBox confirmWriteCommands;
    private JBCheckBox luaReferencesEnabled;
    private JBCheckBox luaCompletionEnabled;
    private JBLabel detectedLabel;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "O3DE";
    }

    @Override
    public @Nullable JComponent createComponent() {
        scriptField = new PathField(null, PathField.Mode.FILE, "Select the o3de script");
        engineField = new PathField(null, PathField.Mode.DIRECTORY, "Select the O3DE engine root");
        extraArgsField = new JTextField();
        showToolWindow = new JBCheckBox("Show the O3DE tool window when a command starts");
        refreshAfterSuccess = new JBCheckBox("Refresh the project after a command succeeds");
        confirmWriteCommands = new JBCheckBox("Ask before running commands that change files");
        luaReferencesEnabled = new JBCheckBox(
                "Read Lua API references (Classes, EBuses, Globals) from a running O3DE target");
        luaCompletionEnabled = new JBCheckBox("Offer Lua code completion from those references");
        detectedLabel = new JBLabel(" ");
        detectedLabel.setIcon(O3deIcons.ToolWindow);

        JButton detect = new JButton("Detect");
        detect.setToolTipText("Search the registered engines for the o3de script");
        detect.addActionListener(e -> detect());
        JPanel detectRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        detectRow.setOpaque(false);
        detectRow.add(detect);

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("o3de script:", scriptField)
                .addLabeledComponent("Engine root:", engineField)
                .addLabeledComponent("Extra arguments:", extraArgsField)
                .addComponent(detectRow)
                .addComponent(detectedLabel)
                .addComponent(showToolWindow)
                .addComponent(refreshAfterSuccess)
                .addComponent(confirmWriteCommands)
                .addComponent(luaReferencesEnabled)
                .addComponent(luaCompletionEnabled)
                .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));

        reset();
        return panel;
    }

    private void detect() {
        Path script = O3deCliLocator.locate();
        if (script == null) {
            Messages.showInfoMessage(
                    "No o3de script found. Register an engine first:\n"
                            + "  o3de register --this-engine",
                    "O3DE");
            return;
        }
        scriptField.setText(script.toString());
        Path engine = O3deCliLocator.engineRootFor(script);
        if (engine != null) {
            engineField.setText(engine.toString());
        }
        detectedLabel.setText("Using " + script);
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(scriptField.getText(), nullToEmpty(settings.getConfiguredScriptPath()))
                || !Objects.equals(engineField.getText(), nullToEmpty(settings.getConfiguredEnginePath()))
                || !Objects.equals(extraArgsField.getText().trim(), settings.getExtraArgs())
                || showToolWindow.isSelected() != settings.isShowToolWindowOnRun()
                || refreshAfterSuccess.isSelected() != settings.isRefreshAfterSuccess()
                || confirmWriteCommands.isSelected() != settings.isConfirmWriteCommands()
                || luaReferencesEnabled.isSelected() != settings.isLuaReferencesEnabled()
                || luaCompletionEnabled.isSelected() != settings.isLuaCompletionEnabled();
    }

    @Override
    public void apply() {
        settings.setScriptPath(scriptField.getText());
        settings.setEnginePath(engineField.getText());
        settings.setExtraArgs(extraArgsField.getText());
        settings.setShowToolWindowOnRun(showToolWindow.isSelected());
        settings.setRefreshAfterSuccess(refreshAfterSuccess.isSelected());
        settings.setConfirmWriteCommands(confirmWriteCommands.isSelected());
        settings.setLuaReferencesEnabled(luaReferencesEnabled.isSelected());
        settings.setLuaCompletionEnabled(luaCompletionEnabled.isSelected());
        detectedLabel.setText("Using " + O3deCliLocator.describe());
    }

    @Override
    public void reset() {
        scriptField.setText(nullToEmpty(settings.getConfiguredScriptPath()));
        engineField.setText(nullToEmpty(settings.getConfiguredEnginePath()));
        extraArgsField.setText(settings.getExtraArgs());
        showToolWindow.setSelected(settings.isShowToolWindowOnRun());
        refreshAfterSuccess.setSelected(settings.isRefreshAfterSuccess());
        confirmWriteCommands.setSelected(settings.isConfirmWriteCommands());
        luaReferencesEnabled.setSelected(settings.isLuaReferencesEnabled());
        luaCompletionEnabled.setSelected(settings.isLuaCompletionEnabled());
        detectedLabel.setText("Using " + O3deCliLocator.describe());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        scriptField = null;
        engineField = null;
        extraArgsField = null;
        showToolWindow = null;
        refreshAfterSuccess = null;
        confirmWriteCommands = null;
        luaReferencesEnabled = null;
        luaCompletionEnabled = null;
        detectedLabel = null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
