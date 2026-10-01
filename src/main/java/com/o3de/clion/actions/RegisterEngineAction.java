/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.o3de.clion.O3deIcons;
import com.o3de.clion.core.O3deCommandLine.RegisterKind;

/** Registers the engine that owns the configured script through {@code o3de register --this-engine}. */
public final class RegisterEngineAction extends RegisterAction {
    public RegisterEngineAction() {
        super(RegisterKind.ENGINE, "Register This Engine", O3deIcons.ToolWindow);
    }
}
