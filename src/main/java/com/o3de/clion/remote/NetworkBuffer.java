/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A growable big-endian byte buffer mirroring the subset of {@code AzNetworking}'s
 * {@code NetworkInputSerializer} / {@code NetworkOutputSerializer} that is needed to speak the
 * RemoteTools protocol.
 *
 * <p>Integers are written big-endian. "Bounded" integers pick their width from the range between
 * the declared minimum and maximum, matching {@code SerializeBoundedValue}: a range that fits in
 * one byte is written as one byte, and so on. The reader derives the width from the same
 * statically-known bounds.</p>
 */
public final class NetworkBuffer {

    private byte[] data;
    private int writePos;
    private int readPos;

    public NetworkBuffer() {
        this(256);
    }

    public NetworkBuffer(int capacity) {
        data = new byte[Math.max(16, capacity)];
    }

    private NetworkBuffer(byte[] data, int writePos) {
        this.data = data;
        this.writePos = writePos;
    }

    public static NetworkBuffer wrap(byte[] content) {
        NetworkBuffer buffer = new NetworkBuffer(content.length);
        System.arraycopy(content, 0, buffer.data, 0, content.length);
        buffer.writePos = content.length;
        return buffer;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(data, writePos);
    }

    public int size() {
        return writePos;
    }

    public int remaining() {
        return writePos - readPos;
    }

    public int readPosition() {
        return readPos;
    }

    public void rewindRead() {
        readPos = 0;
    }

    private void ensure(int extra) {
        if (writePos + extra <= data.length) {
            return;
        }
        int grown = Math.max(data.length * 2, writePos + extra);
        data = Arrays.copyOf(data, grown);
    }

    public void writeU8(int value) {
        ensure(1);
        data[writePos++] = (byte) value;
    }

    public void writeU16(int value) {
        ensure(2);
        data[writePos++] = (byte) (value >>> 8);
        data[writePos++] = (byte) value;
    }

    public void writeU32(long value) {
        ensure(4);
        data[writePos++] = (byte) (value >>> 24);
        data[writePos++] = (byte) (value >>> 16);
        data[writePos++] = (byte) (value >>> 8);
        data[writePos++] = (byte) value;
    }

    public void writeU64(long value) {
        writeU32(value >>> 32);
        writeU32(value & 0xFFFFFFFFL);
    }

    public void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeBytes(byte[] bytes, int offset, int length) {
        ensure(length);
        System.arraycopy(bytes, offset, data, writePos, length);
        writePos += length;
    }

    /** Writes an integer using the smallest wire width in which {@code max - min} fits. */
    public void writeBounded(long value, long min, long max) {
        long range = max - min;
        long adjusted = value - min;
        if (range <= 0xFFL) {
            writeU8((int) adjusted);
        } else if (range <= 0xFFFFL) {
            writeU16((int) adjusted);
        } else if (range <= 0xFFFFFFFFL) {
            writeU32(adjusted);
        } else {
            writeU64(adjusted);
        }
    }

    public int readU8() {
        require(1);
        return data[readPos++] & 0xFF;
    }

    public int readU16() {
        require(2);
        int value = ((data[readPos] & 0xFF) << 8) | (data[readPos + 1] & 0xFF);
        readPos += 2;
        return value;
    }

    public long readU32() {
        require(4);
        long value = ((long) (data[readPos] & 0xFF) << 24)
                | ((long) (data[readPos + 1] & 0xFF) << 16)
                | ((long) (data[readPos + 2] & 0xFF) << 8)
                | (data[readPos + 3] & 0xFF);
        readPos += 4;
        return value;
    }

    public long readU64() {
        long high = readU32();
        long low = readU32();
        return (high << 32) | low;
    }

    public byte[] readBytes(int length) {
        require(length);
        byte[] out = Arrays.copyOfRange(data, readPos, readPos + length);
        readPos += length;
        return out;
    }

    public long readBounded(long min, long max) {
        long range = max - min;
        long adjusted;
        if (range <= 0xFFL) {
            adjusted = readU8();
        } else if (range <= 0xFFFFL) {
            adjusted = readU16();
        } else if (range <= 0xFFFFFFFFL) {
            adjusted = readU32();
        } else {
            adjusted = readU64();
        }
        return adjusted + min;
    }

    /**
     * Writes an {@code AZStd::string}. The wire form is a 32-bit big-endian length, followed by a
     * bounded length whose allowed range is derived from that same value, and finally the bytes.
     */
    public void writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeU32(bytes.length);
        writeBounded(bytes.length, 0, Math.max(bytes.length, 0));
        writeBytes(bytes);
    }

    /** Reads an {@code AZStd::string} in the format written by {@link #writeString}. */
    public String readString() {
        int length = (int) readU32();
        readBounded(0, Math.max(length, 0));
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    private void require(int length) {
        if (readPos + length > writePos) {
            throw new IllegalStateException("Buffer underrun: need " + length + " bytes, have "
                    + (writePos - readPos));
        }
    }
}
