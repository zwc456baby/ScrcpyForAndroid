package org.client.scrcpy;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;

import org.client.scrcpy.decoder.VideoDecoder;
import org.client.scrcpy.model.ByteUtils;
import org.client.scrcpy.model.MediaPacket;
import org.client.scrcpy.model.VideoPacket;
import org.client.scrcpy.utils.Util;
import org.lsposed.lsparanoid.Obfuscate;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;


@Obfuscate
public class Scrcpy extends Service {

    public static final String LOCAL_IP = "127.0.0.1";
    // 本地画面转发占用的端口
    public static final int LOCAL_FORWART_PORT = 7008;

    public static final int DEFAULT_ADB_PORT = 5555;
    private String serverHost;
    private int serverPort = DEFAULT_ADB_PORT;
    private Surface surface;
    private int screenWidth;
    private int screenHeight;

    private final Queue<byte[]> event = new LinkedList<byte[]>();
    // private byte[] event = null;
    private VideoDecoder videoDecoder;
    private final AtomicBoolean updateAvailable = new AtomicBoolean(false);
    private final IBinder mBinder = new MyServiceBinder();
    private boolean first_time = true;
    private final AtomicBoolean LetServceRunning = new AtomicBoolean(true);
    private ServiceCallbacks serviceCallbacks;
    private final int[] remote_dev_resolution = new int[2];
    private boolean socket_status = false;


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setServiceCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    public void setParms(Surface NewSurface, int NewWidth, int NewHeight) {
        this.screenWidth = NewWidth;
        this.screenHeight = NewHeight;
        this.surface = NewSurface;
        videoDecoder.start();
        updateAvailable.set(true);

    }

