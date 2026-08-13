package org.client.scrcpy.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An authenticated ADB connection over a USB bulk transport.
 * <p>
 * Speaks the ADB protocol directly against a device's ADB interface, obtained
 * from Android's {@code UsbManager}. This is what lets a non-rooted host phone
 * talk to a USB-attached device without shelling out to the native adb binary
 * (which cannot open raw USB devices without root).
 */
public class AdbConnection {

    private static final String TAG = "Scrcpy";

    /** ADB interface descriptor: class 0xFF, subclass 0x42, protocol 0x01. */
    private static final int ADB_CLASS = 0xff;
    private static final int ADB_SUBCLASS = 0x42;
    private static final int ADB_PROTOCOL = 0x01;

    /** Largest single bulkTransfer chunk. Keeps well under legacy limits. */
    private static final int MAX_BULK_CHUNK = 16 * 1024;

    private static final int WRITE_TIMEOUT_MS = 5000;

    private final UsbDeviceConnection deviceConnection;
    private final UsbInterface adbInterface;
    private final UsbEndpoint inEndpoint;
    private final UsbEndpoint outEndpoint;
    private final AdbCrypto crypto;

    private final Object writeLock = new Object();
    private final Object connectLock = new Object();
    private final ConcurrentHashMap<Integer, AdbStream> streams = new ConcurrentHashMap<>();
    private final AtomicInteger nextLocalId = new AtomicInteger(1);

    private volatile int maxData = AdbProtocol.CONNECT_MAXDATA;
    private volatile boolean connected;
    private volatile boolean connectionFailed;
    private volatile boolean sentSignature;
    private volatile boolean running;
    private Thread readerThread;

    private AdbConnection(UsbDeviceConnection deviceConnection, UsbInterface adbInterface,
                          UsbEndpoint inEndpoint, UsbEndpoint outEndpoint, AdbCrypto crypto) {
        this.deviceConnection = deviceConnection;
        this.adbInterface = adbInterface;
        this.inEndpoint = inEndpoint;
        this.outEndpoint = outEndpoint;
        this.crypto = crypto;
    }

