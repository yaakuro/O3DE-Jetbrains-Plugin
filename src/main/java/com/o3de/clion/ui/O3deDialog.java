/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.o3de.clion.core.O3deManifest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.List;

/**
 * Shared plumbing for the dialogs that collect parameters for an {@code o3de} invocation.
 */
public abstract class O3deDialog extends DialogWrapper {

    /** Matches what the O3DE templates accept as a {@code ${Name}} replacement. */
    protected static final String NAME_PATTERN = "[A-Za-z0-9_\\-]+";

    protected final Project project;

    protected O3deDialog(@Nullable Project project, @NotNull String title) {
        super(project);
        this.project = project;
        setTitle(title);
        setOKButtonText("Run");
        // NOTE: init() is intentionally not called here. DialogWrapper builds the center panel
        // during init(), and the concrete dialog fields do not exist yet at this point.
        // Every subclass has to call init() as the last statement of its constructor.
    }

    /** Builds the dialog body. */
    protected abstract @NotNull JComponent createBody();

    /** @return an error when the entered values are not usable, otherwise {@code null}. */
    protected abstract @Nullable ValidationInfo validateFields();

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return createBody();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        return validateFields();
    }

    protected static @NotNull JPanel labeled(@NotNull String label, @NotNull JComponent component) {
        FormBuilder form = FormBuilder.createFormBuilder()
                .addLabeledComponent(label, component);
        return form.getPanel();
    }

    protected static @Nullable ValidationInfo requireText(@Nullable String value,
                                                          @NotNull String message,
                                                          @NotNull JComponent component) {
        if (value == null || value.trim().isEmpty()) {
            return new ValidationInfo(message, component);
        }
        return null;
    }

    protected static @Nullable ValidationInfo requireName(@Nullable String value,
                                                          @NotNull String message,
                                                          @NotNull JComponent component) {
        ValidationInfo missing = requireText(value, message, component);
        if (missing != null) {
            return missing;
        }
        if (!value.trim().matches(NAME_PATTERN)) {
            return new ValidationInfo("Only letters, digits, '_' and '-' are allowed", component);
        }
        return null;
    }

    /** Fills a combo box with the registered templates and selects {@code preferred} if present. */
    protected static void fillTemplates(@NotNull javax.swing.JComboBox<O3deManifest.Template> combo,
                                        @Nullable String preferred) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof O3deManifest.Template) {
                    O3deManifest.Template template = (O3deManifest.Template) value;
                    setText(template.getName());
                    setToolTipText(template.getSummary());
                }
                return this;
            }
        });
        combo.setEditable(true);
        List<O3deManifest.Template> templates = O3deManifest.templates();
        for (O3deManifest.Template template : templates) {
            combo.addItem(template);
        }
        if (preferred != null) {
            for (O3deManifest.Template template : templates) {
                if (template.getName().equalsIgnoreCase(preferred)) {
                    combo.setSelectedItem(template);
                    return;
                }
            }
        }
        combo.setSelectedItem(preferred == null ? "" : preferred);
    }

    /** @return the template name currently chosen, whether picked or typed. */
    protected static @NotNull String selectedTemplate(
            @NotNull javax.swing.JComboBox<O3deManifest.Template> combo) {
        Object item = combo.getSelectedItem();
        if (item instanceof O3deManifest.Template) {
            return ((O3deManifest.Template) item).getName();
        }
        return item == null ? "" : item.toString().trim();
    }

    protected static @NotNull JScrollPane scrollable(@NotNull JComponent component, int height) {
        JBScrollPane scrollPane = new JBScrollPane(component);
        scrollPane.setPreferredSize(JBUI.size(440, height));
        return scrollPane;
    }

    protected static @NotNull JBLabel hint(@NotNull String text) {
        JBLabel label = new JBLabel(text);
        label.setBorder(JBUI.Borders.emptyTop(6));
        return label;
    }

    protected static void updateOnType(@NotNull JTextField field, @NotNull Runnable action) {
        javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                action.run();
            }
        };
        field.getDocument().addDocumentListener(listener);
    }
}