    public void start(Surface surface, String serverAdr, int screenHeight, int screenWidth) {
        this.videoDecoder = new VideoDecoder();
        videoDecoder.start();
        String[] serverInfo = Util.getServerHostAndPort(serverAdr);
        this.serverHost = serverInfo[0];
        this.serverPort = Integer.parseInt(serverInfo[1]);

        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
        this.surface = surface;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                startConnection(serverHost, serverPort);
            }
        });
        thread.start();
    }

    public void pause() {
        if (videoDecoder != null) {
            videoDecoder.stop();
        }
    }

    public void resume() {
        if (videoDecoder != null) {
            videoDecoder.start();
        }
        updateAvailable.set(true);
    }

    public void StopService() {
        LetServceRunning.set(false);
        if (videoDecoder != null) {
            videoDecoder.stop();
        }
        stopSelf();
    }


    public boolean touchevent(MotionEvent touch_event, boolean landscape, int displayW, int displayH) {
        float remoteW;
        float remoteH;
        float realH;
        float realW;

        if (landscape) {  // 横屏的话，宽高相反
            remoteW = Math.max(remote_dev_resolution[0], remote_dev_resolution[1]);
            remoteH = Math.min(remote_dev_resolution[0], remote_dev_resolution[1]);

            realW = Math.min(remoteW, screenWidth);
            realH = realW * remoteH / remoteW;
        } else {
            remoteW = Math.min(remote_dev_resolution[0], remote_dev_resolution[1]);
            remoteH = Math.max(remote_dev_resolution[0], remote_dev_resolution[1]);
            realH = Math.min(remoteH, screenHeight);
            realW = realH * remoteW / remoteH;
        }


        int[] buf = new int[]{touch_event.getAction(), touch_event.getButtonState(), (int) (touch_event.getX() * realW / displayW), (int) (touch_event.getY() * realH / displayH)};
        final byte[] array = new byte[buf.length * 4]; // https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        if (LetServceRunning.get()) {
            event.offer(array);
        }
        // event = array;
        return true;
    }

    public int[] get_remote_device_resolution() {
        return remote_dev_resolution;
    }

    public boolean check_socket_connection() {
        return socket_status;
    }

    public void sendKeyevent(int keycode) {
        int[] buf = new int[]{keycode};

        final byte[] array = new byte[buf.length * 4];   // https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        if (LetServceRunning.get()) {
            event.offer(array);
            // event = array;
        }
    }

    private void startConnection(String ip, int port) {
        videoDecoder = new VideoDecoder();
        videoDecoder.start();
        DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;
        Socket socket = null;
        VideoPacket.StreamSettings streamSettings = null;
        boolean firstConnect = true;
        int attempts = 50;
        while (attempts > 0) {
            try {
//                Log.e("Scrcpy","Connecting to " + serverAdr);
//                socket = new Socket(serverHost, 7007);
                Log.e("Scrcpy", "Connecting to " + LOCAL_IP);
//                try {
//                    //TODO: 转发和启动需要一定的时间，如果直接连接，可能导致失败
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
                socket = new Socket(ip, port);
                Log.e("Scrcpy", "Connecting to " + LOCAL_IP + " success");

                // 能够正常进行连接，说明可能建立了 tcp 连接，需要等待数据
                // 一次等待时间为 2s ，最多等待五次，也就是 10秒
                if (firstConnect) {  // 此处有 while 循环，不能一直设置为10
                    firstConnect = false;
                    // waitResolutionCount 为 10，等待100ms 也就是共计一秒钟，设置attempts 为 5，也就是 5秒后则退出
                    attempts = 5;
                }
                dataInputStream = new DataInputStream(socket.getInputStream());
                int waitResolutionCount = 10;
                while (dataInputStream.available() <= 0 && waitResolutionCount > 0) {
                    waitResolutionCount--;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {
                    }
                }
                if (dataInputStream.available() <= 0) {
                    throw new IOException("can't read socket Resolution : " + attempts);
                }


                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                byte[] packetSize;
                attempts = 0;
                byte[] buf = new byte[16];
                dataInputStream.read(buf, 0, 16);
                for (int i = 0; i < remote_dev_resolution.length; i++) {
                    remote_dev_resolution[i] = (((int) (buf[i * 4]) << 24) & 0xFF000000) |
                            (((int) (buf[i * 4 + 1]) << 16) & 0xFF0000) |
                            (((int) (buf[i * 4 + 2]) << 8) & 0xFF00) |
                            ((int) (buf[i * 4 + 3]) & 0xFF);
                }
                if (remote_dev_resolution[0] > remote_dev_resolution[1]) {
                    first_time = false;
                    int i = remote_dev_resolution[0];
                    remote_dev_resolution[0] = remote_dev_resolution[1];
                    remote_dev_resolution[1] = i;
                }
                socket_status = true;
//                    Log.e("Remote device res", String.valueOf(remote_dev_resolution[0]+" x "+remote_dev_resolution[1]));
                while (LetServceRunning.get()) {
                    try {
                        byte[] sendevent = event.poll();
                        if (sendevent != null) {
                            try {
                                dataOutputStream.write(sendevent, 0, sendevent.length);
                            } catch (IOException e) {
                                e.printStackTrace();
                                if (serviceCallbacks != null) {
                                    serviceCallbacks.errorDisconnect();
                                }
                                LetServceRunning.set(false);
                            } finally {
                                // event = null;
                            }
                        }

                        if (dataInputStream.available() > 0) {
                            packetSize = new byte[4];
                            dataInputStream.readFully(packetSize, 0, 4);
                            int size = ByteUtils.bytesToInt(packetSize);
                            byte[] packet = new byte[size];
                            dataInputStream.readFully(packet, 0, size);
                            VideoPacket videoPacket = VideoPacket.fromArray(packet);
                            if (videoPacket.type == MediaPacket.Type.VIDEO) {
                                byte[] data = videoPacket.data;
                                if (videoPacket.flag == VideoPacket.Flag.CONFIG || updateAvailable.get()) {
                                    if (!updateAvailable.get()) {
                                        streamSettings = VideoPacket.getStreamSettings(data);
                                        if (!first_time) {
                                            if (serviceCallbacks != null) {
                                                serviceCallbacks.loadNewRotation();
                                            }
                                            while (!updateAvailable.get()) {
                                                // Waiting for new surface
                                                try {
                                                    Thread.sleep(100);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                        }
                                    }
                                    updateAvailable.set(false);
                                    //                                   first_time = false;
                                    videoDecoder.configure(surface, screenWidth, screenHeight, streamSettings.sps, streamSettings.pps);
                                } else if (videoPacket.flag == VideoPacket.Flag.END) {
                                    // need close stream
                                    Log.e("Scrcpy", "END ... ");
                                } else {
                                    videoDecoder.decodeSample(data, 0, data.length, 0, videoPacket.flag.getFlag());
                                }
                                first_time = false;
                            }

                        }
                    } catch (IOException e) {
                        Log.e("Scrcpy", "IOException: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                attempts--;
                if (attempts < 0) {
                    socket_status = false;

                    if (serviceCallbacks != null) {
                        serviceCallbacks.errorDisconnect();
                    }
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
                Log.e("Scrcpy", e.getMessage());
                Log.e("Scrcpy", "attempts--");
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(dataOutputStream != null){
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(dataInputStream != null){
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 清除事件队列
                event.clear();

            }

        }

    }

    public interface ServiceCallbacks {
        void loadNewRotation();

        void errorDisconnect();
    }

    public class MyServiceBinder extends Binder {
        public Scrcpy getService() {
            return Scrcpy.this;
        }
    }


}
