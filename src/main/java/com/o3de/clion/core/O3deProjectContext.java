/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects which O3DE object the currently opened project corresponds to by looking for
 * {@code project.json} and {@code gem.json} descriptors.
 */
public final class O3deProjectContext {

    private static final int MAX_WALK_UP = 6;

    private O3deProjectContext() {
    }

    /** An O3DE gem found in the opened project. */
    public static final class Gem {
        private final Path root;
        private final String name;

        public Gem(@NotNull Path root, @NotNull String name) {
            this.root = root;
            this.name = name;
        }

        public @NotNull Path getRoot() {
            return root;
        }

        public @NotNull String getName() {
            return name;
        }

        /** @return the directory a component template should be instantiated into. */
        public @NotNull Path componentDestination() {
            Path code = root.resolve("Code");
            return Files.isDirectory(code) ? code : root;
        }
    }

    /** An O3DE project found in the opened project. */
    public static final class ProjectInfo {
        private final Path root;
        private final String name;

        public ProjectInfo(@NotNull Path root, @NotNull String name) {
            this.root = root;
            this.name = name;
        }

        public @NotNull Path getRoot() {
            return root;
        }

        public @NotNull String getName() {
            return name;
        }
    }

    public static @Nullable Path projectBasePath(@NotNull Project project) {
        String basePath = project.getBasePath();
        return basePath == null || basePath.isEmpty() ? null : Path.of(basePath);
    }

    public static @Nullable Gem findGem(@NotNull Project project, @Nullable Path start) {
        Path from = start != null ? start : projectBasePath(project);
        for (Path candidate : walkUp(from)) {
            if (!Files.isRegularFile(candidate.resolve("gem.json"))) {
                continue;
            }
            String name = jsonText(candidate.resolve("gem.json"), "gem_name");
            if (name == null) {
                name = candidate.getFileName() == null ? candidate.toString()
                        : candidate.getFileName().toString();
            }
            return new Gem(candidate, name);
        }
        return null;
    }

    public static @Nullable ProjectInfo findProject(@NotNull Project project, @Nullable Path start) {
        Path from = start != null ? start : projectBasePath(project);
        for (Path candidate : walkUp(from)) {
            if (!Files.isRegularFile(candidate.resolve("project.json"))) {
                continue;
            }
            String name = jsonText(candidate.resolve("project.json"), "name");
            if (name == null) {
                name = candidate.getFileName() == null ? candidate.toString()
                        : candidate.getFileName().toString();
            }
            return new ProjectInfo(candidate, name);
        }
        return null;
    }

    /** @return where a component template should go, or {@code null} when nothing was detected. */
    public static @Nullable Path defaultComponentDestination(@NotNull Project project, @Nullable Path start) {
        Gem gem = findGem(project, start);
        if (gem != null) {
            return gem.componentDestination();
        }
        Path base = projectBasePath(project);
        if (base == null) {
            return null;
        }
        Path code = base.resolve("Code");
        return Files.isDirectory(code) ? code : base;
    }

    /** @return the first directory level that contains a descriptor, starting at {@code from}. */
    public static @NotNull List<Path> walkUp(@Nullable Path from) {
        List<Path> result = new ArrayList<>();
        if (from == null) {
            return result;
        }
        Path current = from.toAbsolutePath().normalize();
        for (int i = 0; i <= MAX_WALK_UP && current != null; i++) {
            result.add(current);
            current = current.getParent();
        }
        return result;
    }

    public static @Nullable Path directoryOf(@Nullable VirtualFile file) {
        if (file == null) {
            return null;
        }
        VirtualFile directory = file.isDirectory() ? file : file.getParent();
        if (directory == null) {
            return null;
        }
        String path = directory.getPath();
        return path.isEmpty() ? null : Path.of(path);
    }

    private static @Nullable String jsonText(@NotNull Path file, @NotNull String key) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return null;
            }
            JsonElement value = ((JsonObject) parsed).get(key);
            if (value == null || !value.isJsonPrimitive()) {
                return null;
            }
            String text = value.getAsString().trim();
            return text.isEmpty() ? null : text;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }
}
