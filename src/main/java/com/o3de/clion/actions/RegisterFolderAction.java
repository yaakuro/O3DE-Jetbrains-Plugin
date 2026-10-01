/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.o3de.clion.O3deIcons;
import com.o3de.clion.core.O3deCommandLine.RegisterKind;

/** Registers an external sub directory with O3DE through {@code o3de register -es}. */
public final class RegisterFolderAction extends RegisterAction {
    public RegisterFolderAction() {
        super(RegisterKind.EXTERNAL, "Register External Folder...", O3deIcons.Register);
    }
}
