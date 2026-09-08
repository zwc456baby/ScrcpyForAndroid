package org.client.scrcpy;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;

import org.client.scrcpy.decoder.AudioDecoder;
import org.client.scrcpy.decoder.VideoDecoder;
import org.client.scrcpy.model.AudioPacket;
import org.client.scrcpy.model.ByteUtils;
import org.client.scrcpy.model.CommandPacket;
import org.client.scrcpy.model.ControlPacket;
import org.client.scrcpy.model.MediaPacket;
import org.client.scrcpy.model.VideoPacket;
import org.client.scrcpy.utils.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;


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
    private AudioDecoder audioDecoder;
    private final AtomicBoolean updateAvailable = new AtomicBoolean(false);
    private final IBinder mBinder = new MyServiceBinder();
    private boolean first_time = true;

    private final AtomicBoolean LetServceRunning = new AtomicBoolean(true);
    private ServiceCallbacks serviceCallbacks;
    private final int[] remote_dev_resolution = new int[2];
    private final int[] remote_video_resolution = new int[2];
    private boolean socket_status = false;

    private String videoMimeType = "video/avc";

    private DataInputStream socketInputStream = null;
    private DataOutputStream socketOutputStream = null;

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
        if (NewSurface != null) {
            this.surface = NewSurface;
        }

        if (videoDecoder != null) {
            videoDecoder.start();
        }
        if (audioDecoder != null) {
            audioDecoder.start();
        }

        updateAvailable.set(true);
    }

    private boolean isSurfaceReady(Surface displaySurface) {
        if (displaySurface == null) {
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return displaySurface.isValid();
        }
        return true;
    }

    private boolean tryConfigureVideoDecoder(VideoPacket.StreamSettings streamSettings) {
        if (streamSettings == null || videoDecoder == null) {
            return false;
        }
        int decodeW = remote_video_resolution[0] > 0
                ? remote_video_resolution[0]
                : (remote_dev_resolution[0] > 0 ? remote_dev_resolution[0] : screenWidth);
        int decodeH = remote_video_resolution[1] > 0
                ? remote_video_resolution[1]
                : (remote_dev_resolution[1] > 0 ? remote_dev_resolution[1] : screenHeight);
        if (!isSurfaceReady(surface)) {
            Log.e("Scrcpy", "Skipping video configure: surface not ready");
            return false;
        }
        Log.i("Scrcpy", "Decoder configure " + decodeW + "x" + decodeH);
        videoDecoder.configure(surface, decodeW, decodeH, streamSettings.sps, streamSettings.pps, videoMimeType);
        return true;
    }

    public void start(Surface surface, String serverAdr, int screenHeight, int screenWidth, int delay, boolean enableAudio) {
        start(surface, serverAdr, screenHeight, screenWidth, delay, enableAudio, Options.CODEC_H264);
    }

    public void start(Surface surface, String serverAdr, int screenHeight, int screenWidth, int delay, boolean enableAudio, String videoCodec) {
        String[] serverInfo = Util.getServerHostAndPort(serverAdr);
        this.serverHost = serverInfo[0];
        this.serverPort = Integer.parseInt(serverInfo[1]);

        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
        this.surface = surface;
        this.videoMimeType = Options.CODEC_H265.equalsIgnoreCase(videoCodec) ? "video/hevc" : "video/avc";
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                startConnection(serverHost, serverPort, delay, enableAudio);
            }
        });
        thread.start();
    }

    public void pause() {
        if (videoDecoder != null) {
            videoDecoder.stop();
        }

        if (audioDecoder != null) {
            audioDecoder.stop();
        }
    }

    public void resume() {
        if (videoDecoder != null) {
            videoDecoder.start();
        }
        if (audioDecoder != null) {
            audioDecoder.start();
        }
        updateAvailable.set(true);

        try {  // 请求关键帧, 避免花屏
            requestNewKeyFrame();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void StopService() {
        LetServceRunning.set(false);
        if (videoDecoder != null) {
            videoDecoder.stop();
        }
        if (audioDecoder != null) {
            audioDecoder.stop();
        }
        stopSelf();
    }


    public boolean touchevent(MotionEvent touch_event, boolean landscape, int displayW, int displayH) {
        if (displayW <= 0 || displayH <= 0) {
            return true;
        }

        int[] remote = getOrientedRemoteSize(landscape);
        float remoteW = remote[0];
        float remoteH = remote[1];
        if (remoteW <= 0 || remoteH <= 0) {
            return true;
        }

        int actionMasked = touch_event.getActionMasked();
        int actionIndex = touch_event.getActionIndex();
        int pointerId = touch_event.getPointerId(actionIndex);

        if (actionMasked == MotionEvent.ACTION_MOVE) {
            for (int i = 0; i < touch_event.getPointerCount(); i++) {
                int currentPointerId = touch_event.getPointerId(i);
                int[] mapped = mapTouchToRemote(touch_event.getX(i), touch_event.getY(i),
                        displayW, displayH, remoteW, remoteH);
                sendTouchEvent(touch_event.getAction(), touch_event.getButtonState(),
                        mapped[0], mapped[1], currentPointerId);
            }
        } else {
            int[] mapped = mapTouchToRemote(touch_event.getX(), touch_event.getY(),
                    displayW, displayH, remoteW, remoteH);
            if (actionMasked == MotionEvent.ACTION_DOWN) {
                Log.i("Scrcpy", "touch DOWN " + mapped[0] + "," + mapped[1]
                        + " surface=" + displayW + "x" + displayH
                        + " remote=" + (int) remoteW + "x" + (int) remoteH
                        + " video=" + remote_video_resolution[0] + "x" + remote_video_resolution[1]);
            }
            sendTouchEvent(touch_event.getAction(), touch_event.getButtonState(),
                    mapped[0], mapped[1], pointerId);
        }
        return true;
    }

    /**
     * Mappa il tocco dal SurfaceView ai pixel del device remoto, con letterbox
     * se l'aspect del remoto non coincide con quello del client.
     */
    private static int[] mapTouchToRemote(float touchX, float touchY,
                                          int displayW, int displayH,
                                          float remoteW, float remoteH) {
        float scale = Math.min(displayW / remoteW, displayH / remoteH);
        float contentW = remoteW * scale;
        float contentH = remoteH * scale;
        if (contentW <= 0f || contentH <= 0f) {
            return new int[]{0, 0};
        }
        float offX = (displayW - contentW) / 2f;
        float offY = (displayH - contentH) / 2f;
        int x = Math.round((touchX - offX) * remoteW / contentW);
        int y = Math.round((touchY - offY) * remoteH / contentH);
        if (x < 0) {
            x = 0;
        } else if (x >= (int) remoteW) {
            x = (int) remoteW - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y >= (int) remoteH) {
            y = (int) remoteH - 1;
        }
        return new int[]{x, y};
    }

    private void sendTouchEvent(int action, int buttonState, int x, int y, int pointerId) {
        // 为支持多点触控，将 pointid 添加到最末尾
        // TODO : 后续需要改造 event 传输方式
        int[] buf = new int[]{action, buttonState, x, y, pointerId};
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
    }

    public int[] get_remote_device_resolution() {
        return remote_dev_resolution;
    }

    public int[] getOrientedRemoteSize(boolean landscape) {
        int w = remote_dev_resolution[0];
        int h = remote_dev_resolution[1];
        if (w > 0 && h > 0) {
            return new int[]{w, h};
        }
        if (landscape) {
            return new int[]{Math.max(w, h), Math.min(w, h)};
        }
        return new int[]{Math.min(w, h), Math.max(w, h)};
    }

    public void applyRemoteRotation() {
        swapPair(remote_dev_resolution);
        swapPair(remote_video_resolution);
        Log.i("Scrcpy", "Remote rotation device=" + remote_dev_resolution[0] + "x" + remote_dev_resolution[1]
                + " video=" + remote_video_resolution[0] + "x" + remote_video_resolution[1]);
    }

    private static void swapPair(int[] values) {
        int tmp = values[0];
        values[0] = values[1];
        values[1] = tmp;
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

    private void startConnection(String ip, int port, int delay, boolean enableAudio) {

        videoDecoder = new VideoDecoder();
        videoDecoder.start();

        if (enableAudio){
            audioDecoder = new AudioDecoder();
            audioDecoder.start();
        }

        DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;
        Socket socket = null;
        boolean firstConnect = true;
        int attempts = 50;
        while (attempts > 0 && LetServceRunning.get()) {
            try {
                Log.e("Scrcpy", "Connecting to " + LOCAL_IP);
                // socket = new Socket(ip, port);
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 5000); //设置超时5000毫秒
                if (!LetServceRunning.get()) {
                    return;
                }

                Log.e("Scrcpy", "Connecting to " + LOCAL_IP + " success");

                // Il server accetta una sola connessione: non chiudere il primo socket troppo presto.
                // ADB forward può risultare "connected" prima che app_process sia in listen.
                int waitResolutionCount;
                if (firstConnect) {
                    firstConnect = false;
                    attempts = 8;
                    waitResolutionCount = 50; // 5s sul primo tentativo
                } else {
                    waitResolutionCount = 20; // 2s sui retry
                }
                dataInputStream = new DataInputStream(socket.getInputStream());
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
                attempts = 0;
                byte[] buf = new byte[16];
                dataInputStream.readFully(buf, 0, 16);
                int[] handshake = new int[4];
                for (int i = 0; i < handshake.length; i++) {
                    handshake[i] = (((int) (buf[i * 4]) << 24) & 0xFF000000) |
                            (((int) (buf[i * 4 + 1]) << 16) & 0xFF0000) |
                            (((int) (buf[i * 4 + 2]) << 8) & 0xFF00) |
                            ((int) (buf[i * 4 + 3]) & 0xFF);
                }
                remote_dev_resolution[0] = handshake[0];
                remote_dev_resolution[1] = handshake[1];
                remote_video_resolution[0] = handshake[2];
                remote_video_resolution[1] = handshake[3];
                Log.i("Scrcpy", "Handshake device=" + handshake[0] + "x" + handshake[1]
                        + " video=" + handshake[2] + "x" + handshake[3]);

                socketInputStream = dataInputStream;
                socketOutputStream = dataOutputStream;

                socket_status = true;

                loop(dataInputStream, dataOutputStream, delay);

            } catch (Exception e) {
                e.printStackTrace();
                if (LetServceRunning.get()) {
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
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                socketInputStream = null;
                socketOutputStream = null;
                // 清除事件队列
                event.clear();

            }

        }

    }

    /**
     * Request Keyframe
     * 请求关键帧
     */
    public boolean requestNewKeyFrame() throws IOException {
        if (LetServceRunning.get() && socketOutputStream != null) {
            socketOutputStream.write(CommandPacket.toArray(MediaPacket.Type.COMMAND, CommandPacket.CmdType.VIDEO_NEW_KEY_FRAME, new byte[0]));
            return true;
        }
        return false;
    }

    private void loop(DataInputStream dataInputStream, DataOutputStream dataOutputStream, int delay) throws InterruptedException {
        VideoPacket.StreamSettings streamSettings = null;
        byte[] packetSize = new byte[4];

        // 由于网络传输存在延迟，丢弃数据包计数
        long lastVideoOffset = 0;
        long lastAudioOffset = 0;

        boolean waitKeyFrame = false;
        boolean pendingConfigure = false;
        long lastKeyframeRequest = 0;


        while (LetServceRunning.get()) {
            boolean waitEvent = true;
            try {
                if (pendingConfigure && tryConfigureVideoDecoder(streamSettings)) {
                    pendingConfigure = false;
                    updateAvailable.set(false);
                }
                byte[] sendevent;
                int drained = 0;
                while ((sendevent = event.poll()) != null) {
                    waitEvent = false;
                    try {
                        byte[] data = ControlPacket.toArray(MediaPacket.Type.CONTROL, sendevent);
                        dataOutputStream.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (serviceCallbacks != null) {
                            serviceCallbacks.errorDisconnect();
                        }
                        LetServceRunning.set(false);
                        break;
                    }
                    drained++;
                    if (drained >= 64) {
                        break;
                    }
                }
                if (drained > 0) {
                    dataOutputStream.flush();
                }

                if (dataInputStream.available() > 0) {
                    waitEvent = false;
                    dataInputStream.readFully(packetSize, 0, 4);
                    int size = ByteUtils.bytesToInt(packetSize);
                    if (size > 4 * 1024 * 1024) {  // 如果单个数据包大于 4m ，直接断开连接
                        if (serviceCallbacks != null) {
                            serviceCallbacks.errorDisconnect();
                        }
                        LetServceRunning.set(false);
                        return;
                    }
                    byte[] packet = new byte[size];
                    dataInputStream.readFully(packet, 0, size);
                    if (MediaPacket.Type.getType(packet[0]) == MediaPacket.Type.VIDEO) {
                        VideoPacket videoPacket = VideoPacket.readHead(packet);
                        if (videoPacket.flag == VideoPacket.Flag.CONFIG) {
                            // Sempre parsare SPS/PPS: setParms/rotazione possono alzare
                            // updateAvailable prima del primo CONFIG e, se lo saltiamo, lo
                            // schermo resta nero per sempre (decoder mai configurato).
                            int dataLength = packet.length - videoPacket.headLength();
                            byte[] data = new byte[dataLength];
                            System.arraycopy(packet, videoPacket.headLength(), data, 0, dataLength);
                            streamSettings = VideoPacket.getStreamSettings(data, "video/hevc".equals(videoMimeType));
                            pendingConfigure = true;
                            if (!first_time) {
                                applyRemoteRotation();
                                if (serviceCallbacks != null) {
                                    serviceCallbacks.loadNewRotation();
                                }
                                int waitSurface = 40;
                                while (!updateAvailable.get() && LetServceRunning.get() && waitSurface-- > 0) {
                                    try {
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        if (pendingConfigure || updateAvailable.get()) {
                            updateAvailable.set(false);
                            if (tryConfigureVideoDecoder(streamSettings)) {
                                pendingConfigure = false;
                            }
                        }
                        if (videoPacket.flag == VideoPacket.Flag.END) {
                            // need close stream
                            Log.e("Scrcpy", "END ... ");
                        } else if (videoPacket.flag != VideoPacket.Flag.CONFIG) {
                            // Log.e("Scrcpy", "videoPacket presentationTimeStamp ... " + videoPacket.presentationTimeStamp);
                            if (lastVideoOffset == 0) {
                                lastVideoOffset = System.currentTimeMillis() - (videoPacket.presentationTimeStamp / 1000);
                            }
                            long latencyMs = System.currentTimeMillis() - (lastVideoOffset + (videoPacket.presentationTimeStamp / 1000));
                            // Software encoder: non scartare i P-frame sotto ~1s, altrimenti
                            // si attende un keyframe e lo specchio sembra lento/a scatti.
                            if (videoPacket.flag == VideoPacket.Flag.KEY_FRAME) {
                                waitKeyFrame = false;
                                if (latencyMs > 1500) {
                                    lastVideoOffset = System.currentTimeMillis() - (videoPacket.presentationTimeStamp / 1000);
                                }
                                videoDecoder.decodeSample(packet, videoPacket.headLength(), packet.length - videoPacket.headLength(),
                                        0, videoPacket.flag.getFlag());
                            } else if (waitKeyFrame) {
                                long now = System.currentTimeMillis();
                                if (now - lastKeyframeRequest > 400) {
                                    lastKeyframeRequest = now;
                                    requestNewKeyFrame();
                                }
                            } else if (latencyMs <= 1500) {
                                videoDecoder.decodeSample(packet, videoPacket.headLength(), packet.length - videoPacket.headLength(),
                                        0, videoPacket.flag.getFlag());
                            } else {
                                waitKeyFrame = true;
                                lastKeyframeRequest = System.currentTimeMillis();
                                requestNewKeyFrame();
                            }
                        }
                        first_time = false;
                    } else if (MediaPacket.Type.getType(packet[0]) == MediaPacket.Type.AUDIO) {
                        AudioPacket audioPacket = AudioPacket.readHead(packet);
                        // byte[] data = audioPacket.data;
                        if (audioPacket.flag == AudioPacket.Flag.CONFIG) {
                            int dataLength = packet.length - audioPacket.headLength();
                            byte[] data = new byte[dataLength];
                            System.arraycopy(packet, audioPacket.headLength(), data, 0, dataLength);
                            if(audioDecoder != null){
                                audioDecoder.configure(data);
                            }
                        } else if (audioPacket.flag == AudioPacket.Flag.END) {
                            // need close stream
                            Log.e("Scrcpy", "Audio END ... ");
                        } else {
                            if (lastAudioOffset == 0) {
                                lastAudioOffset = System.currentTimeMillis() - (audioPacket.presentationTimeStamp / 1000);
                            }
                            if (System.currentTimeMillis() - (lastAudioOffset + (audioPacket.presentationTimeStamp / 1000)) < delay) {
                                if(audioDecoder != null){
                                    audioDecoder.decodeSample(packet, audioPacket.headLength(), packet.length - audioPacket.headLength(),
                                            0, audioPacket.flag.getFlag());
                                }
                            }
                        }
                    }

                }
            } catch (IOException e) {
                Log.e("Scrcpy", "IOException: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (waitEvent) {
                    Thread.sleep(5);
                }
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
