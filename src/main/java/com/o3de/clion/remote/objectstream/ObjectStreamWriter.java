/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote.objectstream;

import com.o3de.clion.remote.AzCrc32;
import com.o3de.clion.remote.AzUuid;
import com.o3de.clion.remote.NetworkBuffer;

/**
 * Emits O3DE binary {@code AZ::ObjectStream} data.
 *
 * <p>Only the shape needed for {@code ScriptDebugRequest} messages is produced: a root class
 * element and, optionally, a few primitive fields. Missing fields simply keep their constructor
 * defaults on the engine side, which deserializes with
 * {@code FILTERFLAG_IGNORE_UNKNOWN_CLASSES}.</p>
 */
public final class ObjectStreamWriter {

    private static final int FLAG_ELEMENT_HEADER = 0x08;
    private static final int FLAG_HAS_VALUE = 0x10;
    private static final int FLAG_EXTRA_SIZE_FIELD = 0x20;
    private static final int FLAG_HAS_NAME = 0x40;

    public static final String UUID_U32 = "{43DA906B-7DEF-4CA8-9790-854106D3F983}";
    public static final String UUID_U64 = "{D6597933-47CD-4FC8-B911-63F3E2B0993A}";
    public static final String UUID_BOOL = "{A0CA880C-AFE4-43CB-926C-59AC48496112}";

    private final NetworkBuffer buffer = new NetworkBuffer();

    public ObjectStreamWriter() {
        buffer.writeU8(0);
        buffer.writeU32(3);
    }

    /** Begins a class element. A non-null name is serialized as its {@code AZ_CRC_CE} CRC. */
    public Element begin(String name, String classUuid) {
        return new Element(this, name, classUuid);
    }

    public byte[] finish() {
        buffer.writeU8(0);
        return buffer.toByteArray();
    }

    private void writeHeader(String name, String classUuid, int valueSize) {
        int flags = FLAG_ELEMENT_HEADER;
        if (name != null) {
            flags |= FLAG_HAS_NAME;
        }
        if (valueSize >= 0) {
            flags |= FLAG_HAS_VALUE;
            if (valueSize < 8) {
                flags |= valueSize;
            } else {
                flags |= FLAG_EXTRA_SIZE_FIELD;
                if (valueSize < 0x100) {
                    flags |= 1;
                } else if (valueSize < 0x10000) {
                    flags |= 2;
                } else {
                    flags |= 4;
                }
            }
        }
        buffer.writeU8(flags);
        if (name != null) {
            buffer.writeU32(AzCrc32.ofLower(name) & 0xFFFFFFFFL);
        }
        buffer.writeBytes(AzUuid.fromString(classUuid));
        if (valueSize >= 8) {
            if (valueSize < 0x100) {
                buffer.writeU8(valueSize);
            } else if (valueSize < 0x10000) {
                buffer.writeU16(valueSize);
            } else {
                buffer.writeU32(valueSize);
            }
        }
    }

    private void writeRawValue(String name, String classUuid, byte[] value) {
        writeHeader(name, classUuid, value.length);
        buffer.writeBytes(value);
    }

    private void writeU32Value(String name, String classUuid, long value) {
        writeHeader(name, classUuid, 4);
        buffer.writeU32(value);
    }

    private void writeBoolValue(String name, String classUuid, boolean value) {
        writeHeader(name, classUuid, 1);
        buffer.writeU8(value ? 1 : 0);
    }

    private void closeElement() {
        buffer.writeU8(0);
    }

    /** A class element that can hold child fields. */
    public static final class Element {
        private final ObjectStreamWriter writer;
        private final String classUuid;
        private boolean closed;

        private Element(ObjectStreamWriter writer, String name, String classUuid) {
            this.writer = writer;
            this.classUuid = classUuid;
            writer.writeHeader(name, classUuid, -1);
        }

        public void u32(String name, long value) {
            writer.writeU32Value(name, UUID_U32, value);
            writer.closeElement();
        }

        public void u64(String name, long value) {
            writer.writeHeader(name, UUID_U64, 8);
            writer.buffer.writeU64(value);
            writer.closeElement();
        }

        public void bool(String name, boolean value) {
            writer.writeBoolValue(name, UUID_BOOL, value);
            writer.closeElement();
        }

        /** Writes a child element whose value bytes are copied verbatim (e.g. an {@code AZStd::string}). */
        public void bytes(String name, String classUuid, byte[] value) {
            writer.writeRawValue(name, classUuid, value);
            writer.closeElement();
        }

        public Element beginChild(String name, String childClassUuid) {
            return writer.begin(name, childClassUuid);
        }

        public void close() {
            if (!closed) {
                writer.closeElement();
                closed = true;
            }
        }
    }
}
