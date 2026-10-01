/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Reads and writes {@link TcpPacket}s over a stream, mirroring {@code AzNetworking}'s TCP transport. */
public final class TcpPacketCodec {

    private TcpPacketCodec() {
    }

    public static byte[] encode(int type, byte[] payload) {
        byte[] out = new byte[TcpPacket.HEADER_SIZE + payload.length];
        out[0] = 0;
        out[1] = (byte) (type >>> 8);
        out[2] = (byte) type;
        out[3] = (byte) (payload.length >>> 8);
        out[4] = (byte) payload.length;
        System.arraycopy(payload, 0, out, TcpPacket.HEADER_SIZE, payload.length);
        return out;
    }

    public static void write(OutputStream out, int type, byte[] payload) throws IOException {
        out.write(encode(type, payload));
        out.flush();
    }

    public static TcpPacket read(InputStream in) throws IOException {
        byte[] header = readFully(in, TcpPacket.HEADER_SIZE);
        int flags = header[0] & 0xFF;
        int type = ((header[1] & 0xFF) << 8) | (header[2] & 0xFF);
        int size = ((header[3] & 0xFF) << 8) | (header[4] & 0xFF);
        byte[] payload = size == 0 ? new byte[0] : readFully(in, size);
        if ((flags & TcpPacket.FLAG_COMPRESSED) != 0) {
            payload = Lz4BlockDecompressor.decompress(payload);
        }
        return new TcpPacket(type, flags, payload);
    }

    private static byte[] readFully(InputStream in, int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(buffer, offset, length - offset);
            if (read < 0) {
                throw new IOException("Connection closed while reading packet (got " + offset + "/" + length + ")");
            }
            offset += read;
        }
        return buffer;
    }
}
