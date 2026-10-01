/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.core;

import com.o3de.clion.settings.O3deSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Works out which {@code o3de.sh} / {@code o3de.bat} script should be executed.
 *
 * <p>Candidates are tried in this order: the path configured in the settings, the engine root
 * configured in the settings, every engine registered in {@code ~/.o3de/o3de_manifest.json} and
 * finally a handful of well known install locations.</p>
 */
public final class O3deCliLocator {

    private O3deCliLocator() {
    }

    /** @return {@code true} when the platform running the IDE is Windows. */
    public static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase(Locale.ROOT).startsWith("windows");
    }

    public static @NotNull String scriptFileName() {
        return isWindows() ? "o3de.bat" : "o3de.sh";
    }

    /** @return the script that should be executed, or {@code null} when O3DE cannot be found. */
    public static @Nullable Path locate() {
        O3deSettings settings = O3deSettings.getInstance();

        Path configured = normalize(settings.getConfiguredScriptPath());
        if (configured != null) {
            return configured;
        }

        Path engine = normalize(settings.getConfiguredEnginePath());
        if (engine != null) {
            Path script = scriptInside(engine);
            if (script != null) {
                return script;
            }
        }

        for (Path candidate : candidateScripts()) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** @return every usable candidate, in the order they should be tried. */
    public static @NotNull List<Path> candidates() {
        Set<Path> result = new LinkedHashSet<>();
        O3deSettings settings = O3deSettings.getInstance();

        Path configured = normalize(settings.getConfiguredScriptPath());
        if (configured != null) {
            result.add(configured);
        }
        Path engine = normalize(settings.getConfiguredEnginePath());
        if (engine != null) {
            Path script = scriptInside(engine);
            if (script != null) {
                result.add(script);
            }
        }
        result.addAll(candidateScripts());
        return new ArrayList<>(result);
    }

    /**
     * @return the engine root that owns {@code script}, or {@code null} when the path does not
     * follow the usual {@code <engine>/scripts/o3de.sh} layout.
     */
    public static @Nullable Path engineRootFor(@NotNull Path script) {
        Path parent = script.toAbsolutePath().normalize().getParent();
        if (parent == null || !parent.getFileName().toString().equalsIgnoreCase("scripts")) {
            return null;
        }
        return parent.getParent();
    }

    private static @NotNull List<Path> candidateScripts() {
        List<Path> result = new ArrayList<>();
        for (String engine : O3deManifest.stringList("engines")) {
            Path script = scriptInside(Path.of(engine));
            if (script != null) {
                result.add(script);
            }
        }

        String home = System.getProperty("user.home", "");
        if (!home.isEmpty()) {
            addIfPresent(result, Path.of(home, "o3de", "scripts", scriptFileName()));
            addIfPresent(result, Path.of(home, "O3DE", "scripts", scriptFileName()));
            addIfPresent(result, Path.of(home, ".o3de", "scripts", scriptFileName()));
        }
        addIfPresent(result, Path.of("/usr/local/o3de/scripts", scriptFileName()));
        addIfPresent(result, Path.of("/opt/o3de/scripts", scriptFileName()));
        addIfPresent(result, Path.of("C:\\o3de\\scripts", scriptFileName()));
        return result;
    }

    private static void addIfPresent(@NotNull List<Path> result, @NotNull Path candidate) {
        if (Files.isRegularFile(candidate)) {
            result.add(candidate);
        }
    }

    private static @Nullable Path scriptInside(@NotNull Path engineRoot) {
        Path script = engineRoot.resolve("scripts").resolve(scriptFileName());
        return Files.isRegularFile(script) ? script : null;
    }

    private static @Nullable Path normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Path path = Path.of(trimmed);
        if (Files.isDirectory(path)) {
            // Accept an engine root or a scripts folder typed into the settings field.
            Path asScript = path.resolve(scriptFileName());
            if (Files.isRegularFile(asScript)) {
                return asScript.normalize();
            }
            Path nested = path.resolve("scripts").resolve(scriptFileName());
            if (Files.isRegularFile(nested)) {
                return nested.normalize();
            }
        }
        return Files.isRegularFile(path) ? path.normalize() : null;
    }

    /** @return a one line description of where the script was found, for diagnostics. */
    public static @NotNull String describe() {
        Path script = locate();
        if (script == null) {
            return "o3de script not found";
        }
        Path engine = engineRootFor(script);
        return engine == null ? script.toString() : engine + " (" + script.getFileName() + ")";
    }

    public static boolean isExecutableScript(@Nullable Path script) {
        if (script == null) {
            return false;
        }
        File file = script.toFile();
        return file.isFile() && file.canExecute();
    }
}