    /** Locate the ADB interface on a USB device, or null if none is present. */
    public static UsbInterface findAdbInterface(UsbDevice device) {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == ADB_CLASS
                    && intf.getInterfaceSubclass() == ADB_SUBCLASS
                    && intf.getInterfaceProtocol() == ADB_PROTOCOL) {
                return intf;
            }
        }
        return null;
    }

    /**
     * Build a connection around an already-opened {@link UsbDeviceConnection}.
     * The ADB interface is claimed here; call {@link #connect(long)} to perform
     * the handshake.
     */
    public static AdbConnection create(UsbDeviceConnection deviceConnection, UsbDevice device,
                                       AdbCrypto crypto) throws IOException {
        UsbInterface adbInterface = findAdbInterface(device);
        if (adbInterface == null) {
            throw new IOException("Device has no ADB interface (is USB debugging enabled?)");
        }
        UsbEndpoint in = null;
        UsbEndpoint out = null;
        for (int i = 0; i < adbInterface.getEndpointCount(); i++) {
            UsbEndpoint endpoint = adbInterface.getEndpoint(i);
            if (endpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
                continue;
            }
            if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                in = endpoint;
            } else {
                out = endpoint;
            }
        }
        if (in == null || out == null) {
            throw new IOException("ADB interface is missing bulk endpoints");
        }
        if (!deviceConnection.claimInterface(adbInterface, true)) {
            throw new IOException("Failed to claim ADB interface");
        }
        return new AdbConnection(deviceConnection, adbInterface, in, out, crypto);
    }

    public int getMaxData() {
        return maxData;
    }

    /**
     * Perform the CNXN/AUTH handshake and block until the device accepts the
     * connection. The device may show an "Allow USB debugging?" prompt on the
     * first connection, which is why the timeout should be generous.
     */
    public void connect(long timeoutMs) throws IOException, InterruptedException {
        running = true;
        readerThread = new Thread(this::readLoop, "adb-usb-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        writeMessage(AdbProtocol.A_CNXN, AdbProtocol.CONNECT_VERSION,
                AdbProtocol.CONNECT_MAXDATA, AdbProtocol.CONNECT_PAYLOAD);

        synchronized (connectLock) {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!connected && !connectionFailed) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
                connectLock.wait(remaining);
            }
        }
        if (!connected) {
            throw new IOException(connectionFailed
                    ? "USB ADB authentication was rejected"
                    : "Timed out waiting for the device to authorize USB debugging");
        }
    }

    /** Open a new ADB stream for the given service string. */
    public AdbStream open(String service, long timeoutMs) throws IOException, InterruptedException {
        if (!connected) {
            throw new IOException("Not connected");
        }
        int localId = nextLocalId.getAndIncrement();
        AdbStream stream = new AdbStream(this, localId);
        streams.put(localId, stream);
        try {
            writeMessage(AdbProtocol.A_OPEN, localId, 0, AdbProtocol.serviceToPayload(service));
            stream.waitForOpen(timeoutMs);
        } catch (IOException | InterruptedException e) {
            streams.remove(localId);
            throw e;
        }
        return stream;
    }

    void sendWrite(AdbStream stream, byte[] payload) throws IOException {
        writeMessage(AdbProtocol.A_WRTE, stream.getLocalId(), stream.getRemoteId(), payload);
    }

    void closeStream(AdbStream stream) {
        streams.remove(stream.getLocalId());
        try {
            writeMessage(AdbProtocol.A_CLSE, stream.getLocalId(), stream.getRemoteId(), null);
        } catch (IOException e) {
            // Best effort.
        }
    }

    private void readLoop() {
        try {
            byte[] header = new byte[AdbProtocol.ADB_HEADER_LENGTH];
            while (running) {
                readFully(header, AdbProtocol.ADB_HEADER_LENGTH);
                AdbProtocol.Message message = AdbProtocol.parseHeader(header);
                if (!message.isValid()) {
                    Log.w(TAG, "Discarding malformed adb message");
                    continue;
                }
                if (message.payloadLength > 0) {
                    message.payload = new byte[message.payloadLength];
                    readFully(message.payload, message.payloadLength);
                }
                handleMessage(message);
            }
        } catch (Exception e) {
            if (running) {
                Log.w(TAG, "USB ADB reader stopped", e);
            }
            failConnection();
        }
    }

    private void handleMessage(AdbProtocol.Message message) throws IOException, InterruptedException {
        switch (message.command) {
            case AdbProtocol.A_CNXN:
                maxData = message.arg1 > 0 ? message.arg1 : AdbProtocol.CONNECT_MAXDATA;
                synchronized (connectLock) {
                    connected = true;
                    connectLock.notifyAll();
                }
                break;
            case AdbProtocol.A_AUTH:
                if (message.arg0 == AdbProtocol.AUTH_TYPE_TOKEN) {
                    try {
                        if (!sentSignature) {
                            sentSignature = true;
                            byte[] signature = crypto.signToken(message.payload);
                            writeMessage(AdbProtocol.A_AUTH, AdbProtocol.AUTH_TYPE_SIGNATURE, 0,
                                    signature);
                        } else {
                            writeMessage(AdbProtocol.A_AUTH, AdbProtocol.AUTH_TYPE_RSA_PUBLIC, 0,
                                    crypto.getPublicKeyPayload());
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "USB ADB auth failed", e);
                        failConnection();
                    }
                }
                break;
            case AdbProtocol.A_OKAY: {
                AdbStream stream = streams.get(message.arg1);
                if (stream != null) {
                    if (stream.getRemoteId() == 0) {
                        stream.onOkay(message.arg0);
                    } else {
                        stream.onWriteAck();
                    }
                }
                break;
            }
            case AdbProtocol.A_WRTE: {
                AdbStream stream = streams.get(message.arg1);
                if (stream != null) {
                    stream.onPayload(message.payload);
                }
                // Acknowledge so the device keeps sending.
                writeMessage(AdbProtocol.A_OKAY, message.arg1, message.arg0, null);
                break;
            }
            case AdbProtocol.A_CLSE: {
                AdbStream stream = streams.remove(message.arg1);
                if (stream != null) {
                    stream.onClose();
                }
                break;
            }
            default:
                break;
        }
    }

    private void failConnection() {
        connectionFailed = true;
        synchronized (connectLock) {
            connectLock.notifyAll();
        }
        for (AdbStream stream : streams.values()) {
            stream.onClose();
        }
        streams.clear();
    }

    private void writeMessage(int command, int arg0, int arg1, byte[] payload) throws IOException {
        byte[] header = AdbProtocol.generateHeader(command, arg0, arg1, payload);
        synchronized (writeLock) {
            bulkWrite(header, header.length);
            if (payload != null && payload.length > 0) {
                bulkWrite(payload, payload.length);
            }
        }
    }

    private void bulkWrite(byte[] data, int length) throws IOException {
        int offset = 0;
        while (offset < length) {
            int chunk = Math.min(MAX_BULK_CHUNK, length - offset);
            int sent = deviceConnection.bulkTransfer(outEndpoint, data, offset, chunk,
                    WRITE_TIMEOUT_MS);
            if (sent < 0) {
                throw new IOException("USB bulk write failed");
            }
            offset += sent;
        }
    }

    private void readFully(byte[] buffer, int length) throws IOException {
        int offset = 0;
        while (offset < length) {
            int want = Math.min(MAX_BULK_CHUNK, length - offset);
            // Zero timeout blocks until data is available.
            int read = deviceConnection.bulkTransfer(inEndpoint, buffer, offset, want, 0);
            if (read < 0) {
                throw new IOException("USB bulk read failed");
            }
            offset += read;
        }
    }

    /** Tear down the connection and release USB resources. */
    public void close() {
        running = false;
        for (AdbStream stream : streams.values()) {
            stream.onClose();
        }
        streams.clear();
        if (readerThread != null) {
            readerThread.interrupt();
        }
        try {
            deviceConnection.releaseInterface(adbInterface);
        } catch (Exception e) {
            // Ignore.
        }
        deviceConnection.close();
    }
}
