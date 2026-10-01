/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote.objectstream;

import java.util.ArrayList;
import java.util.List;

/** A single node of a binary {@code AZ::ObjectStream}, decoded into an untyped tree. */
public final class OsElement {

    private int nameCrc;
    private int version;
    private String uuid;
    private byte[] value = new byte[0];
    private boolean hasValue;
    private final List<OsElement> children = new ArrayList<>();

    public int nameCrc() {
        return nameCrc;
    }

    void setNameCrc(int nameCrc) {
        this.nameCrc = nameCrc;
    }

    public int version() {
        return version;
    }

    void setVersion(int version) {
        this.version = version;
    }

    public String uuid() {
        return uuid;
    }

    void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean hasValue() {
        return hasValue;
    }

    void setHasValue(boolean hasValue) {
        this.hasValue = hasValue;
    }

    public byte[] value() {
        return value;
    }

    void setValue(byte[] value) {
        this.value = value;
    }

    public List<OsElement> children() {
        return children;
    }

    /**
     * Finds the direct child carrying the given serialized field name. Field name CRCs are hashed
     * by {@code AZ_CRC_CE} which lower-cases ASCII, so the comparison uses the same variant.
     */
    public OsElement field(String name) {
        int crc = com.o3de.clion.remote.AzCrc32.ofLower(name);
        for (OsElement child : children) {
            if (child.nameCrc == crc) {
                return child;
            }
        }
        return null;
    }

    /**
     * Finds a field that may live in a reflected base class. O3DE serializes base classes as nested
     * elements named {@code BaseClass1}, {@code BaseClass2}, ... so a derived type's inherited
     * fields are not direct children.
     */
    public OsElement fieldOrBase(String name) {
        OsElement direct = field(name);
        if (direct != null) {
            return direct;
        }
        for (OsElement child : children) {
            if (child.isBaseClass()) {
                OsElement found = child.fieldOrBase(name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean isBaseClass() {
        return nameCrc == com.o3de.clion.remote.AzCrc32.ofLower("BaseClass1")
                || nameCrc == com.o3de.clion.remote.AzCrc32.ofLower("BaseClass2")
                || nameCrc == com.o3de.clion.remote.AzCrc32.ofLower("BaseClass3");
    }

    @Override
    public String toString() {
        return "OsElement{crc=0x" + Integer.toHexString(nameCrc) + ", uuid=" + uuid
                + ", value=" + value.length + "B, children=" + children.size() + '}';
    }
}
