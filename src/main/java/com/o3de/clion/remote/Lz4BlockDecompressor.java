/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote;

import java.util.Arrays;

/**
 * A block-format LZ4 decompressor.
 *
 * <p>O3DE compresses every TCP payload except the initial handshake with an LZ4 block compressor
 * when the {@code MultiplayerCompression} gem is active. Rather than pulling in a native library,
 * the block format is decoded here directly. The input is a raw block (no frame header) and the
 * output length is discovered while decoding, which is exactly what {@code LZ4_decompress_safe}
 * does when given a large enough destination.</p>
 */
public final class Lz4BlockDecompressor {

    private static final int MAX_OUTPUT = 1 << 22;

    private Lz4BlockDecompressor() {
    }

    public static byte[] decompress(byte[] source) {
        byte[] out = new byte[Math.max(64, source.length * 4)];
        int src = 0;
        int dst = 0;

        while (src < source.length) {
            int token = source[src++] & 0xFF;

            int literalLength = token >>> 4;
            if (literalLength == 15) {
                int extra;
                do {
                    extra = source[src++] & 0xFF;
                    literalLength += extra;
                } while (extra == 255);
            }

            if (dst + literalLength > MAX_OUTPUT) {
                throw new IllegalStateException("LZ4 output exceeds maximum size");
            }
            out = ensure(out, dst + literalLength);
            System.arraycopy(source, src, out, dst, literalLength);
            src += literalLength;
            dst += literalLength;

            if (src >= source.length) {
                break;
            }

            int offset = (source[src] & 0xFF) | ((source[src + 1] & 0xFF) << 8);
            src += 2;
            if (offset == 0) {
                throw new IllegalStateException("Invalid LZ4 offset 0");
            }

            int matchLength = token & 0x0F;
            if (matchLength == 15) {
                int extra;
                do {
                    extra = source[src++] & 0xFF;
                    matchLength += extra;
                } while (extra == 255);
            }
            matchLength += 4;

            if (dst + matchLength > MAX_OUTPUT) {
                throw new IllegalStateException("LZ4 output exceeds maximum size");
            }
            out = ensure(out, dst + matchLength);
            int matchSrc = dst - offset;
            for (int i = 0; i < matchLength; i++) {
                out[dst++] = out[matchSrc++];
            }
        }

        return Arrays.copyOf(out, dst);
    }

    private static byte[] ensure(byte[] out, int needed) {
        if (needed <= out.length) {
            return out;
        }
        int grown = Math.max(out.length * 2, needed);
        return Arrays.copyOf(out, grown);
    }
}
