/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.core;

import com.o3de.clion.settings.O3deSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds argument vectors for the {@code o3de} command line tool.
 *
 * <p>Every method returns a complete command: the script first, then the sub command, then the
 * optional extra arguments from the settings, then the arguments of the sub command. Keeping the
 * construction here means the UI classes never have to remember the flag spellings.</p>
 */
public final class O3deCommandLine {

    private O3deCommandLine() {
    }

    /** Which kind of object {@code o3de register} should operate on. */
    public enum RegisterKind {
        ENGINE("--this-engine", "engine"),
        PROJECT("-pp", "project"),
        GEM("-gp", "gem"),
        TEMPLATE("-tp", "template"),
        EXTERNAL("-es", "external folder");

        private final String flag;
        private final String description;

        RegisterKind(@NotNull String flag, @NotNull String description) {
            this.flag = flag;
            this.description = description;
        }

        public @NotNull String getFlag() {
            return flag;
        }

        public @NotNull String getDescription() {
            return description;
        }
    }

    /** Creates the argument list for the given sub command, including the configured extras. */
    public static @NotNull List<String> start(@NotNull Path script, @NotNull String subCommand) {
        List<String> command = new ArrayList<>();
        command.add(script.toAbsolutePath().normalize().toString());
        command.add(subCommand);
        command.addAll(extraArguments());
        return command;
    }

    public static @NotNull List<String> createGem(@NotNull Path script,
                                                  @NotNull String gemPath,
                                                  @Nullable String gemName,
                                                  @Nullable String templateName,
                                                  @Nullable String displayName,
                                                  @Nullable String summary,
                                                  boolean noRegister,
                                                  @NotNull Map<String, String> replacements) {
        List<String> command = start(script, "create-gem");
        add(command, "-gp", gemPath);
        addIfNotEmpty(command, "-gn", gemName);
        addIfNotEmpty(command, "-tn", templateName);
        addIfNotEmpty(command, "-dn", displayName);
        addIfNotEmpty(command, "-s", summary);
        if (noRegister) {
            command.add("--no-register");
        }
        addReplacements(command, replacements);
        return command;
    }

    public static @NotNull List<String> createProject(@NotNull Path script,
                                                      @NotNull String projectPath,
                                                      @Nullable String projectName,
                                                      @Nullable String templateName,
                                                      @Nullable String projectId,
                                                      boolean noRegister,
                                                      @NotNull Map<String, String> replacements) {
        List<String> command = start(script, "create-project");
        add(command, "-pp", projectPath);
        addIfNotEmpty(command, "-pn", projectName);
        addIfNotEmpty(command, "-tn", templateName);
        addIfNotEmpty(command, "--project-id", projectId);
        if (noRegister) {
            command.add("--no-register");
        }
        addReplacements(command, replacements);
        return command;
    }

    public static @NotNull List<String> createFromTemplate(@NotNull Path script,
                                                           @NotNull String destinationPath,
                                                           @NotNull String templateName,
                                                           @Nullable String destinationName,
                                                           @NotNull Map<String, String> replacements) {
        List<String> command = start(script, "create-from-template");
        add(command, "-dp", destinationPath);
        addIfNotEmpty(command, "-tn", templateName);
        addIfNotEmpty(command, "-dn", destinationName);
        addReplacements(command, replacements);
        return command;
    }

    public static @NotNull List<String> register(@NotNull Path script,
                                                 @NotNull RegisterKind kind,
                                                 @Nullable String targetPath) {
        List<String> command = start(script, "register");
        command.add(kind.getFlag());
        if (targetPath != null && !targetPath.trim().isEmpty()) {
            command.add(targetPath.trim());
        }
        return command;
    }

    public static @NotNull List<String> enableGem(@NotNull Path script,
                                                  @Nullable String projectPath,
                                                  @Nullable String gemPath,
                                                  boolean force) {
        List<String> command = start(script, "enable-gem");
        addIfNotEmpty(command, "-pp", projectPath);
        addIfNotEmpty(command, "-gp", gemPath);
        if (force) {
            command.add("-f");
        }
        return command;
    }

    public static @NotNull List<String> disableGem(@NotNull Path script,
                                                   @Nullable String projectPath,
                                                   @Nullable String gemPath) {
        List<String> command = start(script, "disable-gem");
        addIfNotEmpty(command, "-pp", projectPath);
        addIfNotEmpty(command, "-gp", gemPath);
        return command;
    }

    public static @NotNull List<String> registerShow(@NotNull Path script) {
        return start(script, "register-show");
    }

    public static @NotNull List<String> getRegistered(@NotNull Path script, @NotNull String selector) {
        List<String> command = start(script, "get-registered");
        command.add("-df");
        command.add(selector);
        return command;
    }

    /** Builds a command from a sub command chosen by the user plus free form arguments. */
    public static @NotNull List<String> custom(@NotNull Path script,
                                               @NotNull String subCommand,
                                               @NotNull String rawArguments) {
        List<String> command = start(script, subCommand.trim());
        for (String token : splitArguments(rawArguments)) {
            command.add(token);
        }
        return command;
    }

    public static @NotNull List<String> withArgs(@NotNull Path script, @NotNull String subCommand,
                                                 @NotNull String... args) {
        List<String> command = start(script, subCommand);
        Collections.addAll(command, args);
        return command;
    }

    /** Splits a free form argument string on whitespace while honouring simple quoting. */
    public static @NotNull List<String> splitArguments(@NotNull String raw) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else {
                    current.append(c);
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    /** Renders a command the way it could be pasted into a shell. */
    public static @NotNull String render(@NotNull List<String> command) {
        StringBuilder builder = new StringBuilder();
        for (String argument : command) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(quote(argument));
        }
        return builder.toString();
    }

    public static @NotNull String subCommandOf(@NotNull List<String> command) {
        return command.size() > 1 ? command.get(1) : "";
    }

    public static @NotNull Map<String, String> replacements(@NotNull String... pairs) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            result.put(pairs[i], pairs[i + 1]);
        }
        return result;
    }

    private static void addReplacements(@NotNull List<String> command, @NotNull Map<String, String> replacements) {
        if (replacements.isEmpty()) {
            return;
        }
        command.add("-r");
        replacements.forEach((key, value) -> {
            command.add(key);
            command.add(value);
        });
    }

    private static void add(@NotNull List<String> command, @NotNull String flag, @NotNull String value) {
        command.add(flag);
        command.add(value.trim());
    }

    private static void addIfNotEmpty(@NotNull List<String> command, @NotNull String flag, @Nullable String value) {
        if (value != null && !value.trim().isEmpty()) {
            add(command, flag, value);
        }
    }

    private static @NotNull List<String> extraArguments() {
        String extra = O3deSettings.getInstance().getExtraArgs();
        if (extra.isEmpty()) {
            return Collections.emptyList();
        }
        return splitArguments(extra);
    }

    private static @NotNull String quote(@NotNull String value) {
        if (value.isEmpty()) {
            return "''";
        }
        boolean needsQuotes = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || c == '\'' || c == '"' || c == '\\' || c == '$' || c == '`') {
                needsQuotes = true;
                break;
            }
        }
        if (!needsQuotes) {
            return value;
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
