/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote;

/**
 * Helpers for O3DE's 16 byte {@code AZ::Uuid}, whose raw {@code m_data} bytes are already in the
 * canonical big-endian text order (the same order shown in reflection macros).
 */
public final class AzUuid {

    private AzUuid() {
    }

    public static byte[] fromString(String uuid) {
        String hex = uuid.replace("{", "").replace("}", "").replace("-", "");
        if (hex.length() != 32) {
            throw new IllegalArgumentException("Not a uuid: " + uuid);
        }
        byte[] out = new byte[16];
        for (int i = 0; i < 16; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    public static String toString(byte[] uuid) {
        StringBuilder builder = new StringBuilder(38);
        builder.append('{');
        for (int i = 0; i < 16; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                builder.append('-');
            }
            builder.append(Character.forDigit((uuid[i] >> 4) & 0xF, 16));
            builder.append(Character.forDigit(uuid[i] & 0xF, 16));
        }
        builder.append('}');
        return builder.toString().toUpperCase(java.util.Locale.ROOT);
    }
}
