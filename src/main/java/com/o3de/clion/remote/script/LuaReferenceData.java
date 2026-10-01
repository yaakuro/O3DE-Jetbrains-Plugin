/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Lua-visible API surface reported by a running O3DE target: reflected classes, EBuses and
 * global C++ functions/properties.
 */
public final class LuaReferenceData {

    public record Method(String name, String paramInfo, String category) {
    }

    public record Property(String name, boolean readable, boolean writable) {
    }

    public record ScriptClass(String name, String typeId, List<Method> methods, List<Property> properties) {
    }

    public record Ebus(String name, List<Method> events, boolean canBroadcast, boolean canQueue, boolean hasHandler) {
    }

    private final List<ScriptClass> classes;
    private final List<Ebus> ebuses;
    private final List<Method> globalMethods;
    private final List<Property> globalProperties;
    private final List<String> contexts;
    private final String stringTypeId;

    private LuaReferenceData(Builder builder) {
        classes = List.copyOf(builder.classes);
        ebuses = List.copyOf(builder.ebuses);
        globalMethods = List.copyOf(builder.globalMethods);
        globalProperties = List.copyOf(builder.globalProperties);
        contexts = List.copyOf(builder.contexts);
        stringTypeId = builder.stringTypeId;
    }

    public static final class Builder {
        private final List<ScriptClass> classes = new ArrayList<>();
        private final List<Ebus> ebuses = new ArrayList<>();
        private final List<Method> globalMethods = new ArrayList<>();
        private final List<Property> globalProperties = new ArrayList<>();
        private final List<String> contexts = new ArrayList<>();
        private String stringTypeId;

        public List<ScriptClass> classes() {
            return classes;
        }

        public List<Ebus> ebuses() {
            return ebuses;
        }

        public List<Method> globalMethods() {
            return globalMethods;
        }

        public List<Property> globalProperties() {
            return globalProperties;
        }

        public List<String> contexts() {
            return contexts;
        }

        public void setStringTypeId(String stringTypeId) {
            this.stringTypeId = stringTypeId;
        }

        public LuaReferenceData build() {
            return new LuaReferenceData(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ScriptClass> classes() {
        return classes;
    }

    public List<Ebus> ebuses() {
        return ebuses;
    }

    public List<Method> globalMethods() {
        return globalMethods;
    }

    public List<Property> globalProperties() {
        return globalProperties;
    }

    public List<String> contexts() {
        return Collections.unmodifiableList(contexts);
    }

    public String stringTypeId() {
        return stringTypeId;
    }

    public boolean isEmpty() {
        return classes.isEmpty() && ebuses.isEmpty() && globalMethods.isEmpty() && globalProperties.isEmpty();
    }
}
