/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote.script;

import com.o3de.clion.remote.RemoteToolsHost;
import com.o3de.clion.remote.objectstream.ObjectStreamReader;
import com.o3de.clion.remote.objectstream.OsElement;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Drives the engine's {@code ScriptDebugAgent}: enumerates script contexts, attaches to one and
 * then pulls the reflected classes, EBuses and globals.
 */
public final class ScriptDebugClient implements Closeable, RemoteToolsHost.Listener {

    public interface Listener {
        void onStatus(String status);

        void onData(LuaReferenceData data);

        void onError(String message);
    }

    private static final long CONNECT_TIMEOUT_MS = 60_000;
    private static final long RESPONSE_TIMEOUT_MS = 30_000;

    private final RemoteToolsHost host = new RemoteToolsHost(this);
    private final BlockingQueue<byte[]> inbox = new LinkedBlockingQueue<>();
    private final Listener listener;

    private volatile String stringTypeId = ScriptDebugProtocol.UUID_STRING;
    private volatile LuaReferenceData lastData;

    public ScriptDebugClient(Listener listener) {
        this.listener = listener;
    }

    public RemoteToolsHost host() {
        return host;
    }

    public LuaReferenceData getLastData() {
        return lastData;
    }

    /** Starts listening and refreshes the reference data, blocking until done. */
    public LuaReferenceData refresh() throws IOException, InterruptedException {
        host.start();
        listener.onStatus("Waiting for an O3DE Editor or Game on port " + RemoteToolsHost.PORT + "...");
        awaitTarget();
        listener.onStatus("Connected to " + host.getSelectedDisplayName() + ". Enumerating script contexts...");

        byte[] contextsMessage = exchange(ScriptDebugProtocol.ENUM_CONTEXTS, null);
        ScriptDebugParser.Contexts contexts = ScriptDebugParser.parseContexts(contextsMessage);
        if (contexts.stringTypeId() != null) {
            stringTypeId = contexts.stringTypeId();
        }
        List<String> names = contexts.names();
        if (names.isEmpty()) {
            throw new IOException("The target reported no script contexts");
        }

        String context = names.get(0);
        listener.onStatus("Attaching to script context '" + context + "'...");
        byte[] ackMessage = exchange(ScriptDebugProtocol.ATTACH_DEBUGGER, context);
        if (ackMessage != null) {
            OsElement ack = ObjectStreamReader.readRoot(ackMessage);
            OsElement ackCode = ack == null ? null : ack.field("ackCode");
            if (ackCode != null && intOf(ackCode) != ScriptDebugProtocol.ACK_ACK) {
                throw new IOException("The target refused the debugger attach");
            }
        }

        LuaReferenceData.Builder builder = LuaReferenceData.builder();
        builder.setStringTypeId(stringTypeId);
        builder.contexts().addAll(names);

        listener.onStatus("Reading registered classes...");
        ScriptDebugParser.parseClasses(exchange(ScriptDebugProtocol.ENUM_REGISTERED_CLASSES, null), builder);
        listener.onStatus("Reading registered EBuses...");
        ScriptDebugParser.parseEbus(exchange(ScriptDebugProtocol.ENUM_REGISTERED_EBUSES, null), builder);
        listener.onStatus("Reading registered globals...");
        ScriptDebugParser.parseGlobals(exchange(ScriptDebugProtocol.ENUM_REGISTERED_GLOBALS, null), builder);

        LuaReferenceData data = builder.build();
        lastData = data;
        listener.onData(data);
        listener.onStatus("Loaded " + data.classes().size() + " classes, " + data.ebuses().size()
                + " EBuses, " + data.globalMethods().size() + " global functions.");
        return data;
    }

    private void awaitTarget() throws InterruptedException, IOException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CONNECT_TIMEOUT_MS);
        while (!host.isTargetConnected()) {
            if (System.nanoTime() > deadline) {
                throw new IOException("Timed out waiting for an O3DE target on port " + RemoteToolsHost.PORT);
            }
            Thread.sleep(100);
        }
    }

    private byte[] exchange(int requestCode, String context) throws IOException, InterruptedException {
        inbox.clear();
        host.sendMessage(ScriptDebugProtocol.request(requestCode, context));
        return inbox.poll(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private static int intOf(OsElement element) {
        byte[] value = element.value();
        int result = 0;
        for (byte b : value) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    @Override
    public void onEndpointConnected(String displayName) {
        listener.onStatus("O3DE target connected: " + displayName);
    }

    @Override
    public void onEndpointDisconnected(String reason) {
        listener.onStatus("O3DE target disconnected: " + reason);
    }

    @Override
    public void onMessage(byte[] objectStream) {
        inbox.offer(objectStream);
    }

    @Override
    public void onError(Exception error) {
        listener.onError(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
    }

    @Override
    public void close() {
        host.close();
    }
}
