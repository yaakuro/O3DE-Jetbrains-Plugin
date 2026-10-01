/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * A minimal O3DE RemoteTools host.
 *
 * <p>O3DE's Editor and Game applications register themselves as RemoteTools <em>clients</em> and
 * dial out to a host listening on {@code 6777} (the Lua Editor does exactly this). Once connected
 * they push serialized {@code AZ::ObjectStream} blobs wrapped in {@code RemoteToolsMessage}
 * packets; this class accepts those connections, reassembles fragmented messages and hands the
 * decoded blob to a {@link Listener}.</p>
 *
 * <p>The host must mark one endpoint as the "selected target": the engine only routes a message to
 * the host when the sender's connection id matches the selected one.</p>
 */
public final class RemoteToolsHost implements Closeable {

    public static final int PORT = 6777;
    public static final int KEY = 0x3a1e3b6a;
    public static final int MAX_MESSAGE_BUFFER = 16000;

    public interface Listener {
        void onEndpointConnected(String displayName);

        void onEndpointDisconnected(String reason);

        void onMessage(byte[] objectStream);

        default void onError(Exception error) {
        }
    }

    private static final class Connection {
        final Socket socket;
        final InputStream in;
        final OutputStream out;
        final java.io.ByteArrayOutputStream pending = new java.io.ByteArrayOutputStream();
        long expectedSize = -1;
        String displayName = "";
        boolean connected;

        Connection(Socket socket) throws IOException {
            this.socket = socket;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }
    }

    private final Listener listener;
    private final List<Connection> connections = new ArrayList<>();

    private volatile ServerSocket server;
    private volatile Connection selected;
    private volatile boolean closed;
    private volatile String lastError;

    public RemoteToolsHost(Listener listener) {
        this.listener = listener;
    }

    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }
        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(PORT));
        server = socket;
        Thread thread = new Thread(this::acceptLoop, "O3DE-RemoteToolsHost");
        thread.setDaemon(true);
        thread.start();
    }

    public boolean isRunning() {
        return server != null && !closed;
    }

    public boolean isTargetConnected() {
        Connection connection = selected;
        return connection != null && connection.connected;
    }

    public String getLastError() {
        return lastError;
    }

    public String getSelectedDisplayName() {
        Connection connection = selected;
        return connection == null ? null : connection.displayName;
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (server != null) {
            try {
                server.close();
            } catch (IOException ignored) {
            }
            server = null;
        }
        for (Connection connection : new ArrayList<>(connections)) {
            closeQuietly(connection.socket);
        }
        connections.clear();
        selected = null;
    }

    /** Sends a serialized {@code AZ::ObjectStream} blob to the selected target, fragmenting it. */
    public void sendMessage(byte[] objectStream) throws IOException {
        Connection target = selected;
        if (target == null || !target.connected) {
            throw new IOException("No O3DE target is connected");
        }
        synchronized (target.out) {
            int total = objectStream.length;
            int offset = 0;
            while (offset < total) {
                int length = Math.min(MAX_MESSAGE_BUFFER, total - offset);
                NetworkBuffer buffer = new NetworkBuffer(length + 16);
                buffer.writeU16(length);
                buffer.writeBounded(length, 0, MAX_MESSAGE_BUFFER);
                buffer.writeBytes(objectStream, offset, length);
                buffer.writeU32(total);
                buffer.writeU32(KEY);
                TcpPacketCodec.write(target.out, TcpPacket.TYPE_REMOTE_TOOLS_MESSAGE, buffer.toByteArray());
                offset += length;
            }
        }
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);
                Connection connection = new Connection(socket);
                synchronized (this) {
                    connections.add(connection);
                }
                Thread thread = new Thread(() -> readLoop(connection), "O3DE-RemoteToolsClient");
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
                if (!closed) {
                    lastError = e.getMessage();
                    listener.onError(e);
                }
            }
        }
    }

    private void readLoop(Connection connection) {
        String reason = "Connection closed";
        try {
            while (!closed) {
                TcpPacket packet = TcpPacketCodec.read(connection.in);
                handle(connection, packet);
            }
        } catch (Exception e) {
            reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        } finally {
            synchronized (this) {
                connections.remove(connection);
                if (selected == connection) {
                    selected = null;
                    connection.connected = false;
                    listener.onEndpointDisconnected(reason);
                } else {
                    connection.connected = false;
                }
            }
            closeQuietly(connection.socket);
        }
    }

    private void handle(Connection connection, TcpPacket packet) {
        switch (packet.type()) {
            case TcpPacket.TYPE_REMOTE_TOOLS_CONNECT -> handleConnect(connection, packet.payload());
            case TcpPacket.TYPE_REMOTE_TOOLS_MESSAGE -> handleMessage(connection, packet.payload());
            case TcpPacket.TYPE_TERMINATE_CONNECTION -> closeQuietly(connection.socket);
            default -> {
                // InitiateConnection / ConnectionHandshake / Heartbeat / Fragmented need no reply.
            }
        }
    }

    private void handleConnect(Connection connection, byte[] payload) {
        NetworkBuffer buffer = NetworkBuffer.wrap(payload);
        buffer.readU32();
        buffer.readU32();
        connection.displayName = buffer.readString();
        connection.connected = true;
        synchronized (this) {
            selected = connection;
        }
        listener.onEndpointConnected(connection.displayName);
    }

    private void handleMessage(Connection connection, byte[] payload) {
        if (connection != selected) {
            return;
        }
        NetworkBuffer buffer = NetworkBuffer.wrap(payload);
        int chunkSize = buffer.readU16();
        buffer.readBounded(0, MAX_MESSAGE_BUFFER);
        byte[] chunk = buffer.readBytes(chunkSize);
        long totalSize = buffer.readU32();
        buffer.readU32();

        if (totalSize != connection.expectedSize) {
            connection.pending.reset();
            connection.expectedSize = totalSize;
        }
        connection.pending.write(chunk, 0, chunk.length);

        if (connection.pending.size() >= connection.expectedSize) {
            byte[] complete = connection.pending.toByteArray();
            connection.pending.reset();
            connection.expectedSize = -1;
            listener.onMessage(complete);
        }
    }

    private static void closeQuietly(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
