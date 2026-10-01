/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

/**
 * Icons shipped with the plugin. They are plain SVG files under {@code /icons}.
 */
public final class O3deIcons {

    private O3deIcons() {
    }

    public static final Icon ToolWindow = load("/icons/o3de.svg");
    public static final Icon Gem = load("/icons/gem.svg");
    public static final Icon Project = load("/icons/project.svg");
    public static final Icon Component = load("/icons/component.svg");
    public static final Icon Template = load("/icons/template.svg");
    public static final Icon Register = load("/icons/register.svg");
    public static final Icon Run = load("/icons/run.svg");
    public static final Icon Settings = load("/icons/settings.svg");

    private static Icon load(String path) {
        return IconLoader.getIcon(path, O3deIcons.class.getClassLoader());
    }
}
