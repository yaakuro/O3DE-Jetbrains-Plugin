/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.lua;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.o3de.clion.settings.O3deSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Schedules Lua completion after a {@code .} is typed.
 *
 * <p>Plain text files (which is how a {@code .lua} file is edited when no Lua plugin is installed)
 * have no language-specific auto-popup trigger, so the member access dot would otherwise require a
 * manual code completion invocation.</p>
 */
public final class LuaTypedHandler extends TypedHandlerDelegate {

    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (c != '.' || !isLuaFile(file) || hasLuaLanguage(file)) {
            return Result.CONTINUE;
        }
        O3deSettings settings = O3deSettings.getInstance();
        if (!settings.isLuaCompletionEnabled()) {
            return Result.CONTINUE;
        }
        LuaReferenceService service = LuaReferenceService.getInstance(project);
        if (service.getData() == null) {
            service.refresh();
            return Result.CONTINUE;
        }
        AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
        return Result.CONTINUE;
    }

    /**
     * True when a real Lua language plugin (e.g. SumnekoLua/EmmyLua) owns the file. Those plugins
     * drive their own auto-popup and language server completion, so we must not interfere.
     */
    private static boolean hasLuaLanguage(PsiFile file) {
        return "Lua".equals(file.getLanguage().getID());
    }

    static boolean isLuaFile(PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile != null && virtualFile.getName().toLowerCase(Locale.ROOT).endsWith(".lua");
    }
}
