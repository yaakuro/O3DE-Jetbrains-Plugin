/*
 * Copyright (c) 2026 Cengiz Terzibas
 *
 * SPDX-License-Identifier: Apache-2.0 OR MIT
 */

package com.o3de.clion.remote.script;

import com.o3de.clion.remote.AzCrc32;
import com.o3de.clion.remote.objectstream.ObjectStreamWriter;

import java.nio.charset.StandardCharsets;

/** Constants and message builders for the engine's {@code ScriptDebugAgent} protocol. */
public final class ScriptDebugProtocol {

    public static final String UUID_REQUEST = "{2137E01A-F2AE-4137-A17E-6B82F3B7E4DE}";
    public static final String UUID_ACK = "{0CA1671A-BAFD-499C-B2CD-7B7E3DD5E2A8}";
    public static final String UUID_REMOTE_TOOLS_MESSAGE = "{8512328C-949D-4F0C-B48D-77C26C207443}";
    public static final String UUID_ENUM_CONTEXTS_RESULT = "{8CE74569-9B7D-4993-AFE8-38BB8CE419F5}";
    public static final String UUID_REGISTERED_GLOBALS_RESULT = "{CEE4E889-0249-4D59-9D56-CD4BD159E411}";
    public static final String UUID_REGISTERED_CLASSES_RESULT = "{7DF455AB-9AB1-4A95-B906-5DB1D1087EBB}";
    public static final String UUID_REGISTERED_EBUSES_RESULT = "{D2B5D77C-09F3-476D-A611-49B0A1B9EDFB}";

    /**
     * The aggregated {@code AZStd::string} type id (the string template id suffixed with the type
     * ids of its template arguments). Taken from a binary dump produced by the engine itself.
     */
    public static final String UUID_STRING = "{03AAAB3F-5C47-5A66-9EBC-D5FA4DB353C9}";

    /** {@code AZ::u64} value stamped into {@code RemoteToolsMessage::m_msgId} by the constructor. */
    public static final long MSG_ID_SCRIPT_DEBUG_AGENT = AzCrc32.ofLower("ScriptDebugAgent") & 0xFFFFFFFFL;

    /** Name of the reflected base class element under a derived type. */
    public static final String BASE_CLASS_NAME = "BaseClass1";

    public static final int ENUM_CONTEXTS = AzCrc32.ofLower("EnumContexts");
    public static final int ATTACH_DEBUGGER = AzCrc32.ofLower("AttachDebugger");
    public static final int ENUM_REGISTERED_GLOBALS = AzCrc32.ofLower("EnumRegisteredGlobals");
    public static final int ENUM_REGISTERED_CLASSES = AzCrc32.ofLower("EnumRegisteredClasses");
    public static final int ENUM_REGISTERED_EBUSES = AzCrc32.ofLower("EnumRegisteredEBuses");

    public static final int ACK_ACK = AzCrc32.ofLower("Ack");
    public static final int ACK_ILLEGAL_OPERATION = AzCrc32.ofLower("IllegalOperation");

    private ScriptDebugProtocol() {
    }

    /** Builds a {@code ScriptDebugRequest} with an empty context. */
    public static byte[] request(int requestCode) {
        return request(requestCode, "");
    }

    /** Builds a {@code ScriptDebugRequest} carrying a script context name. */
    public static byte[] request(int requestCode, String context) {
        String safeContext = context == null ? "" : context;
        ObjectStreamWriter writer = new ObjectStreamWriter();
        ObjectStreamWriter.Element root = writer.begin(null, UUID_REQUEST);

        ObjectStreamWriter.Element base = root.beginChild(BASE_CLASS_NAME, UUID_REMOTE_TOOLS_MESSAGE);
        base.u64("MsgId", MSG_ID_SCRIPT_DEBUG_AGENT);
        base.close();

        root.u32("request", requestCode & 0xFFFFFFFFL);
        root.bytes("context", UUID_STRING, safeContext.getBytes(StandardCharsets.UTF_8));
        root.close();
        return writer.finish();
    }
}
