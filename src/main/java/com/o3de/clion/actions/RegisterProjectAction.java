/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.o3de.clion.O3deIcons;
import com.o3de.clion.core.O3deCommandLine.RegisterKind;

/** Registers a project folder with O3DE through {@code o3de register -pp}. */
public final class RegisterProjectAction extends RegisterAction {
    public RegisterProjectAction() {
        super(RegisterKind.PROJECT, "Register Project...", O3deIcons.Project);
    }
}
