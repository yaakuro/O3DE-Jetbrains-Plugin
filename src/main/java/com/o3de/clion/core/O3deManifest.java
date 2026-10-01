/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Reads {@code ~/.o3de/o3de_manifest.json}, the registration database maintained by the
 * {@code o3de} command line tool. Reading the manifest directly avoids spawning a process
 * for simple lookups.
 */
public final class O3deManifest {

    private O3deManifest() {
    }

    /** Description of a registered template, read from its {@code template.json}. */
    public static final class Template {
        private final String name;
        private final Path path;
        private final String displayName;
        private final String summary;

        public Template(@NotNull String name, @NotNull Path path, @Nullable String displayName,
                        @Nullable String summary) {
            this.name = name;
            this.path = path;
            this.displayName = displayName;
            this.summary = summary;
        }

        public @NotNull String getName() {
            return name;
        }

        public @NotNull Path getPath() {
            return path;
        }

        public @Nullable String getDisplayName() {
            return displayName;
        }

        public @Nullable String getSummary() {
            return summary;
        }

        @Override
        public String toString() {
            return displayName == null || displayName.isEmpty() ? name : name + " - " + displayName;
        }
    }

    public static @Nullable Path manifestFile() {
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty()) {
            return null;
        }
        return Path.of(home, ".o3de", "o3de_manifest.json");
    }

    /** @return the raw manifest object, or {@code null} when it cannot be read. */
    public static @Nullable JsonObject read() {
        Path file = manifestFile();
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    /** @return every string entry stored under {@code key}, never {@code null}. */
    public static @NotNull List<String> stringList(@NotNull String key) {
        JsonObject root = read();
        if (root == null) {
            return Collections.emptyList();
        }
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonArray()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : (JsonArray) value) {
            if (element.isJsonPrimitive() && !element.getAsString().isEmpty()) {
                result.add(element.getAsString());
            }
        }
        return result;
    }

    public static @Nullable String defaultFolder(@NotNull String key) {
        JsonObject root = read();
        if (root == null) {
            return null;
        }
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return null;
        }
        String text = value.getAsString().trim();
        return text.isEmpty() ? null : text;
    }

    public static @NotNull List<Path> registeredPaths(@NotNull String key) {
        List<Path> paths = new ArrayList<>();
        for (String entry : stringList(key)) {
            paths.add(Path.of(entry));
        }
        return paths;
    }

    /**
     * @return every registered template, sorted by name. Templates whose {@code template.json}
     * cannot be parsed are skipped.
     */
    public static @NotNull List<Template> templates() {
        List<Template> templates = new ArrayList<>();
        for (Path root : registeredPaths("templates")) {
            Template template = readTemplate(root);
            if (template != null) {
                templates.add(template);
            }
        }
        templates.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return templates;
    }

    public static @Nullable Template findTemplate(@NotNull String name) {
        for (Template template : templates()) {
            if (template.getName().equalsIgnoreCase(name)) {
                return template;
            }
        }
        return null;
    }

    private static @Nullable Template readTemplate(@NotNull Path root) {
        Path descriptor = root.resolve("template.json");
        if (!Files.isRegularFile(descriptor)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(descriptor, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                return null;
            }
            JsonObject object = parsed.getAsJsonObject();
            String name = text(object, "template_name");
            if (name == null) {
                name = root.getFileName() == null ? root.toString() : root.getFileName().toString();
            }
            return new Template(name, root, text(object, "display_name"), text(object, "summary"));
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static @Nullable String text(@NotNull JsonObject object, @NotNull String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            return null;
        }
        String text = value.getAsString().trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * @return a human readable summary of what is registered, one entry per line, or an empty
     * list when the manifest is unavailable.
     */
    public static @NotNull List<String> describeRegistration() {
        JsonObject root = read();
        if (root == null) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>();
        for (String key : new String[]{"engines", "projects", "gems", "external_subdirectories",
                "templates", "restricted", "repos"}) {
            JsonElement value = root.get(key);
            if (value == null || !value.isJsonArray()) {
                continue;
            }
            JsonArray array = value.getAsJsonArray();
            lines.add(String.format(Locale.ROOT, "%s (%d)", key, array.size()));
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    lines.add("    " + element.getAsString());
                }
            }
        }
        return lines;
    }
}
