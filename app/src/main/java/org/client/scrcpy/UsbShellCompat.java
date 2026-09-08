package org.client.scrcpy;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Reflection-based bridge to the {@code org.client.scrcpy.usb} package.
 * <p>
 * The USB transport classes live on the main branch and are not yet merged
 * here, so this class must not import them directly. Every call goes through
 * reflection: if the classes are absent the shell page simply reports that
 * USB support is unavailable, and once the package is merged USB shells work
 * without any code change.
 */
public class UsbShellCompat {

    private static final String TAG = "Scrcpy";

    private static final String USB_ADB_CLASS = "org.client.scrcpy.usb.UsbAdb";
    private static final String USB_CONNECTION_CLASS = "org.client.scrcpy.usb.AdbConnection";
    private static final String USB_CRYPTO_CLASS = "org.client.scrcpy.usb.AdbCrypto";
    private static final String USB_STREAM_CLASS = "org.client.scrcpy.usb.AdbStream";

    private static final long CONNECT_TIMEOUT_MS = 30_000;
    private static final long STREAM_TIMEOUT_MS = 10_000;

    private Object connection;
    private Object stream;

    public interface Listener {
        void onOutput(String text);

        void onExited(String reason);
    }

    /** True when the usb package is present on the classpath (merged). */
    public static boolean isUsbAvailable() {
        try {
            Class.forName(USB_ADB_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** The first attached ADB-capable USB device, or null. */
    public static UsbDevice findAdbDevice(Context context) {
        try {
            Class<?> usbAdb = Class.forName(USB_ADB_CLASS);
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            Method find = usbAdb.getMethod("findAdbDevice", UsbManager.class);
            Object device = find.invoke(null, usbManager);
            return device instanceof UsbDevice ? (UsbDevice) device : null;
        } catch (Exception e) {
            Log.e(TAG, "findAdbDevice failed", e);
            return null;
        }
    }

    /** Whether the USB device has been granted permission. */
    public static boolean hasPermission(Context context, UsbDevice device) {
        if (device == null) {
            return false;
        }
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return usbManager.hasPermission(device);
    }

    /**
     * Open an interactive {@code shell:} stream over USB.
     * Returns false when the usb package is not available.
     */
    public boolean connect(Context context, UsbDevice device, Listener listener) {
        try {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            UsbDeviceConnection deviceConnection = usbManager.openDevice(device);
            if (deviceConnection == null) {
                throw new RuntimeException("Unable to open the USB device");
            }
            Class<?> cryptoClass = Class.forName(USB_CRYPTO_CLASS);
            Object crypto;
            try {
                Method loadOrGenerate = cryptoClass.getMethod("loadOrGenerate", File.class);
                crypto = loadOrGenerate.invoke(null, new File(context.getFilesDir(), ".android"));
            } catch (Exception e) {
                deviceConnection.close();
                throw new RuntimeException("Failed to prepare ADB key", e);
            }

            Class<?> connectionClass = Class.forName(USB_CONNECTION_CLASS);
            Method create = connectionClass.getMethod("create",
                    UsbDeviceConnection.class, UsbDevice.class, cryptoClass);
            connection = create.invoke(null, deviceConnection, device, crypto);
            Method connect = connectionClass.getMethod("connect", long.class);
            connect.invoke(connection, CONNECT_TIMEOUT_MS);
            Method open = connectionClass.getMethod("open", String.class, long.class);
            stream = open.invoke(connection, "shell:", STREAM_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "USB shell connect failed", e);
            close();
            if (listener != null) {
                listener.onExited("USB connect failed: " + rootMessage(e));
            }
            return false;
        }
    }

    /**
     * Read the next shell output chunk, or null when the stream is closed.
     */
    public byte[] read() {
        try {
            if (stream == null) {
                return null;
            }
            Class<?> streamClass = Class.forName(USB_STREAM_CLASS);
            Method read = streamClass.getMethod("read");
            Object data = read.invoke(stream);
            return data instanceof byte[] ? (byte[]) data : null;
        } catch (Exception e) {
            Log.e(TAG, "USB shell read failed", e);
            return null;
        }
    }

    /** Write a command (already newline-terminated) to the device shell. */
    public void send(String command) throws Exception {
        if (stream == null) {
            throw new IllegalStateException("not connected");
        }
        Class<?> streamClass = Class.forName(USB_STREAM_CLASS);
        Method write = streamClass.getMethod("write", byte[].class);
        write.invoke(stream, command.getBytes());
    }

    public void close() {
        try {
            if (stream != null) {
                Class<?> streamClass = Class.forName(USB_STREAM_CLASS);
                Method close = streamClass.getMethod("close");
                close.invoke(stream);
            }
        } catch (Exception e) {
            Log.e(TAG, "close stream failed", e);
        }
        try {
            if (connection != null) {
                Class<?> connectionClass = Class.forName(USB_CONNECTION_CLASS);
                Method close = connectionClass.getMethod("close");
                close.invoke(connection);
            }
        } catch (Exception e) {
            Log.e(TAG, "close connection failed", e);
        }
        stream = null;
        connection = null;
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage() == null ? cur.toString() : cur.getMessage();
    }
}