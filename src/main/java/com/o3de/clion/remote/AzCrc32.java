/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * The CRC-32 variant used by O3DE's {@code AZ::Crc32}.
 *
 * <p>The polynomial is {@code 0xEDB88320} with the usual {@code 0xFFFFFFFF} init and final xor.
 * O3DE's {@code AZ_CRC_CE("...")} macro hashes strings through {@code AZStd::string_view}, which
 * lower-cases ASCII letters before hashing. Field names and enums in serialized data therefore
 * need the lower-cased variant.</p>
 */
public final class AzCrc32 {

    private static final int[] TABLE = buildTable();

    private AzCrc32() {
    }

    /** Hashes the string as-is. */
    public static int of(String value) {
        return of(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    /** Hashes the string exactly like {@code AZ_CRC_CE(literal)} does (ASCII lower-cased). */
    public static int ofLower(String value) {
        return of(value.toLowerCase(Locale.ROOT));
    }

    public static int of(byte[] data) {
        int crc = 0xFFFFFFFF;
        for (byte b : data) {
            crc = TABLE[(crc ^ b) & 0xFF] ^ (crc >>> 8);
        }
        return crc ^ 0xFFFFFFFF;
    }

    private static int[] buildTable() {
        int[] table = new int[256];
        for (int i = 0; i < 256; i++) {
            int c = i;
            for (int k = 0; k < 8; k++) {
                c = (c & 1) != 0 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
            }
            table[i] = c;
        }
        return table;
    }
}
