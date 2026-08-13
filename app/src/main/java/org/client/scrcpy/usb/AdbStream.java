package org.client.scrcpy.usb;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A single logical ADB stream (one opened service such as {@code shell:},
 * {@code sync:} or {@code tcp:7007}). Payloads arriving from the device are
 * buffered and can be read; writes are throttled by the ADB flow control rule
 * that only one unacknowledged WRTE may be outstanding at a time.
 */
public class AdbStream {

    private final AdbConnection connection;
    private final int localId;

    private volatile int remoteId;
    private volatile boolean open;
    private volatile boolean closed;
    private boolean writeReady;

    private final Object openLock = new Object();
    private final LinkedBlockingQueue<byte[]> readQueue = new LinkedBlockingQueue<>();

    /** Sentinel pushed onto the read queue when the stream is closed. */
    private static final byte[] CLOSE_SENTINEL = new byte[0];

    AdbStream(AdbConnection connection, int localId) {
        this.connection = connection;
        this.localId = localId;
    }

    int getLocalId() {
        return localId;
    }

    int getRemoteId() {
        return remoteId;
    }

    /** Called by the connection reader when the device acknowledges OPEN. */
    void onOkay(int remoteId) {
        synchronized (openLock) {
            this.remoteId = remoteId;
            this.open = true;
            openLock.notifyAll();
        }
        synchronized (this) {
            writeReady = true;
            notifyAll();
        }
    }

    /** Called by the connection reader when a WRTE payload arrives. */
    void onPayload(byte[] data) {
        if (data != null && data.length > 0) {
            readQueue.add(data);
        }
    }

    /** Called by the connection reader when the device closes the stream. */
    void onClose() {
        closed = true;
        synchronized (openLock) {
            openLock.notifyAll();
        }
        synchronized (this) {
            notifyAll();
        }
        readQueue.add(CLOSE_SENTINEL);
    }

    /** Wait until the device acknowledges the OPEN (or refuses it). */
    void waitForOpen(long timeoutMs) throws IOException, InterruptedException {
        synchronized (openLock) {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (!open && !closed) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new IOException("Timed out opening adb stream");
                }
                openLock.wait(remaining);
            }
            if (closed) {
                throw new IOException("Stream was closed by the device before opening");
            }
        }
    }

    /**
     * Write a payload to the device, chunking it to the negotiated max size and
     * waiting for an OKAY between chunks.
     */
    public void write(byte[] payload) throws IOException, InterruptedException {
        int maxData = connection.getMaxData();
        int offset = 0;
        do {
            int length = Math.min(maxData, payload.length - offset);
            byte[] chunk;
            if (offset == 0 && length == payload.length) {
                chunk = payload;
            } else {
                chunk = new byte[length];
                System.arraycopy(payload, offset, chunk, 0, length);
            }
            sendChunk(chunk);
            offset += length;
        } while (offset < payload.length);
    }

    private void sendChunk(byte[] chunk) throws IOException, InterruptedException {
        synchronized (this) {
            while (!writeReady && !closed) {
                wait();
            }
            if (closed) {
                throw new IOException("Stream closed");
            }
            writeReady = false;
        }
        connection.sendWrite(this, chunk);
    }

    /** Called by the connection reader when the device OKAYs our WRTE. */
    void onWriteAck() {
        synchronized (this) {
            writeReady = true;
            notifyAll();
        }
    }

    /**
     * Read the next payload from the device, or {@code null} if the stream has
     * been closed and drained.
     */
    public byte[] read() throws InterruptedException, IOException {
        byte[] data = readQueue.take();
        if (data == CLOSE_SENTINEL) {
            // Put it back so subsequent reads also observe the close.
            readQueue.add(CLOSE_SENTINEL);
            return null;
        }
        return data;
    }

    /** Read with a timeout. Returns null on timeout or close. */
    public byte[] read(long timeoutMs) throws InterruptedException {
        byte[] data = readQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (data == null) {
            return null;
        }
        if (data == CLOSE_SENTINEL) {
            readQueue.add(CLOSE_SENTINEL);
            return null;
        }
        return data;
    }

    public boolean isClosed() {
        return closed;
    }

    /** Actively close the stream, notifying the device. */
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        connection.closeStream(this);
        synchronized (this) {
            notifyAll();
        }
        readQueue.add(CLOSE_SENTINEL);
    }
}
