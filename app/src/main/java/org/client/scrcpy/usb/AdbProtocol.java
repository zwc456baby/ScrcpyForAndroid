package org.client.scrcpy.usb;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Low level helpers for the ADB wire protocol.
 * <p>
 * A message consists of a 24 byte little-endian header followed by an optional
 * payload:
 * <pre>
 *   uint32 command;
 *   uint32 arg0;
 *   uint32 arg1;
 *   uint32 data_length;
 *   uint32 data_checksum;   // sum of the payload bytes
 *   uint32 magic;           // command ^ 0xffffffff
 * </pre>
 * Reference: platform/system/core/adb/protocol.txt
 */
public final class AdbProtocol {

    private AdbProtocol() {
    }

    /** Size of the message header in bytes. */
    public static final int ADB_HEADER_LENGTH = 24;

    public static final int A_SYNC = 0x434e5953;
    public static final int A_CNXN = 0x4e584e43;
    public static final int A_OPEN = 0x4e45504f;
    public static final int A_OKAY = 0x59414b4f;
    public static final int A_CLSE = 0x45534c43;
    public static final int A_WRTE = 0x45545257;
    public static final int A_AUTH = 0x48545541;

    /** AUTH packet types. */
    public static final int AUTH_TYPE_TOKEN = 1;
    public static final int AUTH_TYPE_SIGNATURE = 2;
    public static final int AUTH_TYPE_RSA_PUBLIC = 3;

    /**
     * Protocol version we advertise in the CNXN handshake.
     */
    public static final int CONNECT_VERSION = 0x01000001;

    /**
     * Maximum payload size we advertise. Kept conservative so it works across a
     * wide range of devices.
     */
    public static final int CONNECT_MAXDATA = 256 * 1024;

    /**
     * The system identity string. We deliberately advertise no features so the
     * device falls back to the legacy (raw) shell and sync behaviour, which is
     * what the rest of this app expects.
     */
    public static final byte[] CONNECT_PAYLOAD = "host::\0".getBytes();

    /** Simple additive checksum over the payload, as used by adb. */
    public static int checksum(byte[] data) {
        int sum = 0;
        if (data != null) {
            for (byte b : data) {
                sum += (b & 0xFF);
            }
        }
        return sum;
    }

    /** Build the 24 byte header for a message with the given payload. */
    public static byte[] generateHeader(int command, int arg0, int arg1, byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(command);
        buffer.putInt(arg0);
        buffer.putInt(arg1);
        buffer.putInt(payload == null ? 0 : payload.length);
        buffer.putInt(checksum(payload));
        buffer.putInt(command ^ 0xFFFFFFFF);
        return buffer.array();
    }

    /** Convenience helper to build the OPEN payload for a service string. */
    public static byte[] serviceToPayload(String service) {
        try {
            return (service + "\0").getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always available.
            throw new RuntimeException(e);
        }
    }

    /** Parsed representation of an incoming message header + payload. */
    public static final class Message {
        public final int command;
        public final int arg0;
        public final int arg1;
        public final int payloadLength;
        public final int checksum;
        public final int magic;
        public byte[] payload;

        Message(byte[] header) {
            ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            command = buffer.getInt();
            arg0 = buffer.getInt();
            arg1 = buffer.getInt();
            payloadLength = buffer.getInt();
            checksum = buffer.getInt();
            magic = buffer.getInt();
        }

        boolean isValid() {
            return magic == (command ^ 0xFFFFFFFF);
        }
    }

    public static Message parseHeader(byte[] header) {
        return new Message(header);
    }
}
