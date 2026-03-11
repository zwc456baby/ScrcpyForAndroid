package org.server.scrcpy;


import android.util.Log;

import org.server.scrcpy.model.ByteUtils;
import org.server.scrcpy.model.ControlPacket;
import org.server.scrcpy.model.MediaPacket;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public final class DroidConnection implements Closeable {


    private static Socket socket = null;
    private OutputStream outputStream;
    private DataInputStream inputStream;

    private DroidConnection(Socket socket) throws IOException {
        this.socket = socket;

        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = socket.getOutputStream();
    }


    private static Socket listenAndAccept() throws IOException {
        ServerSocket serverSocket = new ServerSocket(7007);
        Socket sock = null;
        try {
            sock = serverSocket.accept();
        } finally {
            serverSocket.close();
        }
        return sock;
    }

    public static DroidConnection open(String ip) throws IOException {

        socket = listenAndAccept();
        DroidConnection connection = null;
//        if (socket.getInetAddress().toString().equals(ip)) {
//            connection = new DroidConnection(socket);
//        }
        if (!socket.getInetAddress().toString().equals(ip)) {
            Ln.w("socket connect address != " + ip);
        }
        // 判断 socket 有一个正确的地址
        if (!socket.getInetAddress().toString().isEmpty()) {
            connection = new DroidConnection(socket);
        }
        return connection;
    }

    public void close() throws IOException {
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }


    /**
     * TODO 需要根据原版 scrcpy 进行改造消息传送，目前仅支持 触控消息
     *
     * @return
     * @throws IOException
     */
    public MediaPacket NewReceiveEvent() throws IOException {

        byte[] packetSize = new byte[4];
        inputStream.readFully(packetSize, 0, packetSize.length);

        int size = ByteUtils.bytesToInt(packetSize);

        if (size > 4 * 1024 * 1024) {  // 如果单个数据包大于 4m ，直接断开连接
            throw new EOFException("Event controller socket closed");
        }
        byte[] packet = new byte[size];
        inputStream.readFully(packet, 0, size);

        MediaPacket.Type type = MediaPacket.Type.getType(packet[0]);
        switch (type) {
            case CONTROL:
                return new ControlPacket().fromArray(packet);
            case COMMAND:
                // TODO 实现额外的命令或方法
                break;
        }


//        byte[] buf = new byte[20];
//        int n = inputStream.read(buf, 0, 20);
//        if (n == -1) {
//            throw new EOFException("Event controller socket closed");
//        }
//
//        final int[] array = new int[buf.length / 4];
//        for (int i = 0; i < array.length; i++)
//            array[i] = (((int) (buf[i * 4]) << 24) & 0xFF000000) |
//                    (((int) (buf[i * 4 + 1]) << 16) & 0xFF0000) |
//                    (((int) (buf[i * 4 + 2]) << 8) & 0xFF00) |
//                    ((int) (buf[i * 4 + 3]) & 0xFF);
//        return array;


        return null;

    }

}

