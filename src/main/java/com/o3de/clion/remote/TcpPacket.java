/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote;

/**
 * The 5 byte framing header used by {@code AzNetworking}'s TCP transport.
 *
 * <p>Wire layout: {@code u8 flags}, {@code u16 big-endian packet type},
 * {@code u16 big-endian payload size}. Bit 0 of the flags byte marks an LZ4 compressed payload;
 * the header itself is never compressed.</p>
 */
public final class TcpPacket {

    public static final int FLAG_COMPRESSED = 0x01;

    public static final int TYPE_INITIATE_CONNECTION = 1;
    public static final int TYPE_CONNECTION_HANDSHAKE = 2;
    public static final int TYPE_TERMINATE_CONNECTION = 3;
    public static final int TYPE_HEARTBEAT = 4;
    public static final int TYPE_FRAGMENTED = 5;
    public static final int TYPE_REMOTE_TOOLS_CONNECT = 7;
    public static final int TYPE_REMOTE_TOOLS_MESSAGE = 8;

    public static final int HEADER_SIZE = 5;

    private final int type;
    private final int flags;
    private final byte[] payload;

    public TcpPacket(int type, int flags, byte[] payload) {
        this.type = type;
        this.flags = flags;
        this.payload = payload;
    }

    public int type() {
        return type;
    }

    public int flags() {
        return flags;
    }

    public boolean compressed() {
        return (flags & FLAG_COMPRESSED) != 0;
    }

    /** The uncompressed payload. */
    public byte[] payload() {
        return payload;
    }
}
