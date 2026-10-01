/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.lua;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.o3de.clion.remote.script.LuaReferenceData;
import com.o3de.clion.settings.O3deSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides Lua code completion from the reflected O3DE API surface.
 *
 * <p>The contributor is registered for {@code language="ANY"} and filters on the {@code .lua}
 * extension so it works whether or not a third-party Lua plugin is installed (a plain text file
 * still has no Lua language id).</p>
 */
public final class LuaCompletionContributor extends CompletionContributor {

    public LuaCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new LuaCompletionProvider());
    }

    private static final class LuaCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters,
                                      @NotNull ProcessingContext context,
                                      @NotNull CompletionResultSet result) {
            if (!O3deSettings.getInstance().isLuaCompletionEnabled()) {
                return;
            }
            Project project = parameters.getPosition().getProject();
            LuaReferenceService service = LuaReferenceService.getInstance(project);
            LuaReferenceData data = service.getData();
            if (data == null || data.isEmpty()) {
                if (service.isEnabled()) {
                    service.refresh();
                }
                return;
            }
            if (!isLuaFile(parameters.getOriginalFile())) {
                return;
            }

            List<String> receiverChain = receiverChain(parameters);
            if (receiverChain.isEmpty()) {
                addTopLevel(data, result);
                return;
            }

            String receiver = receiverChain.get(0);
            String member = receiverChain.size() == 2
                    ? receiverChain.get(1).toLowerCase(Locale.ROOT)
                    : null;

            LuaReferenceData.Ebus ebus = findEbus(data, receiver);
            LuaReferenceData.ScriptClass scriptClass = findClass(data, receiver);

            if (receiverChain.size() == 1) {
                if (ebus != null) {
                    add(result, "Broadcast", "EBus broadcast accessor");
                    add(result, "Event", "EBus event handler accessor");
                    add(result, "Queue", "EBus queued broadcast accessor");
                } else if (scriptClass != null) {
                    addMembers(scriptClass, result);
                }
            } else if (ebus != null && ("broadcast".equals(member) || "event".equals(member) || "queue".equals(member))) {
                for (LuaReferenceData.Method event : ebus.events()) {
                    result.addElement(LookupElementBuilder.create(event.name())
                            .withTypeText("EBus event")
                            .withTailText(event.paramInfo().isEmpty() ? "()" : "(" + event.paramInfo() + ")", true));
                }
            }
        }

        private static void addTopLevel(LuaReferenceData data, CompletionResultSet result) {
            for (LuaReferenceData.ScriptClass scriptClass : data.classes()) {
                add(result, scriptClass.name(), "O3DE class");
            }
            for (LuaReferenceData.Ebus ebus : data.ebuses()) {
                add(result, ebus.name(), "O3DE EBus");
            }
            for (LuaReferenceData.Method method : data.globalMethods()) {
                result.addElement(LookupElementBuilder.create(method.name())
                        .withTypeText("O3DE global")
                        .withTailText(method.paramInfo().isEmpty() ? "()" : "(" + method.paramInfo() + ")", true));
            }
            for (LuaReferenceData.Property property : data.globalProperties()) {
                result.addElement(LookupElementBuilder.create(property.name())
                        .withTypeText(property.readable() ? "O3DE global property" : "O3DE global property (write only)"));
            }
        }

        private static void addMembers(LuaReferenceData.ScriptClass scriptClass, CompletionResultSet result) {
            for (LuaReferenceData.Method method : scriptClass.methods()) {
                result.addElement(LookupElementBuilder.create(method.name())
                        .withTypeText("method")
                        .withTailText(method.paramInfo().isEmpty() ? "()" : "(" + method.paramInfo() + ")", true));
            }
            for (LuaReferenceData.Property property : scriptClass.properties()) {
                result.addElement(LookupElementBuilder.create(property.name()).withTypeText("property"));
            }
        }

        private static void add(CompletionResultSet result, String name, String type) {
            if (!name.isEmpty()) {
                result.addElement(LookupElementBuilder.create(name).withTypeText(type));
            }
        }

        private static LuaReferenceData.Ebus findEbus(LuaReferenceData data, String name) {
            for (LuaReferenceData.Ebus ebus : data.ebuses()) {
                if (ebus.name().equals(name)) {
                    return ebus;
                }
            }
            return null;
        }

        private static LuaReferenceData.ScriptClass findClass(LuaReferenceData data, String name) {
            for (LuaReferenceData.ScriptClass scriptClass : data.classes()) {
                if (scriptClass.name().equals(name)) {
                    return scriptClass;
                }
            }
            return null;
        }

        private static boolean isLuaFile(PsiFile file) {
            VirtualFile virtualFile = file.getVirtualFile();
            return virtualFile != null && virtualFile.getName().toLowerCase(Locale.ROOT).endsWith(".lua");
        }

        /** Returns the complete dotted segments before the caret, e.g. {@code [TransformBus, Event]}. */
        private static List<String> receiverChain(CompletionParameters parameters) {
            String text = parameters.getEditor().getDocument().getText();
            int offset = Math.min(parameters.getOffset(), text.length());

            int end = offset;
            int start = end;
            while (start > 0) {
                char c = text.charAt(start - 1);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                    start--;
                } else {
                    break;
                }
            }
            String expression = text.substring(start, end);
            int dot = expression.lastIndexOf('.');
            if (dot < 0) {
                return List.of();
            }
            String beforeDot = expression.substring(0, dot);
            List<String> segments = new ArrayList<>();
            for (String segment : beforeDot.split("\\.")) {
                if (!segment.isEmpty()) {
                    segments.add(segment);
                }
            }
            return segments;
        }
    }
}
