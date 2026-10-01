/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote.script;

import com.o3de.clion.remote.AzUuid;
import com.o3de.clion.remote.objectstream.ObjectStreamReader;
import com.o3de.clion.remote.objectstream.OsElement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Turns {@code ScriptDebugAgent} response blobs into {@link LuaReferenceData}. */
public final class ScriptDebugParser {

    public record Contexts(List<String> names, String stringTypeId) {
    }

    private ScriptDebugParser() {
    }

    public static String rootUuid(byte[] message) {
        OsElement root = ObjectStreamReader.readRoot(message);
        return root == null ? null : root.uuid();
    }

    public static Contexts parseContexts(byte[] message) {
        OsElement root = ObjectStreamReader.readRoot(message);
        List<String> names = new ArrayList<>();
        String stringTypeId = null;
        OsElement vector = root == null ? null : root.field("names");
        if (vector != null) {
            for (OsElement item : vector.children()) {
                names.add(stringOf(item));
                if (stringTypeId == null) {
                    stringTypeId = item.uuid();
                }
            }
        }
        return new Contexts(names, stringTypeId);
    }

    public static void parseGlobals(byte[] message, LuaReferenceData.Builder builder) {
        OsElement root = ObjectStreamReader.readRoot(message);
        if (root == null) {
            return;
        }
        builder.globalMethods().addAll(methods(root.field("methods")));
        builder.globalProperties().addAll(properties(root.field("properties")));
    }

    public static void parseClasses(byte[] message, LuaReferenceData.Builder builder) {
        OsElement root = ObjectStreamReader.readRoot(message);
        OsElement vector = root == null ? null : root.field("classes");
        if (vector == null) {
            return;
        }
        for (OsElement item : vector.children()) {
            builder.classes().add(new LuaReferenceData.ScriptClass(
                    stringOf(item.fieldOrBase("name")),
                    uuidOf(item.fieldOrBase("type")),
                    methods(item.fieldOrBase("methods")),
                    properties(item.fieldOrBase("properties"))));
        }
    }

    public static void parseEbus(byte[] message, LuaReferenceData.Builder builder) {
        OsElement root = ObjectStreamReader.readRoot(message);
        OsElement vector = root == null ? null : root.field("EBusses");
        if (vector == null) {
            return;
        }
        for (OsElement item : vector.children()) {
            builder.ebuses().add(new LuaReferenceData.Ebus(
                    stringOf(item.fieldOrBase("name")),
                    methods(item.fieldOrBase("events"), "category"),
                    boolOf(item.fieldOrBase("canBroadcast")),
                    boolOf(item.fieldOrBase("canQueue")),
                    boolOf(item.fieldOrBase("hasHandler"))));
        }
    }

    private static List<LuaReferenceData.Method> methods(OsElement vector) {
        return methods(vector, null);
    }

    private static List<LuaReferenceData.Method> methods(OsElement vector, String categoryField) {
        List<LuaReferenceData.Method> result = new ArrayList<>();
        if (vector == null) {
            return result;
        }
        for (OsElement item : vector.children()) {
            String category = categoryField == null ? "" : stringOf(item.fieldOrBase(categoryField));
            result.add(new LuaReferenceData.Method(
                    stringOf(item.fieldOrBase("name")),
                    stringOf(item.fieldOrBase("info")),
                    category));
        }
        return result;
    }

    private static List<LuaReferenceData.Property> properties(OsElement vector) {
        List<LuaReferenceData.Property> result = new ArrayList<>();
        if (vector == null) {
            return result;
        }
        for (OsElement item : vector.children()) {
            result.add(new LuaReferenceData.Property(
                    stringOf(item.fieldOrBase("name")),
                    boolOf(item.fieldOrBase("isRead")),
                    boolOf(item.fieldOrBase("isWrite"))));
        }
        return result;
    }

    private static String stringOf(OsElement element) {
        return element == null ? "" : new String(element.value(), StandardCharsets.UTF_8);
    }

    private static String uuidOf(OsElement element) {
        if (element == null || element.value().length != 16) {
            return "";
        }
        return AzUuid.toString(element.value());
    }

    private static boolean boolOf(OsElement element) {
        return element != null && element.value().length > 0 && element.value()[0] != 0;
    }
}
