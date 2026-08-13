package org.client.scrcpy.usb;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Reproduces {@code adb forward tcp:<local> tcp:<remote>} over a USB ADB
 * connection. Listens on {@code 127.0.0.1:localPort}; each accepted local
 * connection is bridged to a fresh ADB {@code tcp:<remotePort>} stream on the
 * device, so the rest of the app (the Scrcpy service) can keep connecting to a
 * plain localhost socket exactly as it does for the WiFi transport.
 */
public class AdbForwardBridge {

    private static final String TAG = "Scrcpy";

    private final AdbConnection connection;
    private final int localPort;
    private final int remotePort;

    private ServerSocket serverSocket;
    private volatile boolean running;
    private Thread acceptThread;

    public AdbForwardBridge(AdbConnection connection, int localPort, int remotePort) {
        this.connection = connection;
        this.localPort = localPort;
        this.remotePort = remotePort;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress("127.0.0.1", localPort));
        running = true;
        acceptThread = new Thread(this::acceptLoop, "adb-usb-forward");
        acceptThread.setDaemon(true);
        acceptThread.start();
        Log.i(TAG, "USB forward listening on 127.0.0.1:" + localPort + " -> tcp:" + remotePort);
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                handleConnection(socket);
            } catch (IOException e) {
                if (running) {
                    Log.w(TAG, "USB forward accept failed", e);
                }
                break;
            }
        }
    }

    private void handleConnection(Socket socket) {
        AdbStream stream = openRemoteStream();
        if (stream == null) {
            closeQuietly(socket);
            return;
        }
        // Socket -> device
        Thread up = new Thread(() -> {
            try {
                InputStream in = socket.getInputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    byte[] chunk = new byte[read];
                    System.arraycopy(buffer, 0, chunk, 0, read);
                    stream.write(chunk);
                }
            } catch (Exception e) {
                // Connection tore down.
            } finally {
                stream.close();
                closeQuietly(socket);
            }
        }, "adb-usb-forward-up");
        up.setDaemon(true);
        up.start();

        // Device -> socket
        Thread down = new Thread(() -> {
            try {
                OutputStream out = socket.getOutputStream();
                byte[] data;
                while ((data = stream.read()) != null) {
                    out.write(data);
                    out.flush();
                }
            } catch (Exception e) {
                // Connection tore down.
            } finally {
                stream.close();
                closeQuietly(socket);
            }
        }, "adb-usb-forward-down");
        down.setDaemon(true);
        down.start();
    }

    /**
     * Opens the device-side stream, retrying briefly because the scrcpy server
     * may not have bound its listening socket the instant we connect.
     */
    private AdbStream openRemoteStream() {
        for (int attempt = 0; attempt < 20 && running; attempt++) {
            try {
                return connection.open("tcp:" + remotePort, 5000);
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // Ignore.
        }
    }

    public void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore.
            }
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
    }
}
