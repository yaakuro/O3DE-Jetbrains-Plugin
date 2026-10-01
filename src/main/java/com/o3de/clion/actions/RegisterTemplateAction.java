/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.actions;

import com.o3de.clion.O3deIcons;
import com.o3de.clion.core.O3deCommandLine.RegisterKind;

/** Registers a template folder with O3DE through {@code o3de register -tp}. */
public final class RegisterTemplateAction extends RegisterAction {
    public RegisterTemplateAction() {
        super(RegisterKind.TEMPLATE, "Register Template...", O3deIcons.Template);
    }
}
