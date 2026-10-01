/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote.objectstream;

/**
 * Decodes O3DE's binary {@code AZ::ObjectStream} format into a {@link OsElement} tree.
 *
 * <p>Stream layout (all integers big-endian):</p>
 * <pre>
 *   u8  streamTag (0)
 *   u32 objectStreamVersion (3)
 *   element*
 * </pre>
 *
 * <p>Each element starts with a flags byte. {@code 0} terminates the currently open element.
 * Otherwise the bits are: {@code 0x08} element header (always set), {@code 0x10} has value,
 * {@code 0x20} value size is stored in a separate field, {@code 0x40} has name, {@code 0x80} has
 * version. The low three bits hold the value size when it is below eight, otherwise the width of
 * the separate size field. The name (a {@code u32} CRC), an optional version byte and the 16 byte
 * type uuid follow, then the value.</p>
 */
public final class ObjectStreamReader {

    private static final int FLAG_HAS_VALUE = 0x10;
    private static final int FLAG_EXTRA_SIZE_FIELD = 0x20;
    private static final int FLAG_HAS_NAME = 0x40;
    private static final int FLAG_HAS_VERSION = 0x80;
    private static final int VALUE_SIZE_MASK = 0x07;

    private final byte[] data;
    private int pos;

    private ObjectStreamReader(byte[] data) {
        this.data = data;
    }

    public static OsElement readRoot(byte[] data) {
        ObjectStreamReader reader = new ObjectStreamReader(data);
        reader.readU8();
        reader.readU32();
        return reader.parseElement();
    }

    private OsElement parseElement() {
        int flags = readU8();
        if (flags == 0) {
            return null;
        }

        OsElement element = new OsElement();
        if ((flags & FLAG_HAS_NAME) != 0) {
            element.setNameCrc((int) readU32());
        }
        if ((flags & FLAG_HAS_VERSION) != 0) {
            element.setVersion(readU8());
        }
        element.setUuid(com.o3de.clion.remote.AzUuid.toString(readBytes(16)));

        if ((flags & FLAG_HAS_VALUE) != 0) {
            int size = flags & VALUE_SIZE_MASK;
            if ((flags & FLAG_EXTRA_SIZE_FIELD) != 0) {
                size = (int) (switch (size) {
                    case 1 -> readU8();
                    case 2 -> readU16();
                    case 4 -> readU32();
                    default -> throw new IllegalStateException("Invalid ObjectStream value size width " + size);
                });
            }
            element.setHasValue(true);
            element.setValue(readBytes(size));
        }

        OsElement child;
        while ((child = parseElement()) != null) {
            element.children().add(child);
        }
        return element;
    }

    private int readU8() {
        return data[pos++] & 0xFF;
    }

    private int readU16() {
        int value = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
        pos += 2;
        return value;
    }

    private long readU32() {
        long value = ((long) (data[pos] & 0xFF) << 24)
                | ((long) (data[pos + 1] & 0xFF) << 16)
                | ((long) (data[pos + 2] & 0xFF) << 8)
                | (data[pos + 3] & 0xFF);
        pos += 4;
        return value;
    }

    private byte[] readBytes(int length) {
        byte[] out = java.util.Arrays.copyOfRange(data, pos, pos + length);
        pos += length;
        return out;
    }
}
